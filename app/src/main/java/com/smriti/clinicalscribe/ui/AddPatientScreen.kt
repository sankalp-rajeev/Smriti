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
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smriti.clinicalscribe.data.Patient
import com.smriti.clinicalscribe.data.PatientRegistrationDraft
import com.smriti.clinicalscribe.data.PatientRegistrationField
import com.smriti.clinicalscribe.data.PatientRegistrationResult
import com.smriti.clinicalscribe.transcript.AndroidOfflineSpeechRecognizerClient
import com.smriti.clinicalscribe.transcript.TranscriptResult
import kotlinx.coroutines.launch

@Composable
fun AddPatientScreen(
    audioPermissionGranted: Boolean,
    onRequestAudioPermission: () -> Unit,
    onSavePatient: (Patient) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var draft by remember { mutableStateOf(PatientRegistrationDraft()) }
    var selectedLanguage by remember { mutableStateOf(RegistrationLanguage.EN) }
    var activeField by remember { mutableStateOf<PatientRegistrationField?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var validationErrors by remember { mutableStateOf<List<String>>(emptyList()) }

    fun listenFor(field: PatientRegistrationField) {
        if (!audioPermissionGranted) {
            statusMessage = "Microphone permission is required for offline speech. You can still type manually."
            onRequestAudioPermission()
            return
        }

        scope.launch {
            activeField = field
            statusMessage = "Listening: ${promptFor(field)}"
            val speechClient = AndroidOfflineSpeechRecognizerClient(
                context = context,
                languageFallbackOrder = selectedLanguage.languageTags
            )
            val result = runCatching {
                speechClient.transcribeLiveSpeech()
            }.getOrElse { error ->
                TranscriptResult.Error(error.message ?: "Could not run Android offline speech recognition.")
            }
            val update = draft.applySpeechResult(field, result)
            draft = update.draft
            statusMessage = update.message
            activeField = null
        }
    }

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
                        Text("Add Patient", style = MaterialTheme.typography.headlineSmall)
                        Text("Voice-first local registration with manual fallback.", style = MaterialTheme.typography.bodyMedium)
                    }
                    OutlinedButton(onClick = onBack) {
                        Text("Patient Roster")
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Registration Language", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            RegistrationLanguage.entries.forEach { language ->
                                FilterChip(
                                    selected = selectedLanguage == language,
                                    onClick = {
                                        selectedLanguage = language
                                        draft = draft.copy(preferredLanguage = language.code)
                                    },
                                    label = { Text(language.label) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                RegistrationField(
                    label = "Patient name",
                    value = draft.name,
                    onValueChange = { draft = draft.copy(name = it) },
                    prompt = "Speak patient name",
                    isListening = activeField == PatientRegistrationField.NAME,
                    onListen = { listenFor(PatientRegistrationField.NAME) }
                )
            }

            item {
                RegistrationField(
                    label = "Age",
                    value = draft.age,
                    onValueChange = { draft = draft.copy(age = it) },
                    prompt = "Speak patient age",
                    isListening = activeField == PatientRegistrationField.AGE,
                    onListen = { listenFor(PatientRegistrationField.AGE) }
                )
            }

            item {
                RegistrationField(
                    label = "Weeks pregnant",
                    value = draft.pregnancyWeeks,
                    onValueChange = { draft = draft.copy(pregnancyWeeks = it) },
                    prompt = "How many weeks pregnant?",
                    isListening = activeField == PatientRegistrationField.PREGNANCY_WEEKS,
                    onListen = { listenFor(PatientRegistrationField.PREGNANCY_WEEKS) }
                )
            }

            item {
                RegistrationField(
                    label = "Village",
                    value = draft.village,
                    onValueChange = { draft = draft.copy(village = it) },
                    prompt = "Speak village name",
                    isListening = activeField == PatientRegistrationField.VILLAGE,
                    onListen = { listenFor(PatientRegistrationField.VILLAGE) }
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = draft.countryCode,
                        onValueChange = { draft = draft.copy(countryCode = it.uppercase().take(2)) },
                        modifier = Modifier.weight(1f),
                        label = { Text("Country code") },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = draft.preferredLanguage,
                        onValueChange = { draft = draft.copy(preferredLanguage = it.lowercase().take(2)) },
                        modifier = Modifier.weight(1f),
                        label = { Text("Language") },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                        singleLine = true
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = draft.notes,
                    onValueChange = { draft = draft.copy(notes = it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 96.dp),
                    label = { Text("Optional notes") },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                    minLines = 3
                )
            }

            statusMessage?.let { message ->
                item {
                    Text(
                        text = message,
                        color = if (message.startsWith("Offline speech unavailable") || message.startsWith("Offline speech error")) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onBackground
                        }
                    )
                }
            }

            if (validationErrors.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        validationErrors.forEach { error ->
                            Text(error, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        when (val result = draft.toPatient()) {
                            is PatientRegistrationResult.Valid -> {
                                validationErrors = emptyList()
                                onSavePatient(result.patient)
                            }

                            is PatientRegistrationResult.Invalid -> {
                                validationErrors = result.errors
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = activeField == null
                ) {
                    Text("Confirm and Add")
                }
            }
        }
    }
}

@Composable
private fun RegistrationField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    prompt: String,
    isListening: Boolean,
    onListen: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(prompt, style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
            singleLine = true
        )
        OutlinedButton(
            onClick = onListen,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isListening
        ) {
            Text(if (isListening) "Listening Offline..." else prompt)
        }
    }
}

private fun promptFor(field: PatientRegistrationField): String {
    return when (field) {
        PatientRegistrationField.NAME -> "Speak patient name"
        PatientRegistrationField.AGE -> "Speak patient age"
        PatientRegistrationField.PREGNANCY_WEEKS -> "How many weeks pregnant?"
        PatientRegistrationField.VILLAGE -> "Speak village name"
    }
}

private enum class RegistrationLanguage(
    val label: String,
    val code: String,
    val languageTags: List<String>
) {
    EN("EN", "en", listOf("en-IN", "en-US", "en")),
    HI("HI", "hi", listOf("hi-IN", "en-IN", "en")),
    ES("ES", "es", listOf("es-PE", "es-ES", "en")),
    SW("SW", "sw", listOf("sw-KE", "en"))
}
