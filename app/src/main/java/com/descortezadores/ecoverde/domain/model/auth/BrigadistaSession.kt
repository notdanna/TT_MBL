package com.descortezadores.ecoverde.domain.model.auth

/**
 * Modelo de dominio inmutable que representa la sesión activa del Brigadista / Experto de campo
 * en el Bosque de Chapultepec (CU-G01, CU-M11, RN-02, RN-03, RF-27).
 */
data class BrigadistaSession(
    val brigadistaId: String,
    val correo: String,
    val nombreCompleto: String,
    val rol: String,
    val estadoCuenta: String,
    val idDispositivo: String,
    val accessToken: String?,
    val isOfflineSession: Boolean,
    val fechaCreacionCuenta: String,
    val loginTimestampMillis: Long
) {
    val isExpertoDeCampo: Boolean
        get() = rol.equals(ROL_EXPERTO_CAMPO, ignoreCase = true)

    companion object {
        const val ROL_EXPERTO_CAMPO = "Experto de campo"
        const val ROL_ADMINISTRADOR = "Administrador"
        const val DEFAULT_BRIGADISTA_ID = "a0000000-0000-0000-0000-000000000002"
        const val DEFAULT_DEVICE_ID = "0821d75c-7d9f-3e60-bd5a-24199908fd4d"
    }
}

/**
 * Resultado de solicitud de recuperación de contraseña (CU-G02, RN-04, RN-06).
 */
data class PasswordRecoveryTicket(
    val mensaje: String,
    val codigoRecuperacion: String,
    val vigenciaHoras: Int
)
