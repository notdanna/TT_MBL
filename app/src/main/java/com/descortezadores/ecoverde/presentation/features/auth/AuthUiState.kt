package com.descortezadores.ecoverde.presentation.features.auth

import android.graphics.Bitmap
import com.descortezadores.ecoverde.core.opencv.OpenCvStatus
import com.descortezadores.ecoverde.domain.model.ClassificationResult
import com.descortezadores.ecoverde.domain.model.DailyJourneySummary
import com.descortezadores.ecoverde.domain.model.DiagnosticCategory
import com.descortezadores.ecoverde.domain.model.FoliageZoneRecord
import com.descortezadores.ecoverde.domain.model.InfestationFocus
import com.descortezadores.ecoverde.domain.model.SampleType
import com.descortezadores.ecoverde.domain.model.TreeRecord
import com.descortezadores.ecoverde.domain.model.auth.BrigadistaSession
import com.descortezadores.ecoverde.domain.model.auth.PasswordRecoveryTicket
import com.descortezadores.ecoverde.domain.repository.SavedDetectionRecord

enum class AuthMode {
    LOGIN,
    RECOVER_PASSWORD,
    RESET_PASSWORD
}

/**
 * Resultado en vivo del laboratorio de Visión Computacional (Fase 3: OpenCV + TFLite).
 */
data class Phase3VisionPreviewState(
    val sampleType: SampleType = SampleType.CORTEZA,
    val isProcessing: Boolean = false,
    val sourceBitmap: Bitmap? = null,
    val overlayBitmap: Bitmap? = null,
    val alphaOpacity: Float = 0.50f,
    val isolatedCategory: DiagnosticCategory? = null,
    val ratiosByCategory: Map<DiagnosticCategory, Double> = emptyMap(),
    val pixelCountsByCategory: Map<DiagnosticCategory, Int> = emptyMap(),
    val totalPixels: Int = 0,
    val classificationResult: ClassificationResult? = null,
    val processingTimeMs: Long = 0L,
    val errorMessage: String? = null
)

/**
 * Estado en vivo de la Fase 4: Persistencia Local Offline (Room SQLite - RN-50) y
 * Sincronización Bidireccional con el Backend (`/api/v1/sync/detecciones`, `/api/v1/arboles`, `/api/v1/estadisticas/focos`).
 */
data class Phase4PersistenceAndSyncState(
    val isSavingOrSyncing: Boolean = false,
    val dailyJourneySummary: DailyJourneySummary = DailyJourneySummary(
        totalCapturasHoy = 0,
        totalSanosHoy = 0,
        totalInfestadosHoy = 0,
        totalNoDeterminadosHoy = 0,
        pendientesSincronizacion = 0,
        erroresSincronizacion = 0,
        alertaEstacionalActiva = true,
        mensajeAlertaEstacional = null
    ),
    val pendingSyncCount: Int = 0,
    val savedDetections: List<SavedDetectionRecord> = emptyList(),
    val localTrees: List<TreeRecord> = emptyList(),
    val localFoliageZones: List<FoliageZoneRecord> = emptyList(),
    val activeFociNom019: List<InfestationFocus> = emptyList(),
    val seasonalAlertActive: Boolean = false,
    val seasonalAlertMessage: String? = null,
    val newTreeOrZoneName: String = "Cedro Blanco #58",
    val newTreeSpecies: String = "Cedro blanco (Cupressus lusitanica)",
    val observationsInput: String = "",
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

/**
 * Estado inmutable de la interfaz para Autenticación, Sesión Offline y Verificación Progresiva.
 */
data class AuthUiState(
    val correo: String = "experto@chapultepec.gob.mx",
    val password: String = "Password123!",
    val recoveryToken: String = "",
    val newPassword: String = "",
    val confirmNewPassword: String = "",
    val authMode: AuthMode = AuthMode.LOGIN,
    val isLoading: Boolean = false,
    val isCheckingSession: Boolean = true,
    val activeSession: BrigadistaSession? = null,
    val openCvStatus: OpenCvStatus = OpenCvStatus.Uninitialized,
    val isBackendReachable: Boolean = false,
    val lastRecoveryTicket: PasswordRecoveryTicket? = null,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val visionPreview: Phase3VisionPreviewState = Phase3VisionPreviewState(),
    val phase4State: Phase4PersistenceAndSyncState = Phase4PersistenceAndSyncState()
)
