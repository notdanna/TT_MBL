package com.descortezadores.ecoverde.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.descortezadores.ecoverde.data.local.entity.AnalisisConCromaticoRelation
import com.descortezadores.ecoverde.data.local.entity.AnalisisCromaticoEntity
import com.descortezadores.ecoverde.data.local.entity.AnalisisEntity
import com.descortezadores.ecoverde.data.local.entity.ArbolEntity
import com.descortezadores.ecoverde.data.local.entity.FocoInfestacionEntity
import com.descortezadores.ecoverde.data.local.entity.ZonaFollajeEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO integral de monitoreo forestal (`analisis`, `arbol`, `zona_follaje`, `foco_infestacion`)
 * para operación 100% Offline-First (`RN-50`, `RF-10`, `RF-12`, `RF-14`, `RF-15`, `RF-17`, `RF-19`..`RF-23`).
 */
@Dao
interface ForestMonitoringDao {

    // =========================================================================
    // 1. ANÁLISIS Y COLA DE SINCRONIZACIÓN (RF-10, RF-12, RF-15, RN-30..33)
    // =========================================================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAnalisis(entity: AnalisisEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAnalisisCromatico(entity: AnalisisCromaticoEntity)

    @Transaction
    suspend fun saveFullAnalysisTransaction(
        analisis: AnalisisEntity,
        cromatico: AnalisisCromaticoEntity
    ) {
        upsertAnalisis(analisis)
        upsertAnalisisCromatico(cromatico)
    }

    @Transaction
    @Query("SELECT * FROM analisis ORDER BY fecha_captura_millis DESC")
    fun observeAllAnalysesWithChromatic(): Flow<List<AnalisisConCromaticoRelation>>

    @Transaction
    @Query(
        "SELECT * FROM analisis " +
            "WHERE fecha_captura_millis >= :startOfDayMillis AND fecha_captura_millis <= :endOfDayMillis " +
            "ORDER BY fecha_captura_millis DESC"
    )
    fun observeDailyJourneyAnalyses(
        startOfDayMillis: Long,
        endOfDayMillis: Long
    ): Flow<List<AnalisisConCromaticoRelation>>

    @Transaction
    @Query("SELECT * FROM analisis WHERE id_analisis = :idAnalisis LIMIT 1")
    suspend fun getAnalysisWithChromaticById(idAnalisis: String): AnalisisConCromaticoRelation?

    @Transaction
    @Query("SELECT * FROM analisis WHERE id_arbol = :idArbol ORDER BY fecha_captura_millis DESC")
    fun observeAnalysesByTree(idArbol: String): Flow<List<AnalisisConCromaticoRelation>>

    @Transaction
    @Query(
        "SELECT * FROM analisis " +
            "WHERE estado_sincronizacion IN ('PENDIENTE', 'ERROR') " +
            "ORDER BY fecha_captura_millis ASC"
    )
    suspend fun getPendingSyncAnalyses(): List<AnalisisConCromaticoRelation>

    @Query(
        "SELECT COUNT(*) FROM analisis " +
            "WHERE estado_sincronizacion IN ('PENDIENTE', 'ERROR')"
    )
    fun observePendingSyncCount(): Flow<Int>

    @Query(
        "UPDATE analisis SET " +
            "estado_sincronizacion = :newStatus, " +
            "fecha_sincronizacion_iso = :syncedAtIso, " +
            "ultimo_error_sincronizacion = NULL " +
            "WHERE id_analisis IN (:ids)"
    )
    suspend fun markAnalysesSynced(ids: List<String>, newStatus: String, syncedAtIso: String)

    @Query(
        "UPDATE analisis SET " +
            "estado_sincronizacion = 'ERROR', " +
            "ultimo_error_sincronizacion = :errorMessage " +
            "WHERE id_analisis IN (:ids)"
    )
    suspend fun markAnalysesFailed(ids: List<String>, errorMessage: String)

    // =========================================================================
    // 2. CATÁLOGO LOCAL DE ÁRBOLES (CU-M09, RF-19..23, RN-35..42)
    // =========================================================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertArbol(arbol: ArbolEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllArboles(arboles: List<ArbolEntity>)

    @Query("SELECT * FROM arbol ORDER BY nombre_especifico ASC")
    fun observeAllArboles(): Flow<List<ArbolEntity>>

    @Query("SELECT * FROM arbol WHERE id_arbol = :idArbol LIMIT 1")
    suspend fun getArbolById(idArbol: String): ArbolEntity?

    @Query("SELECT * FROM arbol WHERE LOWER(TRIM(nombre_especifico)) = LOWER(TRIM(:nombre)) LIMIT 1")
    suspend fun getArbolByNombre(nombre: String): ArbolEntity?

    @Query(
        "SELECT * FROM arbol " +
            "WHERE (:searchQuery = '' OR nombre_especifico LIKE '%' || :searchQuery || '%' " +
            "OR especie LIKE '%' || :searchQuery || '%' " +
            "OR id_arbol LIKE '%' || :searchQuery || '%') " +
            "AND (:healthStatusFilter IS NULL OR ultimo_estado_fitosanitario = :healthStatusFilter) " +
            "ORDER BY nombre_especifico ASC"
    )
    fun observeFilteredArboles(
        searchQuery: String,
        healthStatusFilter: String?
    ): Flow<List<ArbolEntity>>

    @Query("SELECT * FROM arbol WHERE pendiente_sincronizar = 1")
    suspend fun getPendingSyncArboles(): List<ArbolEntity>

    @Query("UPDATE arbol SET pendiente_sincronizar = 0 WHERE id_arbol IN (:ids)")
    suspend fun markArbolesSynced(ids: List<String>)

    // =========================================================================
    // 3. ZONAS MACRO DE FOLLAJE (RN-47, RN-49)
    // =========================================================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertZonaFollaje(zona: ZonaFollajeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllZonasFollaje(zonas: List<ZonaFollajeEntity>)

    @Query("SELECT * FROM zona_follaje ORDER BY nombre_zona ASC")
    fun observeAllZonasFollaje(): Flow<List<ZonaFollajeEntity>>

    @Query("SELECT * FROM zona_follaje WHERE id_zona = :idZona")
    suspend fun getZonaById(idZona: String): ZonaFollajeEntity?

    @Query("SELECT * FROM zona_follaje WHERE pendiente_sincronizar = 1")
    suspend fun getPendingSyncZonas(): List<ZonaFollajeEntity>

    @Query("UPDATE zona_follaje SET pendiente_sincronizar = 0 WHERE id_zona IN (:ids)")
    suspend fun markZonasSynced(ids: List<String>)

    // =========================================================================
    // 4. FOCOS DE INFESTACIÓN NOM-019 (RF-14, RN-25..29)
    // =========================================================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllFocosInfestacion(focos: List<FocoInfestacionEntity>)

    @Query("DELETE FROM foco_infestacion")
    suspend fun clearAllFocosInfestacion()

    @Transaction
    suspend fun replaceAllFocosInfestacion(focos: List<FocoInfestacionEntity>) {
        clearAllFocosInfestacion()
        upsertAllFocosInfestacion(focos)
    }

    @Query("SELECT * FROM foco_infestacion ORDER BY cantidad_arboles_infectados DESC")
    fun observeActiveFocosInfestacion(): Flow<List<FocoInfestacionEntity>>

    @Query("SELECT * FROM foco_infestacion")
    suspend fun getActiveFocosInfestacionList(): List<FocoInfestacionEntity>

    // =========================================================================
    // 5. LIMPIEZA TOTAL Y ELIMINACIÓN DE ÁRBOLES (CATEGORÍAS)
    // =========================================================================

    @Query("DELETE FROM arbol WHERE id_arbol = :idArbol")
    suspend fun deleteArbolById(idArbol: String)

    @Query("DELETE FROM analisis WHERE id_arbol = :idArbol")
    suspend fun deleteAnalisesByTreeId(idArbol: String)

    @Query("DELETE FROM analisis_cromatico WHERE id_analisis_fk IN (SELECT id_analisis FROM analisis WHERE id_arbol = :idArbol)")
    suspend fun deleteAnalisisCromaticoByTreeId(idArbol: String)

    @Query("SELECT * FROM analisis WHERE id_arbol = :idArbol")
    suspend fun getAnalysesByTreeList(idArbol: String): List<AnalisisEntity>

    @Transaction
    suspend fun deleteTreeAndAssociatedData(idArbol: String) {
        deleteAnalisisCromaticoByTreeId(idArbol)
        deleteAnalisesByTreeId(idArbol)
        deleteArbolById(idArbol)
    }

    @Query("UPDATE arbol SET descripcion = :nuevaDescripcion, pendiente_sincronizar = 1 WHERE id_arbol = :idArbol")
    suspend fun updateArbolDescripcion(idArbol: String, nuevaDescripcion: String)

    @Query("DELETE FROM zona_follaje WHERE id_zona = :idZona")
    suspend fun deleteZonaById(idZona: String)

    @Query("DELETE FROM analisis WHERE id_zona = :idZona")
    suspend fun deleteAnalisesByZonaId(idZona: String)

    @Query("DELETE FROM analisis_cromatico WHERE id_analisis_fk IN (SELECT id_analisis FROM analisis WHERE id_zona = :idZona)")
    suspend fun deleteAnalisisCromaticoByZonaId(idZona: String)

    @Query("SELECT * FROM analisis WHERE id_zona = :idZona")
    suspend fun getAnalysesByZonaList(idZona: String): List<AnalisisEntity>

    @Transaction
    suspend fun deleteZonaAndAssociatedData(idZona: String) {
        deleteAnalisisCromaticoByZonaId(idZona)
        deleteAnalisesByZonaId(idZona)
        deleteZonaById(idZona)
    }

    @Query("DELETE FROM analisis_cromatico")
    suspend fun clearAllAnalisisCromatico()

    @Query("DELETE FROM analisis")
    suspend fun clearAllAnalisis()

    @Query("DELETE FROM arbol")
    suspend fun clearAllArboles()

    @Query("DELETE FROM zona_follaje")
    suspend fun clearAllZonasFollaje()

    @Transaction
    suspend fun clearAllLocalMonitoringData() {
        clearAllAnalisisCromatico()
        clearAllAnalisis()
        clearAllArboles()
        clearAllZonasFollaje()
        clearAllFocosInfestacion()
    }
}
