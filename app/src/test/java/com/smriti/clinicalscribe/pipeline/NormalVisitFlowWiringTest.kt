package com.smriti.clinicalscribe.pipeline

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NormalVisitFlowWiringTest {
    @Test
    fun normalUiGenerationPathUsesVisitReasoningPipeline() {
        val mainActivity = appSourceFile("MainActivity.kt").readText()

        assertTrue(mainActivity.contains("VisitReasoningPipeline("))
        assertTrue(mainActivity.contains("visitReasoningPipeline.process("))
        assertTrue(mainActivity.contains("VisitPipelineInput("))
        assertFalse(mainActivity.contains("agent.generateVisitNote("))
    }

    @Test
    fun reviewConfirmationStillOwnsVisitMemorySave() {
        val mainActivity = appSourceFile("MainActivity.kt").readText()

        val confirmIndex = mainActivity.indexOf("onConfirmSave")
        val confirmationBlock = mainActivity.substring(confirmIndex)

        assertTrue(confirmIndex >= 0)
        assertTrue(confirmationBlock.contains("visitMemoryStore.saveConfirmedVisit"))
        assertTrue(confirmationBlock.contains("applySnapshot(snapshot)"))
    }

    @Test
    fun pipelinePackageDoesNotWriteToRoom() {
        val pipelineRoot = appSourceFile("pipeline")
        val combinedPipelineSource = pipelineRoot
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .joinToString(separator = "\n") { it.readText() }

        assertFalse(combinedPipelineSource.contains("AppDatabase"))
        assertFalse(combinedPipelineSource.contains("Room"))
        assertFalse(combinedPipelineSource.contains("Dao"))
        assertFalse(combinedPipelineSource.contains(".insert("))
    }

    @Test
    fun mainSourceDoesNotAddCloudOrDownloadRuntimeCode() {
        val appRoot = appSourceFile("")
        val combinedMainSource = appRoot
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .joinToString(separator = "\n") { it.readText() }
            .lowercase()

        assertFalse(combinedMainSource.contains("http://"))
        assertFalse(combinedMainSource.contains("https://"))
        assertFalse(combinedMainSource.contains("openai"))
        assertFalse(combinedMainSource.contains("gemini api"))
        assertFalse(combinedMainSource.contains("firebase"))
        assertFalse(combinedMainSource.contains("supabase"))
        assertFalse(combinedMainSource.contains("hugging face"))
        assertFalse(combinedMainSource.contains("downloadmodel"))
        assertFalse(combinedMainSource.contains("cloud asr"))
    }

    private fun appSourceFile(relativePath: String): File {
        val modulePath = File("src/main/java/com/smriti/clinicalscribe/$relativePath")
        val rootPath = File("app/src/main/java/com/smriti/clinicalscribe/$relativePath")
        return when {
            modulePath.exists() -> modulePath
            else -> rootPath
        }
    }
}
