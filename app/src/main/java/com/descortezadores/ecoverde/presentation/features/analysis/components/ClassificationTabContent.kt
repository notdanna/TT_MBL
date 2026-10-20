package com.descortezadores.ecoverde.presentation.features.analysis.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.descortezadores.ecoverde.domain.model.ChromaticAnalysisResult
import com.descortezadores.ecoverde.domain.model.ClassificationOutcome
import com.descortezadores.ecoverde.domain.model.ClassificationResult
import com.descortezadores.ecoverde.domain.model.FoliageZoneRecord
import com.descortezadores.ecoverde.domain.model.SampleType
import com.descortezadores.ecoverde.domain.model.TreeRecord
import com.descortezadores.ecoverde.presentation.features.analysis.TargetAssociationMode
import com.descortezadores.ecoverde.presentation.features.analysis.TreeProgressionSummary
import com.descortezadores.ecoverde.presentation.theme.EcoVerdeMotion

/**
 * Pestaña 1 `[Clasificación]` (Figuras 46 y 47 del Trabajo Terminal).
 *
 * Refactorizada bajo lineamientos de Material 3 y Material 3 Expressive:
 * - Superficies tonales (`surfaceContainer`, `surfaceContainerLow`, `surfaceContainerHigh`) con `0.dp` de elevación artificial.
 * - `SingleChoiceSegmentedButtonRow` + `SegmentedButton` para selección mutuamente excluyente de especies y modo de asociación.
 * - Subsistema `ripple(bounded = true)` de Material 3 y `LocalHapticFeedback` al seleccionar árboles, alternar especies y confirmar capturas.
 * - Transición de elemento compartido (`photoSharedModifier`) hacia el lienzo de inspección cromática.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassificationTabContent(
    sampleType: SampleType,
    originalBitmap: Bitmap?,
    classificationResult: ClassificationResult?,
    chromaticResult: ChromaticAnalysisResult?,
    processingElapsedMs: Long,
    captureLatitude: Double,
    captureLongitude: Double,
    captureLocationSourceLabel: String = "Ubicación GPS del dispositivo",
    captureHasExifGps: Boolean = false,
    captureExifDateTimeIso: String? = null,
    captureExifCameraModel: String? = null,
    associationMode: TargetAssociationMode,
    availableTrees: List<TreeRecord>,
    treeProgressionList: List<TreeProgressionSummary>,
    selectedTreeId: String?,
    newTreeName: String,
    newTreeSpecies: String,
    availableZones: List<FoliageZoneRecord>,
    selectedZoneId: String?,
    newZoneName: String,
    newZoneRadiusMeters: String,
    fieldObservations: String,
    isSavingOffline: Boolean,
    isSyncingBackend: Boolean,
    onSwitchToColorTab: () -> Unit,
    onAssociationModeChanged: (TargetAssociationMode) -> Unit,
    onSelectExistingTree: (String) -> Unit,
    onOpenTreeDetailClick: (String) -> Unit,
    onCreateTreeSectionDirectlyClick: () -> Unit,
    onNewTreeNameChanged: (String) -> Unit,
    onNewTreeSpeciesChanged: (String) -> Unit,
    onSelectExistingZone: (String) -> Unit,
    onNewZoneNameChanged: (String) -> Unit,
    onNewZoneRadiusChanged: (String) -> Unit,
    onObservationsChanged: (String) -> Unit,
    onSaveToRoomClick: () -> Unit,
    onSyncNowClick: () -> Unit,
    modifier: Modifier = Modifier,
    photoSharedModifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = EcoVerdeMotion.standardSpatialSpring()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Visor de Fotografía Original (Figura 47: sin imagen coloreada)
        Card(
            modifier = photoSharedModifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Evidencia Fotográfica (${sampleType.displayName})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Offline • ${processingElapsedMs} ms",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(Color(0xFF141A15)),
                    contentAlignment = Alignment.Center
                ) {
                    if (originalBitmap != null) {
                        Image(
                            bitmap = originalBitmap.asImageBitmap(),
                            contentDescription = "Fotografía original capturada",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text(
                            text = "Sin fotografía cargada",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp),
                        shape = MaterialTheme.shapes.small,
                        color = Color.Black.copy(alpha = 0.72f)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text(
                                text = String.format("GPS: %.5f, %.5f", captureLatitude, captureLongitude),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Origen: $captureLocationSourceLabel",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (captureHasExifGps) Color(0xFFA5D6A7) else Color(0xFFFFE082)
                            )
                        }
                    }
                }

                // Resumen de Metadatos EXIF / Ubicación de la fotografía
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = if (captureHasExifGps) {
                                "✓ Metadatos EXIF detectados en imagen ($captureLocationSourceLabel)"
                            } else {
                                "📍 $captureLocationSourceLabel"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        val exifDetails = buildList {
                            captureExifDateTimeIso?.takeIf { it.isNotBlank() }?.let {
                                add("Fecha captura: ${it.take(19).replace('T', ' ')}")
                            }
                            captureExifCameraModel?.takeIf { it.isNotBlank() }?.let {
                                add("Cámara/Disp: $it")
                            }
                            if (originalBitmap != null) {
                                add("Resolución: ${originalBitmap.width}×${originalBitmap.height} px")
                            }
                        }
                        if (exifDetails.isNotEmpty()) {
                            Text(
                                text = exifDetails.joinToString(" • "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 2. Tarjeta de Diagnóstico Fitosanitario (Figura 47 - RN-23, RN-24, RN-26)
        if (classificationResult != null) {
            val statusColor = parseHexColor(classificationResult.outcome.statusHexColor)

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
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Column {
                                Text(
                                    text = if (sampleType == SampleType.CORTEZA) {
                                        "Diagnóstico TFLite (EfficientNet-B3)"
                                    } else {
                                        "Diagnóstico Macro de Dosel (Follaje)"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = classificationResult.outcome.displayTitle.uppercase(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = statusColor
                                )
                            }
                        }

                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = statusColor.copy(alpha = 0.14f)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "${classificationResult.confidencePercentage}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = statusColor
                                )
                                Text(
                                    text = "Confianza",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusColor
                                )
                            }
                        }
                    }

                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "TFLite (%): ${classificationResult.tfliteRawPercentage}%",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Clase_TFLite: ${classificationResult.outcome.displayTitle}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                    }

                    val visualSignals = buildVisualSignalsList(
                        sampleType = sampleType,
                        outcome = classificationResult.outcome,
                        chromaticResult = chromaticResult
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Señales visuales identificadas:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        visualSignals.forEach { signal ->
                            Text(
                                text = "• $signal",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Recomendación Fitosanitaria (NOM-019-SEMARNAT-2017):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = classificationResult.recommendation,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSwitchToColorTab()
                        },
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Inspeccionar Imagen Coloreada por Secciones (Pestaña 2)")
                    }
                }
            }
        }

        // 3. Seleccionar Árbol (Figura 46 - RN-37) o Zona de Follaje (chk_arbol_o_zona) y Guardado Offline (RN-50)
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
                Text(
                    text = if (sampleType == SampleType.CORTEZA) {
                        "Seleccionar árbol"
                    } else {
                        "Seleccionar Zona de Follaje / Dron (chk_arbol_o_zona)"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (sampleType == SampleType.CORTEZA) {
                        "¿A qué árbol pertenece esta captura? Si creas uno nuevo (ej. Fresno95), tendrá su propia sección de progreso."
                    } else {
                        "Asocia esta captura aérea o de copa a una zona de monitoreo del Bosque de Chapultepec."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (sampleType == SampleType.CORTEZA) {
                    if (treeProgressionList.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            treeProgressionList.forEach { prog ->
                                val isSelected = associationMode == TargetAssociationMode.EXISTENTE &&
                                    selectedTreeId == prog.tree.idArbol
                                val badgeColor = when {
                                    prog.isUnreviewed -> Color(0xFFE65100)
                                    prog.currentStatus == ClassificationOutcome.INFESTADO -> Color(0xFFC62828)
                                    prog.currentStatus == ClassificationOutcome.SANO -> Color(0xFF2E7D32)
                                    else -> Color(0xFFE65100)
                                }
                                val rowBgColor by animateColorAsState(
                                    targetValue = if (isSelected) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerLow
                                    },
                                    animationSpec = EcoVerdeMotion.interactiveSpring(),
                                    label = "treeSelectorRowBg"
                                )
                                val rowInteraction = remember(prog.tree.idArbol) { MutableInteractionSource() }

                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = rowBgColor,
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.medium)
                                        .clickable(
                                            interactionSource = rowInteraction,
                                            indication = ripple(bounded = true, color = badgeColor),
                                            role = Role.RadioButton,
                                            onClick = {
                                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onAssociationModeChanged(TargetAssociationMode.EXISTENTE)
                                                onSelectExistingTree(prog.tree.idArbol)
                                            }
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Surface(
                                                shape = MaterialTheme.shapes.small,
                                                color = badgeColor.copy(alpha = 0.14f),
                                                modifier = Modifier.size(38.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = if (isSelected) "✓" else "🌲",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = badgeColor
                                                    )
                                                }
                                            }
                                            Column {
                                                Text(
                                                    text = prog.tree.nombreEspecifico,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                val historySubtitle = if (prog.totalCaptures > 0) {
                                                    "${prog.totalCaptures} análisis • ${prog.healthyHistoryPercentage}% Sano / ${prog.infestedHistoryPercentage}% Infestado"
                                                } else {
                                                    "0 análisis • ${prog.tree.especie}"
                                                }
                                                Text(
                                                    text = historySubtitle,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = MaterialTheme.shapes.small,
                                                color = badgeColor.copy(alpha = 0.14f)
                                            ) {
                                                Text(
                                                    text = prog.statusBadgeLabel,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = badgeColor,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                            Surface(
                                                shape = MaterialTheme.shapes.small,
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                modifier = Modifier
                                                    .clip(MaterialTheme.shapes.small)
                                                    .clickable {
                                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        onOpenTreeDetailClick(prog.tree.idArbol)
                                                    }
                                            ) {
                                                Text(
                                                    text = "Sección ›",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Botón / tarjeta "+ Registrar árbol nuevo" con animación de expansión por resorte (Figura 46)
                    val isNewTreeMode = associationMode == TargetAssociationMode.NUEVO || treeProgressionList.isEmpty()
                    val newTreeInteraction = remember { MutableInteractionSource() }
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = if (isNewTreeMode) {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        } else {
                            Color.Transparent
                        },
                        border = BorderStroke(
                            width = 1.5.dp,
                            color = Color(0xFF2E7D32)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable(
                                interactionSource = newTreeInteraction,
                                indication = ripple(bounded = true, color = Color(0xFF2E7D32)),
                                onClick = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onAssociationModeChanged(TargetAssociationMode.NUEVO)
                                }
                            )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "+ Registrar árbol nuevo (crear sección independiente)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }

                            AnimatedVisibility(
                                visible = isNewTreeMode,
                                enter = expandVertically(animationSpec = EcoVerdeMotion.standardSpatialSpring()) + fadeIn(),
                                exit = shrinkVertically(animationSpec = EcoVerdeMotion.standardSpatialSpring()) + fadeOut()
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = newTreeName,
                                        onValueChange = onNewTreeNameChanged,
                                        label = { Text("Nombre/Identificador del árbol (ej. Fresno95)") },
                                        singleLine = true,
                                        shape = MaterialTheme.shapes.medium,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(
                                        text = "Especie hospedera en Bosque de Chapultepec:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    val speciesOptions = listOf(
                                        "Fresno (Fraxinus uhdei)",
                                        "Cedro blanco (Cupressus lusitanica)"
                                    )
                                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                        speciesOptions.forEachIndexed { index, sp ->
                                            val isSelectedSp = newTreeSpecies == sp
                                            SegmentedButton(
                                                selected = isSelectedSp,
                                                onClick = {
                                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    onNewTreeSpeciesChanged(sp)
                                                },
                                                shape = SegmentedButtonDefaults.itemShape(
                                                    index = index,
                                                    count = speciesOptions.size
                                                ),
                                                label = {
                                                    Text(
                                                        text = if (sp.startsWith("Fresno")) {
                                                            "Fresno (F. uhdei)"
                                                        } else {
                                                            "Cedro blanco"
                                                        },
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                }
                                            )
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onCreateTreeSectionDirectlyClick()
                                        },
                                        shape = MaterialTheme.shapes.medium,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Crear sección '${newTreeName.ifBlank { "Nuevo Árbol" }}' y abrir su progreso")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        TargetAssociationMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = associationMode == mode,
                                onClick = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onAssociationModeChanged(mode)
                                },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = TargetAssociationMode.entries.size
                                ),
                                label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    if (associationMode == TargetAssociationMode.EXISTENTE && availableZones.isNotEmpty()) {
                        Text(
                            text = "Selecciona una zona registrada:",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            availableZones.take(5).forEach { zone ->
                                val isSelected = selectedZoneId == zone.idZona
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSelectExistingZone(zone.idZona)
                                    },
                                    shape = MaterialTheme.shapes.medium,
                                    label = {
                                        Text(
                                            text = "${zone.nombreZona} (${zone.radioMetros.toInt()} m)",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newZoneName,
                                onValueChange = onNewZoneNameChanged,
                                label = { Text("Nombre de Zona Follaje") },
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = newZoneRadiusMeters,
                                onValueChange = onNewZoneRadiusChanged,
                                label = { Text("Radio (m)") },
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.weight(0.45f)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = fieldObservations,
                    onValueChange = onObservationsChanged,
                    placeholder = { Text("Notas de inspección fitosanitaria...") },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(2.dp))

                val selectedTreeLabel = if (sampleType == SampleType.CORTEZA) {
                    if (associationMode == TargetAssociationMode.EXISTENTE) {
                        treeProgressionList.firstOrNull { it.tree.idArbol == selectedTreeId }?.tree?.nombreEspecifico
                            ?: newTreeName
                    } else {
                        newTreeName.ifBlank { "Nuevo Árbol" }
                    }
                } else {
                    newZoneName.ifBlank { "Zona Follaje" }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSaveToRoomClick()
                        },
                        enabled = !isSavingOffline && classificationResult != null && chromaticResult != null,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            if (isSavingOffline) {
                                "Guardando..."
                            } else {
                                "Guardar en '$selectedTreeLabel'"
                            }
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSyncNowClick()
                        },
                        enabled = !isSyncingBackend,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.weight(0.75f)
                    ) {
                        Text(if (isSyncingBackend) "Sincronizando..." else "Sync Nube")
                    }
                }
            }
        }
    }
}

private fun buildVisualSignalsList(
    sampleType: SampleType,
    outcome: ClassificationOutcome,
    chromaticResult: ChromaticAnalysisResult?
): List<String> {
    val signals = mutableListOf<String>()
    val pctDanado = (chromaticResult?.pctDanado ?: 0.0) * 100.0
    val pctRegular = (chromaticResult?.pctRegular ?: 0.0) * 100.0
    val pctSano = (chromaticResult?.pctSano ?: 0.0) * 100.0

    if (sampleType == SampleType.CORTEZA) {
        when (outcome) {
            ClassificationOutcome.INFESTADO -> {
                signals.add("Patrones compatibles con galerías subcorticales / orificios de descortezadores (Phloeosinus / Hylesinus).")
                if (pctDanado >= 8.0) {
                    signals.add(String.format("Exudados café-anaranjados y tejido leñoso alterado en %.1f%% de la superficie.", pctDanado))
                }
                if (pctRegular >= 5.0) {
                    signals.add(String.format("Grumos de resina defensiva blanca/crema en %.1f%% de la muestra.", pctRegular))
                }
            }
            ClassificationOutcome.SANO -> {
                signals.add(String.format("Corteza natural íntegra (%.1f%% sin colonización subcortical aparente).", pctSano))
                signals.add("Ausencia de galerías expuestas u orificios de emergencia activos.")
            }
            ClassificationOutcome.NO_DETERMINADO -> {
                signals.add("Confianza por debajo del umbral o iluminación irregular; se sugiere nueva toma a 20-40 cm.")
            }
        }
    } else {
        signals.add(String.format("Follaje vigoroso (clorofila activa): %.1f%% del dosel.", pctSano))
        signals.add(String.format("Follaje clorótico verde-limón (Fase 2 deterioro activo): %.1f%%.", pctRegular))
        signals.add(String.format("Tejido necrótico café-rojizo (Fase 3 muerte regresiva): %.1f%%.", pctDanado))
    }
    return signals
}
