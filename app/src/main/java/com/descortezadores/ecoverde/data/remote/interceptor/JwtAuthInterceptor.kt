package com.descortezadores.ecoverde.data.remote.interceptor

import android.content.Context
import android.util.Log
import com.descortezadores.ecoverde.core.common.DeviceIdentifierProvider
import com.descortezadores.ecoverde.data.local.session.SessionPreferencesManager
import com.google.gson.JsonParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.BufferedSink
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interceptor OkHttp que:
 * 1. Adjunta automáticamente el token JWT Bearer en las peticiones autenticadas hacia el backend.
 * 2. Normaliza el Content-Type de cuerpos JSON a `application/json` exacto (sin `; charset=UTF-8`),
 *    requerido por el endpoint `POST /login` del backend.
 * 3. Cumple con `RN-03` (la sesión móvil permanece activa hasta el cierre manual): si el JWT de 1 hora
 *    (`expires_in: 3600`) expiró y el backend responde `401 Unauthorized` (o si la sesión se inició
 *    offline y aún no tenía token), renueva silenciosamente el JWT contra `/login` enviando también
 *    `idDispositivo`, `nombreDispositivo` e `identificadorUnico` para el auto-registro del dispositivo.
 */
@Singleton
class JwtAuthInterceptor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionPreferencesManager: SessionPreferencesManager
) : Interceptor {

    private val refreshLock = Any()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val path = originalRequest.url.encodedPath

        val builder = originalRequest.newBuilder()

        // Si el cuerpo es JSON, asegurar que Content-Type sea exactamente "application/json"
        val body = originalRequest.body
        if (body != null && body.contentType()?.subtype.equals("json", ignoreCase = true)) {
            builder.method(originalRequest.method, ExactJsonRequestBody(body))
            builder.header("Content-Type", "application/json")
        }

        // Endpoints públicos según Guía de uso de APIs del backend
        val isPublicEndpoint = path.endsWith("/login") ||
            path.endsWith("/api/v1/health") ||
            path.endsWith("/api/v1/auth/recuperar-password") ||
            path.endsWith("/api/v1/auth/restablecer-password") ||
            path.contains("/api/v1/modelos/")

        if (!isPublicEndpoint && originalRequest.header("Authorization") == null) {
            var token = runBlocking { sessionPreferencesManager.getAccessToken() }
            if (token.isNullOrBlank()) {
                token = trySilentTokenRefresh(chain, originalRequest)
            }
            if (!token.isNullOrBlank()) {
                builder.header("Authorization", "Bearer $token")
            }
        }

        val initialResponse = chain.proceed(builder.build())

        // Si el token JWT expiró (HTTP 401) en un endpoint protegido, renovarlo silenciosamente (RN-03)
        if (!isPublicEndpoint && initialResponse.code == 401) {
            synchronized(refreshLock) {
                val refreshedToken = trySilentTokenRefresh(chain, originalRequest)
                if (!refreshedToken.isNullOrBlank()) {
                    initialResponse.close()
                    val retryBuilder = originalRequest.newBuilder()
                    if (body != null && body.contentType()?.subtype.equals("json", ignoreCase = true)) {
                        retryBuilder.method(originalRequest.method, ExactJsonRequestBody(body))
                        retryBuilder.header("Content-Type", "application/json")
                    }
                    retryBuilder.header("Authorization", "Bearer $refreshedToken")
                    return chain.proceed(retryBuilder.build())
                }
            }
        }

        return initialResponse
    }

    private fun trySilentTokenRefresh(
        chain: Interceptor.Chain,
        referenceRequest: Request
    ): String? {
        return try {
            val creds = runBlocking { sessionPreferencesManager.getCredentialsForAutoRefresh() }
                ?: return null

            val loginUrl = referenceRequest.url.newBuilder()
                .encodedPath("/login")
                .query(null)
                .build()

            val jsonPayload = com.google.gson.JsonObject().apply {
                addProperty("username", creds.first)
                addProperty("password", creds.second)
                addProperty("idDispositivo", DeviceIdentifierProvider.getHardwareDeviceUuid(context))
                addProperty("nombreDispositivo", DeviceIdentifierProvider.getDeviceModelName())
                addProperty("identificadorUnico", DeviceIdentifierProvider.getRealAndroidId(context))
            }.toString()

            val rawBody = jsonPayload.toRequestBody(APPLICATION_JSON)
            val loginRequest = Request.Builder()
                .url(loginUrl)
                .post(ExactJsonRequestBody(rawBody))
                .header("Content-Type", "application/json")
                .build()

            chain.proceed(loginRequest).use { loginResp ->
                if (!loginResp.isSuccessful) {
                    Log.w(TAG, "Renovación silenciosa de JWT falló con HTTP ${loginResp.code}")
                    return null
                }
                val respStr = loginResp.body?.string() ?: return null
                val jsonObj = JsonParser.parseString(respStr).asJsonObject
                val newToken = jsonObj.get("access_token")?.asString
                if (!newToken.isNullOrBlank()) {
                    runBlocking {
                        sessionPreferencesManager.updateAccessToken(newToken)
                    }
                    Log.i(TAG, "Token JWT renovado silenciosamente y dispositivo auto-registrado con éxito (RN-03).")
                    return newToken
                }
                null
            }
        } catch (t: Throwable) {
            Log.w(TAG, "No fue posible renovar silenciosamente el JWT", t)
            null
        }
    }

    private class ExactJsonRequestBody(
        private val delegate: RequestBody
    ) : RequestBody() {
        override fun contentType(): MediaType = APPLICATION_JSON
        override fun contentLength(): Long = delegate.contentLength()
        override fun writeTo(sink: BufferedSink) = delegate.writeTo(sink)
    }

    companion object {
        private const val TAG = "JwtAuthInterceptor"
        private val APPLICATION_JSON = "application/json".toMediaType()
    }
}
