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
    fun visitScreenKeepsSampleManualAndOfflineSpeechTranscriptPaths() {
        val visitScreen = appSourceFile("ui/VisitScreen.kt").readText()

        assertTrue(visitScreen.contains("Use sample danger-sign transcript"))
        assertTrue(visitScreen.contains("onValueChange = { observationText = it }"))
        assertTrue(visitScreen.contains("Try Offline Speech"))
        assertTrue(visitScreen.contains("offlineSpeechClient.transcribeLiveSpeech()"))
        assertTrue(visitScreen.contains("observationText = speechResult.transcript"))
        assertTrue(visitScreen.contains("Review and edit before generating"))
        assertTrue(visitScreen.contains("Offline speech unavailable"))
        assertTrue(visitScreen.contains("Offline speech fallback is active"))
        assertTrue(visitScreen.contains("direct Gemma 4 audio remains blocked and documented"))
        assertFalse(visitScreen.contains("Real Gemma 4 audio integration comes next"))
        assertTrue(visitScreen.contains("Reasoning Mode"))
        assertTrue(visitScreen.contains("Active mode: \$reasoningModeLabel"))
    }

    @Test
    fun offlineSpeechHookDoesNotGenerateOrSaveVisit() {
        val visitScreen = appSourceFile("ui/VisitScreen.kt").readText()
        val speechBlockStart = visitScreen.indexOf("fun tryOfflineSpeech()")
        val speechBlockEnd = visitScreen.indexOf("LaunchedEffect", startIndex = speechBlockStart)
        val speechBlock = visitScreen.substring(speechBlockStart, speechBlockEnd)

        assertTrue(speechBlockStart >= 0)
        assertTrue(speechBlockEnd > speechBlockStart)
        assertFalse(speechBlock.contains("onGenerate("))
        assertFalse(speechBlock.contains("saveConfirmedVisit"))
        assertFalse(speechBlock.contains("visitLogDao"))
        assertFalse(speechBlock.contains("referralFlagDao"))
    }

    @Test
    fun offlineSpeechFailureDoesNotClearEditableTranscript() {
        val visitScreen = appSourceFile("ui/VisitScreen.kt").readText()
        val unavailableStart = visitScreen.indexOf("is TranscriptResult.Unavailable ->")
        val errorStart = visitScreen.indexOf("is TranscriptResult.Error ->")
        val successStart = visitScreen.indexOf("is TranscriptResult.Success ->")
        val unavailableBlock = visitScreen.substring(unavailableStart, errorStart)
        val errorBlock = visitScreen.substring(errorStart, visitScreen.indexOf("}", startIndex = errorStart))

        assertTrue(successStart >= 0)
        assertTrue(unavailableStart > successStart)
        assertTrue(errorStart > unavailableStart)
        assertFalse(unavailableBlock.contains("observationText ="))
        assertFalse(errorBlock.contains("observationText ="))
        assertFalse(unavailableBlock.contains("code 13"))
        assertFalse(errorBlock.contains("code 13"))
    }

    @Test
    fun sampleTranscriptButtonStillFillsEditableTranscript() {
        val visitScreen = appSourceFile("ui/VisitScreen.kt").readText()
        val sampleButtonStart = visitScreen.indexOf("Text(\"Use sample danger-sign transcript\")")
        assertTrue(sampleButtonStart >= 0)
        val outlinedButtonStart = visitScreen.lastIndexOf("OutlinedButton(", startIndex = sampleButtonStart)
        assertTrue(outlinedButtonStart >= 0)
        val sampleButtonBlock = visitScreen.substring(
            outlinedButtonStart,
            sampleButtonStart
        )

        assertTrue(sampleButtonBlock.contains("observationText = SampleDangerSignTranscript"))
        assertTrue(sampleButtonBlock.contains("offlineSpeechStatus = null"))
    }

    @Test
    fun generateStillUsesEditableTranscriptText() {
        val visitScreen = appSourceFile("ui/VisitScreen.kt").readText()

        assertTrue(visitScreen.contains("onGenerate(observationText, savedVoiceNote)"))
        assertTrue(visitScreen.contains("enabled = observationText.isNotBlank() && !isGenerating"))
    }

    @Test
    fun realGemmaIsNotWiredIntoNormalUi() {
        val mainActivity = appSourceFile("MainActivity.kt").readText()
        val visitScreen = appSourceFile("ui/VisitScreen.kt").readText()
        val patientListScreen = appSourceFile("ui/PatientListScreen.kt").readText()
        val summaryScreen = appSourceFile("ui/SummaryScreen.kt").readText()

        assertTrue(mainActivity.contains("realGemmaDevBuildGate: Boolean = BuildConfig.DEBUG && BuildConfig.REAL_GEMMA_DEV_BUILD_GATE"))
        assertTrue(mainActivity.contains("RealGemmaDeveloperMode.isLocalGateEnabled(context.filesDir)"))
        assertTrue(mainActivity.contains("RealGemmaDeveloperAgentFactory.createVisitAgent"))
        assertTrue(mainActivity.contains("summaryAgent = remember { GemmaAgentFactory.create(AgentConfig.DEFAULT_MODE) }"))
        assertFalse(visitScreen.contains("REAL_GEMMA_EXPERIMENTAL"))
        assertFalse(patientListScreen.contains("REAL_GEMMA_EXPERIMENTAL"))
        assertFalse(summaryScreen.contains("REAL_GEMMA_EXPERIMENTAL"))
        assertFalse(visitScreen.contains("RealGemmaAgent("))
        assertFalse(patientListScreen.contains("RealGemmaAgent("))
        assertFalse(summaryScreen.contains("RealGemmaAgent("))
    }

    @Test
    fun realGemmaDeveloperModeStillUsesVisitReasoningPipeline() {
        val mainActivity = appSourceFile("MainActivity.kt").readText()

        assertTrue(mainActivity.contains("visitAgent = remember(realGemmaDeveloperModeStatus, modelStatus)"))
        assertTrue(mainActivity.contains("gemmaAgent = visitAgent"))
        assertTrue(mainActivity.contains("visitReasoningPipeline.process("))
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
