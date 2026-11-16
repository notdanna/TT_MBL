package com.descortezadores.ecoverde.domain.model

/**
 * Estados posibles de sincronización de un registro local (RN-17).
 */
enum class SyncStatus(
    val apiValue: String,
    val displayLabel: String
) {
    PENDIENTE("pendiente", "Pendiente de sincronización"),
    SINCRONIZADO("sincronizado", "Sincronizado"),
    ERROR("error", "Error de sincronización");

    companion object {
        fun fromApiValue(value: String): SyncStatus =
            entries.firstOrNull { it.apiValue.equals(value.trim(), ignoreCase = true) } ?: PENDIENTE
    }
}

/**
 * Especies forestales predefinidas en el Bosque de Chapultepec (RN-38).
 */
enum class TreeSpecies(val officialName: String) {
    FRESNO("Fresno (Fraxinus uhdei)"),
    CEDRO_BLANCO("Cedro blanco (Cupressus lusitanica)"),
    AHUEHUETE("Ahuehuete (Taxodium mucronatum)"),
    PINO("Pino (Pinus spp.)"),
    EUCALIPTO("Eucalipto (Eucalyptus spp.)");

    companion object {
        val defaultList: List<String> = entries.map { it.officialName }
    }
}

/**
 * Asociación obligatoria de una captura según el tipo de muestra (RN-37) y la restricción
 * de exclusión mutua `chk_arbol_o_zona` del backend (`POST /api/v1/sync/detecciones`).
 *
 * - Una muestra de [SampleType.CORTEZA] SOLO puede asociarse a un árbol (existente o nuevo).
 * - Una muestra de [SampleType.FOLLAJE] SOLO puede asociarse a una zona de follaje (existente o nueva).
 */
sealed interface TargetAssociation {
    val sampleType: SampleType

    sealed interface CortezaTarget : TargetAssociation {
        override val sampleType: SampleType get() = SampleType.CORTEZA

        data class ExistingTree(
            val idArbol: String,
            val nombreEspecifico: String,
            val especie: String
        ) : CortezaTarget {
            init {
                require(idArbol.isNotBlank()) { "idArbol es obligatorio para árbol existente" }
            }
        }

        data class NewTree(
            val nombreArbolNuevo: String,
            val especieArbolNuevo: String,
            val descripcion: String? = null
        ) : CortezaTarget {
            init {
                require(nombreArbolNuevo.isNotBlank()) { "nombreArbolNuevo es obligatorio (RN-38)" }
                require(especieArbolNuevo.isNotBlank()) { "especieArbolNuevo es obligatorio (RN-38)" }
            }
        }
    }

    sealed interface FollajeTarget : TargetAssociation {
        override val sampleType: SampleType get() = SampleType.FOLLAJE

        data class ExistingZone(
            val idZona: String,
            val nombreZona: String,
            val radioMetros: Double
        ) : FollajeTarget {
            init {
                require(idZona.isNotBlank()) { "idZona es obligatorio para zona existente" }
            }
        }

        data class NewZone(
            val nombreZonaNuevo: String,
            val radioMetrosZona: Double = 60.0,
            val descripcion: String? = null
        ) : FollajeTarget {
            init {
                require(nombreZonaNuevo.isNotBlank()) { "nombreZonaNuevo es obligatorio para zona nueva" }
                require(radioMetrosZona > 0.0) { "radioMetrosZona debe ser mayor a 0" }
            }
        }
    }
}

/**
 * Modelo de dominio de un Árbol registrado (`ARBOL` - §4.4.9 y `/api/v1/arboles`).
 */
data class TreeRecord(
    val idArbol: String,
    val nombreEspecifico: String,
    val especie: String,
    val latitud: Double?,
    val longitud: Double?,
    val fechaRegistroIso: String,
    val descripcion: String?,
    val idCuenta: String?,
    val idDispositivo: String?,
    val ultimoEstadoSanitario: ClassificationOutcome? = null
)

/**
 * Modelo de dominio de una Zona de Follaje (`ZONA_FOLLAJE` - §4.4.9 y `/api/v1/zonas-follaje`).
 */
data class FoliageZoneRecord(
    val idZona: String,
    val nombreZona: String,
    val latitudCentro: Double,
    val longitudCentro: Double,
    val radioMetros: Double,
    val fechaRegistroIso: String,
    val descripcion: String?,
    val idCuenta: String?,
    val idDispositivo: String?
)

/**
 * Modelo de dominio de un Foco de Infestación Activo conforme a NOM-019-SEMARNAT-2017
 * (`RN-25`: 3+ árboles infectados en <= 1,000 m2; `RN-27`: alerta de proximidad <= 100m).
 */
data class InfestationFocus(
    val idFoco: String,
    val latitudCentro: Double,
    val longitudCentro: Double,
    val radioMetros: Double,
    val cantidadArbolesInfectados: Int,
    val idsArboles: List<String>,
    val nombresArboles: List<String>
)

/**
 * Resumen estadístico de la jornada actual (00:00 - 23:59) (`RF-12`, `RN-30`, `RN-31`).
 */
data class DailyJourneySummary(
    val totalCapturasHoy: Int,
    val totalSanosHoy: Int,
    val totalInfestadosHoy: Int,
    val totalNoDeterminadosHoy: Int,
    val pendientesSincronizacion: Int,
    val erroresSincronizacion: Int,
    val alertaEstacionalActiva: Boolean,
    val mensajeAlertaEstacional: String?
)
