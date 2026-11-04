package com.descortezadores.ecoverde.presentation.theme

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
 * Sistema de movimiento estándar de Material Design 3 (MD3 Expressive & Android Motion Guidelines).
 *
 * Proporciona:
 * - Curvas canónicas de aceleración y desaceleración enfatizadas.
 * - Físicas de resorte espacial y de interacción táctil.
 * - Transición direccional en eje horizontal (Shared Axis X) para pasos de Wizard, pestañas y modos.
 * - Transición de desvanecimiento continuo (Fade Through) para destinos principales.
 * - Transición Maestro-Detalle (Master-Detail Transform) para navegación jerárquica lista <-> ficha.
 */
object EcoVerdeMotion {
    // Curvas canónicas de Material 3
    val EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val EmphasizedDecelerateEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EmphasizedAccelerateEasing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
    val StandardEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val StandardDecelerateEasing = CubicBezierEasing(0.0f, 0.0f, 0.0f, 1.0f)
    val LinearOutSlowInEasing = androidx.compose.animation.core.LinearOutSlowInEasing
    val FastOutLinearInEasing = androidx.compose.animation.core.FastOutLinearInEasing

    // Tiempos canónicos de duración (ms) conforme a tokens MD3
    const val DurationQuickExit = 150
    const val DurationStandardEnter = 280
    const val DurationEmphasized = 400

    // Físicas de resorte
    fun <T> expressiveSpatialSpring() = spring<T>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    fun <T> standardSpatialSpring() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    fun <T> interactiveSpring() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /**
     * Transición Shared Axis X (Eje horizontal direccional).
     * Si `forward` es true: el nuevo contenido entra desde la derecha (+X) y el saliente sale hacia la izquierda (-X).
     * Si `forward` es false: el nuevo contenido entra desde la izquierda (-X) y el saliente sale hacia la derecha (+X).
     */
    fun sharedAxisX(
        forward: Boolean,
        slideDistance: Int = 120
    ): ContentTransform {
        val sign = if (forward) 1 else -1
        val enterTransition: EnterTransition = fadeIn(
            animationSpec = tween(durationMillis = DurationStandardEnter, easing = EmphasizedDecelerateEasing)
        ) + slideInHorizontally(
            animationSpec = tween(durationMillis = DurationStandardEnter, easing = EmphasizedDecelerateEasing),
            initialOffsetX = { fullWidth -> (sign * (slideDistance.coerceAtMost(fullWidth / 3))) }
        )

        val exitTransition: ExitTransition = fadeOut(
            animationSpec = tween(durationMillis = DurationQuickExit, easing = EmphasizedAccelerateEasing)
        ) + slideOutHorizontally(
            animationSpec = tween(durationMillis = DurationQuickExit, easing = EmphasizedAccelerateEasing),
            targetOffsetX = { fullWidth -> (-sign * (slideDistance.coerceAtMost(fullWidth / 4))) }
        )

        return enterTransition togetherWith exitTransition
    }

    /**
     * Transición Fade Through canónica de Material 3 para destinos principales.
     * El destino saliente se desvanece de manera ágil (150ms), mientras que el destino entrante
     * se desvanece con una expansión sutil (0.95f -> 1.0f) en 280ms.
     */
    fun fadeThrough(): ContentTransform {
        val enterTransition: EnterTransition = fadeIn(
            animationSpec = tween(durationMillis = DurationStandardEnter, easing = EmphasizedDecelerateEasing)
        ) + scaleIn(
            initialScale = 0.95f,
            animationSpec = tween(durationMillis = DurationStandardEnter, easing = EmphasizedDecelerateEasing)
        )

        val exitTransition: ExitTransition = fadeOut(
            animationSpec = tween(durationMillis = DurationQuickExit, easing = EmphasizedAccelerateEasing)
        )

        return enterTransition togetherWith exitTransition
    }

    /**
     * Transición Maestro-Detalle para abrir o cerrar fichas de inspección técnica.
     */
    fun masterDetailTransform(isOpeningDetail: Boolean): ContentTransform {
        return sharedAxisX(forward = isOpeningDetail, slideDistance = 140)
    }

    /**
     * Transición ultra-fluida optimizada para 120Hz para alternar destinos en la barra inferior (Bottom Navigation).
     * El destino saliente se retira de inmediato (75ms) y el nuevo entra limpio (140ms).
     * Elimina el efecto fantasma donde se veían fragmentos de la pantalla anterior superpuestos.
     */
    fun navDestinationTransform(): ContentTransform {
        val enterTransition: EnterTransition = fadeIn(
            animationSpec = tween(durationMillis = 140, easing = LinearOutSlowInEasing)
        )
        val exitTransition: ExitTransition = fadeOut(
            animationSpec = tween(durationMillis = 75, easing = FastOutLinearInEasing)
        )
        return enterTransition togetherWith exitTransition
    }

    /**
     * Transición limpia para pestañas internas (Árboles vs Zonas, Clasificación vs Color).
     */
    fun cleanCrossfade(): ContentTransform {
        return fadeIn(animationSpec = tween(durationMillis = 140, easing = LinearOutSlowInEasing))
            .togetherWith(fadeOut(animationSpec = tween(durationMillis = 75, easing = FastOutLinearInEasing)))
    }

    /**
     * Transición para apertura de diálogos o modales a pantalla completa (Asistente de captura).
     */
    fun modalOpenTransform(): ContentTransform {
        return (fadeIn(animationSpec = tween(durationMillis = 180, easing = LinearOutSlowInEasing)) +
            scaleIn(initialScale = 0.96f, animationSpec = tween(durationMillis = 180, easing = LinearOutSlowInEasing)))
            .togetherWith(fadeOut(animationSpec = tween(durationMillis = 75, easing = FastOutLinearInEasing)))
    }

    fun modalCloseTransform(): ContentTransform {
        return fadeIn(animationSpec = tween(durationMillis = 160, easing = LinearOutSlowInEasing))
            .togetherWith(
                fadeOut(animationSpec = tween(durationMillis = 75, easing = FastOutLinearInEasing)) +
                    scaleOut(targetScale = 0.96f, animationSpec = tween(durationMillis = 75, easing = FastOutLinearInEasing))
            )
    }

    /**
     * Transición para abrir y cerrar vistas de detalle técnico.
     */
    fun detailOpenTransform(): ContentTransform {
        return (fadeIn(animationSpec = tween(durationMillis = 180, easing = LinearOutSlowInEasing)) +
            scaleIn(initialScale = 0.95f, animationSpec = tween(durationMillis = 180, easing = LinearOutSlowInEasing)))
            .togetherWith(fadeOut(animationSpec = tween(durationMillis = 75, easing = FastOutLinearInEasing)))
    }

    fun detailCloseTransform(): ContentTransform {
        return fadeIn(animationSpec = tween(durationMillis = 160, easing = LinearOutSlowInEasing))
            .togetherWith(
                fadeOut(animationSpec = tween(durationMillis = 75, easing = FastOutLinearInEasing)) +
                    scaleOut(targetScale = 0.95f, animationSpec = tween(durationMillis = 75, easing = FastOutLinearInEasing))
            )
    }
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
    background = BackgroundLight, // Blanco puro (#FFFFFF)
    onBackground = Color(0xFF191C19),
    surface = SurfaceLight, // Blanco puro (#FFFFFF)
    onSurface = Color(0xFF191C19),
    onSurfaceVariant = Color(0xFF424940),
    surfaceContainerLowest = SurfaceLight,
    surfaceContainerLow = Color(0xFFFFFFFF), // Elementos interiores (blancos para contrastar sobre card gris)
    surfaceContainer = Color(0xFFF1F3F5), // Gris claro visible y limpio para las cards que recubren cada sección
    surfaceContainerHigh = Color(0xFFE9ECEF),
    surfaceContainerHighest = Color(0xFFDEE2E6),
    outline = Color(0xFF8C9398),
    outlineVariant = Color(0xFFE0E3E7), // Borde sutil y elegante para delimitar las cards grises
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
