package com.descortezadores.ecoverde.presentation.features.analysis.components

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.descortezadores.ecoverde.domain.model.ColorBandResult
import com.descortezadores.ecoverde.domain.model.DiagnosticCategory
import com.descortezadores.ecoverde.presentation.theme.EcoVerdeMotion

/**
 * Visor interactivo de la **Nueva Imagen Coloreada por Secciones** (Fase 5 del Plan de Implementación).
 * Refactorizado bajo estándares Material 3 / Material 3 Expressive:
 * - Elevación tonal `surfaceContainer` con radio `16.dp` (`MaterialTheme.shapes.large`)
 * - Zoom y paneo gobernados por físicas de resorte (`EcoVerdeMotion.interactiveSpring()`)
 * - Retroalimentación háptica (`LocalHapticFeedback`) al alcanzar límites de zoom (`1.0x` / `4.0x`) o doble toque
 * - Transición `Crossfade` entre fotografía original e imagen segmentada HSV.
 */
@Composable
fun SegmentedImageCanvas(
    originalBitmap: Bitmap?,
    segmentedOverlayBitmap: Bitmap?,
    bands: List<ColorBandResult>,
    selectedCategory: DiagnosticCategory?,
    overlayAlpha: Float,
    showOriginalComparison: Boolean,
    isRenderingOverlay: Boolean,
    onAlphaChanged: (Float) -> Unit,
    onToggleOriginalComparison: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var targetScale by remember { mutableFloatStateOf(1f) }
    var targetOffset by remember { mutableStateOf(Offset.Zero) }
    var hitZoomBoundary by remember { mutableStateOf(false) }

    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = EcoVerdeMotion.interactiveSpring(),
        label = "canvas_zoom_spring"
    )
    val animatedOffset by animateOffsetAsState(
        targetValue = targetOffset,
        animationSpec = EcoVerdeMotion.interactiveSpring(),
        label = "canvas_pan_spring"
    )

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        val rawScale = targetScale * zoomChange
        val clampedScale = rawScale.coerceIn(1f, 4f)
        if ((rawScale < 1f || rawScale > 4f) && !hitZoomBoundary) {
            hitZoomBoundary = true
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } else if (rawScale in 1.02f..3.98f) {
            hitZoomBoundary = false
        }
        targetScale = clampedScale
        targetOffset = if (clampedScale > 1f) {
            Offset(
                x = (targetOffset.x + panChange.x).coerceIn(-450f, 450f),
                y = (targetOffset.y + panChange.y).coerceIn(-450f, 450f)
            )
        } else {
            Offset.Zero
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (showOriginalComparison) {
                            "Fotografía Original (Sin máscara)"
                        } else if (selectedCategory != null) {
                            "Mapa Cromático HSV: Sección ${selectedCategory.displayName}"
                        } else {
                            "Imagen Coloreada por Secciones (OpenCV HSV)"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Pellizca para hacer zoom (${String.format("%.1fx", animatedScale)}) • Doble toque para alternar",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (targetScale > 1.05f) {
                        AssistChip(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                targetScale = 1f
                                targetOffset = Offset.Zero
                            },
                            shape = MaterialTheme.shapes.medium,
                            label = { Text("1x", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    AssistChip(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleOriginalComparison()
                        },
                        shape = MaterialTheme.shapes.medium,
                        label = {
                            Text(
                                text = if (showOriginalComparison) "Ver Coloreada" else "Ver Original",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(270.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(Color(0xFF141A15))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (targetScale > 1.05f) {
                                    targetScale = 1f
                                    targetOffset = Offset.Zero
                                } else {
                                    targetScale = 2.2f
                                }
                            }
                        )
                    }
                    .transformable(state = transformableState),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(
                    targetState = showOriginalComparison to (selectedCategory?.name ?: "ALL"),
                    animationSpec = EcoVerdeMotion.standardSpatialSpring(),
                    label = "segmented_canvas_crossfade"
                ) { (isOrig, _) ->
                    val displayBitmap = if (isOrig) {
                        originalBitmap ?: segmentedOverlayBitmap
                    } else {
                        segmentedOverlayBitmap ?: originalBitmap
                    }

                    if (displayBitmap != null) {
                        Image(
                            bitmap = displayBitmap.asImageBitmap(),
                            contentDescription = "Imagen segmentada cromáticamente por secciones",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = animatedScale,
                                    scaleY = animatedScale,
                                    translationX = animatedOffset.x,
                                    translationY = animatedOffset.y
                                ),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Selecciona o captura una imagen para visualizar la segmentación por color",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.75f),
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    shape = MaterialTheme.shapes.small,
                    color = Color.Black.copy(alpha = 0.65f)
                ) {
                    Text(
                        text = when {
                            showOriginalComparison -> "MODO ORIGINAL"
                            selectedCategory != null -> "AISLADO: ${selectedCategory.displayName.uppercase()}"
                            else -> "4 SECCIONES + CONTORNOS"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (isRenderingOverlay) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp),
                        shape = MaterialTheme.shapes.small,
                        color = Color.Black.copy(alpha = 0.65f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Text(
                                text = "Actualizando máscara...",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Intensidad de superposición cromática (Alpha Blending)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${(overlayAlpha * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = overlayAlpha,
                    onValueChange = onAlphaChanged,
                    valueRange = 0.0f..1.0f,
                    enabled = !showOriginalComparison
                )
            }

            if (bands.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    bands.forEach { band ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(parseHexColor(band.colorHex))
                            )
                            Text(
                                text = "${band.category.displayName} ${band.percentage.toInt()}%",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}
