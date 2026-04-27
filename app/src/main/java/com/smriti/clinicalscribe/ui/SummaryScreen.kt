package com.smriti.clinicalscribe.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smriti.clinicalscribe.reasoning.SupervisorSummary

@Composable
fun SummaryScreen(
    summary: SupervisorSummary,
    onBack: () -> Unit
) {
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
                        Text("Supervisor Summary", style = MaterialTheme.typography.headlineSmall)
                        Text("End-of-day local brief", style = MaterialTheme.typography.bodyMedium)
                    }
                    OutlinedButton(onClick = onBack) {
                        Text("Roster")
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
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(summary.narrative)
                        Text("Total visits: ${summary.totalVisits}", fontWeight = FontWeight.SemiBold)
                        Text("Referral flags: ${summary.referralsFlagged}", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            item {
                Text("Urgent Cases", style = MaterialTheme.typography.titleMedium)
            }
            if (summary.urgentCases.isEmpty()) {
                item { Text("No urgent referral cases saved yet.") }
            } else {
                items(summary.urgentCases) { itemText -> SummaryItem(itemText) }
            }

            item {
                Text("Follow-ups", style = MaterialTheme.typography.titleMedium)
            }
            if (summary.followUpsDue.isEmpty()) {
                item { Text("No follow-up tasks saved yet.") }
            } else {
                items(summary.followUpsDue) { itemText -> SummaryItem(itemText) }
            }
        }
    }
}

@Composable
private fun SummaryItem(text: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
