package com.smriti.clinicalscribe.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PatientLeaveBehindMessageGeneratorTest {
    @Test
    fun englishUrgentReferralMessageIsPlainAndNonDiagnostic() {
        val patient = DemoSeedData.patients.first { it.id == "patient-fatima" }
        val visit = visit(
            patientId = patient.id,
            observation = "Headache, blurred vision, and high blood pressure BP 150/95 were recorded.",
            followUp = "Urgent review advised."
        )
        val message = PatientLeaveBehindMessageGenerator.generate(
            patient = patient,
            visit = visit,
            referral = referral(patientId = patient.id, visitLogId = visit.id)
        )

        assertTrue(message.contains("Fatima"))
        assertTrue(message.contains("headache"))
        assertTrue(message.contains("blurred vision"))
        assertTrue(message.contains("high blood pressure"))
        assertTrue(message.contains("Please seek review as advised by your health worker."))
        assertTrue(message.contains("This is not a diagnosis."))
        assertNoDiagnosisOrTreatmentClaims(message)
    }

    @Test
    fun hindiMessageUsesHindiSafetyWordingForMeena() {
        val patient = DemoSeedData.patients.first { it.id == "patient-meena" }
        val visit = visit(
            patientId = patient.id,
            observation = "सिरदर्द, धुंधला दिखना, BP 150/95",
            followUp = "स्वास्थ्य केंद्र में समीक्षा"
        )
        val message = PatientLeaveBehindMessageGenerator.generate(
            patient = patient,
            visit = visit,
            referral = referral(patientId = patient.id, visitLogId = visit.id)
        )

        assertTrue(message.contains("Meena"))
        assertTrue(message.contains("सिरदर्द"))
        assertTrue(message.contains("धुंधला दिखना"))
        assertTrue(message.contains("यह निदान नहीं है"))
        assertNoDiagnosisOrTreatmentClaims(message)
    }

    @Test
    fun spanishRoutineMessageForLuciaDoesNotCreateReferralLanguage() {
        val patient = DemoSeedData.patients.first { it.id == "patient-lucia" }
        val message = PatientLeaveBehindMessageGenerator.generate(
            patient = patient,
            visit = visit(patientId = patient.id, observation = "Routine visit recorded. No danger signs."),
            referral = null
        )

        assertTrue(message.contains("Lucia"))
        assertTrue(message.contains("Esto no es un diagnóstico."))
        assertFalse(message.lowercase().contains("urgente"))
        assertFalse(message.lowercase().contains("derivación"))
        assertNoDiagnosisOrTreatmentClaims(message)
    }

    @Test
    fun swahiliRoutineMessageForGraceDoesNotCreateReferralLanguage() {
        val patient = DemoSeedData.patients.first { it.id == "patient-grace" }
        val message = PatientLeaveBehindMessageGenerator.generate(
            patient = patient,
            visit = visit(patientId = patient.id, observation = "Routine visit recorded. No danger signs."),
            referral = null
        )

        assertTrue(message.contains("Grace"))
        assertTrue(message.contains("Hii si utambuzi wa ugonjwa."))
        assertFalse(message.lowercase().contains("dharura"))
        assertFalse(message.lowercase().contains("rufaa"))
        assertNoDiagnosisOrTreatmentClaims(message)
    }

    @Test
    fun fallbackMessageUsesSimpleEnglishWhenLanguageIsUnsupported() {
        val patient = DemoSeedData.patients.first()
            .copy(id = "patient-unsupported-language", name = "Ana Rivera", preferredLanguage = "pt")
        val message = PatientLeaveBehindMessageGenerator.generate(
            patient = patient,
            visit = visit(patientId = patient.id, observation = "Routine visit recorded."),
            referral = null
        )

        assertTrue(message.contains("Ana"))
        assertTrue(message.contains("This is not a diagnosis."))
        assertNoDiagnosisOrTreatmentClaims(message)
    }

    private fun visit(
        patientId: String,
        observation: String,
        followUp: String = "Check again with health worker."
    ): VisitLog {
        return VisitLog(
            id = 45,
            patientId = patientId,
            visitDateMillis = 1_800_000_000_000L,
            observationText = observation,
            structuredNote = observation,
            protocolCitation = "WHO ANC Recommendation B1.2",
            suggestedFollowUp = followUp,
            confirmed = true
        )
    }

    private fun referral(patientId: String, visitLogId: Long): ReferralFlag {
        return ReferralFlag(
            visitLogId = visitLogId,
            patientId = patientId,
            urgency = "IMMEDIATE",
            reason = "Danger signs need review.",
            protocolBasis = "WHO ANC Recommendation B1.2",
            recommendedFacility = "Nearest health facility",
            dangerSigns = "headache, blurred vision, high blood pressure",
            createdAtMillis = 1_800_000_000_000L
        )
    }

    private fun assertNoDiagnosisOrTreatmentClaims(message: String) {
        val lower = message.lowercase()
        assertFalse(lower.contains("preeclampsia"))
        assertFalse(lower.contains("diagnosed with"))
        assertFalse(lower.contains("you have "))
        assertFalse(lower.contains("take medicine"))
        assertFalse(lower.contains("tablet"))
        assertFalse(lower.contains("dose"))
        assertFalse(lower.contains("dosage"))
        assertFalse(lower.contains("treatment"))
    }
}
