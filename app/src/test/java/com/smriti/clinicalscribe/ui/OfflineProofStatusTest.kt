package com.smriti.clinicalscribe.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineProofStatusTest {
    @Test
    fun proofLinesStayConciseAndJudgeFacing() {
        val status = OfflineProofStatus(
            reasoningModeLabel = "RealGemmaAgent",
            realGemmaModelStatusLabel = "Not found",
            realGemmaReadinessLabel = "RealGemma setup required",
            realGemmaInferenceLabel = "Unavailable/setup required",
            realGemmaTextModeLabel = "Setup required",
            realGemmaSubmissionModeLabel = "Required; build flag missing"
        )

        assertEquals(
            listOf(
                "Network required" to "No",
                "Patient data" to "Local Room/SQLite",
                "Protocol source" to "Local JSON; country-aware retrieval",
                "Active reasoning mode" to "RealGemmaAgent",
                "Cloud API" to "No",
                "RealGemma text mode" to "Setup required",
                "Submission mode" to "Required; build flag missing",
                "Real Gemma model" to "Not found",
                "Engine" to "Loads on demand",
                "Inference" to "Unavailable/setup required",
                "Direct Gemma audio" to "Blocked by current public LiteRT-LM Android/Kotlin path; using offline speech/transcript fallback"
            ),
            status.lines
        )
    }

    @Test
    fun compactProofLinesKeepRosterSafetyStatusShort() {
        val status = OfflineProofStatus(
            reasoningModeLabel = "RealGemmaAgent",
            realGemmaModelStatusLabel = "Not found",
            realGemmaReadinessLabel = "RealGemma setup required",
            realGemmaTextModeLabel = "Setup required"
        )

        assertEquals(
            listOf(
                "Network" to "No",
                "Patient data" to "Local Room/SQLite",
                "Protocols" to "Local JSON, country-aware",
                "Active mode" to "RealGemmaAgent",
                "RealGemma text" to "Setup required",
                "Direct Gemma audio" to "Blocked; transcript fallback"
            ),
            status.compactLines
        )
    }

    @Test
    fun developerWarningAppearsOnlyWhenProvided() {
        val status = OfflineProofStatus(
            reasoningModeLabel = "RealGemmaAgent / Developer-only / Experimental",
            realGemmaModelStatusLabel = "Found",
            realGemmaReadinessLabel = "Model found, engine disabled",
            realGemmaEngineStatusLabel = "Loads on demand",
            realGemmaInferenceLabel = "Enabled for developer text mode; CPU backend",
            realGemmaGateLabel = "Build gate: enabled; local gate: enabled",
            realGemmaDeveloperWarning = "Developer-only RealGemma text mode. Not default demo mode. Output must be reviewed before saving."
        )

        assertEquals(
            "Developer warning" to "Developer-only RealGemma text mode. Not default demo mode. Output must be reviewed before saving.",
            status.lines.last()
        )
        assertEquals("Real Gemma model" to "Found", status.lines[7])
        assertEquals("Engine" to "Loads on demand", status.lines[8])
    }

    @Test
    fun successfulGenerationCanShowLoadedEngineState() {
        val status = OfflineProofStatus(
            reasoningModeLabel = "RealGemmaAgent",
            realGemmaModelStatusLabel = "Found",
            realGemmaReadinessLabel = "RealGemma text reasoning active",
            realGemmaEngineStatusLabel = "Loaded",
            realGemmaInferenceLabel = "Enabled; on-device RealGemma text reasoning"
        )

        assertEquals("Real Gemma model" to "Found", status.lines[7])
        assertEquals("Engine" to "Loaded", status.lines[8])
        assertEquals("Inference" to "Enabled; on-device RealGemma text reasoning", status.lines[9])
    }
}
