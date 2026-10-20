package com.descortezadores.ecoverde.domain.model

/**
 * Categoría diagnóstica de las franjas cromáticas en espacio HSV (RN-47, RN-48).
 */
enum class DiagnosticCategory(
    val apiKey: String,
    val displayName: String,
    val severityWeight: Double
) {
    SANO(
        apiKey = "sano",
        displayName = "Sano",
        severityWeight = 0.0
    ),
    REGULAR(
        apiKey = "regular",
        displayName = "Regular",
        severityWeight = 0.45
    ),
    DANADO(
        apiKey = "danado",
        displayName = "Dañado",
        severityWeight = 1.0
    ),
    INCIERTO(
        apiKey = "incierto",
        displayName = "Incierto",
        severityWeight = 0.15
    );

    /**
     * Etiqueta biológica contextual según el tipo de muestra (Tabla Oficial de Calibración HSV).
     */
    fun biologicalSignalDescription(sampleType: SampleType): String = when (sampleType) {
        SampleType.CORTEZA -> when (this) {
            DANADO -> "Exudados café-anaranjados, galerías subcorticales"
            REGULAR -> "Grumos frescos de resina blanca/crema/rojiza"
            SANO -> "Corteza natural no colonizada (grisácea/mate)"
            INCIERTO -> "Sombras profundas, fondos no leñosos"
        }
        SampleType.FOLLAJE -> when (this) {
            DANADO -> "Tejido necrótico, muerte por falta de savia (café-rojizo)"
            REGULAR -> "Clorosis, deterioro activo (verde-amarillento limón)"
            SANO -> "Clorofila activa, follaje vigoroso (verde vivo)"
            INCIERTO -> "Cielo abierto, ramas muertas distantes"
        }
    }

    /**
     * Color hexadecimal oficial según la Tabla de Calibración Cromática HSV (§5 del Plan).
     */
    fun officialHexColor(sampleType: SampleType): String = when (sampleType) {
        SampleType.CORTEZA -> when (this) {
            DANADO -> "#8B4513"   // Marrón / Café-anaranjado
            REGULAR -> "#E6D7B8"  // Crema / Blanco-ambarino
            SANO -> "#2D6A3E"     // Verde bosque / Grisáceo
            INCIERTO -> "#757575" // Gris translúcido
        }
        SampleType.FOLLAJE -> when (this) {
            DANADO -> "#A52A2A"   // Café-rojizo
            REGULAR -> "#9ACD32"  // Verde-amarillento limón
            SANO -> "#2E7D32"     // Verde vivo
            INCIERTO -> "#9E9E9E" // Gris humo
        }
    }
}
