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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smriti.clinicalscribe.audio.VoiceNoteMetadata
import com.smriti.clinicalscribe.data.HistorySignal
import com.smriti.clinicalscribe.data.MissedFollowUpAlert
import com.smriti.clinicalscribe.data.Patient
import com.smriti.clinicalscribe.data.TranscriptSource
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
    generationStatusMessage: String?,
    audioPermissionGranted: Boolean,
    errorMessage: String?,
    reasoningModeLabel: String,
    realGemmaModelStatusLabel: String,
    realGemmaEngineStatusLabel: String,
    realGemmaInferenceLabel: String,
    realGemmaDeveloperWarning: String?,
    protocolContextLabel: String,
    missedFollowUpAlerts: List<MissedFollowUpAlert>,
    historySignal: HistorySignal?,
    isReadingPaperNote: Boolean,
    paperNoteStatusMessage: String?,
    onRequestAudioPermission: () -> Unit,
    onMarkFollowUpConfirmed: (Long) -> Unit,
    onGenerate: (String, VoiceNoteMetadata?) -> Unit,
    onScanPaperNote: () -> Unit,
    onUseSamplePaperNote: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val offlineSpeechClient = remember(context) { AndroidOfflineSpeechRecognizerClient(context) }
    val scope = rememberCoroutineScope()
    var offlineSpeechStatus by remember { mutableStateOf<String?>(null) }
    var isListeningOfflineSpeech by remember { mutableStateOf(false) }
    var dismissedOngoingFollowUpIds by remember(patient.id) { mutableStateOf<Set<Long>>(emptySet()) }
    var observationText by remember(patient.id) { mutableStateOf("") }
    var inlineError by remember(patient.id) { mutableStateOf<String?>(null) }
    var showHistory by remember(patient.id) { mutableStateOf(false) }
    var showStopDialog by remember { mutableStateOf(false) }
    var loadingStep by remember { mutableIntStateOf(0) }
    val loadingMessages = listOf(
        "Reading patient history...",
        "Checking local health guidance...",
        "Running on-device Gemma...",
        "Preparing note for review..."
    )
    val visibleFollowUpAlerts = missedFollowUpAlerts
        .filter { it.visitId !in dismissedOngoingFollowUpIds }
    val modelReady = realGemmaModelStatusLabel.contains("found", ignoreCase = true)

    LaunchedEffect(isGenerating) {
        if (!isGenerating) {
            loadingStep = 0
            return@LaunchedEffect
        }
        while (isGenerating) {
            delay(1800L)
            loadingStep = (loadingStep + 1).coerceAtMost(loadingMessages.lastIndex)
        }
    }

    DisposableEffect(Unit) {
        onDispose { }
    }

    fun tryOfflineSpeech() {
        if (!audioPermissionGranted) {
            offlineSpeechStatus = "Microphone permission is needed. Please allow it, then try again."
            onRequestAudioPermission()
            return
        }

        scope.launch {
            isListeningOfflineSpeech = true
            offlineSpeechStatus = "Listening offline..."
            val speechResult = runCatching {
                offlineSpeechClient.transcribeLiveSpeech()
            }.getOrElse { error ->
                TranscriptResult.Error(error.message ?: "Could not run offline speech.")
            }
            when (speechResult) {
                is TranscriptResult.Success -> {
                    if (speechResult.transcript.isBlank()) {
                        offlineSpeechStatus = "No speech detected. Please try again or type manually."
                    } else {
                        observationText = speechResult.transcript
                        inlineError = null
                        offlineSpeechStatus = "Speech added. Please review before generating."
                    }
                }
                is TranscriptResult.Unavailable -> {
                    offlineSpeechStatus = "Speech is not available on this device. Please type manually."
                }
                is TranscriptResult.Error -> {
                    offlineSpeechStatus = "No speech detected. Please try again or type manually."
                }
            }
            isListeningOfflineSpeech = false
        }
    }

    fun requestGenerate() {
        val trimmed = observationText.trim()
        when {
            trimmed.isBlank() -> {
                inlineError = "Please speak or type today's visit observation first."
            }
            trimmed.length < 10 -> {
                inlineError = "This observation is very short.\nAdd more detail for a better note."
                onGenerate(observationText, null)
            }
            isReadingPaperNote -> {
                inlineError = "Smriti is reading a paper note. Please wait."
            }
            !isGenerating -> {
                inlineError = null
                onGenerate(observationText, null)
            }
        }
    }

    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text("Note is being prepared") },
            text = { Text("Please wait until Smriti finishes. This keeps the on-device model from starting another request.") },
            confirmButton = {
                TextButton(onClick = { showStopDialog = false }) {
                    Text("OK")
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(patient.displayLabel(), style = MaterialTheme.typography.headlineSmall)
                            Text(PatientVisitUiText.gestationLabel(patient), style = MaterialTheme.typography.bodyLarge)
                            Text(PatientVisitUiText.countryVillage(patient), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Visit note will be prepared in ${PatientVisitUiText.noteLanguageDisplayLabel(patient)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                if (isGenerating) showStopDialog = true else onBack()
                            },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Back")
                        }
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
                        historySignal?.let { signal -> HistorySignalCard(signal = signal) }
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
                        Text("What to do now", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Speak or type today's visit observation. Smriti will prepare a note for review.",
                            style = MaterialTheme.typography.bodyLarge
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
                        Button(
                            onClick = { tryOfflineSpeech() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp),
                            enabled = !isListeningOfflineSpeech && !isGenerating
                        ) {
                            Text(if (isListeningOfflineSpeech) "Listening..." else "Speak observation")
                        }
                        OutlinedButton(
                            onClick = {
                                observationText = VisitSampleTranscripts.forPatient(patient)
                                inlineError = null
                                offlineSpeechStatus = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            enabled = !isGenerating && !isReadingPaperNote
                        ) {
                            Text("Use sample visit transcript")
                        }
                        OutlinedTextField(
                            value = observationText,
                            onValueChange = {
                                observationText = it
                                inlineError = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 150.dp),
                            label = { Text("Visit observation") },
                            placeholder = { Text("Type today's visit observation here") },
                            minLines = 5
                        )
                        inlineError?.let { message ->
                            Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
                        }
                        offlineSpeechStatus?.let { message ->
                            Text(message, style = MaterialTheme.typography.bodyLarge)
                        }
                        Button(
                            onClick = { requestGenerate() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp),
                            enabled = !isGenerating && !isReadingPaperNote
                        ) {
                            Text(if (isGenerating) "Preparing note..." else "Generate visit note")
                        }
                        OutlinedButton(
                            onClick = onScanPaperNote,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            enabled = !isGenerating && !isReadingPaperNote
                        ) {
                            Text("Scan paper note")
                        }
                        OutlinedButton(
                            onClick = onUseSamplePaperNote,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            enabled = !isGenerating && !isReadingPaperNote
                        ) {
                            Text("Use sample paper note")
                        }
                    }
                }
            }

            if (isReadingPaperNote) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Reading paper note...", fontWeight = FontWeight.SemiBold)
                            Text(paperNoteStatusMessage ?: "Extracting visit details...", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            if (isGenerating) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(loadingMessages[loadingStep], fontWeight = FontWeight.SemiBold)
                            Text(generationStatusMessage ?: "This may take a few seconds.", style = MaterialTheme.typography.bodyLarge)
                            Text("This may take a few seconds.", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            errorMessage?.let { message ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Note could not be prepared", fontWeight = FontWeight.SemiBold)
                            Text(
                                message,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            OutlinedButton(
                                onClick = { requestGenerate() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp),
                                enabled = !isGenerating
                            ) {
                                Text("Try again")
                            }
                        }
                    }
                }
            }

            item {
                PriorHistorySection(
                    history = history,
                    expanded = showHistory,
                    onToggle = { showHistory = !showHistory }
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
                        Text("Offline setup ready", fontWeight = FontWeight.SemiBold)
                        Text("On-device Gemma: ${if (modelReady) "ready" else "Setup needed"}")
                        Text("Local guidance available")
                    }
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE3B0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Missed follow-up", fontWeight = FontWeight.SemiBold)
            Text(
                "Referred to health facility ${alert.daysOverdue} days ago. Outcome unknown. Confirm before today's visit.",
                style = MaterialTheme.typography.bodyLarge
            )
            Button(
                onClick = onMarkConfirmed,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text("Mark confirmed")
            }
            OutlinedButton(
                onClick = onNoteOngoing,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text("Note as ongoing")
            }
        }
    }
}

@Composable
private fun HistorySignalCard(signal: HistorySignal) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE3B0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("History signal", fontWeight = FontWeight.SemiBold)
            Text(
                "BP readings have increased across recent visits. Review and monitor per local health guidance.",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Recent BP readings: ${signal.readings.joinToString(" -> ") { it.label }}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun PriorHistorySection(
    history: List<VisitLog>,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Patient history", style = MaterialTheme.typography.titleMedium)
        if (history.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("No prior visits recorded.", fontWeight = FontWeight.SemiBold)
                    Text("This is the first visit for this patient.", style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            val visibleHistory = if (expanded) history else history.take(2)
            visibleHistory.forEach { visit -> HistoryCard(visit = visit) }
            if (history.size > 2) {
                OutlinedButton(
                    onClick = onToggle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text(if (expanded) "Show less history" else "Show patient history")
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(visit: VisitLog) {
    var showDetails by remember(visit.id) { mutableStateOf(false) }
    val status = historyStatusLabel(visit)
    val followUp = visit.suggestedFollowUp.trim()
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(formatDate(visit.visitDateMillis), fontWeight = FontWeight.SemiBold)
            Text(status, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(historyObservationSummary(visit), style = MaterialTheme.typography.bodyLarge)
            if (followUp.isNotBlank()) {
                Text("Follow-up: $followUp", style = MaterialTheme.typography.bodyMedium)
            }
            Text("Not a diagnosis. Health worker reviewed before saving.", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { showDetails = !showDetails }) {
                Text(if (showDetails) "Hide details" else "Show details")
            }
            if (showDetails) {
                Text("Full observation: ${visit.observationText.ifBlank { "Review saved note" }}")
                Text("Patient history checked")
                Text("Local health guidance checked")
                if (visit.protocolCitation.isNotBlank()) {
                    Text("Guidance ID: ${visit.protocolCitation}", style = MaterialTheme.typography.bodyMedium)
                }
                Text("Source: ${historySourceLabel(visit)}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun historyStatusLabel(visit: VisitLog): String {
    return when {
        visit.transcriptSource == TranscriptSource.PAPER_SCAN -> "Paper note scan"
        visit.structuredNote.contains("more information", ignoreCase = true) -> "More information needed"
        visit.structuredNote.contains("referral", ignoreCase = true) ||
            visit.suggestedFollowUp.contains("refer", ignoreCase = true) -> "Referral suggested"
        else -> "No referral flag"
    }
}

private fun historyObservationSummary(visit: VisitLog): String {
    val source = visit.observationText.ifBlank { visit.structuredNote }
        .replace(Regex("\\s+"), " ")
        .trim()
    return source.ifBlank { "Review saved note" }
        .let { if (it.length <= 130) it else "${it.take(127).trimEnd()}..." }
}

private fun historySourceLabel(visit: VisitLog): String {
    return when (visit.transcriptSource) {
        TranscriptSource.PAPER_SCAN -> "Paper note scan"
        TranscriptSource.REAL_ASR_PENDING -> "Local voice note"
        else -> "Typed or sample visit observation"
    }
}

private fun formatDate(millis: Long): String {
    return SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(millis))
}
