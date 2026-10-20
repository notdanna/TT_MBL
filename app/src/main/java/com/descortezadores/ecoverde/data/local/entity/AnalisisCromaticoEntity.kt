package com.descortezadores.ecoverde.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.descortezadores.ecoverde.domain.model.BiologicalSeverityLevel
import com.descortezadores.ecoverde.domain.model.ChromaticAnalysisResult
import com.descortezadores.ecoverde.domain.model.ColorBandResult
import com.descortezadores.ecoverde.domain.model.DiagnosticCategory
import com.descortezadores.ecoverde.domain.model.SampleType
import com.descortezadores.ecoverde.domain.model.SyncStatus
import com.descortezadores.ecoverde.domain.usecase.AnalyzeImageColorsUseCase

/**
 * Entidad Room para la persistencia local offline del análisis cromático HSV (`RN-50`).
 *
 * Almacena las proporciones de píxeles (`pct_sano`, `pct_regular`, `pct_danado`, `pct_incierto`)
 * y las rutas en almacenamiento interno (`filesDir`) tanto de la fotografía original como
 * de la imagen coloreada por secciones generada por OpenCV (`ImageOverlayGenerator`).
 */
@Entity(
    tableName = "analisis_cromatico",
    indices = [
        Index(value = ["id_analisis_fk"]),
        Index(value = ["estado_sincronizacion"])
    ]
)
data class AnalisisCromaticoEntity(
    @PrimaryKey
    @ColumnInfo(name = "id_analisis_cromatico")
    val idAnalisisCromatico: String,

    @ColumnInfo(name = "id_analisis_fk")
    val idAnalisisFk: String,

    @ColumnInfo(name = "tipo_muestra")
    val tipoMuestra: String, // "corteza" | "follaje"

    @ColumnInfo(name = "pct_sano")
    val pctSano: Double,

    @ColumnInfo(name = "pct_regular")
    val pctRegular: Double,

    @ColumnInfo(name = "pct_danado")
    val pctDanado: Double,

    @ColumnInfo(name = "pct_incierto")
    val pctIncierto: Double,

    @ColumnInfo(name = "severidad_biologica")
    val severidadBiologica: String,

    @ColumnInfo(name = "ruta_imagen_original")
    val rutaImagenOriginal: String,

    @ColumnInfo(name = "ruta_imagen_segmentada")
    val rutaImagenSegmentada: String,

    @ColumnInfo(name = "total_pixeles_analizados")
    val totalPixelesAnalizados: Int,

    @ColumnInfo(name = "tiempo_procesamiento_ms")
    val tiempoProcesamientoMs: Long,

    @ColumnInfo(name = "fecha_analisis_millis")
    val fechaAnalisisMillis: Long,

    @ColumnInfo(name = "estado_sincronizacion")
    val estadoSincronizacion: String = SyncStatus.PENDIENTE.name
) {
    fun toDomainModel(): ChromaticAnalysisResult {
        val sampleTypeEnum = SampleType.fromApiValue(tipoMuestra)
        val severityEnum = runCatching {
            BiologicalSeverityLevel.valueOf(severidadBiologica)
        }.getOrDefault(BiologicalSeverityLevel.BAJA)

        val ratios = mapOf(
            DiagnosticCategory.DANADO to pctDanado,
            DiagnosticCategory.REGULAR to pctRegular,
            DiagnosticCategory.SANO to pctSano,
            DiagnosticCategory.INCIERTO to pctIncierto
        )

        val bands = listOf(
            DiagnosticCategory.DANADO,
            DiagnosticCategory.REGULAR,
            DiagnosticCategory.SANO,
            DiagnosticCategory.INCIERTO
        ).map { category ->
            val ratio = (ratios[category] ?: 0.0).coerceIn(0.0, 1.0)
            ColorBandResult(
                category = category,
                pixelCount = (ratio * totalPixelesAnalizados.toDouble()).toInt().coerceAtLeast(0),
                ratio = ratio,
                percentage = (ratio * 100.0).toFloat(),
                label = category.biologicalSignalDescription(sampleTypeEnum),
                colorHex = category.officialHexColor(sampleTypeEnum)
            )
        }

        val severityScore = AnalyzeImageColorsUseCase.calculateBiologicalSeverityScore(
            pctDanado = pctDanado,
            pctRegular = pctRegular,
            pctIncierto = pctIncierto
        )
        val dominant = AnalyzeImageColorsUseCase.determineDominantCategory(bands)

        return ChromaticAnalysisResult(
            analysisId = idAnalisisCromatico,
            sampleType = sampleTypeEnum,
            originalImagePath = rutaImagenOriginal,
            segmentedOverlayImagePath = rutaImagenSegmentada,
            totalPixelsAnalyzed = totalPixelesAnalizados,
            bands = bands,
            pctSano = pctSano,
            pctRegular = pctRegular,
            pctDanado = pctDanado,
            pctIncierto = pctIncierto,
            biologicalSeverityScore = severityScore,
            severityLevel = severityEnum,
            dominantCategory = dominant,
            timestampMillis = fechaAnalisisMillis
        )
    }

    companion object {
        fun fromDomainModel(
            result: ChromaticAnalysisResult,
            parentAnalisisId: String = result.analysisId,
            processingTimeMs: Long = 25L,
            syncStatus: SyncStatus = SyncStatus.PENDIENTE
        ): AnalisisCromaticoEntity = AnalisisCromaticoEntity(
            idAnalisisCromatico = result.analysisId,
            idAnalisisFk = parentAnalisisId,
            tipoMuestra = result.sampleType.apiValue,
            pctSano = result.pctSano,
            pctRegular = result.pctRegular,
            pctDanado = result.pctDanado,
            pctIncierto = result.pctIncierto,
            severidadBiologica = result.severityLevel.name,
            rutaImagenOriginal = result.originalImagePath,
            rutaImagenSegmentada = result.segmentedOverlayImagePath,
            totalPixelesAnalizados = result.totalPixelsAnalyzed,
            tiempoProcesamientoMs = processingTimeMs,
            fechaAnalisisMillis = result.timestampMillis,
            estadoSincronizacion = syncStatus.name
        )
    }
}
