package com.descortezadores.ecoverde.domain.repository

import com.descortezadores.ecoverde.domain.model.ChromaticAnalysisResult
import com.descortezadores.ecoverde.domain.model.DiagnosticCategory
import com.descortezadores.ecoverde.domain.model.SampleType
import kotlinx.coroutines.flow.Flow

/**
 * Metadatos básicos de validación previa de una imagen antes de procesarla (RN-07, RN-08, RN-09).
 */
data class ImagePreconditionMetadata(
    val filePath: String,
    val extension: String,
    val fileSizeBytes: Long,
    val widthPx: Int,
    val heightPx: Int
)

/**
 * Contrato del Repositorio de Segmentación Cromática e Imágenes (Fase 2 del Plan de Implementación).
 * Completamente independiente de Android, OpenCV y Room.
 */
interface ImageSegmentationRepository {

    /**
     * Inspecciona el archivo de imagen para validar sus precondiciones físicas (formato, peso y dimensiones).
     */
    suspend fun inspectImagePreconditions(imagePath: String): Result<ImagePreconditionMetadata>

    /**
     * Ejecuta la segmentación cromática en espacio HSV sobre la fotografía ubicada en [imagePath]
     * de acuerdo con el [sampleType] (`CORTEZA` RN-48 o `FOLLAJE` RN-47), genera la imagen coloreada
     * con alpha blending + contornos y persiste el resultado localmente en SQLite/Room (RN-50).
     *
     * @param imagePath Ruta absoluta de la imagen capturada o importada.
     * @param sampleType Tipo de muestra ([SampleType.CORTEZA] o [SampleType.FOLLAJE]).
     * @param analysisId Identificador UUID opcional del análisis al que pertenece.
     * @param alphaOpacity Opacidad inicial del mezclado alfa en `[0.0f, 1.0f]` (por defecto `0.50f`).
     * @param isolatedCategory Si se especifica, genera un mapa visual aislando únicamente esa categoría diagnóstica.
     */
    suspend fun segmentAndPersistImageColors(
        imagePath: String,
        sampleType: SampleType,
        analysisId: String? = null,
        alphaOpacity: Float = 0.50f,
        isolatedCategory: DiagnosticCategory? = null
    ): Result<ChromaticAnalysisResult>

    /**
     * Regenera dinámicamente el mapa visual superpuesto (overlay) ajustando la opacidad alfa o
     * aislando una categoría fitosanitaria específica mediante los chips de filtro.
     */
    suspend fun renderFilteredOverlay(
        originalImagePath: String,
        sampleType: SampleType,
        alphaOpacity: Float,
        isolatedCategory: DiagnosticCategory?
    ): Result<String>

    /**
     * Observa el historial local de análisis cromáticos almacenados en el dispositivo.
     */
    fun observeLocalChromaticAnalyses(): Flow<List<ChromaticAnalysisResult>>
}
