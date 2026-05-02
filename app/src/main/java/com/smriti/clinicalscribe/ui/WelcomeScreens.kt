package com.smriti.clinicalscribe.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
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

@Composable
fun WelcomeScreen(
    onStartVisits: () -> Unit,
    onUserGuide: () -> Unit,
    onCheckOfflineSetup: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Smriti", style = MaterialTheme.typography.headlineLarge)
                Text("Offline health visit assistant", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                text = "Smriti helps you remember patient history, check local health guidance, prepare visit notes, and decide who needs follow-up.\n\nIt does not diagnose. A health worker must review and confirm before saving.",
                style = MaterialTheme.typography.bodyLarge
            )
            Button(
                onClick = onStartVisits,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
            ) {
                Text("Start visits")
            }
            OutlinedButton(
                onClick = onUserGuide,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text("View user guide")
            }
            OutlinedButton(
                onClick = onCheckOfflineSetup,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text("Check offline setup")
            }
            Text(
                text = "Works offline after setup • Local patient memory • On-device Gemma 4 reasoning",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun UserGuideScreen(
    onBack: () -> Unit
) {
    val sections = listOf(
        "Choose a patient" to "Search or select a patient from the list.",
        "Speak or type the visit" to "Use the sample transcript, speak your observation, or type it manually.",
        "Generate note" to "Smriti checks local patient history and local health guidance using on-device Gemma. This takes a few seconds.",
        "Review carefully" to "Smriti does not diagnose. Read the note carefully and check the referral or follow-up suggestion.",
        "Confirm and save" to "Only save after you have reviewed and confirmed.",
        "End of day" to "Open Summary to see urgent cases and follow-ups for tomorrow."
    )

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("User guide", style = MaterialTheme.typography.headlineSmall)
            }
            sections.forEachIndexed { index, (title, body) ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("${index + 1}. $title", fontWeight = FontWeight.SemiBold)
                            Text(body, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
            item {
                Text(
                    text = "If on-device reasoning is unavailable: Check that the model is installed on this device. Ask your supervisor if needed.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            item {
                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text("Back")
                }
            }
        }
    }
}

@Composable
fun SetupGuidanceScreen(
    onContinueWithoutModel: () -> Unit,
    onBack: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("One-time setup needed", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = "Smriti uses an AI model stored on this device. Your supervisor or district health team will provide the model file.\n\nOnce installed, Smriti works fully offline.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("1. Receive the model file from your supervisor")
                        Text("2. Your supervisor will install it on this device")
                        Text("3. Return to Smriti - it will be ready")
                    }
                }
            }
            item {
                Button(
                    onClick = onContinueWithoutModel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                ) {
                    Text("Continue without model (demo mode)")
                }
                Text(
                    text = "In demo mode, notes use sample guidance. Install the model for full on-device reasoning.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            item {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text("Back")
                }
            }
        }
    }
}
