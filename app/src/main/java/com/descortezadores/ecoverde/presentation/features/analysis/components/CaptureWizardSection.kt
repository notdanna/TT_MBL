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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Nature
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.descortezadores.ecoverde.domain.model.DiagnosticCategory
import com.descortezadores.ecoverde.domain.model.SampleType
import com.descortezadores.ecoverde.presentation.features.analysis.AnalysisResultTab
import com.descortezadores.ecoverde.presentation.features.analysis.AnalysisResultUiState
import com.descortezadores.ecoverde.presentation.features.analysis.PREDEFINED_TREE_SPECIES
import com.descortezadores.ecoverde.presentation.features.analysis.TargetAssociationMode
import com.descortezadores.ecoverde.presentation.features.analysis.WizardStep
import com.descortezadores.ecoverde.presentation.theme.EcoVerdeMotion
import com.descortezadores.ecoverde.presentation.theme.GreenPrimary
import com.descortezadores.ecoverde.presentation.theme.StatusHealthyGreen
import com.descortezadores.ecoverde.presentation.theme.StatusInfestedRed

/**
 * Módulo 5: Wizard de Nueva Captura (Flujo lineal guiado en 4 pasos).
 *
 * Pasos:
 * 1. Selección de modalidad (Corteza vs Follaje / Dosel).
 * 2. Vinculación territorial/arbórea (Árbol existente / Alta rápida de nuevo árbol o Zona).
 * 3. Captura y encuadre (Guía cuadrada 300x300 px como referencia de EfficientNet-B3 + Confirmación).
 * 4. Pantalla de Resultados y Divulgación Progresiva.
 */
@Composable
fun CaptureWizardSection(
    uiState: AnalysisResultUiState,
    onStepSelected: (WizardStep) -> Unit,
    onSelectSampleType: (SampleType) -> Unit,
    onAssociationModeChanged: (TargetAssociationMode) -> Unit,
    onSelectExistingTree: (String) -> Unit,
    onNewTreeNameChanged: (String) -> Unit,
    onNewTreeSpeciesChanged: (String) -> Unit,
    onCreateTreeSectionDirectlyClick: () -> Unit,
    onSelectExistingZone: (String) -> Unit,
    onNewZoneNameChanged: (String) -> Unit,
    onNewZoneRadiusChanged: (String) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onLaunchCameraClick: () -> Unit,
    onLaunchGalleryClick: () -> Unit,
    onRunSyntheticSample: (SampleType, Boolean) -> Unit = { _, _ -> },
    onUpdateCroppedBitmap: (Bitmap) -> Unit = {},
    onRestoreOriginalBitmap: () -> Unit = {},
    onRotateBitmap: () -> Unit = {},
    onProcessCurrentSampleClick: () -> Unit,
    onSelectTab: (AnalysisResultTab) -> Unit,
    onCategoryFilterSelected: (DiagnosticCategory?) -> Unit,
    onAlphaChanged: (Float) -> Unit,
    onObservationsChanged: (String) -> Unit,
    onSaveAndFinishClick: () -> Unit,
    onRepeatCaptureClick: () -> Unit,
    onCancelWizardClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = EcoVerdeMotion.standardSpatialSpring()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Cabecera con Stepper de 4 Pasos
        WizardStepperHeader(
            currentStep = uiState.wizardStep,
            onStepClick = { step ->
                if (step.stepNumber <= uiState.wizardStep.stepNumber || uiState.classificationResult != null) {
                    onStepSelected(step)
                }
            },
            onCancelClick = onCancelWizardClick
        )

        // Contenido animado direccional del paso actual
        AnimatedContent(
            targetState = uiState.wizardStep,
            transitionSpec = {
                val forward = targetState.stepNumber >= initialState.stepNumber
                EcoVerdeMotion.sharedAxisX(forward = forward, slideDistance = 100)
            },
            label = "WizardStepAnimatedContent"
        ) { step ->
            when (step) {
                WizardStep.MODALIDAD -> {
                    Step1ModalidadSelection(
                        selectedType = uiState.sampleType,
                        onSelectSampleType = { type ->
                            onSelectSampleType(type)
                            onStepSelected(WizardStep.VINCULACION)
                        }
                    )
                }

                WizardStep.VINCULACION -> {
                    Step2VinculacionTerritorial(
                        uiState = uiState,
                        onAssociationModeChanged = onAssociationModeChanged,
                        onSelectExistingTree = onSelectExistingTree,
                        onNewTreeNameChanged = onNewTreeNameChanged,
                        onNewTreeSpeciesChanged = onNewTreeSpeciesChanged,
                        onCreateTreeSectionDirectlyClick = onCreateTreeSectionDirectlyClick,
                        onSelectExistingZone = onSelectExistingZone,
                        onNewZoneNameChanged = onNewZoneNameChanged,
                        onNewZoneRadiusChanged = onNewZoneRadiusChanged,
                        onSearchQueryChanged = onSearchQueryChanged,
                        onNextClick = { onStepSelected(WizardStep.CAPTURA) },
                        onBackClick = { onStepSelected(WizardStep.MODALIDAD) }
                    )
                }

                WizardStep.CAPTURA -> {
                    Step3CapturaYEncuadre(
                        uiState = uiState,
                        onLaunchCameraClick = onLaunchCameraClick,
                        onLaunchGalleryClick = onLaunchGalleryClick,
                        onUpdateCroppedBitmap = onUpdateCroppedBitmap,
                        onRestoreOriginalBitmap = onRestoreOriginalBitmap,
                        onRotateBitmap = onRotateBitmap,
                        onProcessSampleClick = onProcessCurrentSampleClick,
                        onBackClick = { onStepSelected(WizardStep.VINCULACION) }
                    )
                }

                WizardStep.RESULTADOS -> {
                    BifurcatedResultScreen(
                        uiState = uiState,
                        onSelectTab = onSelectTab,
                        onCategoryFilterSelected = onCategoryFilterSelected,
                        onAlphaChanged = onAlphaChanged,
                        onObservationsChanged = onObservationsChanged,
                        onSaveAndFinishClick = onSaveAndFinishClick,
                        onRepeatCaptureClick = onRepeatCaptureClick
                    )
                }
            }
        }
    }
}

/**
 * Stepper visual superior de 4 pasos para guiar al técnico.
 */
@Composable
private fun WizardStepperHeader(
    currentStep: WizardStep,
    onStepClick: (WizardStep) -> Unit,
    onCancelClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                Column {
                    Text(
                        text = "Asistente de Captura (Paso ${currentStep.stepNumber} de 4)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = currentStep.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(onClick = onCancelClick) {
                    Text("Salir", style = MaterialTheme.typography.labelSmall)
                }
            }

            // Indicador de pasos horizontales
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                WizardStep.entries.forEach { step ->
                    val isDone = step.stepNumber < currentStep.stepNumber
                    val isCurrent = step == currentStep
                    val stepColor = when {
                        isCurrent -> GreenPrimary
                        isDone -> StatusHealthyGreen
                        else -> MaterialTheme.colorScheme.outlineVariant
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(stepColor)
                    )
                }
            }
        }
    }
}

/**
 * Paso 1: Selección de Modalidad (Figura 45 del Trabajo Terminal).
 * Dos tarjetas interactivas grandes con ilustración/icono claro:
 * - Corteza de árbol (análisis individual por micro-inspección)
 * - Follaje / Dosel (análisis de área para dron o foto amplia)
 */
@Composable
private fun Step1ModalidadSelection(
    selectedType: SampleType,
    onSelectSampleType: (SampleType) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Selecciona la modalidad de captura",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Elige el método de inspección fitosanitaria según el objetivo en campo:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Tarjeta 1: Corteza de árbol (Micro-inspección individual)
        ModalidadOptionCard(
            title = "Corteza de árbol",
            subtitle = "Inspección a nivel de fuste individual para identificar signos de daño, galerías o colonización en la corteza.",
            tagText = "Inspección individual",
            icon = Icons.Default.Forest,
            isSelected = selectedType == SampleType.CORTEZA,
            accentColor = GreenPrimary,
            onClick = { onSelectSampleType(SampleType.CORTEZA) }
        )

        // Tarjeta 2: Follaje / Dosel (Macro-análisis aéreo o de copa)
        ModalidadOptionCard(
            title = "Follaje / Dosel",
            subtitle = "Monitoreo de copas para fotografía terrestre o aérea. Evalúa estrés foliar y defoliación en el dosel.",
            tagText = "Monitoreo de dosel",
            icon = Icons.Default.Nature,
            isSelected = selectedType == SampleType.FOLLAJE,
            accentColor = Color(0xFF2E7D32),
            onClick = { onSelectSampleType(SampleType.FOLLAJE) }
        )
    }
}

@Composable
private fun ModalidadOptionCard(
    title: String,
    subtitle: String,
    tagText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                accentColor.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(accentColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = accentColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = tagText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Seleccionado",
                        tint = accentColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onClick,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isSelected) "Continuar con $title →" else "Seleccionar $title",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Paso 2: Vinculación Territorial / Arbórea (Figura 46 del Trabajo Terminal).
 * - Si es corteza: Buscador/selector de árboles existentes en la jornada o botón destacado "+ Dar de alta nuevo árbol".
 * - Si es follaje: Selector o alta rápida de Zona de Follaje.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun Step2VinculacionTerritorial(
    uiState: AnalysisResultUiState,
    onAssociationModeChanged: (TargetAssociationMode) -> Unit,
    onSelectExistingTree: (String) -> Unit,
    onNewTreeNameChanged: (String) -> Unit,
    onNewTreeSpeciesChanged: (String) -> Unit,
    onCreateTreeSectionDirectlyClick: () -> Unit,
    onSelectExistingZone: (String) -> Unit,
    onNewZoneNameChanged: (String) -> Unit,
    onNewZoneRadiusChanged: (String) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val isCorteza = uiState.sampleType == SampleType.CORTEZA

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = if (isCorteza) "Vinculación con Árbol" else "Vinculación con Zona de Follaje",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // Alternador de modo: Existente vs Nuevo registro
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            TargetAssociationMode.entries.forEachIndexed { index, mode ->
                val selected = uiState.associationMode == mode
                val label = if (isCorteza) {
                    mode.label
                } else {
                    if (mode == TargetAssociationMode.EXISTENTE) "Zona existente" else "Dar de alta nueva zona"
                }
                SegmentedButton(
                    selected = selected,
                    onClick = { onAssociationModeChanged(mode) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = TargetAssociationMode.entries.size
                    ),
                    label = {
                        Text(
                            text = label,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        if (isCorteza) {
            // MODO CORTEZA
            if (uiState.associationMode == TargetAssociationMode.EXISTENTE) {
                // Buscador de árboles existentes en la jornada
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
                        OutlinedTextField(
                            value = uiState.treeSearchQuery,
                            onValueChange = onSearchQueryChanged,
                            label = { Text("Buscar árbol en la jornada...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            shape = MaterialTheme.shapes.medium,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        val filteredTrees = uiState.treeProgressionList.filter {
                            it.tree.nombreEspecifico.contains(uiState.treeSearchQuery, ignoreCase = true) ||
                                it.tree.especie.contains(uiState.treeSearchQuery, ignoreCase = true)
                        }

                        if (filteredTrees.isEmpty()) {
                            Text(
                                text = "No se encontraron árboles con ese nombre. Cambia a 'Dar de alta nuevo' para registrarlo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            filteredTrees.take(5).forEach { prog ->
                                val isSelected = prog.tree.idArbol == uiState.selectedTreeId
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = if (isSelected) {
                                        GreenPrimary.copy(alpha = 0.14f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerLow
                                    },
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) GreenPrimary else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.medium)
                                        .clickable { onSelectExistingTree(prog.tree.idArbol) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = prog.tree.nombreEspecifico,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = prog.statusBadgeLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = GreenPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // DAR DE ALTA NUEVO ÁRBOL (RF-19, RN-37, RN-38)
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
                        Text(
                            text = "Dar de alta nuevo árbol",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimary
                        )

                        Text(
                            text = "Se registrará automáticamente con las coordenadas GPS actuales del dispositivo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = uiState.newTreeName,
                            onValueChange = onNewTreeNameChanged,
                            label = { Text("Identificador / Nombre del árbol") },
                            placeholder = { Text("Ej. Árbol 01 - Calzada de los Poetas") },
                            shape = MaterialTheme.shapes.medium,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "Especie forestal",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            PREDEFINED_TREE_SPECIES.forEach { species ->
                                val selected = uiState.newTreeSpecies == species
                                FilterChip(
                                    selected = selected,
                                    onClick = { onNewTreeSpeciesChanged(species) },
                                    label = {
                                        Text(
                                            text = species.substringBefore(" ("),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    leadingIcon = if (selected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }

                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = GreenPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = String.format("GPS automático: %.5f, %.5f", uiState.captureLatitude, uiState.captureLongitude),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // MODO FOLLAJE: Selector o alta rápida de Zona de Follaje
            if (uiState.associationMode == TargetAssociationMode.EXISTENTE) {
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
                        Text(
                            text = "Seleccionar Zona de Follaje Existente",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimary
                        )

                        if (uiState.availableZones.isEmpty()) {
                            Text(
                                text = "No hay zonas de follaje registradas previamente. Cambia a 'Dar de alta nueva zona' para registrar una.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            uiState.availableZones.forEach { zone ->
                                val isSelected = zone.idZona == uiState.selectedZoneId
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = if (isSelected) GreenPrimary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerLow,
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) GreenPrimary else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.medium)
                                        .clickable { onSelectExistingZone(zone.idZona) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = zone.nombreZona,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = zone.descripcion ?: "Zona aérea de monitoreo",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = GreenPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // DAR DE ALTA NUEVA ZONA
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
                        Text(
                            text = "Dar de alta nueva zona de dosel",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimary
                        )
                        Text(
                            text = "Se asociará automáticamente a las coordenadas GPS de la toma o sobrevuelo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = uiState.newZoneName,
                            onValueChange = onNewZoneNameChanged,
                            label = { Text("Nombre de la zona de dosel") },
                            placeholder = { Text("Ej. Zona II - Calzada de los Poetas") },
                            shape = MaterialTheme.shapes.medium,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Botones de navegación del Wizard
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onBackClick,
                shape = CircleShape,
                modifier = Modifier.weight(1f)
            ) {
                Text("← Modalidad")
            }
            Button(
                onClick = onNextClick,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                modifier = Modifier.weight(1f)
            ) {
                Text("Capturar foto →", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Paso 3: Captura y Encuadre con Mini Editor de Imágenes.
 * Permite tomar foto con cámara, elegir de galería, recortar áreas de interés, rotar y restaurar.
 */
@Composable
private fun Step3CapturaYEncuadre(
    uiState: AnalysisResultUiState,
    onLaunchCameraClick: () -> Unit,
    onLaunchGalleryClick: () -> Unit,
    onUpdateCroppedBitmap: (Bitmap) -> Unit,
    onRestoreOriginalBitmap: () -> Unit,
    onRotateBitmap: () -> Unit,
    onProcessSampleClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Captura y Encuadre",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Alinea o recorta el área específica que deseas analizar con las herramientas de encuadre:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (uiState.originalBitmap != null) {
            // MINI EDITOR DE IMAGEN INTERACTIVO
            ImageCropEditor(
                bitmap = uiState.originalBitmap,
                canRestore = uiState.uncroppedBitmap != null && uiState.uncroppedBitmap != uiState.originalBitmap,
                onCropConfirmed = onUpdateCroppedBitmap,
                onRestoreOriginal = onRestoreOriginalBitmap,
                onRotate90 = onRotateBitmap
            )
        } else {
            // Estado vacío previo a la captura
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(GreenPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = GreenPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = "Presiona Cámara o Galería para cargar la toma",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Podrás recortar y seleccionar el área exacta a analizar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Botones de acción para capturar / cambiar foto
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onLaunchCameraClick,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (uiState.originalBitmap != null) "Nueva foto" else "Abrir cámara",
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = onLaunchGalleryClick,
                shape = CircleShape,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (uiState.originalBitmap != null) "Cambiar foto" else "Desde galería")
            }
        }

        // Botón primario de confirmación: Analizar imagen (sin muestras de calibración)
        Button(
            onClick = onProcessSampleClick,
            enabled = uiState.originalBitmap != null && !uiState.isProcessingVision,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            if (uiState.isProcessingVision) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analizando imagen...")
            } else {
                Text(
                    text = "Analizar imagen →",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        OutlinedButton(
            onClick = onBackClick,
            shape = CircleShape,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("← Regresar a vinculación")
        }
    }
}

/**
 * Mini Editor de Imágenes para recorte, enfoque y rotación interactiva antes del análisis.
 */
@Composable
fun ImageCropEditor(
    bitmap: Bitmap,
    canRestore: Boolean,
    onCropConfirmed: (Bitmap) -> Unit,
    onRestoreOriginal: () -> Unit,
    onRotate90: (() -> Unit)? = null
) {
    // Normalizado de 0.0f a 1.0f para la selección de recorte
    var normLeft by remember(bitmap) { mutableFloatStateOf(0.1f) }
    var normTop by remember(bitmap) { mutableFloatStateOf(0.1f) }
    var normRight by remember(bitmap) { mutableFloatStateOf(0.9f) }
    var normBottom by remember(bitmap) { mutableFloatStateOf(0.9f) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Contenedor visual de la imagen con overlay interactivo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(Color.Black)
            ) {
                val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "Muestra para encuadre",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                // Lienzo de recorte y retícula interactiva
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(bitmap) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val w = size.width.toFloat()
                                val h = size.height.toFloat()
                                if (w <= 0f || h <= 0f) return@detectDragGestures

                                val dx = dragAmount.x / w
                                val dy = dragAmount.y / h
                                val touchNormX = change.position.x / w
                                val touchNormY = change.position.y / h

                                val threshold = 0.15f
                                val nearLeft = kotlin.math.abs(touchNormX - normLeft) < threshold
                                val nearRight = kotlin.math.abs(touchNormX - normRight) < threshold
                                val nearTop = kotlin.math.abs(touchNormY - normTop) < threshold
                                val nearBottom = kotlin.math.abs(touchNormY - normBottom) < threshold

                                if (nearLeft && nearTop) {
                                    normLeft = (normLeft + dx).coerceIn(0f, normRight - 0.15f)
                                    normTop = (normTop + dy).coerceIn(0f, normBottom - 0.15f)
                                } else if (nearRight && nearTop) {
                                    normRight = (normRight + dx).coerceIn(normLeft + 0.15f, 1f)
                                    normTop = (normTop + dy).coerceIn(0f, normBottom - 0.15f)
                                } else if (nearLeft && nearBottom) {
                                    normLeft = (normLeft + dx).coerceIn(0f, normRight - 0.15f)
                                    normBottom = (normBottom + dy).coerceIn(normTop + 0.15f, 1f)
                                } else if (nearRight && nearBottom) {
                                    normRight = (normRight + dx).coerceIn(normLeft + 0.15f, 1f)
                                    normBottom = (normBottom + dy).coerceIn(normTop + 0.15f, 1f)
                                } else {
                                    // Mover selección completa
                                    val rectW = normRight - normLeft
                                    val rectH = normBottom - normTop
                                    val newLeft = (normLeft + dx).coerceIn(0f, 1f - rectW)
                                    val newTop = (normTop + dy).coerceIn(0f, 1f - rectH)
                                    normLeft = newLeft
                                    normRight = newLeft + rectW
                                    normTop = newTop
                                    normBottom = newTop + rectH
                                }
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    val pxLeft = normLeft * w
                    val pxTop = normTop * h
                    val pxRight = normRight * w
                    val pxBottom = normBottom * h

                    val overlayColor = Color.Black.copy(alpha = 0.55f)

                    // 4 rectángulos oscurecidos alrededor del área seleccionada
                    drawRect(overlayColor, topLeft = Offset(0f, 0f), size = Size(w, pxTop))
                    drawRect(overlayColor, topLeft = Offset(0f, pxBottom), size = Size(w, h - pxBottom))
                    drawRect(overlayColor, topLeft = Offset(0f, pxTop), size = Size(pxLeft, pxBottom - pxTop))
                    drawRect(overlayColor, topLeft = Offset(pxRight, pxTop), size = Size(w - pxRight, pxBottom - pxTop))

                    // Borde de selección
                    val selWidth = pxRight - pxLeft
                    val selHeight = pxBottom - pxTop
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(pxLeft, pxTop),
                        size = Size(selWidth, selHeight),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                    )

                    // Retícula de tercios interior
                    val oneThirdX = pxLeft + selWidth / 3f
                    val twoThirdsX = pxLeft + 2 * selWidth / 3f
                    val oneThirdY = pxTop + selHeight / 3f
                    val twoThirdsY = pxTop + 2 * selHeight / 3f

                    val gridColor = Color.White.copy(alpha = 0.4f)
                    val gridStroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                    drawLine(gridColor, Offset(oneThirdX, pxTop), Offset(oneThirdX, pxBottom), strokeWidth = gridStroke.width)
                    drawLine(gridColor, Offset(twoThirdsX, pxTop), Offset(twoThirdsX, pxBottom), strokeWidth = gridStroke.width)
                    drawLine(gridColor, Offset(pxLeft, oneThirdY), Offset(pxRight, oneThirdY), strokeWidth = gridStroke.width)
                    drawLine(gridColor, Offset(pxLeft, twoThirdsY), Offset(pxRight, twoThirdsY), strokeWidth = gridStroke.width)

                    // Puntos de esquina
                    val cornerRadius = 6.dp.toPx()
                    val cornerColor = GreenPrimary
                    drawCircle(cornerColor, radius = cornerRadius, center = Offset(pxLeft, pxTop))
                    drawCircle(cornerColor, radius = cornerRadius, center = Offset(pxRight, pxTop))
                    drawCircle(cornerColor, radius = cornerRadius, center = Offset(pxLeft, pxBottom))
                    drawCircle(cornerColor, radius = cornerRadius, center = Offset(pxRight, pxBottom))
                }

                // Insignia informativa
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    shape = MaterialTheme.shapes.small,
                    color = Color.Black.copy(alpha = 0.65f)
                ) {
                    Text(
                        text = "Arrastra el marco o esquinas para encuadrar",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Opciones de edición: Recortar, Rotar 90° y Restaurar a original
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val bWidth = bitmap.width
                            val bHeight = bitmap.height
                            val x = (normLeft * bWidth).toInt().coerceIn(0, bWidth - 1)
                            val y = (normTop * bHeight).toInt().coerceIn(0, bHeight - 1)
                            val w = ((normRight - normLeft) * bWidth).toInt().coerceIn(10, bWidth - x)
                            val h = ((normBottom - normTop) * bHeight).toInt().coerceIn(10, bHeight - y)
                            if (w > 10 && h > 10) {
                                val cropped = Bitmap.createBitmap(bitmap, x, y, w, h)
                                onCropConfirmed(cropped)
                                normLeft = 0.05f
                                normTop = 0.05f
                                normRight = 0.95f
                                normBottom = 0.95f
                            }
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Recorte", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }

                    if (onRotate90 != null) {
                        OutlinedButton(
                            onClick = onRotate90,
                            shape = CircleShape,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rotar 90°", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                        }
                    }
                }

                if (canRestore) {
                    OutlinedButton(
                        onClick = onRestoreOriginal,
                        shape = CircleShape,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restaurar a original", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
