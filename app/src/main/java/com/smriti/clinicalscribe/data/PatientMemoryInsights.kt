package com.smriti.clinicalscribe.data

import java.util.Calendar
import kotlin.math.max

data class MissedFollowUpAlert(
    val visitId: Long,
    val patientId: String,
    val daysOverdue: Long,
    val reason: String,
    val protocolCitation: String
) {
    val message: String
        get() = "Missed follow-up: Follow-up was due after the previous ANC visit. Outcome unknown. Confirm before today's visit."
}

data class HistorySignal(
    val patientId: String,
    val title: String,
    val message: String,
    val readings: List<BloodPressureReading>
)

data class BloodPressureReading(
    val systolic: Int,
    val diastolic: Int,
    val visitDateMillis: Long
) {
    val label: String = "$systolic/$diastolic"
}

object PatientMemoryInsights {
    private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
    private val bloodPressureRegex = Regex("\\b(\\d{2,3})\\s*/\\s*(\\d{2,3})\\b")

    fun missedFollowUpAlerts(
        patientId: String,
        visits: List<VisitLog>,
        nowMillis: Long = System.currentTimeMillis()
    ): List<MissedFollowUpAlert> {
        val todayStartMillis = startOfDayMillis(nowMillis)
        return visits
            .filter { visit ->
                visit.patientId == patientId && visit.hasMissedFollowUpSignal(todayStartMillis)
            }
            .sortedBy { it.followUpDueDateMillis ?: it.visitDateMillis }
            .map { visit ->
                val dueMillis = visit.followUpDueDateMillis ?: visit.visitDateMillis
                val daysOverdue = max(1L, (nowMillis - dueMillis) / DAY_MILLIS)
                MissedFollowUpAlert(
                    visitId = visit.id,
                    patientId = patientId,
                    daysOverdue = daysOverdue,
                    reason = visit.suggestedFollowUp
                        .substringBefore(".")
                        .trim()
                        .ifBlank { "follow-up recommended at prior visit" },
                    protocolCitation = visit.protocolCitation
                )
            }
    }

    private fun VisitLog.hasMissedFollowUpSignal(todayStartMillis: Long): Boolean {
        if (followUpCompleted == true) return false
        val structuredSignal = followUpDueDateMillis != null &&
            followUpDueDateMillis < todayStartMillis
        return structuredSignal || hasLegacyMissedFollowUpText()
    }

    private fun VisitLog.hasLegacyMissedFollowUpText(): Boolean {
        if (followUpDueDateMillis != null) return false
        val text = listOf(observationText, structuredNote, suggestedFollowUp)
            .joinToString(separator = " ")
            .lowercase()
        return "follow-up" in text &&
            "due" in text &&
            "outcome unknown" in text
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

    fun risingBloodPressureSignal(
        patient: Patient,
        visits: List<VisitLog>
    ): HistorySignal? {
        val readings = visits
            .filter { it.patientId == patient.id }
            .sortedBy { it.visitDateMillis }
            .mapNotNull { visit -> parseBloodPressure(visit) }
            .takeLast(4)

        if (readings.size < 3) return null
        val systolicValues = readings.map { it.systolic }
        val isMonotonic = systolicValues.zipWithNext().all { (previous, next) -> next >= previous }
        val totalIncrease = systolicValues.last() - systolicValues.first()
        val clearlyIncreasing = isMonotonic && totalIncrease >= 12 && systolicValues.last() >= 130
        if (!clearlyIncreasing) return null

        return HistorySignal(
            patientId = patient.id,
            title = "History signal",
            message = "History signal: BP readings have increased across recent visits. Review and monitor per ANC protocol.",
            readings = readings
        )
    }

    fun parseBloodPressure(visit: VisitLog): BloodPressureReading? {
        val text = listOf(
            visit.observationText,
            visit.structuredNote,
            visit.suggestedFollowUp
        ).joinToString(separator = " ")
        val match = bloodPressureRegex.find(text) ?: return null
        val systolic = match.groupValues[1].toIntOrNull() ?: return null
        val diastolic = match.groupValues[2].toIntOrNull() ?: return null
        if (systolic !in 70..240 || diastolic !in 40..160) return null
        return BloodPressureReading(
            systolic = systolic,
            diastolic = diastolic,
            visitDateMillis = visit.visitDateMillis
        )
    }
}
