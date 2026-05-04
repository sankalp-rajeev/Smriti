package com.smriti.clinicalscribe.reasoning

import com.smriti.clinicalscribe.data.Patient
import com.smriti.clinicalscribe.data.PatientLanguages
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
        val patientLanguage = PatientLanguages.forPatient(patient)
        val safetyWording = patientLanguage.safetyWording

        if (!hasClinicalDetail) {
            return VisitReasoningResult(
                patientId = patient.id,
                observationText = observationText,
                structuredNote = "Observation is too brief to create a safe structured record. $safetyWording",
                referralFlag = null,
                protocolCitation = protocolCitation,
                suggestedFollowUp = "Ask the CHW to repeat the observation with symptoms, vitals, and fetal movement if relevant. Health guidance: $protocolCitation.",
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
                    append("\n\nLocal guidance support:\nNo matching health guidance was found on this device. $safetyWording")
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
            append("\n\nLocal guidance support:\nDocumentation support only. $safetyWording ")
            if (hasDangerSign) {
                append("Danger signs are present in the observation and require CHW review for referral. Health guidance: $protocolCitation.")
            } else {
                append("No urgent danger signs were flagged from this note. Health guidance: $protocolCitation.")
            }
        }

        val referral = if (hasDangerSign) {
            ReferralFlag(
                patientId = patient.id,
                urgency = protocolChunks.highestReferralLevel(),
                reason = "Local health guidance checked; not a diagnosis: ${protocol!!.text}",
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
                "Contact supervisor and support same-day referral confirmation. Health guidance: $protocolCitation."
            } else {
                "Continue routine ANC follow-up and confirm next visit date. Health guidance: $protocolCitation."
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
        return SupervisorSummaryFormatter.buildLocalSavedSummary(
            patients = patients,
            visits = visits,
            referrals = referrals
        ).copy(
            narrative =
                "Today has ${visits.count { it.confirmed }} confirmed visit(s), with ${referrals.size} referral flag(s). " +
                    "Review urgent cases first, then follow routine ANC follow-ups."
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
