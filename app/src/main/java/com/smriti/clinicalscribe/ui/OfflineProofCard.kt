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
    val modelReadyLabel: String
        get() = if (realGemmaModelStatusLabel.equals("Found", ignoreCase = true)) {
            "ready"
        } else {
            "Setup needed"
        }

    val compactLines: List<Pair<String, String>>
        get() = listOf(
            "Runs after setup without cloud APIs" to "",
            "Local Room database" to "saved on this device",
            "Local protocol pack" to "available",
            "LiteRT-LM Gemma 4" to modelReadyLabel,
            "CHW review required" to "before save"
        )

    val lines: List<Pair<String, String>>
        get() = listOf(
            "Runs after setup without cloud APIs" to "",
            "Local Room database" to "patient memory saved on this device",
            "Local protocol pack" to "cited guidance stored on this device",
            "LiteRT-LM Gemma 4" to modelReadyLabel,
            "Transcript input" to "editable before note generation",
            "Paper note scan" to "review before save",
            "Languages" to "EN, HI, ES, SW",
            "CHW review required" to "before any save"
        )

    val compactDisplayLines: List<String>
        get() = compactLines.toDisplayLines()

    val displayLines: List<String>
        get() = lines.toDisplayLines()
}

@Composable
fun OfflineProofCard(
    status: OfflineProofStatus,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    SmritiCard(
        tone = SmritiTone.Muted,
        modifier = modifier
    ) {
        Text("Offline proof", fontWeight = FontWeight.SemiBold)
        val proofLines = if (compact) status.compactDisplayLines else status.displayLines
        proofLines.forEach { line ->
            Text(
                text = line,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun List<Pair<String, String>>.toDisplayLines(): List<String> {
    return map { (label, value) -> if (value.isBlank()) label else "$label: $value" }
}
