package com.smriti.clinicalscribe.reasoning

import com.smriti.clinicalscribe.data.Patient
import com.smriti.clinicalscribe.data.ReferralFlag
import com.smriti.clinicalscribe.data.VisitLog
import com.smriti.clinicalscribe.rag.ProtocolChunk

class MockGemmaAgent : GemmaAgent {
    override suspend fun generateVisitNote(
        patient: Patient,
        visitHistory: List<VisitLog>,
        observationText: String,
        protocolChunks: List<ProtocolChunk>
    ): VisitReasoningResult {
        val normalized = observationText.lowercase()
        val protocol = protocolChunks.firstOrNull()
        val hasDangerSign = listOf(
            "headache",
            "blurred vision",
            "bp 150",
            "150 over 95",
            "convulsion",
            "bleeding",
            "reduced fetal",
            "reduced movement"
        ).any { normalized.contains(it) }
        val hasClinicalDetail = observationText.trim().length >= 12
        val latestHistory = visitHistory.firstOrNull()?.structuredNote ?: "No prior visit history available."
        val protocolCitation = protocolChunks.citationSummary()

        if (!hasClinicalDetail) {
            return VisitReasoningResult(
                patientId = patient.id,
                observationText = observationText,
                structuredNote = "Observation is too brief to create a safe structured record.",
                referralFlag = null,
                protocolCitation = protocolCitation,
                suggestedFollowUp = "Ask the CHW to repeat the observation with symptoms, vitals, and fetal movement if relevant. Protocol citation: $protocolCitation.",
                protocolChunk = protocol,
                uncertain = true,
                clarificationPrompt = "Please confirm symptoms, blood pressure if measured, fetal movement, and any bleeding or convulsions."
            )
        }

        if (protocolChunks.isEmpty()) {
            return VisitReasoningResult(
                patientId = patient.id,
                observationText = observationText,
                structuredNote = buildString {
                    append("Observation:\n${observationText.trim()}")
                    append("\n\nRelevant history:\n$latestHistory")
                    append("\n\nProtocol-grounded support:\nNo matching protocol citation was found in the offline corpus. This is not a diagnosis. CHW confirmation required before saving or acting.")
                },
                referralFlag = null,
                protocolCitation = protocolCitation,
                suggestedFollowUp = "No matching protocol citation found. Ask the CHW to confirm details or consult a supervisor.",
                protocolChunk = null,
                uncertain = true,
                clarificationPrompt = "No matching local protocol was found. Please confirm the observation and consult a supervisor if there are danger signs."
            )
        }

        val note = buildString {
            append("Observation:\n${observationText.trim()}")
            append("\n\nRelevant history:\n$latestHistory")
            append("\n\nProtocol-grounded support:\nDocumentation support only; not a diagnosis. CHW confirmation required. ")
            if (hasDangerSign) {
                append("Danger signs are present in the observation and require CHW review for referral. Protocol citation: $protocolCitation.")
            } else {
                append("No obvious danger sign keyword was detected in this mock pass. Protocol citation: $protocolCitation.")
            }
        }

        val referral = if (hasDangerSign) {
            ReferralFlag(
                patientId = patient.id,
                urgency = protocolChunks.highestReferralLevel(),
                reason = "Protocol-grounded referral suggestion only, not a diagnosis: ${protocol!!.text}",
                protocolBasis = protocolCitation,
                recommendedFacility = "Nearest PHC or obstetric referral facility",
                dangerSigns = extractDangerSigns(normalized),
                createdAtMillis = System.currentTimeMillis()
            )
        } else {
            null
        }

        return VisitReasoningResult(
            patientId = patient.id,
            observationText = observationText,
            structuredNote = note,
            referralFlag = referral,
            protocolCitation = protocolCitation,
            suggestedFollowUp = if (hasDangerSign) {
                "Contact supervisor and support same-day referral confirmation. Protocol citation: $protocolCitation."
            } else {
                "Continue routine ANC follow-up and confirm next visit date. Protocol citation: $protocolCitation."
            },
            protocolChunk = protocol,
            uncertain = false,
            clarificationPrompt = null
        )
    }

    override suspend fun generateSupervisorSummary(
        patients: List<Patient>,
        visits: List<VisitLog>,
        referrals: List<ReferralFlag>
    ): SupervisorSummary {
        val patientNamesById = patients.associate { it.id to it.name }
        val urgentCases = SupervisorSummaryFormatter.urgentCases(patients, referrals)
        val followUps = visits
            .filter { it.suggestedFollowUp.isNotBlank() }
            .map { visit ->
                val citationText = if (visit.suggestedFollowUp.contains("citation", ignoreCase = true)) {
                    ""
                } else {
                    " Citation: ${visit.protocolCitation}."
                }
                "${patientNamesById[visit.patientId] ?: visit.patientId}: ${visit.suggestedFollowUp}$citationText"
            }

        return SupervisorSummary(
            totalVisits = visits.size,
            referralsFlagged = referrals.size,
            urgentCases = urgentCases,
            followUpsDue = followUps,
            narrative = "Today has ${visits.size} confirmed visit(s), with ${referrals.size} referral flag(s). Review urgent cases first, then follow routine ANC follow-ups."
        )
    }

    private fun extractDangerSigns(normalizedObservation: String): String {
        val signs = mutableListOf<String>()
        if (normalizedObservation.contains("headache")) signs += "headache"
        if (normalizedObservation.contains("blurred vision")) signs += "blurred vision"
        if (normalizedObservation.contains("bp 150") || normalizedObservation.contains("150 over 95")) {
            signs += "high blood pressure"
        }
        if (normalizedObservation.contains("convulsion")) signs += "convulsions"
        if (normalizedObservation.contains("bleeding")) signs += "bleeding"
        if (normalizedObservation.contains("reduced fetal") || normalizedObservation.contains("reduced movement")) {
            signs += "reduced fetal movement"
        }
        return signs.joinToString()
    }

    private fun List<ProtocolChunk>.citationSummary(): String {
        return if (isEmpty()) {
            "No matching protocol citation"
        } else {
            take(3).joinToString(separator = "; ") { it.citation }
        }
    }

    private fun List<ProtocolChunk>.highestReferralLevel(): String {
        val levels = map { it.referralLevel.uppercase() }
        return when {
            "IMMEDIATE" in levels -> "IMMEDIATE"
            "SAME_DAY" in levels -> "SAME_DAY"
            "WITHIN_24H" in levels -> "WITHIN_24H"
            else -> firstOrNull()?.referralLevel?.uppercase().orEmpty().ifBlank { "CHW_CONFIRM" }
        }
    }
}
