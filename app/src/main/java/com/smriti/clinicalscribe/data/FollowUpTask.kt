package com.smriti.clinicalscribe.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Calendar
import java.util.Locale

@Entity(
    tableName = "follow_up_tasks",
    indices = [
        Index(value = ["patientId"]),
        Index(value = ["createdFromVisitId"]),
        Index(value = ["status"]),
        Index(value = ["dueDateMillis"])
    ]
)
data class FollowUpTask(
    @PrimaryKey val id: String,
    val patientId: String,
    val patientName: String,
    val createdFromVisitId: Long?,
    val dueDateMillis: Long,
    val reason: String,
    val language: String,
    val status: String,
    val createdAtMillis: Long,
    val completedAtMillis: Long? = null,
    val updatedAtMillis: Long,
    val source: String
)

object FollowUpTaskStatus {
    const val OPEN = "OPEN"
    const val COMPLETED = "COMPLETED"
    const val RESCHEDULED = "RESCHEDULED"
    const val DISMISSED = "DISMISSED"

    val ACTIVE = listOf(OPEN, RESCHEDULED)
}

object FollowUpTaskSource {
    const val SAVED_VISIT = "SAVED_VISIT"
    const val SEEDED_HISTORY = "SEEDED_HISTORY"
    const val MANUAL = "MANUAL"
}

enum class FollowUpDueState {
    OVERDUE,
    DUE,
    UPCOMING
}

object FollowUpTaskScheduler {
    private const val DAY_MILLIS = 24L * 60L * 60L * 1000L

    fun taskForSavedVisit(
        patient: Patient?,
        visit: VisitLog,
        referralFlag: ReferralFlag?,
        language: String,
        nowMillis: Long
    ): FollowUpTask? {
        val reason = followUpReason(
            followUpText = visit.suggestedFollowUp,
            hasReferral = referralFlag != null
        ) ?: return null
        return FollowUpTask(
            id = savedVisitTaskId(visit.id),
            patientId = visit.patientId,
            patientName = patient?.name.orEmpty(),
            createdFromVisitId = visit.id,
            dueDateMillis = dueDateFromText(
                text = visit.suggestedFollowUp,
                nowMillis = nowMillis,
                explicitDueDateMillis = visit.followUpDueDateMillis,
                hasReferral = referralFlag != null
            ),
            reason = reason,
            language = language.ifBlank { patient?.preferredLanguage.orEmpty().ifBlank { PatientLanguages.English.code } },
            status = FollowUpTaskStatus.OPEN,
            createdAtMillis = nowMillis,
            completedAtMillis = null,
            updatedAtMillis = nowMillis,
            source = FollowUpTaskSource.SAVED_VISIT
        )
    }

    fun taskForSeededVisit(
        patient: Patient?,
        visit: VisitLog,
        nowMillis: Long
    ): FollowUpTask? {
        val dueDateMillis = visit.followUpDueDateMillis ?: return null
        if (visit.followUpCompleted == true) return null
        val reason = followUpReason(
            followUpText = visit.suggestedFollowUp,
            hasReferral = false
        ) ?: "Confirm follow-up outcome"
        return FollowUpTask(
            id = seededVisitTaskId(visit.id),
            patientId = visit.patientId,
            patientName = patient?.name.orEmpty(),
            createdFromVisitId = visit.id,
            dueDateMillis = dueDateMillis,
            reason = reason,
            language = patient?.preferredLanguage ?: PatientLanguages.English.code,
            status = FollowUpTaskStatus.OPEN,
            createdAtMillis = visit.visitDateMillis,
            completedAtMillis = null,
            updatedAtMillis = nowMillis,
            source = FollowUpTaskSource.SEEDED_HISTORY
        )
    }

    fun dueState(task: FollowUpTask, nowMillis: Long = System.currentTimeMillis()): FollowUpDueState {
        val todayStart = startOfDayMillis(nowMillis)
        val tomorrowStart = todayStart + DAY_MILLIS
        return when {
            task.dueDateMillis < todayStart -> FollowUpDueState.OVERDUE
            task.dueDateMillis < tomorrowStart -> FollowUpDueState.DUE
            else -> FollowUpDueState.UPCOMING
        }
    }

    fun savedVisitTaskId(visitId: Long): String = "saved-visit-$visitId"

    fun seededVisitTaskId(visitId: Long): String = "seeded-visit-$visitId"

    fun defaultRescheduleDate(nowMillis: Long): Long {
        return startOfDayMillis(nowMillis) + (7L * DAY_MILLIS)
    }

    private fun followUpReason(
        followUpText: String,
        hasReferral: Boolean
    ): String? {
        val trimmed = followUpText.replace(Regex("\\s+"), " ").trim()
        if (trimmed.isBlank()) return null
        val normalized = trimmed.lowercase(Locale.US)
        val noPlan = listOf(
            "no follow-up plan",
            "no follow up plan",
            "no follow-up was extracted",
            "chw review and confirmation required before saving"
        ).any { it in normalized }
        if (noPlan) return null
        if (hasReferral) return "Confirm referral outcome"
        return trimmed.take(MAX_REASON_LENGTH).trimEnd()
    }

    private fun dueDateFromText(
        text: String,
        nowMillis: Long,
        explicitDueDateMillis: Long?,
        hasReferral: Boolean
    ): Long {
        explicitDueDateMillis?.let { return it }
        if (hasReferral) return defaultRescheduleDate(nowMillis)
        val normalized = text.lowercase(Locale.US)
        val days = when {
            "tomorrow" in normalized -> 1L
            "3 days" in normalized || "three days" in normalized -> 3L
            "2 weeks" in normalized || "two weeks" in normalized -> 14L
            "1 week" in normalized || "one week" in normalized || "next week" in normalized -> 7L
            else -> 7L
        }
        return startOfDayMillis(nowMillis) + (days * DAY_MILLIS)
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

    private const val MAX_REASON_LENGTH = 160
}
