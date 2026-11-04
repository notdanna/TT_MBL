package com.descortezadores.ecoverde.presentation.features.analysis.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.descortezadores.ecoverde.domain.model.ChromaticAnalysisResult
import com.descortezadores.ecoverde.domain.model.ClassificationOutcome
import com.descortezadores.ecoverde.domain.model.ClassificationResult
import com.descortezadores.ecoverde.domain.model.DiagnosticCategory
import com.descortezadores.ecoverde.domain.model.SampleType
import com.descortezadores.ecoverde.domain.model.SyncStatus
import com.descortezadores.ecoverde.presentation.features.analysis.AnalysisResultTab
import com.descortezadores.ecoverde.presentation.features.analysis.AnalysisResultUiState
import com.descortezadores.ecoverde.presentation.theme.ChromaticBarkDamaged
import com.descortezadores.ecoverde.presentation.theme.ChromaticBarkHealthy
import com.descortezadores.ecoverde.presentation.theme.ChromaticBarkRegular
import com.descortezadores.ecoverde.presentation.theme.ChromaticBarkUncertain
import com.descortezadores.ecoverde.presentation.theme.ChromaticFoliageDamaged
import com.descortezadores.ecoverde.presentation.theme.ChromaticFoliageHealthy
import com.descortezadores.ecoverde.presentation.theme.ChromaticFoliageRegular
import com.descortezadores.ecoverde.presentation.theme.ChromaticFoliageUncertain
import com.descortezadores.ecoverde.presentation.theme.EcoVerdeMotion
import com.descortezadores.ecoverde.presentation.theme.GreenPrimary
import com.descortezadores.ecoverde.presentation.theme.StatusHealthyGreen
import com.descortezadores.ecoverde.presentation.theme.StatusInfestedRed
import com.descortezadores.ecoverde.presentation.theme.StatusUncertainAmber

/**
 * Módulo 6: Pantalla de Resultados y Divulgación Progresiva (Bifurcación por Tipo de Captura).
 *
 * Se ramifica según la selección realizada en el asistente:
 * - Caso A: Resultado de Corteza (EfficientNet-B3 + Análisis HSV en 2 niveles con pestañas).
 * - Caso B: Resultado de Follaje (Macro-análisis de Dosel / Dron, nivel único directo).
 */
@Composable
fun BifurcatedResultScreen(
    uiState: AnalysisResultUiState,
    onSelectTab: (AnalysisResultTab) -> Unit,
    onCategoryFilterSelected: (DiagnosticCategory?) -> Unit,
    onAlphaChanged: (Float) -> Unit,
    onObservationsChanged: (String) -> Unit,
    onSaveAndFinishClick: () -> Unit,
    onRepeatCaptureClick: () -> Unit
) {
    if (uiState.isProcessingVision) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    color = GreenPrimary,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Cargando análisis...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Recuperando fotografía y evaluación correspondiente",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    if (uiState.sampleType == SampleType.CORTEZA) {
        BarkResultSection(
            uiState = uiState,
            onSelectTab = onSelectTab,
            onCategoryFilterSelected = onCategoryFilterSelected,
            onAlphaChanged = onAlphaChanged,
            onObservationsChanged = onObservationsChanged,
            onSaveAndFinishClick = onSaveAndFinishClick,
            onRepeatCaptureClick = onRepeatCaptureClick
        )
    } else {
        FoliageResultSection(
            uiState = uiState,
            onCategoryFilterSelected = onCategoryFilterSelected,
            onAlphaChanged = onAlphaChanged,
            onObservationsChanged = onObservationsChanged,
            onSaveAndFinishClick = onSaveAndFinishClick,
            onRepeatCaptureClick = onRepeatCaptureClick
        )
    }
}

/**
 * Caso A: Resultado de Corteza (EfficientNet-B3 + Análisis HSV).
 * Estructura de dos niveles con divulgación progresiva mediante pestañas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BarkResultSection(
    uiState: AnalysisResultUiState,
    onSelectTab: (AnalysisResultTab) -> Unit,
    onCategoryFilterSelected: (DiagnosticCategory?) -> Unit,
    onAlphaChanged: (Float) -> Unit,
    onObservationsChanged: (String) -> Unit,
    onSaveAndFinishClick: () -> Unit,
    onRepeatCaptureClick: () -> Unit
) {
    val clf = uiState.classificationResult
    val confidence = (clf?.confidencePercentage ?: 0f).toInt()
    val isConfident = confidence > 75
    val outcome = clf?.outcome ?: ClassificationOutcome.NO_DETERMINADO

    // Colores oficiales especificados en el documento:
    // Infestado: contenedor #FFEBEE, tipografía #C62828
    // Sano: contenedor #E8F5E9, tipografía #2E7D32
    // No determinado (o confianza <= 75%): contenedor #FFF8E1, tipografía #F57F17
    val (cardContainerColor, cardTextColor, cardTitleText) = when {
        !isConfident || outcome == ClassificationOutcome.NO_DETERMINADO -> Triple(
            Color(0xFFFFF8E1),
            Color(0xFFF57F17),
            "NO DETERMINADO"
        )
        outcome == ClassificationOutcome.INFESTADO -> Triple(
            Color(0xFFFFEBEE),
            Color(0xFFC62828),
            "INFESTADO"
        )
        else -> Triple(
            Color(0xFFE8F5E9),
            Color(0xFF2E7D32),
            "SANO"
        )
    }

    val treeName = uiState.treeProgressionList.firstOrNull { it.tree.idArbol == uiState.selectedTreeId }
        ?.tree?.nombreEspecifico ?: uiState.newTreeName.ifBlank { "Árbol en estudio" }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = EcoVerdeMotion.standardSpatialSpring()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // NIVEL 1: Diagnóstico Inmediato (Visible sin desplazamiento)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = cardContainerColor),
            border = BorderStroke(1.5.dp, cardTextColor.copy(alpha = 0.4f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Barra superior con insignia discreta de estado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = cardTextColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (clf?.isOfflineExecution == false) "NUBE" else "LOCAL",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = cardTextColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = "Diagnóstico fitosanitario",
                        style = MaterialTheme.typography.labelSmall,
                        color = cardTextColor.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Bold
                    )
                }

                // Diagnóstico principal grande
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = cardTitleText,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = cardTextColor
                    )
                    Text(
                        text = "$confidence% de certeza del diagnóstico",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = cardTextColor
                    )
                }

                HorizontalDivider(color = cardTextColor.copy(alpha = 0.25f))

                // Nombre del árbol y su ubicación
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Árbol: $treeName",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = cardTextColor
                    )
                    Text(
                        text = "Lat: ${String.format("%.4f", uiState.captureLatitude)} • Lon: ${String.format("%.4f", uiState.captureLongitude)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = cardTextColor.copy(alpha = 0.85f)
                    )
                }

                // Leyenda de confianza si es <= 75%
                if (!isConfident || outcome == ClassificationOutcome.NO_DETERMINADO) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = cardTextColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Confianza insuficiente (≤ 75%). Se sugiere repetir la captura con mejor iluminación o enfoque sobre la corteza.",
                                style = MaterialTheme.typography.bodySmall,
                                color = cardTextColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Selectores "Manejo de registro" y "Análisis cromático" encima de la imagen
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    AnalysisResultTab.entries.forEachIndexed { index, tab ->
                        val selected = uiState.selectedTab == tab
                        SegmentedButton(
                            selected = selected,
                            onClick = { onSelectTab(tab) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = AnalysisResultTab.entries.size
                            ),
                            label = {
                                Text(
                                    text = tab.title,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        )
                    }
                }

                // La imagen del resultado (en análisis cromático el análisis aparece sobre la misma imagen)
                val barkOrigBmp = uiState.originalBitmap
                val isPortrait = barkOrigBmp != null && barkOrigBmp.height > barkOrigBmp.width
                val barkImageHeight = if (isPortrait) 320.dp else 260.dp
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barkImageHeight)
                        .clip(MaterialTheme.shapes.medium)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (barkOrigBmp != null) {
                        // Fotografía original base
                        Image(
                            bitmap = barkOrigBmp.asImageBitmap(),
                            contentDescription = "Muestra analizada",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // En análisis cromático, la segmentación aparece sobre la misma primer imagen
                        if (uiState.selectedTab == AnalysisResultTab.ANALISIS_COLOR &&
                            uiState.segmentedOverlayBitmap != null &&
                            uiState.overlayOpacityAlpha > 0f
                        ) {
                            Image(
                                bitmap = uiState.segmentedOverlayBitmap.asImageBitmap(),
                                contentDescription = "Máscara de análisis cromático",
                                contentScale = ContentScale.Crop,
                                alpha = uiState.overlayOpacityAlpha,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        CircularProgressIndicator(
                            color = GreenPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Barra de opacidad debajo de la imagen que inicia en 0% (visible en análisis cromático)
                if (uiState.selectedTab == AnalysisResultTab.ANALISIS_COLOR) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Opacidad de máscara (${(uiState.overlayOpacityAlpha * 100).toInt()}%)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = cardTextColor
                            )
                            if (uiState.overlayOpacityAlpha == 0f) {
                                Text(
                                    text = "Desliza para ver daño",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = cardTextColor.copy(alpha = 0.8f)
                                )
                            }
                        }
                        Slider(
                            value = uiState.overlayOpacityAlpha,
                            onValueChange = onAlphaChanged,
                            valueRange = 0.0f..1.0f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Pestañas inferiores con divulgación progresiva direccional
        AnimatedContent(
            targetState = uiState.selectedTab,
            transitionSpec = {
                EcoVerdeMotion.sharedAxisX(
                    forward = targetState == AnalysisResultTab.ANALISIS_COLOR,
                    slideDistance = 80
                )
            },
            label = "BarkTabContentAnimation"
        ) { tab ->
            when (tab) {
                AnalysisResultTab.CLASIFICACION -> {
                    // Pestaña 1: Manejo de registro (mantiene el diseño)
                    BarkManagementAndRegistryTab(
                        uiState = uiState,
                        outcome = outcome,
                        onObservationsChanged = onObservationsChanged
                    )
                }

                AnalysisResultTab.ANALISIS_COLOR -> {
                    // Pestaña 2: Análisis cromático (mantiene la división cromática subcortical, lienzo eliminado)
                    BarkChromaticAnalysisTab(
                        uiState = uiState
                    )
                }
            }
        }

        // BARRA DE ACCIONES PERSISTENTE (Inferior)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.isInspectingHistoricalRecord) {
                    OutlinedButton(
                        onClick = onRepeatCaptureClick,
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("← Volver a lista", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onSaveAndFinishClick,
                        enabled = !uiState.isSavingOffline,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        if (uiState.isSavingOffline) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Guardando...")
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Guardar y finalizar",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onRepeatCaptureClick,
                        shape = CircleShape,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Repetir captura")
                    }
                }
            }
        }
    }
}

/**
 * Pestaña 1 de Corteza: Protocolo NOM-019-SEMARNAT-2017 y observaciones de campo.
 */
@Composable
private fun BarkManagementAndRegistryTab(
    uiState: AnalysisResultUiState,
    outcome: ClassificationOutcome,
    onObservationsChanged: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Protocolo de actuación fitosanitaria derivado de la NOM-019-SEMARNAT-2017
            Text(
                text = "Protocolo de Actuación (NOM-019-SEMARNAT-2017)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            val (protocolTitle, protocolText, protocolColor) = when (outcome) {
                ClassificationOutcome.INFESTADO -> Triple(
                    "Tratamiento Fitosanitario Requerido",
                    "• Delimitar radio de amortiguamiento de 100 m.\n• Derribo, troceo y descortezado manual/mecánico del fuste afectado antes de la emergencia de adultos.\n• Aplicación de aspersión biológica/química en corteza conforme a la NOM-019.\n• Notificación de foco activo a la brigada forestal de SEDEMA.",
                    StatusInfestedRed
                )
                ClassificationOutcome.SANO -> Triple(
                    "Mantenimiento Preventivo Forestal",
                    "• Mantener monitoreo trimestral preventivo.\n• Vigilar desprendimientos o microperforaciones en base y fuste medio.\n• Asegurar riego y nutrición para mantener defensas de resina activas.",
                    StatusHealthyGreen
                )
                ClassificationOutcome.NO_DETERMINADO -> Triple(
                    "Re-inspección Requerida",
                    "• No se detecta un patrón concluyente con certeza > 75%.\n• Realizar inspección ocular terrestre adicional y repetir toma a distancia de 20-30 cm perpendicular a la corteza.",
                    StatusUncertainAmber
                )
            }

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = protocolColor.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = protocolTitle,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = protocolColor
                    )
                    Text(
                        text = protocolText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Campo para ingresar o modificar observaciones de campo
            Text(
                text = "Observaciones de Campo",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = uiState.fieldObservations,
                onValueChange = onObservationsChanged,
                placeholder = { Text("Notas de inspección fitosanitaria (ej. exudado de resina, grumos de aserrín)...") },
                shape = MaterialTheme.shapes.medium,
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Pestaña 2 de Corteza: Distribución cromática subcortical.
 */
@Composable
private fun BarkChromaticAnalysisTab(
    uiState: AnalysisResultUiState
) {
    val chrom = uiState.chromaticResult
    val pctRegular = ((chrom?.pctRegular ?: 0.0) * 100.0).toInt()
    val pctDanado = ((chrom?.pctDanado ?: 0.0) * 100.0).toInt()
    val pctSano = ((chrom?.pctSano ?: 0.0) * 100.0).toInt()
    val pctIncierto = ((chrom?.pctIncierto ?: 0.0) * 100.0).toInt()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Distribución cromática subcortical",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Proporción de coloración detectada en la corteza:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Gráfica horizontal de distribución según rangos cromáticos:
            // 1. Blanco / Crema (Respuesta defensiva / exudado de resina)
            ChromaticHorizontalBar(
                title = "Exudado de resina / defensa (Blanco / Crema)",
                percentage = pctRegular,
                barColor = ChromaticBarkRegular,
                badgeTextColor = Color(0xFF5D4037)
            )

            // 2. Café-Anaranjado (Colonización activa subcortical)
            ChromaticHorizontalBar(
                title = "Signo de colonización activa (Café-Anaranjado)",
                percentage = pctDanado,
                barColor = ChromaticBarkDamaged,
                badgeTextColor = Color.White
            )

            // 3. Café-Grisáceo (Tejido base sin alteración)
            ChromaticHorizontalBar(
                title = "Tejido base sin alteración (Grisáceo)",
                percentage = pctSano,
                barColor = ChromaticBarkHealthy,
                badgeTextColor = Color.White
            )

            // Fondo / No determinado
            if (pctIncierto > 0) {
                ChromaticHorizontalBar(
                    title = "Área no determinada",
                    percentage = pctIncierto,
                    barColor = ChromaticBarkUncertain,
                    badgeTextColor = Color.White
                )
            }
        }
    }
}

/**
 * Caso B: Resultado de Follaje (Macro-análisis de Dosel / Dron).
 * Estructura directa de nivel único, sin pestañas ni clasificación binaria categórica.
 */
@Composable
private fun FoliageResultSection(
    uiState: AnalysisResultUiState,
    onCategoryFilterSelected: (DiagnosticCategory?) -> Unit,
    onAlphaChanged: (Float) -> Unit,
    onObservationsChanged: (String) -> Unit,
    onSaveAndFinishClick: () -> Unit,
    onRepeatCaptureClick: () -> Unit
) {
    val chrom = uiState.chromaticResult
    val pctSano = ((chrom?.pctSano ?: 0.0) * 100.0).toInt()
    val pctRegular = ((chrom?.pctRegular ?: 0.0) * 100.0).toInt()
    val pctDanado = ((chrom?.pctDanado ?: 0.0) * 100.0).toInt()
    val pctIncierto = ((chrom?.pctIncierto ?: 0.0) * 100.0).toInt()

    val zoneName = uiState.availableZones.firstOrNull { it.idZona == uiState.selectedZoneId }
        ?.nombreZona ?: uiState.newZoneName.ifBlank { "Zona II - Calzada de los Poetas" }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = EcoVerdeMotion.standardSpatialSpring()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ENCABEZADO TERRITORIAL CON IMAGEN, OVERLAY Y CONTROL DE OPACIDAD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = GreenPrimary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "LOCAL",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = GreenPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = "Monitoreo de dosel",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = zoneName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Coordenadas centrales: ${String.format("%.5f, %.5f", uiState.captureLatitude, uiState.captureLongitude)} • Radio: ${uiState.newZoneRadiusMeters} m",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Imagen debajo del título "Zona...", aprovechando el espacio según dimensiones
                val origBmp = uiState.originalBitmap
                if (origBmp != null) {
                    val isPortrait = origBmp.height > origBmp.width
                    val imageModifier = if (origBmp.height > 0 && origBmp.width > 0) {
                        val ratio = (origBmp.width.toFloat() / origBmp.height.toFloat()).coerceIn(0.65f, 1.6f)
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(ratio)
                            .clip(MaterialTheme.shapes.medium)
                            .background(Color.Black)
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .height(if (isPortrait) 380.dp else 260.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(Color.Black)
                    }

                    Box(
                        modifier = imageModifier,
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = origBmp.asImageBitmap(),
                            contentDescription = "Captura de follaje",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )

                        // El análisis aparece sobre la misma imagen
                        if (uiState.segmentedOverlayBitmap != null && uiState.overlayOpacityAlpha > 0f) {
                            Image(
                                bitmap = uiState.segmentedOverlayBitmap.asImageBitmap(),
                                contentDescription = "Máscara de análisis cromático foliar",
                                contentScale = ContentScale.Fit,
                                alpha = uiState.overlayOpacityAlpha,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        if (uiState.isRenderingOverlay) {
                            CircularProgressIndicator(
                                color = GreenPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    // Debajo la barra de opacidad que inicie en 0%
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Opacidad de análisis cromático (${(uiState.overlayOpacityAlpha * 100).toInt()}%)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (uiState.overlayOpacityAlpha == 0f) {
                                Text(
                                    text = "Desliza para ver análisis",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                        Slider(
                            value = uiState.overlayOpacityAlpha,
                            onValueChange = onAlphaChanged,
                            valueRange = 0.0f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = GreenPrimary,
                                activeTrackColor = GreenPrimary,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // BLOQUE PRINCIPAL: Distribución de Coloración Foliar (sin la sección de imagen coloreada repetida)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Distribución de coloración foliar",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Proporción de coloración detectada en la copa:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 1. Verde (Follaje sano)
                ChromaticHorizontalBar(
                    title = "Follaje sano (Verde)",
                    percentage = pctSano,
                    barColor = ChromaticFoliageHealthy,
                    badgeTextColor = Color.White
                )

                // 2. Verde-Amarillento / Limón (Estrés hídrico o colonización inicial)
                ChromaticHorizontalBar(
                    title = "Estrés hídrico o colonización inicial (Amarillento)",
                    percentage = pctRegular,
                    barColor = ChromaticFoliageRegular,
                    badgeTextColor = Color(0xFF2E7D32)
                )

                // 3. Café-Rojizo (Necrosis foliar / muerte de copa)
                ChromaticHorizontalBar(
                    title = "Necrosis foliar / desecación (Café-Rojizo)",
                    percentage = pctDanado,
                    barColor = ChromaticFoliageDamaged,
                    badgeTextColor = Color.White
                )

                // 4. Incierto / Fondo (Ruido o fondo descartado)
                if (pctIncierto > 0) {
                    ChromaticHorizontalBar(
                        title = "Área no determinada / fondo",
                        percentage = pctIncierto,
                        barColor = ChromaticFoliageUncertain,
                        badgeTextColor = Color.White
                    )
                }
            }
        }

        // Observaciones adicionales
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Observaciones de la Zona de Follaje",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = uiState.fieldObservations,
                    onValueChange = onObservationsChanged,
                    placeholder = { Text("Notas de sobrevuelo, copa o inspección...") },
                    shape = MaterialTheme.shapes.medium,
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // BARRA DE ACCIONES PERSISTENTE (Inferior)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.isInspectingHistoricalRecord) {
                    OutlinedButton(
                        onClick = onRepeatCaptureClick,
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("← Volver a lista", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onSaveAndFinishClick,
                        enabled = !uiState.isSavingOffline,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        if (uiState.isSavingOffline) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Guardando zona...")
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Guardar zona y finalizar",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onRepeatCaptureClick,
                        shape = CircleShape,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Repetir toma aérea")
                    }
                }
            }
        }
    }
}

/**
 * Barra horizontal con porcentaje según rangos cromáticos.
 */
@Composable
private fun ChromaticHorizontalBar(
    title: String,
    percentage: Int,
    barColor: Color,
    badgeTextColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            Surface(
                shape = MaterialTheme.shapes.small,
                color = barColor
            ) {
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = badgeTextColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        LinearProgressIndicator(
            progress = { (percentage / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = barColor,
            trackColor = barColor.copy(alpha = 0.16f),
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}
