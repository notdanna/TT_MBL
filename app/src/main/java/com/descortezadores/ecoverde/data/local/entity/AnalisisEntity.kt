package com.descortezadores.ecoverde.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.descortezadores.ecoverde.domain.model.SyncStatus

/**
 * Entidad Room para la tabla `analisis` (`RN-50`, `RF-10`, `RF-15`).
 *
 * Respeta la restricción de exclusión mutua `chk_arbol_o_zona`:
 * - Si `tipoImagen == "corteza"`, se puebla `idArbol` o (`nombreArbolNuevo`, `especieArbolNuevo`) y `idZona == null`.
 * - Si `tipoImagen == "follaje"`, se puebla `idZona` o (`nombreZonaNuevo`, `radioMetrosZona`) y `idArbol == null`.
 */
@Entity(
    tableName = "analisis",
    indices = [
        Index(value = ["estado_sincronizacion"]),
        Index(value = ["fecha_captura_millis"]),
        Index(value = ["id_arbol"]),
        Index(value = ["id_zona"])
    ]
)
data class AnalisisEntity(
    @PrimaryKey
    @ColumnInfo(name = "id_analisis")
    val idAnalisis: String,

    @ColumnInfo(name = "brigadista_id")
    val brigadistaId: String,

    @ColumnInfo(name = "id_dispositivo")
    val idDispositivo: String,

    @ColumnInfo(name = "tipo_imagen")
    val tipoImagen: String, // "corteza" | "follaje"

    @ColumnInfo(name = "resultado_clasificacion")
    val resultadoClasificacion: String, // "sano" | "infestado" | "no_determinado"

    @ColumnInfo(name = "confianza")
    val confianza: Double,

    @ColumnInfo(name = "probabilidad_cruda_tflite")
    val probabilidadCrudaTflite: Double,

    @ColumnInfo(name = "latitud_captura")
    val latitudCaptura: Double,

    @ColumnInfo(name = "longitud_captura")
    val longitudCaptura: Double,

    @ColumnInfo(name = "precision_gps_metros")
    val precisionGpsMetros: Float? = null,

    @ColumnInfo(name = "fecha_captura_iso")
    val fechaCapturaIso: String,

    @ColumnInfo(name = "fecha_captura_millis")
    val fechaCapturaMillis: Long,

    @ColumnInfo(name = "observaciones")
    val observaciones: String? = null,

    // --- Asociación mutuamente excluyente: Corteza (Árbol) ---
    @ColumnInfo(name = "id_arbol")
    val idArbol: String? = null,

    @ColumnInfo(name = "nombre_arbol_nuevo")
    val nombreArbolNuevo: String? = null,

    @ColumnInfo(name = "especie_arbol_nuevo")
    val especieArbolNuevo: String? = null,

    // --- Asociación mutuamente excluyente: Follaje (Zona macro) ---
    @ColumnInfo(name = "id_zona")
    val idZona: String? = null,

    @ColumnInfo(name = "nombre_zona_nuevo")
    val nombreZonaNuevo: String? = null,

    @ColumnInfo(name = "radio_metros_zona")
    val radioMetrosZona: Double? = null,

    // --- Rutas de evidencia ---
    @ColumnInfo(name = "ruta_evidencia_local")
    val rutaEvidenciaLocal: String,

    @ColumnInfo(name = "ruta_overlay_local")
    val rutaOverlayLocal: String,

    @ColumnInfo(name = "ruta_evidencia_remota")
    val rutaEvidenciaRemota: String? = null,

    // --- Estado de sincronización con el backend (RF-15) ---
    @ColumnInfo(name = "estado_sincronizacion")
    val estadoSincronizacion: String = SyncStatus.PENDIENTE.name,

    @ColumnInfo(name = "fecha_sincronizacion_iso")
    val fechaSincronizacionIso: String? = null,

    @ColumnInfo(name = "ultimo_error_sincronizacion")
    val ultimoErrorSincronizacion: String? = null
) {
    init {
        require(!(idArbol != null && idZona != null)) {
            "Violación de chk_arbol_o_zona: un análisis no puede asociarse simultáneamente a idArbol e idZona."
        }
    }
}

/**
 * Proyección relacional Room 1:1 entre `analisis` y `analisis_cromatico`.
 */
data class AnalisisConCromaticoRelation(
    @Embedded
    val analisis: AnalisisEntity,

    @Relation(
        parentColumn = "id_analisis",
        entityColumn = "id_analisis_fk"
    )
    val cromatico: AnalisisCromaticoEntity?
)
