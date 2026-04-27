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
        assertTrue(unavailable.status.contains("direct API types compile after KSP migration"))
        assertTrue(unavailable.status.contains("Engine remains disabled"))
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
        assertTrue(unavailable.status.contains("EngineConfig type check passed"))
        assertTrue(unavailable.status.contains("Engine disabled"))
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
        assertTrue(status.contains("com.google.ai.edge.litertlm.Conversation"))
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
