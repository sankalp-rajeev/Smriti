package com.smriti.clinicalscribe.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smriti.clinicalscribe.data.Patient
import com.smriti.clinicalscribe.data.ReferralFlag
import com.smriti.clinicalscribe.data.VisitLog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientListScreen(
    patients: List<Patient>,
    visits: List<VisitLog>,
    referrals: List<ReferralFlag>,
    isLoading: Boolean,
    offlineProofStatus: OfflineProofStatus,
    importStatusMessage: String?,
    isImportingSupervisorRegister: Boolean,
    selectedLanguageCode: String,
    languageStatusMessage: String?,
    onLanguageSelected: (String) -> Unit,
    onPatientSelected: (Patient) -> Unit,
    onAddPatient: () -> Unit,
    onImportSupervisorRegister: () -> Unit,
    onShowSummary: () -> Unit,
    onUserGuide: () -> Unit,
    onCheckOfflineSetup: () -> Unit
) {
    var showImportDialog by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val sortedPatients = remember(patients, visits, referrals) {
        PatientRosterUiLogic.sortPatients(patients, visits, referrals)
    }
    val filteredPatients = remember(sortedPatients, query) {
        PatientRosterUiLogic.filterPatients(sortedPatients, query)
    }
    val attentionPatients = filteredPatients.filter {
        PatientRosterUiLogic.attentionRank(it, visits, referrals) < 4
    }
    val routinePatients = filteredPatients.filter {
        PatientRosterUiLogic.attentionRank(it, visits, referrals) >= 4
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Add patients from file?") },
            text = { Text("This will add patients from the imported file. Existing patients will not be deleted.") },
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

    if (showLanguageSheet) {
        ModalBottomSheet(onDismissRequest = { showLanguageSheet = false }) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Choose language", style = MaterialTheme.typography.titleLarge)
                LanguageChoices.options.forEach { option ->
                    OutlinedButton(
                        onClick = {
                            onLanguageSelected(option.code)
                            showLanguageSheet = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Text(option.label)
                    }
                }
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Smriti", style = MaterialTheme.typography.headlineMedium)
                    Text("Offline health visit assistant", style = MaterialTheme.typography.bodyLarge)
                }
                OutlinedButton(
                    onClick = { showLanguageSheet = true },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(LanguageChoices.labelFor(selectedLanguageCode))
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search patient") },
                singleLine = true
            )

            Button(
                onClick = onAddPatient,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
            ) {
                Text("Add patient")
            }
            OutlinedButton(
                onClick = onShowSummary,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text("End-of-day summary")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { showImportDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    enabled = !isImportingSupervisorRegister
                ) {
                    Text(if (isImportingSupervisorRegister) "Importing..." else "Import register")
                }
                OutlinedButton(
                    onClick = onUserGuide,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Text("User guide")
                }
            }
            TextButton(onClick = onCheckOfflineSetup, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Check offline setup")
            }
            importStatusMessage?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            languageStatusMessage?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }

            if (isLoading) {
                Text("Loading local patient roster...", style = MaterialTheme.typography.bodyLarge)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        OfflineProofCard(status = offlineProofStatus, compact = true)
                    }
                    when {
                        patients.isEmpty() -> item {
                            EmptyRosterState(
                                onAddPatient = onAddPatient,
                                onImportRegister = { showImportDialog = true }
                            )
                        }
                        filteredPatients.isEmpty() -> item {
                            SearchEmptyState(query = query, onAddPatient = onAddPatient)
                        }
                        else -> {
                            if (attentionPatients.isNotEmpty()) {
                                item { SectionHeader("Needs attention") }
                                items(attentionPatients) { patient ->
                                    PatientRow(
                                        patient = patient,
                                        visits = visits,
                                        referrals = referrals,
                                        onPatientSelected = onPatientSelected
                                    )
                                }
                            }
                            if (routinePatients.isNotEmpty()) {
                                item { SectionHeader("Routine visits") }
                                items(routinePatients) { patient ->
                                    PatientRow(
                                        patient = patient,
                                        visits = visits,
                                        referrals = referrals,
                                        onPatientSelected = onPatientSelected
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyRosterState(
    onAddPatient: () -> Unit,
    onImportRegister: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("No patients yet.", style = MaterialTheme.typography.titleLarge)
            Text("Import a patient register or add your first patient.", style = MaterialTheme.typography.bodyLarge)
            Button(
                onClick = onAddPatient,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text("Add patient")
            }
            OutlinedButton(
                onClick = onImportRegister,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text("Import register")
            }
        }
    }
}

@Composable
private fun SearchEmptyState(
    query: String,
    onAddPatient: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("No patient found for '$query'", style = MaterialTheme.typography.titleMedium)
            Text("Check the spelling or add a new patient.", style = MaterialTheme.typography.bodyLarge)
            OutlinedButton(
                onClick = onAddPatient,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text("Add patient")
            }
        }
    }
}

@Composable
private fun SectionHeader(label: String) {
    Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun PatientRow(
    patient: Patient,
    visits: List<VisitLog>,
    referrals: List<ReferralFlag>,
    onPatientSelected: (Patient) -> Unit
) {
    val visitCount = visits.count { it.patientId == patient.id }
    val chips = PatientRosterUiLogic.statusChips(patient, visits, referrals)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(patient.displayLabel(), style = MaterialTheme.typography.titleLarge)
            Text(PatientVisitUiText.gestationLabel(patient), style = MaterialTheme.typography.bodyLarge)
            Text(PatientVisitUiText.countryVillage(patient), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "Output language: ${PatientVisitUiText.outputLanguageLabel(patient)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                chips.forEach { chip -> PatientChip(chip) }
            }
            Text("$visitCount saved visit(s)", style = MaterialTheme.typography.bodyMedium)
            Button(
                onClick = { onPatientSelected(patient) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text("Open visit")
            }
        }
    }
}

@Composable
private fun PatientChip(chip: PatientStatusChip) {
    val color = when (chip.tone) {
        PatientChipTone.Urgent -> MaterialTheme.colorScheme.errorContainer
        PatientChipTone.Caution -> Color(0xFFFFE3B0)
        PatientChipTone.Routine -> Color(0xFFDCEEDB)
    }
    Text(
        text = chip.label,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .background(color, MaterialTheme.shapes.small)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}
