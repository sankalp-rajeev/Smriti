package com.smriti.clinicalscribe.reasoning

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiteRtEngineInitializationCheckerTest {
    private val configFactory = LiteRtEngineConfigFactory()

    @Test
    fun checkerSkipsWhenManualFlagIsFalse() {
        val modelStatus = foundModelStatus()
        val preparation = configFactory.prepare(modelStatus)
        var initializerCalled = false
        val checker = LiteRtEngineInitializationChecker(
            initializer = LiteRtEngineInitializationChecker.EngineInitializer {
                initializerCalled = true
            }
        )

        val result = checker.check(
            modelStatus = modelStatus,
            configPreparation = preparation,
            allowManualEngineInitialization = false
        )

        assertEquals(LiteRtEngineInitializationResult.SkippedNotAllowed, result)
        assertFalse(initializerCalled)
    }

    @Test
    fun checkerReturnsModelNotFoundWhenAllowedButModelIsMissing() {
        val modelStatus = missingModelStatus()
        val preparation = configFactory.prepare(modelStatus)
        var initializerCalled = false
        val checker = LiteRtEngineInitializationChecker(
            initializer = LiteRtEngineInitializationChecker.EngineInitializer {
                initializerCalled = true
            }
        )

        val result = checker.check(
            modelStatus = modelStatus,
            configPreparation = preparation,
            allowManualEngineInitialization = true
        )

        assertEquals(LiteRtEngineInitializationResult.ModelNotFound, result)
        assertFalse(initializerCalled)
    }

    @Test
    fun checkerReturnsConfigNotReadyWhenAllowedButPreparationIsMissing() {
        val modelStatus = foundModelStatus()
        val notPrepared = LiteRtEngineConfigPreparation.NotPrepared(
            reason = "test config intentionally unavailable",
            modelStatus = modelStatus
        )
        var initializerCalled = false
        val checker = LiteRtEngineInitializationChecker(
            initializer = LiteRtEngineInitializationChecker.EngineInitializer {
                initializerCalled = true
            }
        )

        val result = checker.check(
            modelStatus = modelStatus,
            configPreparation = notPrepared,
            allowManualEngineInitialization = true
        )

        assertEquals(LiteRtEngineInitializationResult.ConfigNotReady, result)
        assertFalse(initializerCalled)
    }

    @Test
    fun checkerCanUseFakeInitializerForManualSuccessPath() {
        val modelStatus = foundModelStatus()
        val preparation = configFactory.prepare(modelStatus)
        var initializedModelPath: String? = null
        val checker = LiteRtEngineInitializationChecker(
            initializer = LiteRtEngineInitializationChecker.EngineInitializer { engineConfig ->
                initializedModelPath = engineConfig.modelPath
            }
        )

        val result = checker.check(
            modelStatus = modelStatus,
            configPreparation = preparation,
            allowManualEngineInitialization = true
        )

        assertEquals(LiteRtEngineInitializationResult.InitializedAndClosed, result)
        assertTrue(preparation is LiteRtEngineConfigPreparation.Prepared)
        assertEquals((preparation as LiteRtEngineConfigPreparation.Prepared).modelPath, initializedModelPath)
    }

    @Test
    fun checkerReturnsFailedWhenInitializerThrows() {
        val modelStatus = foundModelStatus()
        val preparation = configFactory.prepare(modelStatus)
        val checker = LiteRtEngineInitializationChecker(
            initializer = LiteRtEngineInitializationChecker.EngineInitializer {
                throw RuntimeException("manual load failed")
            }
        )

        val result = checker.check(
            modelStatus = modelStatus,
            configPreparation = preparation,
            allowManualEngineInitialization = true
        )

        assertTrue(result is LiteRtEngineInitializationResult.Failed)
        assertTrue((result as LiteRtEngineInitializationResult.Failed).reason.contains("manual load failed"))
    }

    private fun missingModelStatus(): ModelStatus {
        val filesDir = Files.createTempDirectory("smriti-engine-check-missing").toFile()
        return ModelAvailability.fromFilesDir(filesDir).check()
    }

    private fun foundModelStatus(): ModelStatus {
        val filesDir = Files.createTempDirectory("smriti-engine-check-found").toFile()
        val modelFile = LiteRtModelPaths.expectedModelFile(filesDir)
        modelFile.parentFile!!.mkdirs()
        modelFile.writeText("fake model placeholder for manual checker test only")
        return ModelAvailability.fromFilesDir(filesDir).check()
    }
}
