package com.smriti.clinicalscribe.data

import com.smriti.clinicalscribe.transcript.TranscriptResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PatientRegistrationDraftTest {
    @Test
    fun manualRegistrationDraftCreatesLocalPatient() {
        val draft = PatientRegistrationDraft(
            name = "Sita Kumari",
            age = "25",
            pregnancyWeeks = "18",
            village = "Rampur",
            countryCode = "IN",
            preferredLanguage = "hi",
            notes = "Synthetic demo registration."
        )

        val result = draft.toPatient(idProvider = { "patient-test-sita" })

        val patient = (result as PatientRegistrationResult.Valid).patient
        assertEquals("patient-test-sita", patient.id)
        assertEquals("Sita Kumari", patient.name)
        assertEquals(25, patient.age)
        assertEquals(18, patient.pregnancyWeeks)
        assertEquals("IN", patient.countryCode)
        assertEquals("hi", patient.preferredLanguage)
        assertEquals("INDIA", patient.protocolRegion)
    }

    @Test
    fun unavailableVoiceRegistrationKeepsEditableManualFallback() {
        val draft = PatientRegistrationDraft(name = "Manual Name")

        val update = draft.applySpeechResult(
            field = PatientRegistrationField.NAME,
            result = TranscriptResult.Unavailable("Offline speech language pack unavailable.")
        )

        assertEquals(draft, update.draft)
        assertTrue(update.canUseManualFallback)
        assertTrue(update.message.contains("Offline speech unavailable"))
    }

    @Test
    fun successfulVoiceRegistrationFillsOnlyRequestedField() {
        val draft = PatientRegistrationDraft(name = "Manual Name", age = "")

        val update = draft.applySpeechResult(
            field = PatientRegistrationField.AGE,
            result = TranscriptResult.Success("24 years")
        )

        assertEquals("Manual Name", update.draft.name)
        assertEquals("24 years", update.draft.age)
        assertTrue(update.canUseManualFallback)
    }
}
