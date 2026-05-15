package com.smriti.clinicalscribe.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.smriti.clinicalscribe.data.FollowUpTask
import com.smriti.clinicalscribe.data.FollowUpTaskStatus
import com.smriti.clinicalscribe.data.Patient
import com.smriti.clinicalscribe.data.ReferralFlag
import com.smriti.clinicalscribe.data.VisitLog

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PatientListScreen(
    patients: List<Patient>,
    visits: List<VisitLog>,
    referrals: List<ReferralFlag>,
    followUpTasks: List<FollowUpTask>,
    isLoading: Boolean,
    importStatusMessage: String?,
    isImportingSupervisorRegister: Boolean,
    onPatientSelected: (Patient) -> Unit,
    onAddPatient: () -> Unit,
    onImportSupervisorRegister: () -> Unit,
    onUrgentProtocolLookup: () -> Unit,
    onShowCommunityPanel: () -> Unit,
    onShowSummary: () -> Unit,
    onUserGuide: () -> Unit,
    onAboutSmriti: () -> Unit,
    onCheckOfflineSetup: () -> Unit
) {
    var showImportDialog by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(RosterFilter.All) }
    val sortedPatients = remember(patients, visits, referrals, followUpTasks) {
        PatientRosterUiLogic.sortPatients(patients, visits, referrals, followUpTasks = followUpTasks)
    }
    val filteredPatients = remember(sortedPatients, query, selectedFilter, visits, referrals, followUpTasks) {
        PatientRosterUiLogic.filterPatients(sortedPatients, query)
            .filter { patient ->
                patient.matchesRosterFilter(selectedFilter, visits, referrals, followUpTasks)
            }
    }
    val attentionPatients = filteredPatients.filter {
        PatientRosterUiLogic.attentionRank(it, visits, referrals, followUpTasks = followUpTasks) < 6
    }
    val routinePatients = filteredPatients.filter {
        PatientRosterUiLogic.attentionRank(it, visits, referrals, followUpTasks = followUpTasks) >= 6
    }
    val needsAttentionCount = sortedPatients.count {
        PatientRosterUiLogic.attentionRank(it, visits, referrals, followUpTasks = followUpTasks) < 6
    }
    val openFollowUpCount = followUpTasks.count { it.status in FollowUpTaskStatus.ACTIVE }
    val savedVisitCount = visits.count { it.confirmed }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Add patients from supervisor file") },
            text = {
                Text(
                    "Import patient register shared by your supervisor. Patient register is stored on this device. Existing patients will not be deleted. No internet needed after setup."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImportDialog = false
                        onImportSupervisorRegister()
                    }
                ) {
                    Text("Import patient register")
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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(SmritiSpacing.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Smriti", style = MaterialTheme.typography.headlineMedium)
                    Text("Daily patient memory for field visits.", style = MaterialTheme.typography.bodyLarge)
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmritiSectionHeader("Today's focus")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        SmritiMetricTile(
                            label = "Needs attention",
                            value = needsAttentionCount.toString(),
                            tone = if (needsAttentionCount > 0) SmritiTone.Caution else SmritiTone.Success,
                            modifier = Modifier.weight(1f)
                        )
                        SmritiMetricTile(
                            label = "Follow-up due",
                            value = openFollowUpCount.toString(),
                            tone = if (openFollowUpCount > 0) SmritiTone.Caution else SmritiTone.Muted,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        SmritiMetricTile(
                            label = "Saved on this device",
                            value = patients.size.toString(),
                            tone = SmritiTone.Info,
                            modifier = Modifier.weight(1f)
                        )
                        SmritiMetricTile(
                            label = "Saved visits",
                            value = savedVisitCount.toString(),
                            tone = SmritiTone.Muted,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search patient") },
                    singleLine = true
                )
            }

            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RosterFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter.label) }
                        )
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmritiSectionHeader("Main actions")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        SmritiTonalButton("Community panel", onShowCommunityPanel, modifier = Modifier.weight(1f), enabled = !isLoading)
                        SmritiSecondaryButton("End-of-day summary", onShowSummary, modifier = Modifier.weight(1f), enabled = !isLoading)
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmritiSectionHeader("Setup actions")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        SmritiSecondaryButton(
                            text = if (isImportingSupervisorRegister) "Importing..." else "Import patient register",
                            onClick = { showImportDialog = true },
                            modifier = Modifier.weight(1f),
                            enabled = !isImportingSupervisorRegister
                        )
                        SmritiSecondaryButton("Add patient", onAddPatient, modifier = Modifier.weight(1f), enabled = !isLoading)
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmritiSectionHeader("Support actions")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        SmritiSecondaryButton("Urgent lookup", onUrgentProtocolLookup, modifier = Modifier.weight(1f), enabled = !isLoading)
                        SmritiSecondaryButton("User guide", onUserGuide, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        SmritiSecondaryButton("About Smriti", onAboutSmriti, modifier = Modifier.weight(1f))
                        SmritiSecondaryButton("Check offline setup", onCheckOfflineSetup, modifier = Modifier.weight(1f))
                    }
                }
            }

            importStatusMessage?.let { message ->
                item {
                    SmritiCard(tone = SmritiTone.Info) {
                        Text("Import patient register", fontWeight = FontWeight.SemiBold)
                        Text(message, style = MaterialTheme.typography.bodyLarge)
                        Text("Review imported patients on the roster.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (isLoading) {
                item {
                    SmritiCard(tone = SmritiTone.Info) {
                        Text("Loading local patient roster...", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            } else {
                when {
                    patients.isEmpty() -> item {
                        EmptyRosterState(
                            onAddPatient = onAddPatient,
                            onImportRegister = { showImportDialog = true }
                        )
                    }
                    filteredPatients.isEmpty() -> item {
                        SearchEmptyState(query = query, selectedFilter = selectedFilter, onAddPatient = onAddPatient)
                    }
                    else -> {
                        if (attentionPatients.isNotEmpty()) {
                            item { SectionHeader("Needs attention") }
                            items(attentionPatients) { patient ->
                                PatientRow(
                                    patient = patient,
                                    visits = visits,
                                    referrals = referrals,
                                    followUpTasks = followUpTasks,
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
                                    followUpTasks = followUpTasks,
                                    onPatientSelected = onPatientSelected
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Saved on this device: ${patients.size} patients, $savedVisitCount visits.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
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
        Text("Import a patient register shared by your supervisor or add your first patient.", style = MaterialTheme.typography.bodyLarge)
        SmritiPrimaryButton("Add patient", onAddPatient)
        SmritiSecondaryButton("Import patient register", onImportRegister)
    }
}

@Composable
private fun SearchEmptyState(
    query: String,
    selectedFilter: RosterFilter,
    onAddPatient: () -> Unit
) {
    SmritiCard {
        val title = if (query.isBlank()) {
            "No patients in ${selectedFilter.label.lowercase()}."
        } else {
            "No patient found for '$query'"
        }
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text("Try another filter, check the spelling, or add a new patient.", style = MaterialTheme.typography.bodyLarge)
        SmritiSecondaryButton("Add patient", onAddPatient)
    }
}

@Composable
private fun SectionHeader(label: String) {
    SmritiSectionHeader(label)
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun PatientRow(
    patient: Patient,
    visits: List<VisitLog>,
    referrals: List<ReferralFlag>,
    followUpTasks: List<FollowUpTask>,
    onPatientSelected: (Patient) -> Unit
) {
    val visitCount = visits.count { it.patientId == patient.id }
    val chips = PatientRosterUiLogic.statusChips(patient, visits, referrals, followUpTasks = followUpTasks)
    val cardTone = when {
        chips.any { it.tone == PatientChipTone.Urgent } -> SmritiTone.Urgent
        chips.any { it.tone == PatientChipTone.Caution } -> SmritiTone.Caution
        else -> SmritiTone.Default
    }
    SmritiCard(tone = cardTone) {
        Text(patient.displayLabel(), style = MaterialTheme.typography.titleLarge)
        Text(PatientVisitUiText.gestationLabel(patient), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Text(PatientVisitUiText.countryVillage(patient), style = MaterialTheme.typography.bodyLarge)
        Text(
            text = "Language: ${PatientVisitUiText.noteLanguageName(patient)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            chips.forEach { chip -> PatientChip(chip) }
        }
        Text("$visitCount history entr${if (visitCount == 1) "y" else "ies"} saved on this device", style = MaterialTheme.typography.bodyMedium)
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

private enum class RosterFilter(val label: String) {
    All("All"),
    NeedsAttention("Needs attention"),
    FollowUpDue("Follow-up due"),
    NearTerm("Near term"),
    Routine("Routine")
}

private fun Patient.matchesRosterFilter(
    filter: RosterFilter,
    visits: List<VisitLog>,
    referrals: List<ReferralFlag>,
    followUpTasks: List<FollowUpTask>
): Boolean {
    if (filter == RosterFilter.All) return true
    val chips = PatientRosterUiLogic.statusChips(this, visits, referrals, followUpTasks = followUpTasks)
    return when (filter) {
        RosterFilter.All -> true
        RosterFilter.NeedsAttention ->
            PatientRosterUiLogic.attentionRank(this, visits, referrals, followUpTasks = followUpTasks) < 6
        RosterFilter.FollowUpDue ->
            chips.any { it.label.startsWith("Follow-up") }
        RosterFilter.NearTerm ->
            (pregnancyWeeks ?: 0) >= 36
        RosterFilter.Routine ->
            chips.any { it.label == "Routine" }
    }
}
