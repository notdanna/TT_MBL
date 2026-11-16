package com.descortezadores.ecoverde.data.segmentation

import com.descortezadores.ecoverde.domain.model.DiagnosticCategory
import com.descortezadores.ecoverde.domain.model.SampleType
import org.opencv.core.Scalar

/**
 * Rango de umbralización HSV en formato OpenCV 8-bit:
 * - Hue (H) en `[0, 180]`
 * - Saturation (S) en `[0, 255]`
 * - Value (V) en `[0, 255]`
 */
data class HsvRange(
    val minH: Int,
    val maxH: Int,
    val minS: Int,
    val maxS: Int,
    val minV: Int,
    val maxV: Int
) {
    fun lowerScalar(): Scalar = Scalar(minH.toDouble(), minS.toDouble(), minV.toDouble())
    fun upperScalar(): Scalar = Scalar(maxH.toDouble(), maxS.toDouble(), maxV.toDouble())

    fun contains(h: Int, s: Int, v: Int): Boolean =
        h in minH..maxH && s in minS..maxS && v in minV..maxV
}

/**
 * Color RGB para renderizado de máscaras y contornos en OpenCV.
 */
data class RgbColor(val r: Int, val g: Int, val b: Int) {
    fun toScalar(): Scalar = Scalar(r.toDouble(), g.toDouble(), b.toDouble())
    fun toContourScalar(): Scalar = Scalar(
        (r * 0.75).toInt().coerceIn(0, 255).toDouble(),
        (g * 0.75).toInt().coerceIn(0, 255).toDouble(),
        (b * 0.75).toInt().coerceIn(0, 255).toDouble()
    )
}

/**
 * Configuración oficial de umbrales HSV conforme al Capítulo 4 (§4.2.4), reglas RN-47 y RN-48,
 * y la Tabla Oficial de Calibración Cromática HSV (§5 del Plan de Implementación).
 */
object ColorThresholdsConfig {

    // =========================================================================================
    // 1. CALIBRACIÓN OFICIAL PARA CORTEZA (RN-48)
    // =========================================================================================

    /**
     * Corteza Dañado: Exudados café-anaranjados, galerías subcorticales.
     * H in [8, 26], S in [60, 255], V in [40, 180]
     */
    val CORTEZA_DANADO_RANGE = HsvRange(
        minH = 8, maxH = 26,
        minS = 60, maxS = 255,
        minV = 40, maxV = 180
    )

    /**
     * Corteza Regular (Bifurcación A - Blanco/Crema de resina fresca con baja saturación y alto brillo):
     * H in [0, 180], S in [0, 45], V in [180, 255]
     */
    val CORTEZA_REGULAR_WHITE_RESIN_RANGE = HsvRange(
        minH = 0, maxH = 180,
        minS = 0, maxS = 45,
        minV = 180, maxV = 255
    )

    /**
     * Corteza Regular (Bifurcación B - Resina crema/ambarina):
     * H in [25, 42], S in [0, 65], V in [170, 255]
     */
    val CORTEZA_REGULAR_CREAM_RESIN_RANGE = HsvRange(
        minH = 25, maxH = 42,
        minS = 0, maxS = 65,
        minV = 170, maxV = 255
    )

    /**
     * Corteza Sano: Corteza natural no colonizada (grisácea/mate).
     * H in [0, 180], S in [0, 55], V in [50, 165]
     */
    val CORTEZA_SANO_RANGE = HsvRange(
        minH = 0, maxH = 180,
        minS = 0, maxS = 55,
        minV = 50, maxV = 165
    )

    // =========================================================================================
    // 2. CALIBRACIÓN OFICIAL PARA FOLLAJE (RN-47)
    // =========================================================================================

    /**
     * Follaje Dañado (Bifurcación Roja Baja - Tejido necrótico café-rojizo):
     * H in [0, 19], S in [50, 255], V in [40, 255]
     */
    val FOLLAJE_DANADO_LOWER_RED_RANGE = HsvRange(
        minH = 0, maxH = 19,
        minS = 50, maxS = 255,
        minV = 40, maxV = 255
    )

    /**
     * Follaje Dañado (Bifurcación Roja Alta - Tejido necrótico rojizo/púrpura):
     * H in [166, 180], S in [50, 255], V in [40, 255]
     */
    val FOLLAJE_DANADO_UPPER_RED_RANGE = HsvRange(
        minH = 166, maxH = 180,
        minS = 50, maxS = 255,
        minV = 40, maxV = 255
    )

    /**
     * Follaje Regular (Clorosis, deterioro activo verde-amarillento limón):
     * H in [20, 36], S in [40, 255], V in [50, 240]
     */
    val FOLLAJE_REGULAR_RANGE = HsvRange(
        minH = 20, maxH = 36,
        minS = 40, maxS = 255,
        minV = 50, maxV = 240
    )

    /**
     * Follaje Sano (Clorofila activa, follaje vigoroso verde vivo):
     * H in [37, 85], S in [45, 255], V in [40, 255]
     */
    val FOLLAJE_SANO_RANGE = HsvRange(
        minH = 37, maxH = 85,
        minS = 45, maxS = 255,
        minV = 40, maxV = 255
    )

    /**
     * Colores RGB oficiales para superposición visual (Tabla §5 del Plan).
     */
    fun overlayRgbFor(sampleType: SampleType, category: DiagnosticCategory): RgbColor =
        when (sampleType) {
            SampleType.CORTEZA -> when (category) {
                DiagnosticCategory.DANADO -> RgbColor(0x8B, 0x45, 0x13)   // #8B4513 Marrón / Café-anaranjado
                DiagnosticCategory.REGULAR -> RgbColor(0xE6, 0xD7, 0xB8)  // #E6D7B8 Crema / Blanco-ambarino
                DiagnosticCategory.SANO -> RgbColor(0x2D, 0x6A, 0x3E)     // #2D6A3E Verde bosque / Grisáceo
                DiagnosticCategory.INCIERTO -> RgbColor(0x75, 0x75, 0x75) // #757575 Gris translúcido
            }
            SampleType.FOLLAJE -> when (category) {
                DiagnosticCategory.DANADO -> RgbColor(0xA5, 0x2A, 0x2A)   // #A52A2A Café-rojizo
                DiagnosticCategory.REGULAR -> RgbColor(0x9A, 0xCD, 0x32)  // #9ACD32 Verde-amarillento limón
                DiagnosticCategory.SANO -> RgbColor(0x2E, 0x7D, 0x32)     // #2E7D32 Verde vivo
                DiagnosticCategory.INCIERTO -> RgbColor(0x9E, 0x9E, 0x9E) // #9E9E9E Gris humo
            }
        }

    /**
     * Clasifica matemáticamente un píxel individual HSV (OpenCV 8-bit: H in 0..180, S in 0..255, V in 0..255)
     * aplicando exactamente la misma jerarquía de máscaras que OpenCvColorSegmenter.
     */
    fun classifyPixelHsv(h: Int, s: Int, v: Int, sampleType: SampleType): DiagnosticCategory =
        when (sampleType) {
            SampleType.CORTEZA -> when {
                CORTEZA_DANADO_RANGE.contains(h, s, v) -> DiagnosticCategory.DANADO
                CORTEZA_REGULAR_WHITE_RESIN_RANGE.contains(h, s, v) ||
                    CORTEZA_REGULAR_CREAM_RESIN_RANGE.contains(h, s, v) -> DiagnosticCategory.REGULAR
                CORTEZA_SANO_RANGE.contains(h, s, v) -> DiagnosticCategory.SANO
                else -> DiagnosticCategory.INCIERTO
            }
            SampleType.FOLLAJE -> when {
                FOLLAJE_DANADO_LOWER_RED_RANGE.contains(h, s, v) ||
                    FOLLAJE_DANADO_UPPER_RED_RANGE.contains(h, s, v) -> DiagnosticCategory.DANADO
                FOLLAJE_REGULAR_RANGE.contains(h, s, v) -> DiagnosticCategory.REGULAR
                FOLLAJE_SANO_RANGE.contains(h, s, v) -> DiagnosticCategory.SANO
                else -> DiagnosticCategory.INCIERTO
            }
        }
}
