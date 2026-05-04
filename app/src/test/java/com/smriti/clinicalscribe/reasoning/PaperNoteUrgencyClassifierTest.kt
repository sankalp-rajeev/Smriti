package com.smriti.clinicalscribe.reasoning

import com.smriti.clinicalscribe.data.TranscriptSource
import com.smriti.clinicalscribe.data.VisitLog
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaperNoteUrgencyClassifierTest {
    private val confirmedPaper = TranscriptSource.PAPER_SCAN

    @Test
    fun highBpAloneTriggersForPaperScanConfirmed() {
        val visit = basePaperVisit(
            observationText = "BP: 190/110",
            structuredNote = "Blood pressure noted as 190/110 on scanned paper.",
            suggestedFollowUp = "Supervisor requested ANC revisit."
        )
        assertTrue(PaperNoteUrgencyClassifier.needsUrgentReview(visit))
        assertTrue(PaperNoteUrgencyClassifier.issueSummaryPhrase(visit).startsWith("BP 190/110"))
    }

    @Test
    fun diastolicAt110Triggers() {
        val visit = basePaperVisit(
            observationText = "BP 130/112",
            structuredNote = "Extracted readings from paper.",
            suggestedFollowUp = "Review next visit window."
        )
        assertTrue(PaperNoteUrgencyClassifier.needsUrgentReview(visit))
    }

    @Test
    fun headacheAndVisionEnglishTriggersWithoutSevereBp() {
        val visit = basePaperVisit(
            observationText = "Symptoms: severe headache\nVisual report: blurred vision",
            structuredNote = "severe headache noted; blurred vision on paper.",
            suggestedFollowUp = ""
        )
        assertTrue(PaperNoteUrgencyClassifier.needsUrgentReview(visit))
    }

    @Test
    fun swahiliHeadacheAndVisualTriggersWithoutSevereBp() {
        val visit = basePaperVisit(
            observationText = "Symptoms: maumivu ya kichwa, kuona ukungu",
            structuredNote = "Swahili paper note excerpt.",
            suggestedFollowUp = ""
        )
        assertTrue(PaperNoteUrgencyClassifier.needsUrgentReview(visit))
    }

    @Test
    fun routineGraceBp116WithoutUnsafeSymptomsIsNotUrgent() {
        val visit = basePaperVisit(
            observationText = """
                BP: 116/74
                Symptoms: no headache, no bleeding, routine visit
            """.trimIndent(),
            structuredNote = "Blood pressure: 116/74. Symptoms documented as no headache.",
            suggestedFollowUp = "routine ANC follow-up"
        )
        assertFalse(PaperNoteUrgencyClassifier.needsUrgentReview(visit))
    }

    @Test
    fun deniesHeadachePreventsDualSymptomPath() {
        val text = """
            BP: 132/82
            denies headache report
            blurred vision present
        """.trimIndent()
        assertFalse(PaperNoteUrgencyClassifier.evaluate(text))
    }

    @Test
    fun nonPaperSourceNeverUrgentRegardlessOfText() {
        val visit = VisitLog(
            id = 1L,
            patientId = "patient-grace",
            visitDateMillis = 1_800_000_000_000L,
            observationText = "BP 200/110",
            structuredNote = "High BP text",
            protocolCitation = "Simulated visit",
            suggestedFollowUp = "Follow locally",
            confirmed = true,
            transcriptSource = TranscriptSource.SIMULATED
        )
        assertFalse(PaperNoteUrgencyClassifier.needsUrgentReview(visit))
    }

    private fun basePaperVisit(
        observationText: String,
        structuredNote: String,
        suggestedFollowUp: String
    ): VisitLog {
        return VisitLog(
            id = 801L,
            patientId = "patient-grace",
            visitDateMillis = 1_800_010_000_000L,
            observationText = observationText,
            structuredNote = structuredNote,
            protocolCitation = "Paper note extraction only.",
            suggestedFollowUp = suggestedFollowUp,
            confirmed = true,
            transcriptSource = confirmedPaper
        )
    }
}
