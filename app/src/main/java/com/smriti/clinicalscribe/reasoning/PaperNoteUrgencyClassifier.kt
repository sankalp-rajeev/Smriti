package com.smriti.clinicalscribe.reasoning

import com.smriti.clinicalscribe.data.TranscriptSource
import com.smriti.clinicalscribe.data.VisitLog

/**
 * Deterministic urgency hints for visits saved from scanned paper notes only (data-entry support).
 * Not clinical reasoning; avoids diagnosis/treatment wording.
 */
object PaperNoteUrgencyClassifier {

    private val bloodPressureRegex = Regex("""(\d{2,3})\s*/\s*(\d{2,3})""")

    fun needsUrgentReview(visit: VisitLog): Boolean {
        if (!visit.confirmed || visit.transcriptSource != TranscriptSource.PAPER_SCAN) return false
        return evaluate(joinedSavedText(visit))
    }

    fun issueSummaryPhrase(visit: VisitLog): String {
        val combined = joinedSavedText(visit)
        val n = combined.lowercase()
        val hp = firstSevereBp(combined)
        val headache = headachePositive(n)
        val visual = visualPositive(n)

        return when {
            hp != null && headache && visual -> "BP ${hp.first}/${hp.second} with headache and blurred vision"
            hp != null && headache -> "BP ${hp.first}/${hp.second} with headache"
            hp != null && visual -> "BP ${hp.first}/${hp.second} with blurred vision"
            hp != null -> "BP ${hp.first}/${hp.second}"
            headache && visual -> "Headache with blurred vision on scanned note"
            else -> "Urgent indicators on scanned note"
        }
    }

    internal fun evaluate(combinedSavedText: String): Boolean {
        val n = combinedSavedText.lowercase()
        val hp = firstSevereBp(combinedSavedText) != null
        return hp || (headachePositive(n) && visualPositive(n))
    }

    internal fun joinedSavedText(visit: VisitLog): String {
        return listOf(visit.observationText, visit.structuredNote, visit.suggestedFollowUp)
            .joinToString("\n")
    }

    private fun firstSevereBp(text: String): Pair<String, String>? {
        bloodPressureRegex.findAll(text).forEach { match ->
            val systolic = match.groupValues[1].toIntOrNull() ?: return@forEach
            val diastolic = match.groupValues[2].toIntOrNull() ?: return@forEach
            if (systolic >= SYSTOLIC_URGENCY_THRESHOLD || diastolic >= DIASTOLIC_URGENCY_THRESHOLD) {
                return match.groupValues[1] to match.groupValues[2]
            }
        }
        return null
    }

    private fun headachePositive(n: String): Boolean {
        if (n.contains(SW_HEADACHE)) return true
        if (n.contains(SEVERE_HEADACHE)) return true
        if (NEGATED_HEADACHE.any { n.contains(it) }) return false
        return Regex("""\bheadache\b""", RegexOption.IGNORE_CASE).containsMatchIn(n)
    }

    private fun visualPositive(n: String): Boolean {
        if (n.contains(SW_VISUAL_FUZZ)) return true
        if (n.contains(SW_EYE_ISSUE)) return true
        if (NEGATED_BLUR.any { n.contains(it) }) return false
        return n.contains(VISUAL_SYMPTOMS) ||
            Regex("""\bblurred\s+vision\b""", RegexOption.IGNORE_CASE).containsMatchIn(n) ||
            Regex("""\bvisual\s+symptoms?\b""", RegexOption.IGNORE_CASE).containsMatchIn(n)
    }

    private const val SYSTOLIC_URGENCY_THRESHOLD = 160
    private const val DIASTOLIC_URGENCY_THRESHOLD = 110
    private const val SW_HEADACHE = "maumivu ya kichwa"
    private const val SEVERE_HEADACHE = "severe headache"
    private const val SW_VISUAL_FUZZ = "kuona ukungu"
    private const val SW_EYE_ISSUE = "matatizo ya macho"
    private const val VISUAL_SYMPTOMS = "visual symptom"

    private val NEGATED_HEADACHE = listOf("no headache", "without headache", "denies headache")
    private val NEGATED_BLUR = listOf("no blurred", "without blurred", "denies blurred")
}
