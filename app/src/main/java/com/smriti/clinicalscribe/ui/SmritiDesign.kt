package com.smriti.clinicalscribe.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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

private val SmritiWarmBackground = Color(0xFFFCF7EF)
private val SmritiCardSurface = Color(0xFFFFFCF7)
private val SmritiCardSurfaceHigh = Color(0xFFF6EFE5)
private val SmritiInfoContainer = Color(0xFFDCECF6)
private val SmritiCautionContainer = Color(0xFFFFE8B8)
private val SmritiUrgentContainer = Color(0xFFFFDDE2)
private val SmritiSuccessContainer = Color(0xFFDCEFE5)
private val SmritiSoftBorder = Color(0xFFE5D9C9)

private val SmritiLightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF185C4C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = SmritiSuccessContainer,
    onPrimaryContainer = Color(0xFF062019),
    secondary = Color(0xFF446676),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = SmritiInfoContainer,
    onSecondaryContainer = Color(0xFF0B2430),
    tertiary = Color(0xFF6B5F20),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = SmritiCautionContainer,
    onTertiaryContainer = Color(0xFF211B00),
    error = Color(0xFFBA1A1A),
    errorContainer = SmritiUrgentContainer,
    onErrorContainer = Color(0xFF410002),
    background = SmritiWarmBackground,
    onBackground = Color(0xFF191C1A),
    surface = SmritiWarmBackground,
    onSurface = Color(0xFF191C1A),
    surfaceVariant = Color(0xFFE9DED0),
    onSurfaceVariant = Color(0xFF51483E),
    outline = Color(0xFF817669),
    outlineVariant = SmritiSoftBorder
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
    val containerColor = smritiToneContainer(tone)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, SmritiSoftBorder)
    ) {
        Column(
            modifier = Modifier.padding(SmritiSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
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
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(top = 6.dp)
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
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        ),
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

@Composable
fun SmritiMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    tone: SmritiTone = SmritiTone.Muted
) {
    Surface(
        modifier = modifier.heightIn(min = 82.dp),
        shape = RoundedCornerShape(20.dp),
        color = smritiToneContainer(tone),
        border = BorderStroke(1.dp, SmritiSoftBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun smritiToneContainer(tone: SmritiTone): Color {
    return when (tone) {
        SmritiTone.Default -> SmritiCardSurface
        SmritiTone.Muted -> SmritiCardSurfaceHigh
        SmritiTone.Info -> SmritiInfoContainer
        SmritiTone.Caution -> SmritiCautionContainer
        SmritiTone.Urgent -> SmritiUrgentContainer
        SmritiTone.Success -> SmritiSuccessContainer
    }
}
