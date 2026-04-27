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
                "Protocol source" to "Local asset JSON",
                "Active reasoning mode" to "MockGemmaAgent",
                "LiteRT-LM dependency" to "Present",
                "Real Gemma model" to "Not found",
                "EngineConfig" to "Ready when model found; Engine manual-only",
                "RealGemma readiness" to "Mock active",
                "Inference" to "Disabled by default; manual-only"
            ),
            status.lines
        )
    }
}
