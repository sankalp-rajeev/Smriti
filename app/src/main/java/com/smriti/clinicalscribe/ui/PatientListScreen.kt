package com.smriti.clinicalscribe.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smriti.clinicalscribe.data.Patient
import com.smriti.clinicalscribe.data.PatientLanguages
import com.smriti.clinicalscribe.data.VisitLog

@Composable
fun PatientListScreen(
    patients: List<Patient>,
    visits: List<VisitLog>,
    isLoading: Boolean,
    offlineProofStatus: OfflineProofStatus,
    importStatusMessage: String?,
    isImportingSupervisorRegister: Boolean,
    onPatientSelected: (Patient) -> Unit,
    onAddPatient: () -> Unit,
    onImportSupervisorRegister: () -> Unit,
    onShowSummary: () -> Unit
) {
    var showImportDialog by remember { mutableStateOf(false) }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Supervisor Register") },
            text = { Text("Import 6 synthetic patients from local supervisor register?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImportDialog = false
                        onImportSupervisorRegister()
                    }
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Smriti", style = MaterialTheme.typography.headlineMedium)
                    Text("Offline CHW visit copilot", style = MaterialTheme.typography.labelLarge)
                    Text("Local patient memory + local protocol pack.", style = MaterialTheme.typography.bodyMedium)
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onAddPatient,
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Text("Add Patient")
                    }
                    OutlinedButton(
                        onClick = onShowSummary,
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Text("End-of-Day Summary")
                    }
                    OutlinedButton(
                        onClick = { showImportDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        enabled = !isImportingSupervisorRegister
                    ) {
                        Text(if (isImportingSupervisorRegister) "Importing..." else "Import Supervisor Register")
                    }
                    importStatusMessage?.let { message ->
                        Text(message, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (isLoading) {
                Text("Loading local patient roster...")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("Works offline; no cloud API required for core runtime.", fontWeight = FontWeight.SemiBold)
                                Text("Protocol-grounded referral support, not diagnosis.")
                                Text("CHW reviews and confirms before saving.")
                            }
                        }
                    }
                    item {
                        OfflineProofCard(status = offlineProofStatus, compact = true)
                    }
                    item {
                        Text("Patient List", style = MaterialTheme.typography.titleMedium)
                    }
                    items(patients) { patient ->
                        val visitCount = visits.count { it.patientId == patient.id }
                        PatientRow(
                            patient = patient,
                            visitCount = visitCount,
                            onPatientSelected = onPatientSelected
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PatientRow(
    patient: Patient,
    visitCount: Int,
    onPatientSelected: (Patient) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(patient.displayLabel(), style = MaterialTheme.typography.titleLarge)
            Text(
                text = "${patient.age}F - ${patient.pregnancyWeeks ?: "-"} weeks - ${patient.village}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${patient.countryCode} / ${patient.preferredLanguage} - ${patient.riskSummary}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Output language: ${PatientLanguages.forPatient(patient).displayLabel}",
                style = MaterialTheme.typography.labelLarge
            )
            patient.scenarioPreview.takeIf { it.isNotBlank() }?.let { preview ->
                Text(preview, style = MaterialTheme.typography.labelLarge)
            }
            Text(
                text = "$visitCount saved visit(s)",
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(onClick = { onPatientSelected(patient) }) {
                Text("Select Patient and View History")
            }
        }
    }
}
