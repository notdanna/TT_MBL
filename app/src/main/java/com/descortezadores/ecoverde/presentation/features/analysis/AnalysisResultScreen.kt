package com.descortezadores.ecoverde.presentation.features.analysis

import android.Manifest
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.descortezadores.ecoverde.core.common.DeviceIdentifierProvider
import com.descortezadores.ecoverde.core.common.ImageMetadataAndLocationHelper
import com.descortezadores.ecoverde.domain.model.ClassificationOutcome
import com.descortezadores.ecoverde.domain.model.DiagnosticCategory
import com.descortezadores.ecoverde.domain.model.SampleType
import com.descortezadores.ecoverde.domain.model.SyncStatus
import com.descortezadores.ecoverde.domain.model.TargetAssociation
import com.descortezadores.ecoverde.domain.model.TreeRecord
import com.descortezadores.ecoverde.domain.model.auth.BrigadistaSession
import com.descortezadores.ecoverde.domain.repository.SavedDetectionRecord
import com.descortezadores.ecoverde.presentation.features.analysis.components.AlertsAndSyncSection
import com.descortezadores.ecoverde.presentation.features.analysis.components.CaptureWizardSection
import com.descortezadores.ecoverde.presentation.features.analysis.components.OfflineForestMapLibreView
import com.descortezadores.ecoverde.presentation.features.analysis.components.parseHexColor
import com.descortezadores.ecoverde.presentation.theme.EcoVerdeMotion
import com.descortezadores.ecoverde.presentation.theme.GreenPrimary
import com.descortezadores.ecoverde.presentation.theme.StatusHealthyGreen
import com.descortezadores.ecoverde.presentation.theme.StatusInfestedRed
import com.descortezadores.ecoverde.presentation.theme.StatusUncertainAmber
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import java.io.File
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Contenedor Principal Hub & Spoke con barra inferior MD3 y Asistente Lineal (Wizard).
 *
 * Estructurado conforme al Plan de Diseño y Reestructuración de Pantallas:
 * - Hub & Spoke: Barra inferior MD3 con botón flotante central (FAB).
 * - Subgrafo de Asistente Lineal (Wizard) para la captura en 4 pasos guiados.
 * - Módulo 2: Hub Principal / Dashboard de la Jornada (4 métricas del día, chips de filtro rápido, tarjetas de árboles con estado fitosanitario y persistencia).
 * - Módulo 3: Detalle de Árbol (Maestro-Detalle con estado actual grande, ficha técnica, observaciones y fotos de la jornada).
 * - Módulo 4: Mapa Local de Árboles y Focos (MapLibre GL offline, círculos NOM-019, radio 100m y Bottom Sheet contextual).
 * - Módulo 5 y 6: Wizard de Captura y Resultados Bifurcados (Caso A: Corteza, Caso B: Follaje).
 * - Módulo 7: Centro de Alertas y Sincronización.
 * - Módulo 8: Perfil y Ajustes Técnicos.
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalSharedTransitionApi::class,
    ExperimentalLayoutApi::class
)
@Composable
fun AnalysisResultScreen(
    session: BrigadistaSession,
    uiState: AnalysisResultUiState,
    onSelectDestination: (EcoVerdeMainDestination) -> Unit,
    onSelectTab: (AnalysisResultTab) -> Unit,
    onSelectSampleType: (SampleType) -> Unit,
    onAnalyzeBitmap: (Bitmap) -> Unit,
    onAnalyzeGalleryUri: (Uri) -> Unit = {},
    onAnalyzeCameraFile: (File) -> Unit = {},
    onRefreshDeviceLocation: () -> Unit = {},
    onRunSyntheticSample: (SampleType, Boolean) -> Unit,
    onCategoryFilterSelected: (DiagnosticCategory?) -> Unit,
    onAlphaChanged: (Float) -> Unit,
    onToggleOriginalComparison: () -> Unit,
    onAssociationModeChanged: (TargetAssociationMode) -> Unit,
    onSelectExistingTree: (String) -> Unit,
    onHomeTreeFilterChanged: (TreeHealthFilter) -> Unit,
    onOpenTreeDetailClick: (String) -> Unit,
    onCloseTreeDetailClick: () -> Unit,
    onStartNewCaptureForTreeClick: (String) -> Unit,
    onCreateTreeSectionDirectlyClick: () -> Unit,
    onNewTreeNameChanged: (String) -> Unit,
    onNewTreeSpeciesChanged: (String) -> Unit,
    onSelectExistingZone: (String) -> Unit,
    onNewZoneNameChanged: (String) -> Unit,
    onNewZoneRadiusChanged: (String) -> Unit,
    onObservationsChanged: (String) -> Unit,
    onSaveToRoomClick: () -> Unit,
    onSyncNowClick: () -> Unit,
    onHistoryFilterChanged: (HistoryFilterOption) -> Unit,
    onInspectRecordClick: (SavedDetectionRecord) -> Unit,
    onSimulateCoordinatesClick: (Double, Double) -> Unit,
    onClearLocalDatabaseClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onStartWizard: (SampleType?, String?) -> Unit = { _, _ -> },
    onSetWizardStep: (WizardStep) -> Unit = {},
    onRepeatCapture: () -> Unit = {},
    onUpdateCroppedBitmap: (Bitmap) -> Unit = {},
    onRestoreOriginalBitmap: () -> Unit = {},
    onRotateBitmap: () -> Unit = {},
    onMapTreeSelected: (TreeRecord?) -> Unit = {},
    onDismissMapBottomSheet: () -> Unit = {},
    onSearchQueryTreeChanged: (String) -> Unit = {},
    onDeleteTreeClick: (String) -> Unit = {},
    onSaveTreeObservationsClick: (String, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    val canHandlePredictiveBack = uiState.selectedTreeProgression != null ||
        uiState.currentDestination != EcoVerdeMainDestination.INICIO
    val predictiveBackProgress = remember { Animatable(0f) }

    PredictiveBackHandler(enabled = canHandlePredictiveBack) { progress: Flow<BackEventCompat> ->
        try {
            progress.collect { backEvent ->
                predictiveBackProgress.snapTo(backEvent.progress)
            }
            predictiveBackProgress.snapTo(0f)
            if (uiState.selectedTreeProgression != null &&
                uiState.currentDestination == EcoVerdeMainDestination.INICIO
            ) {
                onCloseTreeDetailClick()
            } else {
                onSelectDestination(EcoVerdeMainDestination.INICIO)
            }
        } catch (e: CancellationException) {
            predictiveBackProgress.animateTo(
                targetValue = 0f,
                animationSpec = EcoVerdeMotion.expressiveSpatialSpring()
            )
            throw e
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            onAnalyzeGalleryUri(uri)
        }
    }

    var pendingCameraCaptureFile by remember { mutableStateOf<File?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { savedSuccessfully ->
        val capturedFile = pendingCameraCaptureFile
        if (savedSuccessfully && capturedFile != null && capturedFile.exists()) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            onAnalyzeCameraFile(capturedFile)
        }
    }

    val startCameraCaptureWithFileProvider = {
        runCatching {
            val (file, uri) = ImageMetadataAndLocationHelper.createTempCameraCaptureFileAndUri(context)
            pendingCameraCaptureFile = file
            cameraLauncher.launch(uri)
        }
    }

    val cameraAndLocationPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (locationGranted) {
            onRefreshDeviceLocation()
        }
        val cameraGranted = permissions[Manifest.permission.CAMERA] == true ||
            ImageMetadataAndLocationHelper.hasCameraPermission(context)
        if (cameraGranted) {
            startCameraCaptureWithFileProvider()
        }
    }

    val galleryAndLocationPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (locationGranted) {
            onRefreshDeviceLocation()
        }
        galleryLauncher.launch("image/*")
    }

    val initialLocationPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (locationGranted) {
            onRefreshDeviceLocation()
        }
    }

    LaunchedEffect(Unit) {
        if (!ImageMetadataAndLocationHelper.hasLocationPermission(context)) {
            val perms = buildList {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    add(Manifest.permission.ACCESS_MEDIA_LOCATION)
                }
            }.toTypedArray()
            initialLocationPermissionsLauncher.launch(perms)
        } else {
            onRefreshDeviceLocation()
        }
    }

    val triggerCameraWithMetadata: () -> Unit = {
        if (ImageMetadataAndLocationHelper.hasCameraPermission(context) &&
            ImageMetadataAndLocationHelper.hasLocationPermission(context)
        ) {
            startCameraCaptureWithFileProvider()
        } else {
            val perms = buildList {
                add(Manifest.permission.CAMERA)
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }.toTypedArray()
            cameraAndLocationPermissionsLauncher.launch(perms)
        }
    }

    val triggerGalleryWithMetadata: () -> Unit = {
        val needsLocationOrMediaPerm = !ImageMetadataAndLocationHelper.hasLocationPermission(context)
        if (needsLocationOrMediaPerm) {
            val perms = buildList {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    add(Manifest.permission.ACCESS_MEDIA_LOCATION)
                }
            }.toTypedArray()
            galleryAndLocationPermissionsLauncher.launch(perms)
        } else {
            galleryLauncher.launch("image/*")
        }
    }

    // Modal Bottom Sheet Contextual para el Mapa (Módulo 4)
    if (uiState.showMapBottomSheet && uiState.selectedMapTree != null) {
        val mapTree = uiState.selectedMapTree
        val sheetState = rememberModalBottomSheetState()
        val treeLat = mapTree.latitud ?: uiState.captureLatitude
        val treeLon = mapTree.longitud ?: uiState.captureLongitude
        val distanceMeters = calculateHaversineDistanceMeters(
            lat1 = uiState.captureLatitude,
            lon1 = uiState.captureLongitude,
            lat2 = treeLat,
            lon2 = treeLon
        )

        ModalBottomSheet(
            onDismissRequest = onDismissMapBottomSheet,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = mapTree.nombreEspecifico,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = mapTree.especie,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val outcome = mapTree.ultimoEstadoSanitario ?: ClassificationOutcome.NO_DETERMINADO
                    val badgeColor = when (outcome) {
                        ClassificationOutcome.INFESTADO -> StatusInfestedRed
                        ClassificationOutcome.SANO -> StatusHealthyGreen
                        ClassificationOutcome.NO_DETERMINADO -> StatusUncertainAmber
                    }

                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = badgeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = outcome.displayTitle,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NearMe,
                            contentDescription = null,
                            tint = GreenPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Distancia desde tu posición actual: ${distanceMeters.toInt()} metros",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onDismissMapBottomSheet()
                            onOpenTreeDetailClick(mapTree.idArbol)
                        },
                        shape = CircleShape,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Ver Detalle")
                    }

                    Button(
                        onClick = {
                            onDismissMapBottomSheet()
                            onStartNewCaptureForTreeClick(mapTree.idArbol)
                            onStartWizard(SampleType.CORTEZA, mapTree.idArbol)
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+ Nueva Captura")
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                scrollBehavior = scrollBehavior,
                windowInsets = TopAppBarDefaults.windowInsets,
                navigationIcon = {
                    if (uiState.currentDestination == EcoVerdeMainDestination.ANALIZAR) {
                        IconButton(onClick = { onSelectDestination(EcoVerdeMainDestination.INICIO) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver al Dashboard",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                },
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (uiState.currentDestination == EcoVerdeMainDestination.ANALIZAR) {
                            Text(
                                text = "Asistente de Captura",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                text = "Paso ${uiState.wizardStep.stepNumber}: ${uiState.wizardStep.title}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                        } else {
                            // Cabecera Contextual (Módulo 2): Saludo al técnico
                            val firstName = session.nombreCompleto.substringBefore(" ").ifBlank { "Técnico" }
                            Text(
                                text = "Hola, $firstName",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                text = "EcoVerde • Bosque de Chapultepec",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                        }
                    }
                },
                actions = {
                    // Campana de alertas con badge numérico
                    val alertCount = uiState.activeHotspots.size + uiState.daySummary.pendientesSincronizacion
                    IconButton(onClick = { onSelectDestination(EcoVerdeMainDestination.ALERTAS) }) {
                        BadgedBox(
                            badge = {
                                if (alertCount > 0) {
                                    Badge(containerColor = StatusInfestedRed) {
                                        Text("$alertCount")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Centro de Alertas",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    // Indicador de estado de sincronización global
                    val syncIcon = if (uiState.daySummary.pendientesSincronizacion > 0) {
                        Icons.Default.CloudUpload
                    } else {
                        Icons.Default.CloudDone
                    }
                    IconButton(onClick = onSyncNowClick) {
                        Icon(
                            imageVector = syncIcon,
                            contentDescription = "Estado de Sincronización",
                            tint = if (uiState.daySummary.pendientesSincronizacion > 0) {
                                StatusUncertainAmber
                            } else {
                                MaterialTheme.colorScheme.onPrimary
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = GreenPrimary,
                    scrolledContainerColor = GreenPrimary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        // Botón Flotante Central (FAB) para la captura rápida (Hub & Spoke MD3)
        floatingActionButton = {
            if (uiState.currentDestination != EcoVerdeMainDestination.ANALIZAR &&
                uiState.selectedTreeProgression == null
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onStartWizard(SampleType.CORTEZA, null)
                    },
                    icon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                    text = { Text("Nueva Captura", fontWeight = FontWeight.Bold) },
                    containerColor = GreenPrimary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp)
                )
            }
        },
        bottomBar = {
            NavigationBar(
                windowInsets = NavigationBarDefaults.windowInsets,
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 0.dp
            ) {
                val destinations = listOf(
                    Triple(EcoVerdeMainDestination.INICIO, "Inicio", Icons.Default.Forest),
                    Triple(EcoVerdeMainDestination.MAPA, "Mapa", Icons.Default.Map),
                    Triple(EcoVerdeMainDestination.ALERTAS, "Alertas", Icons.Default.Notifications),
                    Triple(EcoVerdeMainDestination.PERFIL, "Perfil", Icons.Default.Person)
                )

                destinations.forEach { (dest, label, icon) ->
                    val selected = uiState.currentDestination == dest
                    val badgeCount = when (dest) {
                        EcoVerdeMainDestination.ALERTAS -> uiState.daySummary.pendientesSincronizacion + uiState.activeHotspots.size
                        else -> 0
                    }

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectDestination(dest)
                        },
                        icon = {
                            if (badgeCount > 0) {
                                BadgedBox(badge = { Badge { Text("$badgeCount") } }) {
                                    Icon(imageVector = icon, contentDescription = label)
                                }
                            } else {
                                Icon(imageVector = icon, contentDescription = label)
                            }
                        },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }
    ) { innerPadding ->
        val backFraction = predictiveBackProgress.value
        SharedTransitionLayout {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .imePadding()
                    .graphicsLayer {
                        val scale = 1f - (backFraction * 0.08f)
                        scaleX = scale
                        scaleY = scale
                        alpha = 1f - (backFraction * 0.15f)
                    }
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Alerta de proximidad NOM-019 (si está a menos de 100m)
                uiState.proximityAlert?.let { alert ->
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = Color(0xFFB71C1C),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ALERTA DE PROXIMIDAD NOM-019 (${alert.distanciaMetros.toInt()} m)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = alert.mensajeAlerta,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.95f)
                                )
                            }
                            TextButton(
                                onClick = { onSelectDestination(EcoVerdeMainDestination.ALERTAS) },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                            ) {
                                Text("Ver alerta")
                            }
                        }
                    }
                }

                // Banner informativo o error (sin mostrar banners de 'Análisis completado')
                uiState.statusBannerMessage?.let { msg ->
                    if (!msg.contains("Análisis completado", ignoreCase = true)) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                uiState.errorMessage?.let { err ->
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Transición animada entre destinos principales
                AnimatedContent(
                    targetState = uiState.currentDestination,
                    transitionSpec = {
                        (fadeIn(animationSpec = EcoVerdeMotion.interactiveSpring()) +
                            scaleIn(
                                initialScale = 0.96f,
                                animationSpec = EcoVerdeMotion.expressiveSpatialSpring()
                            )) togetherWith
                            (fadeOut(animationSpec = EcoVerdeMotion.interactiveSpring()) +
                                scaleOut(
                                    targetScale = 0.96f,
                                    animationSpec = EcoVerdeMotion.standardSpatialSpring()
                                ))
                    },
                    label = "MainDestinationAnimatedContent"
                ) { destination ->
                    when (destination) {
                        EcoVerdeMainDestination.INICIO -> {
                            AnimatedContent(
                                targetState = uiState.selectedTreeProgression,
                                transitionSpec = {
                                    (fadeIn(animationSpec = EcoVerdeMotion.interactiveSpring()) +
                                        scaleIn(
                                            initialScale = 0.95f,
                                            animationSpec = EcoVerdeMotion.expressiveSpatialSpring()
                                        )) togetherWith
                                        (fadeOut(animationSpec = EcoVerdeMotion.interactiveSpring()) +
                                            scaleOut(
                                                targetScale = 0.95f,
                                                animationSpec = EcoVerdeMotion.standardSpatialSpring()
                                            ))
                                },
                                label = "TreeProgressionDetailAnimatedContent"
                            ) { activeTreeProgression ->
                                if (activeTreeProgression != null) {
                                    // Módulo 3: Detalle de Árbol
                                    TreeDetailProgressionSection(
                                        progression = activeTreeProgression,
                                        sharedTransitionScope = this@SharedTransitionLayout,
                                        animatedVisibilityScope = this,
                                        onBackToTreesClick = onCloseTreeDetailClick,
                                        onNewCaptureForTreeClick = {
                                            onStartNewCaptureForTreeClick(activeTreeProgression.tree.idArbol)
                                            onStartWizard(SampleType.CORTEZA, activeTreeProgression.tree.idArbol)
                                            triggerCameraWithMetadata()
                                        },
                                        onCenterMapOnTreeClick = { lat, lon ->
                                            onSelectDestination(EcoVerdeMainDestination.MAPA)
                                            onSimulateCoordinatesClick(lat, lon)
                                        },
                                        onSaveTreeObservationsClick = onSaveTreeObservationsClick,
                                        fieldObservations = uiState.fieldObservations,
                                        onObservationsChanged = onObservationsChanged,
                                        onInspectCaptureClick = onInspectRecordClick,
                                        onDeleteTreeClick = onDeleteTreeClick
                                    )
                                } else {
                                    // Módulo 2: Hub Principal / Dashboard de la Jornada
                                    DashboardJornadaSection(
                                        session = session,
                                        uiState = uiState,
                                        sharedTransitionScope = this@SharedTransitionLayout,
                                        animatedVisibilityScope = this,
                                        onHomeTreeFilterChanged = onHomeTreeFilterChanged,
                                        onOpenTreeDetailClick = onOpenTreeDetailClick,
                                        onNewTreeNameChanged = onNewTreeNameChanged,
                                        onNewTreeSpeciesChanged = onNewTreeSpeciesChanged,
                                        onCreateTreeSectionDirectlyClick = onCreateTreeSectionDirectlyClick,
                                        onNewCaptureClick = { sampleType ->
                                            onStartWizard(sampleType, null)
                                        },
                                        onSyncNowClick = onSyncNowClick
                                    )
                                }
                            }
                        }

                        EcoVerdeMainDestination.ANALIZAR -> {
                            // Módulos 5 y 6: Asistente Lineal (Wizard) y Resultados Bifurcados
                            CaptureWizardSection(
                                uiState = uiState,
                                onStepSelected = onSetWizardStep,
                                onSelectSampleType = onSelectSampleType,
                                onAssociationModeChanged = onAssociationModeChanged,
                                onSelectExistingTree = onSelectExistingTree,
                                onNewTreeNameChanged = onNewTreeNameChanged,
                                onNewTreeSpeciesChanged = onNewTreeSpeciesChanged,
                                onCreateTreeSectionDirectlyClick = onCreateTreeSectionDirectlyClick,
                                onSelectExistingZone = onSelectExistingZone,
                                onNewZoneNameChanged = onNewZoneNameChanged,
                                onNewZoneRadiusChanged = onNewZoneRadiusChanged,
                                onSearchQueryChanged = onSearchQueryTreeChanged,
                                onLaunchCameraClick = triggerCameraWithMetadata,
                                onLaunchGalleryClick = triggerGalleryWithMetadata,
                                onRunSyntheticSample = onRunSyntheticSample,
                                onUpdateCroppedBitmap = onUpdateCroppedBitmap,
                                onRestoreOriginalBitmap = onRestoreOriginalBitmap,
                                onRotateBitmap = onRotateBitmap,
                                onProcessCurrentSampleClick = {
                                    uiState.originalBitmap?.let { onAnalyzeBitmap(it) }
                                },
                                onSelectTab = onSelectTab,
                                onCategoryFilterSelected = onCategoryFilterSelected,
                                onAlphaChanged = onAlphaChanged,
                                onObservationsChanged = onObservationsChanged,
                                onSaveAndFinishClick = onSaveToRoomClick,
                                onRepeatCaptureClick = onRepeatCapture,
                                onCancelWizardClick = {
                                    onSelectDestination(EcoVerdeMainDestination.INICIO)
                                }
                            )
                        }

                        EcoVerdeMainDestination.MAPA -> {
                            // Módulo 4: Mapa Local de Árboles y Focos
                            LocalForestMapSection(
                                uiState = uiState,
                                onRefreshDeviceLocation = onRefreshDeviceLocation,
                                onSimulateCoordinatesClick = onSimulateCoordinatesClick,
                                onTreeMarkerClicked = onMapTreeSelected
                            )
                        }

                        EcoVerdeMainDestination.ALERTAS -> {
                            // Módulo 7: Centro de Alertas y Sincronización
                            AlertsAndSyncSection(
                                uiState = uiState,
                                onSyncNowClick = onSyncNowClick,
                                onHistoryFilterChanged = onHistoryFilterChanged,
                                onInspectRecordClick = onInspectRecordClick,
                                onCenterMapOnCoordinatesClick = { lat, lon ->
                                    onSelectDestination(EcoVerdeMainDestination.MAPA)
                                    onSimulateCoordinatesClick(lat, lon)
                                }
                            )
                        }

                        EcoVerdeMainDestination.PERFIL -> {
                            // Módulo 8: Perfil y Ajustes Técnicos
                            BrigadistaProfileSection(
                                session = session,
                                uiState = uiState,
                                onSyncNowClick = onSyncNowClick,
                                onClearLocalDatabaseClick = onClearLocalDatabaseClick,
                                onLogoutClick = onLogoutClick
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Módulo 2: Hub Principal / Dashboard de la Jornada.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun DashboardJornadaSection(
    session: BrigadistaSession,
    uiState: AnalysisResultUiState,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onHomeTreeFilterChanged: (TreeHealthFilter) -> Unit,
    onOpenTreeDetailClick: (String) -> Unit,
    onNewTreeNameChanged: (String) -> Unit,
    onNewTreeSpeciesChanged: (String) -> Unit,
    onCreateTreeSectionDirectlyClick: () -> Unit,
    onNewCaptureClick: (SampleType) -> Unit,
    onSyncNowClick: () -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // RESUMEN DE LA JORNADA (Card destacada superior - RN-30)
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "RESUMEN DE LA JORNADA",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = GreenPrimary
                        )
                        Text(
                            text = "Inspección diaria en campo",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (uiState.daySummary.pendientesSincronizacion > 0) {
                        Surface(
                            shape = CircleShape,
                            color = StatusUncertainAmber.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "${uiState.daySummary.pendientesSincronizacion} pendientes",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // 4 Métricas Horizontales estrictas del día (RN-30, RN-24, RN-26):
                // Capturas del día, Sanos (verde), Infestados (rojo), Sin determinar (amarillo)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    KpiMetricCard(
                        title = "Capturas",
                        value = "${uiState.daySummary.totalCapturasHoy}",
                        color = Color(0xFF1B5E20),
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = "Sanos",
                        value = "${uiState.daySummary.totalSanosHoy}",
                        color = StatusHealthyGreen,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = "Infestados",
                        value = "${uiState.daySummary.totalInfestadosHoy}",
                        color = StatusInfestedRed,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = "Sin determ.",
                        value = "${uiState.daySummary.totalNoDeterminadosHoy}",
                        color = StatusUncertainAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // LISTA DE TRABAJO DEL DÍA
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .animateContentSize(animationSpec = EcoVerdeMotion.standardSpatialSpring()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Lista de Trabajo del Día",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Pestañas o chips de filtro rápido: Todos, Sanos, Infestados, Pendientes de sincronizar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TreeHealthFilter.entries.forEach { filter ->
                        val isSelected = uiState.homeTreeFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onHomeTreeFilterChanged(filter)
                            },
                            shape = CircleShape,
                            label = { Text(filter.label) }
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Tarjetas de árboles inspeccionados hoy
                val trees = uiState.filteredTreeProgressions
                if (trees.isEmpty()) {
                    Text(
                        text = "No hay árboles que coincidan con el filtro seleccionado.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    trees.forEach { prog ->
                        val sharedCardModifier = with(sharedTransitionScope) {
                            Modifier.sharedBounds(
                                sharedContentState = rememberSharedContentState(
                                    key = "tree-section-${prog.tree.idArbol}"
                                ),
                                animatedVisibilityScope = animatedVisibilityScope,
                                boundsTransform = { _, _ -> EcoVerdeMotion.expressiveSpatialSpring() }
                            )
                        }
                        TreeProgressionRowCard(
                            progression = prog,
                            onClick = { onOpenTreeDetailClick(prog.tree.idArbol) },
                            modifier = sharedCardModifier
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta individual de árbol inspeccionado en la Lista de Trabajo:
 * - Nombre del árbol
 * - Especie
 * - Hora de la última captura
 * - Chip fitosanitario: Verde (Sano), Rojo (Infestado), Amarillo (Indeterminado)
 * - Chip de persistencia: Nube gris flecha arriba (Pendiente), nube verde check (Sincronizado), exclamación roja (Error)
 */
@Composable
private fun TreeProgressionRowCard(
    progression: TreeProgressionSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (statusBg, statusFg) = when (progression.currentStatus) {
        ClassificationOutcome.INFESTADO -> StatusInfestedRed.copy(alpha = 0.15f) to StatusInfestedRed
        ClassificationOutcome.SANO -> StatusHealthyGreen.copy(alpha = 0.15f) to StatusHealthyGreen
        ClassificationOutcome.NO_DETERMINADO -> StatusUncertainAmber.copy(alpha = 0.18f) to Color(0xFFE65100)
    }

    val (syncIcon, syncColor) = when (progression.syncStatus) {
        SyncStatus.SINCRONIZADO -> Icons.Default.CloudDone to StatusHealthyGreen
        SyncStatus.PENDIENTE -> Icons.Default.CloudUpload to Color(0xFFE65100)
        SyncStatus.ERROR -> Icons.Default.Error to StatusInfestedRed
    }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = progression.tree.nombreEspecifico,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${progression.tree.especie} • ${progression.lastCaptureFormattedTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Dos chips de estado: Fitosanitario y Persistencia
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Chip 1: Estado fitosanitario
                    Surface(
                        shape = CircleShape,
                        color = statusBg
                    ) {
                        Text(
                            text = progression.statusBadgeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusFg,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    // Chip 2: Estado de persistencia
                    Surface(
                        shape = CircleShape,
                        color = syncColor.copy(alpha = 0.12f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = syncIcon,
                                contentDescription = null,
                                tint = syncColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = progression.syncStatus.displayLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = syncColor
                            )
                        }
                    }
                }
            }

            Text(
                text = "›",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

/**
 * Módulo 3: Detalle de Árbol (Maestro-Detalle).
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun TreeDetailProgressionSection(
    progression: TreeProgressionSummary,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBackToTreesClick: () -> Unit,
    onNewCaptureForTreeClick: () -> Unit,
    onCenterMapOnTreeClick: (Double, Double) -> Unit,
    onSaveTreeObservationsClick: (String, String) -> Unit = { _, _ -> },
    fieldObservations: String,
    onObservationsChanged: (String) -> Unit,
    onInspectCaptureClick: (SavedDetectionRecord) -> Unit,
    onDeleteTreeClick: (String) -> Unit = {}
) {
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "¿Eliminar árbol?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Se eliminará permanentemente '${progression.tree.nombreEspecifico}' y todas las capturas o análisis asociados a este ejemplar."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteTreeClick(progression.tree.idArbol)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusInfestedRed)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    val (statusColor, containerBg) = when (progression.currentStatus) {
        ClassificationOutcome.INFESTADO -> StatusInfestedRed to Color(0xFFFFEBEE)
        ClassificationOutcome.SANO -> StatusHealthyGreen to Color(0xFFE8F5E9)
        ClassificationOutcome.NO_DETERMINADO -> StatusUncertainAmber to Color(0xFFFFF8E1)
    }

    val treeLat = progression.tree.latitud ?: 19.4218
    val treeLon = progression.tree.longitud ?: -99.1820

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Barra superior
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                OutlinedButton(
                    onClick = onBackToTreesClick,
                    shape = CircleShape
                ) {
                    Text("← Lista de trabajo")
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = progression.tree.nombreEspecifico,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Detalle del ejemplar",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(
                onClick = { showDeleteConfirmDialog = true }
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar árbol",
                    tint = StatusInfestedRed
                )
            }
        }

        // 1. TARJETA DE ESTADO ACTUAL GRANDE (Módulo 3)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = containerBg),
            border = BorderStroke(1.5.dp, statusColor.copy(alpha = 0.4f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ESTADO ACTUAL",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = statusColor
                        )
                        Text(
                            text = progression.statusBadgeLabel.uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = statusColor
                        )
                        Text(
                            text = "Última captura: ${progression.lastCaptureFormattedTime}",
                            style = MaterialTheme.typography.bodySmall,
                            color = statusColor.copy(alpha = 0.9f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(statusColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (progression.currentStatus) {
                                ClassificationOutcome.SANO -> "✓"
                                ClassificationOutcome.INFESTADO -> "!"
                                else -> "?"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }

                // Botón directo: Nueva captura en este árbol
                Button(
                    onClick = onNewCaptureForTreeClick,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Nueva captura en este árbol", fontWeight = FontWeight.Bold)
                }
            }
        }

        // 2. FICHA TÉCNICA (Módulo 3)
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
                    text = "Ficha Técnica del Árbol",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                ProfileFieldRow("Especie validada", progression.tree.especie)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Coordenadas GPS",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format("%.5f, %.5f", treeLat, treeLon),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    OutlinedButton(
                        onClick = { onCenterMapOnTreeClick(treeLat, treeLon) },
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ver en mapa", style = MaterialTheme.typography.labelSmall)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Campo editable de Observaciones de campo
                Text(
                    text = "Observaciones de campo",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = fieldObservations,
                    onValueChange = onObservationsChanged,
                    placeholder = { Text("Añadir observaciones sobre el tronco, resina o follaje...") },
                    shape = MaterialTheme.shapes.medium,
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { onSaveTreeObservationsClick(progression.tree.idArbol, fieldObservations) },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Guardar observaciones", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // 3. HISTORIAL DE LA JORNADA PARA ESTE EJEMPLAR (Módulo 3)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Historial de la jornada para este ejemplar (${progression.captures.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (progression.captures.isEmpty()) {
                    Text(
                        text = "Aún no se han tomado fotografías hoy a este árbol.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    progression.captures.forEach { rec ->
                        TreeCaptureHistoryCardRow(
                            record = rec,
                            onClick = { onInspectCaptureClick(rec) }
                        )
                    }
                }
            }
        }

        // 4. ELIMINAR CATEGORÍA / ÁRBOL
        OutlinedButton(
            onClick = { showDeleteConfirmDialog = true },
            shape = CircleShape,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = StatusInfestedRed
            ),
            border = BorderStroke(1.dp, StatusInfestedRed.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Eliminar esta categoría / árbol y sus registros", fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * Fila del historial cronológico de fotos del árbol:
 * - Miniatura de la foto
 * - Hora exacta
 * - Porcentaje de confianza EfficientNet-B3
 * - Estado de sincronización con la nube
 */
@Composable
private fun TreeCaptureHistoryCardRow(
    record: SavedDetectionRecord,
    onClick: () -> Unit
) {
    val statusColor = when (record.classificationOutcome) {
        ClassificationOutcome.INFESTADO -> StatusInfestedRed
        ClassificationOutcome.SANO -> StatusHealthyGreen
        ClassificationOutcome.NO_DETERMINADO -> StatusUncertainAmber
    }

    val (syncIcon, syncColor) = when (record.syncStatus) {
        SyncStatus.SINCRONIZADO -> Icons.Default.CloudDone to StatusHealthyGreen
        SyncStatus.PENDIENTE -> Icons.Default.CloudUpload to Color(0xFFE65100)
        SyncStatus.ERROR -> Icons.Default.Error to StatusInfestedRed
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Miniatura visual
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(statusColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = "Hora: ${record.fechaCapturaIso.substringAfter("T", "").take(5)} hrs • ${record.classificationOutcome.displayTitle}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${(record.confidence * 100).toInt()}% de confianza",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = syncColor.copy(alpha = 0.12f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = syncIcon,
                        contentDescription = null,
                        tint = syncColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = record.syncStatus.displayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = syncColor
                    )
                }
            }
        }
    }
}

/**
 * Módulo 4: Mapa Local de Árboles y Focos.
 */
@Composable
private fun LocalForestMapSection(
    uiState: AnalysisResultUiState,
    onRefreshDeviceLocation: () -> Unit,
    onSimulateCoordinatesClick: (Double, Double) -> Unit,
    onTreeMarkerClicked: (TreeRecord) -> Unit
) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Mapa Local de Árboles y Focos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GreenPrimary
                    )
                    Text(
                        text = "Cartografía local sin conexión",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = onRefreshDeviceLocation,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mi GPS", style = MaterialTheme.typography.labelSmall)
                }
            }

            // Visor geoespacial MapLibre
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .clip(MaterialTheme.shapes.medium)
            ) {
                OfflineForestMapLibreView(
                    currentLat = uiState.captureLatitude,
                    currentLon = uiState.captureLongitude,
                    trees = uiState.treeProgressionList.map { it.tree },
                    records = uiState.allRecords,
                    hotspots = uiState.activeHotspots,
                    onMapCoordinateClicked = onSimulateCoordinatesClick,
                    modifier = Modifier.fillMaxSize()
                )

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    shape = MaterialTheme.shapes.small,
                    color = Color.Black.copy(alpha = 0.75f)
                ) {
                    Text(
                        text = "Árboles: ${uiState.treeProgressionList.size} • Focos activos: ${uiState.activeHotspots.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text(
                text = "Catálogo de Árboles (Toca para ver ficha o centrar):",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            uiState.treeProgressionList.take(6).forEach { prog ->
                val tree = prog.tree
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { onTreeMarkerClicked(tree) }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = tree.nombreEspecifico,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${tree.especie} • ${prog.statusBadgeLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Ver ›",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Módulo 8: Perfil y Ajustes Técnicos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrigadistaProfileSection(
    session: BrigadistaSession,
    uiState: AnalysisResultUiState,
    onSyncNowClick: () -> Unit,
    onClearLocalDatabaseClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val realAndroidId = DeviceIdentifierProvider.getRealAndroidId(context)
    val hardwareUuid = DeviceIdentifierProvider.getHardwareDeviceUuid(context)
    val deviceName = DeviceIdentifierProvider.getDeviceModelName()
    var showClearDbBottomSheet by remember { mutableStateOf(false) }

    if (showClearDbBottomSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showClearDbBottomSheet = false },
            sheetState = sheetState,
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "¿Borrar datos locales?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = StatusInfestedRed
                )
                Text(
                    text = "Se eliminarán permanentemente del dispositivo todos los registros y capturas locales.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showClearDbBottomSheet = false },
                        shape = CircleShape,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = {
                            showClearDbBottomSheet = false
                            onClearLocalDatabaseClick()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusInfestedRed),
                        shape = CircleShape,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Borrar todo")
                    }
                }
            }
        }
    }

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
                text = "Datos del Técnico",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = GreenPrimary
            )

            ProfileFieldRow("Nombre completo", session.nombreCompleto)
            ProfileFieldRow("Correo institucional", session.correo)
            ProfileFieldRow("Rol asignado", "Experto de campo")
            ProfileFieldRow("ID del dispositivo", session.idDispositivo.take(16))

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text(
                text = "Resumen Operativo del Dispositivo",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            ProfileFieldRow("Total de capturas acumuladas", "${uiState.allRecords.size}")
            ProfileFieldRow("Árboles registrados", "${uiState.treeProgressionList.size}")
            ProfileFieldRow("Pendientes de sincronización", "${uiState.daySummary.pendientesSincronizacion}")

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text(
                text = "Mantenimiento Local",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            ProfileFieldRow("Motor de diagnóstico", "Calibrado y activo")
            ProfileFieldRow("Modo de operación", "Local sin conexión")

            OutlinedButton(
                onClick = { showClearDbBottomSheet = true },
                shape = CircleShape,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusInfestedRed),
                border = BorderStroke(1.dp, StatusInfestedRed),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Borrar datos locales")
            }

            Button(
                onClick = onLogoutClick,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = StatusInfestedRed),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cerrar sesión", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun KpiMetricCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = color.copy(alpha = 0.12f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProfileFieldRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun calculateHaversineDistanceMeters(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double
): Double {
    val r = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
        sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}
