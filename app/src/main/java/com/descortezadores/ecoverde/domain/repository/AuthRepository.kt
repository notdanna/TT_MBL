package com.descortezadores.ecoverde.domain.repository

import com.descortezadores.ecoverde.domain.model.auth.BrigadistaSession
import com.descortezadores.ecoverde.domain.model.auth.PasswordRecoveryTicket
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de repositorio para autenticación, sesión offline-first y perfil del brigadista
 * (CU-G01 a CU-G04, CU-M11, RN-01 a RN-06).
 */
interface AuthRepository {
    /**
     * Flujo reactivo con la sesión activa del brigadista. Emite `null` si no hay sesión iniciada.
     * Conforme a RN-03, la sesión móvil persiste hasta que el usuario la cierre manualmente.
     */
    val activeSessionFlow: Flow<BrigadistaSession?>

    /**
     * Obtiene la sesión actual de manera síncrona/suspensiva.
     */
    suspend fun getCurrentSession(): BrigadistaSession?

    /**
     * Inicia sesión (CU-G01). Intenta autenticar contra `POST /login` y `GET /api/v1/auth/perfil`;
     * si el dispositivo se encuentra sin cobertura en el Bosque de Chapultepec, valida contra el
     * almacén local seguro (DataStore con hash SHA-256 conforme a RNF-06).
     */
    suspend fun login(correo: String, password: String): Result<BrigadistaSession>

    /**
     * Inicio de sesión rápido en modo Brigadista Offline fijando directamente el brigadistaId
     * y perfil operativo para trabajo en campo sin conectividad previa.
     */
    suspend fun startOfflineBrigadistaSession(
        brigadistaId: String,
        nombreCompleto: String,
        correo: String
    ): Result<BrigadistaSession>

    /**
     * Solicita código de recuperación de contraseña (CU-G02, RN-04, RN-06).
     */
    suspend fun requestPasswordRecovery(correo: String): Result<PasswordRecoveryTicket>

    /**
     * Restablece la contraseña utilizando el código de un solo uso (CU-G03, RN-05).
     */
    suspend fun resetPassword(
        token: String,
        nuevaContrasena: String,
        confirmacionContrasena: String
    ): Result<String>

    /**
     * Refresca los datos del perfil desde `GET /api/v1/auth/perfil` (CU-M11) cuando hay red.
     */
    suspend fun refreshProfile(): Result<BrigadistaSession>

    /**
     * Verifica conectividad real con el backend (`GET /api/v1/health`).
     */
    suspend fun checkBackendHealth(): Result<Boolean>

    /**
     * Cierra formalmente la sesión en el backend (si hay red) y limpia la sesión local (CU-G04).
     */
    suspend fun logout(): Result<Unit>
}
