package com.descortezadores.ecoverde.domain.model

/**
 * Tipo de muestra biológica capturada en el Bosque de Chapultepec (RN-47, RN-48, RN-49).
 *
 * - [CORTEZA]: Fotografía de tronco/fuste o tejido subcortical a escala de árbol individual.
 * - [FOLLAJE]: Fotografía aérea/dron o de copa a escala de zona macro.
 */
enum class SampleType(
    val apiValue: String,
    val displayName: String,
    val description: String
) {
    CORTEZA(
        apiValue = "corteza",
        displayName = "Corteza (Árbol Individual)",
        description = "Inspección de fuste, grumos de resina, aserrín o galerías subcorticales (RN-48)."
    ),
    FOLLAJE(
        apiValue = "follaje",
        displayName = "Follaje (Zona / Copa / Dron)",
        description = "Evaluación cromática de clorosis o necrosis en dosel forestal (RN-47)."
    );

    companion object {
        fun fromApiValue(value: String): SampleType =
            entries.firstOrNull { it.apiValue.equals(value.trim(), ignoreCase = true) } ?: CORTEZA
    }
}
