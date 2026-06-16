package com.friendschat.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/*
 * "Aurora" — a soft, pleasant pastel theme for GENZ. Airy lavender-white light
 * mode and a calm indigo-night dark mode, with a friendly periwinkle → pink
 * brand gradient and mint accents. Designed to feel gentle and welcoming.
 */

// Brand accents (shared)
val Periwinkle = Color(0xFF7C6CF0)
val PeriwinkleLight = Color(0xFFB8AEFF)
val Mint = Color(0xFF2FBFA8)
val MintLight = Color(0xFF6FE0CE)
val Blossom = Color(0xFFFF8FB1)
val BlossomLight = Color(0xFFFFB0C8)
val SeenBlue = Color(0xFF3CC8FF)

private val LightColors = lightColorScheme(
    primary = Periwinkle,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7E3FF),
    onPrimaryContainer = Color(0xFF241A66),
    secondary = Mint,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFF5EE),
    onSecondaryContainer = Color(0xFF00382F),
    tertiary = Blossom,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE0E9),
    onTertiaryContainer = Color(0xFF5A1733),
    background = Color(0xFFF8F6FF),
    onBackground = Color(0xFF1C1B2E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1B2E),
    surfaceVariant = Color(0xFFECE9F7),
    onSurfaceVariant = Color(0xFF5E5A72),
    outline = Color(0xFFD9D5EC),
    error = Color(0xFFE5484D)
)

private val DarkColors = darkColorScheme(
    primary = PeriwinkleLight,
    onPrimary = Color(0xFF272063),
    primaryContainer = Color(0xFF463C8C),
    onPrimaryContainer = Color(0xFFE7E3FF),
    secondary = MintLight,
    onSecondary = Color(0xFF00382F),
    secondaryContainer = Color(0xFF1F5247),
    onSecondaryContainer = Color(0xFFCFF5EE),
    tertiary = BlossomLight,
    onTertiary = Color(0xFF5A1733),
    tertiaryContainer = Color(0xFF7A3350),
    onTertiaryContainer = Color(0xFFFFE0E9),
    background = Color(0xFF131221),
    onBackground = Color(0xFFE9E7F7),
    surface = Color(0xFF1C1B2E),
    onSurface = Color(0xFFE9E7F7),
    surfaceVariant = Color(0xFF2C2A40),
    onSurfaceVariant = Color(0xFFB7B2CC),
    outline = Color(0xFF433F58),
    error = Color(0xFFFF6B81)
)

@Composable
fun EmberTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}

/** Signature gradient: periwinkle → pink. Used on brand surfaces and chat bubbles. */
@Composable
fun brandGradient(): Brush = Brush.horizontalGradient(
    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
)

/** Subtle page background gradient. */
@Composable
fun pageGradient(): Brush = Brush.verticalGradient(
    listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surfaceVariant)
)

/** Soft three-stop aurora gradient used behind the onboarding flow. */
@Composable
fun auroraGradient(): Brush = Brush.linearGradient(
    listOf(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.background,
        MaterialTheme.colorScheme.tertiaryContainer
    )
)
