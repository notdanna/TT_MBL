package com.descortezadores.ecoverde.domain.usecase

import com.descortezadores.ecoverde.core.common.SystemMessages
import com.descortezadores.ecoverde.domain.model.BiologicalSeverityLevel
import com.descortezadores.ecoverde.domain.model.ChromaticAnalysisResult
import com.descortezadores.ecoverde.domain.model.ColorBandResult
import com.descortezadores.ecoverde.domain.model.DiagnosticCategory
import com.descortezadores.ecoverde.domain.model.SampleType
import com.descortezadores.ecoverde.domain.repository.ImagePreconditionMetadata
import com.descortezadores.ecoverde.domain.repository.ImageSegmentationRepository
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Caso de Uso de Orquestación (`AnalyzeImageColorsUseCase` - Fase 2 del Plan de Implementación).
 *
 * Responsabilidades en Kotlin puro:
 * 1. Validar las precondiciones de la imagen conforme a:
 *    - RN-07: Formatos aceptados: JPG, JPEG, PNG, WEBP, BMP, HEIC.
 *    - RN-08: Tamaño máximo de archivo: 10 MB (10,485,760 bytes).
 *    - RN-09: Resolución mínima: 300 × 300 píxeles.
 * 2. Invocar la segmentación cromática independiente por tipo de muestra (RN-47 Follaje vs RN-48 Corteza, RN-49).
 * 3. Calcular la métrica global de severidad biológica y clasificar el nivel de riesgo fitosanitario.
 */
class AnalyzeImageColorsUseCase @Inject constructor(
    private val imageSegmentationRepository: ImageSegmentationRepository
) {
    suspend operator fun invoke(
        imagePath: String,
        sampleType: SampleType,
        analysisId: String? = null,
        alphaOpacity: Float = DEFAULT_ALPHA_OPACITY,
        isolatedCategory: DiagnosticCategory? = null
    ): Result<ChromaticAnalysisResult> {
        if (imagePath.isBlank()) {
            return Result.failure(IllegalArgumentException(SystemMessages.MSG19))
        }

        val clampedAlpha = alphaOpacity.coerceIn(0.0f, 1.0f)

        // 1. Inspeccionar y validar precondiciones físicas de la imagen (RN-07, RN-08, RN-09)
        val metadataResult = imageSegmentationRepository.inspectImagePreconditions(imagePath)
        val metadata = metadataResult.getOrElse { error ->
            return Result.failure(IllegalArgumentException(error.message ?: SystemMessages.MSG19))
        }

        val validationError = validateImagePreconditions(metadata)
        if (validationError != null) {
            return Result.failure(IllegalArgumentException(validationError))
        }

        // 2. Invocar la segmentación cromática HSV según el tipo de muestra (RN-47, RN-48, RN-49)
        val rawResult = imageSegmentationRepository.segmentAndPersistImageColors(
            imagePath = imagePath,
            sampleType = sampleType,
            analysisId = analysisId,
            alphaOpacity = clampedAlpha,
            isolatedCategory = isolatedCategory
        ).getOrElse { error ->
            return Result.failure(error)
        }

        // 3. Recalcular y verificar la métrica global de severidad biológica
        val severityScore = calculateBiologicalSeverityScore(
            pctDanado = rawResult.pctDanado,
            pctRegular = rawResult.pctRegular,
            pctIncierto = rawResult.pctIncierto
        )
        val severityLevel = classifyBiologicalSeverity(
            pctDanado = rawResult.pctDanado,
            pctRegular = rawResult.pctRegular,
            pctSano = rawResult.pctSano,
            pctIncierto = rawResult.pctIncierto
        )
        val dominantCategory = determineDominantCategory(rawResult.bands)

        return Result.success(
            rawResult.copy(
                biologicalSeverityScore = severityScore,
                severityLevel = severityLevel,
                dominantCategory = dominantCategory
            )
        )
    }

    /**
     * Valida las reglas de negocio RN-07, RN-08 y RN-09 sobre los metadatos de la imagen.
     * Retorna `null` si la imagen es válida, o el mensaje de error oficial ([SystemMessages.MSG19]) con detalle.
     */
    fun validateImagePreconditions(metadata: ImagePreconditionMetadata): String? {
        val ext = metadata.extension.trim().lowercase().removePrefix(".")
        if (ext !in ALLOWED_EXTENSIONS) {
            return "${SystemMessages.MSG19} (RN-07: Formato '$ext' no permitido. Use JPG, JPEG, PNG, WEBP, BMP o HEIC)."
        }
        if (metadata.fileSizeBytes <= 0L || metadata.fileSizeBytes > MAX_IMAGE_SIZE_BYTES) {
            return "${SystemMessages.MSG19} (RN-08: El tamaño excede el límite máximo de 10 MB)."
        }
        if (metadata.widthPx < MIN_DIMENSION_PX || metadata.heightPx < MIN_DIMENSION_PX) {
            return "${SystemMessages.MSG19} (RN-09: Resolución mínima requerida de 300×300 px; recibida ${metadata.widthPx}×${metadata.heightPx} px)."
        }
        return null
    }

    companion object {
        const val DEFAULT_ALPHA_OPACITY = 0.50f
        const val MIN_DIMENSION_PX = 300 // RN-09: 300x300 px
        const val MAX_IMAGE_SIZE_BYTES = 10L * 1024L * 1024L // RN-08: 10 MB

        // RN-07: Formatos aceptados
        val ALLOWED_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "bmp", "heic")

        /**
         * Calcula el índice compuesto de severidad biológica en `[0.0, 1.0]` ponderando:
         * - Tejido Dañado (peso 1.0)
         * - Tejido Regular / Resina / Clorosis (peso 0.45)
         * - Región Incierta (peso 0.15)
         */
        fun calculateBiologicalSeverityScore(
            pctDanado: Double,
            pctRegular: Double,
            pctIncierto: Double
        ): Double {
            val rawScore = (pctDanado * DiagnosticCategory.DANADO.severityWeight) +
                (pctRegular * DiagnosticCategory.REGULAR.severityWeight) +
                (pctIncierto * DiagnosticCategory.INCIERTO.severityWeight)
            return ((rawScore.coerceIn(0.0, 1.0) * 10000.0).roundToInt()) / 10000.0
        }

        /**
         * Determina el nivel global de severidad biológica a partir de las proporciones HSV.
         */
        fun classifyBiologicalSeverity(
            pctDanado: Double,
            pctRegular: Double,
            pctSano: Double,
            pctIncierto: Double
        ): BiologicalSeverityLevel {
            if (pctIncierto >= 0.55) {
                return BiologicalSeverityLevel.NO_CONCLUYENTE
            }
            val score = calculateBiologicalSeverityScore(pctDanado, pctRegular, pctIncierto)
            return when {
                pctDanado >= 0.30 || score >= 0.40 -> BiologicalSeverityLevel.ALTA
                pctRegular >= 0.25 || pctDanado >= 0.12 || score >= 0.20 -> BiologicalSeverityLevel.MODERADA
                else -> BiologicalSeverityLevel.BAJA
            }
        }

        fun determineDominantCategory(bands: List<ColorBandResult>): DiagnosticCategory {
            return bands.maxByOrNull { it.pixelCount }?.category ?: DiagnosticCategory.INCIERTO
        }
    }
}
