package com.smriti.clinicalscribe.ui

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smriti.clinicalscribe.audio.AudioRecorder
import com.smriti.clinicalscribe.audio.VoiceNoteMetadata
import com.smriti.clinicalscribe.data.HistorySignal
import com.smriti.clinicalscribe.data.MissedFollowUpAlert
import com.smriti.clinicalscribe.data.Patient
import com.smriti.clinicalscribe.data.PatientLanguages
import com.smriti.clinicalscribe.data.VisitLog
import com.smriti.clinicalscribe.transcript.AndroidOfflineSpeechRecognizerClient
import com.smriti.clinicalscribe.transcript.TranscriptResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VisitScreen(
    patient: Patient,
    history: List<VisitLog>,
    isGenerating: Boolean,
    audioPermissionGranted: Boolean,
    errorMessage: String?,
    reasoningModeLabel: String,
    realGemmaModelStatusLabel: String,
    realGemmaInferenceLabel: String,
    realGemmaDeveloperWarning: String?,
    protocolContextLabel: String,
    missedFollowUpAlerts: List<MissedFollowUpAlert>,
    historySignal: HistorySignal?,
    onRequestAudioPermission: () -> Unit,
    onMarkFollowUpConfirmed: (Long) -> Unit,
    onGenerate: (String, VoiceNoteMetadata?) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val audioRecorder = remember(context) { AudioRecorder(context) }
    val offlineSpeechClient = remember(context) { AndroidOfflineSpeechRecognizerClient(context) }
    val scope = rememberCoroutineScope()
    var voiceNoteStatus by remember { mutableStateOf(VoiceNoteStatus.Idle) }
    var voiceNoteError by remember { mutableStateOf<String?>(null) }
    var offlineSpeechStatus by remember { mutableStateOf<String?>(null) }
    var isListeningOfflineSpeech by remember { mutableStateOf(false) }
    var isRecordingVoiceNote by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableStateOf(0) }
    var savedVoiceNote by remember { mutableStateOf<VoiceNoteMetadata?>(null) }
    var dismissedOngoingFollowUpIds by remember(patient.id) { mutableStateOf<Set<Long>>(emptySet()) }
    var observationText by remember {
        mutableStateOf(
            SampleDangerSignTranscript
        )
    }

    fun stopVoiceNote() {
        audioRecorder.stop()
            .onSuccess { metadata ->
                savedVoiceNote = metadata
                elapsedSeconds = metadata.audioDurationSeconds
                voiceNoteStatus = VoiceNoteStatus.SavedLocally
                voiceNoteError = null
            }
            .onFailure { error ->
                voiceNoteStatus = VoiceNoteStatus.Error
                voiceNoteError = error.message ?: "Could not save local voice note."
            }
        isRecordingVoiceNote = false
    }

    fun startVoiceNote() {
        if (!audioPermissionGranted) {
            voiceNoteStatus = VoiceNoteStatus.Error
            voiceNoteError = "Microphone permission is required to record a local voice note."
            onRequestAudioPermission()
            return
        }

        audioRecorder.start()
            .onSuccess {
                savedVoiceNote = null
                elapsedSeconds = 0
                isRecordingVoiceNote = true
                voiceNoteStatus = VoiceNoteStatus.RecordingLocally
                voiceNoteError = null
            }
            .onFailure { error ->
                isRecordingVoiceNote = false
                voiceNoteStatus = VoiceNoteStatus.Error
                voiceNoteError = error.message ?: "Could not start local voice recording."
        }
    }

    fun tryOfflineSpeech() {
        if (!audioPermissionGranted) {
            offlineSpeechStatus = "Microphone permission is required for offline speech recognition. Grant permission, then try again."
            onRequestAudioPermission()
            return
        }

        scope.launch {
            isListeningOfflineSpeech = true
            offlineSpeechStatus = "Listening with Android offline speech..."
            val speechResult = runCatching {
                offlineSpeechClient.transcribeLiveSpeech()
            }.getOrElse { error ->
                TranscriptResult.Error(error.message ?: "Could not run Android offline speech recognition.")
            }
            when (speechResult) {
                is TranscriptResult.Success -> {
                    observationText = speechResult.transcript
                    offlineSpeechStatus = "Offline speech transcript added. Review and edit before generating."
                }

                is TranscriptResult.Unavailable -> {
                    offlineSpeechStatus = "Offline speech unavailable: ${speechResult.reason}"
                }

                is TranscriptResult.Error -> {
                    offlineSpeechStatus = "Offline speech error: ${speechResult.reason}"
                }
            }
            isListeningOfflineSpeech = false
        }
    }

    LaunchedEffect(isRecordingVoiceNote) {
        while (isRecordingVoiceNote) {
            delay(1000L)
            elapsedSeconds = (elapsedSeconds + 1).coerceAtMost(AudioRecorder.MAX_DURATION_SECONDS)
            if (elapsedSeconds >= AudioRecorder.MAX_DURATION_SECONDS) {
                stopVoiceNote()
            }
        }
    }

    DisposableEffect(audioRecorder) {
        onDispose {
            audioRecorder.cancel()
        }
    }

    val visibleFollowUpAlerts = missedFollowUpAlerts
        .filter { it.visitId !in dismissedOngoingFollowUpIds }

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
                        Text(patient.displayLabel(), style = MaterialTheme.typography.headlineSmall)
                        Text("Local visit workspace", style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = "${patient.pregnancyWeeks ?: "-"} weeks - ${patient.village}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Output language: ${PatientLanguages.forPatient(patient).displayLabel}",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    OutlinedButton(onClick = onBack) {
                        Text("Patient Roster")
                    }
                }
            }

            if (visibleFollowUpAlerts.isNotEmpty() || historySignal != null) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        visibleFollowUpAlerts.forEach { alert ->
                            MissedFollowUpCard(
                                alert = alert,
                                onMarkConfirmed = { onMarkFollowUpConfirmed(alert.visitId) },
                                onNoteOngoing = {
                                    dismissedOngoingFollowUpIds = dismissedOngoingFollowUpIds + alert.visitId
                                }
                            )
                        }
                        historySignal?.let { signal ->
                            HistorySignalCard(signal = signal)
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
                        Text("Local Reasoning and Protocol", style = MaterialTheme.typography.titleMedium)
                        Text("Active mode: $reasoningModeLabel")
                        Text("Protocol pack: $protocolContextLabel")
                        Text("Local patient memory + local protocol pack.")
                        Text("Protocol-grounded referral support, not diagnosis.")
                        Text("Real Gemma model: $realGemmaModelStatusLabel")
                        Text("Inference: $realGemmaInferenceLabel")
                        realGemmaDeveloperWarning?.let { warning ->
                            Text(warning, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            item {
                Text("Prior Visit History", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Recent confirmed local history used as context before generating the next note.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (history.isEmpty()) {
                item {
                    Text("No prior visits saved for this patient.")
                }
            } else {
                items(history.take(2)) { visit ->
                    HistoryCard(visit = visit)
                }
                if (history.size > 2) {
                    item {
                        Text(
                            text = "${history.size - 2} older visit(s) kept in local memory.",
                            style = MaterialTheme.typography.labelLarge
                        )
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
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Transcript Input", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Use the sample, type the observation, or try Android offline speech. Direct Gemma 4 audio remains blocked and documented.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = {
                                if (isRecordingVoiceNote) {
                                    stopVoiceNote()
                                } else {
                                    startVoiceNote()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isRecordingVoiceNote) "Stop voice note" else "Start voice note")
                        }
                        Text(
                            text = "Status: ${voiceNoteStatus.label}",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = "Elapsed: ${elapsedSeconds}s / ${AudioRecorder.MAX_DURATION_SECONDS}s",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = if (isRecordingVoiceNote) {
                                "Listening locally... 30-second max chunk"
                            } else {
                                "Ready for local app-private audio storage"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        savedVoiceNote?.let { voiceNote ->
                            Text(
                                text = "Saved locally: ${voiceNote.fileName} (${voiceNote.audioDurationSeconds}s)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        voiceNoteError?.let { message ->
                            Text(message, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = {
                        observationText = SampleDangerSignTranscript
                        offlineSpeechStatus = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Use sample danger-sign transcript")
                }
            }

            item {
                OutlinedButton(
                    onClick = { tryOfflineSpeech() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isListeningOfflineSpeech && !isGenerating && !isRecordingVoiceNote
                ) {
                    Text(if (isListeningOfflineSpeech) "Listening Offline..." else "Try Offline Speech")
                }
                offlineSpeechStatus?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (message.startsWith("Offline speech unavailable") || message.startsWith("Offline speech error")) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onBackground
                        },
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = observationText,
                    onValueChange = { observationText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp),
                    label = { Text("Editable transcript") },
                    placeholder = { Text("Type the CHW's spoken observation here") },
                    minLines = 5
                )
            }

            errorMessage?.let { message ->
                item {
                    Text(message, color = MaterialTheme.colorScheme.error)
                }
            }

            item {
                Button(
                    onClick = { onGenerate(observationText, savedVoiceNote) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = observationText.isNotBlank() && !isGenerating
                ) {
                    Text(if (isGenerating) "Generating Local Visit Note..." else "Generate Local Visit Note")
                }
            }
        }
    }
}

@Composable
private fun MissedFollowUpCard(
    alert: MissedFollowUpAlert,
    onMarkConfirmed: () -> Unit,
    onNoteOngoing: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Missed follow-up", fontWeight = FontWeight.SemiBold)
            Text(alert.message, style = MaterialTheme.typography.bodyMedium)
            Text("Protocol basis: ${alert.protocolCitation}", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onMarkConfirmed, modifier = Modifier.weight(1f)) {
                    Text("Mark Confirmed")
                }
                OutlinedButton(onClick = onNoteOngoing, modifier = Modifier.weight(1f)) {
                    Text("Note as Ongoing")
                }
            }
        }
    }
}

@Composable
private fun HistorySignalCard(signal: HistorySignal) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(signal.title, fontWeight = FontWeight.SemiBold)
            Text(signal.message, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "Recent BP readings: ${signal.readings.joinToString(" -> ") { it.label }}",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun HistoryCard(visit: VisitLog) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(formatDate(visit.visitDateMillis), fontWeight = FontWeight.SemiBold)
            Text(visit.structuredNote, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "Citation: ${visit.protocolCitation}",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

private fun formatDate(millis: Long): String {
    return SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(millis))
}

private const val SampleDangerSignTranscript =
    "Meena is 28 years old and 7 months pregnant. She reports severe headache and blurred vision. Blood pressure is 150 over 95. She has reduced fetal movement today."

private enum class VoiceNoteStatus(val label: String) {
    Idle("Idle"),
    RecordingLocally("Recording locally"),
    SavedLocally("Saved locally"),
    Error("Error")
}
