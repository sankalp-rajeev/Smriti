package com.smriti.clinicalscribe.reasoning

import com.google.ai.edge.litertlm.Backend
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
        assertEquals(modelFile.absolutePath, prepared.engineConfig.modelPath)
        assertTrue(prepared.engineConfig.backend is Backend.CPU)
        assertEquals("CPU", prepared.backendLabel)
        assertTrue(prepared.configConstructionAllowed)
        assertTrue(prepared.reason.contains("EngineConfig constructed"))
        assertTrue(prepared.reason.contains("manual-only"))
        assertFalse(prepared.engineCreated)
        assertFalse(prepared.engineInitializationAttempted)
        assertFalse(prepared.conversationCreated)
        assertFalse(prepared.inferenceAttempted)
    }

    @Test
    fun gpuConfigPlanIsExplicitlyOptIn() {
        val filesDir = Files.createTempDirectory("smriti-config-gpu").toFile()
        val modelFile = LiteRtModelPaths.expectedModelFile(filesDir)
        modelFile.parentFile!!.mkdirs()
        modelFile.writeText("fake model placeholder for gpu config path test only")
        val modelStatus = ModelAvailability.fromFilesDir(filesDir).check()

        val result = LiteRtEngineConfigFactory(LiteRtBackendMode.GPU_EXPERIMENTAL).prepare(modelStatus)

        assertTrue(result is LiteRtEngineConfigPreparation.Prepared)
        val prepared = result as LiteRtEngineConfigPreparation.Prepared
        assertEquals(modelFile.absolutePath, prepared.engineConfig.modelPath)
        assertTrue(prepared.engineConfig.backend is Backend.GPU)
        assertEquals("GPU experimental", prepared.backendLabel)
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
        assertTrue(prepared.configConstructionAllowed)
        assertTrue(prepared.reason.contains("EngineConfig constructed"))
        assertTrue(prepared.reason.contains("manual-only"))
        assertFalse(prepared.engineCreated)
        assertFalse(prepared.engineInitializationAttempted)
    }
}
