package com.smriti.clinicalscribe.reasoning

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
    fun configPlanIsPreparedWhenFakeModelFileExists() {
        val filesDir = Files.createTempDirectory("smriti-config-found").toFile()
        val modelFile = LiteRtModelPaths.expectedModelFile(filesDir)
        modelFile.parentFile!!.mkdirs()
        modelFile.writeText("fake model placeholder for config path test only")
        val modelStatus = ModelAvailability.fromFilesDir(filesDir).check()

        val result = factory.prepare(modelStatus)

        assertTrue(result is LiteRtEngineConfigPreparation.Prepared)
        val prepared = result as LiteRtEngineConfigPreparation.Prepared
        assertEquals(modelFile.absolutePath, prepared.modelPath)
        assertEquals("CPU", prepared.backendLabel)
        assertFalse(prepared.configConstructionAllowed)
        assertTrue(prepared.reason.contains("KAPT cannot read Java 21 LiteRT classes"))
        assertFalse(prepared.engineCreated)
        assertFalse(prepared.engineInitializationAttempted)
        assertFalse(prepared.conversationCreated)
        assertFalse(prepared.inferenceAttempted)
    }

    @Test
    fun directEngineConfigConstructionIsNotAllowedInAppModule() {
        val filesDir = Files.createTempDirectory("smriti-config-deferred").toFile()
        val modelFile = LiteRtModelPaths.expectedModelFile(filesDir)
        modelFile.parentFile!!.mkdirs()
        modelFile.writeText("fake model placeholder for deferred config test only")
        val modelStatus = ModelAvailability.fromFilesDir(filesDir).check()

        val result = factory.prepare(modelStatus)

        assertTrue(result is LiteRtEngineConfigPreparation.Prepared)
        val prepared = result as LiteRtEngineConfigPreparation.Prepared
        assertFalse(prepared.configConstructionAllowed)
        assertTrue(prepared.reason.contains("EngineConfig construction deferred"))
    }
}
