package com.smriti.clinicalscribe.reasoning

import com.smriti.clinicalscribe.data.Patient
import com.smriti.clinicalscribe.data.ReferralFlag
import com.smriti.clinicalscribe.data.VisitLog

object SupervisorSummaryFormatter {
    fun buildLocalSavedSummary(
        patients: List<Patient>,
        visits: List<VisitLog>,
        referrals: List<ReferralFlag>,
        narrative: String = "Saved visits, patient history, and local health guidance on this device."
    ): SupervisorSummary {
        val patientNamesById = patients.associate { it.id to it.name }
        val paperUrgent = visits
            .filter { PaperNoteUrgencyClassifier.needsUrgentReview(it) }
            .sortedByDescending { it.visitDateMillis }
        val paperUrgentVisitIds = paperUrgent.map { it.id }.toSet()
        val followUpsDue = visits
            .filter { it.confirmed && it.suggestedFollowUp.isNotBlank() && it.id !in paperUrgentVisitIds }
            .map { visit ->
                val patientName = patientNamesById[visit.patientId] ?: visit.patientId
                "$patientName: ${visit.suggestedFollowUp}"
            }
        val paperLines = paperUrgent.map { visit ->
            val name = patientNamesById[visit.patientId] ?: visit.patientId
            paperScanUrgentReviewCardBody(displayName = name, visit = visit)
        }

        return SupervisorSummary(
            totalVisits = visits.count { it.confirmed },
            referralsFlagged = referrals.size,
            urgentCases = urgentCases(patients, referrals),
            followUpsDue = followUpsDue,
            narrative = narrative,
            paperScanNeedsUrgentReview = paperLines
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

    private const val MAX_FALLBACK_DANGER_SIGNS_LENGTH = 90
}
