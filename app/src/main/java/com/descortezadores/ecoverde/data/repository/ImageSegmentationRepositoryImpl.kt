package com.descortezadores.ecoverde.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorSpace
import android.os.SystemClock
import com.descortezadores.ecoverde.core.common.SystemMessages
import com.descortezadores.ecoverde.data.local.dao.AnalisisCromaticoDao
import com.descortezadores.ecoverde.data.local.entity.AnalisisCromaticoEntity
import com.descortezadores.ecoverde.data.segmentation.ImageOverlayGenerator
import com.descortezadores.ecoverde.data.segmentation.OpenCvColorSegmenter
import com.descortezadores.ecoverde.di.DefaultDispatcher
import com.descortezadores.ecoverde.di.IoDispatcher
import com.descortezadores.ecoverde.domain.model.ChromaticAnalysisResult
import com.descortezadores.ecoverde.domain.model.ColorBandResult
import com.descortezadores.ecoverde.domain.model.DiagnosticCategory
import com.descortezadores.ecoverde.domain.model.SampleType
import com.descortezadores.ecoverde.domain.repository.ImagePreconditionMetadata
import com.descortezadores.ecoverde.domain.repository.ImageSegmentationRepository
import com.descortezadores.ecoverde.domain.usecase.AnalyzeImageColorsUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación del puerto [ImageSegmentationRepository] (Fase 4).
 *
 * Coordina:
 * 1. Inspección de precondiciones (`RN-07`, `RN-08`, `RN-09`).
 * 2. Segmentación matemática HSV mediante [OpenCvColorSegmenter] en `@DefaultDispatcher`.
 * 3. Generación de la Imagen Coloreada por Secciones (alpha blending + contornos) mediante [ImageOverlayGenerator].
 * 4. Almacenamiento de los archivos de imagen en `context.filesDir/chromatic_analyses/` y
 *    persistencia transaccional de los metadatos en SQLite Room ([AnalisisCromaticoDao]) en `@IoDispatcher` (`RN-50`).
 */
@Singleton
class ImageSegmentationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val openCvColorSegmenter: OpenCvColorSegmenter,
    private val imageOverlayGenerator: ImageOverlayGenerator,
    private val analisisCromaticoDao: AnalisisCromaticoDao,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ImageSegmentationRepository {

    override suspend fun inspectImagePreconditions(imagePath: String): Result<ImagePreconditionMetadata> =
        withContext(ioDispatcher) {
            try {
                val file = File(imagePath)
                if (!file.exists() || !file.canRead()) {
                    return@withContext Result.failure(IllegalArgumentException(SystemMessages.MSG19))
                }

                val boundsOptions = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(file.absolutePath, boundsOptions)

                val ext = file.extension.lowercase().ifBlank { "jpg" }
                Result.success(
                    ImagePreconditionMetadata(
                        filePath = file.absolutePath,
                        extension = ext,
                        fileSizeBytes = file.length(),
                        widthPx = boundsOptions.outWidth.coerceAtLeast(0),
                        heightPx = boundsOptions.outHeight.coerceAtLeast(0)
                    )
                )
            } catch (t: Throwable) {
                Result.failure(IllegalArgumentException(SystemMessages.MSG19, t))
            }
        }

    override suspend fun segmentAndPersistImageColors(
        imagePath: String,
        sampleType: SampleType,
        analysisId: String?,
        alphaOpacity: Float,
        isolatedCategory: DiagnosticCategory?
    ): Result<ChromaticAnalysisResult> = withContext(ioDispatcher) {
        try {
            val sourceFile = File(imagePath)
            if (!sourceFile.exists() || !sourceFile.canRead()) {
                return@withContext Result.failure(IllegalArgumentException(SystemMessages.MSG19))
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inScaled = false
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inPreferredColorSpace = ColorSpace.get(ColorSpace.Named.SRGB)
            }
            val decodedBitmap = BitmapFactory.decodeFile(sourceFile.absolutePath, decodeOptions)
                ?: return@withContext Result.failure(IllegalArgumentException(SystemMessages.MSG19))

            // Ejecutar segmentación HSV y composición de overlay en DefaultDispatcher (CPU-bound)
            val startSegmentedMs = SystemClock.elapsedRealtime()
            val (overlayBitmap, ratiosByCategory, countsAndTotal) = withContext(defaultDispatcher) {
                val masks = openCvColorSegmenter.segmentBitmapToMasks(decodedBitmap, sampleType)
                try {
                    val ovl = imageOverlayGenerator.generateOverlayBitmap(
                        segmentationMasks = masks,
                        sampleType = sampleType,
                        alphaOpacity = alphaOpacity,
                        isolatedCategory = isolatedCategory
                    )
                    Triple(
                        ovl,
                        masks.ratiosByCategory,
                        masks.pixelCountsByCategory to masks.totalPixels
                    )
                } finally {
                    masks.releaseMasks()
                }
            }
            val processingTimeMs = (SystemClock.elapsedRealtime() - startSegmentedMs).coerceAtLeast(1L)

            val effectiveId = analysisId?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
            val storageDir = File(context.filesDir, CHROMATIC_DIR_NAME).apply { mkdirs() }

            val persistedOriginalFile = File(storageDir, "orig_$effectiveId.jpg")
            val persistedOverlayFile = File(storageDir, "ovl_$effectiveId.jpg")

            saveBitmapAsJpeg(decodedBitmap, persistedOriginalFile, quality = 95)
            saveBitmapAsJpeg(overlayBitmap, persistedOverlayFile, quality = 92)

            val pctDanado = ratiosByCategory[DiagnosticCategory.DANADO] ?: 0.0
            val pctRegular = ratiosByCategory[DiagnosticCategory.REGULAR] ?: 0.0
            val pctSano = ratiosByCategory[DiagnosticCategory.SANO] ?: 0.0
            val pctIncierto = ratiosByCategory[DiagnosticCategory.INCIERTO] ?: 0.0

            val orderedBands = listOf(
                DiagnosticCategory.DANADO,
                DiagnosticCategory.REGULAR,
                DiagnosticCategory.SANO,
                DiagnosticCategory.INCIERTO
            ).map { category ->
                val ratio = (ratiosByCategory[category] ?: 0.0).coerceIn(0.0, 1.0)
                ColorBandResult(
                    category = category,
                    pixelCount = (countsAndTotal.first[category] ?: 0).coerceAtLeast(0),
                    ratio = ratio,
                    percentage = (ratio * 100.0).toFloat(),
                    label = category.biologicalSignalDescription(sampleType),
                    colorHex = category.officialHexColor(sampleType)
                )
            }

            val severityScore = AnalyzeImageColorsUseCase.calculateBiologicalSeverityScore(
                pctDanado = pctDanado,
                pctRegular = pctRegular,
                pctIncierto = pctIncierto
            )
            val severityLevel = AnalyzeImageColorsUseCase.classifyBiologicalSeverity(
                pctDanado = pctDanado,
                pctRegular = pctRegular,
                pctSano = pctSano,
                pctIncierto = pctIncierto
            )
            val dominantCategory = AnalyzeImageColorsUseCase.determineDominantCategory(orderedBands)

            val nowMillis = System.currentTimeMillis()
            val domainResult = ChromaticAnalysisResult(
                analysisId = effectiveId,
                sampleType = sampleType,
                originalImagePath = persistedOriginalFile.absolutePath,
                segmentedOverlayImagePath = persistedOverlayFile.absolutePath,
                totalPixelsAnalyzed = countsAndTotal.second,
                bands = orderedBands,
                pctSano = pctSano,
                pctRegular = pctRegular,
                pctDanado = pctDanado,
                pctIncierto = pctIncierto,
                biologicalSeverityScore = severityScore,
                severityLevel = severityLevel,
                dominantCategory = dominantCategory,
                timestampMillis = nowMillis
            )

            // Persistir automáticamente en Room SQLite (RN-50)
            val entity = AnalisisCromaticoEntity.fromDomainModel(
                result = domainResult,
                parentAnalisisId = effectiveId,
                processingTimeMs = processingTimeMs
            )
            analisisCromaticoDao.upsertAnalisisCromatico(entity)

            Result.success(domainResult)
        } catch (t: Throwable) {
            Result.failure(IllegalStateException(SystemMessages.MSG33, t))
        }
    }

    override suspend fun renderFilteredOverlay(
        originalImagePath: String,
        sampleType: SampleType,
        alphaOpacity: Float,
        isolatedCategory: DiagnosticCategory?
    ): Result<String> = withContext(ioDispatcher) {
        try {
            val origFile = File(originalImagePath)
            if (!origFile.exists() || !origFile.canRead()) {
                return@withContext Result.failure(IllegalArgumentException(SystemMessages.MSG19))
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inScaled = false
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inPreferredColorSpace = ColorSpace.get(ColorSpace.Named.SRGB)
            }
            val bitmap = BitmapFactory.decodeFile(origFile.absolutePath, decodeOptions)
                ?: return@withContext Result.failure(IllegalArgumentException(SystemMessages.MSG19))

            val overlayBitmap = withContext(defaultDispatcher) {
                val masks = openCvColorSegmenter.segmentBitmapToMasks(bitmap, sampleType)
                try {
                    imageOverlayGenerator.generateOverlayBitmap(
                        segmentationMasks = masks,
                        sampleType = sampleType,
                        alphaOpacity = alphaOpacity,
                        isolatedCategory = isolatedCategory
                    )
                } finally {
                    masks.releaseMasks()
                }
            }

            val storageDir = File(context.filesDir, CHROMATIC_DIR_NAME).apply { mkdirs() }
            val filterTag = isolatedCategory?.name?.lowercase() ?: "all"
            val alphaTag = (alphaOpacity * 100).toInt()
            val filteredFile = File(
                storageDir,
                "${origFile.nameWithoutExtension}_ovl_${filterTag}_a${alphaTag}.jpg"
            )
            saveBitmapAsJpeg(overlayBitmap, filteredFile, quality = 92)

            Result.success(filteredFile.absolutePath)
        } catch (t: Throwable) {
            Result.failure(IllegalStateException(SystemMessages.MSG33, t))
        }
    }

    override fun observeLocalChromaticAnalyses(): Flow<List<ChromaticAnalysisResult>> =
        analisisCromaticoDao.observeAll().map { entities ->
            entities.map { it.toDomainModel() }
        }

    private fun saveBitmapAsJpeg(bitmap: Bitmap, destinationFile: File, quality: Int) {
        FileOutputStream(destinationFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(70, 100), out)
            out.flush()
        }
    }

    companion object {
        const val CHROMATIC_DIR_NAME = "chromatic_analyses"
    }
}
