package com.smriti.clinicalscribe.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
    isResettingDemoData: Boolean,
    onResetDemoData: () -> Unit,
    onBack: () -> Unit
) {
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
                        Text("End-of-Day Supervisor Summary", style = MaterialTheme.typography.headlineSmall)
                        Text("Offline demo mode", style = MaterialTheme.typography.labelLarge)
                        Text("Local brief from confirmed visits", style = MaterialTheme.typography.bodyMedium)
                    }
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isResettingDemoData
                    ) {
                        Text("Back to Patient Roster")
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
                OutlinedButton(
                    onClick = onResetDemoData,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isResettingDemoData
                ) {
                    Text(if (isResettingDemoData) "Resetting Demo Data..." else "Reset Demo Data")
                }
                Text(
                    text = "Demo mode only: clears saved mock visits and referral flags, then restores the original Meena history.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 6.dp)
                )
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
