package com.smriti.clinicalscribe.reasoning

import com.smriti.clinicalscribe.BuildConfig
import com.smriti.clinicalscribe.data.DemoSeedData
import com.smriti.clinicalscribe.data.PatientLanguages
import com.smriti.clinicalscribe.pipeline.VisitPipelineInput
import com.smriti.clinicalscribe.pipeline.VisitReasoningPipeline
import com.smriti.clinicalscribe.rag.ProtocolChunk
import com.smriti.clinicalscribe.rag.ProtocolRetriever
import com.smriti.clinicalscribe.transcript.SimulatedTranscriptClient
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RealGemmaDeveloperModeTest {
    @Test
    fun defaultModeRequiresRealGemmaWhileLegacyDevBuildGateStaysOff() {
        assertEquals(AgentMode.REAL_GEMMA_REQUIRED, AgentConfig.DEFAULT_MODE)
        assertFalse(BuildConfig.REAL_GEMMA_DEV_BUILD_GATE)
    }

    @Test
    fun realGemmaDeveloperModeRequiresBothGates() {
        val modelStatus = missingModelStatus()

        val bothDisabled = RealGemmaDeveloperMode.evaluate(false, false, modelStatus)
        val buildOnly = RealGemmaDeveloperMode.evaluate(true, false, modelStatus)
        val localOnly = RealGemmaDeveloperMode.evaluate(false, true, modelStatus)
        val bothEnabled = RealGemmaDeveloperMode.evaluate(true, true, modelStatus)

        assertEquals(AgentMode.REAL_GEMMA_REQUIRED, bothDisabled.activeAgentMode)
        assertEquals(AgentMode.REAL_GEMMA_REQUIRED, buildOnly.activeAgentMode)
        assertEquals(AgentMode.REAL_GEMMA_REQUIRED, localOnly.activeAgentMode)
        assertEquals(AgentMode.REAL_GEMMA_EXPERIMENTAL, bothEnabled.activeAgentMode)
        assertTrue(buildOnly.usesRealGemmaVisitAgent)
        assertTrue(localOnly.usesRealGemmaVisitAgent)
        assertTrue(bothEnabled.usesRealGemmaVisitAgent)
    }

    @Test
    fun normalDeveloperModeStatusCreatesUnavailableRealGemmaAgent() = runBlocking {
        val modelStatus = foundModelStatus()
        val status = RealGemmaDeveloperMode.evaluate(
            buildTimeGateEnabled = false,
            localGateEnabled = true,
            modelStatus = modelStatus
        )

        val agent = RealGemmaDeveloperAgentFactory.createVisitAgent(status, modelStatus)

        assertTrue(agent is RealGemmaAgent)
        val result = agent.generateVisitNote(
            patient = DemoSeedData.patients.first { it.id == "patient-meena" },
            visitHistory = DemoSeedData.initialVisitLogs(),
            observationText = "Meena reports severe headache and blurred vision.",
            protocolChunks = listOf(protocolChunk())
        )
        assertTrue(result.uncertain)
        assertTrue(result.structuredNote.contains("RealGemma setup required"))
    }

    @Test
    fun missingModelKeepsRealGemmaDeveloperModeSafeAndUnavailable() = runBlocking {
        val modelStatus = missingModelStatus()
        val status = RealGemmaDeveloperMode.evaluate(
            buildTimeGateEnabled = true,
            localGateEnabled = true,
            modelStatus = modelStatus
        )
        val agent = RealGemmaDeveloperAgentFactory.createVisitAgent(status, modelStatus)

        val result = agent.generateVisitNote(
            patient = DemoSeedData.patients.first { it.id == "patient-meena" },
            visitHistory = DemoSeedData.initialVisitLogs(),
            observationText = "Meena reports severe headache and blurred vision.",
            protocolChunks = listOf(protocolChunk())
        )

        assertTrue(agent is RealGemmaAgent)
        assertFalse(status.inferenceEnabled)
        assertTrue(result.uncertain)
        assertEquals(null, result.referralFlag)
        assertTrue(result.structuredNote.contains("RealGemma setup required"))
        assertTrue(result.structuredNote.contains(PatientLanguages.Hindi.safetyWording))
    }

    @Test
    fun realGemmaDeveloperModeUsesVisitReasoningPipelineSafely() = runBlocking {
        val modelStatus = missingModelStatus()
        val status = RealGemmaDeveloperMode.evaluate(
            buildTimeGateEnabled = true,
            localGateEnabled = true,
            modelStatus = modelStatus
        )
        val pipeline = VisitReasoningPipeline(
            protocolRetriever = ProtocolRetriever(listOf(protocolChunk())),
            gemmaAgent = RealGemmaDeveloperAgentFactory.createVisitAgent(status, modelStatus),
            speechToTextClient = SimulatedTranscriptClient()
        )

        val result = pipeline.process(
            VisitPipelineInput(
                patient = DemoSeedData.patients.first { it.id == "patient-meena" },
                priorVisits = DemoSeedData.initialVisitLogs(),
                transcriptText = "Meena reports severe headache and blurred vision."
            )
        )

        assertEquals(AgentMode.REAL_GEMMA_EXPERIMENTAL, status.activeAgentMode)
        val reasoning = result.reasoningResult ?: throw AssertionError("Expected safe uncertain result")
        assertTrue(reasoning.uncertain)
        assertTrue(reasoning.structuredNote.contains("RealGemma setup required"))
    }

    @Test
    fun developerModeLabelsExposeModeGatesAndWarning() {
        val modelStatus = foundModelStatus()
        val status = RealGemmaDeveloperMode.evaluate(
            buildTimeGateEnabled = true,
            localGateEnabled = true,
            modelStatus = modelStatus
        )

        assertTrue(status.inferenceEnabled)
        assertEquals("RealGemmaAgent / Developer-only / Experimental", status.reasoningModeLabel)
        assertEquals("Enabled for developer text mode; CPU backend", status.inferenceStatusLabel)
        assertEquals("Build gate: enabled; local gate: enabled", status.gateStatusLabel)
        assertEquals(
            "Developer-only RealGemma text mode. Not default demo mode. Output must be reviewed before saving.",
            status.developerWarning
        )
    }

    private fun missingModelStatus(): ModelStatus {
        val filesDir = Files.createTempDirectory("smriti-dev-mode-missing").toFile()
        return ModelAvailability.fromFilesDir(filesDir).check()
    }

    private fun foundModelStatus(): ModelStatus {
        val filesDir = Files.createTempDirectory("smriti-dev-mode-found").toFile()
        val modelFile = LiteRtModelPaths.expectedModelFile(filesDir)
        modelFile.parentFile!!.mkdirs()
        modelFile.writeText("fake model placeholder for gate test only")
        return ModelAvailability.fromFilesDir(filesDir).check()
    }

    private fun protocolChunk(): ProtocolChunk {
        return ProtocolChunk(
            id = "danger-headache",
            title = "Danger Signs",
            source = "Smriti Demo Maternal Health Protocol",
            section = "Danger Signs",
            text = "Severe headache and blurred vision require same-day referral support.",
            keywords = "headache|blurred vision",
            referralLevel = "SAME_DAY"
        )
    }
}
