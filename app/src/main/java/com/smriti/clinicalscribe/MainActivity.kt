package com.smriti.clinicalscribe

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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
import com.smriti.clinicalscribe.data.DemoSeedData
import com.smriti.clinicalscribe.data.Patient
import com.smriti.clinicalscribe.data.ReferralFlag
import com.smriti.clinicalscribe.data.TranscriptSource
import com.smriti.clinicalscribe.data.VisitLog
import com.smriti.clinicalscribe.export.JsonExporter
import com.smriti.clinicalscribe.rag.ProtocolRetriever
import com.smriti.clinicalscribe.reasoning.AgentConfig
import com.smriti.clinicalscribe.reasoning.AgentMode
import com.smriti.clinicalscribe.reasoning.GemmaAgent
import com.smriti.clinicalscribe.reasoning.GemmaAgentFactory
import com.smriti.clinicalscribe.reasoning.LiteRtEngineConfigFactory
import com.smriti.clinicalscribe.reasoning.ModelAvailability
import com.smriti.clinicalscribe.reasoning.RealGemmaReadinessEvaluator
import com.smriti.clinicalscribe.reasoning.SupervisorSummary
import com.smriti.clinicalscribe.reasoning.VisitReasoningResult
import com.smriti.clinicalscribe.tts.AndroidVoiceOutput
import com.smriti.clinicalscribe.tts.VoiceOutputResult
import com.smriti.clinicalscribe.ui.OfflineProofStatus
import com.smriti.clinicalscribe.ui.PatientListScreen
import com.smriti.clinicalscribe.ui.ReviewScreen
import com.smriti.clinicalscribe.ui.SummaryScreen
import com.smriti.clinicalscribe.ui.VisitScreen
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

private sealed interface SmritiScreen {
    data object PatientRoster : SmritiScreen
    data class Visit(val patient: Patient) : SmritiScreen
    data class Review(
        val patient: Patient,
        val result: VisitReasoningResult,
        val voiceNote: VoiceNoteMetadata?
    ) : SmritiScreen
    data class Summary(val summary: SupervisorSummary) : SmritiScreen
}

@Composable
private fun SmritiApp(
    database: AppDatabase,
    agentMode: AgentMode = AgentConfig.DEFAULT_MODE,
    agent: GemmaAgent = GemmaAgentFactory.create(agentMode)
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val retriever = remember { ProtocolRetriever.fromAsset(context) }
    val jsonExporter = remember { JsonExporter.appPrivate(context) }
    val voiceOutput = remember { AndroidVoiceOutput(context) }
    val modelAvailability = remember { ModelAvailability.fromFilesDir(context.filesDir) }
    val modelStatus = remember { modelAvailability.check() }
    val engineConfigFactory = remember { LiteRtEngineConfigFactory() }
    val readinessEvaluator = remember { RealGemmaReadinessEvaluator() }
    val realGemmaReadiness = remember(agentMode, modelStatus) {
        readinessEvaluator.evaluate(
            agentMode = agentMode,
            modelStatus = modelStatus,
            engineConfigPreparation = engineConfigFactory.prepare(modelStatus)
        )
    }
    val offlineProofStatus = remember(agentMode, modelStatus, realGemmaReadiness) {
        OfflineProofStatus(
            reasoningModeLabel = agentMode.displayName,
            realGemmaModelStatusLabel = modelStatus.proofLabel,
            realGemmaReadinessLabel = realGemmaReadiness.judgeLabel
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

    DisposableEffect(voiceOutput) {
        onDispose {
            voiceOutput.release()
        }
    }

    suspend fun refreshLocalState() {
        patients = database.patientDao().getAll()
        visits = database.visitLogDao().getAll()
        referrals = database.referralFlagDao().getAll()
    }

    suspend fun resetDemoData() {
        database.referralFlagDao().deleteAll()
        database.visitLogDao().deleteAll()
        database.patientDao().upsertAll(DemoSeedData.patients)
        database.protocolChunkDao().upsertAll(retriever.allChunks())
        DemoSeedData.initialVisitLogs().forEach { database.visitLogDao().insert(it) }
        refreshLocalState()
    }

    LaunchedEffect(Unit) {
        isLoading = true
        runCatching {
            if (database.patientDao().getAll().isEmpty()) {
                database.patientDao().upsertAll(DemoSeedData.patients)
            }
            if (database.protocolChunkDao().getAll().isEmpty()) {
                database.protocolChunkDao().upsertAll(retriever.allChunks())
            }
            if (database.visitLogDao().getAll().isEmpty()) {
                DemoSeedData.initialVisitLogs().forEach { database.visitLogDao().insert(it) }
            }
            refreshLocalState()
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
                        onPatientSelected = { patient ->
                            errorMessage = null
                            currentScreen = SmritiScreen.Visit(patient)
                        },
                        onShowSummary = {
                            scope.launch {
                                val summary = agent.generateSupervisorSummary(patients, visits, referrals)
                                currentScreen = SmritiScreen.Summary(summary)
                            }
                        }
                    )

                    is SmritiScreen.Visit -> VisitScreen(
                        patient = screen.patient,
                        history = visits.filter { it.patientId == screen.patient.id },
                        isGenerating = isGenerating,
                        audioPermissionGranted = audioPermissionGranted,
                        errorMessage = errorMessage,
                        onRequestAudioPermission = {
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        onGenerate = { observation, voiceNote ->
                            scope.launch {
                                isGenerating = true
                                errorMessage = null
                                exportVisitPath = null
                                ttsStatusMessage = null
                                runCatching {
                                    val protocolChunks = retriever.retrieve(observation)
                                    agent.generateVisitNote(
                                        patient = screen.patient,
                                        visitHistory = visits.filter { it.patientId == screen.patient.id },
                                        observationText = observation,
                                        protocolChunks = protocolChunks
                                    )
                                }.onSuccess { result ->
                                    currentScreen = SmritiScreen.Review(screen.patient, result, voiceNote)
                                }.onFailure { error ->
                                    errorMessage = "Could not generate local visit note: ${error.message}"
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
                                    val visitId = database.visitLogDao().insert(
                                        VisitLog(
                                            patientId = screen.patient.id,
                                            visitDateMillis = System.currentTimeMillis(),
                                            observationText = screen.result.observationText,
                                            structuredNote = editedNote,
                                            protocolCitation = screen.result.protocolCitation,
                                            suggestedFollowUp = editedFollowUp,
                                            confirmed = true,
                                            audioFilePath = screen.voiceNote?.audioFilePath,
                                            audioDurationSeconds = screen.voiceNote?.audioDurationSeconds,
                                            transcriptSource = if (screen.voiceNote == null) {
                                                TranscriptSource.SIMULATED
                                            } else {
                                                TranscriptSource.REAL_ASR_PENDING
                                            }
                                        )
                                    )
                                    screen.result.referralFlag?.let { flag ->
                                        database.referralFlagDao().insert(flag.copy(visitLogId = visitId))
                                    }
                                    refreshLocalState()
                                    agent.generateSupervisorSummary(patients, visits, referrals)
                                }.onSuccess { summary ->
                                    currentScreen = SmritiScreen.Summary(summary)
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
                                    resetDemoData()
                                    agent.generateSupervisorSummary(
                                        database.patientDao().getAll(),
                                        database.visitLogDao().getAll(),
                                        database.referralFlagDao().getAll()
                                    )
                                }.onSuccess { summary ->
                                    currentScreen = SmritiScreen.Summary(summary)
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
                    if (currentScreen is SmritiScreen.PatientRoster) {
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
