package com.descortezadores.ecoverde.domain.model

/**
 * Nivel de severidad biológica global calculado a partir de la distribución cromática HSV.
 */
enum class BiologicalSeverityLevel(
    val title: String,
    val recommendation: String
) {
    BAJA(
        title = "Severidad Baja (Predominio Sano)",
        recommendation = "Mantener monitoreo preventivo ordinario conforme a NOM-019-SEMARNAT-2017."
    ),
    MODERADA(
        title = "Severidad Moderada (Alerta Temprana / Defensa Activa)",
        recommendation = "Inspeccionar fuste en busca de grumos frescos de resina o inicio de clorosis. Programar seguimiento prioritario."
    ),
    ALTA(
        title = "Severidad Alta (Colonización Avanzada / Daño Severo)",
        recommendation = "Atención fitosanitaria inmediata. Evaluar saneamiento forestal y verificar árboles en un radio de 100 m (RN-25 / RN-27)."
    ),
    NO_CONCLUYENTE(
        title = "Muestra No Concluyente (Predominio Incierto)",
        recommendation = "Se recomienda repetir la captura mejorando la iluminación y encuadre sobre la corteza o copa (RN-24)."
    )
}

/**
 * Resultado integral e inmutable del análisis cromático HSV con OpenCV (RN-46 a RN-50).
 */
data class ChromaticAnalysisResult(
    val analysisId: String,
    val sampleType: SampleType,
    val originalImagePath: String,
    val segmentedOverlayImagePath: String,
    val totalPixelsAnalyzed: Int,
    val bands: List<ColorBandResult>,
    val pctSano: Double,
    val pctRegular: Double,
    val pctDanado: Double,
    val pctIncierto: Double,
    val biologicalSeverityScore: Double,
    val severityLevel: BiologicalSeverityLevel,
    val dominantCategory: DiagnosticCategory,
    val timestampMillis: Long,
    val brigadistaId: String? = null
) {
    /**
     * Obtiene el resultado de una categoría diagnóstica específica.
     */
    fun bandFor(category: DiagnosticCategory): ColorBandResult? =
        bands.firstOrNull { it.category == category }
}
