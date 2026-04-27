package com.smriti.clinicalscribe.reasoning

import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiteRtGemmaTextClientTest {
    @Test
    fun liteRtClientReturnsUnavailableSafely() = runBlocking {
        val client = LiteRtGemmaTextClient()

        val result = client.generateText("Build a safe visit note JSON.")

        assertTrue(result is TextGenerationResult.Unavailable)
        val unavailable = result as TextGenerationResult.Unavailable
        assertTrue(unavailable.status.contains("LiteRT-LM client scaffold present"))
        assertTrue(unavailable.status.contains("direct API use deferred"))
        assertTrue(unavailable.status.contains("KAPT cannot read Java 21 LiteRT classes"))
        assertTrue(unavailable.status.contains("No diagnosis generated"))
        assertTrue(unavailable.status.contains("CHW confirmation required"))
        assertTrue(unavailable.status.contains("Protocol citation required before recommendation"))
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
        assertTrue(unavailable.status.contains("EngineConfig deferred"))
        assertTrue(unavailable.status.contains("KAPT cannot read Java 21 LiteRT classes"))
        assertFalse(client.modelLoadAttempted)
        assertFalse(client.engineInitializationAttempted)
        assertFalse(client.conversationCreated)
        assertFalse(client.inferenceAttempted)
    }

    @Test
    fun liteRtApiSurfaceIsDeferredWithoutRuntimeInitialization() {
        val client = LiteRtGemmaTextClient()

        assertTrue(client.apiSurfaceProbeStatus().contains("Direct LiteRT-LM API use deferred"))
        assertFalse(client.modelLoadAttempted)
        assertFalse(client.engineInitializationAttempted)
        assertFalse(client.conversationCreated)
        assertFalse(client.inferenceAttempted)
    }

    @Test
    fun defaultModeStillUsesMockAgent() {
        assertTrue(GemmaAgentFactory.create() is MockGemmaAgent)
    }
}
