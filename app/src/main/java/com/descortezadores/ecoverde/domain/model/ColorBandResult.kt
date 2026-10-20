package com.descortezadores.ecoverde.domain.model

import kotlin.math.roundToInt

/**
 * Modelo de dominio inmutable que representa el resultado estadístico y visual
 * de una banda cromática individual tras la segmentación en espacio HSV (RN-47, RN-48).
 *
 * @property category Categoría fitosanitaria ([DiagnosticCategory]).
 * @property pixelCount Conteo absoluto de píxeles clasificados en esta banda.
 * @property ratio Proporción normalizada en el rango `[0.0, 1.0]` (compatible con API `pctSano`, `pctDanado`, etc.).
 * @property percentage Porcentaje expresado en escala `[0.0, 100.0]`.
 * @property label Etiqueta descriptiva de la señal biológica.
 * @property colorHex Color hexadecimal oficial para renderizado en UI y overlay.
 * @property layerMaskImagePath Ruta opcional en almacenamiento interno de la máscara aislada para filtrado por chips.
 */
data class ColorBandResult(
    val category: DiagnosticCategory,
    val pixelCount: Int,
    val ratio: Double,
    val percentage: Float,
    val label: String,
    val colorHex: String,
    val layerMaskImagePath: String? = null
) {
    init {
        require(pixelCount >= 0) { "pixelCount no puede ser negativo: $pixelCount" }
        require(ratio in 0.0..1.0001) { "ratio fuera de rango [0, 1]: $ratio" }
    }

    val formattedPercentage: String
        get() = "${(percentage * 10f).roundToInt() / 10f}%"
}
