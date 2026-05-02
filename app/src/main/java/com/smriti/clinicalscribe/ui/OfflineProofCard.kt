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
    val realGemmaEngineStatusLabel: String = "Loads on demand",
    val realGemmaInferenceLabel: String = "Disabled by default; manual-only",
    val realGemmaGateLabel: String = "Build gate: disabled; local gate: disabled",
    val realGemmaTextModeLabel: String = "Disabled",
    val realGemmaSubmissionModeLabel: String = "Disabled",
    val realGemmaDeveloperWarning: String? = null
) {
    val compactLines: List<Pair<String, String>>
        get() = listOf(
            "Network" to "No",
            "Patient data" to "Local Room/SQLite",
            "Protocols" to "Local JSON, country-aware",
            "Active mode" to reasoningModeLabel,
            "RealGemma text" to realGemmaTextModeLabel,
            "Direct Gemma audio" to "Blocked; transcript fallback"
        )

    val lines: List<Pair<String, String>>
        get() = listOf(
            "Network required" to "No",
            "Patient data" to "Local Room/SQLite",
            "Protocol source" to "Local JSON; country-aware retrieval",
            "Active reasoning mode" to reasoningModeLabel,
            "Cloud API" to "No",
            "RealGemma text mode" to realGemmaTextModeLabel,
            "Submission mode" to realGemmaSubmissionModeLabel,
            "Real Gemma model" to realGemmaModelStatusLabel,
            "Engine" to realGemmaEngineStatusLabel,
            "Inference" to realGemmaInferenceLabel,
            "Direct Gemma audio" to "Blocked by current public LiteRT-LM Android/Kotlin path; using offline speech/transcript fallback"
        ) + realGemmaDeveloperWarning?.let { warning ->
            listOf("Developer warning" to warning)
        }.orEmpty()
}

@Composable
fun OfflineProofCard(
    status: OfflineProofStatus,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp)
        ) {
            Text("Offline Proof", fontWeight = FontWeight.SemiBold)
            val proofLines = if (compact) status.compactLines else status.lines
            proofLines.forEach { (label, value) ->
                Text("$label: $value", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
