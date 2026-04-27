package com.smriti.clinicalscribe.reasoning

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ManualInferenceWiringSafetyTest {
    @Test
    fun normalUiFlowDoesNotCallManualLiteRtInference() {
        val appRoot = File("src/main/java/com/smriti/clinicalscribe")
            .takeIf { it.exists() }
            ?: File("app/src/main/java/com/smriti/clinicalscribe")
        val normalFlowFiles = listOf(
            File(appRoot, "MainActivity.kt"),
            File(appRoot, "ui/PatientListScreen.kt"),
            File(appRoot, "ui/VisitScreen.kt"),
            File(appRoot, "ui/ReviewScreen.kt"),
            File(appRoot, "ui/SummaryScreen.kt")
        )

        val combined = normalFlowFiles.joinToString(separator = "\n") { it.readText() }

        assertFalse(combined.contains("generateTextManual"))
        assertFalse(combined.contains("allowManualTextInference"))
        assertFalse(combined.contains("LiteRtGemmaTextClient("))
    }

    @Test
    fun conversationAndSendMessageStayInsideExplicitManualLiteRtClient() {
        val appRoot = File("src/main/java/com/smriti/clinicalscribe")
            .takeIf { it.exists() }
            ?: File("app/src/main/java/com/smriti/clinicalscribe")
        val filesWithConversationOrSendMessage = appRoot
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { file ->
                val text = file.readText()
                text.contains("createConversation()") || text.contains("sendMessage(")
            }
            .map { it.relativeTo(appRoot).invariantSeparatorsPath }
            .toList()

        assertEquals(listOf("reasoning/LiteRtGemmaTextClient.kt"), filesWithConversationOrSendMessage)
    }
}
