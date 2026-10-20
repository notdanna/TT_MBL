package com.descortezadores.ecoverde.domain.model

import kotlin.math.roundToInt

/**
 * Categoría de clasificación fitosanitaria generada por el modelo TensorFlow Lite (RN-23, RN-24, RN-26).
 */
enum class ClassificationOutcome(
    val apiValue: String,
    val displayTitle: String,
    val statusHexColor: String
) {
    SANO(
        apiValue = "sano",
        displayTitle = "Sano",
        statusHexColor = "#2E7D32" // Verde (RN-26)
    ),
    INFESTADO(
        apiValue = "infestado",
        displayTitle = "Infestado",
        statusHexColor = "#C62828" // Rojo (RN-26)
    ),
    NO_DETERMINADO(
        apiValue = "no_determinado",
        displayTitle = "No determinado",
        statusHexColor = "#F9A825" // Amarillo/Ámbar (RN-26)
    );

    companion object {
        const val MIN_CONFIDENCE_THRESHOLD = 0.75 // RN-24: Umbral de referencia para Modo Estricto

        /**
         * Evalúa la probabilidad emitida por `light_classifier.tflite` (`salida_infestado` en `[0.0, 1.0]`).
         *
         * Nota técnica sobre `models/evaluar_test.py` y `entrenamiento_efficientnetb3.py`:
         * El modelo fue entrenado con `BinaryCrossentropy(label_smoothing=0.1)` y en `evaluar_test.py`
         * la regla de decisión es:
         * `clase_tflite = "Infestado" if prob_tflite >= umbral else "Sano"` (con `umbral = 0.5`).
         *
         * @param infestationProbability Salida sigmoide `prob_tflite` en `[0.0, 1.0]`.
         * @param decisionThreshold Umbral binario (`0.50` por defecto, idéntico a `evaluar_test.py`).
         * @param enforceStrictRn24 Si es `false` (por defecto para paridad 1:1 con `evaluar_test.py`),
         *                          clasifica como `INFESTADO` si `>= 0.50` y `SANO` si `< 0.50`.
         *                          Si es `true`, exige confianza `>= 0.75` para no marcar `NO_DETERMINADO`.
         */
        fun evaluateFromProbability(
            infestationProbability: Double,
            decisionThreshold: Double = 0.50,
            enforceStrictRn24: Boolean = false
        ): Pair<ClassificationOutcome, Double> {
            val clampedProb = infestationProbability.coerceIn(0.0, 1.0)
            val isInfestedRaw = clampedProb >= decisionThreshold
            val classConfidence = if (isInfestedRaw) clampedProb else (1.0 - clampedProb)

            return if (enforceStrictRn24 && classConfidence < MIN_CONFIDENCE_THRESHOLD) {
                NO_DETERMINADO to classConfidence
            } else if (isInfestedRaw) {
                INFESTADO to classConfidence
            } else {
                SANO to classConfidence
            }
        }

        fun fromApiValue(value: String): ClassificationOutcome =
            entries.firstOrNull { it.apiValue.equals(value.trim(), ignoreCase = true) } ?: NO_DETERMINADO
    }
}

/**
 * Resultado inmutable de la inferencia local con TensorFlow Lite (RF-08, RF-09, RN-22 a RN-24, RN-45).
 */
data class ClassificationResult(
    val outcome: ClassificationOutcome,
    val confidence: Double,
    val rawInfestationProbability: Double,
    val isOfflineExecution: Boolean = true,
    val executionBadgeLabel: String = "Offline", // RN-22: Etiquetado explícitamente "Offline"
    val visualEvidencesDetected: List<String>,
    val recommendation: String,
    val inferenceTimeMs: Long
) {
    /**
     * Confianza de la clase ganadora en porcentaje (ej. 92.4%).
     */
    val confidencePercentage: Float
        get() = ((confidence * 10000.0).roundToInt() / 100.0f)

    /**
     * Porcentaje crudo `TFLite (%)` idéntico a `round(float(prob_tflite) * 100, 2)` en `evaluar_test.py`.
     */
    val tfliteRawPercentage: Float
        get() = ((rawInfestationProbability * 10000.0).roundToInt() / 100.0f)
}
