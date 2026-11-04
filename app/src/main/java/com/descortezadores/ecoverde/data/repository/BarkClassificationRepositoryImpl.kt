package com.descortezadores.ecoverde.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.descortezadores.ecoverde.data.classification.TfliteBarkClassifier
import com.descortezadores.ecoverde.data.remote.api.MonitoringApiService
import com.descortezadores.ecoverde.di.IoDispatcher
import com.descortezadores.ecoverde.domain.model.ClassificationOutcome
import com.descortezadores.ecoverde.domain.model.ClassificationResult
import com.descortezadores.ecoverde.domain.repository.BarkClassificationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación del repositorio de clasificación fitosanitaria de corteza.
 * Aplica estrategia híbrida:
 * - Online: ejecuta el modelo de alta precisión `classifier_full` en AWS Lambda (EfficientNet-B3).
 * - Offline o falla de red: conmuta de forma transparente e inmediata a `light_classifier.tflite` local.
 */
@Singleton
class BarkClassificationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tfliteBarkClassifier: TfliteBarkClassifier,
    private val monitoringApiService: MonitoringApiService,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : BarkClassificationRepository {

    override suspend fun classifyImageOffline(imagePath: String): Result<ClassificationResult> {
        return tfliteBarkClassifier.classifyImageFile(imagePath)
    }

    override suspend fun classifyBark(bitmap: Bitmap): Result<ClassificationResult> = withContext(ioDispatcher) {
        if (isNetworkAvailable()) {
            try {
                // Inferencia en la nube con timeout preventivo de 7s
                val cloudResult = withTimeoutOrNull(CLOUD_INFERENCE_TIMEOUT_MS) {
                    classifyViaCloud(bitmap)
                }

                if (cloudResult != null && cloudResult.isSuccess) {
                    Log.i(TAG, "Inferencia completada exitosamente en la NUBE (AWS Lambda).")
                    return@withContext cloudResult
                } else {
                    Log.w(TAG, "Inferencia en la nube falló o excedió tiempo límite. Conmutando a inferencia LOCAL.")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Excepción durante inferencia en la nube: ${e.message}. Conmutando a inferencia LOCAL.")
            }
        } else {
            Log.i(TAG, "Dispositivo offline o sin conexión a Internet. Ejecutando inferencia LOCAL.")
        }

        // Fallback local garantizado con TFLite
        tfliteBarkClassifier.classifyBitmap(bitmap)
    }

    private fun isNetworkAvailable(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun classifyViaCloud(bitmap: Bitmap): Result<ClassificationResult> {
        val outputStream = ByteArrayOutputStream()
        // Calidad 85% para balance óptimo de transferencia y fidelidad visual
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val imageBytes = outputStream.toByteArray()

        val requestBody = imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("imagen", "corteza.jpg", requestBody)

        val response = monitoringApiService.clasificarCortezaCloud(part)
        if (!response.isSuccessful || response.body() == null) {
            val errorMsg = response.errorBody()?.string() ?: response.message()
            return Result.failure(IllegalStateException("Error en servidor nube: ${response.code()} $errorMsg"))
        }

        val dto = response.body()!!
        val (outcome, confidence) = ClassificationOutcome.evaluateFromProbability(
            infestationProbability = dto.probabilidadInfestado,
            decisionThreshold = dto.umbral,
            enforceStrictRn24 = false
        )

        val visualEvidences = buildVisualEvidences(outcome, dto.probabilidadInfestado)
        val recommendation = buildRecommendation(outcome, confidence, dto.probabilidadInfestado)

        return Result.success(
            ClassificationResult(
                outcome = outcome,
                confidence = confidence,
                rawInfestationProbability = dto.probabilidadInfestado,
                isOfflineExecution = false,
                executionBadgeLabel = "NUBE",
                visualEvidencesDetected = visualEvidences,
                recommendation = recommendation,
                inferenceTimeMs = dto.tiempoInferenciaMs
            )
        )
    }

    private fun buildVisualEvidences(outcome: ClassificationOutcome, rawProb: Double): List<String> =
        when (outcome) {
            ClassificationOutcome.INFESTADO -> listOf(
                "Patrones texturales compatibles con galerías subcorticales o perforaciones en fuste.",
                "Posibles exudados/grumos de resina o alteración cromática asociada a colonización.",
                "Probabilidad estimada de infestación: ${String.format(Locale.US, "%.1f", rawProb * 100.0)}%."
            )
            ClassificationOutcome.SANO -> listOf(
                "Corteza con textura continua sin evidencia dominante de galerías ni grumos de resina.",
                "Ausencia de señales visuales críticas de colonización activa.",
                "Probabilidad estimada de infestación: ${String.format(Locale.US, "%.1f", rawProb * 100.0)}%."
            )
            ClassificationOutcome.NO_DETERMINADO -> listOf(
                "La certeza del diagnóstico no superó el umbral de confiabilidad.",
                "Posible variación de iluminación, sombras o distancia de captura."
            )
        }

    private fun buildRecommendation(
        outcome: ClassificationOutcome,
        confidence: Double,
        rawProb: Double
    ): String {
        val lowConfidenceNotice = if (confidence < ClassificationOutcome.MIN_CONFIDENCE_THRESHOLD) {
            " (Confianza ${(confidence * 100).toInt()}% menor al umbral; se sugiere segunda toma de confirmación)."
        } else {
            ""
        }
        return when (outcome) {
            ClassificationOutcome.INFESTADO ->
                "Clasificado como INFESTADO (${String.format(Locale.US, "%.1f", rawProb * 100.0)}%). Inspeccionar el ejemplar conforme a NOM-019-SEMARNAT-2017 y revisar árboles en un radio de 100 m.$lowConfidenceNotice"
            ClassificationOutcome.SANO ->
                "Clasificado como SANO (${String.format(Locale.US, "%.1f", rawProb * 100.0)}%). Mantener monitoreo preventivo periódico.$lowConfidenceNotice"
            ClassificationOutcome.NO_DETERMINADO ->
                "Resultado No determinado. Se recomienda repetir la captura acercando el encuadre a la corteza con iluminación uniforme."
        }
    }

    companion object {
        private const val TAG = "BarkClassifRepo"
        private const val CLOUD_INFERENCE_TIMEOUT_MS = 7000L
    }
}
