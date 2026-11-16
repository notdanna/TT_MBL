package com.descortezadores.ecoverde.data.segmentation

import android.graphics.Bitmap
import com.descortezadores.ecoverde.di.DefaultDispatcher
import com.descortezadores.ecoverde.domain.model.DiagnosticCategory
import com.descortezadores.ecoverde.domain.model.SampleType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.imgproc.Imgproc
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generador de Superposición Visual (`ImageOverlayGenerator` - Fase 3 del Plan de Implementación).
 *
 * Responsabilidades:
 * 1. Construir el mapa cromático coloreando cada región según la Tabla Oficial (§5 del Plan).
 * 2. Aplicar mezclado alfa (`Core.addWeighted`) con opacidad inicial de `0.50` (o configurable)
 *    sobre la fotografía original.
 * 3. Detectar bordes mediante `Imgproc.findContours` y delinear cada región con `Imgproc.drawContours`
 *    para máxima claridad visual en campo.
 * 4. Permitir aislar una categoría específica (`isolatedCategory`) para los `ColorCategoryFilterChips`.
 * 5. Liberar formalmente todas las instancias de [Mat] y [MatOfPoint] en bloques `finally`.
 */
@Singleton
class ImageOverlayGenerator @Inject constructor(
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    suspend fun generateOverlayBitmap(
        segmentationMasks: RawSegmentationMasks,
        sampleType: SampleType,
        alphaOpacity: Float = DEFAULT_ALPHA,
        isolatedCategory: DiagnosticCategory? = null
    ): Bitmap = withContext(defaultDispatcher) {
        val alpha = alphaOpacity.coerceIn(0.0f, 1.0f).toDouble()
        val beta = 1.0 - alpha

        val sourceRgb = segmentationMasks.rgbSourceMat
        val colorMapMat = sourceRgb.clone()
        val blendedRgbMat = Mat()
        val activeRegionMask = Mat.zeros(sourceRgb.size(), org.opencv.core.CvType.CV_8UC1)
        val compositeRgbMat = sourceRgb.clone()
        val outputRgbaMat = Mat()

        val categoriesToRender = if (isolatedCategory != null) {
            listOf(isolatedCategory)
        } else {
            // En vista general coloreamos Sano, Regular y Dañado (y opcionalmente Incierto con tinte suave)
            listOf(
                DiagnosticCategory.SANO,
                DiagnosticCategory.REGULAR,
                DiagnosticCategory.DANADO,
                DiagnosticCategory.INCIERTO
            )
        }

        try {
            // 1. Pintar cada máscara activa con su color oficial en colorMapMat
            for (category in categoriesToRender) {
                val categoryMask = segmentationMasks.masksByCategory[category] ?: continue
                val rgbColor = ColorThresholdsConfig.overlayRgbFor(sampleType, category)
                colorMapMat.setTo(rgbColor.toScalar(), categoryMask)
                Core.bitwise_or(activeRegionMask, categoryMask, activeRegionMask)
            }

            // 2. Mezclado Alfa (Core.addWeighted) entre el mapa de color y la foto original
            Core.addWeighted(
                colorMapMat,
                alpha,
                sourceRgb,
                beta,
                0.0,
                blendedRgbMat
            )

            // Si se filtró una categoría individual, solo aplicar el blend en la máscara de esa categoría
            blendedRgbMat.copyTo(compositeRgbMat, activeRegionMask)

            // 3. Detección y trazado de contornos nítidos con Imgproc.findContours + drawContours
            val contourCategories = if (isolatedCategory != null) {
                listOf(isolatedCategory)
            } else {
                listOf(
                    DiagnosticCategory.DANADO,
                    DiagnosticCategory.REGULAR,
                    DiagnosticCategory.SANO
                )
            }

            for (category in contourCategories) {
                val mask = segmentationMasks.masksByCategory[category] ?: continue
                drawCategoryContours(
                    targetRgbMat = compositeRgbMat,
                    categoryMask = mask,
                    sampleType = sampleType,
                    category = category
                )
            }

            // 4. Convertir RGB Mat resultante a Bitmap ARGB_8888
            Imgproc.cvtColor(compositeRgbMat, outputRgbaMat, Imgproc.COLOR_RGB2RGBA)
            val resultBitmap = Bitmap.createBitmap(
                outputRgbaMat.cols(),
                outputRgbaMat.rows(),
                Bitmap.Config.ARGB_8888
            )
            Utils.matToBitmap(outputRgbaMat, resultBitmap)
            resultBitmap
        } finally {
            colorMapMat.release()
            blendedRgbMat.release()
            activeRegionMask.release()
            compositeRgbMat.release()
            outputRgbaMat.release()
        }
    }

    private fun drawCategoryContours(
        targetRgbMat: Mat,
        categoryMask: Mat,
        sampleType: SampleType,
        category: DiagnosticCategory
    ) {
        val maskCopy = categoryMask.clone()
        val hierarchy = Mat()
        val contours = ArrayList<MatOfPoint>()
        try {
            Imgproc.findContours(
                maskCopy,
                contours,
                hierarchy,
                Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_SIMPLE
            )
            val contourColor = ColorThresholdsConfig.overlayRgbFor(sampleType, category).toContourScalar()
            for (i in contours.indices) {
                val area = Imgproc.contourArea(contours[i])
                if (area >= MIN_CONTOUR_AREA_PX) {
                    Imgproc.drawContours(
                        targetRgbMat,
                        contours,
                        i,
                        contourColor,
                        CONTOUR_THICKNESS_PX
                    )
                }
            }
        } finally {
            maskCopy.release()
            hierarchy.release()
            contours.forEach { it.release() }
        }
    }

    companion object {
        const val DEFAULT_ALPHA = 0.50f
        private const val MIN_CONTOUR_AREA_PX = 18.0
        private const val CONTOUR_THICKNESS_PX = 2
    }
}
