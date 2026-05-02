package com.smriti.clinicalscribe.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import com.smriti.clinicalscribe.reasoning.SupervisorPriorityQueue
import com.smriti.clinicalscribe.reasoning.SupervisorSummary

@Composable
fun SummaryScreen(
    summary: SupervisorSummary,
    priorityQueue: SupervisorPriorityQueue?,
    priorityUnavailableMessage: String?,
    isResettingDemoData: Boolean,
    offlineProofStatus: OfflineProofStatus,
    ttsStatusMessage: String?,
    exportSummaryPath: String?,
    onReadSummary: () -> Unit,
    onExportSummaryJson: () -> Unit,
    onResetDemoData: () -> Unit,
    onBack: () -> Unit
) {
    var showResetDialog by remember { mutableStateOf(false) }
    var showLocalProof by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset all demo data?") },
            text = { Text("This will clear all saved visits and restore the original patient list.") },
            confirmButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        onResetDemoData()
                    }
                ) {
                    Text("Reset", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("End-of-day summary", style = MaterialTheme.typography.headlineSmall)
                    Text("Confirmed local data only", style = MaterialTheme.typography.bodyLarge)
                    Button(
                        onClick = onBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp),
                        enabled = !isResettingDemoData
                    ) {
                        Text("Back to patients")
                    }
                }
            }

            if (summary.totalVisits == 0) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("No visits recorded today.", style = MaterialTheme.typography.titleMedium)
                            Text("Visit a patient and confirm a note to see today's summary.", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            } else {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Today's priority list", fontWeight = FontWeight.SemiBold)
                            Text("Total confirmed visits: ${summary.totalVisits}", style = MaterialTheme.typography.bodyLarge)
                            Text("Referral flags: ${summary.referralsFlagged}", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            priorityUnavailableMessage?.let {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE3B0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("On-device summary unavailable.", fontWeight = FontWeight.SemiBold)
                            Text("Local visit counts are shown below.", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            priorityQueue?.items?.takeIf { it.isNotEmpty() }?.let { items ->
                item { Text("Today's priority list", style = MaterialTheme.typography.titleMedium) }
                items(items) { item ->
                    SummaryItem(
                        title = "${item.patientName} - ${item.urgency.lowercase().replace('_', ' ')}",
                        body = item.reason,
                        tone = when (item.urgency) {
                            "IMMEDIATE" -> SummaryTone.Urgent
                            "WITHIN_24H" -> SummaryTone.Caution
                            else -> SummaryTone.Routine
                        }
                    )
                }
            }

            item { Text("Urgent cases", style = MaterialTheme.typography.titleMedium) }
            if (summary.urgentCases.isEmpty()) {
                item { SummaryItem("No urgent cases", "No immediate referral cases saved yet.", SummaryTone.Routine) }
            } else {
                items(summary.urgentCases) { itemText ->
                    SummaryItem("Immediate referral", itemText, SummaryTone.Urgent)
                }
            }

            item { Text("Follow-ups", style = MaterialTheme.typography.titleMedium) }
            if (summary.followUpsDue.isEmpty()) {
                item { SummaryItem("No follow-ups", "No follow-up tasks saved yet.", SummaryTone.Routine) }
            } else {
                items(summary.followUpsDue) { itemText ->
                    SummaryItem("Follow-up due", itemText, SummaryTone.Caution)
                }
            }

            item {
                SummaryItem(
                    title = "Routine visits",
                    body = "Routine confirmed visits: ${(summary.totalVisits - summary.referralsFlagged).coerceAtLeast(0)}",
                    tone = SummaryTone.Routine
                )
            }

            item {
                OutlinedButton(
                    onClick = onReadSummary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text("Read summary aloud")
                }
                OutlinedButton(
                    onClick = onExportSummaryJson,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .padding(top = 8.dp)
                ) {
                    Text("Export visit data")
                }
                ttsStatusMessage?.let { message ->
                    Text(text = message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
                }
                exportSummaryPath?.let { path ->
                    Text(text = "Export saved locally: $path", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
                }
            }

            item {
                OutlinedButton(
                    onClick = { showResetDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    enabled = !isResettingDemoData
                ) {
                    Text(if (isResettingDemoData) "Resetting..." else "Reset Demo Data")
                }
            }

            item {
                OutlinedButton(
                    onClick = { showLocalProof = !showLocalProof },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text("Local proof")
                }
                if (showLocalProof) {
                    OfflineProofCard(
                        status = offlineProofStatus,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

private enum class SummaryTone {
    Urgent,
    Caution,
    Routine
}

@Composable
private fun SummaryItem(
    title: String,
    body: String,
    tone: SummaryTone
) {
    val color = when (tone) {
        SummaryTone.Urgent -> MaterialTheme.colorScheme.errorContainer
        SummaryTone.Caution -> Color(0xFFFFE3B0)
        SummaryTone.Routine -> MaterialTheme.colorScheme.surfaceVariant
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = color),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
