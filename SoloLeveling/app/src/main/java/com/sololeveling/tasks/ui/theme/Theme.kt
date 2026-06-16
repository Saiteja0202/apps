package com.sololeveling.tasks.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AccentBlue = Color(0xFF4D7CFF)
val AccentCyan = Color(0xFF19D3FF)
val AccentViolet = Color(0xFF8B5CFF)
val AccentGold = Color(0xFFFFC857)
val Danger = Color(0xFFFF5C7A)
val HpGreen = Color(0xFF36D399)

private val DarkColors = darkColorScheme(
    primary = AccentBlue, onPrimary = Color.White,
    secondary = AccentCyan, onSecondary = Color(0xFF00202B),
    tertiary = AccentViolet,
    background = Color(0xFF070A14), onBackground = Color(0xFFEAF0FF),
    surface = Color(0xFF121830), onSurface = Color(0xFFEAF0FF),
    surfaceVariant = Color(0xFF18203F), onSurfaceVariant = Color(0xFF8A95C0),
    primaryContainer = Color(0xFF1B2550), onPrimaryContainer = Color(0xFFCBD7FF),
    outline = Color(0xFF2C3A6E), error = Danger
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF3559E0), onPrimary = Color.White,
    secondary = Color(0xFF0E8FB0), onSecondary = Color.White,
    tertiary = Color(0xFF6D43E0),
    background = Color(0xFFF4F6FC), onBackground = Color(0xFF101426),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF101426),
    surfaceVariant = Color(0xFFE9ECF7), onSurfaceVariant = Color(0xFF5A6080),
    primaryContainer = Color(0xFFDCE3FF), onPrimaryContainer = Color(0xFF12235E),
    outline = Color(0xFFC6CCE2), error = Color(0xFFD92D43)
)

private val SoloType = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.Black, fontSize = 26.sp, letterSpacing = 1.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.2.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.8.sp)
)

@Composable
fun SoloLevelingTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, typography = SoloType, content = content)
}

@Composable
fun systemGradient(): Brush = Brush.verticalGradient(
    listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surfaceVariant)
)
