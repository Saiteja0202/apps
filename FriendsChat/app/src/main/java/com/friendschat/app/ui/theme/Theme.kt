package com.friendschat.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/*
 * "Paperback" — a warm, editorial theme for GENZ. Think a well-loved paperback:
 * a cream/sand canvas, espresso ink, and a terracotta → mustard accent family with
 * a calm sage support colour. Big serif display type pairs with a clean sans body
 * for a friendly, magazine-like feel that doesn't imitate any mainstream app.
 */

// Brand accents (shared). Names kept for backwards-compatibility with callers.
val Periwinkle = Color(0xFFC2603F)        // terracotta — primary ink-accent
val PeriwinkleLight = Color(0xFFE5896A)   // warm clay (dark mode primary)
val Mint = Color(0xFF6F8061)              // sage — secondary
val MintLight = Color(0xFFA9BC97)
val Blossom = Color(0xFFCC9A3D)           // mustard / gold — tertiary highlight
val BlossomLight = Color(0xFFE6C46E)
val SeenBlue = Color(0xFF3F9E92)          // muted teal "seen" tick (warm-friendly)

private val LightColors = lightColorScheme(
    primary = Periwinkle,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF6DCCF),
    onPrimaryContainer = Color(0xFF4A1E0C),
    secondary = Mint,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE6CF),
    onSecondaryContainer = Color(0xFF2A331E),
    tertiary = Blossom,
    onTertiary = Color(0xFF3B2D08),
    tertiaryContainer = Color(0xFFF4E4BE),
    onTertiaryContainer = Color(0xFF4A3A12),
    background = Color(0xFFFAF3E8),
    onBackground = Color(0xFF2C2620),
    surface = Color(0xFFFFFCF5),
    onSurface = Color(0xFF2C2620),
    surfaceVariant = Color(0xFFEFE5D4),
    onSurfaceVariant = Color(0xFF6B5F50),
    outline = Color(0xFFD8C9B2),
    error = Color(0xFFB3382C)
)

private val DarkColors = darkColorScheme(
    primary = PeriwinkleLight,
    onPrimary = Color(0xFF4A1E0C),
    primaryContainer = Color(0xFF6E3320),
    onPrimaryContainer = Color(0xFFF6DCCF),
    secondary = MintLight,
    onSecondary = Color(0xFF2A331E),
    secondaryContainer = Color(0xFF44523A),
    onSecondaryContainer = Color(0xFFDCE6CF),
    tertiary = BlossomLight,
    onTertiary = Color(0xFF3B2D08),
    tertiaryContainer = Color(0xFF5E4A1E),
    onTertiaryContainer = Color(0xFFF4E4BE),
    background = Color(0xFF1A1610),
    onBackground = Color(0xFFEFE6D6),
    surface = Color(0xFF241F18),
    onSurface = Color(0xFFEFE6D6),
    surfaceVariant = Color(0xFF342D24),
    onSurfaceVariant = Color(0xFFC3B5A1),
    outline = Color(0xFF4A4135),
    error = Color(0xFFFF8A7A)
)

/* ---- Editorial typography: a serif display voice over a clean sans body. ---- */
private val Serif = FontFamily.Serif
private val Base = Typography()

private val EditorialTypography = Base.copy(
    displayLarge = Base.displayLarge.copy(fontFamily = Serif, fontWeight = FontWeight.Bold),
    displayMedium = Base.displayMedium.copy(fontFamily = Serif, fontWeight = FontWeight.Bold),
    displaySmall = Base.displaySmall.copy(fontFamily = Serif, fontWeight = FontWeight.Bold),
    headlineLarge = Base.headlineLarge.copy(fontFamily = Serif, fontWeight = FontWeight.Bold),
    headlineMedium = Base.headlineMedium.copy(fontFamily = Serif, fontWeight = FontWeight.SemiBold),
    headlineSmall = Base.headlineSmall.copy(fontFamily = Serif, fontWeight = FontWeight.SemiBold),
    titleLarge = Base.titleLarge.copy(fontFamily = Serif, fontWeight = FontWeight.Bold)
)

@Composable
fun EmberTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = EditorialTypography,
        content = content
    )
}

/** Signature gradient: terracotta → mustard. Used on brand surfaces and chat bubbles. */
@Composable
fun brandGradient(): Brush = Brush.horizontalGradient(
    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
)

/** Subtle page background gradient. */
@Composable
fun pageGradient(): Brush = Brush.verticalGradient(
    listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surfaceVariant)
)

/** Soft three-stop warm gradient used behind the onboarding flow. */
@Composable
fun auroraGradient(): Brush = Brush.linearGradient(
    listOf(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.background,
        MaterialTheme.colorScheme.tertiaryContainer
    )
)
