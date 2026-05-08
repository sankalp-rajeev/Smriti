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
            "Works offline after setup" to "",
            "Patient memory" to "saved on this device",
            "Health guidance" to "stored on this device",
            "On-device Gemma" to modelReadyLabel,
            "Paper note scan" to "available",
            "Cloud APIs" to "none",
            "Gemma audio transcript" to "editable only"
        )

    val lines: List<Pair<String, String>>
        get() = listOf(
            "Patient memory" to "Room/SQLite local storage",
            "Health guidance" to "Local JSON pack: 46 chunks, 6 countries",
            "Model file" to modelReadyLabel,
            "Engine state" to if (modelReadyLabel == "ready") "ready for use" else "manual only",
            "Transcript source" to "offline speech or manual typing",
            "Gemma audio transcript" to "Editable transcript only",
            "Paper note scan" to "Available",
            "Vision support" to "Uses local Gemma vision",
            "Scan review" to "Review required before save",
            "Cloud OCR" to "none",
            "Languages" to "EN, HI, ES, SW",
            "Cloud APIs" to "none",
            "Audio save" to "none from audio alone"
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
        Text("Offline setup checklist", fontWeight = FontWeight.SemiBold)
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
