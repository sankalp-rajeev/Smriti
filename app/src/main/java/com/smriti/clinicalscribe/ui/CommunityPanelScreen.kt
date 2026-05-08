package com.smriti.clinicalscribe.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun CommunityPanelScreen(
    panel: CommunityPanel,
    onOpenPatient: (String) -> Unit,
    onBack: () -> Unit
) {
    SmritiScreenSurface {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(SmritiSpacing.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(SmritiSpacing.CardGap)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Community panel", style = MaterialTheme.typography.headlineSmall)
                    Text("Local view of your saved patient roster.", style = MaterialTheme.typography.bodyLarge)
                    SmritiSecondaryButton("Back to patients", onBack)
                }
            }

            item {
                SmritiCard(tone = SmritiTone.Info) {
                    Text("Today's focus", fontWeight = FontWeight.SemiBold)
                    Text(panel.narrative, style = MaterialTheme.typography.bodyLarge)
                    Text("Saved on this device. No internet needed for these counts.", style = MaterialTheme.typography.bodyMedium)
                }
            }

            item {
                SmritiCard(tone = if (panel.attentionPatients.isEmpty()) SmritiTone.Success else SmritiTone.Caution) {
                    Text("Needs attention", fontWeight = FontWeight.SemiBold)
                    MetricLine("Attention cases", panel.attentionPatients.size.toString())
                    MetricLine("Urgent review saved", panel.urgentReferralSavedCount.toString())
                    MetricLine("History signal", panel.historySignalCount.toString())
                    MetricLine("No recent visit", panel.missedRecentVisitCount.toString())
                }
            }

            item {
                SmritiCard(tone = SmritiTone.Muted) {
                    Text("Follow-ups", fontWeight = FontWeight.SemiBold)
                    MetricLine("Open follow-ups", panel.openFollowUpCount.toString())
                    MetricLine("Follow-ups overdue", panel.overdueFollowUpCount.toString())
                    MetricLine("Due or upcoming", panel.dueOrUpcomingFollowUpCount.toString())
                }
            }

            if (panel.todayFocus.isNotEmpty()) {
                item { SmritiSectionHeader("Priority list") }
                items(panel.todayFocus.take(6)) { line ->
                    CommunityPanelPatientRow(line = line, onOpenPatient = onOpenPatient)
                }
            }

            item {
                SmritiCard {
                    Text("Pregnancy stage", fontWeight = FontWeight.SemiBold)
                    MetricLine("Patients in roster", panel.totalPatients.toString())
                    MetricLine("Pregnancy weeks recorded", panel.pregnantPatients.toString())
                    MetricLine("Third trimester", panel.thirdTrimesterCount.toString())
                    MetricLine("Near term", panel.nearTermCount.toString())
                }
            }

            item {
                SmritiCard(tone = SmritiTone.Muted) {
                    Text("Languages", fontWeight = FontWeight.SemiBold)
                    Text(panel.noteLanguagesRepresented.joinToString().ifBlank { "Not recorded" }, style = MaterialTheme.typography.bodyLarge)
                    Text("Countries: ${panel.countriesRepresented.joinToString().ifBlank { "Not recorded" }}", style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (panel.followUpPatients.isNotEmpty()) {
                item { SmritiSectionHeader("Follow-up patients") }
                items(panel.followUpPatients.take(6)) { line ->
                    CommunityPanelPatientRow(line = line, onOpenPatient = onOpenPatient)
                }
            }

            if (panel.urgentPatients.isNotEmpty()) {
                item { SmritiSectionHeader("Urgent review saved") }
                items(panel.urgentPatients.take(6)) { line ->
                    CommunityPanelPatientRow(line = line, onOpenPatient = onOpenPatient)
                }
            }

            item {
                SmritiCard(tone = SmritiTone.Info) {
                    Text("Offline proof", fontWeight = FontWeight.SemiBold)
                    Text("This panel uses patients, saved visits, referral flags, and follow-up tasks stored locally.", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun MetricLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CommunityPanelPatientRow(
    line: CommunityPanelLine,
    onOpenPatient: (String) -> Unit
) {
    val tone = when (line.tone) {
        PatientChipTone.Urgent -> SmritiTone.Urgent
        PatientChipTone.Caution -> SmritiTone.Caution
        PatientChipTone.Routine -> SmritiTone.Muted
    }
    SmritiCard(tone = tone) {
        Text(line.patientName, style = MaterialTheme.typography.titleMedium)
        SmritiStatusChip(line.label, tone = tone)
        Text(line.detail, style = MaterialTheme.typography.bodyLarge)
        TextButton(
            onClick = { onOpenPatient(line.patientId) },
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Text("Open visit")
        }
    }
}
