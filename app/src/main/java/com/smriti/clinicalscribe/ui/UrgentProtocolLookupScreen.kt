package com.smriti.clinicalscribe.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UrgentProtocolLookupScreen(
    patientName: String?,
    patientContextLabel: String,
    result: UrgentProtocolLookupResult?,
    onLookup: (List<UrgentProtocolSign>, String) -> Unit,
    onBack: () -> Unit
) {
    var selectedSigns by remember(patientName) { mutableStateOf<Set<UrgentProtocolSign>>(emptySet()) }
    var freeText by remember(patientName) { mutableStateOf("") }
    val canLookup = selectedSigns.isNotEmpty() || freeText.isNotBlank()

    SmritiScreenSurface {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(SmritiSpacing.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(SmritiSpacing.CardGap)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Urgent protocol lookup", style = MaterialTheme.typography.headlineSmall)
                        TextButton(onClick = onBack) {
                            Text("Back")
                        }
                    }
                    Text("Check urgent guidance from local health guidance.", style = MaterialTheme.typography.bodyLarge)
                    Text(patientName ?: "No patient selected", style = MaterialTheme.typography.bodyMedium)
                    Text(patientContextLabel, style = MaterialTheme.typography.bodyMedium)
                }
            }

            item {
                SmritiCard(tone = SmritiTone.Info) {
                    Text("Observed signs", fontWeight = FontWeight.SemiBold)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UrgentProtocolLookupSigns.all.forEach { sign ->
                            val selected = sign in selectedSigns
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    selectedSigns = if (selected) {
                                        selectedSigns - sign
                                    } else {
                                        selectedSigns + sign
                                    }
                                },
                                label = { Text(sign.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            )
                        }
                    }
                    OutlinedTextField(
                        value = freeText,
                        onValueChange = { freeText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        label = { Text("Optional observation") },
                        minLines = 3
                    )
                    SmritiPrimaryButton(
                        text = "Check urgent guidance",
                        onClick = { onLookup(selectedSigns.toList(), freeText) },
                        enabled = canLookup
                    )
                }
            }

            result?.let { lookupResult ->
                item {
                    LookupResultCard(result = lookupResult)
                }
            }
        }
    }
}

@Composable
private fun LookupResultCard(result: UrgentProtocolLookupResult) {
    if (!result.hasGuidance) {
        SmritiCard(tone = SmritiTone.Caution) {
            Text("No matching local guidance found", fontWeight = FontWeight.SemiBold)
            Text(
                "Document the observation and contact a supervisor or health facility.",
                style = MaterialTheme.typography.bodyLarge
            )
            Text("This is not a diagnosis. Follow local protocol.", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    val chunk = result.guidanceChunk ?: return
    SmritiCard(tone = if (result.urgentReviewMayBeNeeded) SmritiTone.Urgent else SmritiTone.Info) {
        if (result.urgentReviewMayBeNeeded) {
            Text("Urgent review may be needed", fontWeight = FontWeight.SemiBold)
        } else {
            Text("Local guidance checked", fontWeight = FontWeight.SemiBold)
        }
        Text(
            "Observed: ${observedText(result)}",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(chunk.text, style = MaterialTheme.typography.bodyLarge)
        Text("Health guidance used: ${chunk.citation}", style = MaterialTheme.typography.bodyMedium)
        Text("This is not a diagnosis. Follow local protocol and supervisor guidance.", style = MaterialTheme.typography.bodyMedium)
        SmritiSectionHeader("Next steps")
        Text("1. Document the observation.", style = MaterialTheme.typography.bodyMedium)
        Text("2. Contact supervisor or health facility.", style = MaterialTheme.typography.bodyMedium)
        Text("3. Do not delay urgent review when danger signs are present.", style = MaterialTheme.typography.bodyMedium)
        Text("No visit, referral flag, or follow-up task is saved from this lookup.", style = MaterialTheme.typography.bodyMedium)
    }
}

private fun observedText(result: UrgentProtocolLookupResult): String {
    val values = result.observedSigns + result.freeText.takeIf { it.isNotBlank() }.orEmpty()
    return values.filter { it.isNotBlank() }.joinToString().ifBlank { "Observation entered" }
}
