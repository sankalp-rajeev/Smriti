package com.smriti.clinicalscribe.reasoning

import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiteRtGemmaTextClientTest {
    @Test
    fun realGemmaInferenceGateAllowsOnlyOneRequestAtATime() {
        RealGemmaInferenceGate.resetForTests()
        val first = RealGemmaInferenceGate.tryAcquire(
            RealGemmaRequestType.VISIT_NOTE,
            testDiagnostics()
        )
        val second = RealGemmaInferenceGate.tryAcquire(
            RealGemmaRequestType.SUPERVISOR_SUMMARY,
            testDiagnostics()
        )

        assertTrue(first != null)
        assertEquals(null, second)
        first!!.release()
        assertFalse(RealGemmaInferenceGate.isBusy)
    }

    @Test
    fun secondManualInferenceWhileGateActiveReturnsBusyWithoutCallingLiteRt() = runBlocking {
        RealGemmaInferenceGate.resetForTests()
        var runnerCalled = false
        val active = RealGemmaInferenceGate.tryAcquire(
            RealGemmaRequestType.VISIT_NOTE,
            testDiagnostics()
        )!!
        val client = LiteRtGemmaTextClient(
            modelStatus = foundModelStatus(),
            manualInferenceRunner = LiteRtGemmaTextClient.ManualTextInferenceRunner { _, _ ->
                runnerCalled = true
                "should not run"
            }
        )

        val result = client.generateTextManual(
            prompt = "Supervisor should wait",
            allowManualTextInference = true,
            requestType = RealGemmaRequestType.SUPERVISOR_SUMMARY
        )

        assertTrue(result is TextGenerationResult.Unavailable)
        assertEquals(RealGemmaInferenceGate.BUSY_MESSAGE, (result as TextGenerationResult.Unavailable).status)
        assertFalse(runnerCalled)
        active.release()
    }

    @Test
    fun liteRtClientReturnsUnavailableSafely() = runBlocking {
        val client = LiteRtGemmaTextClient()

        val result = client.generateText("Build a safe visit note JSON.")

        assertTrue(result is TextGenerationResult.Unavailable)
        val unavailable = result as TextGenerationResult.Unavailable
        assertTrue(unavailable.status.contains("LiteRT-LM client scaffold present"))
        assertTrue(unavailable.status.contains("Engine initialization is manual-only"))
        assertTrue(unavailable.status.contains("inference remains disabled"))
        assertTrue(unavailable.status.contains("No diagnosis generated"))
        assertTrue(unavailable.status.contains("CHW confirmation required"))
        assertTrue(unavailable.status.contains("Protocol citation required before recommendation"))
    }

    @Test
    fun defaultGenerateTextDoesNotCallManualInferenceRunner() = runBlocking {
        var runnerCalled = false
        val client = LiteRtGemmaTextClient(
            modelStatus = foundModelStatus(),
            manualInferenceRunner = LiteRtGemmaTextClient.ManualTextInferenceRunner { _, _ ->
                runnerCalled = true
                "should not run"
            }
        )

        val result = client.generateText("Normal RealGemma text client call")

        assertTrue(result is TextGenerationResult.Unavailable)
        assertFalse(runnerCalled)
        assertFalse(client.modelLoadAttempted)
        assertFalse(client.engineInitializationAttempted)
        assertFalse(client.conversationCreated)
        assertFalse(client.inferenceAttempted)
    }

    @Test
    fun liteRtClientDoesNotAttemptModelLoadingEngineInitializationOrInference() = runBlocking {
        val filesDir = Files.createTempDirectory("smriti-litert-client").toFile()
        val modelStatus = ModelAvailability.fromFilesDir(filesDir).check()
        val client = LiteRtGemmaTextClient(modelStatus = modelStatus)

        client.generateText("This prompt must not run inference.")

        assertFalse(client.modelLoadAttempted)
        assertFalse(client.engineInitializationAttempted)
        assertFalse(client.conversationCreated)
        assertFalse(client.inferenceAttempted)
    }

    @Test
    fun manualTextInferenceFlagFalseNeverInitializesEngine() = runBlocking {
        var runnerCalled = false
        val client = LiteRtGemmaTextClient(
            modelStatus = foundModelStatus(),
            manualInferenceRunner = LiteRtGemmaTextClient.ManualTextInferenceRunner { _, _ ->
                runnerCalled = true
                "should not run"
            }
        )

        val result = client.generateTextManual(
            prompt = "Manual test prompt",
            allowManualTextInference = false
        )

        assertTrue(result is TextGenerationResult.Unavailable)
        assertFalse(runnerCalled)
        assertFalse(client.modelLoadAttempted)
        assertFalse(client.engineInitializationAttempted)
        assertFalse(client.conversationCreated)
        assertFalse(client.inferenceAttempted)
    }

    @Test
    fun manualTextInferenceMissingModelReturnsUnavailable() = runBlocking {
        var runnerCalled = false
        val client = LiteRtGemmaTextClient(
            modelStatus = missingModelStatus(),
            manualInferenceRunner = LiteRtGemmaTextClient.ManualTextInferenceRunner { _, _ ->
                runnerCalled = true
                "should not run"
            }
        )

        val result = client.generateTextManual(
            prompt = "Manual test prompt",
            allowManualTextInference = true
        )

        assertTrue(result is TextGenerationResult.Unavailable)
        val unavailable = result as TextGenerationResult.Unavailable
        assertTrue(unavailable.status.contains("model not found"))
        assertFalse(runnerCalled)
        assertFalse(client.modelLoadAttempted)
        assertFalse(client.engineInitializationAttempted)
        assertFalse(client.conversationCreated)
        assertFalse(client.inferenceAttempted)
    }

    @Test
    fun preloadMissingModelReturnsUnavailableWithoutInitializingRunner() = runBlocking {
        var preloadCalled = false
        val client = LiteRtGemmaTextClient(
            modelStatus = missingModelStatus(),
            manualInferenceRunner = object : LiteRtGemmaTextClient.ManualTextInferenceRunner {
                override fun generateText(
                    engineConfig: com.google.ai.edge.litertlm.EngineConfig,
                    prompt: String
                ): String = "should not run"

                override fun preload(engineConfig: com.google.ai.edge.litertlm.EngineConfig) {
                    preloadCalled = true
                }
            }
        )

        val result = client.preloadManual(allowManualTextInference = true)

        assertTrue(result is RealGemmaPreloadResult.Unavailable)
        assertFalse(preloadCalled)
        assertFalse(client.modelLoadAttempted)
        assertFalse(client.engineInitializationAttempted)
        assertFalse(client.conversationCreated)
        assertFalse(client.inferenceAttempted)
    }

    @Test
    fun preloadFoundModelTransitionsThroughPreparingReadyPath() = runBlocking {
        var preloadCount = 0
        val client = LiteRtGemmaTextClient(
            modelStatus = foundModelStatus(),
            manualInferenceRunner = object : LiteRtGemmaTextClient.ManualTextInferenceRunner {
                override fun generateText(
                    engineConfig: com.google.ai.edge.litertlm.EngineConfig,
                    prompt: String
                ): String = "generated text"

                override fun preload(engineConfig: com.google.ai.edge.litertlm.EngineConfig) {
                    preloadCount += 1
                }
            }
        )

        val result = client.preloadManual(allowManualTextInference = true)

        assertEquals(RealGemmaPreloadResult.Ready, result)
        assertEquals(1, preloadCount)
        assertTrue(client.modelLoadAttempted)
        assertTrue(client.engineInitializationAttempted)
        assertTrue(client.conversationCreated)
        assertFalse(client.inferenceAttempted)
        assertEquals("Preparing", RealGemmaEnginePreloadState.PREPARING.label)
        assertEquals("Ready", RealGemmaEnginePreloadState.READY.label)
    }

    @Test
    fun manualTextInferenceFakeSuccessReturnsGeneratedText() = runBlocking {
        val client = LiteRtGemmaTextClient(
            modelStatus = foundModelStatus(),
            manualInferenceRunner = LiteRtGemmaTextClient.ManualTextInferenceRunner { engineConfig, prompt ->
                assertTrue(engineConfig.modelPath.endsWith(LiteRtModelPaths.GEMMA_E2B_MODEL_FILE_NAME))
                assertEquals("CPU", engineConfig.backend.name)
                assertEquals("Manual test prompt", prompt)
                "generated text"
            }
        )

        val result = client.generateTextManual(
            prompt = "Manual test prompt",
            allowManualTextInference = true
        )

        assertEquals(TextGenerationResult.Success("generated text"), result)
        assertTrue(client.modelLoadAttempted)
        assertTrue(client.engineInitializationAttempted)
        assertTrue(client.conversationCreated)
        assertTrue(client.inferenceAttempted)
    }

    @Test
    fun experimentalGpuBackendConfigIsOptInOnly() = runBlocking {
        val defaultClient = LiteRtGemmaTextClient(
            modelStatus = foundModelStatus(),
            manualInferenceRunner = LiteRtGemmaTextClient.ManualTextInferenceRunner { engineConfig, _ ->
                assertEquals("CPU", engineConfig.backend.name)
                "default cpu text"
            }
        )

        assertEquals(
            TextGenerationResult.Success("default cpu text"),
            defaultClient.generateTextManual(
                prompt = "Manual CPU prompt",
                allowManualTextInference = true
            )
        )

        val gpuClient = LiteRtGemmaTextClient(
            modelStatus = foundModelStatus(),
            engineConfigFactory = LiteRtEngineConfigFactory(LiteRtBackendMode.GPU_EXPERIMENTAL),
            manualInferenceRunner = LiteRtGemmaTextClient.ManualTextInferenceRunner { engineConfig, _ ->
                assertEquals("GPU", engineConfig.backend.name)
                "experimental gpu text"
            }
        )

        assertEquals(
            TextGenerationResult.Success("experimental gpu text"),
            gpuClient.generateTextManual(
                prompt = "Manual GPU prompt",
                allowManualTextInference = true
            )
        )
    }

    @Test
    fun manualTextInferenceFakeEngineFailureReturnsSafeFailure() = runBlocking {
        val client = LiteRtGemmaTextClient(
            modelStatus = foundModelStatus(),
            manualInferenceRunner = LiteRtGemmaTextClient.ManualTextInferenceRunner { _, _ ->
                throw RuntimeException("engine failed safely")
            }
        )

        val result = client.generateTextManual(
            prompt = "Manual test prompt",
            allowManualTextInference = true
        )

        assertTrue(result is TextGenerationResult.Failed)
        assertTrue((result as TextGenerationResult.Failed).error.contains("engine failed safely"))
        assertTrue(client.modelLoadAttempted)
        assertTrue(client.engineInitializationAttempted)
        assertTrue(client.conversationCreated)
        assertTrue(client.inferenceAttempted)
    }

    @Test
    fun liteRtClientCanPrepareConfigPlanButStillReturnsUnavailable() = runBlocking {
        val filesDir = Files.createTempDirectory("smriti-litert-client-found").toFile()
        val modelFile = LiteRtModelPaths.expectedModelFile(filesDir)
        modelFile.parentFile!!.mkdirs()
        modelFile.writeText("fake model placeholder for status test only")
        val modelStatus = ModelAvailability.fromFilesDir(filesDir).check()
        val client = LiteRtGemmaTextClient(modelStatus = modelStatus)

        val result = client.generateText("This prompt must not run inference.")

        assertTrue(result is TextGenerationResult.Unavailable)
        val unavailable = result as TextGenerationResult.Unavailable
        assertTrue(unavailable.status.contains("EngineConfig ready"))
        assertTrue(unavailable.status.contains("EngineConfig constructed"))
        assertTrue(unavailable.status.contains("manual-only"))
        assertFalse(client.modelLoadAttempted)
        assertFalse(client.engineInitializationAttempted)
        assertFalse(client.conversationCreated)
        assertFalse(client.inferenceAttempted)
    }

    @Test
    fun liteRtApiSurfaceIsDeferredWithoutRuntimeInitialization() {
        val client = LiteRtGemmaTextClient()

        val status = client.apiSurfaceProbeStatus()

        assertTrue(status.contains("Direct LiteRT-LM API types compile"))
        assertTrue(status.contains("com.google.ai.edge.litertlm.Engine"))
        assertTrue(status.contains("com.google.ai.edge.litertlm.EngineConfig"))
        assertTrue(status.contains("com.google.ai.edge.litertlm.Backend"))
        assertTrue(status.contains("com.google.ai.edge.litertlm.Content.Text"))
        assertTrue(status.contains("com.google.ai.edge.litertlm.Content.ImageBytes"))
        assertTrue(status.contains("com.google.ai.edge.litertlm.Content.ImageFile"))
        assertTrue(status.contains("com.google.ai.edge.litertlm.InputData.Image"))
        assertTrue(status.contains("com.google.ai.edge.litertlm.Conversation"))
        assertFalse(client.modelLoadAttempted)
        assertFalse(client.engineInitializationAttempted)
        assertFalse(client.conversationCreated)
        assertFalse(client.inferenceAttempted)
    }

    @Test
    fun defaultModeUsesRealGemmaAgent() {
        assertTrue(GemmaAgentFactory.create() is RealGemmaAgent)
    }

    private fun missingModelStatus(): ModelStatus {
        val filesDir = Files.createTempDirectory("smriti-litert-client-missing").toFile()
        return ModelAvailability.fromFilesDir(filesDir).check()
    }

    private fun foundModelStatus(): ModelStatus {
        val filesDir = Files.createTempDirectory("smriti-litert-client-found").toFile()
        val modelFile = LiteRtModelPaths.expectedModelFile(filesDir)
        modelFile.parentFile!!.mkdirs()
        modelFile.writeText("fake model placeholder for manual inference test only")
        return ModelAvailability.fromFilesDir(filesDir).check()
    }

    private fun testDiagnostics(): RealGemmaRequestDiagnostics {
        return RealGemmaRequestDiagnostics(
            modelExists = true,
            modelSizeBytes = 123L,
            sentinelExists = true,
            backendMode = "CPU",
            engineState = "test",
            lastEngineFailure = null
        )
    }
}
