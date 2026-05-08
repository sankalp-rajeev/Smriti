package com.smriti.clinicalscribe

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import com.smriti.clinicalscribe.BuildConfig
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.smriti.clinicalscribe.audio.VoiceNoteMetadata
import com.smriti.clinicalscribe.data.AppDatabase
import com.smriti.clinicalscribe.data.DemoSupervisorRegisterImporter
import com.smriti.clinicalscribe.data.LocalVisitMemoryStore
import com.smriti.clinicalscribe.data.Patient
import com.smriti.clinicalscribe.data.PatientMemoryInsights
import com.smriti.clinicalscribe.data.ReferralFlag
import com.smriti.clinicalscribe.data.VisitLog
import com.smriti.clinicalscribe.data.VisitMemorySnapshot
import com.smriti.clinicalscribe.export.JsonExporter
import com.smriti.clinicalscribe.pipeline.VisitPipelineInput
import com.smriti.clinicalscribe.pipeline.VisitReasoningPipeline
import com.smriti.clinicalscribe.rag.ProtocolRetriever
import com.smriti.clinicalscribe.reasoning.LiteRtEngineConfigFactory
import com.smriti.clinicalscribe.reasoning.ModelAvailability
import com.smriti.clinicalscribe.reasoning.ModelStatusKind
import com.smriti.clinicalscribe.reasoning.RealGemmaReadinessEvaluator
import com.smriti.clinicalscribe.reasoning.RealGemmaDeveloperMode
import com.smriti.clinicalscribe.reasoning.SupervisorSummary
import com.smriti.clinicalscribe.reasoning.SupervisorSummaryFormatter
import com.smriti.clinicalscribe.reasoning.SupervisorPriorityQueue
import com.smriti.clinicalscribe.reasoning.RealGemmaUnavailableResult
import com.smriti.clinicalscribe.reasoning.RealGemmaInferenceGate
import com.smriti.clinicalscribe.reasoning.RealGemmaLifecyclePolicy
import com.smriti.clinicalscribe.reasoning.RealGemmaRequiredAgentFactory
import com.smriti.clinicalscribe.reasoning.RealGemmaRequiredMode
import com.smriti.clinicalscribe.reasoning.RealGemmaDeveloperTextClient
import com.smriti.clinicalscribe.reasoning.RealGemmaEnginePreloadState
import com.smriti.clinicalscribe.reasoning.RealGemmaPreloadResult
import com.smriti.clinicalscribe.reasoning.PaperNoteVisionExtraction
import com.smriti.clinicalscribe.reasoning.PaperNoteVisionGenerationResult
import com.smriti.clinicalscribe.reasoning.PaperNoteVisionParseResult
import com.smriti.clinicalscribe.reasoning.PaperNoteVisionParser
import com.smriti.clinicalscribe.reasoning.RealGemmaVisionPaperNoteClient
import com.smriti.clinicalscribe.reasoning.SmritiLatencyLogger
import com.smriti.clinicalscribe.reasoning.VisitReasoningResult
import com.smriti.clinicalscribe.transcript.SimulatedTranscriptClient
import com.smriti.clinicalscribe.tts.AndroidVoiceOutput
import com.smriti.clinicalscribe.tts.VoiceOutputResult
import com.smriti.clinicalscribe.ui.AddPatientScreen
import com.smriti.clinicalscribe.ui.OfflineSetupScreen
import com.smriti.clinicalscribe.ui.OfflineProofStatus
import com.smriti.clinicalscribe.ui.PatientListScreen
import com.smriti.clinicalscribe.ui.ReviewScreen
import com.smriti.clinicalscribe.ui.ReviewScannedNoteScreen
import com.smriti.clinicalscribe.ui.SetupGuidanceScreen
import com.smriti.clinicalscribe.ui.SmritiTheme
import com.smriti.clinicalscribe.ui.SummaryScreen
import com.smriti.clinicalscribe.ui.UserGuideScreen
import com.smriti.clinicalscribe.ui.VisitScreen
import com.smriti.clinicalscribe.ui.WelcomeScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = AppDatabase.getInstance(applicationContext)

        setContent {
            SmritiApp(database = database)
        }
    }
}

private fun buildRawLocalSummary(
    summaryPatients: List<Patient>,
    summaryVisits: List<VisitLog>,
    summaryReferrals: List<ReferralFlag>
): SupervisorSummary {
    return SupervisorSummaryFormatter.buildLocalSavedSummary(
        patients = summaryPatients,
        visits = summaryVisits,
        referrals = summaryReferrals,
        nowMillis = System.currentTimeMillis()
    )
}

private suspend fun runPaperNoteVisionExtraction(
    imageBytes: ByteArray,
    currentPatient: Patient,
    paperNoteVisionClient: RealGemmaVisionPaperNoteClient,
    paperNoteVisionParser: PaperNoteVisionParser,
    setReading: (Boolean) -> Unit,
    setStatus: (String?) -> Unit,
    setError: (String?) -> Unit,
    openReview: (PaperNoteVisionExtraction) -> Unit
) {
    setReading(true)
    setStatus("Extracting visit details...")
    setError(null)
    when (val generated = paperNoteVisionClient.extractPaperNote(imageBytes)) {
        is PaperNoteVisionGenerationResult.Success -> {
            when (val parsed = paperNoteVisionParser.parse(generated.rawText)) {
                is PaperNoteVisionParseResult.Success -> {
                    openReview(parsed.extraction)
                }
                is PaperNoteVisionParseResult.Rejected -> {
                    setError("Paper note could not be read safely: ${parsed.reason}")
                }
            }
        }
        is PaperNoteVisionGenerationResult.Unavailable -> {
            setError(generated.reason)
        }
        is PaperNoteVisionGenerationResult.Failed -> {
            setError("Paper note extraction failed: ${generated.reason}")
        }
    }
    setStatus(null)
    setReading(false)
}

private fun namesMatch(left: String, right: String): Boolean {
    fun normalize(value: String): String {
        return value.lowercase()
            .filter { it.isLetterOrDigit() || it.isWhitespace() }
            .trim()
            .replace(Regex("\\s+"), " ")
    }
    val normalizedLeft = normalize(left)
    val normalizedRight = normalize(right)
    return normalizedLeft.isNotBlank() && normalizedLeft == normalizedRight
}

private sealed interface SmritiScreen {
    data object Welcome : SmritiScreen
    data object UserGuide : SmritiScreen
    data object SetupGuidance : SmritiScreen
    data object OfflineSetup : SmritiScreen
    data object PatientRoster : SmritiScreen
    data object AddPatient : SmritiScreen
    data class Visit(val patient: Patient) : SmritiScreen
    data class Review(
        val patient: Patient,
        val result: VisitReasoningResult,
        val voiceNote: VoiceNoteMetadata?
    ) : SmritiScreen
    data class ReviewScannedNote(
        val currentPatient: Patient,
        val extraction: PaperNoteVisionExtraction
    ) : SmritiScreen
    data class Summary(
        val summary: SupervisorSummary,
        val priorityQueue: SupervisorPriorityQueue? = null,
        val priorityUnavailableMessage: String? = null
    ) : SmritiScreen
}

@Composable
private fun SmritiApp(
    database: AppDatabase,
    realGemmaRequiredBuildGate: Boolean = BuildConfig.REAL_GEMMA_SUBMISSION_MODE,
    finalRecordingUi: Boolean = BuildConfig.FINAL_RECORDING_UI,
    recycleRealGemmaEngineAfterVisitNote: Boolean = BuildConfig.RECYCLE_REAL_GEMMA_ENGINE_AFTER_VISIT_NOTE,
    realGemmaLocalGateOverride: Boolean? = null
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val retriever = remember { ProtocolRetriever.fromAsset(context) }
    val visitMemoryStore = remember(database) { LocalVisitMemoryStore(database) }
    val jsonExporter = remember { JsonExporter.appPrivate(context) }
    val voiceOutput = remember { AndroidVoiceOutput(context) }
    val modelAvailability = remember { ModelAvailability.fromFilesDir(context.filesDir) }
    val modelStatus = remember {
        SmritiLatencyLogger.measure("modelReadinessCheck") {
            modelAvailability.check()
        }
    }
    val modelFileReady = modelStatus.kind == ModelStatusKind.FOUND_NOT_LOADED
    val detectedRealGemmaLocalGate = remember { RealGemmaDeveloperMode.isLocalGateEnabled(context.filesDir) }
    val realGemmaLocalGate = realGemmaLocalGateOverride ?: detectedRealGemmaLocalGate
    val realGemmaRequiredModeStatus = remember(realGemmaRequiredBuildGate, realGemmaLocalGate, modelStatus) {
        RealGemmaRequiredMode.evaluate(
            buildTimeGateEnabled = realGemmaRequiredBuildGate,
            localGateEnabled = realGemmaLocalGate,
            modelStatus = modelStatus
        )
    }
    val realGemmaLifecyclePolicy = remember(finalRecordingUi, recycleRealGemmaEngineAfterVisitNote) {
        RealGemmaLifecyclePolicy(
            finalRecordingUi = finalRecordingUi,
            freshConversationForVisitNote = true,
            recycleEngineAfterVisitNote = recycleRealGemmaEngineAfterVisitNote
        )
    }
    val sharedRealGemmaTextClient = remember(
        realGemmaRequiredModeStatus,
        modelStatus,
        realGemmaLocalGate,
        realGemmaLifecyclePolicy
    ) {
        if (realGemmaRequiredModeStatus.inferenceEnabled) {
            RealGemmaDeveloperTextClient(
                modelStatus = modelStatus,
                sentinelExists = realGemmaLocalGate,
                lifecyclePolicy = realGemmaLifecyclePolicy
            )
        } else {
            null
        }
    }
    val visitAgent = remember(realGemmaRequiredModeStatus, modelStatus, sharedRealGemmaTextClient) {
        RealGemmaRequiredAgentFactory.createVisitAgent(
            status = realGemmaRequiredModeStatus,
            modelStatus = modelStatus,
            sharedTextClient = sharedRealGemmaTextClient
        )
    }
    val visitReasoningPipeline = remember(visitAgent, retriever) {
        VisitReasoningPipeline(
            protocolRetriever = retriever,
            gemmaAgent = visitAgent,
            speechToTextClient = SimulatedTranscriptClient()
        )
    }
    val paperNoteVisionClient = remember(modelStatus, context.cacheDir.absolutePath, realGemmaLocalGate) {
        RealGemmaVisionPaperNoteClient(
            modelStatus = modelStatus,
            cacheDirPath = context.cacheDir.absolutePath,
            sentinelExists = realGemmaLocalGate
        )
    }
    val paperNoteVisionParser = remember { PaperNoteVisionParser() }
    val engineConfigFactory = remember { LiteRtEngineConfigFactory() }
    val readinessEvaluator = remember { RealGemmaReadinessEvaluator() }
    val activeAgentMode = com.smriti.clinicalscribe.reasoning.AgentMode.REAL_GEMMA_REQUIRED
    var hasSuccessfulRealGemmaGeneration by remember { mutableStateOf(false) }
    var realGemmaPreloadState by remember { mutableStateOf(RealGemmaEnginePreloadState.LOADS_ON_DEMAND) }
    val realGemmaReadiness = remember(activeAgentMode, modelStatus) {
        readinessEvaluator.evaluate(
            agentMode = activeAgentMode,
            modelStatus = modelStatus,
            engineConfigPreparation = engineConfigFactory.prepare(modelStatus)
        )
    }
    val realGemmaEngineStatusLabel = when {
        hasSuccessfulRealGemmaGeneration -> "Loaded"
        realGemmaPreloadState == RealGemmaEnginePreloadState.PREPARING -> "Preparing"
        realGemmaPreloadState == RealGemmaEnginePreloadState.READY -> "Ready"
        realGemmaPreloadState == RealGemmaEnginePreloadState.FAILED -> "Failed"
        else -> "Loads on demand"
    }
    val offlineProofStatus = remember(
        realGemmaRequiredModeStatus,
        modelStatus,
        realGemmaReadiness,
        realGemmaEngineStatusLabel
    ) {
        OfflineProofStatus(
            reasoningModeLabel = realGemmaRequiredModeStatus.reasoningModeLabel,
            realGemmaModelStatusLabel = modelStatus.proofLabel,
            realGemmaReadinessLabel = realGemmaReadiness.judgeLabel,
            realGemmaEngineStatusLabel = realGemmaEngineStatusLabel,
            realGemmaInferenceLabel = realGemmaRequiredModeStatus.inferenceStatusLabel,
            realGemmaGateLabel = realGemmaRequiredModeStatus.gateStatusLabel,
            realGemmaTextModeLabel = realGemmaRequiredModeStatus.textModeLabel,
            realGemmaSubmissionModeLabel = realGemmaRequiredModeStatus.submissionModeLabel,
            realGemmaDeveloperWarning = realGemmaRequiredModeStatus.warning
        )
    }

    LaunchedEffect(finalRecordingUi, recycleRealGemmaEngineAfterVisitNote) {
        SmritiLatencyLogger.mark(
            "realGemmaLifecycle finalRecordingUi=$finalRecordingUi; " +
                "recycleEngineAfterVisitNote=$recycleRealGemmaEngineAfterVisitNote"
        )
    }

    LaunchedEffect(sharedRealGemmaTextClient, finalRecordingUi) {
        val client = sharedRealGemmaTextClient
        if (client == null) {
            realGemmaPreloadState = RealGemmaEnginePreloadState.UNAVAILABLE
            return@LaunchedEffect
        }
        if (finalRecordingUi) {
            realGemmaPreloadState = RealGemmaEnginePreloadState.LOADS_ON_DEMAND
            SmritiLatencyLogger.mark("realGemmaPreloadSkipped finalRecordingUi=true")
            return@LaunchedEffect
        }
        realGemmaPreloadState = RealGemmaEnginePreloadState.PREPARING
        SmritiLatencyLogger.mark("realGemmaPreloadStart")
        when (val result = client.preload()) {
            RealGemmaPreloadResult.Ready -> {
                realGemmaPreloadState = RealGemmaEnginePreloadState.READY
                SmritiLatencyLogger.mark("realGemmaPreloadReady")
            }
            is RealGemmaPreloadResult.Unavailable -> {
                realGemmaPreloadState = RealGemmaEnginePreloadState.LOADS_ON_DEMAND
                SmritiLatencyLogger.mark("realGemmaPreloadUnavailable:${result.reason.take(80)}")
            }
            is RealGemmaPreloadResult.Failed -> {
                realGemmaPreloadState = RealGemmaEnginePreloadState.FAILED
                SmritiLatencyLogger.mark("realGemmaPreloadFailed:${result.reason.take(80)}")
            }
        }
    }
    var audioPermissionGranted by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        audioPermissionGranted = granted
    }
    val firstLaunchPrefs = remember {
        context.getSharedPreferences("smriti_first_launch", android.content.Context.MODE_PRIVATE)
    }
    var currentScreen by remember {
        mutableStateOf<SmritiScreen>(
            if (firstLaunchPrefs.getBoolean("welcome_seen", false)) {
                SmritiScreen.PatientRoster
            } else {
                SmritiScreen.Welcome
            }
        )
    }
    var patients by remember { mutableStateOf<List<Patient>>(emptyList()) }
    var visits by remember { mutableStateOf<List<VisitLog>>(emptyList()) }
    var referrals by remember { mutableStateOf<List<ReferralFlag>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isGenerating by remember { mutableStateOf(false) }
    var generationStatusMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var isReadingPaperNote by remember { mutableStateOf(false) }
    var paperNoteStatusMessage by remember { mutableStateOf<String?>(null) }
    var scannedNoteSaveStatusMessage by remember { mutableStateOf<String?>(null) }
    var isResettingDemoData by remember { mutableStateOf(false) }
    var isImportingSupervisorRegister by remember { mutableStateOf(false) }
    var importStatusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var ttsStatusMessage by remember { mutableStateOf<String?>(null) }
    var exportVisitPath by remember { mutableStateOf<String?>(null) }
    var exportSummaryPath by remember { mutableStateOf<String?>(null) }
    val paperNoteImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        val visitScreen = currentScreen as? SmritiScreen.Visit ?: return@rememberLauncherForActivityResult
        if (uri == null) {
            paperNoteStatusMessage = "No image selected."
            return@rememberLauncherForActivityResult
        }
        if (isGenerating || isReadingPaperNote) {
            errorMessage = RealGemmaInferenceGate.BUSY_MESSAGE
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val bytes = runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("Could not open selected image.")
            }
            bytes.onSuccess { imageBytes ->
                runPaperNoteVisionExtraction(
                    imageBytes = imageBytes,
                    currentPatient = visitScreen.patient,
                    paperNoteVisionClient = paperNoteVisionClient,
                    paperNoteVisionParser = paperNoteVisionParser,
                    setReading = { isReadingPaperNote = it },
                    setStatus = { paperNoteStatusMessage = it },
                    setError = { errorMessage = it },
                    openReview = { extraction ->
                        scannedNoteSaveStatusMessage = null
                        currentScreen = SmritiScreen.ReviewScannedNote(visitScreen.patient, extraction)
                    }
                )
            }.onFailure { error ->
                errorMessage = "Could not read selected paper note image: ${error.message}"
            }
        }
    }

    fun speakOffline(text: String) {
        ttsStatusMessage = when (val result = voiceOutput.speak(text)) {
            VoiceOutputResult.Started -> "Reading note aloud."
            is VoiceOutputResult.Unavailable -> "Voice for this language is not installed on this device."
        }
    }

    fun openVisitsAfterWelcome() {
        firstLaunchPrefs.edit().putBoolean("welcome_seen", true).apply()
        currentScreen = if (!modelFileReady && !firstLaunchPrefs.getBoolean("setup_seen", false)) {
            SmritiScreen.SetupGuidance
        } else {
            SmritiScreen.PatientRoster
        }
    }

    fun applySnapshot(snapshot: VisitMemorySnapshot) {
        patients = snapshot.patients
        visits = snapshot.visits
        referrals = snapshot.referrals
    }

    fun buildSummaryScreen(
        summaryPatients: List<Patient>,
        summaryVisits: List<VisitLog>,
        summaryReferrals: List<ReferralFlag>
    ): SmritiScreen.Summary {
        return SmritiScreen.Summary(
            summary = buildRawLocalSummary(summaryPatients, summaryVisits, summaryReferrals)
        )
    }

    DisposableEffect(voiceOutput) {
        onDispose {
            voiceOutput.release()
        }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        runCatching {
            applySnapshot(visitMemoryStore.seedDemoIfNeeded(retriever.allChunks()))
        }.onFailure { error ->
            errorMessage = "Could not load local demo data: ${error.message}"
        }
        isLoading = false
    }

    SmritiTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (val screen = currentScreen) {
                    SmritiScreen.Welcome -> WelcomeScreen(
                        onStartVisits = { openVisitsAfterWelcome() },
                        onUserGuide = { currentScreen = SmritiScreen.UserGuide },
                        onCheckOfflineSetup = {
                            currentScreen = SmritiScreen.OfflineSetup
                        }
                    )

                    SmritiScreen.UserGuide -> UserGuideScreen(
                        onBack = {
                            currentScreen = if (firstLaunchPrefs.getBoolean("welcome_seen", false)) {
                                SmritiScreen.PatientRoster
                            } else {
                                SmritiScreen.Welcome
                            }
                        }
                    )

                    SmritiScreen.SetupGuidance -> SetupGuidanceScreen(
                        onContinueWithoutModel = {
                            firstLaunchPrefs.edit().putBoolean("setup_seen", true).apply()
                                errorMessage = null
                                currentScreen = SmritiScreen.PatientRoster
                        },
                        onBack = {
                            currentScreen = if (firstLaunchPrefs.getBoolean("welcome_seen", false)) {
                                SmritiScreen.PatientRoster
                            } else {
                                SmritiScreen.Welcome
                            }
                        }
                    )

                    SmritiScreen.OfflineSetup -> OfflineSetupScreen(
                        status = offlineProofStatus,
                        onBack = {
                            currentScreen = if (firstLaunchPrefs.getBoolean("welcome_seen", false)) {
                                SmritiScreen.PatientRoster
                            } else {
                                SmritiScreen.Welcome
                            }
                        }
                    )

                    SmritiScreen.PatientRoster -> PatientListScreen(
                        patients = patients,
                        visits = visits,
                        referrals = referrals,
                        isLoading = isLoading,
                        importStatusMessage = importStatusMessage,
                        isImportingSupervisorRegister = isImportingSupervisorRegister,
                        onPatientSelected = { patient ->
                            errorMessage = null
                            currentScreen = SmritiScreen.Visit(patient)
                        },
                        onAddPatient = {
                            errorMessage = null
                            currentScreen = SmritiScreen.AddPatient
                        },
                        onImportSupervisorRegister = {
                            scope.launch {
                                isImportingSupervisorRegister = true
                                errorMessage = null
                                importStatusMessage = null
                                try {
                                    val result = withContext(Dispatchers.IO) {
                                        val register = DemoSupervisorRegisterImporter.fromAsset(context)
                                        visitMemoryStore.importSupervisorRegister(register)
                                    }
                                    applySnapshot(result.snapshot)
                                    importStatusMessage = "Register imported on this device."
                                } catch (_: Throwable) {
                                    importStatusMessage = null
                                    errorMessage =
                                        "Register could not be imported. Please try again."
                                } finally {
                                    isImportingSupervisorRegister = false
                                }
                            }
                        },
                        onShowSummary = {
                            currentScreen = buildSummaryScreen(patients, visits, referrals)
                        },
                        onUserGuide = { currentScreen = SmritiScreen.UserGuide },
                        onCheckOfflineSetup = {
                            currentScreen = SmritiScreen.OfflineSetup
                        }
                    )

                    SmritiScreen.AddPatient -> AddPatientScreen(
                        audioPermissionGranted = audioPermissionGranted,
                        onRequestAudioPermission = {
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        onSavePatient = { patient ->
                            scope.launch {
                                errorMessage = null
                                runCatching {
                                    visitMemoryStore.addPatient(patient)
                                }.onSuccess { snapshot ->
                                    applySnapshot(snapshot)
                                    importStatusMessage = "Patient added locally. Review before using in a visit."
                                    currentScreen = SmritiScreen.PatientRoster
                                }.onFailure { error ->
                                    errorMessage = "Could not save patient locally: ${error.message}"
                                }
                            }
                        },
                        onBack = { currentScreen = SmritiScreen.PatientRoster }
                    )

                    is SmritiScreen.Visit -> {
                        val visitMemorySnapshot = VisitMemorySnapshot(patients, visits, referrals)
                        val visitHistory = remember(screen.patient.id, visits, referrals) {
                            SmritiLatencyLogger.measure(
                                label = "patientHistoryLoadFormat",
                                scenario = screen.patient.id
                            ) {
                                visitMemoryStore.historyForPatient(
                                    visitMemorySnapshot,
                                    screen.patient.id
                                )
                            }
                        }
                        VisitScreen(
                            patient = screen.patient,
                            history = visitHistory,
                            isGenerating = isGenerating,
                            generationStatusMessage = generationStatusMessage,
                            audioPermissionGranted = audioPermissionGranted,
                            errorMessage = errorMessage,
                            reasoningModeLabel = offlineProofStatus.reasoningModeLabel,
                            realGemmaModelStatusLabel = modelStatus.proofLabel,
                            realGemmaEngineStatusLabel = offlineProofStatus.realGemmaEngineStatusLabel,
                            realGemmaInferenceLabel = offlineProofStatus.realGemmaInferenceLabel,
                            realGemmaDeveloperWarning = offlineProofStatus.realGemmaDeveloperWarning,
                            protocolContextLabel = screen.patient.protocolContextLabel(),
                            missedFollowUpAlerts = PatientMemoryInsights.missedFollowUpAlerts(
                                patientId = screen.patient.id,
                                visits = visits
                            ),
                            historySignal = PatientMemoryInsights.risingBloodPressureSignal(
                                patient = screen.patient,
                                visits = visits
                            ),
                            isReadingPaperNote = isReadingPaperNote,
                            paperNoteStatusMessage = paperNoteStatusMessage,
                            showDemoControls = !finalRecordingUi,
                            onRequestAudioPermission = {
                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            },
                            onMarkFollowUpConfirmed = { visitId ->
                                scope.launch {
                                    errorMessage = null
                                    runCatching {
                                        visitMemoryStore.markFollowUpConfirmed(visitId)
                                    }.onSuccess { snapshot ->
                                        applySnapshot(snapshot)
                                    }.onFailure { error ->
                                        errorMessage = "Could not update follow-up status: ${error.message}"
                                    }
                                }
                            },
                            onGenerate = { observation, voiceNote ->
                                scope.launch {
                                    if (isGenerating || isReadingPaperNote) return@launch
                                    isGenerating = true
                                    generationStatusMessage = "Reading patient history..."
                                    errorMessage = null
                                    exportVisitPath = null
                                    ttsStatusMessage = null
                                    runCatching {
                                        generationStatusMessage = "Checking local health guidance..."
                                        generationStatusMessage = "Running on-device Gemma 4 reasoning..."
                                        val pipelineResult = visitReasoningPipeline.process(
                                            VisitPipelineInput(
                                                patient = screen.patient,
                                                priorVisits = visitHistory,
                                                transcriptText = observation,
                                                protocolContext = screen.patient.protocolContext()
                                            )
                                        )
                                        val reasoningResult = pipelineResult.reasoningResult ?: error(
                                            pipelineResult.unavailableReason
                                                ?: "Transcript text is required before visit reasoning."
                                        )
                                        if (RealGemmaUnavailableResult.isUnavailable(reasoningResult)) {
                                            error(RealGemmaUnavailableResult.retryMessageFor(reasoningResult))
                                        }
                                        generationStatusMessage = "Preparing note for review..."
                                        reasoningResult
                                    }.onSuccess { result ->
                                        hasSuccessfulRealGemmaGeneration = true
                                        SmritiLatencyLogger.mark("reviewScreenNavigation", screen.patient.id)
                                        currentScreen = SmritiScreen.Review(screen.patient, result, voiceNote)
                                    }.onFailure { error ->
                                        val detail = error.message.orEmpty()
                                        errorMessage = if (detail.contains(RealGemmaInferenceGate.BUSY_MESSAGE)) {
                                            RealGemmaInferenceGate.BUSY_MESSAGE
                                        } else {
                                            "On-device reasoning was unavailable. Please check the model is installed, then try again."
                                        }
                                    }
                                    isGenerating = false
                                    generationStatusMessage = null
                                }
                            },
                            onScanPaperNote = scanPaper@{
                                if (isGenerating || isReadingPaperNote) {
                                    errorMessage = RealGemmaInferenceGate.BUSY_MESSAGE
                                    return@scanPaper
                                }
                                errorMessage = null
                                paperNoteStatusMessage = null
                                paperNoteImageLauncher.launch("image/*")
                            },
                            onUseSamplePaperNote = samplePaper@{
                                if (isGenerating || isReadingPaperNote) {
                                    errorMessage = RealGemmaInferenceGate.BUSY_MESSAGE
                                    return@samplePaper
                                }
                                scope.launch {
                                    val sampleBytes = runCatching {
                                        context.assets.open("demo/sample_paper_visit_note.png").use { it.readBytes() }
                                    }
                                    sampleBytes.onSuccess { imageBytes ->
                                        runPaperNoteVisionExtraction(
                                            imageBytes = imageBytes,
                                            currentPatient = screen.patient,
                                            paperNoteVisionClient = paperNoteVisionClient,
                                            paperNoteVisionParser = paperNoteVisionParser,
                                            setReading = { isReadingPaperNote = it },
                                            setStatus = { paperNoteStatusMessage = it },
                                            setError = { errorMessage = it },
                                            openReview = { extraction ->
                                                scannedNoteSaveStatusMessage = null
                                                currentScreen = SmritiScreen.ReviewScannedNote(screen.patient, extraction)
                                            }
                                        )
                                    }.onFailure { error ->
                                        errorMessage = "Could not load sample paper note: ${error.message}"
                                    }
                                }
                            },
                            onBack = {
                                errorMessage = null
                                paperNoteStatusMessage = null
                                currentScreen = SmritiScreen.PatientRoster
                            }
                        )
                    }

                    is SmritiScreen.Review -> ReviewScreen(
                        patient = screen.patient,
                        result = screen.result,
                        voiceNote = screen.voiceNote,
                        priorVisitCount = visits.count { it.patientId == screen.patient.id },
                        isSaving = isSaving,
                        ttsStatusMessage = ttsStatusMessage,
                        exportVisitPath = exportVisitPath,
                        onReadReferralSuggestion = {
                            val referral = screen.result.referralFlag
                            if (referral == null) {
                                ttsStatusMessage = "No referral suggestion is available to read aloud."
                            } else {
                                speakOffline("${referral.urgency} referral suggestion. ${referral.reason}. ${screen.result.suggestedFollowUp}.")
                            }
                        },
                        onExportVisitJson = { editedNote, editedFollowUp ->
                            runCatching {
                                jsonExporter.exportVisit(
                                    result = screen.result,
                                    editedNote = editedNote,
                                    editedFollowUp = editedFollowUp,
                                    voiceNote = screen.voiceNote
                                )
                            }.onSuccess { file ->
                                exportVisitPath = file.absolutePath
                            }.onFailure { error ->
                                exportVisitPath = "Export failed: ${error.message}"
                            }
                        },
                        onConfirmSave = saveVisit@{ editedNote, editedFollowUp ->
                            if (isSaving) return@saveVisit
                            isSaving = true
                            scope.launch {
                                errorMessage = null
                                runCatching {
                                    val saveStarted = System.currentTimeMillis()
                                    val snapshot = withContext(Dispatchers.IO) {
                                        visitMemoryStore.saveConfirmedVisit(
                                            result = screen.result,
                                            editedNote = editedNote,
                                            editedFollowUp = editedFollowUp,
                                            voiceNote = screen.voiceNote
                                        )
                                    }
                                    SmritiLatencyLogger.log(
                                        label = "confirmSaveRoomWrite",
                                        durationMillis = System.currentTimeMillis() - saveStarted,
                                        scenario = screen.patient.id
                                    )
                                    applySnapshot(snapshot)
                                    val summaryStarted = System.currentTimeMillis()
                                    val summary = SmritiScreen.Summary(
                                        summary = buildRawLocalSummary(
                                            snapshot.patients,
                                            snapshot.visits,
                                            snapshot.referrals
                                        )
                                    )
                                    SmritiLatencyLogger.log(
                                        label = "summaryRefresh",
                                        durationMillis = System.currentTimeMillis() - summaryStarted,
                                        scenario = screen.patient.id
                                    )
                                    summary
                                }.onSuccess { summary ->
                                    currentScreen = summary
                                }.onFailure { error ->
                                    errorMessage = "Could not save confirmed visit: ${error.message}"
                                }
                                isSaving = false
                            }
                        },
                        onBack = { currentScreen = SmritiScreen.Visit(screen.patient) }
                    )

                    is SmritiScreen.ReviewScannedNote -> {
                        val matchedPatient = patients.firstOrNull { patient ->
                            namesMatch(patient.name, screen.extraction.patientName)
                        }
                        ReviewScannedNoteScreen(
                            currentPatient = screen.currentPatient,
                            matchedPatient = matchedPatient,
                            extraction = screen.extraction,
                            isSaving = isSaving,
                            saveStatusMessage = scannedNoteSaveStatusMessage,
                            onSave = saveScanned@{ targetPatient, editedPatientName, editedVisitDate, editedBloodPressure, editedSymptoms, editedFollowUpPlan ->
                                if (isSaving) return@saveScanned
                                isSaving = true
                                scope.launch {
                                    errorMessage = null
                                    runCatching {
                                        val snapshot = withContext(Dispatchers.IO) {
                                            visitMemoryStore.saveConfirmedScannedPaperNote(
                                                patientId = targetPatient.id,
                                                extraction = screen.extraction,
                                                editedPatientName = editedPatientName,
                                                editedVisitDate = editedVisitDate,
                                                editedBloodPressure = editedBloodPressure,
                                                editedSymptoms = editedSymptoms,
                                                editedFollowUpPlan = editedFollowUpPlan
                                            )
                                        }
                                        applySnapshot(snapshot)
                                    }.onSuccess {
                                        scannedNoteSaveStatusMessage = "Saved to patient history."
                                        currentScreen = SmritiScreen.Visit(targetPatient)
                                    }.onFailure { error ->
                                        errorMessage = "Could not save scanned note: ${error.message}"
                                    }
                                    isSaving = false
                                }
                            },
                            onCancel = { currentScreen = SmritiScreen.Visit(screen.currentPatient) }
                        )
                    }

                    is SmritiScreen.Summary -> SummaryScreen(
                        summary = screen.summary,
                        priorityQueue = screen.priorityQueue,
                        priorityUnavailableMessage = screen.priorityUnavailableMessage,
                        isResettingDemoData = isResettingDemoData,
                        showDemoControls = !finalRecordingUi,
                        offlineProofStatus = offlineProofStatus,
                        ttsStatusMessage = ttsStatusMessage,
                        exportSummaryPath = exportSummaryPath,
                        onReadSummary = {
                            speakOffline(
                                "${screen.summary.narrative} Total visits: ${screen.summary.totalVisits}. Referral flags: ${screen.summary.referralsFlagged}."
                            )
                        },
                        onExportSummaryJson = {
                            runCatching {
                                jsonExporter.exportSummary(screen.summary)
                            }.onSuccess { file ->
                                exportSummaryPath = file.absolutePath
                            }.onFailure { error ->
                                exportSummaryPath = "Export failed: ${error.message}"
                            }
                        },
                        onResetDemoData = {
                            scope.launch {
                                isResettingDemoData = true
                                errorMessage = null
                                exportSummaryPath = null
                                runCatching {
                                    val snapshot = visitMemoryStore.resetDemoData(retriever.allChunks())
                                    applySnapshot(snapshot)
                                    importStatusMessage = "Reset Demo Data restored the six-patient synthetic roster."
                                    buildSummaryScreen(snapshot.patients, snapshot.visits, snapshot.referrals)
                                }.onSuccess { summary ->
                                    currentScreen = summary
                                }.onFailure { error ->
                                    errorMessage = "Could not reset demo data: ${error.message}"
                                }
                                isResettingDemoData = false
                            }
                        },
                        onBack = { currentScreen = SmritiScreen.PatientRoster }
                    )
                }

                errorMessage?.let { message ->
                    if (currentScreen is SmritiScreen.PatientRoster || currentScreen is SmritiScreen.AddPatient) {
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
