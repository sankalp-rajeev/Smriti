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
import com.smriti.clinicalscribe.reasoning.AgentConfig
import com.smriti.clinicalscribe.reasoning.GemmaAgentFactory
import com.smriti.clinicalscribe.reasoning.LiteRtEngineConfigFactory
import com.smriti.clinicalscribe.reasoning.ModelAvailability
import com.smriti.clinicalscribe.reasoning.RealGemmaReadinessEvaluator
import com.smriti.clinicalscribe.reasoning.RealGemmaDeveloperAgentFactory
import com.smriti.clinicalscribe.reasoning.RealGemmaDeveloperMode
import com.smriti.clinicalscribe.reasoning.SupervisorSummary
import com.smriti.clinicalscribe.reasoning.SupervisorPriorityQueue
import com.smriti.clinicalscribe.reasoning.SupervisorPriorityQueueGenerator
import com.smriti.clinicalscribe.reasoning.SupervisorPriorityQueueResult
import com.smriti.clinicalscribe.reasoning.RealGemmaSubmissionMode
import com.smriti.clinicalscribe.reasoning.RealGemmaUnavailableResult
import com.smriti.clinicalscribe.reasoning.RealGemmaDeveloperTextClient
import com.smriti.clinicalscribe.reasoning.RealGemmaAgent
import com.smriti.clinicalscribe.reasoning.VisitReasoningResult
import com.smriti.clinicalscribe.transcript.SimulatedTranscriptClient
import com.smriti.clinicalscribe.tts.AndroidVoiceOutput
import com.smriti.clinicalscribe.tts.VoiceOutputResult
import com.smriti.clinicalscribe.ui.AddPatientScreen
import com.smriti.clinicalscribe.ui.OfflineProofStatus
import com.smriti.clinicalscribe.ui.PatientListScreen
import com.smriti.clinicalscribe.ui.ReviewScreen
import com.smriti.clinicalscribe.ui.SummaryScreen
import com.smriti.clinicalscribe.ui.VisitScreen
import java.util.Calendar
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = AppDatabase.getInstance(applicationContext)

        setContent {
            SmritiApp(database = database)
        }
    }
}

private fun startOfTodayMillis(): Long {
    return Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private sealed interface SmritiScreen {
    data object PatientRoster : SmritiScreen
    data object AddPatient : SmritiScreen
    data class Visit(val patient: Patient) : SmritiScreen
    data class Review(
        val patient: Patient,
        val result: VisitReasoningResult,
        val voiceNote: VoiceNoteMetadata?
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
    realGemmaDevBuildGate: Boolean = BuildConfig.DEBUG && BuildConfig.REAL_GEMMA_DEV_BUILD_GATE,
    realGemmaSubmissionBuildGate: Boolean = BuildConfig.REAL_GEMMA_SUBMISSION_MODE,
    realGemmaLocalGateOverride: Boolean? = null
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val retriever = remember { ProtocolRetriever.fromAsset(context) }
    val visitMemoryStore = remember(database) { LocalVisitMemoryStore(database) }
    val jsonExporter = remember { JsonExporter.appPrivate(context) }
    val voiceOutput = remember { AndroidVoiceOutput(context) }
    val modelAvailability = remember { ModelAvailability.fromFilesDir(context.filesDir) }
    val modelStatus = remember { modelAvailability.check() }
    val detectedRealGemmaLocalGate = remember { RealGemmaDeveloperMode.isLocalGateEnabled(context.filesDir) }
    val realGemmaLocalGate = realGemmaLocalGateOverride ?: detectedRealGemmaLocalGate
    val realGemmaDeveloperModeStatus = remember(realGemmaDevBuildGate, realGemmaLocalGate, modelStatus) {
        RealGemmaDeveloperMode.evaluate(
            buildTimeGateEnabled = realGemmaDevBuildGate,
            localGateEnabled = realGemmaLocalGate,
            modelStatus = modelStatus
        )
    }
    val realGemmaSubmissionModeStatus = remember(realGemmaSubmissionBuildGate, realGemmaLocalGate, modelStatus) {
        RealGemmaSubmissionMode.evaluate(
            buildTimeGateEnabled = realGemmaSubmissionBuildGate,
            localGateEnabled = realGemmaLocalGate,
            modelStatus = modelStatus
        )
    }
    val visitAgent = remember(realGemmaSubmissionModeStatus, realGemmaDeveloperModeStatus, modelStatus) {
        when {
            realGemmaSubmissionModeStatus.usesRealGemmaVisitAgent ->
                RealGemmaAgent(textClient = RealGemmaDeveloperTextClient(modelStatus))
            else -> RealGemmaDeveloperAgentFactory.createVisitAgent(realGemmaDeveloperModeStatus, modelStatus)
        }
    }
    val summaryAgent = remember { GemmaAgentFactory.create(AgentConfig.DEFAULT_MODE) }
    val visitReasoningPipeline = remember(visitAgent, retriever) {
        VisitReasoningPipeline(
            protocolRetriever = retriever,
            gemmaAgent = visitAgent,
            speechToTextClient = SimulatedTranscriptClient()
        )
    }
    val engineConfigFactory = remember { LiteRtEngineConfigFactory() }
    val readinessEvaluator = remember { RealGemmaReadinessEvaluator() }
    val activeAgentMode = if (realGemmaSubmissionModeStatus.usesRealGemmaVisitAgent) {
        com.smriti.clinicalscribe.reasoning.AgentMode.REAL_GEMMA_EXPERIMENTAL
    } else {
        realGemmaDeveloperModeStatus.activeAgentMode
    }
    val realGemmaReadiness = remember(activeAgentMode, modelStatus) {
        readinessEvaluator.evaluate(
            agentMode = activeAgentMode,
            modelStatus = modelStatus,
            engineConfigPreparation = engineConfigFactory.prepare(modelStatus)
        )
    }
    val offlineProofStatus = remember(realGemmaDeveloperModeStatus, realGemmaSubmissionModeStatus, modelStatus, realGemmaReadiness) {
        val submissionStatusVisible = realGemmaSubmissionModeStatus.isRequested
        val inferenceLabel = if (submissionStatusVisible) {
            realGemmaSubmissionModeStatus.inferenceStatusLabel
        } else {
            realGemmaDeveloperModeStatus.inferenceStatusLabel
        }
        val reasoningModeLabel = if (submissionStatusVisible) {
            realGemmaSubmissionModeStatus.reasoningModeLabel
        } else {
            realGemmaDeveloperModeStatus.reasoningModeLabel
        }
        val warning = realGemmaSubmissionModeStatus.warning
            ?: realGemmaDeveloperModeStatus.developerWarning
        OfflineProofStatus(
            reasoningModeLabel = reasoningModeLabel,
            realGemmaModelStatusLabel = modelStatus.proofLabel,
            realGemmaReadinessLabel = if (realGemmaSubmissionModeStatus.isFullyActive) {
                "Submission text inference enabled"
            } else if (realGemmaDeveloperModeStatus.inferenceEnabled) {
                "Developer text inference enabled"
            } else {
                realGemmaReadiness.judgeLabel
            },
            realGemmaInferenceLabel = inferenceLabel,
            realGemmaGateLabel = if (submissionStatusVisible) {
                realGemmaSubmissionModeStatus.gateStatusLabel
            } else {
                realGemmaDeveloperModeStatus.gateStatusLabel
            },
            realGemmaTextModeLabel = if (realGemmaSubmissionModeStatus.isFullyActive) {
                realGemmaSubmissionModeStatus.realGemmaTextModeLabel
            } else if (realGemmaDeveloperModeStatus.inferenceEnabled) {
                "ACTIVE"
            } else {
                "Disabled"
            },
            realGemmaSubmissionModeLabel = realGemmaSubmissionModeStatus.submissionModeLabel,
            realGemmaDeveloperWarning = warning
        )
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

    var currentScreen by remember { mutableStateOf<SmritiScreen>(SmritiScreen.PatientRoster) }
    var patients by remember { mutableStateOf<List<Patient>>(emptyList()) }
    var visits by remember { mutableStateOf<List<VisitLog>>(emptyList()) }
    var referrals by remember { mutableStateOf<List<ReferralFlag>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isGenerating by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var isResettingDemoData by remember { mutableStateOf(false) }
    var isImportingSupervisorRegister by remember { mutableStateOf(false) }
    var importStatusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var ttsStatusMessage by remember { mutableStateOf<String?>(null) }
    var exportVisitPath by remember { mutableStateOf<String?>(null) }
    var exportSummaryPath by remember { mutableStateOf<String?>(null) }

    fun speakOffline(text: String) {
        ttsStatusMessage = when (val result = voiceOutput.speak(text)) {
            VoiceOutputResult.Started -> "Reading aloud offline with Android TTS."
            is VoiceOutputResult.Unavailable -> "TTS unavailable: ${result.reason}"
        }
    }

    fun applySnapshot(snapshot: VisitMemorySnapshot) {
        patients = snapshot.patients
        visits = snapshot.visits
        referrals = snapshot.referrals
    }

    suspend fun buildSummaryScreen(
        summaryPatients: List<Patient>,
        summaryVisits: List<VisitLog>,
        summaryReferrals: List<ReferralFlag>
    ): SmritiScreen.Summary {
        val deterministicSummary = summaryAgent.generateSupervisorSummary(
            summaryPatients,
            summaryVisits,
            summaryReferrals
        )
        if (!realGemmaSubmissionModeStatus.isFullyActive) {
            return SmritiScreen.Summary(summary = deterministicSummary)
        }

        val missedFollowUps = summaryPatients.flatMap { patient ->
            PatientMemoryInsights.missedFollowUpAlerts(
                patientId = patient.id,
                visits = summaryVisits
            )
        }
        val historySignals = summaryPatients.mapNotNull { patient ->
            PatientMemoryInsights.risingBloodPressureSignal(
                patient = patient,
                visits = summaryVisits
            )
        }
        val todayVisits = summaryVisits.filter { visit ->
            visit.confirmed && visit.visitDateMillis >= startOfTodayMillis()
        }
        val priorityGenerator = SupervisorPriorityQueueGenerator(
            textClient = RealGemmaDeveloperTextClient(modelStatus)
        )
        return when (val priority = priorityGenerator.generate(
            patients = summaryPatients,
            todayVisits = todayVisits,
            referrals = summaryReferrals,
            missedFollowUps = missedFollowUps,
            historySignals = historySignals
        )) {
            is SupervisorPriorityQueueResult.Available -> SmritiScreen.Summary(
                summary = deterministicSummary,
                priorityQueue = priority.queue
            )
            is SupervisorPriorityQueueResult.Unavailable -> SmritiScreen.Summary(
                summary = deterministicSummary,
                priorityUnavailableMessage = priority.reason
            )
        }
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

    MaterialTheme {
        Scaffold { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (val screen = currentScreen) {
                    SmritiScreen.PatientRoster -> PatientListScreen(
                        patients = patients,
                        visits = visits,
                        isLoading = isLoading,
                        offlineProofStatus = offlineProofStatus,
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
                                runCatching {
                                    val register = DemoSupervisorRegisterImporter.fromAsset(context)
                                    visitMemoryStore.importSupervisorRegister(register)
                                }.onSuccess { result ->
                                    applySnapshot(result.snapshot)
                                    importStatusMessage =
                                        "${result.patientCount} synthetic patients imported from local supervisor register."
                                }.onFailure { error ->
                                    errorMessage = "Could not import local supervisor register: ${error.message}"
                                }
                                isImportingSupervisorRegister = false
                            }
                        },
                        onShowSummary = {
                            scope.launch {
                                currentScreen = buildSummaryScreen(patients, visits, referrals)
                            }
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

                    is SmritiScreen.Visit -> VisitScreen(
                        patient = screen.patient,
                        history = visitMemoryStore.historyForPatient(
                            VisitMemorySnapshot(patients, visits, referrals),
                            screen.patient.id
                        ),
                        isGenerating = isGenerating,
                        audioPermissionGranted = audioPermissionGranted,
                        errorMessage = errorMessage,
                        reasoningModeLabel = offlineProofStatus.reasoningModeLabel,
                        realGemmaModelStatusLabel = modelStatus.proofLabel,
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
                                isGenerating = true
                                errorMessage = null
                                exportVisitPath = null
                                ttsStatusMessage = null
                                runCatching {
                                    val history = visitMemoryStore.historyForPatient(
                                        VisitMemorySnapshot(patients, visits, referrals),
                                        screen.patient.id
                                    )
                                    val pipelineResult = visitReasoningPipeline.process(
                                        VisitPipelineInput(
                                            patient = screen.patient,
                                            priorVisits = history,
                                            transcriptText = observation,
                                            protocolContext = screen.patient.protocolContext()
                                        )
                                    )
                                    val reasoningResult = pipelineResult.reasoningResult ?: error(
                                        pipelineResult.unavailableReason
                                            ?: "Transcript text is required before visit reasoning."
                                    )
                                    if (
                                        realGemmaSubmissionModeStatus.isFullyActive &&
                                        RealGemmaUnavailableResult.isUnavailable(reasoningResult)
                                    ) {
                                        error(RealGemmaUnavailableResult.RETRY_MESSAGE)
                                    }
                                    reasoningResult
                                }.onSuccess { result ->
                                    currentScreen = SmritiScreen.Review(screen.patient, result, voiceNote)
                                }.onFailure { error ->
                                    errorMessage = if (error.message == RealGemmaUnavailableResult.RETRY_MESSAGE) {
                                        RealGemmaUnavailableResult.RETRY_MESSAGE
                                    } else {
                                        "Could not generate local visit note: ${error.message}"
                                    }
                                }
                                isGenerating = false
                            }
                        },
                        onBack = { currentScreen = SmritiScreen.PatientRoster }
                    )

                    is SmritiScreen.Review -> ReviewScreen(
                        patient = screen.patient,
                        result = screen.result,
                        voiceNote = screen.voiceNote,
                        isSaving = isSaving,
                        ttsStatusMessage = ttsStatusMessage,
                        exportVisitPath = exportVisitPath,
                        onReadReferralSuggestion = {
                            val referral = screen.result.referralFlag
                            if (referral == null) {
                                ttsStatusMessage = "No referral suggestion is available to read aloud."
                            } else {
                                speakOffline("${referral.urgency} referral suggestion. ${referral.reason}. Protocol citation: ${referral.protocolBasis}.")
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
                        onConfirmSave = { editedNote, editedFollowUp ->
                            scope.launch {
                                isSaving = true
                                errorMessage = null
                                runCatching {
                                    val snapshot = visitMemoryStore.saveConfirmedVisit(
                                        result = screen.result,
                                        editedNote = editedNote,
                                        editedFollowUp = editedFollowUp,
                                        voiceNote = screen.voiceNote
                                    )
                                    applySnapshot(snapshot)
                                    buildSummaryScreen(snapshot.patients, snapshot.visits, snapshot.referrals)
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

                    is SmritiScreen.Summary -> SummaryScreen(
                        summary = screen.summary,
                        priorityQueue = screen.priorityQueue,
                        priorityUnavailableMessage = screen.priorityUnavailableMessage,
                        isResettingDemoData = isResettingDemoData,
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
