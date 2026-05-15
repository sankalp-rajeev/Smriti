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
                "Runs after setup without cloud APIs" to "",
                "Local Room database" to "patient memory saved on this device",
                "Local protocol pack" to "cited guidance stored on this device",
                "LiteRT-LM Gemma 4" to "Setup needed",
                "Transcript input" to "editable before note generation",
                "Paper note scan" to "review before save",
                "Languages" to "EN, HI, ES, SW",
                "CHW review required" to "before any save"
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
                "Runs after setup without cloud APIs" to "",
                "Local Room database" to "saved on this device",
                "Local protocol pack" to "available",
                "LiteRT-LM Gemma 4" to "Setup needed",
                "CHW review required" to "before save"
            ),
            status.compactLines
        )
        assertEquals(
            listOf(
                "Runs after setup without cloud APIs",
                "Local Room database: saved on this device",
                "Local protocol pack: available",
                "LiteRT-LM Gemma 4: Setup needed",
                "CHW review required: before save"
            ),
            status.compactDisplayLines
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

        assertEquals("CHW review required" to "before any save", status.lines.last())
        assertEquals("LiteRT-LM Gemma 4" to "ready", status.lines[3])
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

        assertEquals("LiteRT-LM Gemma 4" to "ready", status.lines[3])
    }
}
