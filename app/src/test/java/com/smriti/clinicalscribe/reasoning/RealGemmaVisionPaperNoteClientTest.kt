package com.smriti.clinicalscribe.reasoning

import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RealGemmaVisionPaperNoteClientTest {
    @Test
    fun missingModelReturnsSetupNeededWithoutRunner() = runBlocking {
        var runnerCalled = false
        val client = RealGemmaVisionPaperNoteClient(
            modelStatus = missingModelStatus(),
            cacheDirPath = "cache",
            runner = RealGemmaVisionPaperNoteClient.VisionInferenceRunner { _, _, _ ->
                runnerCalled = true
                "should not run"
            }
        )

        val result = client.extractPaperNote(byteArrayOf(1, 2, 3))

        assertTrue(result is PaperNoteVisionGenerationResult.Unavailable)
        assertTrue((result as PaperNoteVisionGenerationResult.Unavailable).reason.contains("setup needed"))
        assertEquals(false, runnerCalled)
    }

    @Test
    fun foundModelUsesImageBytesAndPromptWithoutMockFallback() = runBlocking {
        val imageBytes = byteArrayOf(9, 8, 7)
        val client = RealGemmaVisionPaperNoteClient(
            modelStatus = foundModelStatus(),
            cacheDirPath = "cache",
            runner = RealGemmaVisionPaperNoteClient.VisionInferenceRunner { engineConfig, prompt, bytes ->
                assertTrue(engineConfig.modelPath.endsWith(LiteRtModelPaths.GEMMA_E2B_MODEL_FILE_NAME))
                assertEquals(imageBytes.toList(), bytes.toList())
                assertTrue(prompt.contains("Extract only what is written in the paper note image"))
                assertTrue(prompt.contains("Do not diagnose"))
                assertTrue(prompt.contains("Do not infer referral need"))
                assertTrue(prompt.contains("Return JSON only"))
                """{"patientName":"Grace Achieng","needsReview":true}"""
            }
        )

        val result = client.extractPaperNote(imageBytes)

        assertEquals(
            PaperNoteVisionGenerationResult.Success("""{"patientName":"Grace Achieng","needsReview":true}"""),
            result
        )
    }

    private fun missingModelStatus(): ModelStatus {
        val filesDir = Files.createTempDirectory("smriti-vision-missing").toFile()
        return ModelAvailability.fromFilesDir(filesDir).check()
    }

    private fun foundModelStatus(): ModelStatus {
        val filesDir = Files.createTempDirectory("smriti-vision-found").toFile()
        val modelFile = LiteRtModelPaths.expectedModelFile(filesDir)
        modelFile.parentFile!!.mkdirs()
        modelFile.writeText("fake model placeholder")
        return ModelAvailability.fromFilesDir(filesDir).check()
    }
}
