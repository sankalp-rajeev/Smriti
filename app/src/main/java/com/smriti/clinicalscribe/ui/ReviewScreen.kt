package com.smriti.clinicalscribe.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import com.smriti.clinicalscribe.audio.VoiceNoteMetadata
import com.smriti.clinicalscribe.data.Patient
import com.smriti.clinicalscribe.reasoning.VisitReasoningResult

@Composable
fun ReviewScreen(
    patient: Patient,
    result: VisitReasoningResult,
    voiceNote: VoiceNoteMetadata?,
    isSaving: Boolean,
    ttsStatusMessage: String?,
    exportVisitPath: String?,
    onReadReferralSuggestion: () -> Unit,
    onExportVisitJson: (String, String) -> Unit,
    onConfirmSave: (String, String) -> Unit,
    onBack: () -> Unit
) {
    val initialSections = remember(result) { ReviewNoteSections.from(result.structuredNote) }
    var observationText by remember(result) { mutableStateOf(initialSections.observation) }
    var historyText by remember(result) { mutableStateOf(initialSections.relevantHistory) }
    var supportText by remember(result) { mutableStateOf(initialSections.protocolSupport) }
    var followUpText by remember(result) { mutableStateOf(result.suggestedFollowUp) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column {
                        Text("Review and Confirm Visit Note", style = MaterialTheme.typography.headlineSmall)
                        Text("CHW reviews and confirms before saving.", style = MaterialTheme.typography.labelLarge)
                        Text(patient.displayLabel(), style = MaterialTheme.typography.bodyMedium)
                    }
                    OutlinedButton(
                        onClick = onBack,
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Edit Observation")
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Safety Gate", fontWeight = FontWeight.SemiBold)
                        Text("Protocol-grounded referral support, not diagnosis.")
                        Text("CHW reviews and confirms before saving.")
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

            val referralFlag = result.referralFlag
            if (referralFlag != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Referral Support", fontWeight = FontWeight.SemiBold)
                            Text("Protocol-grounded referral support, not diagnosis.")
                            Text("CHW confirmation and clinical referral judgment are required.")
                            Text("Urgency: ${referralFlag.urgency}")
                            Text(referralFlag.reason)
                            Text("Danger signs: ${referralFlag.dangerSigns}")
                            Text("Protocol citation: ${referralFlag.protocolBasis}")
                            Text("Facility: ${referralFlag.recommendedFacility}")
                            OutlinedButton(
                                onClick = onReadReferralSuggestion,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Read referral suggestion aloud")
                            }
                        }
                    }
                }
            } else if (!result.uncertain) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Routine / No referral flag", fontWeight = FontWeight.SemiBold)
                            Text("No danger-sign referral flag was generated.")
                            Text("CHW reviews and confirms before saving.")
                        }
                    }
                }
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
                        Text("Locally Saved Voice Note", fontWeight = FontWeight.SemiBold)
                        if (voiceNote == null) {
                            Text("No real audio attached. Transcript source: SIMULATED")
                        } else {
                            Text("File: ${voiceNote.fileName}")
                            Text("Duration: ${voiceNote.audioDurationSeconds}s")
                            Text("Transcript source: REAL_ASR_PENDING")
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = observationText,
                    onValueChange = { observationText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 110.dp),
                    label = { Text("Observation") },
                    minLines = 3
                )
            }

            item {
                OutlinedTextField(
                    value = historyText,
                    onValueChange = { historyText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    label = { Text("Relevant history") },
                    minLines = 3
                )
            }

            item {
                OutlinedTextField(
                    value = supportText,
                    onValueChange = { supportText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 130.dp),
                    label = { Text("Protocol-grounded support") },
                    minLines = 4
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

            ttsStatusMessage?.let { message ->
                item {
                    Text(message, style = MaterialTheme.typography.bodyMedium)
                }
            }

            item {
                OutlinedButton(
                    onClick = {
                        onExportVisitJson(
                            buildStructuredNote(
                                observation = observationText,
                                relevantHistory = historyText,
                                protocolSupport = supportText
                            ),
                            followUpText
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export Visit JSON")
                }
                exportVisitPath?.let { path ->
                    Text(
                        text = "Export saved locally: $path",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        onConfirmSave(
                            buildStructuredNote(
                                observation = observationText,
                                relevantHistory = historyText,
                                protocolSupport = supportText
                            ),
                            followUpText
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = observationText.isNotBlank() && supportText.isNotBlank() && !isSaving
                ) {
                    Text(if (isSaving) "Saving Confirmed Visit..." else "Confirm CHW Review and Save")
                }
            }
        }
    }
}

private data class ReviewNoteSections(
    val observation: String,
    val relevantHistory: String,
    val protocolSupport: String
) {
    companion object {
        fun from(note: String): ReviewNoteSections {
            return ReviewNoteSections(
                observation = extractSection(note, "Observation:", "Relevant history:"),
                relevantHistory = extractSection(note, "Relevant history:", "Protocol-grounded support:"),
                protocolSupport = extractSection(note, "Protocol-grounded support:", null)
            )
        }

        private fun extractSection(note: String, start: String, end: String?): String {
            val startIndex = note.indexOf(start)
            if (startIndex < 0) return note.trim()

            val contentStart = startIndex + start.length
            val contentEnd = end
                ?.let { note.indexOf(it, startIndex = contentStart) }
                ?.takeIf { it >= 0 }
                ?: note.length

            return note.substring(contentStart, contentEnd).trim()
        }
    }
}

private fun buildStructuredNote(
    observation: String,
    relevantHistory: String,
    protocolSupport: String
): String {
    return listOf(
        "Observation:\n${observation.trim()}",
        "Relevant history:\n${relevantHistory.trim()}",
        "Protocol-grounded support:\n${protocolSupport.trim()}"
    ).joinToString(separator = "\n\n")
}
