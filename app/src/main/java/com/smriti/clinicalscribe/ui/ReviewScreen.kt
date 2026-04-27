package com.smriti.clinicalscribe.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smriti.clinicalscribe.data.Patient
import com.smriti.clinicalscribe.reasoning.VisitReasoningResult

@Composable
fun ReviewScreen(
    patient: Patient,
    result: VisitReasoningResult,
    isSaving: Boolean,
    onConfirmSave: (String, String) -> Unit,
    onBack: () -> Unit
) {
    var noteText by remember(result) { mutableStateOf(result.structuredNote) }
    var followUpText by remember(result) { mutableStateOf(result.suggestedFollowUp) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Review and Confirm Visit Note", style = MaterialTheme.typography.headlineSmall)
                        Text("Offline demo mode", style = MaterialTheme.typography.labelLarge)
                        Text(patient.displayLabel(), style = MaterialTheme.typography.bodyMedium)
                    }
                    OutlinedButton(onClick = onBack, enabled = !isSaving) {
                        Text("Edit Observation")
                    }
                }
            }

            if (result.uncertain && result.clarificationPrompt != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Needs Confirmation", fontWeight = FontWeight.SemiBold)
                            Text(result.clarificationPrompt)
                        }
                    }
                }
            }

            result.referralFlag?.let { flag ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Protocol-Grounded Referral Suggestion", fontWeight = FontWeight.SemiBold)
                            Text("This is not a diagnosis. CHW confirmation and clinical referral judgment are required.")
                            Text("Urgency: ${flag.urgency}")
                            Text(flag.reason)
                            Text("Danger signs: ${flag.dangerSigns}")
                            Text("Protocol citation: ${flag.protocolBasis}")
                            Text("Facility: ${flag.recommendedFacility}")
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 220.dp),
                    label = { Text("Structured visit note") },
                    minLines = 8
                )
            }

            item {
                OutlinedTextField(
                    value = followUpText,
                    onValueChange = { followUpText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Suggested follow-up with citation") },
                    minLines = 2
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Protocol Citation", fontWeight = FontWeight.SemiBold)
                        Text(result.protocolCitation)
                        result.protocolChunk?.let { Text(it.text) }
                    }
                }
            }

            item {
                Button(
                    onClick = { onConfirmSave(noteText, followUpText) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = noteText.isNotBlank() && !isSaving
                ) {
                    Text(if (isSaving) "Saving Confirmed Visit..." else "Confirm CHW Review and Save")
                }
            }
        }
    }
}
