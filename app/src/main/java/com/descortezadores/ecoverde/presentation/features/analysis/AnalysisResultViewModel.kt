package com.descortezadores.ecoverde.presentation.features.analysis

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.descortezadores.ecoverde.core.common.DeviceIdentifierProvider
import com.descortezadores.ecoverde.core.common.ExtractedImagePayload
import com.descortezadores.ecoverde.core.common.ImageMetadataAndLocationHelper
import com.descortezadores.ecoverde.core.common.SystemMessages
import com.descortezadores.ecoverde.core.opencv.OpenCvInitializer
import com.descortezadores.ecoverde.domain.repository.BarkClassificationRepository
import com.descortezadores.ecoverde.data.segmentation.ImageOverlayGenerator
import com.descortezadores.ecoverde.data.segmentation.OpenCvColorSegmenter
import com.descortezadores.ecoverde.data.sync.DeteccionesSyncWorker
import com.descortezadores.ecoverde.domain.model.BiologicalSeverityLevel
import com.descortezadores.ecoverde.domain.model.ChromaticAnalysisResult
import com.descortezadores.ecoverde.domain.model.ClassificationOutcome
import com.descortezadores.ecoverde.domain.model.ClassificationResult
import com.descortezadores.ecoverde.domain.model.ColorBandResult
import com.descortezadores.ecoverde.domain.model.DailyJourneySummary
import com.descortezadores.ecoverde.domain.model.DiagnosticCategory
import com.descortezadores.ecoverde.domain.model.SampleType
import com.descortezadores.ecoverde.domain.model.TargetAssociation
import com.descortezadores.ecoverde.domain.model.TreeRecord
import com.descortezadores.ecoverde.domain.repository.AuthRepository
import com.descortezadores.ecoverde.domain.repository.ForestMonitoringRepository
import com.descortezadores.ecoverde.domain.repository.SavedDetectionRecord
import com.descortezadores.ecoverde.domain.usecase.AnalyzeImageColorsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.core.Core
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * ViewModel principal de la Capa de Presentación Declarativa (Fase 5).
 */
@HiltViewModel
class AnalysisResultViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val authRepository: AuthRepository,
    private val openCvInitializer: OpenCvInitializer,
    private val openCvColorSegmenter: OpenCvColorSegmenter,
    private val imageOverlayGenerator: ImageOverlayGenerator,
    private val barkClassificationRepository: BarkClassificationRepository,
    private val forestMonitoringRepository: ForestMonitoringRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalysisResultUiState())
    val uiState: StateFlow<AnalysisResultUiState> = _uiState.asStateFlow()

    private var overlayRenderJob: Job? = null
    private var currentAnalysisJob: Job? = null

    init {
        observeLocalDatabaseFlows()
        refreshSystemConnectivityAndSample()
    }

    private fun observeLocalDatabaseFlows() {
        viewModelScope.launch {
            forestMonitoringRepository.observeDailyJourneySummary().collectLatest { summary ->
                _uiState.update { it.copy(daySummary = summary) }
            }
        }
        viewModelScope.launch(Dispatchers.Default) {
            forestMonitoringRepository.observeAllDetections().collectLatest { records ->
                val trees = _uiState.value.availableTrees
                val zones = _uiState.value.availableZones
                val treesProg = AnalysisResultUiState.computeTreeProgressions(trees, records)
                val zonesProg = AnalysisResultUiState.computeZoneProgressions(zones, records)
                _uiState.update {
                    it.copy(
                        allRecords = records,
                        treeProgressionList = treesProg,
                        zoneProgressionList = zonesProg
                    )
                }
                checkProximityToHotspots()
            }
        }
        viewModelScope.launch(Dispatchers.Default) {
            forestMonitoringRepository.observeTrees().collectLatest { trees ->
                val records = _uiState.value.allRecords
                val treesProg = AnalysisResultUiState.computeTreeProgressions(trees, records)
                _uiState.update { state ->
                    val defaultSelected = state.selectedTreeId ?: trees.firstOrNull()?.idArbol
                    state.copy(
                        availableTrees = trees,
                        selectedTreeId = defaultSelected,
                        treeProgressionList = treesProg
                    )
                }
                checkProximityToHotspots()
            }
        }
        viewModelScope.launch(Dispatchers.Default) {
            forestMonitoringRepository.observeFoliageZones().collectLatest { zones ->
                val records = _uiState.value.allRecords
                val zonesProg = AnalysisResultUiState.computeZoneProgressions(zones, records)
                _uiState.update { state ->
                    val defaultZone = state.selectedZoneId ?: zones.firstOrNull()?.idZona
                    state.copy(
                        availableZones = zones,
                        selectedZoneId = defaultZone,
                        zoneProgressionList = zonesProg
                    )
                }
            }
        }
        viewModelScope.launch {
            forestMonitoringRepository.observeActiveInfestationFoci().collectLatest { hotspots ->
                _uiState.update { it.copy(activeHotspots = hotspots) }
                checkProximityToHotspots()
            }
        }
    }

    private fun refreshSystemConnectivityAndSample() {
        viewModelScope.launch {
            val openCvReady = openCvInitializer.ensureInitialized()
            val version = if (openCvReady) Core.VERSION else "No disponible"
            val health = authRepository.checkBackendHealth().getOrDefault(false)
            _uiState.update {
                it.copy(
                    openCvNativeVersion = version,
                    isBackendReachable = health
                )
            }
            refreshCurrentDeviceLocation()
            startRealtimeLocationTracking()
            if (health) {
                forestMonitoringRepository.refreshRemoteCatalogsAndFoci()
            }
        }
    }

    private var locationTrackingJob: Job? = null

    /**
     * Inicia la escucha continua de ubicación GPS en tiempo real para actualizar el marcador
     * del usuario en el mapa y recalcular alertas en radio de 100 m automáticamente.
     */
    fun startRealtimeLocationTracking() {
        if (locationTrackingJob?.isActive == true) return
        locationTrackingJob = viewModelScope.launch {
            ImageMetadataAndLocationHelper.observeLocationUpdates(appContext).collect { gpsFix ->
                if (gpsFix.isLiveGps) {
                    _uiState.update { state ->
                        if (!state.captureHasExifGps) {
                            state.copy(
                                captureLatitude = gpsFix.latitude,
                                captureLongitude = gpsFix.longitude,
                                captureGpsAccuracyMeters = gpsFix.accuracyMeters,
                                captureLocationSourceLabel = "GPS en tiempo real"
                            )
                        } else {
                            state
                        }
                    }
                    checkProximityToHotspots()
                }
            }
        }
    }

    /**
     * Obtiene la ubicación GPS actual real del dispositivo y actualiza el estado si no se está
     * mostrando una imagen con coordenadas EXIF propias.
     */
    fun refreshCurrentDeviceLocation() {
        startRealtimeLocationTracking()
        viewModelScope.launch {
            val currentState = _uiState.value
            val gpsFix = ImageMetadataAndLocationHelper.getCurrentDeviceLocation(
                context = appContext,
                fallbackLat = currentState.captureLatitude,
                fallbackLon = currentState.captureLongitude
            )
            if (gpsFix.isLiveGps) {
                _uiState.update { state ->
                    if (!state.captureHasExifGps) {
                        state.copy(
                            captureLatitude = gpsFix.latitude,
                            captureLongitude = gpsFix.longitude,
                            captureGpsAccuracyMeters = gpsFix.accuracyMeters,
                            captureLocationSourceLabel = "Ubicación GPS actual del dispositivo"
                        )
                    } else {
                        state
                    }
                }
                checkProximityToHotspots()
            }
        }
    }

    fun selectMainDestination(destination: EcoVerdeMainDestination) {
        _uiState.update {
            it.copy(
                currentDestination = destination,
                selectedTreeDetailId = if (destination != EcoVerdeMainDestination.INICIO) null else it.selectedTreeDetailId,
                selectedZoneDetailId = if (destination != EcoVerdeMainDestination.INICIO) null else it.selectedZoneDetailId,
                errorMessage = null
            )
        }
    }

    fun onHomeWorkTabChanged(tab: HomeWorkTab) {
        _uiState.update { it.copy(homeWorkTab = tab) }
    }

    fun onHomeTreeFilterChanged(filter: TreeHealthFilter) {
        _uiState.update { it.copy(homeTreeFilter = filter) }
    }

    fun openTreeDetailSection(treeId: String) {
        val tree = _uiState.value.availableTrees.firstOrNull { it.idArbol == treeId }
        _uiState.update {
            it.copy(
                currentDestination = EcoVerdeMainDestination.INICIO,
                selectedTreeDetailId = treeId,
                selectedTreeId = treeId,
                selectedZoneDetailId = null,
                homeWorkTab = HomeWorkTab.ARBOLES,
                fieldObservations = tree?.descripcion ?: "",
                associationMode = TargetAssociationMode.EXISTENTE,
                sampleType = SampleType.CORTEZA,
                errorMessage = null
            )
        }
    }

    fun closeTreeDetailSection() {
        _uiState.update { it.copy(selectedTreeDetailId = null) }
    }

    fun openZoneDetailSection(zoneId: String) {
        val zone = _uiState.value.availableZones.firstOrNull { it.idZona == zoneId }
        _uiState.update {
            it.copy(
                currentDestination = EcoVerdeMainDestination.INICIO,
                selectedZoneDetailId = zoneId,
                selectedZoneId = zoneId,
                selectedTreeDetailId = null,
                homeWorkTab = HomeWorkTab.ZONAS_FOLLAJE,
                fieldObservations = zone?.descripcion ?: "",
                associationMode = TargetAssociationMode.EXISTENTE,
                sampleType = SampleType.FOLLAJE,
                errorMessage = null
            )
        }
    }

    fun closeZoneDetailSection() {
        _uiState.update { it.copy(selectedZoneDetailId = null) }
    }

    fun startNewCaptureForTree(treeId: String) {
        currentAnalysisJob?.cancel()
        overlayRenderJob?.cancel()
        _uiState.update {
            it.copy(
                sampleType = SampleType.CORTEZA,
                associationMode = TargetAssociationMode.EXISTENTE,
                selectedTreeId = treeId,
                selectedTreeDetailId = treeId,
                selectedZoneDetailId = null,
                currentDestination = EcoVerdeMainDestination.ANALIZAR,
                wizardStep = WizardStep.CAPTURA,
                selectedTab = AnalysisResultTab.CLASIFICACION,
                originalBitmap = null,
                uncroppedBitmap = null,
                segmentedOverlayBitmap = null,
                classificationResult = null,
                chromaticResult = null,
                overlayOpacityAlpha = 0.0f,
                isProcessingVision = false,
                isInspectingHistoricalRecord = false,
                statusBannerMessage = "Capturando progreso para el árbol seleccionado"
            )
        }
    }

    fun startNewCaptureForZone(zoneId: String) {
        currentAnalysisJob?.cancel()
        overlayRenderJob?.cancel()
        _uiState.update {
            it.copy(
                sampleType = SampleType.FOLLAJE,
                associationMode = TargetAssociationMode.EXISTENTE,
                selectedZoneId = zoneId,
                selectedZoneDetailId = zoneId,
                selectedTreeDetailId = null,
                currentDestination = EcoVerdeMainDestination.ANALIZAR,
                wizardStep = WizardStep.CAPTURA,
                selectedTab = AnalysisResultTab.ANALISIS_COLOR,
                originalBitmap = null,
                uncroppedBitmap = null,
                segmentedOverlayBitmap = null,
                classificationResult = null,
                chromaticResult = null,
                overlayOpacityAlpha = 0.0f,
                isProcessingVision = false,
                isInspectingHistoricalRecord = false,
                statusBannerMessage = "Capturando toma de dosel para la zona seleccionada"
            )
        }
    }

    fun startWizard(sampleType: SampleType? = null, treeId: String? = null) {
        currentAnalysisJob?.cancel()
        overlayRenderJob?.cancel()
        _uiState.update { state ->
            val effectiveType = sampleType ?: state.sampleType
            val effectiveTreeId = treeId ?: state.selectedTreeId
            state.copy(
                currentDestination = EcoVerdeMainDestination.ANALIZAR,
                wizardStep = if (treeId != null) WizardStep.CAPTURA else WizardStep.MODALIDAD,
                sampleType = effectiveType,
                associationMode = if (treeId != null) TargetAssociationMode.EXISTENTE else state.associationMode,
                selectedTreeId = effectiveTreeId,
                selectedTreeDetailId = treeId ?: state.selectedTreeDetailId,
                selectedZoneDetailId = null,
                isInspectingHistoricalRecord = false,
                newTreeName = "",
                fieldObservations = "",
                originalBitmap = null,
                uncroppedBitmap = null,
                segmentedOverlayBitmap = null,
                classificationResult = null,
                chromaticResult = null,
                overlayOpacityAlpha = 0.0f,
                isProcessingVision = false,
                errorMessage = null,
                statusBannerMessage = null
            )
        }
    }

    fun setWizardStep(step: WizardStep) {
        _uiState.update { it.copy(wizardStep = step) }
    }

    fun repeatCapture() {
        currentAnalysisJob?.cancel()
        overlayRenderJob?.cancel()
        _uiState.update {
            it.copy(
                wizardStep = WizardStep.CAPTURA,
                originalBitmap = null,
                uncroppedBitmap = null,
                classificationResult = null,
                chromaticResult = null,
                segmentedOverlayBitmap = null,
                overlayOpacityAlpha = 0.0f,
                isProcessingVision = false,
                statusBannerMessage = null
            )
        }
    }

    fun updateCroppedBitmap(bitmap: Bitmap) {
        if (bitmap.width < ImageMetadataAndLocationHelper.MIN_IMAGE_DIMENSION_PX ||
            bitmap.height < ImageMetadataAndLocationHelper.MIN_IMAGE_DIMENSION_PX
        ) {
            _uiState.update {
                it.copy(
                    errorMessage = "${SystemMessages.MSG24} El recorte debe medir al menos 300×300 px (recorte actual: ${bitmap.width}×${bitmap.height} px)."
                )
            }
            return
        }
        _uiState.update {
            it.copy(
                originalBitmap = bitmap,
                uncroppedBitmap = it.uncroppedBitmap ?: it.originalBitmap,
                errorMessage = null
            )
        }
    }

    fun restoreOriginalBitmap() {
        val uncropped = _uiState.value.uncroppedBitmap ?: return
        _uiState.update {
            it.copy(originalBitmap = uncropped)
        }
    }

    fun rotateCurrentBitmap() {
        val current = _uiState.value.originalBitmap ?: return
        val matrix = android.graphics.Matrix().apply { postRotate(90f) }
        val rotated = Bitmap.createBitmap(current, 0, 0, current.width, current.height, matrix, true)
        _uiState.update {
            it.copy(originalBitmap = rotated)
        }
    }

    fun onMapTreeSelected(tree: TreeRecord?) {
        _uiState.update {
            it.copy(
                selectedMapTree = tree,
                showMapBottomSheet = tree != null
            )
        }
    }

    fun focusTreeOnMap(tree: TreeRecord) {
        val lat = tree.latitud ?: return
        val lon = tree.longitud ?: return
        _uiState.update {
            it.copy(
                currentDestination = EcoVerdeMainDestination.MAPA,
                focusedMapTreeId = tree.idArbol,
                showUserLocationOnMap = false,
                mapTargetCoordinates = Pair(lat, lon),
                mapCameraRequestNonce = it.mapCameraRequestNonce + 1L,
                showMapBottomSheet = false
            )
        }
    }

    fun focusCoordinatesOnMap(lat: Double, lon: Double) {
        _uiState.update {
            it.copy(
                currentDestination = EcoVerdeMainDestination.MAPA,
                focusedMapTreeId = null,
                showUserLocationOnMap = false,
                mapTargetCoordinates = Pair(lat, lon),
                mapCameraRequestNonce = it.mapCameraRequestNonce + 1L,
                showMapBottomSheet = false
            )
        }
    }

    fun centerOnUserGps() {
        refreshCurrentDeviceLocation()
        _uiState.update {
            it.copy(
                focusedMapTreeId = null,
                showUserLocationOnMap = true,
                mapTargetCoordinates = Pair(it.captureLatitude, it.captureLongitude),
                mapCameraRequestNonce = it.mapCameraRequestNonce + 1L
            )
        }
    }

    fun dismissMapBottomSheet() {
        _uiState.update { it.copy(showMapBottomSheet = false) }
    }

    fun setSearchQueryTree(query: String) {
        _uiState.update { it.copy(treeSearchQuery = query) }
    }

    fun registerNewTreeSectionDirectly() {
        val state = _uiState.value
        val name = state.newTreeName.trim()
        if (name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Ingresa un nombre o identificador para el árbol (ej. Fresno95).") }
            return
        }
        val species = state.newTreeSpecies.trim().ifBlank { PREDEFINED_TREE_SPECIES.first() }
        viewModelScope.launch {
            val res = forestMonitoringRepository.registerTree(
                nombreEspecifico = name,
                especie = species,
                latitud = state.captureLatitude,
                longitud = state.captureLongitude,
                descripcion = state.fieldObservations.trim()
            )
            res.fold(
                onSuccess = { createdTree ->
                    _uiState.update {
                        it.copy(
                            associationMode = TargetAssociationMode.EXISTENTE,
                            selectedTreeId = createdTree.idArbol,
                            selectedTreeDetailId = createdTree.idArbol,
                            statusBannerMessage = "Sección creada para '${createdTree.nombreEspecifico}'. Ahora puedes tomar fotos de su progreso."
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update { it.copy(errorMessage = err.message ?: SystemMessages.MSG18) }
                }
            )
        }
    }

    fun selectAnalysisTab(tab: AnalysisResultTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun selectSampleType(sampleType: SampleType) {
        if (_uiState.value.sampleType == sampleType) return
        val targetTab = if (sampleType == SampleType.FOLLAJE) {
            AnalysisResultTab.ANALISIS_COLOR
        } else {
            _uiState.value.selectedTab
        }
        _uiState.update {
            it.copy(
                sampleType = sampleType,
                selectedTab = targetTab,
                selectedCategoryFilter = null
            )
        }
        val currentBitmap = _uiState.value.originalBitmap
        if (currentBitmap != null) {
            analyzeCapturedOrImportedBitmap(currentBitmap, sampleTypeOverride = sampleType)
        }
    }

    /**
     * Procesa una imagen seleccionada desde la Galería (`Uri`):
     * - Revisa primero si la imagen contiene metadatos EXIF (`latLong`, fecha/hora, cámara).
     * - Si tiene coordenadas GPS en EXIF, las utiliza.
     * - Si NO tiene coordenadas GPS en EXIF, obtiene y utiliza la ubicación GPS actual del dispositivo.
     */
    fun analyzeGalleryUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessingVision = true,
                    currentDestination = EcoVerdeMainDestination.ANALIZAR,
                    errorMessage = null,
                    statusBannerMessage = null
                )
            }
            val currentState = _uiState.value
            val validationError = ImageMetadataAndLocationHelper.validateGalleryUriPreconditions(
                context = appContext,
                uri = uri
            )
            if (validationError != null) {
                _uiState.update {
                    it.copy(
                        isProcessingVision = false,
                        errorMessage = validationError
                    )
                }
                return@launch
            }
            val payload = ImageMetadataAndLocationHelper.processGalleryUri(
                context = appContext,
                uri = uri,
                fallbackLat = currentState.captureLatitude,
                fallbackLon = currentState.captureLongitude
            )
            if (payload == null) {
                _uiState.update {
                    it.copy(
                        isProcessingVision = false,
                        errorMessage = SystemMessages.MSG18
                    )
                }
                return@launch
            }
            val locationBanner = if (payload.hasExifGps) {
                val distFromDevice = haversineDistanceMeters(
                    currentState.captureLatitude,
                    currentState.captureLongitude,
                    payload.latitude,
                    payload.longitude
                )
                if (distFromDevice >= 20.0) {
                    val distLabel = if (distFromDevice >= 1000.0) {
                        String.format(java.util.Locale.US, "%.2f km", distFromDevice / 1000.0)
                    } else {
                        "${distFromDevice.toInt()} m"
                    }
                    "Ubicación tomada de los metadatos de la imagen (a $distLabel de tu ubicación actual)."
                } else {
                    "Ubicación tomada de los metadatos EXIF de la imagen."
                }
            } else {
                "La imagen no contiene ubicación en sus metadatos; se usó tu ubicación actual."
            }

            if (currentState.wizardStep == WizardStep.CAPTURA) {
                _uiState.update {
                    it.copy(
                        isProcessingVision = false,
                        originalBitmap = payload.bitmap,
                        uncroppedBitmap = payload.bitmap,
                        captureLatitude = payload.latitude,
                        captureLongitude = payload.longitude,
                        captureGpsAccuracyMeters = payload.gpsAccuracyMeters,
                        captureHasExifGps = payload.hasExifGps,
                        captureLocationSourceLabel = payload.locationSourceLabel,
                        captureExifDateTimeIso = payload.exifDateTimeIso,
                        captureExifCameraModel = payload.exifCameraModel,
                        statusBannerMessage = locationBanner,
                        errorMessage = null
                    )
                }
                checkProximityToHotspots()
            } else {
                analyzeCapturedOrImportedBitmap(
                    bitmap = payload.bitmap,
                    extractedPayload = payload,
                    locationBannerOverride = locationBanner
                )
            }
        }
    }

    /**
     * Procesa una fotografía tomada directamente con la Cámara (`File` vía `FileProvider`):
     * - Recupera los metadatos EXIF de la captura directa y/o graba la ubicación GPS actual del
     *   teléfono en los encabezados EXIF del archivo JPEG antes de ejecutar TFLite + OpenCV HSV.
     */
    fun analyzeCameraCaptureFile(captureFile: File) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessingVision = true,
                    currentDestination = EcoVerdeMainDestination.ANALIZAR,
                    errorMessage = null,
                    statusBannerMessage = null
                )
            }
            val currentState = _uiState.value
            val validationError = ImageMetadataAndLocationHelper.validateImageFilePreconditions(captureFile)
            if (validationError != null) {
                _uiState.update {
                    it.copy(
                        isProcessingVision = false,
                        errorMessage = validationError
                    )
                }
                return@launch
            }
            val payload = ImageMetadataAndLocationHelper.processCameraCaptureFile(
                context = appContext,
                captureFile = captureFile,
                fallbackLat = currentState.captureLatitude,
                fallbackLon = currentState.captureLongitude
            )
            if (payload == null) {
                _uiState.update {
                    it.copy(
                        isProcessingVision = false,
                        errorMessage = SystemMessages.MSG18
                    )
                }
                return@launch
            }
            val locationBanner = if (payload.hasExifGps) {
                val distFromDevice = haversineDistanceMeters(
                    currentState.captureLatitude,
                    currentState.captureLongitude,
                    payload.latitude,
                    payload.longitude
                )
                if (distFromDevice >= 20.0) {
                    val distLabel = if (distFromDevice >= 1000.0) {
                        String.format(java.util.Locale.US, "%.2f km", distFromDevice / 1000.0)
                    } else {
                        "${distFromDevice.toInt()} m"
                    }
                    "Ubicación tomada de los metadatos de la imagen (a $distLabel de tu ubicación actual)."
                } else {
                    "Ubicación tomada de los metadatos EXIF de la imagen."
                }
            } else {
                "La imagen no contiene ubicación en sus metadatos; se usó tu ubicación actual."
            }

            if (currentState.wizardStep == WizardStep.CAPTURA) {
                _uiState.update {
                    it.copy(
                        isProcessingVision = false,
                        originalBitmap = payload.bitmap,
                        uncroppedBitmap = payload.bitmap,
                        captureLatitude = payload.latitude,
                        captureLongitude = payload.longitude,
                        captureGpsAccuracyMeters = payload.gpsAccuracyMeters,
                        captureHasExifGps = payload.hasExifGps,
                        captureLocationSourceLabel = payload.locationSourceLabel,
                        captureExifDateTimeIso = payload.exifDateTimeIso,
                        captureExifCameraModel = payload.exifCameraModel,
                        statusBannerMessage = locationBanner,
                        errorMessage = null
                    )
                }
                checkProximityToHotspots()
            } else {
                analyzeCapturedOrImportedBitmap(
                    bitmap = payload.bitmap,
                    extractedPayload = payload,
                    locationBannerOverride = locationBanner
                )
            }
        }
    }

    fun analyzeCapturedOrImportedBitmap(
        bitmap: Bitmap,
        sampleTypeOverride: SampleType? = null,
        extractedPayload: ExtractedImagePayload? = null,
        locationBannerOverride: String? = null
    ) {
        val effectiveSampleType = sampleTypeOverride ?: _uiState.value.sampleType
        currentAnalysisJob?.cancel()
        overlayRenderJob?.cancel()
        currentAnalysisJob = viewModelScope.launch {
            // Si se invocó con un Bitmap directo sin payload EXIF previo, intentar refrescar GPS actual
            val resolvedLat: Double
            val resolvedLon: Double
            val resolvedAccuracy: Float?
            val resolvedHasExifGps: Boolean
            val resolvedSourceLabel: String
            val resolvedExifDateIso: String?
            val resolvedCameraModel: String?

            if (extractedPayload != null) {
                resolvedLat = extractedPayload.latitude
                resolvedLon = extractedPayload.longitude
                resolvedAccuracy = extractedPayload.gpsAccuracyMeters
                resolvedHasExifGps = extractedPayload.hasExifGps
                resolvedSourceLabel = extractedPayload.locationSourceLabel
                resolvedExifDateIso = extractedPayload.exifDateTimeIso
                resolvedCameraModel = extractedPayload.exifCameraModel
            } else if (sampleTypeOverride != null && _uiState.value.originalBitmap === bitmap) {
                // Re-análisis al cambiar entre Corteza y Follaje sobre la misma imagen ya cargada: conservar sus metadatos
                val st = _uiState.value
                resolvedLat = st.captureLatitude
                resolvedLon = st.captureLongitude
                resolvedAccuracy = st.captureGpsAccuracyMeters
                resolvedHasExifGps = st.captureHasExifGps
                resolvedSourceLabel = st.captureLocationSourceLabel
                resolvedExifDateIso = st.captureExifDateTimeIso
                resolvedCameraModel = st.captureExifCameraModel
            } else {
                val gpsFix = ImageMetadataAndLocationHelper.getCurrentDeviceLocation(
                    context = appContext,
                    fallbackLat = _uiState.value.captureLatitude,
                    fallbackLon = _uiState.value.captureLongitude
                )
                resolvedLat = gpsFix.latitude
                resolvedLon = gpsFix.longitude
                resolvedAccuracy = gpsFix.accuracyMeters
                resolvedHasExifGps = false
                resolvedSourceLabel = if (gpsFix.isLiveGps) {
                    "Ubicación GPS actual del dispositivo"
                } else {
                    "Ubicación activa en mapa"
                }
                resolvedExifDateIso = java.time.Instant.now().toString()
                resolvedCameraModel = DeviceIdentifierProvider.getDeviceModelName()
            }

            _uiState.update {
                it.copy(
                    isProcessingVision = true,
                    sampleType = effectiveSampleType,
                    currentDestination = EcoVerdeMainDestination.ANALIZAR,
                    wizardStep = WizardStep.RESULTADOS,
                    originalBitmap = bitmap,
                    uncroppedBitmap = bitmap,
                    segmentedOverlayBitmap = null,
                    classificationResult = null,
                    chromaticResult = null,
                    captureLatitude = resolvedLat,
                    captureLongitude = resolvedLon,
                    captureGpsAccuracyMeters = resolvedAccuracy,
                    captureHasExifGps = resolvedHasExifGps,
                    captureLocationSourceLabel = resolvedSourceLabel,
                    captureExifDateTimeIso = resolvedExifDateIso,
                    captureExifCameraModel = resolvedCameraModel,
                    errorMessage = null,
                    statusBannerMessage = locationBannerOverride ?: if (resolvedHasExifGps) it.statusBannerMessage else null
                )
            }
            checkProximityToHotspots()

            try {
                val startMs = SystemClock.elapsedRealtime()
                val analysisUuid = UUID.randomUUID().toString()
                val currentFilter = _uiState.value.selectedCategoryFilter
                val currentAlpha = _uiState.value.overlayOpacityAlpha

                // Inferencia híbrida de IA (Cloud si online, TFLite si offline) concurrente con OpenCV HSV
                val tfliteDeferred = async(Dispatchers.IO) {
                    if (effectiveSampleType == SampleType.CORTEZA) {
                        barkClassificationRepository.classifyBark(bitmap).getOrNull()
                    } else {
                        null
                    }
                }

                val openCvDeferred = async(Dispatchers.Default) {
                    val masks = openCvColorSegmenter.segmentBitmapToMasks(bitmap, effectiveSampleType)
                    try {
                        val overlay = imageOverlayGenerator.generateOverlayBitmap(
                            segmentationMasks = masks,
                            sampleType = effectiveSampleType,
                            alphaOpacity = if (currentAlpha > 0f) currentAlpha else 0.70f,
                            isolatedCategory = currentFilter
                        )
                        Triple(overlay, masks.ratiosByCategory, masks.pixelCountsByCategory to masks.totalPixels)
                    } finally {
                        masks.releaseMasks()
                    }
                }

                val rawTflite = tfliteDeferred.await()
                val (overlayBitmap, ratios, countsAndTotal) = openCvDeferred.await()
                val pixelCounts = countsAndTotal.first
                val totalPixels = countsAndTotal.second.coerceAtLeast(1)

                val (origPath, ovlPath) = persistAnalysisImagesToDisk(
                    analysisId = analysisUuid,
                    original = bitmap,
                    overlay = overlayBitmap,
                    latitude = resolvedLat,
                    longitude = resolvedLon,
                    captureTimestampIso = resolvedExifDateIso,
                    cameraModel = resolvedCameraModel
                )

                val pctDanado = ratios[DiagnosticCategory.DANADO] ?: 0.0
                val pctRegular = ratios[DiagnosticCategory.REGULAR] ?: 0.0
                val pctSano = ratios[DiagnosticCategory.SANO] ?: 0.0
                val pctIncierto = ratios[DiagnosticCategory.INCIERTO] ?: 0.0

                val bands = listOf(
                    DiagnosticCategory.DANADO,
                    DiagnosticCategory.REGULAR,
                    DiagnosticCategory.SANO,
                    DiagnosticCategory.INCIERTO
                ).map { cat ->
                    val ratio = (ratios[cat] ?: 0.0).coerceIn(0.0, 1.0)
                    ColorBandResult(
                        category = cat,
                        pixelCount = (pixelCounts[cat] ?: 0).coerceAtLeast(0),
                        ratio = ratio,
                        percentage = (ratio * 100.0).toFloat(),
                        label = cat.biologicalSignalDescription(effectiveSampleType),
                        colorHex = cat.officialHexColor(effectiveSampleType)
                    )
                }

                val severityScore = AnalyzeImageColorsUseCase.calculateBiologicalSeverityScore(
                    pctDanado = pctDanado,
                    pctRegular = pctRegular,
                    pctIncierto = pctIncierto
                )
                val severityLevel = AnalyzeImageColorsUseCase.classifyBiologicalSeverity(
                    pctDanado = pctDanado,
                    pctRegular = pctRegular,
                    pctSano = pctSano,
                    pctIncierto = pctIncierto
                )
                val dominantCategory = AnalyzeImageColorsUseCase.determineDominantCategory(bands)

                val chromaticDomain = ChromaticAnalysisResult(
                    analysisId = analysisUuid,
                    sampleType = effectiveSampleType,
                    originalImagePath = origPath,
                    segmentedOverlayImagePath = ovlPath,
                    totalPixelsAnalyzed = totalPixels,
                    bands = bands,
                    pctSano = pctSano,
                    pctRegular = pctRegular,
                    pctDanado = pctDanado,
                    pctIncierto = pctIncierto,
                    biologicalSeverityScore = severityScore,
                    severityLevel = severityLevel,
                    dominantCategory = dominantCategory,
                    timestampMillis = System.currentTimeMillis()
                )

                val elapsedMs = (SystemClock.elapsedRealtime() - startMs).coerceAtLeast(1L)

                val classificationResult = rawTflite ?: buildFallbackClassificationFromChromatic(
                    pctDanado = pctDanado,
                    pctRegular = pctRegular,
                    elapsedMs = elapsedMs
                )

                _uiState.update {
                    it.copy(
                        isProcessingVision = false,
                        wizardStep = WizardStep.RESULTADOS,
                        currentDestination = EcoVerdeMainDestination.ANALIZAR,
                        originalBitmap = bitmap,
                        uncroppedBitmap = it.uncroppedBitmap ?: bitmap,
                        segmentedOverlayBitmap = overlayBitmap,
                        classificationResult = classificationResult,
                        chromaticResult = chromaticDomain,
                        processingElapsedMs = elapsedMs,
                        statusBannerMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessingVision = false,
                        errorMessage = e.message ?: SystemMessages.MSG22
                    )
                }
            } finally {
                if (_uiState.value.isProcessingVision) {
                    _uiState.update { it.copy(isProcessingVision = false) }
                }
            }
        }
    }

    fun analyzeSyntheticFieldSample(
        sampleType: SampleType,
        simulateInfested: Boolean
    ) {
        val syntheticBitmap = generateSyntheticSampleBitmap(sampleType, simulateInfested)
        analyzeCapturedOrImportedBitmap(syntheticBitmap, sampleTypeOverride = sampleType)
    }

    fun onCategoryFilterSelected(category: DiagnosticCategory?) {
        _uiState.update {
            it.copy(
                selectedCategoryFilter = category,
                showOriginalComparison = false
            )
        }
        reRenderOverlayForCurrentParameters()
    }

    fun onOverlayAlphaChanged(newAlpha: Float) {
        val clamped = newAlpha.coerceIn(0f, 1f)
        _uiState.update {
            it.copy(
                overlayOpacityAlpha = clamped,
                showOriginalComparison = false
            )
        }
    }

    fun toggleOriginalComparison() {
        _uiState.update { it.copy(showOriginalComparison = !it.showOriginalComparison) }
    }

    private fun reRenderOverlayForCurrentParameters() {
        val sourceBitmap = _uiState.value.originalBitmap ?: return
        val sampleType = _uiState.value.sampleType
        val filter = _uiState.value.selectedCategoryFilter
        val alpha = _uiState.value.overlayOpacityAlpha

        overlayRenderJob?.cancel()
        overlayRenderJob = viewModelScope.launch {
            _uiState.update { it.copy(isRenderingOverlay = true) }
            try {
                val masks = openCvColorSegmenter.segmentBitmapToMasks(sourceBitmap, sampleType)
                val updatedBitmap = try {
                    imageOverlayGenerator.generateOverlayBitmap(
                        segmentationMasks = masks,
                        sampleType = sampleType,
                        alphaOpacity = if (alpha > 0f) alpha else 0.70f,
                        isolatedCategory = filter
                    )
                } finally {
                    masks.releaseMasks()
                }
                _uiState.update {
                    it.copy(
                        isRenderingOverlay = false,
                        segmentedOverlayBitmap = updatedBitmap
                    )
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isRenderingOverlay = false) }
            }
        }
    }

    fun onAssociationModeChanged(mode: TargetAssociationMode) {
        _uiState.update { it.copy(associationMode = mode) }
    }

    fun onSelectExistingTree(treeId: String) {
        _uiState.update { it.copy(selectedTreeId = treeId) }
    }

    fun onNewTreeNameChanged(value: String) {
        _uiState.update { it.copy(newTreeName = value) }
    }

    fun onNewTreeSpeciesChanged(value: String) {
        _uiState.update { it.copy(newTreeSpecies = value) }
    }

    fun onSelectExistingZone(zoneId: String) {
        _uiState.update { it.copy(selectedZoneId = zoneId) }
    }

    fun onNewZoneNameChanged(value: String) {
        _uiState.update { it.copy(newZoneName = value) }
    }

    fun onNewZoneRadiusChanged(value: String) {
        _uiState.update { it.copy(newZoneRadiusMeters = value) }
    }

    fun onObservationsChanged(value: String) {
        _uiState.update { it.copy(fieldObservations = value) }
    }

    fun onHistoryFilterChanged(filter: HistoryFilterOption) {
        _uiState.update { it.copy(historyFilter = filter) }
    }

    fun updateSimulatedCoordinates(lat: Double, lon: Double) {
        _uiState.update {
            it.copy(
                captureLatitude = lat,
                captureLongitude = lon,
                captureHasExifGps = false,
                captureLocationSourceLabel = "Coordenadas simuladas en mapa"
            )
        }
        checkProximityToHotspots()
    }

    private fun checkProximityToHotspots() {
        val state = _uiState.value

        // 1. Verificar focos activos del backend en radio circular de 100 m
        val closestHotspot = state.activeHotspots.mapNotNull { focus ->
            val distMeters = haversineDistanceMeters(
                state.captureLatitude,
                state.captureLongitude,
                focus.latitudCentro,
                focus.longitudCentro
            )
            if (distMeters <= 100.0) {
                ProximityAlertUi(
                    idFoco = focus.idFoco,
                    distanciaMetros = distMeters,
                    cantidadArbolesInfectados = focus.cantidadArbolesInfectados,
                    mensajeAlerta = "Precaución: Te encuentras a ${distMeters.toInt()} m de un Foco Activo con ${focus.cantidadArbolesInfectados} árboles infestados.",
                    latitud = focus.latitudCentro,
                    longitud = focus.longitudCentro
                )
            } else {
                null
            }
        }.minByOrNull { it.distanciaMetros }

        // 2. Verificar árboles con infestación activa en radio circular de 100 m
        val nearbyTrees = state.treeProgressionList.mapNotNull { prog ->
            if (prog.currentStatus != ClassificationOutcome.INFESTADO) return@mapNotNull null
            val latestCapture = prog.captures.firstOrNull()
            val treeLat = prog.tree.latitud ?: latestCapture?.latitud ?: return@mapNotNull null
            val treeLon = prog.tree.longitud ?: latestCapture?.longitud ?: return@mapNotNull null
            val distMeters = haversineDistanceMeters(
                state.captureLatitude,
                state.captureLongitude,
                treeLat,
                treeLon
            )
            if (distMeters <= 100.0) {
                NearbyInfestedTreeAlert(
                    idArbol = prog.tree.idArbol,
                    nombreArbol = prog.tree.nombreEspecifico,
                    latitud = treeLat,
                    longitud = treeLon,
                    distanciaMetros = distMeters
                )
            } else {
                null
            }
        }.sortedBy { it.distanciaMetros }

        val effectiveProximityAlert = closestHotspot ?: nearbyTrees.firstOrNull()?.let { closestTree ->
            ProximityAlertUi(
                idFoco = closestTree.idArbol,
                distanciaMetros = closestTree.distanciaMetros,
                cantidadArbolesInfectados = nearbyTrees.size,
                mensajeAlerta = "Precaución: '${closestTree.nombreArbol}' con infestación está a ${closestTree.distanciaMetros.toInt()} m de tu ubicación.",
                latitud = closestTree.latitud,
                longitud = closestTree.longitud
            )
        }

        _uiState.update {
            it.copy(
                nearbyInfestedTrees = nearbyTrees,
                proximityAlert = effectiveProximityAlert
            )
        }
    }

    private fun haversineDistanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val earthRadius = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    fun saveCurrentAnalysisToRoom() {
        val state = _uiState.value
        val clf = state.classificationResult ?: return
        val chrom = state.chromaticResult ?: return

        if (state.isInspectingHistoricalRecord) {
            viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        isSavingOffline = true,
                        errorMessage = null,
                        statusBannerMessage = null
                    )
                }
                val result = forestMonitoringRepository.updateDetectionObservations(
                    idAnalisis = chrom.analysisId,
                    observaciones = state.fieldObservations
                )
                result.fold(
                    onSuccess = {
                        _uiState.update {
                            it.copy(
                                isSavingOffline = false,
                                statusBannerMessage = "Observaciones actualizadas."
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isSavingOffline = false,
                                errorMessage = "No se pudieron actualizar las observaciones: ${error.localizedMessage ?: "Error local"}"
                            )
                        }
                    }
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSavingOffline = true,
                    errorMessage = null,
                    statusBannerMessage = null
                )
            }

            val session = authRepository.getCurrentSession()
            val brigadistaId = session?.brigadistaId ?: "a0000000-0000-0000-0000-000000000002"
            val idDispositivo = DeviceIdentifierProvider.getHardwareDeviceUuid(appContext)

            val isCorteza = state.sampleType == SampleType.CORTEZA
            val targetAssociation: TargetAssociation = if (isCorteza) {
                val existingTree = state.availableTrees.firstOrNull { it.idArbol == state.selectedTreeId }
                if (state.associationMode == TargetAssociationMode.EXISTENTE && existingTree != null) {
                    TargetAssociation.CortezaTarget.ExistingTree(
                        idArbol = existingTree.idArbol,
                        nombreEspecifico = existingTree.nombreEspecifico,
                        especie = existingTree.especie
                    )
                } else {
                    TargetAssociation.CortezaTarget.NewTree(
                        nombreArbolNuevo = state.newTreeName.trim().ifBlank { "Árbol Chapultepec" },
                        especieArbolNuevo = state.newTreeSpecies.trim().ifBlank { PREDEFINED_TREE_SPECIES.first() },
                        descripcion = state.fieldObservations.trim()
                    )
                }
            } else {
                val existingZone = state.availableZones.firstOrNull { it.idZona == state.selectedZoneId }
                if (state.associationMode == TargetAssociationMode.EXISTENTE && existingZone != null) {
                    TargetAssociation.FollajeTarget.ExistingZone(
                        idZona = existingZone.idZona,
                        nombreZona = existingZone.nombreZona,
                        radioMetros = existingZone.radioMetros
                    )
                } else {
                    TargetAssociation.FollajeTarget.NewZone(
                        nombreZonaNuevo = state.newZoneName.trim().ifBlank { "Zona Follaje Chapultepec" },
                        radioMetrosZona = state.newZoneRadiusMeters.toDoubleOrNull() ?: 50.0,
                        descripcion = state.fieldObservations.trim()
                    )
                }
            }

            val saveResult = forestMonitoringRepository.saveFieldDetectionOffline(
                brigadistaId = brigadistaId,
                idDispositivo = idDispositivo,
                sampleType = state.sampleType,
                classificationResult = clf,
                chromaticResult = chrom,
                latitud = state.captureLatitude,
                longitud = state.captureLongitude,
                precisionGpsMetros = state.captureGpsAccuracyMeters,
                observaciones = state.fieldObservations.trim(),
                targetAssociation = targetAssociation
            )

            saveResult.fold(
                onSuccess = { savedRecord ->
                    DeteccionesSyncWorker.enqueueImmediateSync(appContext)
                    val isCorteza = state.sampleType == SampleType.CORTEZA
                    val savedTreeId = (savedRecord.targetAssociation as? TargetAssociation.CortezaTarget.ExistingTree)?.idArbol
                        ?: state.selectedTreeId
                    val savedZoneId = (savedRecord.targetAssociation as? TargetAssociation.FollajeTarget.ExistingZone)?.idZona
                        ?: state.selectedZoneId
                    val savedTargetName = when (val assoc = savedRecord.targetAssociation) {
                        is TargetAssociation.CortezaTarget.ExistingTree -> assoc.nombreEspecifico
                        is TargetAssociation.CortezaTarget.NewTree -> assoc.nombreArbolNuevo
                        is TargetAssociation.FollajeTarget.ExistingZone -> assoc.nombreZona
                        is TargetAssociation.FollajeTarget.NewZone -> assoc.nombreZonaNuevo
                    }
                    _uiState.update {
                        it.copy(
                            isSavingOffline = false,
                            currentDestination = EcoVerdeMainDestination.INICIO,
                            homeWorkTab = if (isCorteza) HomeWorkTab.ARBOLES else HomeWorkTab.ZONAS_FOLLAJE,
                            wizardStep = WizardStep.MODALIDAD,
                            originalBitmap = null,
                            uncroppedBitmap = null,
                            segmentedOverlayBitmap = null,
                            classificationResult = null,
                            chromaticResult = null,
                            overlayOpacityAlpha = 0.0f,
                            isInspectingHistoricalRecord = false,
                            associationMode = if (isCorteza && savedTreeId != null) {
                                TargetAssociationMode.EXISTENTE
                            } else {
                                it.associationMode
                            },
                            selectedTreeId = if (isCorteza) (savedTreeId ?: it.selectedTreeId) else it.selectedTreeId,
                            selectedTreeDetailId = if (isCorteza) savedTreeId else null,
                            selectedZoneId = if (!isCorteza) (savedZoneId ?: it.selectedZoneId) else it.selectedZoneId,
                            selectedZoneDetailId = if (!isCorteza) savedZoneId else null,
                            statusBannerMessage = "Registro guardado correctamente en la sección de '$savedTargetName'"
                        )
                    }
                    checkProximityToHotspots()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSavingOffline = false,
                            errorMessage = error.message ?: SystemMessages.MSG18
                        )
                    }
                }
            )
        }
    }

    fun syncPendingDetectionsNow() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSyncingBackend = true,
                    errorMessage = null,
                    statusBannerMessage = null
                )
            }

            val result = forestMonitoringRepository.syncPendingDetectionsWithBackend()
            result.fold(
                onSuccess = { batchResult ->
                    val health = authRepository.checkBackendHealth().getOrDefault(true)
                    val msg = if (batchResult.errors.isEmpty()) {
                        "Sincronización completada (${batchResult.totalSynced}/${batchResult.totalPendingFound} registros sincronizados)."
                    } else {
                        "Sincronizados ${batchResult.totalSynced}/${batchResult.totalPendingFound}. Avisos: ${batchResult.errors.joinToString("; ")}"
                    }
                    _uiState.update {
                        it.copy(
                            isSyncingBackend = false,
                            isBackendReachable = health,
                            statusBannerMessage = msg
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSyncingBackend = false,
                            errorMessage = error.message ?: SystemMessages.MSG25
                        )
                    }
                }
            )
        }
    }

    fun inspectHistoricalRecord(record: SavedDetectionRecord) {
        currentAnalysisJob?.cancel()
        overlayRenderJob?.cancel()

        val targetTreeId = when (val assoc = record.targetAssociation) {
            is TargetAssociation.CortezaTarget.ExistingTree -> assoc.idArbol
            else -> null
        }
        val targetZoneId = when (val assoc = record.targetAssociation) {
            is TargetAssociation.FollajeTarget.ExistingZone -> assoc.idZona
            else -> null
        }
        val treeOrZoneName = when (val assoc = record.targetAssociation) {
            is TargetAssociation.CortezaTarget.ExistingTree -> assoc.nombreEspecifico
            is TargetAssociation.CortezaTarget.NewTree -> assoc.nombreArbolNuevo
            is TargetAssociation.FollajeTarget.ExistingZone -> assoc.nombreZona
            is TargetAssociation.FollajeTarget.NewZone -> assoc.nombreZonaNuevo
        }

        val recommendation = when (record.classificationOutcome) {
            ClassificationOutcome.INFESTADO ->
                "Atención prioritaria: Se detectan indicadores de daño activo."
            ClassificationOutcome.SANO ->
                "Continuar monitoreo preventivo ordinario."
            ClassificationOutcome.NO_DETERMINADO ->
                "Repetir captura con iluminación uniforme."
        }

        val clf = ClassificationResult(
            outcome = record.classificationOutcome,
            confidence = record.confidence,
            rawInfestationProbability = record.rawTfliteProbability,
            isOfflineExecution = record.isOfflineExecution,
            executionBadgeLabel = record.executionBadgeLabel,
            visualEvidencesDetected = emptyList(),
            recommendation = recommendation,
            inferenceTimeMs = 0L
        )

        val chromatic = record.chromaticResult ?: run {
            val isInfested = record.classificationOutcome == ClassificationOutcome.INFESTADO
            val pctDanado = if (isInfested) 0.65 else 0.05
            val pctRegular = if (isInfested) 0.20 else 0.10
            val pctSano = if (isInfested) 0.15 else 0.85
            val bands = listOf(
                DiagnosticCategory.DANADO,
                DiagnosticCategory.REGULAR,
                DiagnosticCategory.SANO,
                DiagnosticCategory.INCIERTO
            ).map { cat ->
                val ratio = when (cat) {
                    DiagnosticCategory.DANADO -> pctDanado
                    DiagnosticCategory.REGULAR -> pctRegular
                    DiagnosticCategory.SANO -> pctSano
                    DiagnosticCategory.INCIERTO -> 0.0
                }
                ColorBandResult(
                    category = cat,
                    pixelCount = (ratio * 10000).toInt(),
                    ratio = ratio,
                    percentage = (ratio * 100.0).toFloat(),
                    label = cat.biologicalSignalDescription(record.sampleType),
                    colorHex = cat.officialHexColor(record.sampleType)
                )
            }
            ChromaticAnalysisResult(
                analysisId = record.idAnalisis,
                sampleType = record.sampleType,
                originalImagePath = record.originalImagePath,
                segmentedOverlayImagePath = record.overlayImagePath,
                totalPixelsAnalyzed = 10000,
                bands = bands,
                pctSano = pctSano,
                pctRegular = pctRegular,
                pctDanado = pctDanado,
                pctIncierto = 0.0,
                biologicalSeverityScore = (pctDanado * 100.0),
                severityLevel = if (isInfested) BiologicalSeverityLevel.ALTA else BiologicalSeverityLevel.BAJA,
                dominantCategory = if (isInfested) DiagnosticCategory.DANADO else DiagnosticCategory.SANO,
                timestampMillis = record.fechaCapturaMillis
            )
        }

        // Limpiar inmediatamente el bitmap anterior para evitar empalmes visuales
        _uiState.update {
            it.copy(
                isProcessingVision = true,
                currentDestination = EcoVerdeMainDestination.ANALIZAR,
                wizardStep = WizardStep.RESULTADOS,
                sampleType = record.sampleType,
                selectedTab = if (record.sampleType == SampleType.FOLLAJE) AnalysisResultTab.ANALISIS_COLOR else AnalysisResultTab.CLASIFICACION,
                selectedCategoryFilter = null,
                overlayOpacityAlpha = if (record.sampleType == SampleType.FOLLAJE) 0.70f else 0.0f,
                isInspectingHistoricalRecord = true,
                classificationResult = clf,
                chromaticResult = chromatic,
                fieldObservations = record.observaciones ?: "",
                selectedTreeId = targetTreeId ?: it.selectedTreeId,
                newTreeName = if (record.sampleType == SampleType.CORTEZA) treeOrZoneName else it.newTreeName,
                selectedZoneId = targetZoneId ?: it.selectedZoneId,
                newZoneName = if (record.sampleType == SampleType.FOLLAJE) treeOrZoneName else it.newZoneName,
                captureLatitude = record.latitud,
                captureLongitude = record.longitud,
                captureLocationSourceLabel = "Registro histórico (${record.fechaCapturaIso.take(16).replace('T', ' ')})",
                originalBitmap = null,
                uncroppedBitmap = null,
                segmentedOverlayBitmap = null,
                errorMessage = null,
                statusBannerMessage = null
            )
        }

        currentAnalysisJob = viewModelScope.launch(Dispatchers.IO) {
            val file = forestMonitoringRepository.resolveEvidenceFile(record.originalImagePath)
                ?: File(record.originalImagePath)
            var origBmp: Bitmap? = if (file.exists()) {
                try {
                    val opts = BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    BitmapFactory.decodeFile(file.absolutePath, opts)
                } catch (e: Exception) {
                    Log.w("AnalysisResultViewModel", "Error decodificando imagen original histórica", e)
                    null
                }
            } else null

            var ovlBmp: Bitmap? = null
            val ovlFile = File(record.overlayImagePath)
            if (ovlFile.exists()) {
                try {
                    val opts = BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    ovlBmp = BitmapFactory.decodeFile(ovlFile.absolutePath, opts)
                } catch (e: Exception) {
                    Log.w("AnalysisResultViewModel", "Error decodificando overlay histórico", e)
                    null
                }
            }

            if (origBmp == null) {
                origBmp = generateSyntheticSampleBitmap(
                    record.sampleType,
                    record.classificationOutcome == ClassificationOutcome.INFESTADO
                )
            }

            if (ovlBmp == null) {
                try {
                    val masks = openCvColorSegmenter.segmentBitmapToMasks(origBmp, record.sampleType)
                    try {
                        ovlBmp = imageOverlayGenerator.generateOverlayBitmap(
                            segmentationMasks = masks,
                            sampleType = record.sampleType,
                            alphaOpacity = 0.70f,
                            isolatedCategory = null
                        )
                    } finally {
                        masks.releaseMasks()
                    }
                } catch (e: Exception) {
                    Log.w("AnalysisResultViewModel", "Error generando overlay fallback para histórico", e)
                }
            }

            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        isProcessingVision = false,
                        originalBitmap = origBmp,
                        uncroppedBitmap = origBmp,
                        segmentedOverlayBitmap = ovlBmp
                    )
                }
            }
        }
    }

    fun clearAllLocalDatabaseFromProfile() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    errorMessage = null,
                    statusBannerMessage = null
                )
            }
            val result = forestMonitoringRepository.clearAllLocalDatabase()
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            selectedTreeId = null,
                            selectedTreeDetailId = null,
                            selectedZoneId = null,
                            selectedMapTree = null,
                            associationMode = TargetAssociationMode.NUEVO,
                            availableTrees = emptyList(),
                            availableZones = emptyList(),
                            allRecords = emptyList(),
                            activeHotspots = emptyList(),
                            proximityAlert = null,
                            treeSearchQuery = "",
                            daySummary = DailyJourneySummary(
                                totalCapturasHoy = 0,
                                totalSanosHoy = 0,
                                totalInfestadosHoy = 0,
                                totalNoDeterminadosHoy = 0,
                                pendientesSincronizacion = 0,
                                erroresSincronizacion = 0,
                                alertaEstacionalActiva = false,
                                mensajeAlertaEstacional = null
                            ),
                            statusBannerMessage = "Base de datos local SQLite (árboles, análisis, zonas e imágenes) borrada correctamente."
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            errorMessage = error.message ?: "No fue posible borrar la base de datos local."
                        )
                    }
                }
            )
        }
    }

    fun deleteTreeSection(treeId: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    errorMessage = null,
                    statusBannerMessage = null
                )
            }
            val result = forestMonitoringRepository.deleteTree(treeId)
            result.fold(
                onSuccess = {
                    val remainingTrees = _uiState.value.availableTrees.filterNot { t -> t.idArbol == treeId }
                    val remainingRecords = _uiState.value.allRecords.filterNot { r ->
                        when (val assoc = r.targetAssociation) {
                            is TargetAssociation.CortezaTarget.ExistingTree -> assoc.idArbol == treeId
                            else -> false
                        }
                    }
                    val treesProg = AnalysisResultUiState.computeTreeProgressions(remainingTrees, remainingRecords)
                    _uiState.update {
                        it.copy(
                            selectedTreeDetailId = if (it.selectedTreeDetailId == treeId) null else it.selectedTreeDetailId,
                            selectedTreeId = if (it.selectedTreeId == treeId) remainingTrees.firstOrNull()?.idArbol else it.selectedTreeId,
                            availableTrees = remainingTrees,
                            allRecords = remainingRecords,
                            treeProgressionList = treesProg,
                            statusBannerMessage = "Árbol eliminado correctamente de la base de datos."
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            errorMessage = error.message ?: "No fue posible eliminar el árbol."
                        )
                    }
                }
            )
        }
    }

    fun updateTreeObservations(treeId: String, newObservations: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingOffline = true, errorMessage = null) }
            val result = forestMonitoringRepository.updateTreeDescription(treeId, newObservations)
            result.fold(
                onSuccess = { updatedTree ->
                    val updatedList = _uiState.value.availableTrees.map { t ->
                        if (t.idArbol == treeId) t.copy(descripcion = newObservations.trim()) else t
                    }
                    val treesProg = AnalysisResultUiState.computeTreeProgressions(updatedList, _uiState.value.allRecords)
                    _uiState.update { state ->
                        state.copy(
                            isSavingOffline = false,
                            availableTrees = updatedList,
                            treeProgressionList = treesProg,
                            fieldObservations = newObservations.trim(),
                            statusBannerMessage = "Observaciones de '${updatedTree.nombreEspecifico}' actualizadas correctamente."
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSavingOffline = false,
                            errorMessage = error.message ?: "No se pudieron actualizar las observaciones del árbol."
                        )
                    }
                }
            )
        }
    }

    fun deleteZoneSection(zoneId: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    errorMessage = null,
                    statusBannerMessage = null
                )
            }
            val result = forestMonitoringRepository.deleteZonaFollaje(zoneId)
            result.fold(
                onSuccess = {
                    val remainingZones = _uiState.value.availableZones.filterNot { z -> z.idZona == zoneId }
                    val remainingRecords = _uiState.value.allRecords.filterNot { r ->
                        when (val assoc = r.targetAssociation) {
                            is TargetAssociation.FollajeTarget.ExistingZone -> assoc.idZona == zoneId
                            else -> false
                        }
                    }
                    val zonesProg = AnalysisResultUiState.computeZoneProgressions(remainingZones, remainingRecords)
                    _uiState.update {
                        it.copy(
                            selectedZoneDetailId = if (it.selectedZoneDetailId == zoneId) null else it.selectedZoneDetailId,
                            selectedZoneId = if (it.selectedZoneId == zoneId) remainingZones.firstOrNull()?.idZona else it.selectedZoneId,
                            availableZones = remainingZones,
                            allRecords = remainingRecords,
                            zoneProgressionList = zonesProg,
                            statusBannerMessage = "Zona de follaje eliminada correctamente de la base de datos."
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            errorMessage = error.message ?: "No fue posible eliminar la zona de follaje."
                        )
                    }
                }
            )
        }
    }

    fun updateZoneObservations(zoneId: String, newObservations: String) {
        val zone = _uiState.value.availableZones.firstOrNull { it.idZona == zoneId } ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingOffline = true, errorMessage = null) }
            val result = forestMonitoringRepository.updateZonaFollaje(
                idZona = zoneId,
                nombreZona = zone.nombreZona,
                radioMetros = zone.radioMetros,
                nuevaDescripcion = newObservations.trim()
            )
            result.fold(
                onSuccess = {
                    val updatedList = _uiState.value.availableZones.map { z ->
                        if (z.idZona == zoneId) z.copy(descripcion = newObservations.trim()) else z
                    }
                    val zonesProg = AnalysisResultUiState.computeZoneProgressions(updatedList, _uiState.value.allRecords)
                    _uiState.update { state ->
                        state.copy(
                            isSavingOffline = false,
                            availableZones = updatedList,
                            zoneProgressionList = zonesProg,
                            fieldObservations = newObservations.trim(),
                            statusBannerMessage = "Observaciones de '${zone.nombreZona}' actualizadas correctamente."
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSavingOffline = false,
                            errorMessage = error.message ?: "No se pudieron actualizar las observaciones de la zona de follaje."
                        )
                    }
                }
            )
        }
    }

    private suspend fun persistAnalysisImagesToDisk(
        analysisId: String,
        original: Bitmap,
        overlay: Bitmap,
        latitude: Double,
        longitude: Double,
        captureTimestampIso: String?,
        cameraModel: String?
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        val dir = File(appContext.filesDir, "chromatic_analyses").apply {
            if (!exists()) mkdirs()
        }
        val origFile = File(dir, "orig_$analysisId.jpg")
        val ovlFile = File(dir, "ovl_$analysisId.jpg")

        FileOutputStream(origFile).use { out ->
            original.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        ImageMetadataAndLocationHelper.writeExifMetadataToFile(
            targetJpgFile = origFile,
            latitude = latitude,
            longitude = longitude,
            captureTimestampIso = captureTimestampIso,
            cameraModel = cameraModel
        )
        FileOutputStream(ovlFile).use { out ->
            overlay.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        origFile.absolutePath to ovlFile.absolutePath
    }

    private fun buildFallbackClassificationFromChromatic(
        pctDanado: Double,
        pctRegular: Double,
        elapsedMs: Long
    ): ClassificationResult {
        val prob = (pctDanado * 1.3 + pctRegular * 0.6).coerceIn(0.05, 0.96)
        val (outcome, conf) = ClassificationOutcome.evaluateFromProbability(prob)
        val recommendation = when (outcome) {
            ClassificationOutcome.INFESTADO ->
                "Atención prioritaria NOM-019-SEMARNAT-2017: Se detectan indicadores cromáticos de daño activo."
            ClassificationOutcome.SANO ->
                "Continuar monitoreo preventivo ordinario en el Bosque de Chapultepec."
            ClassificationOutcome.NO_DETERMINADO ->
                "Repetir captura a menor distancia y con iluminación uniforme."
        }
        return ClassificationResult(
            outcome = outcome,
            confidence = conf,
            rawInfestationProbability = prob,
            isOfflineExecution = true,
            executionBadgeLabel = "LOCAL",
            visualEvidencesDetected = emptyList(),
            recommendation = recommendation,
            inferenceTimeMs = elapsedMs
        )
    }

    private fun generateSyntheticSampleBitmap(
        sampleType: SampleType,
        simulateInfested: Boolean
    ): Bitmap {
        val width = 300
        val height = 300
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        if (sampleType == SampleType.CORTEZA) {
            paint.color = Color.rgb(108, 102, 96)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

            if (simulateInfested) {
                paint.color = Color.rgb(155, 78, 28)
                canvas.drawOval(30f, 35f, 225f, 165f, paint)
                canvas.drawOval(75f, 155f, 270f, 260f, paint)

                paint.color = Color.rgb(235, 228, 205)
                canvas.drawCircle(115f, 95f, 32f, paint)
                canvas.drawCircle(195f, 200f, 28f, paint)
            } else {
                paint.color = Color.rgb(95, 92, 88)
                for (x in 20 until 280 step 35) {
                    canvas.drawRect(x.toFloat(), 0f, (x + 16).toFloat(), height.toFloat(), paint)
                }
            }
        } else {
            paint.color = Color.rgb(46, 125, 50)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

            if (simulateInfested) {
                paint.color = Color.rgb(165, 205, 45)
                canvas.drawOval(25f, 30f, 195f, 175f, paint)

                paint.color = Color.rgb(165, 42, 42)
                canvas.drawOval(110f, 115f, 275f, 265f, paint)
            }
        }
        return bmp
    }
}
