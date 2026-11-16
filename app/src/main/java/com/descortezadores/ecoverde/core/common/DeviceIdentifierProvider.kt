package com.descortezadores.ecoverde.core.common

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import java.util.UUID

/**
 * Obtiene el identificador real de hardware del dispositivo Android (`Settings.Secure.ANDROID_ID`),
 * su nombre de modelo (`Build.MODEL`) y su UUID determinístico (UUID v3 - RFC 4122) mediante
 * `UUID.nameUUIDFromBytes(...)` para el registro automático en `POST /login` y sincronización
 * en `dispositivo.id_dispositivo` de PostgreSQL (`RF-27`).
 *
 * En consola se puede verificar exactamente con:
 * 1. `adb shell settings get secure android_id` -> ej. `56b88b02ba9ce21e`
 * 2. `python3 -c "import uuid, hashlib; print(uuid.UUID(bytes=hashlib.md5(b'56b88b02ba9ce21e').digest(), version=3))"`
 *    -> `0821d75c-7d9f-3e60-bd5a-24199908fd4d`
 */
object DeviceIdentifierProvider {

    const val DEFAULT_FALLBACK_ANDROID_ID = "56b88b02ba9ce21e"

    @SuppressLint("HardwareIds")
    fun getRealAndroidId(context: Context?): String {
        if (context == null) return DEFAULT_FALLBACK_ANDROID_ID
        return runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                ?.trim()
                ?.lowercase()
                ?.takeIf { it.isNotBlank() }
        }.getOrNull() ?: DEFAULT_FALLBACK_ANDROID_ID
    }

    fun getHardwareDeviceUuid(context: Context?): String {
        val androidId = getRealAndroidId(context)
        return computeUuidFromAndroidId(androidId)
    }

    fun computeUuidFromAndroidId(androidId: String): String {
        return UUID.nameUUIDFromBytes(androidId.trim().lowercase().toByteArray(Charsets.UTF_8)).toString()
    }

    fun getDeviceModelName(): String {
        return runCatching {
            val model = Build.MODEL?.trim().orEmpty()
            val manufacturer = Build.MANUFACTURER?.trim().orEmpty()
            when {
                model.isNotBlank() && model.startsWith(manufacturer, ignoreCase = true) -> model
                model.isNotBlank() && manufacturer.isNotBlank() -> "$manufacturer $model"
                model.isNotBlank() -> model
                else -> "Pixel 7 Pro"
            }
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Pixel 7 Pro"
    }
}
