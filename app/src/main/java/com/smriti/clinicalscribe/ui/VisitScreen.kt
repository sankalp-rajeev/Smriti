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
import com.smriti.clinicalscribe.data.VisitLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VisitScreen(
    patient: Patient,
    history: List<VisitLog>,
    isGenerating: Boolean,
    errorMessage: String?,
    onGenerate: (String) -> Unit,
    onBack: () -> Unit
) {
    var observationText by remember {
        mutableStateOf(
            "Meena is 32 weeks pregnant. Complaining of headache and blurred vision since yesterday. BP 150 over 95. Fetal movement reduced today."
        )
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
                        Text(patient.displayLabel(), style = MaterialTheme.typography.headlineSmall)
                        Text("Offline demo mode", style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = "${patient.pregnancyWeeks ?: "-"} weeks - ${patient.village}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    OutlinedButton(onClick = onBack) {
                        Text("Patient Roster")
                    }
                }
            }

            item {
                Text("Prior Visit History", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Review local history before entering the simulated voice observation.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (history.isEmpty()) {
                item {
                    Text("No prior visits saved for this patient.")
                }
            } else {
                items(history) { visit ->
                    HistoryCard(visit = visit)
                }
            }

            item {
                OutlinedTextField(
                    value = observationText,
                    onValueChange = { observationText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp),
                    label = { Text("Simulated voice observation text") },
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
                    onClick = { onGenerate(observationText) },
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
