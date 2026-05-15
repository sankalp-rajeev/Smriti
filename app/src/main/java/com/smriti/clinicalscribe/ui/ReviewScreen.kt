package com.smriti.clinicalscribe.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
    priorVisitCount: Int,
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
    var supportText by remember(result) { mutableStateOf(initialSections.guidanceSupport) }
    var followUpText by remember(result) { mutableStateOf(result.suggestedFollowUp) }
    var showSourceDetails by remember(result) { mutableStateOf(false) }

    SmritiScreenSurface {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(SmritiSpacing.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(SmritiSpacing.CardGap)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Review before saving", style = MaterialTheme.typography.headlineSmall)
                    SmritiCard(tone = SmritiTone.Success) {
                        Text(patient.displayLabel(), fontWeight = FontWeight.SemiBold)
                        Text(
                            "Smriti drafted a cited note. The health worker reviews and confirms before it is saved on this device.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "Smriti does not diagnose.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    SmritiSecondaryButton("Back to visit", onBack, enabled = !isSaving)
                }
            }

            result.clarificationPrompt?.takeIf { it.isNotBlank() }?.let { question ->
                item {
                    SmritiCard(tone = SmritiTone.Caution) {
                        Text("More information needed", fontWeight = FontWeight.SemiBold)
                        Text(question, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            val referralFlag = result.referralFlag
            item {
                SmritiCard(tone = if (referralFlag != null) SmritiTone.Urgent else if (result.uncertain) SmritiTone.Caution else SmritiTone.Success) {
                    Text("Referral-support status", fontWeight = FontWeight.SemiBold)
                    when {
                        referralFlag != null -> {
                            SmritiStatusChip("Referral suggested", tone = SmritiTone.Urgent)
                            Text(referralFlag.reason, style = MaterialTheme.typography.bodyLarge)
                            if (referralFlag.dangerSigns.isNotBlank()) {
                                Text("Danger signs: ${referralFlag.dangerSigns}", style = MaterialTheme.typography.bodyLarge)
                            }
                            OutlinedButton(
                                onClick = onReadReferralSuggestion,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                            ) {
                                Text("Read note aloud")
                            }
                        }
                        result.uncertain -> {
                            SmritiStatusChip("Needs confirmation", tone = SmritiTone.Caution)
                            Text("Smriti needs the health worker to confirm missing details before saving.", style = MaterialTheme.typography.bodyLarge)
                        }
                        else -> {
                            SmritiStatusChip("No referral flag", tone = SmritiTone.Success)
                            Text("No urgent danger signs were flagged from this note.", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            item {
                SmritiCard(tone = SmritiTone.Info) {
                    Text("Local guidance / citation", fontWeight = FontWeight.SemiBold)
                    Text(result.protocolChunk?.title ?: "Local guidance checked", style = MaterialTheme.typography.bodyLarge)
                    Text("Guidance ID: ${result.protocolCitation}", style = MaterialTheme.typography.bodyMedium)
                    result.protocolChunk?.text?.takeIf { it.isNotBlank() }?.let { text ->
                        Text(guidanceSummary(text), style = MaterialTheme.typography.bodyMedium)
                    }
                    OutlinedButton(
                        onClick = { showSourceDetails = !showSourceDetails },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Text(if (showSourceDetails) "Hide preparation details" else "How was this prepared?")
                    }
                    if (showSourceDetails) {
                        Text("This note was prepared using today's observation.", style = MaterialTheme.typography.bodyMedium)
                        if (priorVisitCount > 0) {
                            Text("Patient history from $priorVisitCount prior visits", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            Text("No prior visit history on this device", style = MaterialTheme.typography.bodyMedium)
                        }
                        val localSource = patient.country.ifBlank { "this country" }
                        Text("Local guidance for $localSource", style = MaterialTheme.typography.bodyMedium)
                        Text("Gemma 4 on device; no internet used", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            item {
                SmritiCard {
                    Text("Generated note", fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = observationText,
                        onValueChange = { observationText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 110.dp),
                        label = { Text("Observation") },
                        minLines = 3
                    )
                    OutlinedTextField(
                        value = historyText,
                        onValueChange = { historyText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp),
                        label = { Text("Relevant history") },
                        minLines = 3
                    )
                    OutlinedTextField(
                        value = supportText,
                        onValueChange = { supportText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 130.dp),
                        label = { Text("Local guidance support") },
                        minLines = 4
                    )
                    OutlinedTextField(
                        value = followUpText,
                        onValueChange = { followUpText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Follow-up plan") },
                        minLines = 2
                    )
                }
            }

            item {
                SmritiCard(tone = SmritiTone.Muted) {
                    Text("CHW confirm/save", fontWeight = FontWeight.SemiBold)
                    Text("Review the note, citation, and follow-up plan before saving.", style = MaterialTheme.typography.bodyLarge)
                    voiceNote?.let { note ->
                        Text(
                            "Local voice note attached: ${note.audioDurationSeconds}s",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    ttsStatusMessage?.let { message ->
                        Text(message, style = MaterialTheme.typography.bodyLarge)
                    }
                    OutlinedButton(
                        onClick = {
                            onExportVisitJson(
                                buildStructuredNote(
                                    observation = observationText,
                                    relevantHistory = historyText,
                                    guidanceSupport = supportText
                                ),
                                followUpText
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Text("Export visit data")
                    }
                    exportVisitPath?.let { path ->
                        Text(
                            text = "Export saved locally: $path",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        enabled = !isSaving
                    ) {
                        Text("Edit note")
                    }
                    SmritiPrimaryButton(
                        text = if (isSaving) "Saving locally..." else "Confirm and save",
                        onClick = {
                            onConfirmSave(
                                buildStructuredNote(
                                    observation = observationText,
                                    relevantHistory = historyText,
                                    guidanceSupport = supportText
                                ),
                                followUpText
                            )
                        },
                        enabled = observationText.isNotBlank() && supportText.isNotBlank() && !isSaving
                    )
                }
            }
        }
    }
}

private data class ReviewNoteSections(
    val observation: String,
    val relevantHistory: String,
    val guidanceSupport: String
) {
    companion object {
        fun from(note: String): ReviewNoteSections {
            return ReviewNoteSections(
                observation = extractSection(note, "Observation:", "Relevant history:"),
                relevantHistory = extractSection(note, "Relevant history:", listOf("Local guidance support:")),
                guidanceSupport = extractSection(note, listOf("Local guidance support:"), null)
            )
        }

        private fun extractSection(note: String, start: String, end: String?): String {
            return extractSection(note, listOf(start), end?.let { listOf(it) })
        }

        private fun extractSection(note: String, start: String, end: List<String>?): String {
            return extractSection(note, listOf(start), end)
        }

        private fun extractSection(note: String, starts: List<String>, ends: List<String>?): String {
            val startMatch = starts
                .mapNotNull { marker ->
                    val index = note.indexOf(marker)
                    if (index >= 0) marker to index else null
                }
                .minByOrNull { it.second }
                ?: return note.trim()

            val contentStart = startMatch.second + startMatch.first.length
            val contentEnd = ends
                ?.mapNotNull { marker -> note.indexOf(marker, startIndex = contentStart).takeIf { it >= 0 } }
                ?.minOrNull()
                ?: note.length

            return note.substring(contentStart, contentEnd).trim()
        }
    }
}

private fun buildStructuredNote(
    observation: String,
    relevantHistory: String,
    guidanceSupport: String
): String {
    return listOf(
        "Observation:\n${observation.trim()}",
        "Relevant history:\n${relevantHistory.trim()}",
        "Local guidance support:\n${guidanceSupport.trim()}"
    ).joinToString(separator = "\n\n")
}

private fun guidanceSummary(text: String): String {
    val cleaned = text.replace(Regex("\\s+"), " ").trim()
    return if (cleaned.length <= 260) cleaned else "${cleaned.take(257).trimEnd()}..."
}

