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
        val confirmationEnd = mainActivity.indexOf("onBack = { currentScreen = SmritiScreen.Visit", startIndex = confirmIndex)
        val confirmationBlock = mainActivity.substring(confirmIndex, confirmationEnd)

        assertTrue(confirmIndex >= 0)
        assertTrue(confirmationEnd > confirmIndex)
        assertTrue(confirmationBlock.contains("visitMemoryStore.saveConfirmedVisit"))
        assertTrue(confirmationBlock.contains("withContext(Dispatchers.IO)"))
        assertTrue(confirmationBlock.contains("applySnapshot(snapshot)"))
        assertTrue(confirmationBlock.contains("buildRawLocalSummary"))
        assertTrue(confirmationBlock.contains("confirmSaveRoomWrite"))
        assertTrue(confirmationBlock.contains("patientMessageTarget = latestPatientMessageTarget(screen.patient, snapshot)"))
        assertFalse(confirmationBlock.contains("buildSummaryScreen"))
        assertFalse(confirmationBlock.contains("visitReasoningPipeline.process"))
        assertFalse(confirmationBlock.contains("RealGemma"))
        assertFalse(confirmationBlock.contains("retriever."))
        assertFalse(confirmationBlock.contains("ProtocolRetriever"))
        assertFalse(confirmationBlock.contains("jsonExporter.exportVisit"))
        assertFalse(confirmationBlock.contains("PatientLeaveBehindMessageGenerator.generate"))
        assertFalse(confirmationBlock.contains("sharePatientMessage"))
        assertFalse(confirmationBlock.contains("Intent.ACTION_SEND"))
    }

    @Test
    fun patientMessageIsGeneratedOnlyFromSavedVisitTarget() {
        val mainActivity = appSourceFile("MainActivity.kt").readText()

        val targetStart = mainActivity.indexOf("fun latestPatientMessageTarget")
        val targetEnd = mainActivity.indexOf("DisposableEffect", startIndex = targetStart)
        val targetBlock = mainActivity.substring(targetStart, targetEnd)
        val patientMessageStart = mainActivity.indexOf("is SmritiScreen.PatientMessage")
        val patientMessageEnd = mainActivity.indexOf("errorMessage?.let", startIndex = patientMessageStart)
        val patientMessageBlock = mainActivity.substring(patientMessageStart, patientMessageEnd)

        assertTrue(targetBlock.contains("it.confirmed"))
        assertTrue(targetBlock.contains("transcriptSource !="))
        assertTrue(targetBlock.contains("PatientMessageTarget(patient = patient, visit = visit, referral = referral)"))
        assertTrue(patientMessageBlock.contains("PatientLeaveBehindMessageGenerator.generate"))
        assertTrue(patientMessageBlock.contains("PatientMessageScreen("))
        assertFalse(targetBlock.contains("PatientLeaveBehindMessageGenerator.generate"))
        assertFalse(mainActivity.substring(0, targetStart).contains("PatientLeaveBehindMessageGenerator.generate"))
    }

    @Test
    fun patientMessageSharingIsUserInitiatedAndUsesShareIntentOnly() {
        val mainActivity = appSourceFile("MainActivity.kt").readText()
        val screen = appSourceFile("ui/PatientMessageScreen.kt").readText()

        val shareStart = mainActivity.indexOf("fun sharePatientMessage")
        val shareEnd = mainActivity.indexOf("fun openVisitsAfterWelcome", startIndex = shareStart)
        val shareBlock = mainActivity.substring(shareStart, shareEnd)
        val confirmStart = mainActivity.indexOf("onConfirmSave")
        val confirmEnd = mainActivity.indexOf("onBack = { currentScreen = SmritiScreen.Visit", startIndex = confirmStart)
        val confirmBlock = mainActivity.substring(confirmStart, confirmEnd)

        assertTrue(shareBlock.contains("Intent(Intent.ACTION_SEND)"))
        assertTrue(shareBlock.contains("type = \"text/plain\""))
        assertTrue(shareBlock.contains("Intent.createChooser"))
        assertTrue(shareBlock.contains("context.startActivity(chooser)"))
        assertTrue(screen.contains("onClick = { onShare(message) }"))
        assertTrue(mainActivity.contains("onShare = { message -> sharePatientMessage(message) }"))
        assertFalse(confirmBlock.contains("context.startActivity"))
        assertFalse(confirmBlock.contains("Intent.ACTION_SEND"))
        assertFalse(mainActivity.contains("SmsManager"))
        assertFalse(mainActivity.contains("sendTextMessage"))
        assertFalse(mainActivity.contains("ACTION_SENDTO"))
    }

    @Test
    fun patientMessageScreenRequiresEditableReviewBeforeSharing() {
        val screen = appSourceFile("ui/PatientMessageScreen.kt").readText()
        val summaryScreen = appSourceFile("ui/SummaryScreen.kt").readText()

        assertTrue(screen.contains("OutlinedTextField("))
        assertTrue(screen.contains("Review before sharing"))
        assertTrue(screen.contains("Message for patient"))
        assertTrue(screen.contains("SmritiPrimaryButton("))
        assertTrue(screen.contains("text = \"Share\""))
        assertTrue(screen.contains("text = \"Copy\""))
        assertTrue(summaryScreen.contains("Prepare patient message"))
        assertTrue(summaryScreen.contains("Smriti will not send it automatically."))
    }

    @Test
    fun patientMessageDoesNotPersistAsVisitOrFollowUpTask() {
        val mainActivity = appSourceFile("MainActivity.kt").readText()
        val generator = appSourceFile("data/PatientLeaveBehindMessage.kt").readText()

        val patientMessageStart = mainActivity.indexOf("is SmritiScreen.PatientMessage")
        val patientMessageEnd = mainActivity.indexOf("errorMessage?.let", startIndex = patientMessageStart)
        val patientMessageBlock = mainActivity.substring(patientMessageStart, patientMessageEnd)

        assertFalse(patientMessageBlock.contains("saveConfirmedVisit"))
        assertFalse(patientMessageBlock.contains("saveConfirmedScannedPaperNote"))
        assertFalse(patientMessageBlock.contains("FollowUpTask"))
        assertFalse(patientMessageBlock.contains("visitMemoryStore."))
        assertFalse(generator.contains("AppDatabase"))
        assertFalse(generator.contains("Dao"))
        assertFalse(generator.contains("insert"))
        assertFalse(generator.contains("FollowUpTask"))
    }

    @Test
    fun communityPanelUsesLocalStateWithoutInferenceOrPersistence() {
        val mainActivity = appSourceFile("MainActivity.kt").readText()
        val panelModel = appSourceFile("ui/CommunityPanel.kt").readText()
        val panelScreen = appSourceFile("ui/CommunityPanelScreen.kt").readText()

        val screenStart = mainActivity.indexOf("SmritiScreen.CommunityPanel ->")
        val screenEnd = mainActivity.indexOf("SmritiScreen.AddPatient ->", startIndex = screenStart)
        val screenBlock = mainActivity.substring(screenStart, screenEnd)

        assertTrue(screenBlock.contains("CommunityPanelBuilder.build"))
        assertTrue(screenBlock.contains("patients = patients"))
        assertTrue(screenBlock.contains("visits = visits"))
        assertTrue(screenBlock.contains("referrals = referrals"))
        assertTrue(screenBlock.contains("followUpTasks = followUpTasks"))
        assertFalse(screenBlock.contains("visitReasoningPipeline.process"))
        assertFalse(screenBlock.contains("RealGemma"))
        assertFalse(screenBlock.contains("LiteRt"))
        assertFalse(screenBlock.contains("retriever."))
        assertFalse(screenBlock.contains("ProtocolRetriever"))
        assertFalse(screenBlock.contains("visitMemoryStore."))
        assertFalse(panelModel.contains("AppDatabase"))
        assertFalse(panelModel.contains("Dao"))
        assertFalse(panelModel.contains("insert("))
        assertFalse(panelModel.contains("upsert"))
        assertFalse(panelScreen.contains("PatientLeaveBehindMessage"))
    }

    @Test
    fun visitScreenKeepsSampleManualAndOfflineSpeechTranscriptPaths() {
        val visitScreen = appSourceFile("ui/VisitScreen.kt").readText()

        assertTrue(visitScreen.contains("Use sample visit transcript"))
        assertTrue(visitScreen.contains("onValueChange = {"))
        assertTrue(visitScreen.contains("Speak observation"))
        assertTrue(visitScreen.contains("offlineSpeechClient.transcribeLiveSpeech()"))
        assertTrue(visitScreen.contains("observationText = speechResult.transcript"))
        assertTrue(visitScreen.contains("Speech added. Please review before generating."))
        assertTrue(visitScreen.contains("Speech is not available on this device. Please type manually."))
        assertTrue(visitScreen.contains("No speech detected. Please try again or type manually."))
        assertFalse(visitScreen.contains("Real Gemma 4 audio integration comes next"))
        assertTrue(visitScreen.contains("Today's observation"))
        assertTrue(visitScreen.contains("Offline setup ready"))
        assertTrue(visitScreen.contains("On-device Gemma:"))
        assertTrue(visitScreen.contains("Local guidance available"))
        assertTrue(visitScreen.contains("Visit note will be prepared in \${PatientVisitUiText.noteLanguageDisplayLabel(patient)}"))
    }

    @Test
    fun judgeFacingScreensKeepOfflineSafetyCopy() {
        val patientListScreen = appSourceFile("ui/PatientListScreen.kt").readText()
        val reviewScreen = appSourceFile("ui/ReviewScreen.kt").readText()
        val summaryScreen = appSourceFile("ui/SummaryScreen.kt").readText()

        assertTrue(patientListScreen.contains("Helping field workers carry care forward."))
        assertTrue(patientListScreen.contains("Note language: \${PatientVisitUiText.noteLanguageName(patient)}"))
        assertTrue(patientListScreen.contains("Check offline setup"))
        assertFalse(patientListScreen.contains("Language set to"))
        assertFalse(patientListScreen.contains("Protocol-grounded"))
        assertTrue(reviewScreen.contains("Smriti does not diagnose. Health worker must review before saving."))
        assertTrue(reviewScreen.contains("No referral flag"))
        assertTrue(reviewScreen.contains("No urgent danger signs were flagged from this note."))
        assertTrue(reviewScreen.contains("How was this prepared?"))
        assertTrue(reviewScreen.contains("Confirm and save"))
        assertFalse(reviewScreen.contains("Protocol-grounded"))
        assertFalse(reviewScreen.contains("Protocol Citation"))
        assertFalse(reviewScreen.contains("Confirmed local data only"))
        assertTrue(summaryScreen.contains("Saved visits on this device"))
        assertTrue(summaryScreen.contains("Today's priority list"))
        assertTrue(summaryScreen.contains("How was this prepared?"))
        listOf(
            "Protocol-grounded",
            "RealGemma context",
            "Protocol Citation",
            "Confirmed local data only"
        ).forEach { forbidden ->
            assertFalse("Found forbidden summary wording: $forbidden", summaryScreen.contains(forbidden))
        }
    }

    @Test
    fun reviewScreenOnlyShowsReferralSuggestedWhenReferralFlagExists() {
        val reviewScreen = appSourceFile("ui/ReviewScreen.kt").readText()
        val referralSupportIndex = reviewScreen.indexOf("Text(\"Referral suggested\"")
        val referralGuardIndex = reviewScreen.indexOf("if (referralFlag != null)")
        val routineCardIndex = reviewScreen.indexOf("Text(\"No referral flag\"")

        assertTrue(referralGuardIndex >= 0)
        assertTrue(referralSupportIndex > referralGuardIndex)
        assertTrue(routineCardIndex > referralSupportIndex)
    }

    @Test
    fun rosterUsesReadableFullWidthActionsAndMovesProofBehindSetup() {
        val patientListScreen = appSourceFile("ui/PatientListScreen.kt").readText()
        val welcomeScreens = appSourceFile("ui/WelcomeScreens.kt").readText()
        val mainActivity = appSourceFile("MainActivity.kt").readText()

        assertTrue(patientListScreen.contains("SmritiPrimaryButton(\"Add patient\""))
        assertTrue(patientListScreen.contains("SmritiTonalButton(\"End-of-day summary\""))
        assertTrue(patientListScreen.contains("text = if (isImportingSupervisorRegister) \"Importing...\" else \"Import register\""))
        assertTrue(patientListScreen.contains("Text(\"Check offline setup\")"))
        assertTrue(patientListScreen.contains("Text(\"About Smriti\")"))
        assertTrue(patientListScreen.contains(".heightIn(min = 48.dp)"))
        assertFalse(patientListScreen.contains("OfflineProofCard("))
        assertTrue(welcomeScreens.contains("OfflineProofCard(status = status)"))
        assertTrue(mainActivity.contains("SmritiScreen.OfflineSetup"))
        assertFalse(patientListScreen.contains("Load Demo Supervisor Register"))
    }

    @Test
    fun visitScreenShowsPhaseBSignalsBeforeHistoryAndTranscript() {
        val visitScreen = appSourceFile("ui/VisitScreen.kt").readText()

        val alertIndex = visitScreen.indexOf("MissedFollowUpCard(")
        val signalIndex = visitScreen.indexOf("HistorySignalCard(signal = signal)")
        val instructionIndex = visitScreen.indexOf("Text(\"Today's observation\"")
        val transcriptIndex = visitScreen.indexOf("Text(if (isListeningOfflineSpeech) \"Listening...\" else \"Speak observation\")")
        val historyIndex = visitScreen.indexOf("PriorHistorySection(")

        assertTrue(alertIndex >= 0)
        assertTrue(signalIndex >= 0)
        assertTrue(instructionIndex > alertIndex)
        assertTrue(transcriptIndex > instructionIndex)
        assertTrue(historyIndex > transcriptIndex)
        assertTrue(visitScreen.contains("history.take(2)"))
        assertTrue(visitScreen.contains("Show patient history"))
    }

    @Test
    fun offlineSpeechHookDoesNotGenerateOrSaveVisit() {
        val visitScreen = appSourceFile("ui/VisitScreen.kt").readText()
        val speechBlockStart = visitScreen.indexOf("fun tryOfflineSpeech()")
        val speechBlockEnd = visitScreen.indexOf("fun requestGenerate", startIndex = speechBlockStart)
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
        val sampleButtonStart = visitScreen.indexOf("Text(\"Use sample visit transcript\")")
        assertTrue(sampleButtonStart >= 0)
        val outlinedButtonStart = visitScreen.lastIndexOf("OutlinedButton(", startIndex = sampleButtonStart)
        assertTrue(outlinedButtonStart >= 0)
        val sampleButtonBlock = visitScreen.substring(outlinedButtonStart, sampleButtonStart)

        assertTrue(sampleButtonBlock.contains("observationText = VisitSampleTranscripts.forPatient(patient)"))
        assertTrue(sampleButtonBlock.contains("offlineSpeechStatus = null"))
    }

    @Test
    fun generateStillUsesEditableTranscriptText() {
        val visitScreen = appSourceFile("ui/VisitScreen.kt").readText()

        assertTrue(visitScreen.contains("onGenerate(observationText, null)"))
        assertTrue(visitScreen.contains("enabled = !isGenerating"))
        assertTrue(visitScreen.contains("Please speak or type today's visit observation first."))
        assertTrue(visitScreen.contains("This observation is very short."))
    }

    @Test
    fun appFacingUiRequiresRealGemmaAndNeverConstructsMockAgents() {
        val mainActivity = appSourceFile("MainActivity.kt").readText()
        val visitScreen = appSourceFile("ui/VisitScreen.kt").readText()
        val patientListScreen = appSourceFile("ui/PatientListScreen.kt").readText()
        val summaryScreen = appSourceFile("ui/SummaryScreen.kt").readText()

        assertTrue(mainActivity.contains("realGemmaRequiredBuildGate: Boolean = BuildConfig.REAL_GEMMA_SUBMISSION_MODE"))
        assertTrue(mainActivity.contains("RealGemmaDeveloperMode.isLocalGateEnabled(context.filesDir)"))
        assertTrue(mainActivity.contains("RealGemmaRequiredMode.evaluate"))
        assertTrue(mainActivity.contains("RealGemmaRequiredAgentFactory.createVisitAgent"))
        assertFalse(mainActivity.contains("MockGemmaAgent("))
        assertFalse(mainActivity.contains("GemmaAgentFactory.create(AgentConfig.DEFAULT_MODE)"))
        assertFalse(mainActivity.contains("summaryAgent = remember"))
        assertFalse(visitScreen.contains("REAL_GEMMA_EXPERIMENTAL"))
        assertFalse(patientListScreen.contains("REAL_GEMMA_EXPERIMENTAL"))
        assertFalse(summaryScreen.contains("REAL_GEMMA_EXPERIMENTAL"))
        assertFalse(visitScreen.contains("RealGemmaAgent("))
        assertFalse(patientListScreen.contains("RealGemmaAgent("))
        assertFalse(summaryScreen.contains("RealGemmaAgent("))
    }

    @Test
    fun realGemmaRequiredModeStillUsesVisitReasoningPipeline() {
        val mainActivity = appSourceFile("MainActivity.kt").readText()

        assertTrue(mainActivity.contains("visitAgent = remember(realGemmaRequiredModeStatus, modelStatus, sharedRealGemmaTextClient)"))
        assertTrue(mainActivity.contains("sharedTextClient = sharedRealGemmaTextClient"))
        assertTrue(mainActivity.contains("gemmaAgent = visitAgent"))
        assertTrue(mainActivity.contains("visitReasoningPipeline.process("))
        assertTrue(mainActivity.contains("RealGemmaUnavailableResult.retryMessageFor(reasoningResult)"))
    }

    @Test
    fun localReasoningStatusPreloadsWithoutNotLoadedCopy() {
        val mainActivity = appSourceFile("MainActivity.kt").readText()

        assertTrue(mainActivity.contains("RealGemmaEnginePreloadState.PREPARING"))
        assertTrue(mainActivity.contains("RealGemmaEnginePreloadState.READY"))
        assertTrue(mainActivity.contains("SmritiLatencyLogger.mark(\"realGemmaPreloadStart\")"))
        assertTrue(mainActivity.contains("realGemmaPreloadSkipped finalRecordingUi=true"))
        assertTrue(mainActivity.contains("\"Loads on demand\""))
        assertFalse(mainActivity.contains("\"Found, not loaded\""))
    }

    @Test
    fun finalRecordingModeUsesRealGemmaLifecyclePolicy() {
        val mainActivity = appSourceFile("MainActivity.kt").readText()
        val buildGradle = File("app/build.gradle.kts")
            .takeIf { it.exists() }
            ?: File("build.gradle.kts").takeIf { it.exists() }
            ?: File("../app/build.gradle.kts")

        assertTrue(mainActivity.contains("recycleRealGemmaEngineAfterVisitNote: Boolean = BuildConfig.RECYCLE_REAL_GEMMA_ENGINE_AFTER_VISIT_NOTE"))
        assertTrue(mainActivity.contains("RealGemmaLifecyclePolicy("))
        assertTrue(mainActivity.contains("freshConversationForVisitNote = true"))
        assertTrue(mainActivity.contains("recycleEngineAfterVisitNote = recycleRealGemmaEngineAfterVisitNote"))
        assertTrue(buildGradle.readText().contains("smriti.recycleRealGemmaEngineAfterVisitNote"))
    }

    @Test
    fun summaryScreenCanDisplayPriorityQueueAndFallbackMessage() {
        val summaryScreen = appSourceFile("ui/SummaryScreen.kt").readText()
        val mainActivity = appSourceFile("MainActivity.kt").readText()

        assertTrue(summaryScreen.contains("Today's priority list"))
        assertTrue(summaryScreen.contains("priorityUnavailableMessage"))
        assertTrue(summaryScreen.contains("priorityQueue"))
        assertTrue(summaryScreen.contains("On-device priority summary unavailable. Showing saved local visit flags."))
        assertFalse(mainActivity.contains("createSupervisorPriorityGenerator"))
        assertFalse(mainActivity.contains("SupervisorPriorityQueueResult"))
        assertFalse(summaryScreen.contains("deterministic local summary"))
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
