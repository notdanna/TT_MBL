package com.descortezadores.ecoverde

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.descortezadores.ecoverde.core.common.SystemMessages
import com.descortezadores.ecoverde.data.local.session.SessionPreferencesManager
import com.descortezadores.ecoverde.data.remote.api.AuthApiService
import com.descortezadores.ecoverde.data.remote.dto.HealthResponseDto
import com.descortezadores.ecoverde.data.remote.dto.LoginRequestDto
import com.descortezadores.ecoverde.data.remote.dto.LoginResponseDto
import com.descortezadores.ecoverde.data.remote.dto.LogoutResponseDto
import com.descortezadores.ecoverde.data.remote.dto.MensajeResponseDto
import com.descortezadores.ecoverde.data.remote.dto.PerfilUsuarioDto
import com.descortezadores.ecoverde.data.remote.dto.RecuperarPasswordRequestDto
import com.descortezadores.ecoverde.data.remote.dto.RecuperarPasswordResponseDto
import com.descortezadores.ecoverde.data.remote.dto.RestablecerPasswordRequestDto
import com.descortezadores.ecoverde.data.repository.AuthRepositoryImpl
import com.descortezadores.ecoverde.domain.model.auth.BrigadistaSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.Response
import java.io.IOException

/**
 * Pruebas unitarias de la Fase 1:
 * - Cimientos de Infraestructura (Despachadores de corrutinas)
 * - Módulo Perimetral de Autenticación contra Backend y Persistencia de Sesión Offline (PT-01, PT-02, CU-G01 a CU-G04, RN-02 a RN-06, RNF-06)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class Phase1AuthAndSessionTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private class FakeBackendAuthApi(var isOnline: Boolean = true) : AuthApiService {
        private var currentPassword = "Password456!"
        private var activeRecoveryToken: String? = null
        private var recoveryRequestsCount = 0
        var lastLoginRequest: LoginRequestDto? = null

        override suspend fun checkHealth(): Response<HealthResponseDto> {
            if (!isOnline) throw IOException("Sin red")
            return Response.success(
                HealthResponseDto("UP", "backend-descortezadores", "1.0.0", "2026-10-16T19:09:14Z")
            )
        }

        override suspend fun login(request: LoginRequestDto): Response<LoginResponseDto> {
            if (!isOnline) throw IOException("Sin red")
            lastLoginRequest = request
            return when {
                request.username == "experto@chapultepec.gob.mx" && request.password == currentPassword -> {
                    Response.success(
                        LoginResponseDto(
                            accessToken = "eyJhbGciOiJIUzI1NiJ9.e30.signature",
                            tokenType = "Bearer",
                            expiresIn = 3600,
                            username = request.username,
                            roles = listOf("Experto de campo")
                        )
                    )
                }
                request.username == "admin@chapultepec.gob.mx" && request.password == "Admin123!" -> {
                    Response.success(
                        LoginResponseDto(
                            accessToken = "eyJhbGciOiJIUzI1NiJ9.e30.admin",
                            tokenType = "Bearer",
                            expiresIn = 3600,
                            username = request.username,
                            roles = listOf("Administrador")
                        )
                    )
                }
                else -> {
                    Response.error(
                        401,
                        """{"message":"Credenciales inválidas"}""".toResponseBody("application/json".toMediaType())
                    )
                }
            }
        }

        override suspend fun logout(): Response<LogoutResponseDto> {
            if (!isOnline) throw IOException("Sin red")
            return Response.success(
                LogoutResponseDto("Sesión finalizada correctamente.", "experto@chapultepec.gob.mx")
            )
        }

        override suspend fun recuperarPassword(
            request: RecuperarPasswordRequestDto
        ): Response<RecuperarPasswordResponseDto> {
            if (!isOnline) throw IOException("Sin red")
            if (request.correo != "experto@chapultepec.gob.mx") {
                return Response.error(
                    404,
                    """{"mensaje":"${SystemMessages.MSG2}"}""".toResponseBody("application/json".toMediaType())
                )
            }
            if (recoveryRequestsCount >= 3) {
                return Response.error(
                    429,
                    """{"codigo":"RN-06","mensaje":"Ha excedido el límite de 3 solicitudes cada 30 días."}"""
                        .toResponseBody("application/json".toMediaType())
                )
            }
            recoveryRequestsCount++
            val token = "92DE051B"
            activeRecoveryToken = token
            return Response.success(
                RecuperarPasswordResponseDto(
                    mensaje = "Se ha enviado el enlace o código de recuperación a su correo electrónico.",
                    codigoRecuperacion = token,
                    vigenciaHoras = 24
                )
            )
        }

        override suspend fun restablecerPassword(
            request: RestablecerPasswordRequestDto
        ): Response<MensajeResponseDto> {
            if (!isOnline) throw IOException("Sin red")
            if (activeRecoveryToken == null || activeRecoveryToken != request.token) {
                return Response.error(
                    400,
                    """{"codigo":"ERROR_RESTABLECER","mensaje":"${SystemMessages.MSG7}"}"""
                        .toResponseBody("application/json".toMediaType())
                )
            }
            activeRecoveryToken = null // RN-05: Un solo uso
            currentPassword = request.nuevaContrasena
            return Response.success(
                MensajeResponseDto(
                    mensaje = "La contraseña fue restablecida correctamente.",
                    codigo = null,
                    message = null
                )
            )
        }

        override suspend fun obtenerPerfil(): Response<PerfilUsuarioDto> {
            if (!isOnline) throw IOException("Sin red")
            return Response.success(
                PerfilUsuarioDto(
                    idCuenta = BrigadistaSession.DEFAULT_BRIGADISTA_ID,
                    correo = "experto@chapultepec.gob.mx",
                    nombreCompleto = "Biólogo de Campo",
                    rol = BrigadistaSession.ROL_EXPERTO_CAMPO,
                    estadoCuenta = "Activa",
                    fechaCreacion = "2026-10-16T19:06:59.996180Z"
                )
            )
        }
    }

    private fun createRepository(
        scope: TestScope,
        dispatcher: kotlinx.coroutines.CoroutineDispatcher,
        fakeApi: FakeBackendAuthApi = FakeBackendAuthApi()
    ): Pair<AuthRepositoryImpl, FakeBackendAuthApi> {
        val testFile = tempFolder.newFile("test_session_${System.nanoTime()}.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope.backgroundScope,
            produceFile = { testFile }
        )
        val sessionManager = SessionPreferencesManager(dataStore)
        return AuthRepositoryImpl(
            authApiService = fakeApi,
            sessionPreferencesManager = sessionManager,
            ioDispatcher = dispatcher
        ) to fakeApi
    }

    @Test
    fun `PT-01 login contra backend fija brigadistaId y permite reanudar offline despues`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val (repo, fakeApi) = createRepository(this, dispatcher)

        // 1. Login online contra el backend
        val result = repo.login("experto@chapultepec.gob.mx", "Password456!")
        assertTrue(result.isSuccess)

        val session = result.getOrNull()
        assertNotNull(session)
        assertEquals(BrigadistaSession.DEFAULT_BRIGADISTA_ID, session!!.brigadistaId)
        assertEquals(BrigadistaSession.ROL_EXPERTO_CAMPO, session.rol)
        assertFalse(session.isOfflineSession)
        assertEquals("0821d75c-7d9f-3e60-bd5a-24199908fd4d", fakeApi.lastLoginRequest?.idDispositivo)
        assertEquals("56b88b02ba9ce21e", fakeApi.lastLoginRequest?.identificadorUnico)
        assertFalse(fakeApi.lastLoginRequest?.nombreDispositivo.isNullOrBlank())

        // 2. Cerrar sesión y simular pérdida de señal en el Bosque de Chapultepec
        assertTrue(repo.logout().isSuccess)
        assertNull(repo.getCurrentSession())

        fakeApi.isOnline = false
        val offlineRelogin = repo.login("experto@chapultepec.gob.mx", "Password456!")
        assertTrue(offlineRelogin.isSuccess)
        assertTrue(offlineRelogin.getOrNull()!!.isOfflineSession)
    }

    @Test
    fun `PT-02 login fallido retorna mensajes de error y rechaza rol Administrador en movil (RN-02)`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val (repo, _) = createRepository(this, dispatcher)

        // Campos vacíos -> MSG1
        val emptyResult = repo.login("", "")
        assertEquals(SystemMessages.MSG1, emptyResult.exceptionOrNull()?.message)

        // Rol Administrador en app móvil -> MSG5 (RN-02)
        val adminResult = repo.login("admin@chapultepec.gob.mx", "Admin123!")
        assertEquals(SystemMessages.MSG5, adminResult.exceptionOrNull()?.message)
    }

    @Test
    fun `CU-G02 y CU-G03 recuperacion de contrasena contra backend valida RN-05 y RN-06`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val (repo, _) = createRepository(this, dispatcher)

        val req1 = repo.requestPasswordRecovery("experto@chapultepec.gob.mx")
        assertTrue(req1.isSuccess)
        val token1 = requireNotNull(req1.getOrNull()!!.codigoRecuperacion)

        // Restablecer con contraseñas que no coinciden -> MSG9
        val mismatch = repo.resetPassword(token1, "NuevaPass123!", "OtraPass123!")
        assertEquals(SystemMessages.MSG9, mismatch.exceptionOrNull()?.message)

        // Restablecer con contraseña débil -> MSG10
        val weak = repo.resetPassword(token1, "debil", "debil")
        assertEquals(SystemMessages.MSG10, weak.exceptionOrNull()?.message)

        // Restablecer válido -> Éxito
        val resetOk = repo.resetPassword(token1, "NuevaPass123!", "NuevaPass123!")
        assertTrue(resetOk.isSuccess)

        // RN-05: Reutilizar el mismo token debe fallar con MSG7
        val reuse = repo.resetPassword(token1, "OtraNueva123!", "OtraNueva123!")
        assertEquals(SystemMessages.MSG7, reuse.exceptionOrNull()?.message)
    }

    @Test
    fun `RNF-06 hash de contrasena SHA-256 es deterministico y sensible a correo y contrasena`() {
        val hash1 = SessionPreferencesManager.hashPassword("experto@chapultepec.gob.mx", "Password456!")
        val hash2 = SessionPreferencesManager.hashPassword("EXPERTO@chapultepec.gob.mx ", "Password456!")
        val hashDiff = SessionPreferencesManager.hashPassword("experto@chapultepec.gob.mx", "OtraPassword456!")

        assertEquals(hash1, hash2)
        assertNotEquals(hash1, hashDiff)
        assertEquals(64, hash1.length)
    }
}
