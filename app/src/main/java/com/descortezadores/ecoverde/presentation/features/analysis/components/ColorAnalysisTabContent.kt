package com.descortezadores.ecoverde.presentation.features.analysis.components

import android.graphics.Bitmap
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.descortezadores.ecoverde.domain.model.BiologicalSeverityLevel
import com.descortezadores.ecoverde.domain.model.ChromaticAnalysisResult
import com.descortezadores.ecoverde.domain.model.DiagnosticCategory
import com.descortezadores.ecoverde.domain.model.SampleType
import com.descortezadores.ecoverde.presentation.theme.EcoVerdeMotion

/**
 * Pestaña 2 `[Análisis de color]` (Figuras 48 y 49 del Trabajo Terminal +
 * **Nueva Imagen Coloreada por Secciones** definida en el Plan de Implementación Integral).
 *
 * Refactorizada bajo lineamientos de Material 3 y Material 3 Expressive:
 * - Superficies tonales (`surfaceContainer`, `surfaceContainerLow`) sin sombras artificiales (`0.dp`).
 * - Tokens de forma estándar M3 (`MaterialTheme.shapes.large` = `16.dp`, `shapes.medium` = `12.dp`).
 * - Animación de contenedor guiada por físicas de resorte (`EcoVerdeMotion.standardSpatialSpring()`).
 * - Soporte de elemento compartido (`canvasSharedModifier`) hacia el lienzo `SegmentedImageCanvas`.
 */
@Composable
fun ColorAnalysisTabContent(
    sampleType: SampleType,
    originalBitmap: Bitmap?,
    segmentedOverlayBitmap: Bitmap?,
    chromaticResult: ChromaticAnalysisResult?,
    selectedCategoryFilter: DiagnosticCategory?,
    overlayOpacityAlpha: Float,
    showOriginalComparison: Boolean,
    isRenderingOverlay: Boolean,
    onCategoryFilterSelected: (DiagnosticCategory?) -> Unit,
    onAlphaChanged: (Float) -> Unit,
    onToggleOriginalComparison: () -> Unit,
    modifier: Modifier = Modifier,
    canvasSharedModifier: Modifier = Modifier
) {
    val bands = chromaticResult?.bands.orEmpty()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = EcoVerdeMotion.standardSpatialSpring()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Nueva Imagen Coloreada por Secciones con Zoom/Pan, Haptics y Opacidad
        SegmentedImageCanvas(
            originalBitmap = originalBitmap,
            segmentedOverlayBitmap = segmentedOverlayBitmap,
            bands = bands,
            selectedCategory = selectedCategoryFilter,
            overlayAlpha = overlayOpacityAlpha,
            showOriginalComparison = showOriginalComparison,
            isRenderingOverlay = isRenderingOverlay,
            onAlphaChanged = onAlphaChanged,
            onToggleOriginalComparison = onToggleOriginalComparison,
            modifier = canvasSharedModifier
        )

        // 2. Chips interactivos M3 para aislar franjas cromáticas en vivo
        if (bands.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    ColorCategoryFilterChips(
                        bands = bands,
                        selectedCategory = selectedCategoryFilter,
                        onCategorySelected = onCategoryFilterSelected
                    )
                }
            }
        }

        // 3. Resumen de Severidad Biológica y Barras Lineales M3 (Figuras 48 y 49)
        if (chromaticResult != null) {
            val severityColor = when (chromaticResult.severityLevel) {
                BiologicalSeverityLevel.ALTA -> parseHexColor("#C62828")
                BiologicalSeverityLevel.MODERADA -> parseHexColor("#F9A825")
                BiologicalSeverityLevel.BAJA -> parseHexColor("#2E7D32")
                BiologicalSeverityLevel.NO_CONCLUYENTE -> parseHexColor("#757575")
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (sampleType == SampleType.CORTEZA) {
                                    "Distribución cromática en corteza"
                                } else {
                                    "Distribución cromática en follaje"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Toca cualquier barra para aislar esa sección en la imagen superior",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = severityColor.copy(alpha = 0.14f)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = chromaticResult.severityLevel.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = severityColor
                                )
                                Text(
                                    text = String.format("Índice S = %.2f", chromaticResult.biologicalSeverityScore),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = severityColor
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Las 4 barras lineales de progreso (Sano, Regular, Dañado, Incierto) con StrokeCap.Round y spring
                    bands.forEach { band ->
                        val isSelected = selectedCategoryFilter == band.category
                        ChromaticMetricBar(
                            band = band,
                            isSelectedFilter = isSelected,
                            onClick = {
                                onCategoryFilterSelected(
                                    if (isSelected) null else band.category
                                )
                            }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total analizado: ${chromaticResult.totalPixelsAnalyzed} px (300×300 HSV)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Dominante: ${chromaticResult.dominantCategory.displayName}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
