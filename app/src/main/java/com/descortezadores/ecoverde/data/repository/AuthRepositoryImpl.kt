package com.descortezadores.ecoverde.data.repository

import android.content.Context
import android.util.Base64
import com.descortezadores.ecoverde.core.common.DeviceIdentifierProvider
import com.descortezadores.ecoverde.core.common.SystemMessages
import com.descortezadores.ecoverde.data.local.session.SessionPreferencesManager
import com.descortezadores.ecoverde.data.local.session.SessionPreferencesManager.OfflineVerificationResult
import com.descortezadores.ecoverde.data.remote.api.AuthApiService
import com.descortezadores.ecoverde.data.remote.dto.LoginRequestDto
import com.descortezadores.ecoverde.data.remote.dto.MensajeResponseDto
import com.descortezadores.ecoverde.data.remote.dto.RecuperarPasswordRequestDto
import com.descortezadores.ecoverde.data.remote.dto.RestablecerPasswordRequestDto
import com.descortezadores.ecoverde.di.IoDispatcher
import com.descortezadores.ecoverde.domain.model.auth.BrigadistaSession
import com.descortezadores.ecoverde.domain.model.auth.PasswordRecoveryTicket
import com.descortezadores.ecoverde.domain.repository.AuthRepository
import com.google.gson.Gson
import com.google.gson.JsonObject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación del módulo de Autenticación y Sesión (CU-G01 a CU-G04, CU-M11).
 * Toda validación de cuentas, contraseñas, auto-registro de dispositivo y recuperación depende del Backend
 * (`POST /login`, `GET /api/v1/auth/perfil`, `POST /api/v1/auth/recuperar-password`, `POST /api/v1/auth/restablecer-password`).
 * Una vez autenticado contra el backend, la sesión persiste en DataStore (RN-03).
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApiService: AuthApiService,
    private val sessionPreferencesManager: SessionPreferencesManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationContext private val context: Context? = null
) : AuthRepository {

    // Constructor secundario para pruebas unitarias JVM sin Context de Android
    constructor(
        authApiService: AuthApiService,
        sessionPreferencesManager: SessionPreferencesManager,
        ioDispatcher: CoroutineDispatcher
    ) : this(authApiService, sessionPreferencesManager, ioDispatcher, null)

    private val gson = Gson()

    override val activeSessionFlow: Flow<BrigadistaSession?> =
        sessionPreferencesManager.activeSessionFlow

    override suspend fun getCurrentSession(): BrigadistaSession? =
        withContext(ioDispatcher) {
            sessionPreferencesManager.getCurrentSession()
        }

    override suspend fun login(correo: String, password: String): Result<BrigadistaSession> =
        withContext(ioDispatcher) {
            val trimmedEmail = correo.trim()
            if (trimmedEmail.isBlank() || password.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException(SystemMessages.MSG1))
            }

            val idDispositivoHardware = DeviceIdentifierProvider.getHardwareDeviceUuid(context)
            val identificadorUnicoHardware = DeviceIdentifierProvider.getRealAndroidId(context)
            val nombreDispositivoHardware = DeviceIdentifierProvider.getDeviceModelName()

            try {
                // 1. Autenticación contra POST /login del Backend enviando datos del dispositivo para auto-registro
                val response = authApiService.login(
                    LoginRequestDto(
                        username = trimmedEmail,
                        password = password,
                        idDispositivo = idDispositivoHardware,
                        nombreDispositivo = nombreDispositivoHardware,
                        identificadorUnico = identificadorUnicoHardware
                    )
                )

                if (response.isSuccessful && response.body() != null) {
                    val loginBody = response.body()!!
                    val jwtClaims = decodeJwtClaims(loginBody.accessToken)
                    val roleFromToken = loginBody.roles?.firstOrNull()
                        ?: jwtClaims?.get("rol")?.asString
                        ?: BrigadistaSession.ROL_EXPERTO_CAMPO

                    // RN-02: En app móvil solo accede el rol "Experto de campo"
                    if (!roleFromToken.equals(BrigadistaSession.ROL_EXPERTO_CAMPO, ignoreCase = true)) {
                        return@withContext Result.failure(IllegalStateException(SystemMessages.MSG5))
                    }

                    val idCuentaFromJwt = jwtClaims?.get("id_cuenta")?.asString
                        ?: BrigadistaSession.DEFAULT_BRIGADISTA_ID
                    val nombreFromJwt = jwtClaims?.get("nombre_completo")?.asString
                        ?: "Biólogo de Campo"
                    val idDispositivoFromJwt = jwtClaims?.get("id_dispositivo")?.asString
                        ?.takeIf { it.isNotBlank() }
                        ?: idDispositivoHardware

                    val preliminarySession = BrigadistaSession(
                        brigadistaId = idCuentaFromJwt,
                        correo = loginBody.username,
                        nombreCompleto = nombreFromJwt,
                        rol = roleFromToken,
                        estadoCuenta = "Activa",
                        idDispositivo = idDispositivoFromJwt,
                        accessToken = loginBody.accessToken,
                        isOfflineSession = false,
                        fechaCreacionCuenta = "2026-10-16T19:06:59Z",
                        loginTimestampMillis = System.currentTimeMillis()
                    )
                    sessionPreferencesManager.saveSession(preliminarySession, rawPasswordToCache = password)

                    // 2. Consultar perfil completo en GET /api/v1/auth/perfil (CU-M11)
                    val enrichedSession = try {
                        val perfilResponse = authApiService.obtenerPerfil()
                        if (perfilResponse.isSuccessful && perfilResponse.body() != null) {
                            val perfil = perfilResponse.body()!!
                            if (!perfil.estadoCuenta.equals("Activa", ignoreCase = true)) {
                                sessionPreferencesManager.clearActiveSession()
                                return@withContext Result.failure(IllegalStateException(SystemMessages.MSG4))
                            }
                            preliminarySession.copy(
                                brigadistaId = perfil.idCuenta,
                                correo = perfil.correo,
                                nombreCompleto = perfil.nombreCompleto,
                                rol = perfil.rol,
                                estadoCuenta = perfil.estadoCuenta,
                                fechaCreacionCuenta = perfil.fechaCreacion
                            )
                        } else {
                            preliminarySession
                        }
                    } catch (_: Exception) {
                        preliminarySession
                    }

                    sessionPreferencesManager.saveSession(enrichedSession, rawPasswordToCache = password)
                    return@withContext Result.success(enrichedSession)
                } else {
                    val errorBodyStr = response.errorBody()?.string()
                    val backendMsg = parseBackendErrorMessage(errorBodyStr)
                    val errorMsg = when (response.code()) {
                        401 -> backendMsg ?: SystemMessages.MSG3
                        403 -> backendMsg ?: SystemMessages.MSG4
                        else -> backendMsg ?: SystemMessages.MSG3
                    }
                    return@withContext Result.failure(IllegalArgumentException(errorMsg))
                }
            } catch (_: IOException) {
                // 3. Si no hay red en el Bosque de Chapultepec, verificar si esta cuenta ya inició sesión antes en el dispositivo
                return@withContext authenticateOfflineIfCached(trimmedEmail, password)
            }
        }

    private suspend fun authenticateOfflineIfCached(
        correo: String,
        password: String
    ): Result<BrigadistaSession> {
        return when (val result = sessionPreferencesManager.verifyOfflineCredentials(correo, password)) {
            is OfflineVerificationResult.Success -> {
                val session = result.session
                if (!session.isExpertoDeCampo) {
                    Result.failure(IllegalStateException(SystemMessages.MSG5))
                } else {
                    sessionPreferencesManager.saveSession(session, rawPasswordToCache = password)
                    Result.success(session)
                }
            }
            OfflineVerificationResult.NoOfflineCacheForUser ->
                Result.failure(
                    IllegalStateException(
                        "Sin conexión con el servidor backend y esta cuenta aún no tiene sesión previa guardada en el dispositivo."
                    )
                )
            OfflineVerificationResult.WrongPassword ->
                Result.failure(IllegalArgumentException(SystemMessages.MSG3))
            OfflineVerificationResult.AccountDisabled ->
                Result.failure(IllegalStateException(SystemMessages.MSG4))
        }
    }

    override suspend fun startOfflineBrigadistaSession(
        brigadistaId: String,
        nombreCompleto: String,
        correo: String
    ): Result<BrigadistaSession> = withContext(ioDispatcher) {
        if (brigadistaId.isBlank() || nombreCompleto.isBlank() || correo.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException(SystemMessages.MSG1))
        }
        val session = BrigadistaSession(
            brigadistaId = brigadistaId.trim(),
            correo = correo.trim().lowercase(),
            nombreCompleto = nombreCompleto.trim(),
            rol = BrigadistaSession.ROL_EXPERTO_CAMPO,
            estadoCuenta = "Activa",
            idDispositivo = DeviceIdentifierProvider.getHardwareDeviceUuid(context),
            accessToken = null,
            isOfflineSession = true,
            fechaCreacionCuenta = "2026-10-16T19:06:59Z",
            loginTimestampMillis = System.currentTimeMillis()
        )
        sessionPreferencesManager.saveSession(session)
        Result.success(session)
    }

    override suspend fun requestPasswordRecovery(correo: String): Result<PasswordRecoveryTicket> =
        withContext(ioDispatcher) {
            val trimmed = correo.trim()
            if (trimmed.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException(SystemMessages.MSG1))
            }

            try {
                val response = authApiService.recuperarPassword(RecuperarPasswordRequestDto(trimmed))
                if (response.isSuccessful && response.body() != null) {
                    val dto = response.body()!!
                    return@withContext Result.success(
                        PasswordRecoveryTicket(
                            mensaje = dto.mensaje,
                            codigoRecuperacion = dto.codigoRecuperacion,
                            vigenciaHoras = dto.vigenciaHoras
                        )
                    )
                } else {
                    val backendMsg = parseBackendErrorMessage(response.errorBody()?.string())
                    val msg = when (response.code()) {
                        400 -> backendMsg ?: SystemMessages.MSG1
                        404 -> backendMsg ?: SystemMessages.MSG2
                        429 -> backendMsg ?: "Ha excedido el límite de 3 solicitudes cada 30 días."
                        else -> backendMsg ?: SystemMessages.MSG6
                    }
                    return@withContext Result.failure(IllegalStateException(msg))
                }
            } catch (_: IOException) {
                return@withContext Result.failure(IllegalStateException(SystemMessages.MSG6))
            }
        }

    override suspend fun resetPassword(
        token: String,
        nuevaContrasena: String,
        confirmacionContrasena: String
    ): Result<String> = withContext(ioDispatcher) {
        if (token.isBlank() || nuevaContrasena.isBlank() || confirmacionContrasena.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException(SystemMessages.MSG1))
        }
        if (nuevaContrasena != confirmacionContrasena) {
            return@withContext Result.failure(IllegalArgumentException(SystemMessages.MSG9))
        }
        if (!isValidPasswordPolicy(nuevaContrasena)) {
            return@withContext Result.failure(IllegalArgumentException(SystemMessages.MSG10))
        }

        try {
            val response = authApiService.restablecerPassword(
                RestablecerPasswordRequestDto(
                    token = token.trim(),
                    nuevaContrasena = nuevaContrasena,
                    confirmacionContrasena = confirmacionContrasena
                )
            )
            if (response.isSuccessful) {
                val msg = response.body()?.mensaje ?: "La contraseña fue restablecida correctamente."
                return@withContext Result.success(msg)
            } else {
                val backendMsg = parseBackendErrorMessage(response.errorBody()?.string())
                return@withContext Result.failure(
                    IllegalArgumentException(backendMsg ?: SystemMessages.MSG7)
                )
            }
        } catch (_: IOException) {
            return@withContext Result.failure(IllegalStateException(SystemMessages.MSG7))
        }
    }

    override suspend fun refreshProfile(): Result<BrigadistaSession> = withContext(ioDispatcher) {
        val current = sessionPreferencesManager.getCurrentSession()
            ?: return@withContext Result.failure(IllegalStateException(SystemMessages.MSG11))

        try {
            val response = authApiService.obtenerPerfil()
            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                val updated = current.copy(
                    brigadistaId = dto.idCuenta,
                    correo = dto.correo,
                    nombreCompleto = dto.nombreCompleto,
                    rol = dto.rol,
                    estadoCuenta = dto.estadoCuenta,
                    fechaCreacionCuenta = dto.fechaCreacion,
                    isOfflineSession = false
                )
                sessionPreferencesManager.saveSession(updated)
                return@withContext Result.success(updated)
            }
        } catch (_: Exception) {
            // Conservar perfil local en modo offline
        }
        Result.success(current)
    }

    override suspend fun checkBackendHealth(): Result<Boolean> = withContext(ioDispatcher) {
        try {
            val response = authApiService.checkHealth()
            val isUp = response.isSuccessful && response.body()?.status.equals("UP", ignoreCase = true)
            Result.success(isUp)
        } catch (_: Exception) {
            Result.success(false)
        }
    }

    override suspend fun logout(): Result<Unit> = withContext(ioDispatcher) {
        val current = sessionPreferencesManager.getCurrentSession()
            ?: return@withContext Result.failure(IllegalStateException(SystemMessages.MSG11))

        if (!current.accessToken.isNullOrBlank()) {
            try {
                authApiService.logout()
            } catch (_: Exception) {
                // Permitir el cierre local de sesión aunque no haya red
            }
        }
        sessionPreferencesManager.clearActiveSession()
        Result.success(Unit)
    }

    private fun decodeJwtClaims(jwt: String): JsonObject? {
        return try {
            val parts = jwt.split(".")
            if (parts.size < 2) return null
            val payloadBytes = Base64.decode(
                parts[1],
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            )
            val payloadJson = String(payloadBytes, Charsets.UTF_8)
            gson.fromJson(payloadJson, JsonObject::class.java)
        } catch (_: Exception) {
            null
        }
    }

    private fun parseBackendErrorMessage(rawBody: String?): String? {
        if (rawBody.isNullOrBlank()) return null
        return try {
            val dto = gson.fromJson(rawBody, MensajeResponseDto::class.java)
            dto.mensaje ?: dto.message
        } catch (_: Exception) {
            null
        }
    }

    private fun isValidPasswordPolicy(password: String): Boolean {
        if (password.length < 8) return false
        val hasUpper = password.any { it.isUpperCase() }
        val hasLower = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecial = password.any { !it.isLetterOrDigit() }
        return hasUpper && hasLower && hasDigit && hasSpecial
    }
}
