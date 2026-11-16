package com.descortezadores.ecoverde.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO para la respuesta del microservicio de inferencia en la nube
 * ejecutado sobre AWS Lambda (EfficientNet-B3 Full Precision).
 */
data class CloudClassificationResponseDto(
    @SerializedName("resultado")
    val resultado: String,

    @SerializedName("confianza")
    val confianza: Double,

    @SerializedName("probabilidadInfestado")
    val probabilidadInfestado: Double,

    @SerializedName("umbral")
    val umbral: Double = 0.50,

    @SerializedName("tiempoInferenciaMs")
    val tiempoInferenciaMs: Long = 0L,

    @SerializedName("dimensionesOriginales")
    val dimensionesOriginales: String? = null,

    @SerializedName("modelo")
    val modelo: String? = null,

    @SerializedName("tipoInferencia")
    val tipoInferencia: String? = null,

    @SerializedName("version")
    val version: String? = null
)
