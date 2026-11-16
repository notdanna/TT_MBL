package com.descortezadores.ecoverde.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.descortezadores.ecoverde.data.local.entity.AnalisisCromaticoEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO de Room para el acceso local offline a la tabla `analisis_cromatico` (`RN-50`).
 */
@Dao
interface AnalisisCromaticoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAnalisisCromatico(entity: AnalisisCromaticoEntity)

    @Query("SELECT * FROM analisis_cromatico WHERE id_analisis_cromatico = :id LIMIT 1")
    suspend fun getById(id: String): AnalisisCromaticoEntity?

    @Query("SELECT * FROM analisis_cromatico WHERE id_analisis_fk = :idAnalisisFk LIMIT 1")
    suspend fun getByAnalisisFk(idAnalisisFk: String): AnalisisCromaticoEntity?

    @Query("SELECT * FROM analisis_cromatico WHERE id_analisis_fk = :idAnalisisFk LIMIT 1")
    fun observeByAnalisisFk(idAnalisisFk: String): Flow<AnalisisCromaticoEntity?>

    @Query("SELECT * FROM analisis_cromatico ORDER BY fecha_analisis_millis DESC")
    fun observeAll(): Flow<List<AnalisisCromaticoEntity>>

    @Query("SELECT * FROM analisis_cromatico WHERE estado_sincronizacion = :status ORDER BY fecha_analisis_millis ASC")
    suspend fun getBySyncStatus(status: String): List<AnalisisCromaticoEntity>

    @Query("UPDATE analisis_cromatico SET estado_sincronizacion = :newStatus WHERE id_analisis_fk IN (:parentAnalisisIds)")
    suspend fun updateSyncStatusByParentIds(parentAnalisisIds: List<String>, newStatus: String)

    @Query("SELECT COUNT(*) FROM analisis_cromatico")
    fun observeTotalCount(): Flow<Int>
}
