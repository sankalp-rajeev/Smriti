package com.smriti.clinicalscribe.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

object SmritiSpacing {
    val ScreenPadding = 16.dp
    val CardPadding = 16.dp
    val CardGap = 12.dp
    val SectionGap = 24.dp
    val ButtonMinHeight = 48.dp
    val PrimaryButtonMinHeight = 52.dp
}

private val SmritiLightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF1F6F5B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC7EBDD),
    onPrimaryContainer = Color(0xFF062019),
    secondary = Color(0xFF52665C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD5E7DD),
    onSecondaryContainer = Color(0xFF102019),
    tertiary = Color(0xFF6B5F20),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF4E5A3),
    onTertiaryContainer = Color(0xFF211B00),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFBFCF8),
    onBackground = Color(0xFF191C1A),
    surface = Color(0xFFFBFCF8),
    onSurface = Color(0xFF191C1A),
    surfaceVariant = Color(0xFFDDE5DF),
    onSurfaceVariant = Color(0xFF414942),
    outline = Color(0xFF717971)
)

@Composable
fun SmritiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SmritiLightColorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}

@Composable
fun SmritiScreenSurface(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
        content = content
    )
}

enum class SmritiTone {
    Default,
    Muted,
    Info,
    Caution,
    Urgent,
    Success
}

@Composable
fun SmritiCard(
    modifier: Modifier = Modifier,
    tone: SmritiTone = SmritiTone.Default,
    content: @Composable ColumnScope.() -> Unit
) {
    val containerColor = when (tone) {
        SmritiTone.Default -> MaterialTheme.colorScheme.surfaceContainer
        SmritiTone.Muted -> MaterialTheme.colorScheme.surfaceContainerHigh
        SmritiTone.Info -> MaterialTheme.colorScheme.secondaryContainer
        SmritiTone.Caution -> MaterialTheme.colorScheme.tertiaryContainer
        SmritiTone.Urgent -> MaterialTheme.colorScheme.errorContainer
        SmritiTone.Success -> MaterialTheme.colorScheme.primaryContainer
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(SmritiSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
fun SmritiSectionHeader(label: String, modifier: Modifier = Modifier) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(top = 4.dp)
    )
}

@Composable
fun SmritiPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SmritiSpacing.PrimaryButtonMinHeight)
    ) {
        Text(text)
    }
}

@Composable
fun SmritiSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SmritiSpacing.ButtonMinHeight)
    ) {
        Text(text)
    }
}

@Composable
fun SmritiTonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SmritiSpacing.ButtonMinHeight)
    ) {
        Text(text)
    }
}

@Composable
fun SmritiStatusChip(
    label: String,
    tone: SmritiTone = SmritiTone.Muted,
    modifier: Modifier = Modifier
) {
    val containerColor = when (tone) {
        SmritiTone.Urgent -> MaterialTheme.colorScheme.errorContainer
        SmritiTone.Caution -> MaterialTheme.colorScheme.tertiaryContainer
        SmritiTone.Success -> MaterialTheme.colorScheme.primaryContainer
        SmritiTone.Info -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    Row(
        modifier = modifier
            .background(containerColor, RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
    }
}
