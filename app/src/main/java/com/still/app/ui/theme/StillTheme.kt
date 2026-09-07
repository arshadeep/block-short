package com.still.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Ink = Color(0xFF111610)
val Paper = Color(0xFFF2F0E8)
val PaperMuted = Color(0xFFE4E5D9)
val Sage = Color(0xFFCAD7BC)
val Moss = Color(0xFF405B3F)
val Sunrise = Color(0xFFFFB45E)
val Coral = Color(0xFFF0785C)
val Cream = Color(0xFFFFF9EC)

private val StillColors = darkColorScheme(
    primary = Sunrise,
    onPrimary = Ink,
    secondary = Sage,
    onSecondary = Ink,
    background = Ink,
    onBackground = Paper,
    surface = Color(0xFF1A2119),
    onSurface = Paper,
    surfaceVariant = Color(0xFF283126),
    onSurfaceVariant = Color(0xFFBCC6B5),
    error = Coral,
    onError = Ink,
    outline = Color(0xFF536050),
)

private val StillTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 52.sp,
        lineHeight = 51.sp,
        letterSpacing = (-1.5).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 38.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 27.sp,
        lineHeight = 30.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 17.sp,
        lineHeight = 25.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 15.sp,
        lineHeight = 21.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 1.2.sp,
    ),
)

@Composable
fun StillTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StillColors,
        typography = StillTypography,
        content = content,
    )
}
