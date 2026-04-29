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
                "Protocol source" to "Local asset JSON; country-aware retrieval",
                "Active reasoning mode" to "MockGemmaAgent",
                "LiteRT-LM dependency" to "Present",
                "Real Gemma model" to "Not found",
                "EngineConfig" to "Ready when model found; Engine manual-only",
                "RealGemma readiness" to "Mock active",
                "RealGemma dev gate" to "Build gate: disabled; local gate: disabled",
                "Inference" to "Disabled by default; manual-only",
                "Backend" to "CPU when developer text inference is enabled"
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
