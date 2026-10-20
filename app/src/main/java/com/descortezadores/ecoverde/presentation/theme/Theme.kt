package com.descortezadores.ecoverde.presentation.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

/**
 * Tokens de forma estándar de Material 3 (esquinas continuas de 12dp, 16dp y 28dp según jerarquía).
 */
val EcoVerdeShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/**
 * Especificaciones de movimiento basadas en físicas de resorte (Material 3 & Material 3 Expressive).
 */
object EcoVerdeMotion {
    fun <T> expressiveSpatialSpring() = spring<T>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    fun <T> standardSpatialSpring() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    fun <T> interactiveSpring() = spring<T>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMedium
    )
}

private val LightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = Color.White,
    primaryContainer = GreenContainer,
    onPrimaryContainer = OnGreenContainer,
    secondary = ForestSecondary,
    onSecondary = Color.White,
    secondaryContainer = ForestSecondaryContainer,
    onSecondaryContainer = OnForestSecondaryContainer,
    tertiary = BarkTertiary,
    tertiaryContainer = BarkTertiaryContainer,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceContainerLowest = SurfaceLight,
    surfaceContainerLow = lerp(SurfaceLight, GreenContainer, 0.16f),
    surfaceContainer = lerp(SurfaceLight, GreenContainer, 0.28f),
    surfaceContainerHigh = lerp(SurfaceLight, GreenContainer, 0.42f),
    surfaceContainerHighest = lerp(SurfaceLight, GreenContainer, 0.56f),
    outline = lerp(ForestSecondary, OnGreenContainer, 0.25f),
    outlineVariant = lerp(SurfaceLight, ForestSecondaryContainer, 0.72f),
    error = StatusInfestedRed
)

private val DarkColorScheme = darkColorScheme(
    primary = GreenPrimaryLight,
    onPrimary = OnGreenContainer,
    primaryContainer = GreenPrimaryDark,
    onPrimaryContainer = GreenContainer,
    secondary = ForestSecondaryContainer,
    onSecondary = OnForestSecondaryContainer,
    tertiary = BarkTertiaryContainer,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceContainerLowest = BackgroundDark,
    surfaceContainerLow = lerp(BackgroundDark, SurfaceDark, 0.65f),
    surfaceContainer = SurfaceDark,
    surfaceContainerHigh = lerp(SurfaceDark, GreenPrimaryDark, 0.28f),
    surfaceContainerHighest = lerp(SurfaceDark, GreenPrimaryDark, 0.45f),
    outline = lerp(SurfaceDark, ForestSecondaryContainer, 0.48f),
    outlineVariant = lerp(SurfaceDark, ForestSecondaryContainer, 0.24f),
    error = Color(0xFFEF5350)
)

@Composable
fun EcoVerdeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = EcoVerdeShapes,
        content = content
    )
}
