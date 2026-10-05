package com.parallel.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object ParallelPalette {
    val Background = Color(0xFF0A0B0F)
    val Surface = Color(0xFF13151B)
    val SurfaceRaised = Color(0xFF191C23)
    val Outline = Color(0xFF2A2E38)
    val TextPrimary = Color(0xFFE9EAF0)
    val TextSecondary = Color(0xFFADB2BE)
    val TextMuted = Color(0xFF858B99)
    val Accent = Color(0xFFAAB6D4)
    val AccentSurface = Color(0xFF202532)
    val Error = Color(0xFFE1A2A5)
}

private val ParallelColors = darkColorScheme(
    primary = ParallelPalette.Accent,
    onPrimary = Color(0xFF11141C),
    secondary = Color(0xFF8793B1),
    background = ParallelPalette.Background,
    surface = ParallelPalette.Surface,
    surfaceVariant = ParallelPalette.SurfaceRaised,
    outline = ParallelPalette.Outline,
    error = ParallelPalette.Error,
    onBackground = ParallelPalette.TextPrimary,
    onSurface = ParallelPalette.TextPrimary,
    onSurfaceVariant = ParallelPalette.TextSecondary
)

private val ParallelTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = (-1.1).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 29.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.55).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 19.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.15).sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 23.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 21.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 19.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        letterSpacing = 1.05.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 1.05.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 0.9.sp
    )
)

@Composable
fun ParallelTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ParallelColors,
        typography = ParallelTypography,
        content = content
    )
}
