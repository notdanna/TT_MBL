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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.TopAppBar
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
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.zIndex
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
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
import com.descortezadores.ecoverde.presentation.features.about.TermsAndConditionsScreen
import com.descortezadores.ecoverde.presentation.features.analysis.components.AlertsAndSyncSection
import com.descortezadores.ecoverde.presentation.features.analysis.components.CaptureWizardSection
import com.descortezadores.ecoverde.presentation.features.analysis.components.OfflineForestMapLibreView
import com.descortezadores.ecoverde.presentation.features.analysis.components.prewarmOfflineMapStyle
import com.descortezadores.ecoverde.presentation.features.analysis.components.parseHexColor
import com.descortezadores.ecoverde.presentation.theme.ChromaticFoliageDamaged
import com.descortezadores.ecoverde.presentation.theme.ChromaticFoliageHealthy
import com.descortezadores.ecoverde.presentation.theme.ChromaticFoliageRegular
import com.descortezadores.ecoverde.presentation.theme.EcoVerdeMotion
import com.descortezadores.ecoverde.presentation.theme.GreenPrimary
import com.descortezadores.ecoverde.presentation.theme.StatusHealthyGreen
import com.descortezadores.ecoverde.presentation.theme.StatusInfestedRed
import com.descortezadores.ecoverde.presentation.theme.StatusUncertainAmber
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
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
    onSaveTreeObservationsClick: (String, String) -> Unit = { _, _ -> },
    onHomeWorkTabChanged: (HomeWorkTab) -> Unit = {},
    onOpenZoneDetailClick: (String) -> Unit = {},
    onCloseZoneDetailClick: () -> Unit = {},
    onStartNewCaptureForZoneClick: (String) -> Unit = {},
    onDeleteZoneClick: (String) -> Unit = {},
    onSaveZoneObservationsClick: (String, String) -> Unit = { _, _ -> },
    onFocusTreeOnMap: (TreeRecord) -> Unit = {},
    onFocusCoordinatesOnMap: (Double, Double) -> Unit = { _, _ -> },
    onCenterOnUserGps: () -> Unit = {}
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    var showTermsScreen by remember { mutableStateOf(false) }

    if (showTermsScreen) {
        TermsAndConditionsScreen(
            onBackClick = { showTermsScreen = false }
        )
        return
    }

    val canHandlePredictiveBack = uiState.selectedTreeProgression != null ||
        uiState.selectedZoneProgression != null ||
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
            } else if (uiState.selectedZoneProgression != null &&
                uiState.currentDestination == EcoVerdeMainDestination.INICIO
            ) {
                onCloseZoneDetailClick()
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
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        if (result.resultCode == android.app.Activity.RESULT_OK && uri != null) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            onAnalyzeGalleryUri(uri)
        }
    }

    val launchGalleryPicker = {
        val intent = android.content.Intent(
            android.content.Intent.ACTION_PICK,
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        ).apply {
            type = "image/*"
        }
        galleryLauncher.launch(intent)
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
        launchGalleryPicker()
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
        launch {
            prewarmOfflineMapStyle(context)
        }
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
        val needsLocationOrMediaPerm = !ImageMetadataAndLocationHelper.hasLocationPermission(context) ||
            !ImageMetadataAndLocationHelper.hasMediaLocationPermission(context)
        if (needsLocationOrMediaPerm) {
            val perms = buildList {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.READ_MEDIA_IMAGES)
                } else {
                    add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    add(Manifest.permission.ACCESS_MEDIA_LOCATION)
                }
            }.toTypedArray()
            galleryAndLocationPermissionsLauncher.launch(perms)
        } else {
            launchGalleryPicker()
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onFocusTreeOnMap(mapTree)
                            onDismissMapBottomSheet()
                        },
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Ir en mapa",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            onDismissMapBottomSheet()
                            onOpenTreeDetailClick(mapTree.idArbol)
                        },
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Text(
                            text = "Ver Detalle",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    Button(
                        onClick = {
                            onDismissMapBottomSheet()
                            onStartNewCaptureForTreeClick(mapTree.idArbol)
                            onStartWizard(SampleType.CORTEZA, mapTree.idArbol)
                        },
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        modifier = Modifier
                            .weight(1.15f)
                            .height(42.dp)
                    ) {
                        Text(
                            text = "+ Nueva Captura",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
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
            TopAppBar(
                scrollBehavior = scrollBehavior,
                windowInsets = TopAppBarDefaults.windowInsets,
                navigationIcon = {
                    if (uiState.currentDestination == EcoVerdeMainDestination.ANALIZAR) {
                        IconButton(onClick = {
                            if (uiState.isInspectingHistoricalRecord) {
                                onSelectDestination(EcoVerdeMainDestination.ALERTAS)
                            } else {
                                onSelectDestination(EcoVerdeMainDestination.INICIO)
                            }
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver al Dashboard",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                title = {
                    Column(horizontalAlignment = Alignment.Start) {
                        if (uiState.currentDestination == EcoVerdeMainDestination.ANALIZAR) {
                            Text(
                                text = "Asistente de Captura",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Paso ${uiState.wizardStep.stepNumber}: ${uiState.wizardStep.title}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            // Cabecera Contextual: Saludo al biólogo (alineado a la izquierda)
                            val firstName = session.nombreCompleto.substringBefore(" ").ifBlank { "Biólogo" }
                            Text(
                                text = "Hola, $firstName",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "EcoVerde • Bosque de Chapultepec",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Campana de alertas con badge numérico
                    val alertCount = uiState.activeHotspots.size + uiState.nearbyInfestedTrees.size + uiState.daySummary.pendientesSincronizacion
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
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Indicador de estado de sincronización global (AWS / Cloud)
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
                                GreenPrimary
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
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
                val destinations = remember {
                    listOf(
                        Triple(EcoVerdeMainDestination.INICIO, "Inicio", Icons.Default.Forest),
                        Triple(EcoVerdeMainDestination.MAPA, "Mapa", Icons.Default.Map),
                        Triple(EcoVerdeMainDestination.ALERTAS, "Alertas", Icons.Default.Notifications),
                        Triple(EcoVerdeMainDestination.PERFIL, "Perfil", Icons.Default.Person)
                    )
                }

                destinations.forEach { (dest, label, icon) ->
                    val selected = uiState.currentDestination == dest
                    val badgeCount = when (dest) {
                        EcoVerdeMainDestination.ALERTAS -> uiState.daySummary.pendientesSincronizacion + uiState.activeHotspots.size + uiState.nearbyInfestedTrees.size
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
        ) {
            val hasBanners = (uiState.statusBannerMessage != null && !uiState.statusBannerMessage.contains("Análisis completado", ignoreCase = true)) ||
                uiState.errorMessage != null

            if (hasBanners) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                }
            }

            val saveableStateHolder = rememberSaveableStateHolder()
            val isMapDestination = uiState.currentDestination == EcoVerdeMainDestination.MAPA
            var hasVisitedMap by remember { mutableStateOf(isMapDestination) }
            LaunchedEffect(isMapDestination) {
                if (isMapDestination) {
                    hasVisitedMap = true
                }
            }
            val mapOverlayAlpha by animateFloatAsState(
                targetValue = if (isMapDestination) 1f else 0f,
                animationSpec = tween(
                    durationMillis = if (isMapDestination) 180 else 120,
                    easing = EcoVerdeMotion.LinearOutSlowInEasing
                ),
                label = "mapTabAlpha"
            )

            // Contenedor de destinos con preservación de estado (estilo Mihon) y mapa persistente en memoria
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (hasVisitedMap) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(if (isMapDestination) 1f else 0f)
                            .graphicsLayer { alpha = mapOverlayAlpha }
                    ) {
                        LocalForestMapSection(
                            uiState = uiState,
                            isVisible = isMapDestination || mapOverlayAlpha > 0.01f,
                            onRefreshDeviceLocation = onRefreshDeviceLocation,
                            onSimulateCoordinatesClick = onSimulateCoordinatesClick,
                            onTreeMarkerClicked = onMapTreeSelected,
                            onFocusTreeOnMap = onFocusTreeOnMap,
                            onCenterOnUserGps = onCenterOnUserGps
                        )
                    }
                }

                AnimatedContent(
                    targetState = if (isMapDestination) null else uiState.currentDestination,
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(if (isMapDestination) 0f else 1f),
                    transitionSpec = {
                        if (targetState == EcoVerdeMainDestination.ANALIZAR) {
                            EcoVerdeMotion.modalOpenTransform()
                        } else if (initialState == EcoVerdeMainDestination.ANALIZAR) {
                            EcoVerdeMotion.modalCloseTransform()
                        } else {
                            // Transición Fade Through estilo Mihon: limpia, sin SizeTransform ni saltos
                            EcoVerdeMotion.navDestinationTransform()
                        }
                    },
                    contentKey = { it?.name ?: "MAPA_HIDDEN" },
                    label = "MainDestinationAnimatedContent"
                ) { destination ->
                    if (destination == null) {
                        Spacer(modifier = Modifier.fillMaxSize())
                    } else {
                        saveableStateHolder.SaveableStateProvider(key = destination.name) {
                            when (destination) {
                                EcoVerdeMainDestination.INICIO -> {
                                    AnimatedContent(
                                        targetState = Pair(uiState.selectedTreeProgression, uiState.selectedZoneProgression),
                                        modifier = Modifier.fillMaxSize(),
                                        transitionSpec = {
                                            val isOpeningDetail = (initialState.first == null && initialState.second == null) &&
                                                (targetState.first != null || targetState.second != null)
                                            val isClosingDetail = (initialState.first != null || initialState.second != null) &&
                                                (targetState.first == null && targetState.second == null)

                                            if (isOpeningDetail) {
                                                EcoVerdeMotion.detailOpenTransform()
                                            } else if (isClosingDetail) {
                                                EcoVerdeMotion.detailCloseTransform()
                                            } else {
                                                EcoVerdeMotion.cleanCrossfade()
                                            }
                                        },
                                        contentKey = { (tree, zone) ->
                                            when {
                                                tree != null -> "tree_${tree.tree.idArbol}"
                                                zone != null -> "zone_${zone.zone.idZona}"
                                                else -> "dashboard"
                                            }
                                        },
                                        label = "ProgressionDetailAnimatedContent"
                                    ) { (activeTreeProgression, activeZoneProgression) ->
                                        if (activeTreeProgression != null) {
                                            // Módulo 3: Detalle de Árbol
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .verticalScroll(rememberScrollState())
                                                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp)
                                            ) {
                                                TreeDetailProgressionSection(
                                                    progression = activeTreeProgression,
                                                    onBackToTreesClick = onCloseTreeDetailClick,
                                                    onNewCaptureForTreeClick = {
                                                        onStartNewCaptureForTreeClick(activeTreeProgression.tree.idArbol)
                                                        onStartWizard(SampleType.CORTEZA, activeTreeProgression.tree.idArbol)
                                                        triggerCameraWithMetadata()
                                                    },
                                                    onCenterMapOnTreeClick = { _, _ ->
                                                        onFocusTreeOnMap(activeTreeProgression.tree)
                                                    },
                                                    onSaveTreeObservationsClick = onSaveTreeObservationsClick,
                                                    fieldObservations = uiState.fieldObservations,
                                                    onObservationsChanged = onObservationsChanged,
                                                    onInspectCaptureClick = onInspectRecordClick,
                                                    onDeleteTreeClick = onDeleteTreeClick
                                                )
                                            }
                                        } else if (activeZoneProgression != null) {
                                            // Detalle de Zona de Follaje (Dosel)
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .verticalScroll(rememberScrollState())
                                                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp)
                                            ) {
                                                ZoneDetailProgressionSection(
                                                    progression = activeZoneProgression,
                                                    onBackToZonesClick = onCloseZoneDetailClick,
                                                    onNewCaptureForZoneClick = {
                                                        onStartNewCaptureForZoneClick(activeZoneProgression.zone.idZona)
                                                        onStartWizard(SampleType.FOLLAJE, null)
                                                        triggerCameraWithMetadata()
                                                    },
                                                    onCenterMapOnZoneClick = { lat, lon ->
                                                        onFocusCoordinatesOnMap(lat, lon)
                                                    },
                                                    onSaveZoneObservationsClick = onSaveZoneObservationsClick,
                                                    fieldObservations = uiState.fieldObservations,
                                                    onObservationsChanged = onObservationsChanged,
                                                    onInspectCaptureClick = onInspectRecordClick,
                                                    onDeleteZoneClick = onDeleteZoneClick
                                                )
                                            }
                                        } else {
                                            saveableStateHolder.SaveableStateProvider(key = "INICIO_DASHBOARD") {
                                                // Módulo 2: Hub Principal / Dashboard de la Jornada (LazyColumn virtualizada a 120Hz)
                                                DashboardJornadaSection(
                                                    session = session,
                                                    uiState = uiState,
                                                    onHomeWorkTabChanged = onHomeWorkTabChanged,
                                                    onHomeTreeFilterChanged = onHomeTreeFilterChanged,
                                                    onOpenTreeDetailClick = onOpenTreeDetailClick,
                                                    onOpenZoneDetailClick = onOpenZoneDetailClick,
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
                                }

                                EcoVerdeMainDestination.ANALIZAR -> {
                                    // Módulos 5 y 6: Asistente Lineal (Wizard) y Resultados Bifurcados
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState())
                                            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp)
                                    ) {
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
                                }

                                EcoVerdeMainDestination.MAPA -> Unit

                                EcoVerdeMainDestination.ALERTAS -> {
                                    // Módulo 7: Centro de Alertas y Sincronización (LazyColumn virtualizada)
                                    AlertsAndSyncSection(
                                        uiState = uiState,
                                        onSyncNowClick = onSyncNowClick,
                                        onHistoryFilterChanged = onHistoryFilterChanged,
                                        onInspectRecordClick = onInspectRecordClick,
                                        onCenterMapOnCoordinatesClick = { lat, lon ->
                                            onFocusCoordinatesOnMap(lat, lon)
                                        },
                                        onFocusTreeOnMap = onFocusTreeOnMap
                                    )
                                }

                                EcoVerdeMainDestination.PERFIL -> {
                                    // Módulo 8: Perfil y Ajustes Técnicos
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState())
                                            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp)
                                    ) {
                                        BrigadistaProfileSection(
                                            session = session,
                                            uiState = uiState,
                                            onSyncNowClick = onSyncNowClick,
                                            onClearLocalDatabaseClick = onClearLocalDatabaseClick,
                                            onLogoutClick = onLogoutClick,
                                            onOpenTermsClick = { showTermsScreen = true }
                                        )
                                    }
                                }
                            }
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardJornadaSection(
    session: BrigadistaSession,
    uiState: AnalysisResultUiState,
    onHomeWorkTabChanged: (HomeWorkTab) -> Unit,
    onHomeTreeFilterChanged: (TreeHealthFilter) -> Unit,
    onOpenTreeDetailClick: (String) -> Unit,
    onOpenZoneDetailClick: (String) -> Unit,
    onNewTreeNameChanged: (String) -> Unit,
    onNewTreeSpeciesChanged: (String) -> Unit,
    onCreateTreeSectionDirectlyClick: () -> Unit,
    onNewCaptureClick: (SampleType) -> Unit,
    onSyncNowClick: () -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    val surfaceContainerColor = MaterialTheme.colorScheme.surfaceContainer
    val outlineVariantColor = MaterialTheme.colorScheme.outlineVariant
    val filterRowScrollState = rememberScrollState()

    val trees = remember(uiState.treeProgressionList, uiState.homeTreeFilter) {
        uiState.filteredTreeProgressions
    }
    val zones = remember(uiState.zoneProgressionList, uiState.homeTreeFilter) {
        uiState.filteredZoneProgressions
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp)
    ) {
        // RESUMEN DE LA JORNADA (Card destacada superior - RN-30)
        item(key = "resumen_jornada", contentType = "summary") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = surfaceContainerColor),
                border = BorderStroke(1.dp, outlineVariantColor),
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
        }

        // LISTA DE TRABAJO DEL DÍA - Cabecera superior de la tarjeta contenedora
        item(key = "lista_trabajo_header", contentType = "work_header") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(surfaceContainerColor)
                    .drawBehind {
                        val stroke = 1.dp.toPx()
                        // Top, left and right borders (bottom left open for seamless items below)
                        drawLine(outlineVariantColor, Offset(0f, stroke / 2f), Offset(size.width, stroke / 2f), stroke)
                        drawLine(outlineVariantColor, Offset(stroke / 2f, 0f), Offset(stroke / 2f, size.height), stroke)
                        drawLine(outlineVariantColor, Offset(size.width - stroke / 2f, 0f), Offset(size.width - stroke / 2f, size.height), stroke)
                    }
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Lista de Trabajo del Día",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Alternador de Pestaña: Árboles (Corteza) vs Zonas de Follaje (Dosel)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        HomeWorkTab.entries.forEachIndexed { index, tab ->
                            val selected = uiState.homeWorkTab == tab
                            SegmentedButton(
                                selected = selected,
                                onClick = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onHomeWorkTabChanged(tab)
                                },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = HomeWorkTab.entries.size
                                ),
                                label = {
                                    Text(
                                        text = tab.label,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }

                    // Pestañas o chips de filtro rápido: Todos, Sanos, Infestados, Pendientes de sincronizar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(filterRowScrollState),
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

                    HorizontalDivider(color = outlineVariantColor)
                }
            }
        }

        if (uiState.homeWorkTab == HomeWorkTab.ARBOLES) {
            if (trees.isEmpty()) {
                item(key = "trees_empty", contentType = "empty_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(surfaceContainerColor)
                            .drawBehind {
                                val stroke = 1.dp.toPx()
                                drawLine(outlineVariantColor, Offset(stroke / 2f, 0f), Offset(stroke / 2f, size.height), stroke)
                                drawLine(outlineVariantColor, Offset(size.width - stroke / 2f, 0f), Offset(size.width - stroke / 2f, size.height), stroke)
                            }
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "No hay árboles que coincidan con el filtro seleccionado.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(
                    items = trees,
                    key = { "tree-${it.tree.idArbol}" },
                    contentType = { "tree_row" }
                ) { prog ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(surfaceContainerColor)
                            .drawBehind {
                                val stroke = 1.dp.toPx()
                                drawLine(outlineVariantColor, Offset(stroke / 2f, 0f), Offset(stroke / 2f, size.height), stroke)
                                drawLine(outlineVariantColor, Offset(size.width - stroke / 2f, 0f), Offset(size.width - stroke / 2f, size.height), stroke)
                            }
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        TreeProgressionRowCard(
                            progression = prog,
                            onClick = { onOpenTreeDetailClick(prog.tree.idArbol) }
                        )
                    }
                }
            }
        } else {
            if (zones.isEmpty()) {
                item(key = "zones_empty", contentType = "empty_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(surfaceContainerColor)
                            .drawBehind {
                                val stroke = 1.dp.toPx()
                                drawLine(outlineVariantColor, Offset(stroke / 2f, 0f), Offset(stroke / 2f, size.height), stroke)
                                drawLine(outlineVariantColor, Offset(size.width - stroke / 2f, 0f), Offset(size.width - stroke / 2f, size.height), stroke)
                            }
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "No hay zonas de follaje que coincidan con el filtro. Utiliza el botón '+' para registrar una nueva toma aérea o de dosel.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(
                    items = zones,
                    key = { "zone-${it.zone.idZona}" },
                    contentType = { "zone_row" }
                ) { prog ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(surfaceContainerColor)
                            .drawBehind {
                                val stroke = 1.dp.toPx()
                                drawLine(outlineVariantColor, Offset(stroke / 2f, 0f), Offset(stroke / 2f, size.height), stroke)
                                drawLine(outlineVariantColor, Offset(size.width - stroke / 2f, 0f), Offset(size.width - stroke / 2f, size.height), stroke)
                            }
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        ZoneProgressionRowCard(
                            progression = prog,
                            onClick = { onOpenZoneDetailClick(prog.zone.idZona) }
                        )
                    }
                }
            }
        }

        // Pie redondeado de la tarjeta contenedora de Lista de Trabajo
        item(key = "lista_trabajo_footer", contentType = "work_footer") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                    .background(surfaceContainerColor)
                    .drawBehind {
                        val stroke = 1.dp.toPx()
                        drawLine(outlineVariantColor, Offset(0f, size.height - stroke / 2f), Offset(size.width, size.height - stroke / 2f), stroke)
                        drawLine(outlineVariantColor, Offset(stroke / 2f, 0f), Offset(stroke / 2f, size.height), stroke)
                        drawLine(outlineVariantColor, Offset(size.width - stroke / 2f, 0f), Offset(size.width - stroke / 2f, size.height), stroke)
                    }
            )
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
                    text = progression.lastCaptureFormattedTime,
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
@Composable
private fun TreeDetailProgressionSection(
    progression: TreeProgressionSummary,
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
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
 * Tarjeta individual de Zona de Follaje en la Lista de Trabajo:
 * - Nombre de la zona
 * - Radio y hora de última captura
 * - Chip fitosanitario según balance cromático de dosel
 * - Chip de sincronización
 */
@Composable
private fun ZoneProgressionRowCard(
    progression: FoliageZoneProgressionSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (statusBg, statusFg) = when {
        progression.avgPctDanado >= 35 -> StatusInfestedRed.copy(alpha = 0.15f) to StatusInfestedRed
        progression.avgPctRegular >= 30 -> StatusUncertainAmber.copy(alpha = 0.18f) to Color(0xFFE65100)
        else -> StatusHealthyGreen.copy(alpha = 0.15f) to StatusHealthyGreen
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
                    text = progression.zone.nombreZona,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                val tomasLabel = if (progression.totalCapturas == 1) "1 toma" else "${progression.totalCapturas} tomas"
                Text(
                    text = "${progression.lastCaptureFormattedTime} • $tomasLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (progression.totalCapturas > 0) {
                    Text(
                        text = "Dosel: ${progression.avgPctSano}% sano • ${progression.avgPctRegular}% estrés • ${progression.avgPctDanado}% daño",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
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
    }
}

/**
 * Detalle independiente de una Zona de Follaje / Polígono de sobrevuelo (Dosel).
 */
@Composable
private fun ZoneDetailProgressionSection(
    progression: FoliageZoneProgressionSummary,
    onBackToZonesClick: () -> Unit,
    onNewCaptureForZoneClick: () -> Unit,
    onCenterMapOnZoneClick: (Double, Double) -> Unit,
    onSaveZoneObservationsClick: (String, String) -> Unit = { _, _ -> },
    fieldObservations: String,
    onObservationsChanged: (String) -> Unit,
    onInspectCaptureClick: (SavedDetectionRecord) -> Unit,
    onDeleteZoneClick: (String) -> Unit = {}
) {
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "¿Eliminar zona de follaje?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Se eliminará permanentemente '${progression.zone.nombreZona}' y todas las tomas aéreas asociadas a esta zona."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteZoneClick(progression.zone.idZona)
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

    val (statusColor, containerBg) = when {
        progression.avgPctDanado >= 35 -> StatusInfestedRed to Color(0xFFFFEBEE)
        progression.avgPctRegular >= 30 -> StatusUncertainAmber to Color(0xFFFFF8E1)
        else -> StatusHealthyGreen to Color(0xFFE8F5E9)
    }

    val zoneLat = progression.zone.latitudCentro
    val zoneLon = progression.zone.longitudCentro

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
                    onClick = onBackToZonesClick,
                    shape = CircleShape
                ) {
                    Text("← Lista de trabajo")
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = progression.zone.nombreZona,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Zona de monitoreo de dosel (Follaje)",
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
                    contentDescription = "Eliminar zona",
                    tint = StatusInfestedRed
                )
            }
        }

        // 1. TARJETA DE ESTADO ACTUAL DE LA ZONA
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
                            text = "ESTADO DEL DOSEL",
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
                            text = "Última toma: ${progression.lastCaptureFormattedTime}",
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
                        Icon(
                            imageVector = Icons.Default.Yard,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                // Distribución cromática promedio
                if (progression.totalCapturas > 0) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Promedio de coloración foliar en la zona:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = ChromaticFoliageHealthy.copy(alpha = 0.2f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Sano", style = MaterialTheme.typography.labelSmall, color = ChromaticFoliageHealthy, fontWeight = FontWeight.Bold)
                                    Text("${progression.avgPctSano}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = ChromaticFoliageHealthy)
                                }
                            }
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = ChromaticFoliageRegular.copy(alpha = 0.25f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Estrés", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                    Text("${progression.avgPctRegular}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Color(0xFF2E7D32))
                                }
                            }
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = ChromaticFoliageDamaged.copy(alpha = 0.2f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Dañado", style = MaterialTheme.typography.labelSmall, color = ChromaticFoliageDamaged, fontWeight = FontWeight.Bold)
                                    Text("${progression.avgPctDanado}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = ChromaticFoliageDamaged)
                                }
                            }
                        }
                    }
                }

                // Botón directo: Nueva toma de follaje en esta zona
                Button(
                    onClick = onNewCaptureForZoneClick,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Nueva toma aérea / follaje en esta zona", fontWeight = FontWeight.Bold)
                }
            }
        }

        // 2. FICHA TÉCNICA DE LA ZONA
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
                    text = "Ficha Técnica de la Zona",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                ProfileFieldRow("Nombre", progression.zone.nombreZona)
                ProfileFieldRow("Coordenadas centrales", String.format("%.5f, %.5f", zoneLat, zoneLon))
                ProfileFieldRow("Fecha de alta", progression.zone.fechaRegistroIso.take(10))

                Button(
                    onClick = { onCenterMapOnZoneClick(zoneLat, zoneLon) },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ver ubicación en mapa")
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Text(
                    text = "Notas y Observaciones de la Zona:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = fieldObservations,
                    onValueChange = onObservationsChanged,
                    placeholder = { Text("Añadir observaciones sobre el dosel, condiciones de vuelo...") },
                    shape = MaterialTheme.shapes.medium,
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { onSaveZoneObservationsClick(progression.zone.idZona, fieldObservations) },
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

        // 3. HISTORIAL DE TOMAS AÉREAS / FOLLAJE PARA ESTA ZONA
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
                    text = "Historial de tomas para esta zona (${progression.captures.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (progression.captures.isEmpty()) {
                    Text(
                        text = "Aún no se han registrado tomas de follaje para esta zona.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    progression.captures.forEach { rec ->
                        ZoneCaptureHistoryCardRow(
                            record = rec,
                            onClick = { onInspectCaptureClick(rec) }
                        )
                    }
                }
            }
        }

        // 4. ELIMINAR ZONA
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
            Text("Eliminar esta zona de follaje y sus registros", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ZoneCaptureHistoryCardRow(
    record: SavedDetectionRecord,
    onClick: () -> Unit
) {
    val chrom = record.chromaticResult
    val pctDanado = ((chrom?.pctDanado ?: 0.0) * 100.0).toInt()
    val pctSano = ((chrom?.pctSano ?: 0.0) * 100.0).toInt()
    val pctRegular = ((chrom?.pctRegular ?: 0.0) * 100.0).toInt()

    val statusColor = when {
        pctDanado >= 35 -> StatusInfestedRed
        pctRegular >= 30 -> StatusUncertainAmber
        else -> StatusHealthyGreen
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
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(statusColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Yard,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Toma aérea • ${record.fechaCapturaIso.take(16).replace('T', ' ')}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Verde: $pctSano% • Estrés: $pctRegular% • Necrosis: $pctDanado%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = syncColor.copy(alpha = 0.12f)
            ) {
                Icon(
                    imageVector = syncIcon,
                    contentDescription = null,
                    tint = syncColor,
                    modifier = Modifier
                        .padding(6.dp)
                        .size(16.dp)
                )
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
    isVisible: Boolean = true,
    onRefreshDeviceLocation: () -> Unit,
    onSimulateCoordinatesClick: (Double, Double) -> Unit,
    onTreeMarkerClicked: (TreeRecord) -> Unit,
    onFocusTreeOnMap: (TreeRecord) -> Unit = {},
    onFocusCoordinatesOnMap: (Double, Double) -> Unit = { _, _ -> },
    onCenterOnUserGps: () -> Unit = {}
) {
    var isCatalogExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(isVisible) {
        if (!isVisible) {
            isCatalogExpanded = false
        }
    }
    val mapTrees = remember(uiState.treeProgressionList) {
        uiState.treeProgressionList.map { it.tree.copy(ultimoEstadoSanitario = it.currentStatus) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Mapa Vectorial Offline ocupando el 100% del espacio disponible
        OfflineForestMapLibreView(
            currentLat = uiState.captureLatitude,
            currentLon = uiState.captureLongitude,
            showUserLocation = uiState.showUserLocationOnMap,
            isVisible = isVisible,
            focusedTree = uiState.focusedMapTree,
            targetCoordinates = uiState.mapTargetCoordinates,
            mapCameraNonce = uiState.mapCameraRequestNonce,
            trees = mapTrees,
            records = uiState.allRecords,
            hotspots = uiState.activeHotspots,
            onMapCoordinateClicked = onSimulateCoordinatesClick,
            onTreeSelected = onTreeMarkerClicked,
            modifier = Modifier.fillMaxSize()
        )

        if (isVisible) {
        // 2. Píldora de estado superior izquierda
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 12.dp),
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.72f)
        ) {
            Text(
                text = "Árboles: ${uiState.treeProgressionList.size} • Zonas: ${uiState.zoneProgressionList.size} • Focos: ${uiState.activeHotspots.size}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        // 3. Botón flotante "Mi GPS" superior derecho
        Surface(
            onClick = onCenterOnUserGps,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 12.dp, top = 12.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 3.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Mi GPS",
                    tint = GreenPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Mi GPS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // 4. Pastilla flotante colapsada justo encima del navbar (misma altura de 56.dp que el FAB "Nueva Captura")
        AnimatedVisibility(
            visible = !isCatalogExpanded,
            enter = fadeIn(tween(120)) + scaleIn(initialScale = 0.94f, animationSpec = tween(120)),
            exit = fadeOut(tween(80)),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 16.dp)
        ) {
            Surface(
                onClick = { isCatalogExpanded = true },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = 3.dp,
                modifier = Modifier
                    .height(56.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount < -10f) {
                                isCatalogExpanded = true
                            }
                        }
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Forest,
                        contentDescription = null,
                        tint = GreenPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Catálogo (${uiState.treeProgressionList.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Expandir catálogo",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 5. Hoja inferior expandible estilo Google Maps al tocar o deslizar hacia arriba
        AnimatedVisibility(
            visible = isCatalogExpanded,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(durationMillis = 180, easing = EcoVerdeMotion.LinearOutSlowInEasing)
            ) + fadeIn(tween(120)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(durationMillis = 140, easing = EcoVerdeMotion.FastOutLinearInEasing)
            ) + fadeOut(tween(90)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 380.dp),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Barra de arrastre y cabecera colapsable
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectVerticalDragGestures { _, dragAmount ->
                                    if (dragAmount > 14f) {
                                        isCatalogExpanded = false
                                    }
                                }
                            }
                            .clickable { isCatalogExpanded = false }
                            .padding(top = 10.dp, start = 16.dp, end = 12.dp, bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Catálogo de Árboles (${uiState.treeProgressionList.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Toca un árbol para ver su ficha o centrar el mapa",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { isCatalogExpanded = false }) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Colapsar catálogo"
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    if (uiState.treeProgressionList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No hay árboles registrados aún.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 84.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = uiState.treeProgressionList,
                                key = { "map-tree-${it.tree.idArbol}" },
                                contentType = { "map_tree_row" }
                            ) { prog ->
                                val tree = prog.tree
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.medium)
                                        .clickable {
                                            isCatalogExpanded = false
                                            onTreeMarkerClicked(tree)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = tree.nombreEspecifico,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = prog.statusBadgeLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val treeLat = tree.latitud
                                            val treeLon = tree.longitud
                                            if (treeLat != null && treeLon != null) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = GreenPrimary.copy(alpha = 0.12f),
                                                    modifier = Modifier.clickable {
                                                        isCatalogExpanded = false
                                                        onFocusTreeOnMap(tree)
                                                    }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.LocationOn,
                                                            contentDescription = "Ir al mapa",
                                                            tint = GreenPrimary,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Text(
                                                            text = "Ir al mapa",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = GreenPrimary
                                                        )
                                                    }
                                                }
                                            }

                                            Text(
                                                text = "Ver ›",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
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
    onLogoutClick: () -> Unit,
    onOpenTermsClick: () -> Unit
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val deviceName = remember { DeviceIdentifierProvider.getDeviceModelName() }
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

    val initials = remember(session.nombreCompleto) {
        session.nombreCompleto
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifBlank { "EC" }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Tarjeta de Identidad del Técnico
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = GreenPrimary,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = initials,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = session.nombreCompleto,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = CircleShape,
                            color = GreenPrimary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = session.rol.ifBlank { "Experto de campo" },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = GreenPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = GreenPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = session.correo,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "$deviceName • ID ${session.idDispositivo.take(12)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 2. Resumen Operativo y Sincronización
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
                    text = "Resumen Operativo",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProfileStatTile(
                        icon = Icons.Default.CameraAlt,
                        label = "Capturas",
                        value = "${uiState.allRecords.size}",
                        accentColor = GreenPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    ProfileStatTile(
                        icon = Icons.Default.Forest,
                        label = "Árboles",
                        value = "${uiState.treeProgressionList.size}",
                        accentColor = GreenPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    val pendingCount = uiState.daySummary.pendientesSincronizacion
                    ProfileStatTile(
                        icon = if (pendingCount > 0) Icons.Default.CloudUpload else Icons.Default.CloudDone,
                        label = "Pendientes",
                        value = "$pendingCount",
                        accentColor = if (pendingCount > 0) Color(0xFFEF6C00) else GreenPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Button(
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSyncNowClick()
                    },
                    enabled = !uiState.isSyncingBackend,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isSyncingBackend) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sincronizando...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sincronización manual", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3. Estado Técnico y Normativa
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
                    text = "Sistema y Normativa",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                ProfileInfoTile(
                    icon = Icons.Default.CheckCircle,
                    title = "Motor de diagnóstico",
                    subtitle = "IA híbrida (Nube + TFLite local) calibrada y activa"
                )
                ProfileInfoTile(
                    icon = Icons.Default.LocationOn,
                    title = "Geolocalización NOM-019",
                    subtitle = "Radio de control fitosanitario de 100 m"
                )
                ProfileInfoTile(
                    icon = Icons.Default.Lock,
                    title = "Privacidad de imágenes",
                    subtitle = "Uso exclusivo para diagnóstico fitosanitario"
                )
                ProfileInfoTile(
                    icon = Icons.Default.Info,
                    title = "Versión del sistema",
                    subtitle = "EcoVerde 1.0.0 (Build 2026-B161)"
                )

                OutlinedButton(
                    onClick = onOpenTermsClick,
                    shape = CircleShape,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenPrimary),
                    border = BorderStroke(1.dp, GreenPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Términos, Privacidad y Acerca de", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // 4. Cuenta y Mantenimiento Local
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
                    text = "Cuenta y Datos Locales",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedButton(
                    onClick = { showClearDbBottomSheet = true },
                    shape = CircleShape,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusInfestedRed),
                    border = BorderStroke(1.dp, StatusInfestedRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Borrar datos locales", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onLogoutClick,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = StatusInfestedRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cerrar sesión", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ProfileStatTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProfileInfoTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = GreenPrimary.copy(alpha = 0.10f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = GreenPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
