package com.smriti.clinicalscribe.reasoning

import com.smriti.clinicalscribe.data.PatientLanguages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RealGemmaSafetyPostProcessorTest {
    private val processor = RealGemmaSafetyPostProcessor()

    @Test
    fun appendsEnglishSafetyWording() {
        val result = processor.enforce(baseResult("Protocol-grounded note."), "en")

        assertTrue(result.structuredNote.contains(PatientLanguages.English.safetyWording))
    }

    @Test
    fun appendsHindiSafetyWording() {
        val result = processor.enforce(baseResult("प्रोटोकॉल आधारित नोट।"), "hi")

        assertTrue(result.structuredNote.contains(PatientLanguages.Hindi.safetyWording))
    }

    @Test
    fun appendsSpanishSafetyWording() {
        val result = processor.enforce(baseResult("Nota basada en protocolo."), "es")

        assertTrue(result.structuredNote.contains(PatientLanguages.Spanish.safetyWording))
    }

    @Test
    fun appendsSwahiliSafetyWording() {
        val result = processor.enforce(baseResult("Dokezo linalotegemea itifaki."), "sw")

        assertTrue(result.structuredNote.contains(PatientLanguages.Swahili.safetyWording))
    }

    @Test
    fun existingLanguageSafetyIsNotDuplicated() {
        val safeNote = "Nota. ${PatientLanguages.Spanish.safetyWording}"

        val result = processor.enforce(baseResult(safeNote), "es")

        assertEquals(safeNote, result.structuredNote)
    }

    private fun baseResult(note: String): VisitReasoningResult {
        return VisitReasoningResult(
            patientId = "patient-test",
            observationText = "Observation",
            structuredNote = note,
            referralFlag = null,
            protocolCitation = "Protocol",
            suggestedFollowUp = "Follow-up",
            protocolChunk = null,
            uncertain = false,
            clarificationPrompt = null
        )
    }
}
