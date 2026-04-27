package com.smriti.clinicalscribe.reasoning

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiteRtEngineConfigFactoryTest {
    private val factory = LiteRtEngineConfigFactory()

    @Test
    fun noConfigIsPreparedWhenModelIsNotFound() {
        val filesDir = Files.createTempDirectory("smriti-config-missing").toFile()
        val modelStatus = ModelAvailability.fromFilesDir(filesDir).check()

        val result = factory.prepare(modelStatus)

        assertTrue(result is LiteRtEngineConfigPreparation.NotPrepared)
        val notPrepared = result as LiteRtEngineConfigPreparation.NotPrepared
        assertEquals(ModelStatusKind.NOT_FOUND, notPrepared.modelStatus.kind)
        assertTrue(notPrepared.reason.contains("model file was not found"))
    }

    @Test
    fun engineConfigIsPreparedWhenFakeModelFileExists() {
        val filesDir = Files.createTempDirectory("smriti-config-found").toFile()
        val modelFile = File(File(filesDir, "models").also { it.mkdirs() }, "gemma-4-E2B-it-int4.litertlm")
        modelFile.writeText("fake model placeholder for config path test only")
        val modelStatus = ModelAvailability.fromFilesDir(filesDir).check()

        val result = factory.prepare(modelStatus)

        assertTrue(result is LiteRtEngineConfigPreparation.Prepared)
        val prepared = result as LiteRtEngineConfigPreparation.Prepared
        assertEquals(modelFile.absolutePath, prepared.modelPath)
        assertEquals(modelFile.absolutePath, prepared.engineConfig.modelPath)
        assertEquals("cpu", prepared.backendLabel.lowercase())
        assertFalse(prepared.engineCreated)
        assertFalse(prepared.engineInitializationAttempted)
        assertFalse(prepared.conversationCreated)
        assertFalse(prepared.inferenceAttempted)
    }
}
