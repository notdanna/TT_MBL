package com.descortezadores.ecoverde.data.remote.api

import com.descortezadores.ecoverde.data.remote.dto.AnalisisDetalleDto
import com.descortezadores.ecoverde.data.remote.dto.ArbolDto
import com.descortezadores.ecoverde.data.remote.dto.DashboardEstadisticasDto
import com.descortezadores.ecoverde.data.remote.dto.EvidenciaUploadResponseDto
import com.descortezadores.ecoverde.data.remote.dto.FocoInfestacionDto
import com.descortezadores.ecoverde.data.remote.dto.ModeloOtaMetadataDto
import com.descortezadores.ecoverde.data.remote.dto.SyncBatchResponseDto
import com.descortezadores.ecoverde.data.remote.dto.SyncItemDto
import com.descortezadores.ecoverde.data.remote.dto.ZonaFollajeDto
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Streaming

/**
 * Interfaz Retrofit con todos los endpoints de sincronización, catálogos, evidencias,
 * actualización OTA de modelos `.tflite` y estadísticas NOM-019 especificados en
 * `Guia de uso de APIs del backend.md`.
 */
interface MonitoringApiService {

    // -------------------------------------------------------------------------
    // 3. Sincronización Móvil Offline-First y Evidencias
    // -------------------------------------------------------------------------

    @POST("api/v1/sync/detecciones")
    suspend fun syncDetecciones(
        @Body items: List<SyncItemDto>
    ): Response<SyncBatchResponseDto>

    @Multipart
    @POST("api/v1/evidencias/upload")
    suspend fun uploadEvidencia(
        @Part file: MultipartBody.Part
    ): Response<EvidenciaUploadResponseDto>

    @Streaming
    @GET("api/v1/evidencias/{filename}")
    suspend fun downloadEvidencia(
        @Path("filename") filename: String
    ): Response<ResponseBody>

    // -------------------------------------------------------------------------
    // 4. Distribución y Actualización OTA de Modelos TFLite
    // -------------------------------------------------------------------------

    @GET("api/v1/modelos/latest")
    suspend fun getLatestModeloMetadata(): Response<ModeloOtaMetadataDto>

    @Streaming
    @GET("api/v1/modelos/descargar/{nombreArchivo}")
    suspend fun downloadModeloFile(
        @Path("nombreArchivo") nombreArchivo: String
    ): Response<ResponseBody>

    // -------------------------------------------------------------------------
    // 5. Catálogo de Árboles y Zonas de Follaje
    // -------------------------------------------------------------------------

    @GET("api/v1/arboles")
    suspend fun getArboles(): Response<List<ArbolDto>>

    @POST("api/v1/arboles")
    suspend fun createArbol(
        @Body arbol: ArbolDto
    ): Response<ArbolDto>

    @PUT("api/v1/arboles/{id}")
    suspend fun updateArbol(
        @Path("id") idArbol: String,
        @Body arbol: ArbolDto
    ): Response<ArbolDto>

    @DELETE("api/v1/arboles")
    suspend fun deleteAllArboles(): Response<Unit>

    @DELETE("api/v1/arboles/{id}")
    suspend fun deleteArbol(
        @Path("id") idArbol: String
    ): Response<Unit>

    @GET("api/v1/zonas-follaje")
    suspend fun getZonasFollaje(): Response<List<ZonaFollajeDto>>

    @POST("api/v1/zonas-follaje")
    suspend fun createZonaFollaje(
        @Body zona: ZonaFollajeDto
    ): Response<ZonaFollajeDto>

    @PUT("api/v1/zonas-follaje/{id}")
    suspend fun updateZonaFollaje(
        @Path("id") idZona: String,
        @Body zona: ZonaFollajeDto
    ): Response<ZonaFollajeDto>

    @DELETE("api/v1/zonas-follaje/{id}")
    suspend fun deleteZonaFollaje(
        @Path("id") idZona: String
    ): Response<Unit>

    @DELETE("api/v1/zonas-follaje")
    suspend fun deleteAllZonasFollaje(): Response<Unit>

    // -------------------------------------------------------------------------
    // 6. Jornada Actual, Historial y Alertas NOM-019
    // -------------------------------------------------------------------------

    @GET("api/v1/analisis/jornada")
    suspend fun getAnalisisJornadaActual(): Response<List<AnalisisDetalleDto>>

    @GET("api/v1/analisis/{id}")
    suspend fun getAnalisisDetalle(
        @Path("id") idAnalisis: String
    ): Response<AnalisisDetalleDto>

    @GET("api/v1/estadisticas/focos")
    suspend fun getFocosInfestacionActivos(): Response<List<FocoInfestacionDto>>

    @GET("api/v1/estadisticas/dashboard")
    suspend fun getDashboardEstadisticas(): Response<DashboardEstadisticasDto>
}
