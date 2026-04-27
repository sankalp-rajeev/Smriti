package com.smriti.clinicalscribe.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class OfflineProofStatus(
    val reasoningModeLabel: String,
    val realGemmaModelStatusLabel: String,
    val realGemmaReadinessLabel: String
) {
    val lines: List<Pair<String, String>>
        get() = listOf(
            "Network required" to "No",
            "Protocol source" to "Local asset JSON",
            "Active reasoning mode" to reasoningModeLabel,
            "LiteRT-LM dependency" to "Present",
            "Real Gemma model" to realGemmaModelStatusLabel,
            "EngineConfig" to "Ready when model found; Engine manual-only",
            "RealGemma readiness" to realGemmaReadinessLabel,
            "Inference" to "Disabled by default; manual-only"
        )
}

@Composable
fun OfflineProofCard(
    status: OfflineProofStatus,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Offline Proof", fontWeight = FontWeight.SemiBold)
            status.lines.forEach { (label, value) ->
                Text("$label: $value")
            }
        }
    }
}
