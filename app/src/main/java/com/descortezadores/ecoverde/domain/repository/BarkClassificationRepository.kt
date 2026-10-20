package com.descortezadores.ecoverde.domain.repository

import com.descortezadores.ecoverde.domain.model.ClassificationResult

/**
 * Contrato de repositorio para la inferencia local con TensorFlow Lite (`light_classifier.tflite`)
 * y actualización OTA del modelo desde el backend (`GET /api/v1/modelos/latest`).
 */
interface BarkClassificationRepository {
    /**
     * Clasifica una fotografía de corteza o follaje localmente en el dispositivo sin conexión (RN-22 a RN-24).
     */
    suspend fun classifyImageOffline(imagePath: String): Result<ClassificationResult>
}
