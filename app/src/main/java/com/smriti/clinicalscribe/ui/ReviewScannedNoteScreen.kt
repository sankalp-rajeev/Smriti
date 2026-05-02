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
import com.smriti.clinicalscribe.reasoning.PaperNoteVisionExtraction

@Composable
fun ReviewScannedNoteScreen(
    currentPatient: Patient,
    matchedPatient: Patient?,
    extraction: PaperNoteVisionExtraction,
    isSaving: Boolean,
    saveStatusMessage: String?,
    onSave: (
        targetPatient: Patient,
        editedPatientName: String,
        editedVisitDate: String,
        editedBloodPressure: String,
        editedSymptoms: List<String>,
        editedFollowUpPlan: String
    ) -> Unit,
    onCancel: () -> Unit
) {
    var patientName by remember(extraction) { mutableStateOf(extraction.patientName) }
    var visitDate by remember(extraction) { mutableStateOf(extraction.visitDate) }
    var bloodPressure by remember(extraction) { mutableStateOf(extraction.bloodPressure) }
    var symptomsText by remember(extraction) { mutableStateOf(extraction.symptoms.joinToString(separator = "\n")) }
    var followUpPlan by remember(extraction) { mutableStateOf(extraction.followUpPlan) }
    var targetPatient by remember(currentPatient, matchedPatient) {
        mutableStateOf<Patient?>(null)
    }
    val matchedCurrent = matchedPatient?.id == currentPatient.id

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Review scanned note", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "Text was extracted from a paper note. Review before saving.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.heightIn(min = 48.dp),
                        enabled = !isSaving
                    ) {
                        Text("Cancel")
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(extraction.confidence.chwMessage, fontWeight = FontWeight.SemiBold)
                        Text(
                            "No diagnosis or referral decision was generated from this image.",
                            style = MaterialTheme.typography.bodyMedium
                        )
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
                        if (matchedCurrent || matchedPatient == null) {
                            Text("Save this scanned note to ${currentPatient.name}?", fontWeight = FontWeight.SemiBold)
                            Button(
                                onClick = { targetPatient = currentPatient },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp),
                                enabled = !isSaving
                            ) {
                                Text("Yes, save to this patient")
                            }
                        } else {
                            Text(
                                "This looks like ${matchedPatient.name} in your roster. Link this note to her record?",
                                fontWeight = FontWeight.SemiBold
                            )
                            Button(
                                onClick = { targetPatient = matchedPatient },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp),
                                enabled = !isSaving
                            ) {
                                Text("Yes, link")
                            }
                            OutlinedButton(
                                onClick = { targetPatient = currentPatient },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp),
                                enabled = !isSaving
                            ) {
                                Text("Save to current patient")
                            }
                        }
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            enabled = !isSaving
                        ) {
                            Text("Cancel")
                        }
                        targetPatient?.let { target ->
                            Text("Selected record: ${target.name}", style = MaterialTheme.typography.bodyMedium)
                        }
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
                        OutlinedTextField(
                            value = patientName,
                            onValueChange = { patientName = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Patient name") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = visitDate,
                            onValueChange = { visitDate = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Date") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = bloodPressure,
                            onValueChange = { bloodPressure = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("BP") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = symptomsText,
                            onValueChange = { symptomsText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 110.dp),
                            label = { Text("Symptoms") },
                            minLines = 3
                        )
                        OutlinedTextField(
                            value = followUpPlan,
                            onValueChange = { followUpPlan = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Follow-up plan") }
                        )
                    }
                }
            }

            saveStatusMessage?.let { message ->
                item {
                    Text(message, style = MaterialTheme.typography.bodyLarge)
                }
            }

            item {
                Button(
                    onClick = {
                        val target = targetPatient ?: return@Button
                        onSave(
                            target,
                            patientName,
                            visitDate,
                            bloodPressure,
                            symptomsText.lines().map { it.trim() }.filter { it.isNotBlank() },
                            followUpPlan
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    enabled = !isSaving && targetPatient != null
                ) {
                    Text(if (isSaving) "Saving..." else "Save to patient history")
                }
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .heightIn(min = 48.dp),
                    enabled = !isSaving
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}
