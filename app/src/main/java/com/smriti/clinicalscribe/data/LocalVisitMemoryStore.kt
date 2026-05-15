package com.smriti.clinicalscribe.data

import com.smriti.clinicalscribe.audio.VoiceNoteMetadata
import com.smriti.clinicalscribe.rag.ProtocolChunk
import com.smriti.clinicalscribe.reasoning.PaperNoteVisionExtraction
import com.smriti.clinicalscribe.reasoning.RealGemmaUnavailableResult
import com.smriti.clinicalscribe.reasoning.VisitReasoningResult

class LocalVisitMemoryStore(
    private val patientDao: PatientDao,
    private val visitLogDao: VisitLogDao,
    private val referralFlagDao: ReferralFlagDao,
    private val followUpTaskDao: FollowUpTaskDao,
    private val protocolChunkDao: ProtocolChunkDao
) {
    constructor(database: AppDatabase) : this(
        patientDao = database.patientDao(),
        visitLogDao = database.visitLogDao(),
        referralFlagDao = database.referralFlagDao(),
        followUpTaskDao = database.followUpTaskDao(),
        protocolChunkDao = database.protocolChunkDao()
    )

    suspend fun seedDemoIfNeeded(
        protocolChunks: List<ProtocolChunk>,
        nowMillis: Long = System.currentTimeMillis()
    ): VisitMemorySnapshot {
        val existingPatientIds = patientDao.getAll().map { it.id }.toSet()
        val missingDemoPatients = DemoSeedData.patients.filter { it.id !in existingPatientIds }
        if (missingDemoPatients.isNotEmpty()) {
            patientDao.upsertAll(missingDemoPatients)
        }
        if (protocolChunkDao.getAll().isEmpty()) {
            protocolChunkDao.upsertAll(protocolChunks)
        }
        val existingVisits = visitLogDao.getAll()
        val existingVisitIds = existingVisits.map { it.id }.toSet()
        val demoVisits = DemoSeedData.initialVisitLogs(nowMillis)
        val missingDemoVisits = demoVisits
            .filter { it.id !in existingVisitIds }
        if (missingDemoVisits.isNotEmpty()) {
            visitLogDao.upsertAll(missingDemoVisits)
        }
        val phaseBackfills = demoVisits.mapNotNull { demoVisit ->
            val existing = existingVisits.firstOrNull { it.id == demoVisit.id } ?: return@mapNotNull null
            existing.withMissingDemoFollowUpFieldsFrom(demoVisit)
        }
        if (phaseBackfills.isNotEmpty()) {
            visitLogDao.upsertAll(phaseBackfills)
        }
        seedMissingFollowUpTasks(nowMillis)
        return refresh()
    }

    suspend fun addPatient(patient: Patient): VisitMemorySnapshot {
        patientDao.upsertAll(listOf(patient))
        return refresh()
    }

    suspend fun markFollowUpConfirmed(visitId: Long): VisitMemorySnapshot {
        visitLogDao.updateFollowUpCompleted(visitId = visitId, completed = true)
        followUpTaskDao.markCompleted(
            taskId = FollowUpTaskScheduler.seededVisitTaskId(visitId),
            status = FollowUpTaskStatus.COMPLETED,
            completedAtMillis = System.currentTimeMillis(),
            updatedAtMillis = System.currentTimeMillis()
        )
        return refresh()
    }

    suspend fun markFollowUpTaskCompleted(
        taskId: String,
        nowMillis: Long = System.currentTimeMillis()
    ): VisitMemorySnapshot {
        followUpTaskDao.markCompleted(
            taskId = taskId,
            status = FollowUpTaskStatus.COMPLETED,
            completedAtMillis = nowMillis,
            updatedAtMillis = nowMillis
        )
        refresh().followUpTasks.firstOrNull { it.id == taskId }?.createdFromVisitId?.let { visitId ->
            visitLogDao.updateFollowUpCompleted(visitId = visitId, completed = true)
        }
        return refresh()
    }

    suspend fun rescheduleFollowUpTask(
        taskId: String,
        dueDateMillis: Long,
        reason: String,
        nowMillis: Long = System.currentTimeMillis()
    ): VisitMemorySnapshot {
        followUpTaskDao.reschedule(
            taskId = taskId,
            dueDateMillis = dueDateMillis,
            reason = reason.trim().ifBlank { "Check again" },
            status = FollowUpTaskStatus.RESCHEDULED,
            updatedAtMillis = nowMillis
        )
        return refresh()
    }

    suspend fun importSupervisorRegister(
        register: SupervisorRegister
    ): SupervisorRegisterImportResult {
        val existingPatientIds = patientDao.getAll().map { it.id }.toSet()
        patientDao.upsertAll(register.patients)
        visitLogDao.upsertAll(register.priorVisits)
        seedMissingFollowUpTasks(nowMillis = System.currentTimeMillis())
        val snapshot = refresh()
        val importedPatientIds = register.patients.map { it.id }.toSet()
        return SupervisorRegisterImportResult(
            patientCount = register.patients.size,
            patientsAdded = importedPatientIds.count { it !in existingPatientIds },
            patientsUpdated = importedPatientIds.count { it in existingPatientIds },
            visitCount = register.priorVisits.size,
            snapshot = snapshot
        )
    }

    suspend fun saveConfirmedVisit(
        result: VisitReasoningResult,
        editedNote: String,
        editedFollowUp: String,
        voiceNote: VoiceNoteMetadata?,
        nowMillis: Long = System.currentTimeMillis()
    ): VisitMemorySnapshot {
        require(!RealGemmaUnavailableResult.isUnavailable(result)) {
            "Invalid or unavailable RealGemma output cannot be saved."
        }
        val visitId = visitLogDao.insert(
            VisitLog(
                patientId = result.patientId,
                visitDateMillis = nowMillis,
                observationText = result.observationText,
                structuredNote = editedNote,
                protocolCitation = result.protocolCitation,
                suggestedFollowUp = editedFollowUp,
                confirmed = true,
                audioFilePath = voiceNote?.audioFilePath,
                audioDurationSeconds = voiceNote?.audioDurationSeconds,
                transcriptSource = if (voiceNote == null) {
                    TranscriptSource.SIMULATED
                } else {
                    TranscriptSource.REAL_ASR_PENDING
                }
            )
        )
        val savedReferral = result.referralFlag?.let { flag ->
            flag.copy(visitLogId = visitId).also { referralFlagDao.insert(it) }
        }
        val savedVisit = VisitLog(
            id = visitId,
            patientId = result.patientId,
            visitDateMillis = nowMillis,
            observationText = result.observationText,
            structuredNote = editedNote,
            protocolCitation = result.protocolCitation,
            suggestedFollowUp = editedFollowUp,
            confirmed = true,
            audioFilePath = voiceNote?.audioFilePath,
            audioDurationSeconds = voiceNote?.audioDurationSeconds,
            transcriptSource = if (voiceNote == null) {
                TranscriptSource.SIMULATED
            } else {
                TranscriptSource.REAL_ASR_PENDING
            }
        )
        createFollowUpTaskForSavedVisit(
            visit = savedVisit,
            referral = savedReferral,
            nowMillis = nowMillis
        )
        return refresh()
    }

    suspend fun saveConfirmedScannedPaperNote(
        patientId: String,
        extraction: PaperNoteVisionExtraction,
        editedPatientName: String,
        editedVisitDate: String,
        editedBloodPressure: String,
        editedSymptoms: List<String>,
        editedFollowUpPlan: String,
        nowMillis: Long = System.currentTimeMillis()
    ): VisitMemorySnapshot {
        require(patientId.isNotBlank()) {
            "A patient must be selected before saving a scanned paper note."
        }
        val confirmedExtraction = extraction.copy(
            patientName = editedPatientName.trim(),
            visitDate = editedVisitDate.trim(),
            bloodPressure = editedBloodPressure.trim(),
            symptoms = editedSymptoms.map { it.trim() }.filter { it.isNotBlank() },
            followUpPlan = editedFollowUpPlan.trim(),
            needsReview = true
        )
        val savedVisit = VisitLog(
                patientId = patientId,
                visitDateMillis = nowMillis,
                observationText = confirmedExtraction.toObservationText(),
                structuredNote = confirmedExtraction.toStructuredNote(),
                protocolCitation = "Paper note extraction only; no referral or diagnosis generated from image.",
                suggestedFollowUp = confirmedExtraction.followUpPlan.ifBlank {
                    "No follow-up plan was extracted from the paper note."
                },
                confirmed = true,
                transcriptSource = TranscriptSource.PAPER_SCAN
        )
        val visitId = visitLogDao.insert(savedVisit)
        createFollowUpTaskForSavedVisit(
            visit = savedVisit.copy(id = visitId),
            referral = null,
            nowMillis = nowMillis
        )
        return refresh()
    }

    suspend fun resetDemoData(
        protocolChunks: List<ProtocolChunk>,
        nowMillis: Long = System.currentTimeMillis()
    ): VisitMemorySnapshot {
        referralFlagDao.deleteAll()
        followUpTaskDao.deleteAll()
        visitLogDao.deleteAll()
        patientDao.deleteAll()
        patientDao.upsertAll(DemoSeedData.patients)
        protocolChunkDao.upsertAll(protocolChunks)
        visitLogDao.upsertAll(DemoSeedData.initialVisitLogs(nowMillis))
        seedMissingFollowUpTasks(nowMillis)
        return refresh()
    }

    suspend fun refresh(): VisitMemorySnapshot {
        return VisitMemorySnapshot(
            patients = patientDao.getAll(),
            visits = visitLogDao.getAll(),
            referrals = referralFlagDao.getAll(),
            followUpTasks = followUpTaskDao.getAll()
        )
    }

    fun historyForPatient(snapshot: VisitMemorySnapshot, patientId: String): List<VisitLog> {
        return snapshot.visits.filter { it.patientId == patientId }
    }

    private fun VisitLog.withMissingDemoFollowUpFieldsFrom(demoVisit: VisitLog): VisitLog? {
        if (demoVisit.followUpDueDateMillis == null) return null
        val needsDueDate = followUpDueDateMillis == null
        val needsCompletion = followUpCompleted == null
        if (!needsDueDate && !needsCompletion) return null
        return copy(
            followUpDueDateMillis = followUpDueDateMillis ?: demoVisit.followUpDueDateMillis,
            followUpCompleted = followUpCompleted ?: demoVisit.followUpCompleted
        )
    }

    private suspend fun seedMissingFollowUpTasks(nowMillis: Long) {
        val existingTaskIds = followUpTaskDao.getAll().map { it.id }.toSet()
        val patientsById = patientDao.getAll().associateBy { it.id }
        val tasks = visitLogDao.getAll()
            .filter { it.transcriptSource == TranscriptSource.SEEDED_PRIOR_HISTORY }
            .mapNotNull { visit ->
                FollowUpTaskScheduler.taskForSeededVisit(
                    patient = patientsById[visit.patientId],
                    visit = visit,
                    nowMillis = nowMillis
                )
            }
            .filter { it.id !in existingTaskIds }
        if (tasks.isNotEmpty()) {
            followUpTaskDao.upsertAll(tasks)
        }
    }

    private suspend fun createFollowUpTaskForSavedVisit(
        visit: VisitLog,
        referral: ReferralFlag?,
        nowMillis: Long
    ) {
        val patient = patientDao.getAll().firstOrNull { it.id == visit.patientId }
        val task = FollowUpTaskScheduler.taskForSavedVisit(
            patient = patient,
            visit = visit,
            referralFlag = referral,
            language = patient?.preferredLanguage.orEmpty(),
            nowMillis = nowMillis
        ) ?: return
        val duplicateActiveTask = followUpTaskDao.getOpenForPatient(
            patientId = visit.patientId,
            activeStatuses = FollowUpTaskStatus.ACTIVE
        ).any { existing ->
            existing.source == FollowUpTaskSource.SAVED_VISIT &&
                existing.reason == task.reason &&
                existing.dueDateMillis == task.dueDateMillis
        }
        if (!duplicateActiveTask) {
            followUpTaskDao.upsert(task)
        }
    }
}

data class VisitMemorySnapshot(
    val patients: List<Patient>,
    val visits: List<VisitLog>,
    val referrals: List<ReferralFlag>,
    val followUpTasks: List<FollowUpTask> = emptyList()
)

data class SupervisorRegister(
    val patients: List<Patient>,
    val priorVisits: List<VisitLog>
)

data class SupervisorRegisterImportResult(
    val patientCount: Int,
    val patientsAdded: Int,
    val patientsUpdated: Int,
    val visitCount: Int,
    val snapshot: VisitMemorySnapshot
)
