package com.descortezadores.ecoverde.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTOs de Autenticación, Sesión y Salud del Servidor conforme a la
 * Guía de Uso de APIs del Backend (§1 y §2) y `LoginController.java`.
 */
data class HealthResponseDto(
    @SerializedName("status") val status: String,
    @SerializedName("service") val service: String?,
    @SerializedName("version") val version: String?,
    @SerializedName("timestamp") val timestamp: String?
)

/**
 * Cuerpo enviado a `POST /login` (y `POST /api/v1/auth/login`).
 * Incluye `idDispositivo` (UUID), `nombreDispositivo` e `identificadorUnico` (ANDROID_ID)
 * para que el backend valide y auto-registre el dispositivo en `dispositivo` y `cuenta_dispositivo`.
 */
data class LoginRequestDto(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String,
    @SerializedName("idDispositivo") val idDispositivo: String? = null,
    @SerializedName("nombreDispositivo") val nombreDispositivo: String? = null,
    @SerializedName("identificadorUnico") val identificadorUnico: String? = null
)

data class LoginResponseDto(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String?,
    @SerializedName("expires_in") val expiresIn: Long?,
    @SerializedName("username") val username: String,
    @SerializedName("roles") val roles: List<String>?
)

data class LogoutResponseDto(
    @SerializedName("mensaje") val mensaje: String?,
    @SerializedName("usuario") val usuario: String?
)

data class RecuperarPasswordRequestDto(
    @SerializedName("correo") val correo: String
)

data class RecuperarPasswordResponseDto(
    @SerializedName("mensaje") val mensaje: String,
    @SerializedName("codigoRecuperacion") val codigoRecuperacion: String,
    @SerializedName("vigenciaHoras") val vigenciaHoras: Int
)

data class RestablecerPasswordRequestDto(
    @SerializedName("token") val token: String,
    @SerializedName("nuevaContrasena") val nuevaContrasena: String,
    @SerializedName("confirmacionContrasena") val confirmacionContrasena: String
)

data class MensajeResponseDto(
    @SerializedName("mensaje") val mensaje: String?,
    @SerializedName("codigo") val codigo: String?,
    @SerializedName("message") val message: String?
)

data class PerfilUsuarioDto(
    @SerializedName("idCuenta") val idCuenta: String,
    @SerializedName("correo") val correo: String,
    @SerializedName("nombreCompleto") val nombreCompleto: String,
    @SerializedName("rol") val rol: String,
    @SerializedName("estadoCuenta") val estadoCuenta: String,
    @SerializedName("fechaCreacion") val fechaCreacion: String
)
