package com.smriti.clinicalscribe.data

import com.smriti.clinicalscribe.audio.VoiceNoteMetadata
import com.smriti.clinicalscribe.rag.ProtocolChunk
import com.smriti.clinicalscribe.reasoning.VisitReasoningResult

class LocalVisitMemoryStore(
    private val patientDao: PatientDao,
    private val visitLogDao: VisitLogDao,
    private val referralFlagDao: ReferralFlagDao,
    private val protocolChunkDao: ProtocolChunkDao
) {
    constructor(database: AppDatabase) : this(
        patientDao = database.patientDao(),
        visitLogDao = database.visitLogDao(),
        referralFlagDao = database.referralFlagDao(),
        protocolChunkDao = database.protocolChunkDao()
    )

    suspend fun seedDemoIfNeeded(
        protocolChunks: List<ProtocolChunk>,
        nowMillis: Long = System.currentTimeMillis()
    ): VisitMemorySnapshot {
        if (patientDao.getAll().isEmpty()) {
            patientDao.upsertAll(DemoSeedData.patients)
        }
        if (protocolChunkDao.getAll().isEmpty()) {
            protocolChunkDao.upsertAll(protocolChunks)
        }
        if (visitLogDao.getAll().isEmpty()) {
            DemoSeedData.initialVisitLogs(nowMillis).forEach { visitLogDao.insert(it) }
        }
        return refresh()
    }

    suspend fun saveConfirmedVisit(
        result: VisitReasoningResult,
        editedNote: String,
        editedFollowUp: String,
        voiceNote: VoiceNoteMetadata?,
        nowMillis: Long = System.currentTimeMillis()
    ): VisitMemorySnapshot {
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
        result.referralFlag?.let { flag ->
            referralFlagDao.insert(flag.copy(visitLogId = visitId))
        }
        return refresh()
    }

    suspend fun resetDemoData(
        protocolChunks: List<ProtocolChunk>,
        nowMillis: Long = System.currentTimeMillis()
    ): VisitMemorySnapshot {
        referralFlagDao.deleteAll()
        visitLogDao.deleteAll()
        patientDao.upsertAll(DemoSeedData.patients)
        protocolChunkDao.upsertAll(protocolChunks)
        DemoSeedData.initialVisitLogs(nowMillis).forEach { visitLogDao.insert(it) }
        return refresh()
    }

    suspend fun refresh(): VisitMemorySnapshot {
        return VisitMemorySnapshot(
            patients = patientDao.getAll(),
            visits = visitLogDao.getAll(),
            referrals = referralFlagDao.getAll()
        )
    }

    fun historyForPatient(snapshot: VisitMemorySnapshot, patientId: String): List<VisitLog> {
        return snapshot.visits.filter { it.patientId == patientId }
    }
}

data class VisitMemorySnapshot(
    val patients: List<Patient>,
    val visits: List<VisitLog>,
    val referrals: List<ReferralFlag>
)
