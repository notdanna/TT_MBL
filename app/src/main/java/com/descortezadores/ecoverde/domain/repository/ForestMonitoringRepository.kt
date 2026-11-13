package com.descortezadores.ecoverde.domain.repository

import com.descortezadores.ecoverde.domain.model.ChromaticAnalysisResult
import com.descortezadores.ecoverde.domain.model.ClassificationOutcome
import com.descortezadores.ecoverde.domain.model.ClassificationResult
import com.descortezadores.ecoverde.domain.model.DailyJourneySummary
import com.descortezadores.ecoverde.domain.model.FoliageZoneRecord
import com.descortezadores.ecoverde.domain.model.InfestationFocus
import com.descortezadores.ecoverde.domain.model.SampleType
import com.descortezadores.ecoverde.domain.model.SyncStatus
import com.descortezadores.ecoverde.domain.model.TargetAssociation
import com.descortezadores.ecoverde.domain.model.TreeRecord
import kotlinx.coroutines.flow.Flow

/**
 * Modelo de dominio de un registro completo de análisis fitosanitario persistido en SQLite Room (`RN-50`).
 */
data class SavedDetectionRecord(
    val idAnalisis: String,
    val brigadistaId: String,
    val idDispositivo: String,
    val sampleType: SampleType,
    val classificationOutcome: ClassificationOutcome,
    val confidence: Double,
    val rawTfliteProbability: Double,
    val latitud: Double,
    val longitud: Double,
    val fechaCapturaIso: String,
    val fechaCapturaMillis: Long,
    val observaciones: String?,
    val targetAssociation: TargetAssociation,
    val originalImagePath: String,
    val overlayImagePath: String,
    val syncStatus: SyncStatus,
    val chromaticResult: ChromaticAnalysisResult?,
    val isOfflineExecution: Boolean = true,
    val executionBadgeLabel: String = "LOCAL"
)

/**
 * Resultado de un ciclo de sincronización por lotes (`POST /api/v1/sync/detecciones`).
 */
data class BatchSyncSummary(
    val totalPendingFound: Int,
    val totalSynced: Int,
    val syncedIds: List<String>,
    val errors: List<String>,
    val catalogsRefreshed: Boolean,
    val seasonalAlertActive: Boolean = false,
    val seasonalAlertMessage: String? = null
)

/**
 * Puerto de dominio para la persistencia local Offline-First (`RN-50`) y sincronización con el backend (`RF-15`).
 */
interface ForestMonitoringRepository {

    /**
     * Guarda de forma atómica una detección completa (Clasificación TFLite + Análisis Cromático OpenCV +
     * Coordenadas GPS + Asociación mutuamente excluyente Árbol/Zona `chk_arbol_o_zona`) en SQLite Room (`RN-50`).
     */
    suspend fun saveFieldDetectionOffline(
        brigadistaId: String,
        idDispositivo: String,
        sampleType: SampleType,
        classificationResult: ClassificationResult,
        chromaticResult: ChromaticAnalysisResult,
        latitud: Double,
        longitud: Double,
        precisionGpsMetros: Float? = null,
        observaciones: String? = null,
        targetAssociation: TargetAssociation
    ): Result<SavedDetectionRecord>

    /**
     * Observa todas las detecciones guardadas localmente en SQLite Room (`RF-17`).
     */
    fun observeAllDetections(): Flow<List<SavedDetectionRecord>>

    /**
     * Actualiza las observaciones de campo de un análisis ya guardado localmente en Room.
     */
    suspend fun updateDetectionObservations(idAnalisis: String, observaciones: String?): Result<Unit>

    /**
     * Observa el resumen estadístico de la jornada actual (`00:00` a `23:59`, `RF-12`, `RN-30`, `RN-31`).
     */
    fun observeDailyJourneySummary(): Flow<DailyJourneySummary>

    /**
     * Observa la cantidad de registros en cola pendientes de sincronización (`RF-15`, `RN-33`).
     */
    fun observePendingSyncCount(): Flow<Int>

    /**
     * Observa el catálogo local de árboles (`CU-M09`, `RF-21`, `RN-38`, `RN-39`).
     */
    fun observeTrees(
        searchQuery: String = "",
        healthStatusFilter: ClassificationOutcome? = null
    ): Flow<List<TreeRecord>>

    /**
     * Observa las zonas macro de follaje registradas en SQLite (`RN-47`).
     */
    fun observeFoliageZones(): Flow<List<FoliageZoneRecord>>

    /**
     * Observa los Focos de Infestación Activos NOM-019 almacenados en SQLite (`RF-14`, `RN-25`..`RN-29`).
     */
    fun observeActiveInfestationFoci(): Flow<List<InfestationFocus>>

    /**
     * Registra un árbol nuevo en SQLite local e intenta sincronizarlo con `POST /api/v1/arboles` si hay red (`RF-19`).
     */
    suspend fun registerTree(
        nombreEspecifico: String,
        especie: String,
        latitud: Double,
        longitud: Double,
        descripcion: String?
    ): Result<TreeRecord>

    /**
     * Actualiza las observaciones/descripción de un árbol existente (`PUT /api/v1/arboles/{id}`, `RF-23`, `RN-41`).
     */
    suspend fun updateTreeDescription(
        idArbol: String,
        nuevaDescripcion: String
    ): Result<TreeRecord>

    /**
     * Ejecuta la sincronización bidireccional con el backend (`POST /api/v1/sync/detecciones`,
     * descarga de árboles `/api/v1/arboles`, zonas `/api/v1/zonas-follaje`, focos `/api/v1/estadisticas/focos`
     * y alerta estacional `/api/v1/estadisticas/dashboard`).
     */
    suspend fun syncPendingDetectionsWithBackend(): Result<BatchSyncSummary>

    /**
     * Descarga y actualiza los catálogos remotos (árboles, zonas de follaje y focos activos NOM-019) en SQLite Room.
     */
    suspend fun refreshRemoteCatalogsAndFoci(): Result<Boolean>

    /**
     * Elimina un árbol individual (categoría) y todas sus capturas/fotografías asociadas,
     * tanto en la base de datos local SQLite Room como en el backend si hay conectividad.
     */
    suspend fun deleteTree(idArbol: String): Result<Unit>

    /**
     * Elimina una zona de follaje individual y todas sus capturas asociadas,
     * tanto en la base de datos local SQLite Room como en el backend si hay conectividad.
     */
    suspend fun deleteZonaFollaje(idZona: String): Result<Unit>

    /**
     * Actualiza los datos u observaciones de una zona de follaje (`PUT /api/v1/zonas-follaje/{id}`).
     */
    suspend fun updateZonaFollaje(
        idZona: String,
        nombreZona: String,
        radioMetros: Double,
        nuevaDescripcion: String?
    ): Result<Unit>

    /**
     * Elimina todos los registros locales de SQLite Room (análisis, análisis cromáticos, árboles,
     * zonas de follaje y focos) así como las imágenes locales almacenadas en el dispositivo.
     */
    suspend fun clearAllLocalDatabase(): Result<Unit>

    /**
     * Resuelve una ruta de evidencia local o URL remota (descargando desde el backend y cacheando
     * localmente si aún no existe en el dispositivo).
     */
    suspend fun resolveEvidenceFile(pathOrUrl: String): java.io.File? = null
}
