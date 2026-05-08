package com.smriti.clinicalscribe.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun WelcomeScreen(
    onStartVisits: () -> Unit,
    onUserGuide: () -> Unit,
    onCheckOfflineSetup: () -> Unit
) {
    SmritiScreenSurface {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Smriti", style = MaterialTheme.typography.headlineLarge)
                    Text(
                        "For the ones who show up.",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Smriti helps health workers carry each visit forward, with patient context, structured notes, and follow-up support ready for review.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                MemoryThreadMotif()
            }
            item {
                SmritiCard(tone = SmritiTone.Default) {
                    Text("Smriti means memory.", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "It is built for the people who carry care from home to home, helping them remember what matters across visits.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        "Smriti does not diagnose. Health worker must review and confirm before saving.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            item { PillarCard("Remember every visit", "Keep each patient's story moving forward, from one home visit to the next.") }
            item { PillarCard("Support every worker", "Prepare structured notes and review support while the health worker stays in control.") }
            item { PillarCard("Close every loop", "Bring follow-ups, urgent reviews, and end-of-day summaries back into view.") }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        SmritiStatusChip("Review before saving", tone = SmritiTone.Info, modifier = Modifier.weight(1f))
                        SmritiStatusChip("Patient context", tone = SmritiTone.Success, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        SmritiStatusChip("Follow-up support", tone = SmritiTone.Caution, modifier = Modifier.weight(1f))
                        SmritiStatusChip("Works after setup", tone = SmritiTone.Muted, modifier = Modifier.weight(1f))
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SmritiPrimaryButton("Start visits", onStartVisits)
                    SmritiSecondaryButton("Check offline setup", onCheckOfflineSetup)
                    SmritiTonalButton("View user guide", onUserGuide)
                }
            }
        }
    }
}

@Composable
private fun MemoryThreadMotif() {
    val lineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.36f)
    val dotColor = MaterialTheme.colorScheme.primary
    val middleColor = MaterialTheme.colorScheme.tertiary
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
    ) {
        val y = size.height / 2f
        val startX = 18.dp.toPx()
        val midX = size.width / 2f
        val endX = size.width - 18.dp.toPx()
        drawLine(
            color = lineColor,
            start = Offset(startX, y),
            end = Offset(endX, y),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawCircle(color = dotColor, radius = 7.dp.toPx(), center = Offset(startX, y))
        drawCircle(color = middleColor, radius = 9.dp.toPx(), center = Offset(midX, y))
        drawCircle(color = dotColor, radius = 7.dp.toPx(), center = Offset(endX, y))
    }
}

@Composable
private fun PillarCard(title: String, body: String) {
    SmritiCard(tone = SmritiTone.Info) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(body, style = MaterialTheme.typography.bodyLarge)
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

    SmritiScreenSurface {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(SmritiSpacing.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(SmritiSpacing.CardGap)
        ) {
            item {
                Text("User guide", style = MaterialTheme.typography.headlineSmall)
            }
            sections.forEachIndexed { index, (title, body) ->
                item {
                    SmritiCard {
                        Text("${index + 1}. $title", fontWeight = FontWeight.SemiBold)
                        Text(body, style = MaterialTheme.typography.bodyLarge)
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
                SmritiPrimaryButton("Back", onBack)
            }
        }
    }
}

@Composable
fun SetupGuidanceScreen(
    onContinueWithoutModel: () -> Unit,
    onBack: () -> Unit
) {
    SmritiScreenSurface {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(SmritiSpacing.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(SmritiSpacing.CardGap)
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
                SmritiCard {
                    Text("1. Receive the model file from your supervisor")
                    Text("2. Your supervisor will install it on this device")
                    Text("3. Return to Smriti - it will be ready")
                }
            }
            item {
                SmritiPrimaryButton("Continue without model (demo mode)", onContinueWithoutModel)
                Text(
                    text = "In demo mode, notes use sample guidance. Install the model for full on-device reasoning.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            item {
                SmritiSecondaryButton("Back", onBack)
            }
        }
    }
}

@Composable
fun OfflineSetupScreen(
    status: OfflineProofStatus,
    onBack: () -> Unit
) {
    SmritiScreenSurface {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(SmritiSpacing.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(SmritiSpacing.CardGap)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Offline setup", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "These checks stay on this setup screen so the patient roster can stay focused on visits.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            item {
                OfflineProofCard(status = status)
            }
            item {
                SmritiPrimaryButton("Back", onBack)
            }
        }
    }
}
