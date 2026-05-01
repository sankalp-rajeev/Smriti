package com.smriti.clinicalscribe.reasoning

import com.smriti.clinicalscribe.BuildConfig
import com.smriti.clinicalscribe.data.DemoSeedData
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

class RealGemmaSubmissionModeTest {
    @Test
    fun normalBuildStillUsesMockDefaults() {
        assertFalse(BuildConfig.REAL_GEMMA_SUBMISSION_MODE)
        assertEquals(AgentMode.MOCK, AgentConfig.DEFAULT_MODE)
        assertTrue(GemmaAgentFactory.create() is MockGemmaAgent)
    }

    @Test
    fun submissionModeRequiresBuildFlagLocalGateAndModel() {
        val missing = missingModelStatus()
        val found = foundModelStatus()

        assertFalse(RealGemmaSubmissionMode.evaluate(false, false, missing).isFullyActive)
        assertFalse(RealGemmaSubmissionMode.evaluate(true, false, found).isFullyActive)
        assertFalse(RealGemmaSubmissionMode.evaluate(true, true, missing).isFullyActive)

        val active = RealGemmaSubmissionMode.evaluate(true, true, found)

        assertTrue(active.isFullyActive)
        assertTrue(active.usesRealGemmaVisitAgent)
        assertEquals("RealGemmaAgent", active.reasoningModeLabel)
        assertEquals("ACTIVE", active.realGemmaTextModeLabel)
        assertEquals("ACTIVE", active.submissionModeLabel)
    }

    @Test
    fun missingModelSubmissionRequestStaysMockAndDoesNotPretendRealGemma() {
        val status = RealGemmaSubmissionMode.evaluate(
            buildTimeGateEnabled = true,
            localGateEnabled = true,
            modelStatus = missingModelStatus()
        )

        assertFalse(status.isFullyActive)
        assertFalse(status.usesRealGemmaVisitAgent)
        assertEquals("MockGemmaAgent", status.reasoningModeLabel)
        assertEquals("Disabled", status.realGemmaTextModeLabel)
        assertTrue(status.warning!!.contains("normal MockGemmaAgent behavior remains active"))
    }

    @Test
    fun activeSubmissionPipelineUsesRealGemmaAgentAndDoesNotUseMockFallback() = runBlocking {
        val agent = RealGemmaAgent(
            textClient = FakeTextClient(
                TextGenerationResult.Unavailable("model unavailable in test")
            )
        )
        val pipeline = VisitReasoningPipeline(
            protocolRetriever = ProtocolRetriever(listOf(protocolChunk())),
            gemmaAgent = agent,
            speechToTextClient = SimulatedTranscriptClient()
        )

        val result = pipeline.process(
            VisitPipelineInput(
                patient = DemoSeedData.patients.first { it.id == "patient-meena" },
                priorVisits = DemoSeedData.initialVisitLogs(),
                transcriptText = "Meena reports severe headache and blurred vision."
            )
        )

        val reasoning = result.reasoningResult ?: throw AssertionError("Expected safe RealGemma unavailable result")
        assertTrue(reasoning.uncertain)
        assertTrue(RealGemmaUnavailableResult.isUnavailable(reasoning))
        assertEquals(null, reasoning.referralFlag)
        assertFalse(reasoning.structuredNote.contains("Danger signs are present in the observation"))
    }

    @Test
    fun retryMessageIsUsedForUnavailableRealGemmaResults() {
        val result = VisitReasoningResult(
            patientId = "patient-meena",
            observationText = "test",
            structuredNote = "Experimental Real Gemma path unavailable: text generation failed safely.",
            referralFlag = null,
            protocolCitation = "Protocol",
            suggestedFollowUp = "Protocol citation required before recommendation.",
            protocolChunk = protocolChunk(),
            uncertain = true,
            clarificationPrompt = "Experimental Real Gemma path unavailable."
        )

        assertTrue(RealGemmaUnavailableResult.isUnavailable(result))
        assertEquals("On-device reasoning unavailable — please retry.", RealGemmaUnavailableResult.RETRY_MESSAGE)
    }

    private class FakeTextClient(
        private val result: TextGenerationResult
    ) : RealGemmaTextClient {
        override suspend fun generateText(prompt: String): TextGenerationResult = result
    }

    private fun missingModelStatus(): ModelStatus {
        val filesDir = Files.createTempDirectory("smriti-submission-missing").toFile()
        return ModelAvailability.fromFilesDir(filesDir).check()
    }

    private fun foundModelStatus(): ModelStatus {
        val filesDir = Files.createTempDirectory("smriti-submission-found").toFile()
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
