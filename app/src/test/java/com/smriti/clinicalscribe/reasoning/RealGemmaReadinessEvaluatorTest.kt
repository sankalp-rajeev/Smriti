package com.smriti.clinicalscribe.reasoning

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RealGemmaReadinessEvaluatorTest {
    private val evaluator = RealGemmaReadinessEvaluator()
    private val configFactory = LiteRtEngineConfigFactory()

    @Test
    fun defaultAgentModeReturnsMockActive() {
        val modelStatus = missingModelStatus()

        val summary = evaluator.evaluate(
            agentMode = AgentConfig.DEFAULT_MODE,
            modelStatus = modelStatus,
            engineConfigPreparation = configFactory.prepare(modelStatus)
        )

        assertEquals(AgentMode.MOCK, AgentConfig.DEFAULT_MODE)
        assertEquals(RealGemmaReadinessStatus.MOCK_ACTIVE, summary.status)
        assertEquals("Mock active", summary.judgeLabel)
        assertSafeFlags(summary)
    }

    @Test
    fun explicitMockModeReturnsMockActiveEvenIfFakeModelExists() {
        val modelStatus = foundModelStatus()

        val summary = evaluator.evaluate(
            agentMode = AgentMode.MOCK,
            modelStatus = modelStatus,
            engineConfigPreparation = configFactory.prepare(modelStatus)
        )

        assertEquals(RealGemmaReadinessStatus.MOCK_ACTIVE, summary.status)
        assertSafeFlags(summary)
    }

    @Test
    fun experimentalModeWithMissingModelReturnsModelNotFound() {
        val modelStatus = missingModelStatus()

        val summary = evaluator.evaluate(
            agentMode = AgentMode.REAL_GEMMA_EXPERIMENTAL,
            modelStatus = modelStatus,
            engineConfigPreparation = configFactory.prepare(modelStatus)
        )

        assertEquals(RealGemmaReadinessStatus.MODEL_NOT_FOUND, summary.status)
        assertEquals("Model not found", summary.judgeLabel)
        assertSafeFlags(summary)
    }

    @Test
    fun experimentalModeWithFakeModelReturnsConfigReadyEngineDisabled() {
        val modelStatus = foundModelStatus()

        val summary = evaluator.evaluate(
            agentMode = AgentMode.REAL_GEMMA_EXPERIMENTAL,
            modelStatus = modelStatus,
            engineConfigPreparation = configFactory.prepare(modelStatus)
        )

        assertEquals(RealGemmaReadinessStatus.MODEL_FOUND_CONFIG_READY_ENGINE_DISABLED, summary.status)
        assertEquals("Model found, engine disabled", summary.judgeLabel)
        assertSafeFlags(summary)
    }

    @Test
    fun experimentalActivationAttemptReturnsDisabled() {
        val modelStatus = foundModelStatus()

        val summary = evaluator.evaluate(
            agentMode = AgentMode.REAL_GEMMA_EXPERIMENTAL,
            modelStatus = modelStatus,
            engineConfigPreparation = configFactory.prepare(modelStatus),
            engineInitializationAllowed = true
        )

        assertEquals(RealGemmaReadinessStatus.EXPERIMENTAL_DISABLED, summary.status)
        assertEquals("Experimental disabled", summary.judgeLabel)
        assertSafeFlags(summary)
    }

    @Test
    fun foundFakeModelDoesNotAccidentallyEnableInference() {
        val modelStatus = foundModelStatus()

        val summary = evaluator.evaluate(
            agentMode = AgentMode.REAL_GEMMA_EXPERIMENTAL,
            modelStatus = modelStatus,
            engineConfigPreparation = configFactory.prepare(modelStatus)
        )

        assertFalse(summary.modelLoadingAllowed)
        assertFalse(summary.inferenceAllowed)
        assertFalse(summary.engineCreated)
        assertFalse(summary.engineInitializationAttempted)
        assertFalse(summary.conversationCreated)
        assertFalse(summary.sendMessageAttempted)
    }

    @Test
    fun readinessTextDoesNotUseDiagnosticOrAutonomousDecisionLanguage() {
        val summaries = listOf(
            evaluator.evaluate(AgentMode.MOCK, missingModelStatus()),
            evaluator.evaluate(AgentMode.REAL_GEMMA_EXPERIMENTAL, missingModelStatus()),
            evaluator.evaluate(AgentMode.REAL_GEMMA_EXPERIMENTAL, foundModelStatus()),
            evaluator.evaluate(
                agentMode = AgentMode.REAL_GEMMA_EXPERIMENTAL,
                modelStatus = foundModelStatus(),
                engineInitializationAllowed = true
            )
        )

        summaries.forEach { summary ->
            val text = "${summary.judgeLabel}\n${summary.developerReason}".lowercase()
            assertFalse(text.contains("diagnosis"))
            assertFalse(text.contains("diagnostic"))
            assertFalse(text.contains("autonomous"))
            assertFalse(text.contains("clinical decision"))
        }
    }

    @Test
    fun liteRtGemmaTextClientStillReturnsUnavailableSafely() = kotlinx.coroutines.runBlocking {
        val result = LiteRtGemmaTextClient(modelStatus = foundModelStatus()).generateText("Do not run.")

        assertTrue(result is TextGenerationResult.Unavailable)
        val unavailable = result as TextGenerationResult.Unavailable
        assertTrue(unavailable.status.contains("LiteRT-LM client scaffold present"))
        assertTrue(unavailable.status.contains("Engine initialization is manual-only"))
        assertTrue(unavailable.status.contains("inference remains disabled"))
    }

    private fun assertSafeFlags(summary: RealGemmaReadinessSummary) {
        assertFalse(summary.modelLoadingAllowed)
        assertFalse(summary.inferenceAllowed)
        assertTrue(summary.demoSafe)
        assertFalse(summary.engineCreated)
        assertFalse(summary.engineInitializationAttempted)
        assertFalse(summary.conversationCreated)
        assertFalse(summary.sendMessageAttempted)
    }

    private fun missingModelStatus(): ModelStatus {
        val filesDir = Files.createTempDirectory("smriti-readiness-missing").toFile()
        return ModelAvailability.fromFilesDir(filesDir).check()
    }

    private fun foundModelStatus(): ModelStatus {
        val filesDir = Files.createTempDirectory("smriti-readiness-found").toFile()
        val modelFile = LiteRtModelPaths.expectedModelFile(filesDir)
        modelFile.parentFile!!.mkdirs()
        modelFile.writeText("fake model placeholder for readiness test only")
        return ModelAvailability.fromFilesDir(filesDir).check()
    }
}
