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
                "Patient memory" to "Room/SQLite local storage",
                "Health guidance" to "Local JSON pack: 46 chunks, 6 countries",
                "Model file" to "Setup needed",
                "Engine state" to "manual only",
                "Transcript source" to "offline speech or manual typing",
                "Paper note scan" to "Available",
                "Vision support" to "Uses local Gemma vision",
                "Scan review" to "Review required before save",
                "Cloud OCR" to "none",
                "Languages" to "EN, HI, ES, SW",
                "Cloud APIs" to "none",
                "Direct Gemma audio" to "not used"
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
                "Works offline after setup" to "",
                "Patient memory" to "saved on this device",
                "Health guidance" to "stored on this device",
                "On-device Gemma" to "Setup needed",
                "Paper note scan" to "setup needed",
                "Cloud APIs" to "none",
                "Direct Gemma audio" to "not used"
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

        assertEquals("Direct Gemma audio" to "not used", status.lines.last())
        assertEquals("Model file" to "ready", status.lines[2])
        assertEquals("Engine state" to "ready for use", status.lines[3])
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

        assertEquals("Model file" to "ready", status.lines[2])
        assertEquals("Engine state" to "ready for use", status.lines[3])
    }
}
