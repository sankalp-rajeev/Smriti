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

@Composable
fun OfflineProofCard(
    reasoningModeLabel: String,
    agentModeLabel: String,
    realGemmaModelStatusLabel: String,
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
            Text("Network required: No")
            Text("Protocol source: Local asset JSON")
            Text("Reasoning engine: $reasoningModeLabel")
            Text("Active reasoning mode: $agentModeLabel")
            Text("Fallback active: No")
            Text("Audio storage: Local app-private .m4a file")
            Text("Transcript source: Simulated / REAL_ASR_PENDING when audio is attached")
            Text("Cloud APIs: None")
            Text("LiteRT-LM dependency: Present")
            Text("Real Gemma model: $realGemmaModelStatusLabel")
            Text("EngineConfig: Prepared only if model found")
            Text("RealGemma text client: Scaffolded, disabled")
            Text("Inference: Disabled")
        }
    }
}
