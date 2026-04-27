package com.smriti.clinicalscribe.reasoning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ModelAvailabilityTest {
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
        val modelDir = File(filesDir, "models").also { it.mkdirs() }
        val modelFile = File(modelDir, "gemma-4-E2B-it-int4.litertlm")
        modelFile.writeBytes(ByteArray(2048))

        val status = ModelAvailability.fromFilesDir(filesDir).check()

        assertEquals(ModelStatusKind.FOUND_NOT_LOADED, status.kind)
        assertEquals(modelFile.absolutePath, status.expectedPath)
        assertEquals(2048L, status.fileSizeBytes)
        assertTrue(status.proofLabel.contains("not loaded"))
    }
}
