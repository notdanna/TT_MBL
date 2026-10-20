package com.descortezadores.ecoverde

import com.descortezadores.ecoverde.data.segmentation.ColorThresholdsConfig
import com.descortezadores.ecoverde.data.segmentation.OpenCvColorSegmenter
import com.descortezadores.ecoverde.domain.model.DiagnosticCategory
import com.descortezadores.ecoverde.domain.model.SampleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Pruebas unitarias de la Fase 3 (Motor de Visión Computacional y Umbrales HSV):
 * - Calibración oficial de umbrales HSV para Corteza (RN-48) y Follaje (RN-47)
 * - Bifurcación de tonos rojos en follaje necrótico (H < 20 y H > 165)
 * - Bifurcación de tonos blanco/crema y ambarino en resina de corteza
 * - Normalización porcentual de píxeles con margen de error < 0.5% (RN-51)
 */
class Phase3VisionEngineTest {

    @Test
    fun `RN-48 ColorThresholdsConfig clasifica correctamente rangos HSV de Corteza`() {
        // Exudados café-anaranjados / galerías -> DANADO (H in 8..26, S in 60..255, V in 40..180)
        assertEquals(
            DiagnosticCategory.DANADO,
            ColorThresholdsConfig.classifyPixelHsv(h = 16, s = 180, v = 130, sampleType = SampleType.CORTEZA)
        )

        // Grumos frescos de resina blanca (S <= 45, V >= 180) -> REGULAR
        assertEquals(
            DiagnosticCategory.REGULAR,
            ColorThresholdsConfig.classifyPixelHsv(h = 10, s = 25, v = 220, sampleType = SampleType.CORTEZA)
        )

        // Grumos frescos de resina crema/ambarina (H in 25..42, S <= 65, V >= 170) -> REGULAR
        assertEquals(
            DiagnosticCategory.REGULAR,
            ColorThresholdsConfig.classifyPixelHsv(h = 32, s = 55, v = 200, sampleType = SampleType.CORTEZA)
        )

        // Corteza natural grisácea/mate (S <= 55, V in 50..165) -> SANO
        assertEquals(
            DiagnosticCategory.SANO,
            ColorThresholdsConfig.classifyPixelHsv(h = 90, s = 30, v = 110, sampleType = SampleType.CORTEZA)
        )

        // Sombra profunda (V < 40) -> INCIERTO
        assertEquals(
            DiagnosticCategory.INCIERTO,
            ColorThresholdsConfig.classifyPixelHsv(h = 0, s = 10, v = 20, sampleType = SampleType.CORTEZA)
        )
    }

    @Test
    fun `RN-47 ColorThresholdsConfig clasifica correctamente rangos HSV de Follaje incluyendo bifurcacion roja`() {
        // Tejido necrótico rojo bajo (H < 20, S >= 50, V >= 40) -> DANADO
        assertEquals(
            DiagnosticCategory.DANADO,
            ColorThresholdsConfig.classifyPixelHsv(h = 10, s = 180, v = 140, sampleType = SampleType.FOLLAJE)
        )

        // Tejido necrótico rojo alto (H > 165, S >= 50, V >= 40) -> DANADO
        assertEquals(
            DiagnosticCategory.DANADO,
            ColorThresholdsConfig.classifyPixelHsv(h = 172, s = 170, v = 135, sampleType = SampleType.FOLLAJE)
        )

        // Clorosis verde-amarillento limón (H in 20..36) -> REGULAR
        assertEquals(
            DiagnosticCategory.REGULAR,
            ColorThresholdsConfig.classifyPixelHsv(h = 28, s = 160, v = 190, sampleType = SampleType.FOLLAJE)
        )

        // Follaje vigoroso verde vivo (H in 37..85) -> SANO
        assertEquals(
            DiagnosticCategory.SANO,
            ColorThresholdsConfig.classifyPixelHsv(h = 60, s = 175, v = 165, sampleType = SampleType.FOLLAJE)
        )

        // Cielo azul abierto (H = 105) -> INCIERTO
        assertEquals(
            DiagnosticCategory.INCIERTO,
            ColorThresholdsConfig.classifyPixelHsv(h = 105, s = 80, v = 230, sampleType = SampleType.FOLLAJE)
        )
    }

    @Test
    fun `RN-51 normalizeRatios calcula porcentajes con margen de error menor a 0_5 porciento`() {
        val totalPixels = 90_000 // 300 x 300
        val counts = mapOf(
            DiagnosticCategory.DANADO to 58_500,   // 65.0%
            DiagnosticCategory.SANO to 18_000,     // 20.0%
            DiagnosticCategory.REGULAR to 9_000,   // 10.0%
            DiagnosticCategory.INCIERTO to 4_500   // 5.0%
        )

        val ratios = OpenCvColorSegmenter.normalizeRatios(counts, totalPixels)
        val sum = ratios.values.sum()

        assertTrue(abs((ratios[DiagnosticCategory.DANADO] ?: 0.0) - 0.65) < 0.005)
        assertTrue(abs((ratios[DiagnosticCategory.SANO] ?: 0.0) - 0.20) < 0.005)
        assertTrue(abs((ratios[DiagnosticCategory.REGULAR] ?: 0.0) - 0.10) < 0.005)
        assertTrue(abs((ratios[DiagnosticCategory.INCIERTO] ?: 0.0) - 0.05) < 0.005)
        assertEquals(1.0, sum, 0.0001)
    }
}
