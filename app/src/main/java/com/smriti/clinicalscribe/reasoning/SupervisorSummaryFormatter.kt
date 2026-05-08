package com.smriti.clinicalscribe.reasoning

import com.smriti.clinicalscribe.data.Patient
import com.smriti.clinicalscribe.data.FollowUpDueState
import com.smriti.clinicalscribe.data.FollowUpTask
import com.smriti.clinicalscribe.data.FollowUpTaskScheduler
import com.smriti.clinicalscribe.data.FollowUpTaskStatus
import com.smriti.clinicalscribe.data.ReferralFlag
import com.smriti.clinicalscribe.data.TranscriptSource
import com.smriti.clinicalscribe.data.VisitLog
import java.util.Calendar

object SupervisorSummaryFormatter {
    fun buildLocalSavedSummary(
        patients: List<Patient>,
        visits: List<VisitLog>,
        referrals: List<ReferralFlag>,
        followUpTasks: List<FollowUpTask> = emptyList(),
        narrative: String = "Saved visits, patient history, and local health guidance on this device.",
        nowMillis: Long = summaryAnchorMillis(visits, referrals)
    ): SupervisorSummary {
        val patientNamesById = patients.associate { it.id to it.name }
        val todayStart = startOfDayMillis(nowMillis)
        val savedVisitsToday = visits.filter { it.isUserSavedVisitToday(todayStart) }
        val savedVisitIdsToday = savedVisitsToday.map { it.id }.toSet()
        val savedReferralsToday = referrals.filter { referral ->
            referral.createdAtMillis >= todayStart &&
                (referral.visitLogId == null || referral.visitLogId in savedVisitIdsToday)
        }
        val paperUrgent = savedVisitsToday
            .filter { PaperNoteUrgencyClassifier.needsUrgentReview(it) }
            .sortedByDescending { it.visitDateMillis }
        val paperUrgentVisitIds = paperUrgent.map { it.id }.toSet()
        val activeFollowUpTasks = followUpTasks
            .filter { it.status in FollowUpTaskStatus.ACTIVE }
            .sortedBy { it.dueDateMillis }
        val taskFollowUpsDue = activeFollowUpTasks.map { task ->
            val patientName = task.patientName.ifBlank { patientNamesById[task.patientId] ?: task.patientId }
            "$patientName: ${task.reason.ifBlank { "Check again" }} (${followUpStateLabel(task, nowMillis)})"
        }
        val savedVisitFollowUpsDue = savedVisitsToday
            .filter {
                it.confirmed &&
                    it.suggestedFollowUp.isNotBlank() &&
                    it.id !in paperUrgentVisitIds &&
                    !ReferralLanguageGuard.containsReferralLikeLanguage(it.suggestedFollowUp)
            }
            .map { visit ->
                val patientName = patientNamesById[visit.patientId] ?: visit.patientId
                "$patientName: ${visit.suggestedFollowUp}"
            }
        val followUpsDue = taskFollowUpsDue.ifEmpty { savedVisitFollowUpsDue }
        val paperLines = paperUrgent.map { visit ->
            val name = patientNamesById[visit.patientId] ?: visit.patientId
            paperScanUrgentReviewCardBody(displayName = name, visit = visit)
        }
        val taskStates = activeFollowUpTasks.map { FollowUpTaskScheduler.dueState(it, nowMillis) }

        return SupervisorSummary(
            totalVisits = savedVisitsToday.size,
            referralsFlagged = savedReferralsToday.size,
            urgentCases = urgentCases(patients, savedReferralsToday),
            followUpsDue = followUpsDue,
            narrative = narrative,
            paperScanNeedsUrgentReview = paperLines,
            openFollowUps = activeFollowUpTasks.size,
            overdueFollowUps = taskStates.count { it == FollowUpDueState.OVERDUE },
            dueTodayFollowUps = taskStates.count { it == FollowUpDueState.DUE },
            upcomingFollowUps = taskStates.count { it == FollowUpDueState.UPCOMING }
        )
    }

    fun paperScanUrgentReviewCardBody(displayName: String, visit: VisitLog): String {
        val phrase = PaperNoteUrgencyClassifier.issueSummaryPhrase(visit)
        return "$displayName: $phrase. Review scanned note and local guidance.\n\n" +
            "Not a diagnosis. Health worker reviewed before saving."
    }

    fun urgentCases(
        patients: List<Patient>,
        referrals: List<ReferralFlag>
    ): List<String> {
        val patientNamesById = patients.associate { it.id to it.name }
        return referrals
            .groupBy { it.patientId }
            .values
            .mapNotNull { flags -> flags.maxByOrNull { it.createdAtMillis } }
            .sortedByDescending { it.createdAtMillis }
            .map { flag ->
                val patientName = patientNamesById[flag.patientId] ?: flag.patientId
                val dangerSigns = conciseDangerSigns(flag)
                val citation = briefCitation(flag.protocolBasis)
                "$patientName - ${flag.urgency} - $dangerSigns. Health guidance: $citation."
            }
    }

    private fun conciseDangerSigns(flag: ReferralFlag): String {
        val rawText = flag.dangerSigns.ifBlank { flag.reason }
        val normalized = rawText.lowercase()
        val matchedSigns = buildList {
            if ("headache" in normalized) add("headache")
            if ("blurred vision" in normalized) add("blurred vision")
            if (
                "high blood pressure" in normalized ||
                "bp 150" in normalized ||
                "150 over 95" in normalized
            ) {
                add("high blood pressure")
            }
            if ("reduced fetal" in normalized || "reduced movement" in normalized) {
                add("reduced fetal movement")
            }
            if ("convulsion" in normalized) add("convulsions")
            if ("bleeding" in normalized) add("bleeding")
        }.distinct()

        if (matchedSigns.isNotEmpty()) {
            return matchedSigns.joinToString()
        }

        return rawText
            .replace(Regex("\\s+"), " ")
            .trim()
            .takeIf { it.isNotBlank() }
            ?.let { if (it.length <= MAX_FALLBACK_DANGER_SIGNS_LENGTH) it else it.take(MAX_FALLBACK_DANGER_SIGNS_LENGTH).trimEnd() }
            ?: "danger signs recorded"
    }

    private fun briefCitation(protocolBasis: String): String {
        return protocolBasis
            .substringBefore(";")
            .replace(Regex("\\s+"), " ")
            .trim()
            .ifBlank { "Review saved note" }
    }

    private fun followUpStateLabel(task: FollowUpTask, nowMillis: Long): String {
        return when (FollowUpTaskScheduler.dueState(task, nowMillis)) {
            FollowUpDueState.OVERDUE -> "overdue"
            FollowUpDueState.DUE -> "due today"
            FollowUpDueState.UPCOMING -> "upcoming"
        }
    }

    private fun VisitLog.isUserSavedVisitToday(todayStart: Long): Boolean {
        return confirmed &&
            transcriptSource != TranscriptSource.SEEDED_PRIOR_HISTORY &&
            visitDateMillis >= todayStart
    }

    private fun startOfDayMillis(nowMillis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun summaryAnchorMillis(
        visits: List<VisitLog>,
        referrals: List<ReferralFlag>
    ): Long {
        val latestSavedVisit = visits
            .filter { it.confirmed && it.transcriptSource != TranscriptSource.SEEDED_PRIOR_HISTORY }
            .maxOfOrNull { it.visitDateMillis }
        val latestReferral = referrals.maxOfOrNull { it.createdAtMillis }
        return listOfNotNull(latestSavedVisit, latestReferral)
            .maxOrNull()
            ?: System.currentTimeMillis()
    }

    private const val MAX_FALLBACK_DANGER_SIGNS_LENGTH = 90
}
