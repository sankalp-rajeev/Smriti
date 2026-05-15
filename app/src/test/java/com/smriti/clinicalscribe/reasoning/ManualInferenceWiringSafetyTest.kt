package com.smriti.clinicalscribe.reasoning

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
        assertFalse(combined.contains("allowManualVisionInference"))
        assertFalse(combined.contains("ManualRealGemmaVisionProbeInstrumentedTest"))
    }

    @Test
    fun scanPaperNoteUiRequiresReviewAndDoesNotAutoSave() {
        val appRoot = File("src/main/java/com/smriti/clinicalscribe")
            .takeIf { it.exists() }
            ?: File("app/src/main/java/com/smriti/clinicalscribe")
        val uiFiles = listOf(
            File(appRoot, "MainActivity.kt"),
            File(appRoot, "ui/PatientListScreen.kt"),
            File(appRoot, "ui/VisitScreen.kt"),
            File(appRoot, "ui/ReviewScreen.kt"),
            File(appRoot, "ui/SummaryScreen.kt")
        )
        val combined = uiFiles
            .joinToString(separator = "\n") { it.readText() }
            .lowercase()
        val reviewScannedNote = File(appRoot, "ui/ReviewScannedNoteScreen.kt").readText()
        val mainActivity = File(appRoot, "MainActivity.kt").readText()

        assertTrue(combined.contains("scan paper note"))
        assertTrue(combined.contains("reading paper note"))
        assertTrue(reviewScannedNote.contains("Review scanned note"))
        assertTrue(reviewScannedNote.contains("Save to patient history"))
        assertTrue(reviewScannedNote.contains("targetPatient != null"))
        assertTrue(reviewScannedNote.contains("Yes, save to this patient"))
        assertTrue(reviewScannedNote.contains("Yes, link"))
        assertTrue(reviewScannedNote.contains("mutableStateOf<Patient?>(null)"))
        assertTrue(reviewScannedNote.contains("Text was extracted from a paper note. Review before saving."))
        assertTrue(mainActivity.contains("saveConfirmedScannedPaperNote"))
        val reviewScanStart = mainActivity.indexOf("is SmritiScreen.ReviewScannedNote")
        val reviewScanEnd = mainActivity.indexOf("is SmritiScreen.Summary", startIndex = reviewScanStart)
        assertTrue(reviewScanStart >= 0)
        assertTrue(reviewScanEnd > reviewScanStart)
        assertFalse(mainActivity.substring(reviewScanStart, reviewScanEnd).contains("visitReasoningPipeline.process("))
        assertFalse(combined.contains("content.imagebytes"))
        assertFalse(combined.contains("inputdata.image"))
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

        assertEquals(
            listOf(
                "reasoning/LiteRtApiSurfaceProbe.kt",
                "reasoning/LiteRtGemmaAudioTranscriptClient.kt",
                "reasoning/LiteRtGemmaTextClient.kt",
                "reasoning/RealGemmaVisionPaperNoteClient.kt"
            ),
            filesWithConversationOrSendMessage
        )
    }

    @Test
    fun speculativeDecodingProbeIsNotWiredIntoNormalAppFlow() {
        val appRoot = File("src/main/java/com/smriti/clinicalscribe")
            .takeIf { it.exists() }
            ?: File("app/src/main/java/com/smriti/clinicalscribe")
        val normalFlowFiles = listOf(
            File(appRoot, "MainActivity.kt"),
            File(appRoot, "ui/PatientListScreen.kt"),
            File(appRoot, "ui/VisitScreen.kt"),
            File(appRoot, "ui/ReviewScreen.kt"),
            File(appRoot, "ui/SummaryScreen.kt"),
            File(appRoot, "reasoning/LiteRtGemmaTextClient.kt"),
            File(appRoot, "reasoning/RealGemmaDeveloperMode.kt")
        )
        val combined = normalFlowFiles.joinToString(separator = "\n") { it.readText() }
        val engineConfigFactory = File(appRoot, "reasoning/LiteRtEngineConfigFactory.kt").readText()

        assertFalse(combined.contains("enableSpeculativeDecoding"))
        assertFalse(combined.contains("hasSpeculativeDecodingSupport"))
        assertFalse(combined.contains("ManualRealGemmaSpeculativeLatencyInstrumentedTest"))
        assertFalse(combined.contains("SmritiSpeculativeLatency"))
        assertTrue(engineConfigFactory.contains("private val backendMode: LiteRtBackendMode = LiteRtBackendMode.CPU"))
        assertTrue(engineConfigFactory.contains("backend = backendMode.toBackend()"))
        assertFalse(engineConfigFactory.contains("enableSpeculativeDecoding"))
    }

    @Test
    fun protocolToolCallingProbeIsNotWiredIntoNormalAppFlow() {
        val appRoot = File("src/main/java/com/smriti/clinicalscribe")
            .takeIf { it.exists() }
            ?: File("app/src/main/java/com/smriti/clinicalscribe")
        val normalFlowFiles = listOf(
            File(appRoot, "MainActivity.kt"),
            File(appRoot, "ui/VisitScreen.kt"),
            File(appRoot, "ui/SummaryScreen.kt"),
            File(appRoot, "pipeline/VisitReasoningPipeline.kt"),
            File(appRoot, "reasoning/RealGemmaAgent.kt"),
            File(appRoot, "reasoning/LiteRtGemmaTextClient.kt")
        )
        val combined = normalFlowFiles.joinToString(separator = "\n") { it.readText() }
        val pipeline = File(appRoot, "pipeline/VisitReasoningPipeline.kt").readText()

        assertFalse(combined.contains("ManualLiteRtProtocolToolCallingInstrumentedTest"))
        assertFalse(combined.contains("SmritiProtocolToolCall"))
        assertFalse(combined.contains("lookupProtocol"))
        assertFalse(combined.contains("OpenApiTool"))
        assertFalse(combined.contains("ConversationConfig("))
        assertFalse(combined.contains("automaticToolCalling"))
        assertTrue(pipeline.contains("protocolRetriever.retrieve("))
        assertTrue(pipeline.contains("gemmaAgent.generateVisitNote("))
    }
}
