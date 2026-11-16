package com.descortezadores.ecoverde.data.remote.api

import com.descortezadores.ecoverde.data.remote.dto.HealthResponseDto
import com.descortezadores.ecoverde.data.remote.dto.LoginRequestDto
import com.descortezadores.ecoverde.data.remote.dto.LoginResponseDto
import com.descortezadores.ecoverde.data.remote.dto.LogoutResponseDto
import com.descortezadores.ecoverde.data.remote.dto.MensajeResponseDto
import com.descortezadores.ecoverde.data.remote.dto.PerfilUsuarioDto
import com.descortezadores.ecoverde.data.remote.dto.RecuperarPasswordRequestDto
import com.descortezadores.ecoverde.data.remote.dto.RecuperarPasswordResponseDto
import com.descortezadores.ecoverde.data.remote.dto.RestablecerPasswordRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Interfaz Retrofit para los endpoints de Diagnóstico y Autenticación (§1 y §2
 * de la Guía de uso de APIs del backend).
 */
interface AuthApiService {

    @GET("api/v1/health")
    suspend fun checkHealth(): Response<HealthResponseDto>

    @POST("login")
    suspend fun login(@Body request: LoginRequestDto): Response<LoginResponseDto>

    @POST("api/v1/auth/logout")
    suspend fun logout(): Response<LogoutResponseDto>

    @POST("api/v1/auth/recuperar-password")
    suspend fun recuperarPassword(
        @Body request: RecuperarPasswordRequestDto
    ): Response<RecuperarPasswordResponseDto>

    @POST("api/v1/auth/restablecer-password")
    suspend fun restablecerPassword(
        @Body request: RestablecerPasswordRequestDto
    ): Response<MensajeResponseDto>

    @GET("api/v1/auth/perfil")
    suspend fun obtenerPerfil(): Response<PerfilUsuarioDto>
}
