package com.descortezadores.ecoverde.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.descortezadores.ecoverde.data.local.dao.AnalisisCromaticoDao
import com.descortezadores.ecoverde.data.local.dao.ForestMonitoringDao
import com.descortezadores.ecoverde.data.local.entity.AnalisisCromaticoEntity
import com.descortezadores.ecoverde.data.local.entity.AnalisisEntity
import com.descortezadores.ecoverde.data.local.entity.ArbolEntity
import com.descortezadores.ecoverde.data.local.entity.FocoInfestacionEntity
import com.descortezadores.ecoverde.data.local.entity.ZonaFollajeEntity

/**
 * Base de datos SQLite local Offline-First gestionada por Room (`RN-50`, `RNF-06`).
 *
 * Incluye las 5 tablas troncales del sistema móvil EcoVerde:
 * - `analisis_cromatico`: Estadísticas HSV (`pct_sano`, `pct_regular`, `pct_danado`, `pct_incierto`) y rutas de imágenes (`RN-50`).
 * - `analisis`: Registro completo de captura con coordenadas GPS, clasificación TFLite y exclusión mutua `chk_arbol_o_zona`.
 * - `arbol`: Catálogo local de árboles georreferenciados en el Bosque de Chapultepec (`CU-M09`).
 * - `zona_follaje`: Zonas macro de copa/dron (`RN-47`).
 * - `foco_infestacion`: Focos activos NOM-019-SEMARNAT-2017 para alertas de proximidad offline a `< 50 m` (`RN-28`).
 */
@Database(
    entities = [
        AnalisisCromaticoEntity::class,
        AnalisisEntity::class,
        ArbolEntity::class,
        ZonaFollajeEntity::class,
        FocoInfestacionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class EcoVerdeDatabase : RoomDatabase() {
    abstract fun analisisCromaticoDao(): AnalisisCromaticoDao
    abstract fun forestMonitoringDao(): ForestMonitoringDao

    companion object {
        const val DATABASE_NAME = "ecoverde_chapultepec_offline.db"
    }
}
