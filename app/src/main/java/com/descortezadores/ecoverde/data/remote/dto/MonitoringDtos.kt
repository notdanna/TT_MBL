package com.descortezadores.ecoverde.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO de envío para `POST /api/v1/sync/detecciones` (`RF-15`, `PI-01`, `§8.2.5`).
 *
 * Respeta estrictamente la exclusión mutua `chk_arbol_o_zona`:
 * - Si `tipoImagen == "corteza"`, se incluyen `idArbol` o (`nombreArbolNuevo`, `especieArbolNuevo`) y `idZona = null`.
 * - Si `tipoImagen == "follaje"`, se incluyen `idZona` o (`nombreZonaNuevo`, `radioMetrosZona`) y `idArbol = null`.
 */
data class SyncItemDto(
    @SerializedName("idAnalisisLocal")
    val idAnalisisLocal: String,
    @SerializedName("latitud")
    val latitud: Double,
    @SerializedName("longitud")
    val longitud: Double,
    @SerializedName("fechaCaptura")
    val fechaCaptura: String,
    @SerializedName("tipoImagen")
    val tipoImagen: String, // "corteza" | "follaje"
    @SerializedName("resultadoClasificacion")
    val resultadoClasificacion: String, // "sano" | "infestado" | "no_determinado"
    @SerializedName("confianza")
    val confianza: Double,
    @SerializedName("observaciones")
    val observaciones: String? = null,

    // Opciones exclusivas para tipoImagen = "corteza"
    @SerializedName("idArbol")
    val idArbol: String? = null,
    @SerializedName("nombreArbolNuevo")
    val nombreArbolNuevo: String? = null,
    @SerializedName("especieArbolNuevo")
    val especieArbolNuevo: String? = null,

    // Opciones exclusivas para tipoImagen = "follaje"
    @SerializedName("idZona")
    val idZona: String? = null,
    @SerializedName("nombreZonaNuevo")
    val nombreZonaNuevo: String? = null,
    @SerializedName("radioMetrosZona")
    val radioMetrosZona: Double? = null,

    // Análisis Cromático HSV Complementario (RN-47, RN-48)
    @SerializedName("pctSano")
    val pctSano: Double,
    @SerializedName("pctDanado")
    val pctDanado: Double,
    @SerializedName("pctRegular")
    val pctRegular: Double,
    @SerializedName("pctIncierto")
    val pctIncierto: Double,

    // Evidencia fotográfica opcional en Base64 (RN-07..09)
    @SerializedName("imagenBase64")
    val imagenBase64: String? = null,

    // Identificador del dispositivo autorizado (RF-27)
    @SerializedName("idDispositivo")
    val idDispositivo: String? = null
)

/**
 * Respuesta de `POST /api/v1/sync/detecciones`.
 */
data class SyncBatchResponseDto(
    @SerializedName("totalRecibidos")
    val totalRecibidos: Int = 0,
    @SerializedName("totalSincronizados")
    val totalSincronizados: Int = 0,
    @SerializedName("idsSincronizados")
    val idsSincronizados: List<String> = emptyList(),
    @SerializedName("errores")
    val errores: List<String> = emptyList()
)

/**
 * Respuesta de `POST /api/v1/evidencias/upload`.
 */
data class EvidenciaUploadResponseDto(
    @SerializedName("filename")
    val filename: String,
    @SerializedName("url")
    val url: String,
    @SerializedName("sizeBytes")
    val sizeBytes: String? = null
)

/**
 * Respuesta de `GET /api/v1/modelos/latest` (`PT-15`, `PI-07`).
 */
data class ModeloOtaMetadataDto(
    @SerializedName("version")
    val version: String,
    @SerializedName("nombreArchivo")
    val nombreArchivo: String,
    @SerializedName("tamanoBytes")
    val tamanoBytes: Long,
    @SerializedName("sha256")
    val sha256: String,
    @SerializedName("urlDescarga")
    val urlDescarga: String,
    @SerializedName("fechaPublicacion")
    val fechaPublicacion: String? = null
)

/**
 * DTO de Árbol para `GET /api/v1/arboles`, `POST /api/v1/arboles`, `PUT /api/v1/arboles/{id}`.
 */
data class ArbolDto(
    @SerializedName("idArbol")
    val idArbol: String? = null,
    @SerializedName("nombreEspecifico")
    val nombreEspecifico: String,
    @SerializedName("especie")
    val especie: String,
    @SerializedName("latitud")
    val latitud: Double? = null,
    @SerializedName("longitud")
    val longitud: Double? = null,
    @SerializedName("fechaRegistro")
    val fechaRegistro: String? = null,
    @SerializedName("descripcion")
    val descripcion: String? = null,
    @SerializedName("idCuenta")
    val idCuenta: String? = null,
    @SerializedName("idDispositivo")
    val idDispositivo: String? = null
)

/**
 * DTO de Zona de Follaje para `GET /api/v1/zonas-follaje`.
 */
data class ZonaFollajeDto(
    @SerializedName("idZona")
    val idZona: String,
    @SerializedName("latitudCentro")
    val latitudCentro: Double,
    @SerializedName("longitudCentro")
    val longitudCentro: Double,
    @SerializedName("fechaRegistro")
    val fechaRegistro: String? = null,
    @SerializedName("descripcion")
    val descripcion: String? = null,
    @SerializedName("nombreZona")
    val nombreZona: String,
    @SerializedName("radioMetros")
    val radioMetros: Double? = 50.0,
    @SerializedName("idDispositivo")
    val idDispositivo: String? = null,
    @SerializedName("idCuenta")
    val idCuenta: String? = null
)

/**
 * DTO de Análisis Cromático anidado dentro de `AnalisisDetalleDto`.
 */
data class AnalisisCromaticoNestedDto(
    @SerializedName("idAnalisisCromatico")
    val idAnalisisCromatico: String? = null,
    @SerializedName("pctIncierto")
    val pctIncierto: Double = 0.0,
    @SerializedName("pctDanado")
    val pctDanado: Double = 0.0,
    @SerializedName("pctRegular")
    val pctRegular: Double = 0.0,
    @SerializedName("pctSano")
    val pctSano: Double = 0.0
)

/**
 * DTO de Análisis para `GET /api/v1/analisis/jornada` y `GET /api/v1/analisis/{id}`.
 */
data class AnalisisDetalleDto(
    @SerializedName("idAnalisis")
    val idAnalisis: String,
    @SerializedName("rutaEvidencia")
    val rutaEvidencia: String? = null,
    @SerializedName("latitudCaptura")
    val latitudCaptura: Double = 0.0,
    @SerializedName("longitudCaptura")
    val longitudCaptura: Double = 0.0,
    @SerializedName("fechaCaptura")
    val fechaCaptura: String,
    @SerializedName("fechaSincronizacion")
    val fechaSincronizacion: String? = null,
    @SerializedName("estadoSincronizacion")
    val estadoSincronizacion: String? = "sincronizado",
    @SerializedName("tipoImagen")
    val tipoImagen: String = "corteza",
    @SerializedName("resultadoClasificacion")
    val resultadoClasificacion: String = "sano",
    @SerializedName("confianza")
    val confianza: Double = 0.0,
    @SerializedName("observaciones")
    val observaciones: String? = null,
    @SerializedName("idArbol")
    val idArbol: String? = null,
    @SerializedName("idZona")
    val idZona: String? = null,
    @SerializedName("cromatico")
    val cromatico: AnalisisCromaticoNestedDto? = null
)

/**
 * DTO de Foco de Infestación NOM-019 para `GET /api/v1/estadisticas/focos` (`RF-14`, `RN-25`..`RN-29`).
 */
data class FocoInfestacionDto(
    @SerializedName("idFoco")
    val idFoco: String,
    @SerializedName("latitudCentro")
    val latitudCentro: Double,
    @SerializedName("longitudCentro")
    val longitudCentro: Double,
    @SerializedName("radioMetros")
    val radioMetros: Double = 17.84,
    @SerializedName("cantidadArbolesInfectados")
    val cantidadArbolesInfectados: Int = 3,
    @SerializedName("idsArboles")
    val idsArboles: List<String> = emptyList(),
    @SerializedName("nombresArboles")
    val nombresArboles: List<String> = emptyList()
)

/**
 * DTO de Estadísticas y Alerta Estacional NOM-019 para `GET /api/v1/estadisticas/dashboard` (`RN-44`).
 */
data class DashboardEstadisticasDto(
    @SerializedName("totalArboles")
    val totalArboles: Int = 0,
    @SerializedName("totalSanos")
    val totalSanos: Int = 0,
    @SerializedName("totalInfestados")
    val totalInfestados: Int = 0,
    @SerializedName("totalNoDeterminado")
    val totalNoDeterminado: Int = 0,
    @SerializedName("totalAnalisis")
    val totalAnalisis: Int = 0,
    @SerializedName("totalZonasFollaje")
    val totalZonasFollaje: Int = 0,
    @SerializedName("totalFocosActivos")
    val totalFocosActivos: Int = 0,
    @SerializedName("distribucionPorEspecie")
    val distribucionPorEspecie: Map<String, Int> = emptyMap(),
    @SerializedName("distribucionPorEstado")
    val distribucionPorEstado: Map<String, Int> = emptyMap(),
    @SerializedName("alertaEstacionalActiva")
    val alertaEstacionalActiva: Boolean = false,
    @SerializedName("mensajeAlertaEstacional")
    val mensajeAlertaEstacional: String? = null
)
