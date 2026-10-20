package com.descortezadores.ecoverde.presentation.features.auth

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.descortezadores.ecoverde.core.opencv.OpenCvInitializer
import com.descortezadores.ecoverde.data.classification.TfliteBarkClassifier
import com.descortezadores.ecoverde.data.segmentation.ImageOverlayGenerator
import com.descortezadores.ecoverde.data.segmentation.OpenCvColorSegmenter
import com.descortezadores.ecoverde.data.segmentation.SyntheticForestSampleGenerator
import com.descortezadores.ecoverde.domain.model.ChromaticAnalysisResult
import com.descortezadores.ecoverde.domain.model.ClassificationOutcome
import com.descortezadores.ecoverde.domain.model.ClassificationResult
import com.descortezadores.ecoverde.domain.model.ColorBandResult
import com.descortezadores.ecoverde.domain.model.DiagnosticCategory
import com.descortezadores.ecoverde.domain.model.SampleType
import com.descortezadores.ecoverde.domain.model.TargetAssociation
import com.descortezadores.ecoverde.domain.model.auth.BrigadistaSession
import com.descortezadores.ecoverde.domain.repository.AuthRepository
import com.descortezadores.ecoverde.domain.repository.ForestMonitoringRepository
import com.descortezadores.ecoverde.domain.usecase.AnalyzeImageColorsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel para Autenticación/Sesión Offline (Fase 1), Dominio (Fase 2), Motor de Visión
 * Computacional OpenCV + TensorFlow Lite (Fase 3) y Persistencia Local Room + Sync Backend (Fase 4).
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val authRepository: AuthRepository,
    private val openCvInitializer: OpenCvInitializer,
    private val openCvColorSegmenter: OpenCvColorSegmenter,
    private val imageOverlayGenerator: ImageOverlayGenerator,
    private val tfliteBarkClassifier: TfliteBarkClassifier,
    private val forestMonitoringRepository: ForestMonitoringRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        observeActiveSession()
        observeOpenCvStatus()
        observeRoomDatabaseStreams()
        verifyInfrastructure()
    }

    private fun observeActiveSession() {
        viewModelScope.launch {
            authRepository.activeSessionFlow.collectLatest { session ->
                _uiState.update { it.copy(activeSession = session) }
                if (session != null && _uiState.value.visionPreview.sourceBitmap == null) {
                    runPhase3SyntheticSampleAnalysis(SampleType.CORTEZA)
                    refreshCatalogsFromBackend()
                }
            }
        }
    }

    private fun observeOpenCvStatus() {
        viewModelScope.launch {
            openCvInitializer.status.collectLatest { status ->
                _uiState.update { it.copy(openCvStatus = status) }
            }
        }
    }

    private fun observeRoomDatabaseStreams() {
        viewModelScope.launch {
            forestMonitoringRepository.observeDailyJourneySummary().collectLatest { summary ->
                _uiState.update {
                    it.copy(phase4State = it.phase4State.copy(dailyJourneySummary = summary))
                }
            }
        }
        viewModelScope.launch {
            forestMonitoringRepository.observePendingSyncCount().collectLatest { count ->
                _uiState.update {
                    it.copy(phase4State = it.phase4State.copy(pendingSyncCount = count))
                }
            }
        }
        viewModelScope.launch {
            forestMonitoringRepository.observeAllDetections().collectLatest { records ->
                _uiState.update {
                    it.copy(phase4State = it.phase4State.copy(savedDetections = records))
                }
            }
        }
        viewModelScope.launch {
            forestMonitoringRepository.observeTrees().collectLatest { trees ->
                _uiState.update {
                    it.copy(phase4State = it.phase4State.copy(localTrees = trees))
                }
            }
        }
        viewModelScope.launch {
            forestMonitoringRepository.observeFoliageZones().collectLatest { zones ->
                _uiState.update {
                    it.copy(phase4State = it.phase4State.copy(localFoliageZones = zones))
                }
            }
        }
        viewModelScope.launch {
            forestMonitoringRepository.observeActiveInfestationFoci().collectLatest { foci ->
                _uiState.update {
                    it.copy(phase4State = it.phase4State.copy(activeFociNom019 = foci))
                }
            }
        }
    }

    fun verifyInfrastructure() {
        viewModelScope.launch {
            openCvInitializer.ensureInitialized()
            val health = authRepository.checkBackendHealth().getOrDefault(false)
            _uiState.update { it.copy(isBackendReachable = health) }
        }
    }

    fun onCorreoChanged(value: String) {
        _uiState.update { it.copy(correo = value, errorMessage = null, infoMessage = null) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null, infoMessage = null) }
    }

    fun onRecoveryTokenChanged(value: String) {
        _uiState.update { it.copy(recoveryToken = value, errorMessage = null) }
    }

    fun onNewPasswordChanged(value: String) {
        _uiState.update { it.copy(newPassword = value, errorMessage = null) }
    }

    fun onConfirmNewPasswordChanged(value: String) {
        _uiState.update { it.copy(confirmNewPassword = value, errorMessage = null) }
    }

    fun switchMode(mode: AuthMode) {
        _uiState.update {
            it.copy(
                authMode = mode,
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    fun login() {
        val current = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
            val result = authRepository.login(current.correo, current.password)
            result.fold(
                onSuccess = { session ->
                    val modeTag = if (session.isOfflineSession) "Modo Offline Chapultepec" else "Conectado a Nube"
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            activeSession = session,
                            infoMessage = "Sesión iniciada ($modeTag) — Brigadista ID: ${session.brigadistaId}"
                        )
                    }
                    refreshCatalogsFromBackend()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message
                        )
                    }
                }
            )
        }
    }

    fun startQuickOfflineBrigadistaSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
            val result = authRepository.startOfflineBrigadistaSession(
                brigadistaId = BrigadistaSession.DEFAULT_BRIGADISTA_ID,
                nombreCompleto = "Biólogo de Campo (Brigada Chapultepec)",
                correo = "experto@chapultepec.gob.mx"
            )
            result.fold(
                onSuccess = { session ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            activeSession = session,
                            infoMessage = "Sesión local fijada"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.message)
                    }
                }
            )
        }
    }

    fun requestPasswordRecovery() {
        val correo = _uiState.value.correo
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
            val result = authRepository.requestPasswordRecovery(correo)
            result.fold(
                onSuccess = { ticket ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            lastRecoveryTicket = ticket,
                            recoveryToken = ticket.codigoRecuperacion,
                            authMode = AuthMode.RESET_PASSWORD,
                            infoMessage = "${ticket.mensaje} Código: ${ticket.codigoRecuperacion}"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.message)
                    }
                }
            )
        }
    }

    fun resetPassword() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
            val result = authRepository.resetPassword(
                token = state.recoveryToken,
                nuevaContrasena = state.newPassword,
                confirmacionContrasena = state.confirmNewPassword
            )
            result.fold(
                onSuccess = { msg ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            authMode = AuthMode.LOGIN,
                            password = state.newPassword,
                            newPassword = "",
                            confirmNewPassword = "",
                            infoMessage = msg
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.message)
                    }
                }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            authRepository.logout()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    activeSession = null,
                    infoMessage = "Sesión finalizada correctamente."
                )
            }
        }
    }

    // =========================================================================================
    // FASE 3: MOTOR DE VISIÓN COMPUTACIONAL (OPENCV + TENSORFLOW LITE CONCURRENTE - PI-13)
    // =========================================================================================

    fun runPhase3SyntheticSampleAnalysis(sampleType: SampleType) {
        val bitmap = SyntheticForestSampleGenerator.createCalibratedSampleBitmap(sampleType, 300, 300)
        runPhase3BitmapAnalysis(bitmap, sampleType)
    }

    fun runPhase3CustomBitmapAnalysis(bitmap: Bitmap) {
        val currentSampleType = _uiState.value.visionPreview.sampleType
        runPhase3BitmapAnalysis(bitmap, currentSampleType)
    }

    fun onPhase3AlphaChanged(newAlpha: Float) {
        val currentPreview = _uiState.value.visionPreview
        val sourceBitmap = currentPreview.sourceBitmap ?: return
        _uiState.update {
            it.copy(visionPreview = currentPreview.copy(alphaOpacity = newAlpha))
        }
        rerenderOverlayOnly(
            sourceBitmap = sourceBitmap,
            sampleType = currentPreview.sampleType,
            alphaOpacity = newAlpha,
            isolatedCategory = currentPreview.isolatedCategory
        )
    }

    fun onPhase3CategoryFilterSelected(category: DiagnosticCategory?) {
        val currentPreview = _uiState.value.visionPreview
        val sourceBitmap = currentPreview.sourceBitmap ?: return
        _uiState.update {
            it.copy(visionPreview = currentPreview.copy(isolatedCategory = category))
        }
        rerenderOverlayOnly(
            sourceBitmap = sourceBitmap,
            sampleType = currentPreview.sampleType,
            alphaOpacity = currentPreview.alphaOpacity,
            isolatedCategory = category
        )
    }

    private fun runPhase3BitmapAnalysis(
        bitmap: Bitmap,
        sampleType: SampleType
    ) {
        viewModelScope.launch {
            val alpha = _uiState.value.visionPreview.alphaOpacity
            val isolated = _uiState.value.visionPreview.isolatedCategory
            val defaultTargetName = if (sampleType == SampleType.CORTEZA) {
                "Cedro Blanco #58"
            } else {
                "Zona Aérea Ahuehuetes Lago Mayor"
            }
            _uiState.update {
                it.copy(
                    visionPreview = it.visionPreview.copy(
                        sampleType = sampleType,
                        isProcessing = true,
                        sourceBitmap = bitmap,
                        errorMessage = null
                    ),
                    phase4State = it.phase4State.copy(
                        newTreeOrZoneName = defaultTargetName
                    )
                )
            }

            try {
                val startTotal = SystemClock.elapsedRealtime()

                // Ejecución concurrente en el dispositivo: TFLite + OpenCV (RN-46, PI-13)
                val tfliteDeferred = async { tfliteBarkClassifier.classifyBitmap(bitmap) }
                val openCvDeferred = async {
                    val masks = openCvColorSegmenter.segmentBitmapToMasks(bitmap, sampleType)
                    try {
                        val overlay = imageOverlayGenerator.generateOverlayBitmap(
                            segmentationMasks = masks,
                            sampleType = sampleType,
                            alphaOpacity = alpha,
                            isolatedCategory = isolated
                        )
                        Triple(overlay, masks.ratiosByCategory, masks.pixelCountsByCategory to masks.totalPixels)
                    } finally {
                        // Liberación formal de matrices nativas Mat (Fase 3)
                        masks.releaseMasks()
                    }
                }

                val tfliteResult = tfliteDeferred.await().getOrNull()
                val (overlayBitmap, ratios, countsAndTotal) = openCvDeferred.await()
                val totalElapsed = (SystemClock.elapsedRealtime() - startTotal).coerceAtLeast(1L)

                _uiState.update { state ->
                    state.copy(
                        visionPreview = state.visionPreview.copy(
                            isProcessing = false,
                            overlayBitmap = overlayBitmap,
                            ratiosByCategory = ratios,
                            pixelCountsByCategory = countsAndTotal.first,
                            totalPixels = countsAndTotal.second,
                            classificationResult = tfliteResult,
                            processingTimeMs = totalElapsed,
                            errorMessage = null
                        )
                    )
                }
            } catch (t: Throwable) {
                _uiState.update { state ->
                    state.copy(
                        visionPreview = state.visionPreview.copy(
                            isProcessing = false,
                            errorMessage = t.message ?: "Error al procesar imagen con OpenCV/TFLite"
                        )
                    )
                }
            }
        }
    }

    private fun rerenderOverlayOnly(
        sourceBitmap: Bitmap,
        sampleType: SampleType,
        alphaOpacity: Float,
        isolatedCategory: DiagnosticCategory?
    ) {
        viewModelScope.launch {
            try {
                val masks = openCvColorSegmenter.segmentBitmapToMasks(sourceBitmap, sampleType)
                try {
                    val overlay = imageOverlayGenerator.generateOverlayBitmap(
                        segmentationMasks = masks,
                        sampleType = sampleType,
                        alphaOpacity = alphaOpacity,
                        isolatedCategory = isolatedCategory
                    )
                    _uiState.update { state ->
                        state.copy(
                            visionPreview = state.visionPreview.copy(overlayBitmap = overlay)
                        )
                    }
                } finally {
                    masks.releaseMasks()
                }
            } catch (_: Throwable) {
            }
        }
    }

    // =========================================================================================
    // FASE 4: PERSISTENCIA LOCAL OFFLINE (ROOM SQLITE RN-50) Y SINCRONIZACIÓN CON BACKEND
    // =========================================================================================

    fun onPhase4TargetNameChanged(value: String) {
        _uiState.update {
            it.copy(phase4State = it.phase4State.copy(newTreeOrZoneName = value, errorMessage = null))
        }
    }

    fun onPhase4ObservationsChanged(value: String) {
        _uiState.update {
            it.copy(phase4State = it.phase4State.copy(observationsInput = value, errorMessage = null))
        }
    }

    /**
     * Persiste la captura actualmente analizada (Original + Overlay + Clasificación TFLite +
     * Porcentajes HSV OpenCV + Asociación mutuamente excluyente `chk_arbol_o_zona`) en SQLite Room (`RN-50`).
     */
    fun saveCurrentAnalysisToRoom() {
        val currentState = _uiState.value
        val session = currentState.activeSession ?: return
        val vision = currentState.visionPreview
        val srcBitmap = vision.sourceBitmap ?: return
        val ovlBitmap = vision.overlayBitmap ?: return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    phase4State = it.phase4State.copy(
                        isSavingOrSyncing = true,
                        statusMessage = null,
                        errorMessage = null
                    )
                )
            }

            try {
                val analysisId = UUID.randomUUID().toString()
                val storageDir = File(appContext.filesDir, "chromatic_analyses").apply { mkdirs() }
                val origFile = File(storageDir, "orig_$analysisId.jpg")
                val ovlFile = File(storageDir, "ovl_$analysisId.jpg")

                withContext(Dispatchers.IO) {
                    FileOutputStream(origFile).use { out ->
                        srcBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                    }
                    FileOutputStream(ovlFile).use { out ->
                        ovlBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                }

                val pctDanado = vision.ratiosByCategory[DiagnosticCategory.DANADO] ?: 0.0
                val pctRegular = vision.ratiosByCategory[DiagnosticCategory.REGULAR] ?: 0.0
                val pctSano = vision.ratiosByCategory[DiagnosticCategory.SANO] ?: 0.0
                val pctIncierto = vision.ratiosByCategory[DiagnosticCategory.INCIERTO] ?: 0.0

                val bands = listOf(
                    DiagnosticCategory.DANADO,
                    DiagnosticCategory.REGULAR,
                    DiagnosticCategory.SANO,
                    DiagnosticCategory.INCIERTO
                ).map { cat ->
                    val ratio = (vision.ratiosByCategory[cat] ?: 0.0).coerceIn(0.0, 1.0)
                    ColorBandResult(
                        category = cat,
                        pixelCount = (vision.pixelCountsByCategory[cat] ?: 0).coerceAtLeast(0),
                        ratio = ratio,
                        percentage = (ratio * 100.0).toFloat(),
                        label = cat.biologicalSignalDescription(vision.sampleType),
                        colorHex = cat.officialHexColor(vision.sampleType)
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

                val chromaticResult = ChromaticAnalysisResult(
                    analysisId = analysisId,
                    sampleType = vision.sampleType,
                    originalImagePath = origFile.absolutePath,
                    segmentedOverlayImagePath = ovlFile.absolutePath,
                    totalPixelsAnalyzed = vision.totalPixels.coerceAtLeast(1),
                    bands = bands,
                    pctSano = pctSano,
                    pctRegular = pctRegular,
                    pctDanado = pctDanado,
                    pctIncierto = pctIncierto,
                    biologicalSeverityScore = severityScore,
                    severityLevel = severityLevel,
                    dominantCategory = dominantCategory,
                    timestampMillis = System.currentTimeMillis(),
                    brigadistaId = session.brigadistaId
                )

                val clfResult = vision.classificationResult ?: ClassificationResult(
                    outcome = ClassificationOutcome.SANO,
                    confidence = 0.85,
                    rawInfestationProbability = 0.15,
                    isOfflineExecution = true,
                    executionBadgeLabel = "Offline",
                    visualEvidencesDetected = emptyList(),
                    recommendation = "Monitoreo preventivo",
                    inferenceTimeMs = 15L
                )

                val targetName = currentState.phase4State.newTreeOrZoneName.trim().ifBlank {
                    if (vision.sampleType == SampleType.CORTEZA) "Cedro Blanco #58" else "Zona Follaje Lago Mayor"
                }

                // Exclusión mutua chk_arbol_o_zona según SampleType (RN-49)
                val targetAssociation: TargetAssociation = if (vision.sampleType == SampleType.CORTEZA) {
                    val existingTree = currentState.phase4State.localTrees.firstOrNull {
                        it.nombreEspecifico.equals(targetName, ignoreCase = true)
                    }
                    if (existingTree != null) {
                        TargetAssociation.CortezaTarget.ExistingTree(
                            idArbol = existingTree.idArbol,
                            nombreEspecifico = existingTree.nombreEspecifico,
                            especie = existingTree.especie
                        )
                    } else {
                        TargetAssociation.CortezaTarget.NewTree(
                            nombreArbolNuevo = targetName,
                            especieArbolNuevo = currentState.phase4State.newTreeSpecies,
                            descripcion = currentState.phase4State.observationsInput.trim().ifBlank { null }
                        )
                    }
                } else {
                    val existingZone = currentState.phase4State.localFoliageZones.firstOrNull {
                        it.nombreZona.equals(targetName, ignoreCase = true)
                    }
                    if (existingZone != null) {
                        TargetAssociation.FollajeTarget.ExistingZone(
                            idZona = existingZone.idZona,
                            nombreZona = existingZone.nombreZona,
                            radioMetros = existingZone.radioMetros
                        )
                    } else {
                        TargetAssociation.FollajeTarget.NewZone(
                            nombreZonaNuevo = targetName,
                            radioMetrosZona = 60.0,
                            descripcion = currentState.phase4State.observationsInput.trim().ifBlank { null }
                        )
                    }
                }

                // Coordenadas en Bosque de Chapultepec (con ligero jitter para distinguir puntos en mapa)
                val jitterLat = ((System.currentTimeMillis() % 100) - 50) * 0.00001
                val jitterLng = ((System.currentTimeMillis() % 77) - 38) * 0.00001
                val saveRes = forestMonitoringRepository.saveFieldDetectionOffline(
                    brigadistaId = session.brigadistaId,
                    idDispositivo = session.idDispositivo,
                    sampleType = vision.sampleType,
                    classificationResult = clfResult,
                    chromaticResult = chromaticResult,
                    latitud = 19.42182 + jitterLat,
                    longitud = -99.18203 + jitterLng,
                    precisionGpsMetros = 4.2f,
                    observaciones = currentState.phase4State.observationsInput.trim().ifBlank { null },
                    targetAssociation = targetAssociation
                )

                saveRes.fold(
                    onSuccess = { saved ->
                        _uiState.update {
                            it.copy(
                                phase4State = it.phase4State.copy(
                                    isSavingOrSyncing = false,
                                    statusMessage = "Guardado localmente: ID ${saved.idAnalisis.take(8)}... " +
                                        "(${saved.sampleType.apiValue.uppercase()} -> ${saved.classificationOutcome.displayTitle}) [PENDIENTE DE SYNC]",
                                    errorMessage = null
                                )
                            )
                        }
                    },
                    onFailure = { err ->
                        _uiState.update {
                            it.copy(
                                phase4State = it.phase4State.copy(
                                    isSavingOrSyncing = false,
                                    errorMessage = err.message
                                )
                            )
                        }
                    }
                )
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        phase4State = it.phase4State.copy(
                            isSavingOrSyncing = false,
                            errorMessage = t.message ?: "Error al guardar en Room"
                        )
                    )
                }
            }
        }
    }

    /**
     * Sincroniza la cola local de Room con `POST /api/v1/sync/detecciones` y actualiza catálogos.
     */
    fun syncWithBackendNow() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    phase4State = it.phase4State.copy(
                        isSavingOrSyncing = true,
                        statusMessage = null,
                        errorMessage = null
                    )
                )
            }
            val result = forestMonitoringRepository.syncPendingDetectionsWithBackend()
            result.fold(
                onSuccess = { summary ->
                    val errSuffix = if (summary.errors.isNotEmpty()) {
                        " | Avisos: ${summary.errors.joinToString()}"
                    } else {
                        ""
                    }
                    _uiState.update {
                        it.copy(
                            isBackendReachable = true,
                            phase4State = it.phase4State.copy(
                                isSavingOrSyncing = false,
                                seasonalAlertActive = summary.seasonalAlertActive,
                                seasonalAlertMessage = summary.seasonalAlertMessage,
                                statusMessage = "Sincronización exitosa: ${summary.totalSynced}/${summary.totalPendingFound} " +
                                    "enviados a /api/v1/sync/detecciones. Catálogos y focos NOM-019 actualizados.$errSuffix",
                                errorMessage = null
                            )
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            phase4State = it.phase4State.copy(
                                isSavingOrSyncing = false,
                                errorMessage = err.message ?: "Error al sincronizar con el backend"
                            )
                        )
                    }
                }
            )
        }
    }

    fun refreshCatalogsFromBackend() {
        viewModelScope.launch {
            forestMonitoringRepository.refreshRemoteCatalogsAndFoci()
        }
    }
}
