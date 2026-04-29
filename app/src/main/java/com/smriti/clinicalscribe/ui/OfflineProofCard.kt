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
            "Patient data" to "Local Room/SQLite",
            "Protocol source" to "Local JSON; country-aware retrieval",
            "Active reasoning mode" to reasoningModeLabel,
            "Mode guard" to "MockGemmaAgent by default; RealGemmaAgent only in developer mode",
            "Real Gemma model" to realGemmaModelStatusLabel,
            "Inference" to realGemmaInferenceLabel,
            "Direct Gemma audio" to "Blocked by current public LiteRT-LM Android/Kotlin path; using offline speech/transcript fallback"
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
