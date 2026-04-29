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
    val realGemmaReadinessLabel: String,
    val realGemmaInferenceLabel: String = "Disabled by default; manual-only",
    val realGemmaGateLabel: String = "Build gate: disabled; local gate: disabled",
    val realGemmaDeveloperWarning: String? = null
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
            "RealGemma dev gate" to realGemmaGateLabel,
            "Inference" to realGemmaInferenceLabel,
            "Backend" to "CPU when developer text inference is enabled"
        ) + realGemmaDeveloperWarning?.let { warning ->
            listOf("Developer warning" to warning)
        }.orEmpty()
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
