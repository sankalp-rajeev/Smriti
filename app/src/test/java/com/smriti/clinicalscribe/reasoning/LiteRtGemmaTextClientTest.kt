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
        assertTrue(unavailable.status.contains("engine initialization intentionally disabled"))
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
        assertFalse(client.inferenceAttempted)
    }

    @Test
    fun liteRtApiSurfaceNamesAreDocumentedWithoutRuntimeInitialization() {
        val client = LiteRtGemmaTextClient()
        val classNames = client.apiSurfaceClassNames()

        assertTrue(classNames.contains("com.google.ai.edge.litertlm.Engine"))
        assertTrue(classNames.contains("com.google.ai.edge.litertlm.EngineConfig"))
        assertTrue(classNames.contains("com.google.ai.edge.litertlm.Backend"))
        assertTrue(classNames.contains("com.google.ai.edge.litertlm.Content\$Text"))
        assertTrue(classNames.contains("com.google.ai.edge.litertlm.Conversation"))
        assertTrue(client.apiSurfaceProbeStatus().contains("Direct LiteRT-LM imports compile under JDK 21"))
    }

    @Test
    fun defaultModeStillUsesMockAgent() {
        assertTrue(GemmaAgentFactory.create() is MockGemmaAgent)
    }
}
