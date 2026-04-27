package com.smriti.clinicalscribe

import android.os.Bundle
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.smriti.clinicalscribe.data.AppDatabase
import com.smriti.clinicalscribe.data.DemoSeedData
import com.smriti.clinicalscribe.data.Patient
import com.smriti.clinicalscribe.data.ReferralFlag
import com.smriti.clinicalscribe.data.VisitLog
import com.smriti.clinicalscribe.rag.ProtocolRetriever
import com.smriti.clinicalscribe.reasoning.GemmaAgent
import com.smriti.clinicalscribe.reasoning.MockGemmaAgent
import com.smriti.clinicalscribe.reasoning.SupervisorSummary
import com.smriti.clinicalscribe.reasoning.VisitReasoningResult
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
    data class Review(val patient: Patient, val result: VisitReasoningResult) : SmritiScreen
    data class Summary(val summary: SupervisorSummary) : SmritiScreen
}

@Composable
private fun SmritiApp(
    database: AppDatabase,
    agent: GemmaAgent = MockGemmaAgent()
) {
    val scope = rememberCoroutineScope()
    val retriever = remember { ProtocolRetriever() }

    var currentScreen by remember { mutableStateOf<SmritiScreen>(SmritiScreen.PatientRoster) }
    var patients by remember { mutableStateOf<List<Patient>>(emptyList()) }
    var visits by remember { mutableStateOf<List<VisitLog>>(emptyList()) }
    var referrals by remember { mutableStateOf<List<ReferralFlag>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isGenerating by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    suspend fun refreshLocalState() {
        patients = database.patientDao().getAll()
        visits = database.visitLogDao().getAll()
        referrals = database.referralFlagDao().getAll()
    }

    LaunchedEffect(Unit) {
        isLoading = true
        runCatching {
            if (database.patientDao().getAll().isEmpty()) {
                database.patientDao().upsertAll(DemoSeedData.patients)
            }
            if (database.protocolChunkDao().getAll().isEmpty()) {
                database.protocolChunkDao().upsertAll(DemoSeedData.protocolChunks)
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
                        errorMessage = errorMessage,
                        onGenerate = { observation ->
                            scope.launch {
                                isGenerating = true
                                errorMessage = null
                                runCatching {
                                    val protocolChunks = retriever.retrieve(observation)
                                    agent.generateVisitNote(
                                        patient = screen.patient,
                                        visitHistory = visits.filter { it.patientId == screen.patient.id },
                                        observationText = observation,
                                        protocolChunks = protocolChunks
                                    )
                                }.onSuccess { result ->
                                    currentScreen = SmritiScreen.Review(screen.patient, result)
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
                        isSaving = isSaving,
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
                                            confirmed = true
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
