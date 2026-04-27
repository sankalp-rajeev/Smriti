package com.smriti.clinicalscribe.reasoning

import java.nio.file.Files
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelAvailabilityTest {
    @Test
    fun centralizedModelPathBuildsExpectedFilename() {
        val filesDir = Files.createTempDirectory("smriti-model-path").toFile()

        val modelFile = LiteRtModelPaths.expectedModelFile(filesDir)

        assertEquals(LiteRtModelPaths.GEMMA_E2B_MODEL_FILE_NAME, modelFile.name)
        assertEquals(LiteRtModelPaths.MODELS_DIRECTORY_NAME, modelFile.parentFile!!.name)
        assertTrue(modelFile.absolutePath.endsWith("models${java.io.File.separator}gemma-4-E2B-it-int4.litertlm"))
    }

    @Test
    fun missingExpectedModelReturnsNotFound() {
        val filesDir = Files.createTempDirectory("smriti-model-missing").toFile()

        val status = ModelAvailability.fromFilesDir(filesDir).check()

        assertEquals(ModelStatusKind.NOT_FOUND, status.kind)
        assertTrue(status.expectedPath.endsWith("models${File.separator}gemma-4-E2B-it-int4.litertlm"))
        assertNull(status.fileSizeBytes)
        assertEquals("Not found (inference disabled)", status.proofLabel)
    }

    @Test
    fun existingExpectedModelReturnsFoundNotLoadedWithSize() {
        val filesDir = Files.createTempDirectory("smriti-model-found").toFile()
        val modelFile = LiteRtModelPaths.expectedModelFile(filesDir)
        modelFile.parentFile!!.mkdirs()
        modelFile.writeBytes(ByteArray(2048))

        val status = ModelAvailability.fromFilesDir(filesDir).check()

        assertEquals(ModelStatusKind.FOUND_NOT_LOADED, status.kind)
        assertEquals(modelFile.absolutePath, status.expectedPath)
        assertEquals(2048L, status.fileSizeBytes)
        assertTrue(status.proofLabel.contains("not loaded"))
    }
}
