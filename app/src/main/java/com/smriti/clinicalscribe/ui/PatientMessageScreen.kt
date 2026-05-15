package com.smriti.clinicalscribe.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PatientMessageScreen(
    patientName: String,
    initialMessage: String,
    copyStatusMessage: String?,
    shareStatusMessage: String?,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onBack: () -> Unit
) {
    var message by remember(initialMessage) { mutableStateOf(initialMessage) }

    SmritiScreenSurface {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(SmritiSpacing.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(SmritiSpacing.CardGap)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Patient message for review", style = MaterialTheme.typography.headlineSmall)
                    Text(patientName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    SmritiSecondaryButton("Back", onBack)
                }
            }

            item {
                SmritiCard(tone = SmritiTone.Info) {
                    Text("Editable leave-behind", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Review this message with the patient before copying or opening the phone share sheet.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text("Smriti does not auto-send anything.", style = MaterialTheme.typography.bodyMedium)
                }
            }

            item {
                SmritiCard {
                    Text("Message card", fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = SmritiSpacing.PrimaryButtonMinHeight * 5),
                        label = { Text("Message for patient") },
                        minLines = 8
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    SmritiSecondaryButton(
                        text = "Copy",
                        onClick = { onCopy(message) },
                        modifier = Modifier.weight(1f),
                        enabled = message.isNotBlank()
                    )
                    SmritiPrimaryButton(
                        text = "Share",
                        onClick = { onShare(message) },
                        modifier = Modifier.weight(1f),
                        enabled = message.isNotBlank()
                    )
                }
                copyStatusMessage?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                shareStatusMessage?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}
