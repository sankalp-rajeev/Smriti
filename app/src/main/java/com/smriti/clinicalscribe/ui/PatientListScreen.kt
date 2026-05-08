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
import androidx.compose.material3.MaterialTheme
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

@Composable
fun PatientListScreen(
    patients: List<Patient>,
    visits: List<VisitLog>,
    referrals: List<ReferralFlag>,
    isLoading: Boolean,
    importStatusMessage: String?,
    isImportingSupervisorRegister: Boolean,
    onPatientSelected: (Patient) -> Unit,
    onAddPatient: () -> Unit,
    onImportSupervisorRegister: () -> Unit,
    onShowSummary: () -> Unit,
    onUserGuide: () -> Unit,
    onCheckOfflineSetup: () -> Unit
) {
    var showImportDialog by remember { mutableStateOf(false) }
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

    SmritiScreenSurface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(SmritiSpacing.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(SmritiSpacing.CardGap)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Smriti", style = MaterialTheme.typography.headlineMedium)
                    Text("Offline health visit assistant", style = MaterialTheme.typography.bodyLarge)
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search patient") },
                singleLine = true
            )

            SmritiPrimaryButton("Add patient", onAddPatient, enabled = !isLoading)
            SmritiTonalButton("End-of-day summary", onShowSummary, enabled = !isLoading)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                SmritiSecondaryButton(
                    text = if (isImportingSupervisorRegister) "Importing..." else "Import register",
                    onClick = { showImportDialog = true },
                    modifier = Modifier.weight(1f),
                    enabled = !isImportingSupervisorRegister
                )
                SmritiSecondaryButton("User guide", onUserGuide, modifier = Modifier.weight(1f))
            }
            TextButton(onClick = onCheckOfflineSetup, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Check offline setup")
            }
            importStatusMessage?.let {
                SmritiStatusChip(it, tone = SmritiTone.Info)
            }

            if (isLoading) {
                SmritiCard(tone = SmritiTone.Info) {
                    Text("Loading local patient roster...", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
    SmritiCard {
        Text("No patients yet.", style = MaterialTheme.typography.titleLarge)
        Text("Import a patient register or add your first patient.", style = MaterialTheme.typography.bodyLarge)
        SmritiPrimaryButton("Add patient", onAddPatient)
        SmritiSecondaryButton("Import register", onImportRegister)
    }
}

@Composable
private fun SearchEmptyState(
    query: String,
    onAddPatient: () -> Unit
) {
    SmritiCard {
        Text("No patient found for '$query'", style = MaterialTheme.typography.titleMedium)
        Text("Check the spelling or add a new patient.", style = MaterialTheme.typography.bodyLarge)
        SmritiSecondaryButton("Add patient", onAddPatient)
    }
}

@Composable
private fun SectionHeader(label: String) {
    SmritiSectionHeader(label)
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
    SmritiCard {
        Text(patient.displayLabel(), style = MaterialTheme.typography.titleLarge)
        Text(PatientVisitUiText.gestationLabel(patient), style = MaterialTheme.typography.bodyLarge)
        Text(PatientVisitUiText.countryVillage(patient), style = MaterialTheme.typography.bodyLarge)
        Text(
            text = "Note language: ${PatientVisitUiText.noteLanguageName(patient)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            chips.forEach { chip -> PatientChip(chip) }
        }
        Text("$visitCount history entr${if (visitCount == 1) "y" else "ies"}", style = MaterialTheme.typography.bodyMedium)
        SmritiPrimaryButton("Open visit", onClick = { onPatientSelected(patient) })
    }
}

@Composable
private fun PatientChip(chip: PatientStatusChip) {
    val tone = when (chip.tone) {
        PatientChipTone.Urgent -> SmritiTone.Urgent
        PatientChipTone.Caution -> SmritiTone.Caution
        PatientChipTone.Routine -> SmritiTone.Success
    }
    SmritiStatusChip(chip.label, tone = tone)
}
