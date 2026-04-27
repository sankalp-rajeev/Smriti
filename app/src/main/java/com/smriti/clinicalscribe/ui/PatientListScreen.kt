package com.smriti.clinicalscribe.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smriti.clinicalscribe.data.Patient
import com.smriti.clinicalscribe.data.VisitLog

@Composable
fun PatientListScreen(
    patients: List<Patient>,
    visits: List<VisitLog>,
    isLoading: Boolean,
    onPatientSelected: (Patient) -> Unit,
    onShowSummary: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Smriti", style = MaterialTheme.typography.headlineMedium)
                    Text("Offline visit copilot", style = MaterialTheme.typography.bodyMedium)
                }
                OutlinedButton(onClick = onShowSummary, enabled = !isLoading) {
                    Text("Summary")
                }
            }

            if (isLoading) {
                Text("Loading local patient roster...")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(patients) { patient ->
                        val visitCount = visits.count { it.patientId == patient.id }
                        PatientRow(
                            patient = patient,
                            visitCount = visitCount,
                            onPatientSelected = onPatientSelected
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PatientRow(
    patient: Patient,
    visitCount: Int,
    onPatientSelected: (Patient) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(patient.displayLabel(), style = MaterialTheme.typography.titleLarge)
            Text(
                text = "${patient.village} - ${patient.riskSummary}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "$visitCount saved visit(s)",
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(onClick = { onPatientSelected(patient) }) {
                Text("Start Visit")
            }
        }
    }
}
