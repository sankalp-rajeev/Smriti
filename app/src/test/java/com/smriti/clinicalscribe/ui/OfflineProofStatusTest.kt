package com.smriti.clinicalscribe.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineProofStatusTest {
    @Test
    fun proofLinesStayConciseAndJudgeFacing() {
        val status = OfflineProofStatus(
            reasoningModeLabel = "MockGemmaAgent",
            realGemmaModelStatusLabel = "Not found",
            realGemmaReadinessLabel = "Mock active"
        )

        assertEquals(
            listOf(
                "Network required" to "No",
                "Patient data" to "Local Room/SQLite",
                "Protocol source" to "Local JSON; country-aware retrieval",
                "Active reasoning mode" to "MockGemmaAgent",
                "Mode guard" to "MockGemmaAgent by default; RealGemmaAgent only when gated",
                "RealGemma text mode" to "Disabled",
                "Submission mode" to "Disabled",
                "Real Gemma model" to "Not found",
                "Inference" to "Disabled by default; manual-only",
                "Direct Gemma audio" to "Blocked by current public LiteRT-LM Android/Kotlin path; using offline speech/transcript fallback"
            ),
            status.lines
        )
    }

    @Test
    fun developerWarningAppearsOnlyWhenProvided() {
        val status = OfflineProofStatus(
            reasoningModeLabel = "RealGemmaAgent / Developer-only / Experimental",
            realGemmaModelStatusLabel = "Found, not loaded",
            realGemmaReadinessLabel = "Model found, engine disabled",
            realGemmaInferenceLabel = "Enabled for developer text mode; CPU backend",
            realGemmaGateLabel = "Build gate: enabled; local gate: enabled",
            realGemmaDeveloperWarning = "Developer-only RealGemma text mode. Not default demo mode. Output must be reviewed before saving."
        )

        assertEquals(
            "Developer warning" to "Developer-only RealGemma text mode. Not default demo mode. Output must be reviewed before saving.",
            status.lines.last()
        )
    }
}
