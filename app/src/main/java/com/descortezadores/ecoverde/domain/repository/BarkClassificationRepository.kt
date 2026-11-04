package com.descortezadores.ecoverde.domain.repository

import android.graphics.Bitmap
import com.descortezadores.ecoverde.domain.model.ClassificationResult

/**
 * Contrato de repositorio para la inferencia de corteza con estrategia híbrida:
 * modelo grande en la nube (AWS Lambda EfficientNet-B3) cuando hay conexión online,
 * con fallback automático y local a TensorFlow Lite (`light_classifier.tflite`) en modo offline.
 */
interface BarkClassificationRepository {
    /**
     * Clasifica una fotografía de corteza localmente en el dispositivo sin conexión (RN-22 a RN-24).
     */
    suspend fun classifyImageOffline(imagePath: String): Result<ClassificationResult>

    /**
     * Clasifica una muestra de corteza con estrategia híbrida:
     * Consulta el modelo de alta precisión en la nube si hay red disponible; si no hay conexión
     * o la solicitud falla/agota el tiempo, conmuta inmediatamente a inferencia local con TFLite.
     */
    suspend fun classifyBark(bitmap: Bitmap): Result<ClassificationResult>
}
