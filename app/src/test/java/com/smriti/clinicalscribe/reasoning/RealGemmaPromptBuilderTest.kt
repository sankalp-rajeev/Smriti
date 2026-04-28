package com.smriti.clinicalscribe.reasoning

import com.smriti.clinicalscribe.data.DemoSeedData
import com.smriti.clinicalscribe.rag.ProtocolChunk
import org.junit.Assert.assertTrue
import org.junit.Test

class RealGemmaPromptBuilderTest {
    @Test
    fun promptIncludesPatientHistoryObservationAndProtocolContext() {
        val patient = DemoSeedData.patients.first { it.id == "patient-meena" }
        val history = DemoSeedData.initialVisitLogs(nowMillis = 1_700_000_000_000L)
        val protocol = protocolChunk()

        val prompt = RealGemmaPromptBuilder().buildVisitReasoningPrompt(
            patient = patient,
            visitHistory = history,
            observationText = "Meena reports severe headache and blurred vision.",
            protocolChunks = listOf(protocol)
        )

        assertTrue(prompt.contains("patient-meena"))
        assertTrue(prompt.contains("Meena, 28F"))
        assertTrue(prompt.contains("Prior visit history"))
        assertTrue(prompt.contains(history.first().structuredNote.take(40)))
        assertTrue(prompt.contains("Meena reports severe headache and blurred vision."))
        assertTrue(prompt.contains(protocol.citation))
        assertTrue(prompt.contains("This is not a diagnosis"))
        assertTrue(prompt.contains("CHW confirmation is required"))
        assertTrue(prompt.contains("Return compact JSON only"))
        assertTrue(prompt.contains("Output exactly one JSON object and nothing else"))
        assertTrue(prompt.contains("The first character must be { and the last character must be }"))
        assertTrue(prompt.contains("protocolCitation must be exactly one supplied citation string"))
        assertTrue(prompt.contains("choose the single most urgent or primary citation"))
        assertTrue(prompt.contains("Do not join citations with semicolons"))
        assertTrue(prompt.contains("same exact supplied citation as protocolCitation"))
    }

    @Test
    fun promptExplainsEmptyCitationContractWhenNoProtocolChunksSupplied() {
        val patient = DemoSeedData.patients.first { it.id == "patient-meena" }
        val history = DemoSeedData.initialVisitLogs(nowMillis = 1_700_000_000_000L)

        val prompt = RealGemmaPromptBuilder().buildVisitReasoningPrompt(
            patient = patient,
            visitHistory = history,
            observationText = "Meena feels unwell but vitals are missing.",
            protocolChunks = emptyList()
        )

        assertTrue(prompt.contains("No protocol chunk was supplied"))
        assertTrue(prompt.contains("Set protocolCitation to \"\""))
        assertTrue(prompt.contains("Set uncertain to true"))
        assertTrue(prompt.contains("Set referralFlag to null"))
        assertTrue(prompt.contains("Do not write \"No matching protocol citation\""))
    }

    @Test
    fun promptCanIncludeExpandedHistoryForManualMemoryStress() {
        val patient = DemoSeedData.patients.first { it.id == "patient-meena" }
        val history = (1..8).map { index ->
            DemoSeedData.initialVisitLogs(nowMillis = 1_700_000_000_000L)
                .first()
                .copy(
                    visitDateMillis = 1_700_000_000_000L - index,
                    structuredNote = "Compressed visit $index summary."
                )
        }

        val prompt = RealGemmaPromptBuilder(
            maxHistoryVisits = 8,
            historyFormatter = RealGemmaHistoryFormatter.Default
        ).buildVisitReasoningPrompt(
            patient = patient,
            visitHistory = history,
            observationText = "Meena reports severe headache and blurred vision.",
            protocolChunks = listOf(protocolChunk())
        )

        assertTrue(prompt.contains("Compressed visit 1 summary."))
        assertTrue(prompt.contains("Compressed visit 8 summary."))
    }

    @Test
    fun compactHistoryFormatterUsesNumberedSingleLineEntries() {
        val patient = DemoSeedData.patients.first { it.id == "patient-meena" }
        val history = (1..3).map { index ->
            DemoSeedData.initialVisitLogs(nowMillis = 1_700_000_000_000L)
                .first()
                .copy(
                    visitDateMillis = 1_700_000_000_000L - index,
                    observationText = "Visit $index observation with quoted \"text\" and extra   spaces.",
                    suggestedFollowUp = "Follow routine ANC schedule and review danger signs.",
                    protocolCitation = "WHO ANC Contact schedule"
                )
        }

        val prompt = RealGemmaPromptBuilder(
            maxHistoryVisits = 3,
            historyFormatter = RealGemmaHistoryFormatter.Compact
        ).buildVisitReasoningPrompt(
            patient = patient,
            visitHistory = history,
            observationText = "Meena reports severe headache and blurred vision.",
            protocolChunks = listOf(protocolChunk())
        )

        assertTrue(prompt.contains("V01: date="))
        assertTrue(prompt.contains("V03: date="))
        assertTrue(prompt.contains("issue=Visit"))
        assertTrue(prompt.contains("citation=WHO ANC Contact schedule"))
    }

    private fun protocolChunk() = ProtocolChunk(
        id = "danger-headache",
        title = "Maternal danger signs",
        source = "Smriti Demo Maternal Health Protocol",
        section = "Danger Signs",
        text = "Severe headache with blurred vision requires same-day referral support.",
        keywords = "headache, blurred vision",
        referralLevel = "SAME_DAY"
    )
}
