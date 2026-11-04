package com.example.fadhkuruni

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/** خط قمرة للنصوص، وخط غروب للعناوين. */
private val Qomra = FontFamily(
    Font(R.font.qomra_regular, FontWeight.Normal),
    Font(R.font.qomra_medium, FontWeight.Medium),
    Font(R.font.qomra_bold, FontWeight.Bold),
)

private val Ghroob = FontFamily(
    Font(R.font.ghroob_bold, FontWeight.Bold),
)

private val baseTypography = Typography()

private val AppTypography = Typography(
    displaySmall = baseTypography.displaySmall.copy(fontFamily = Qomra),
    headlineSmall = baseTypography.headlineSmall.copy(fontFamily = Ghroob),
    titleLarge = baseTypography.titleLarge.copy(fontFamily = Ghroob),
    titleMedium = baseTypography.titleMedium.copy(fontFamily = Qomra, fontWeight = FontWeight.Bold),
    titleSmall = baseTypography.titleSmall.copy(fontFamily = Qomra),
    bodyLarge = baseTypography.bodyLarge.copy(fontFamily = Qomra),
    bodyMedium = baseTypography.bodyMedium.copy(fontFamily = Qomra),
    bodySmall = baseTypography.bodySmall.copy(fontFamily = Qomra),
    labelLarge = baseTypography.labelLarge.copy(fontFamily = Qomra),
    labelMedium = baseTypography.labelMedium.copy(fontFamily = Qomra),
    labelSmall = baseTypography.labelSmall.copy(fontFamily = Qomra),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B6B50),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDEBDD),
    onPrimaryContainer = Color(0xFF052417),
    secondary = Color(0xFFB08A3E),
    secondaryContainer = Color(0xFFF3E6C4),
    background = Color(0xFFF7F5EF),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEFEDE4),
    onSurface = Color(0xFF1B1B1B),
    onSurfaceVariant = Color(0xFF4A4A44),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FD6B6),
    onPrimary = Color(0xFF00382A),
    primaryContainer = Color(0xFF1B5140),
    onPrimaryContainer = Color(0xFFCDEBDD),
    secondary = Color(0xFFE0C27A),
    secondaryContainer = Color(0xFF4A3D1E),
    background = Color(0xFF111412),
    surface = Color(0xFF181C1A),
    surfaceVariant = Color(0xFF262B28),
    onSurface = Color(0xFFE3E3DE),
    onSurfaceVariant = Color(0xFFC3C8C2),
)

@Composable
fun AdhkarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
