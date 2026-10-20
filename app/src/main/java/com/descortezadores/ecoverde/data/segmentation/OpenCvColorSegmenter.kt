package com.descortezadores.ecoverde.data.segmentation

import android.graphics.Bitmap
import com.descortezadores.ecoverde.core.opencv.OpenCvInitializer
import com.descortezadores.ecoverde.di.DefaultDispatcher
import com.descortezadores.ecoverde.domain.model.DiagnosticCategory
import com.descortezadores.ecoverde.domain.model.SampleType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Resultado interno de la segmentación OpenCV que transporta los conteos de píxeles
 * y las máscaras binarias de 8 bits (1 canal) para ser procesadas por [ImageOverlayGenerator].
 *
 * IMPORTANTE: El llamador debe invocar [releaseMasks] dentro de un bloque `finally` para liberar
 * la memoria nativa C++/JNI de cada [Mat].
 */
data class RawSegmentationMasks(
    val rgbSourceMat: Mat,
    val masksByCategory: Map<DiagnosticCategory, Mat>,
    val pixelCountsByCategory: Map<DiagnosticCategory, Int>,
    val ratiosByCategory: Map<DiagnosticCategory, Double>,
    val totalPixels: Int
) {
    fun releaseMasks() {
        rgbSourceMat.release()
        masksByCategory.values.forEach { it.release() }
    }
}

/**
 * Segmentador Digital con OpenCV Android (`OpenCvColorSegmenter` - Fase 3 del Plan de Implementación).
 *
 * Implementa:
 * 1. Conversión de espacio de color `Imgproc.COLOR_RGB2HSV`.
 * 2. Segmentación por máscaras mediante `Core.inRange`.
 * 3. Combinación de máscaras bifurcadas (follaje necrótico rojo RN-47 y resina blanca/ambarina RN-48) con `Core.bitwise_or`.
 * 4. Filtrado morfológico (`MORPH_OPEN` + `MORPH_CLOSE`) con kernel elíptico 3×3 para eliminar ruido impulsivo.
 * 5. Garantía de exclusión mutua entre categorías y liberación estricta de todas las matrices intermedias `Mat`.
 */
@Singleton
class OpenCvColorSegmenter @Inject constructor(
    private val openCvInitializer: OpenCvInitializer,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    suspend fun segmentBitmapToMasks(
        sourceBitmap: Bitmap,
        sampleType: SampleType
    ): RawSegmentationMasks = withContext(defaultDispatcher) {
        check(openCvInitializer.ensureInitialized()) {
            "El motor nativo de OpenCV no pudo inicializarse."
        }

        val argbBitmap = if (sourceBitmap.config == Bitmap.Config.ARGB_8888) {
            sourceBitmap
        } else {
            sourceBitmap.copy(Bitmap.Config.ARGB_8888, false)
        }

        val rgbaMat = Mat()
        val rgbMat = Mat()
        val hsvMat = Mat()
        val morphKernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(3.0, 3.0))

        // Máscaras crudas e intermedias que deben liberarse siempre en finally
        val rawDanado = Mat()
        val rawRegular = Mat()
        val rawSano = Mat()
        val tempSecondaryMask = Mat()
        val tempMorph = Mat()
        val occupiedMask = Mat()

        try {
            // 1. Bitmap -> RGBA Mat -> RGB Mat -> HSV Mat
            Utils.bitmapToMat(argbBitmap, rgbaMat)
            Imgproc.cvtColor(rgbaMat, rgbMat, Imgproc.COLOR_RGBA2RGB)
            Imgproc.cvtColor(rgbMat, hsvMat, Imgproc.COLOR_RGB2HSV)

            // 2. Umbralización por rangos HSV según el tipo de muestra (RN-47 vs RN-48, RN-49)
            when (sampleType) {
                SampleType.CORTEZA -> {
                    // Dañado: Café-anaranjado / galerías
                    Core.inRange(
                        hsvMat,
                        ColorThresholdsConfig.CORTEZA_DANADO_RANGE.lowerScalar(),
                        ColorThresholdsConfig.CORTEZA_DANADO_RANGE.upperScalar(),
                        rawDanado
                    )

                    // Regular: Bifurcación Resina Blanca/Crema OR Resina Ambarina
                    Core.inRange(
                        hsvMat,
                        ColorThresholdsConfig.CORTEZA_REGULAR_WHITE_RESIN_RANGE.lowerScalar(),
                        ColorThresholdsConfig.CORTEZA_REGULAR_WHITE_RESIN_RANGE.upperScalar(),
                        rawRegular
                    )
                    Core.inRange(
                        hsvMat,
                        ColorThresholdsConfig.CORTEZA_REGULAR_CREAM_RESIN_RANGE.lowerScalar(),
                        ColorThresholdsConfig.CORTEZA_REGULAR_CREAM_RESIN_RANGE.upperScalar(),
                        tempSecondaryMask
                    )
                    Core.bitwise_or(rawRegular, tempSecondaryMask, rawRegular)

                    // Sano: Corteza natural grisácea/mate
                    Core.inRange(
                        hsvMat,
                        ColorThresholdsConfig.CORTEZA_SANO_RANGE.lowerScalar(),
                        ColorThresholdsConfig.CORTEZA_SANO_RANGE.upperScalar(),
                        rawSano
                    )
                }

                SampleType.FOLLAJE -> {
                    // Dañado: Bifurcación Tonos Rojos Bajos (H < 20) OR Rojos Altos (H > 165)
                    Core.inRange(
                        hsvMat,
                        ColorThresholdsConfig.FOLLAJE_DANADO_LOWER_RED_RANGE.lowerScalar(),
                        ColorThresholdsConfig.FOLLAJE_DANADO_LOWER_RED_RANGE.upperScalar(),
                        rawDanado
                    )
                    Core.inRange(
                        hsvMat,
                        ColorThresholdsConfig.FOLLAJE_DANADO_UPPER_RED_RANGE.lowerScalar(),
                        ColorThresholdsConfig.FOLLAJE_DANADO_UPPER_RED_RANGE.upperScalar(),
                        tempSecondaryMask
                    )
                    Core.bitwise_or(rawDanado, tempSecondaryMask, rawDanado)

                    // Regular: Clorosis verde-amarillento limón
                    Core.inRange(
                        hsvMat,
                        ColorThresholdsConfig.FOLLAJE_REGULAR_RANGE.lowerScalar(),
                        ColorThresholdsConfig.FOLLAJE_REGULAR_RANGE.upperScalar(),
                        rawRegular
                    )

                    // Sano: Follaje verde vivo
                    Core.inRange(
                        hsvMat,
                        ColorThresholdsConfig.FOLLAJE_SANO_RANGE.lowerScalar(),
                        ColorThresholdsConfig.FOLLAJE_SANO_RANGE.upperScalar(),
                        rawSano
                    )
                }
            }

            // 3. Filtrado Morfológico: MORPH_OPEN + MORPH_CLOSE con Kernel Elíptico 3x3
            val cleanDanado = Mat()
            val cleanRegular = Mat()
            val cleanSano = Mat()
            val cleanIncierto = Mat()

            applyMorphologicalFilter(rawDanado, cleanDanado, tempMorph, morphKernel)
            applyMorphologicalFilter(rawRegular, cleanRegular, tempMorph, morphKernel)
            applyMorphologicalFilter(rawSano, cleanSano, tempMorph, morphKernel)

            // 4. Garantizar exclusión mutua a nivel de píxel:
            // Prioridad fitosanitaria: DANADO -> REGULAR -> SANO -> INCIERTO
            val notDanado = Mat()
            val notOccupied = Mat()
            try {
                Core.bitwise_not(cleanDanado, notDanado)
                Core.bitwise_and(cleanRegular, notDanado, cleanRegular)

                Core.bitwise_or(cleanDanado, cleanRegular, occupiedMask)
                Core.bitwise_not(occupiedMask, notOccupied)
                Core.bitwise_and(cleanSano, notOccupied, cleanSano)

                Core.bitwise_or(occupiedMask, cleanSano, occupiedMask)
                Core.bitwise_not(occupiedMask, cleanIncierto)
            } finally {
                notDanado.release()
                notOccupied.release()
            }

            // 5. Cálculo Estadístico de Píxeles y Proporciones (RN-51)
            val totalPixels = (rgbMat.rows() * rgbMat.cols()).coerceAtLeast(1)
            val countDanado = Core.countNonZero(cleanDanado)
            val countRegular = Core.countNonZero(cleanRegular)
            val countSano = Core.countNonZero(cleanSano)
            val countIncierto = (totalPixels - countDanado - countRegular - countSano).coerceAtLeast(0)

            val countsMap = mapOf(
                DiagnosticCategory.SANO to countSano,
                DiagnosticCategory.REGULAR to countRegular,
                DiagnosticCategory.DANADO to countDanado,
                DiagnosticCategory.INCIERTO to countIncierto
            )

            val ratiosMap = normalizeRatios(countsMap, totalPixels)

            val masksMap = mapOf(
                DiagnosticCategory.SANO to cleanSano,
                DiagnosticCategory.REGULAR to cleanRegular,
                DiagnosticCategory.DANADO to cleanDanado,
                DiagnosticCategory.INCIERTO to cleanIncierto
            )

            RawSegmentationMasks(
                rgbSourceMat = rgbMat.clone(),
                masksByCategory = masksMap,
                pixelCountsByCategory = countsMap,
                ratiosByCategory = ratiosMap,
                totalPixels = totalPixels
            )
        } finally {
            rgbaMat.release()
            rgbMat.release()
            hsvMat.release()
            morphKernel.release()
            rawDanado.release()
            rawRegular.release()
            rawSano.release()
            tempSecondaryMask.release()
            tempMorph.release()
            occupiedMask.release()
        }
    }

    private fun applyMorphologicalFilter(
        sourceMask: Mat,
        destMask: Mat,
        intermediateMat: Mat,
        kernel: Mat
    ) {
        Imgproc.morphologyEx(sourceMask, intermediateMat, Imgproc.MORPH_OPEN, kernel)
        Imgproc.morphologyEx(intermediateMat, destMask, Imgproc.MORPH_CLOSE, kernel)
    }

    companion object {
        /**
         * Convierte los conteos de píxeles en proporciones redondeadas a 4 decimales que suman 1.0.
         */
        fun normalizeRatios(
            counts: Map<DiagnosticCategory, Int>,
            totalPixels: Int
        ): Map<DiagnosticCategory, Double> {
            val safeTotal = totalPixels.coerceAtLeast(1).toDouble()
            val rawSano = ((counts[DiagnosticCategory.SANO] ?: 0) / safeTotal * 10000.0).roundToInt() / 10000.0
            val rawRegular = ((counts[DiagnosticCategory.REGULAR] ?: 0) / safeTotal * 10000.0).roundToInt() / 10000.0
            val rawDanado = ((counts[DiagnosticCategory.DANADO] ?: 0) / safeTotal * 10000.0).roundToInt() / 10000.0
            val rawIncierto = ((1.0 - (rawSano + rawRegular + rawDanado)).coerceIn(0.0, 1.0) * 10000.0).roundToInt() / 10000.0

            return mapOf(
                DiagnosticCategory.SANO to rawSano,
                DiagnosticCategory.REGULAR to rawRegular,
                DiagnosticCategory.DANADO to rawDanado,
                DiagnosticCategory.INCIERTO to rawIncierto
            )
        }
    }
}
