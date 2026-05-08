package com.smriti.clinicalscribe.ui

import com.smriti.clinicalscribe.data.FollowUpDueState
import com.smriti.clinicalscribe.data.FollowUpTask
import com.smriti.clinicalscribe.data.FollowUpTaskScheduler
import com.smriti.clinicalscribe.data.FollowUpTaskStatus
import com.smriti.clinicalscribe.data.Patient
import com.smriti.clinicalscribe.data.PatientMemoryInsights
import com.smriti.clinicalscribe.data.ReferralFlag
import com.smriti.clinicalscribe.data.VisitLog

data class CommunityPanel(
    val totalPatients: Int,
    val pregnantPatients: Int,
    val thirdTrimesterCount: Int,
    val nearTermCount: Int,
    val urgentReferralSavedCount: Int,
    val openFollowUpCount: Int,
    val overdueFollowUpCount: Int,
    val dueOrUpcomingFollowUpCount: Int,
    val historySignalCount: Int,
    val missedRecentVisitCount: Int,
    val routineCount: Int,
    val countriesRepresented: List<String>,
    val noteLanguagesRepresented: List<String>,
    val attentionPatients: List<CommunityPanelLine>,
    val followUpPatients: List<CommunityPanelLine>,
    val urgentPatients: List<CommunityPanelLine>,
    val todayFocus: List<CommunityPanelLine>,
    val narrative: String
)

data class CommunityPanelLine(
    val patientId: String,
    val patientName: String,
    val label: String,
    val detail: String,
    val tone: PatientChipTone
)

object CommunityPanelBuilder {
    private const val RECENT_VISIT_WINDOW_MILLIS = 60L * 24L * 60L * 60L * 1000L

    fun build(
        patients: List<Patient>,
        visits: List<VisitLog>,
        referrals: List<ReferralFlag>,
        followUpTasks: List<FollowUpTask>,
        nowMillis: Long = System.currentTimeMillis()
    ): CommunityPanel {
        val patientsById = patients.associateBy { it.id }
        val activeTasks = followUpTasks
            .filter { it.status in FollowUpTaskStatus.ACTIVE }
            .sortedBy { it.dueDateMillis }
        val taskStates = activeTasks.map { task -> task.id to FollowUpTaskScheduler.dueState(task, nowMillis) }.toMap()
        val latestReferralByPatient = referrals
            .groupBy { it.patientId }
            .mapValues { entry -> entry.value.maxByOrNull { it.createdAtMillis }!! }
        val historySignalPatientIds = patients
            .filter { patient -> PatientMemoryInsights.risingBloodPressureSignal(patient, visits) != null }
            .map { it.id }
            .toSet()
        val noRecentVisitPatientIds = patients
            .filter { patient -> hasNoRecentVisit(patient.id, visits, nowMillis) }
            .map { it.id }
            .toSet()

        val urgentPatients = latestReferralByPatient.values
            .sortedByDescending { it.createdAtMillis }
            .mapNotNull { referral ->
                val patient = patientsById[referral.patientId] ?: return@mapNotNull null
                CommunityPanelLine(
                    patientId = patient.id,
                    patientName = patient.name,
                    label = "Urgent review saved",
                    detail = referral.dangerSigns.ifBlank { referral.reason }.shortLine("Review saved note"),
                    tone = PatientChipTone.Urgent
                )
            }
        val followUpPatients = activeTasks.map { task ->
            val patient = patientsById[task.patientId]
            CommunityPanelLine(
                patientId = task.patientId,
                patientName = task.patientName.ifBlank { patient?.name ?: task.patientId },
                label = followUpLabel(taskStates.getValue(task.id)),
                detail = task.reason.ifBlank { "Check again" },
                tone = if (taskStates.getValue(task.id) == FollowUpDueState.OVERDUE) {
                    PatientChipTone.Urgent
                } else {
                    PatientChipTone.Caution
                }
            )
        }
        val todayFocus = patients
            .sortedWith(compareBy<Patient> { communityPriorityRank(it, referrals, activeTasks, historySignalPatientIds, noRecentVisitPatientIds, nowMillis) }
                .thenBy { it.name.lowercase() })
            .map { patient -> focusLine(patient, referrals, activeTasks, historySignalPatientIds, noRecentVisitPatientIds, nowMillis) }
        val attentionPatients = todayFocus.filter { it.label != "Routine" }
        val routineCount = todayFocus.count { it.label == "Routine" }
        val overdueCount = taskStates.values.count { it == FollowUpDueState.OVERDUE }
        val dueOrUpcomingCount = taskStates.values.count { it == FollowUpDueState.DUE || it == FollowUpDueState.UPCOMING }

        return CommunityPanel(
            totalPatients = patients.size,
            pregnantPatients = patients.count { it.pregnancyWeeks != null },
            thirdTrimesterCount = patients.count { (it.pregnancyWeeks ?: 0) >= 28 },
            nearTermCount = patients.count { (it.pregnancyWeeks ?: 0) >= 36 },
            urgentReferralSavedCount = latestReferralByPatient.size,
            openFollowUpCount = activeTasks.size,
            overdueFollowUpCount = overdueCount,
            dueOrUpcomingFollowUpCount = dueOrUpcomingCount,
            historySignalCount = historySignalPatientIds.size,
            missedRecentVisitCount = noRecentVisitPatientIds.size,
            routineCount = routineCount,
            countriesRepresented = patients.map { it.country.ifBlank { it.countryCode.ifBlank { "Local" } } }.distinct().sorted(),
            noteLanguagesRepresented = patients.map { PatientVisitUiText.noteLanguageName(it) }.distinct().sorted(),
            attentionPatients = attentionPatients,
            followUpPatients = followUpPatients,
            urgentPatients = urgentPatients,
            todayFocus = todayFocus,
            narrative = panelNarrative(latestReferralByPatient.size, overdueCount, dueOrUpcomingCount, attentionPatients.size)
        )
    }

    private fun communityPriorityRank(
        patient: Patient,
        referrals: List<ReferralFlag>,
        activeTasks: List<FollowUpTask>,
        historySignalPatientIds: Set<String>,
        noRecentVisitPatientIds: Set<String>,
        nowMillis: Long
    ): Int {
        if (referrals.any { it.patientId == patient.id }) return 0
        val taskState = activeTasks
            .filter { it.patientId == patient.id }
            .minByOrNull { it.dueDateMillis }
            ?.let { FollowUpTaskScheduler.dueState(it, nowMillis) }
        when (taskState) {
            FollowUpDueState.OVERDUE -> return 1
            FollowUpDueState.DUE -> return 2
            FollowUpDueState.UPCOMING -> return 3
            null -> Unit
        }
        if (patient.id in historySignalPatientIds) return 4
        if (patient.id in noRecentVisitPatientIds) return 5
        if ((patient.pregnancyWeeks ?: 0) >= 36) return 6
        return 7
    }

    private fun focusLine(
        patient: Patient,
        referrals: List<ReferralFlag>,
        activeTasks: List<FollowUpTask>,
        historySignalPatientIds: Set<String>,
        noRecentVisitPatientIds: Set<String>,
        nowMillis: Long
    ): CommunityPanelLine {
        val referral = referrals.filter { it.patientId == patient.id }.maxByOrNull { it.createdAtMillis }
        if (referral != null) {
            return CommunityPanelLine(patient.id, patient.name, "Urgent review saved", referral.dangerSigns.ifBlank { referral.reason }.shortLine("Review saved note"), PatientChipTone.Urgent)
        }
        val task = activeTasks.filter { it.patientId == patient.id }.minByOrNull { it.dueDateMillis }
        if (task != null) {
            val state = FollowUpTaskScheduler.dueState(task, nowMillis)
            return CommunityPanelLine(
                patientId = patient.id,
                patientName = patient.name,
                label = followUpLabel(state),
                detail = task.reason.ifBlank { "Check again" },
                tone = if (state == FollowUpDueState.OVERDUE) PatientChipTone.Urgent else PatientChipTone.Caution
            )
        }
        if (patient.id in historySignalPatientIds) {
            return CommunityPanelLine(patient.id, patient.name, "History signal", "Review recent visit pattern", PatientChipTone.Caution)
        }
        if (patient.id in noRecentVisitPatientIds) {
            return CommunityPanelLine(patient.id, patient.name, "No recent visit", "Check whether a visit is needed", PatientChipTone.Caution)
        }
        if ((patient.pregnancyWeeks ?: 0) >= 36) {
            return CommunityPanelLine(patient.id, patient.name, "Near term", PatientVisitUiText.gestationLabel(patient), PatientChipTone.Caution)
        }
        return CommunityPanelLine(patient.id, patient.name, "Routine", PatientVisitUiText.gestationLabel(patient), PatientChipTone.Routine)
    }

    private fun hasNoRecentVisit(
        patientId: String,
        visits: List<VisitLog>,
        nowMillis: Long
    ): Boolean {
        val latestVisit = visits
            .filter { it.patientId == patientId && it.confirmed }
            .maxOfOrNull { it.visitDateMillis }
            ?: return true
        return latestVisit < nowMillis - RECENT_VISIT_WINDOW_MILLIS
    }

    private fun followUpLabel(state: FollowUpDueState): String {
        return when (state) {
            FollowUpDueState.OVERDUE -> "Follow-up overdue"
            FollowUpDueState.DUE -> "Follow-up due"
            FollowUpDueState.UPCOMING -> "Follow-up upcoming"
        }
    }

    private fun panelNarrative(
        urgentCount: Int,
        overdueCount: Int,
        dueOrUpcomingCount: Int,
        attentionCount: Int
    ): String {
        return when {
            urgentCount > 0 -> "Start with $urgentCount patient${plural(urgentCount)} with urgent review saved, then follow-ups due."
            overdueCount > 0 -> "Start with $overdueCount overdue follow-up${plural(overdueCount)}, then check upcoming visits."
            dueOrUpcomingCount > 0 -> "Today, confirm $dueOrUpcomingCount follow-up${plural(dueOrUpcomingCount)} before routine visits."
            attentionCount > 0 -> "Review attention cases first, then continue routine visits."
            else -> "No attention cases are saved on this device right now."
        }
    }

    private fun plural(count: Int): String = if (count == 1) "" else "s"

    private fun String.shortLine(fallback: String): String {
        val cleaned = replace(Regex("\\s+"), " ").trim()
        if (cleaned.isBlank()) return fallback
        return if (cleaned.length <= 96) cleaned else cleaned.take(96).trimEnd()
    }
}
