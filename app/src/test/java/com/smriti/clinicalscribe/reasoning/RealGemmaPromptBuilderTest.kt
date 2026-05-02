package com.smriti.clinicalscribe.reasoning

import com.smriti.clinicalscribe.data.DemoSeedData
import com.smriti.clinicalscribe.rag.ProtocolChunk
import org.junit.Assert.assertFalse
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
        assertTrue(prompt.contains("Meena Sharma, 28F"))
        assertTrue(prompt.contains("Prior visit history"))
        assertTrue(prompt.contains(history.maxBy { it.visitDateMillis }.structuredNote.take(40)))
        assertTrue(prompt.contains("Meena reports severe headache and blurred vision."))
        assertTrue(prompt.contains(protocol.citation))
        assertTrue(prompt.contains("ALLOWED CITATIONS:"))
        assertTrue(prompt.contains("Use only citation IDs from the supplied protocol chunks"))
        assertTrue(prompt.contains("If referralFlag is false and no protocol-specific recommendation is needed, citations may be empty"))
        assertTrue(prompt.contains("Routine follow-up wording is allowed for no-danger-sign cases"))
        assertTrue(prompt.contains("This is not a diagnosis"))
        assertTrue(prompt.contains("CHW confirmation is required"))
        assertTrue(prompt.contains("Generate all user-facing output in Hindi"))
        assertTrue(prompt.contains("Use plain, non-technical language for a community health worker"))
        assertTrue(prompt.contains("Do not diagnose. If uncertain, ask for clarification. Safety wording must appear in Hindi"))
        assertTrue(prompt.contains("Safety wording must appear in Hindi"))
        assertTrue(prompt.contains("Protocol citation IDs may remain in English"))
        assertTrue(prompt.contains("If uncertain, ask for clarification."))
        assertTrue(prompt.contains("preferred output language: Hindi (hi)"))
        assertTrue(prompt.contains("Return exact JSON only"))
        assertTrue(prompt.contains("Output exactly one JSON object and nothing else"))
        assertTrue(prompt.contains("The first character must be { and the last character must be }"))
        assertTrue(prompt.contains("Do not wrap in ```json"))
        assertTrue(prompt.contains("\"referralFlag\":true|false"))
        assertTrue(prompt.contains("\"citations\":[\"exact supplied citation\"]"))
        assertTrue(prompt.contains("referralFlag must always be present as a boolean"))
        assertTrue(prompt.contains("citations must always be an array"))
        assertTrue(prompt.contains("confidence must be exactly HIGH, MEDIUM, or LOW"))
        assertTrue(prompt.contains("safetyNote must include the required non-diagnostic"))
        assertTrue(prompt.contains("put the single most urgent or primary citation first"))
        assertTrue(prompt.contains("Do not join citations with semicolons"))
        assertTrue(prompt.contains("For routine visits with no danger signs, set referralFlag=false, dangerSigns=[], and provide a brief routine follow-up plan"))
        assertTrue(prompt.contains("Example valid output for this Meena danger-sign case"))
    }

    @Test
    fun promptUsesPatientPreferredLanguageForSpanishAndSwahili() {
        val lucia = DemoSeedData.patients.first { it.id == "patient-lucia" }
        val grace = DemoSeedData.patients.first { it.id == "patient-grace" }

        val spanishPrompt = RealGemmaPromptBuilder().buildVisitReasoningPrompt(
            patient = lucia,
            visitHistory = emptyList(),
            observationText = "Routine ANC visit with headache check.",
            protocolChunks = listOf(protocolChunk())
        )
        val swahiliPrompt = RealGemmaPromptBuilder().buildVisitReasoningPrompt(
            patient = grace,
            visitHistory = emptyList(),
            observationText = "Routine ANC visit with headache check.",
            protocolChunks = listOf(protocolChunk())
        )

        assertTrue(spanishPrompt.contains("Generate all user-facing output in Spanish"))
        assertTrue(spanishPrompt.contains("Safety wording must appear in Spanish"))
        assertTrue(spanishPrompt.contains("preferred output language: Spanish (es)"))
        assertTrue(swahiliPrompt.contains("Generate all user-facing output in Swahili"))
        assertTrue(swahiliPrompt.contains("Safety wording must appear in Swahili"))
        assertTrue(swahiliPrompt.contains("preferred output language: Swahili (sw)"))
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
        assertTrue(prompt.contains("Set citations to []"))
        assertTrue(prompt.contains("Set referralFlag to false"))
        assertTrue(prompt.contains("Set confidence to LOW"))
        assertTrue(prompt.contains("Do not write \"No matching protocol citation\" anywhere"))
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

    @Test
    fun defaultPromptLimitsPriorVisitsAndProtocolChunksForLatency() {
        val patient = DemoSeedData.patients.first { it.id == "patient-meena" }
        val history = (1..5).map { index ->
            DemoSeedData.initialVisitLogs(nowMillis = 1_700_000_000_000L)
                .first()
                .copy(
                    visitDateMillis = 1_700_000_000_000L - index,
                    structuredNote = "Latency limited visit $index summary."
                )
        }
        val chunks = (1..4).map { index ->
            protocolChunk().copy(
                id = "chunk-$index",
                title = "Protocol $index",
                text = "Protocol guidance $index",
                section = "Section $index"
            )
        }

        val prompt = RealGemmaPromptBuilder().buildVisitReasoningPrompt(
            patient = patient,
            visitHistory = history,
            observationText = "Meena reports severe headache and blurred vision.",
            protocolChunks = chunks
        )

        assertTrue(prompt.contains("Latency limited visit 1 summary."))
        assertTrue(prompt.contains("Latency limited visit 3 summary."))
        assertFalse(prompt.contains("Latency limited visit 4 summary."))
        assertTrue(prompt.contains("id=chunk-1"))
        assertTrue(prompt.contains("id=chunk-2"))
        assertFalse(prompt.contains("id=chunk-3"))
        assertTrue(prompt.contains("\"referralFlag\":true|false"))
        assertTrue(prompt.contains("\"safetyNote\""))
        assertTrue(prompt.contains("ALLOWED CITATIONS:"))
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
