package com.descortezadores.ecoverde.domain.usecase

import com.descortezadores.ecoverde.domain.model.BiologicalSeverityLevel
import com.descortezadores.ecoverde.domain.model.ChromaticAnalysisResult
import com.descortezadores.ecoverde.domain.model.ClassificationOutcome
import com.descortezadores.ecoverde.domain.model.ColorBandResult
import com.descortezadores.ecoverde.domain.model.DiagnosticCategory
import com.descortezadores.ecoverde.domain.model.SampleType
import com.descortezadores.ecoverde.domain.model.TargetAssociation
import com.descortezadores.ecoverde.domain.repository.ImagePreconditionMetadata
import com.descortezadores.ecoverde.domain.repository.ImageSegmentationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias de la Fase 2 (Pure Kotlin Core):
 * - Modelos inmutables: SampleType, DiagnosticCategory, ColorBandResult, ChromaticAnalysisResult
 * - Validación de precondiciones de imagen (RN-07 formato, RN-08 tamaño <= 10MB, RN-09 resolución >= 300x300 px)
 * - Orquestación en AnalyzeImageColorsUseCase y cálculo de severidad biológica
 * - Clasificación TFLite con umbral de confianza del 75% (RN-23, RN-24)
 * - Regla de exclusión mutua chk_arbol_o_zona (TargetAssociation)
 */
class Phase2DomainUseCaseTest {

    private class FakeSegmentationRepository(
        private val metadataToReturn: ImagePreconditionMetadata,
        private val pctSano: Double = 0.20,
        private val pctRegular: Double = 0.10,
        private val pctDanado: Double = 0.65,
        private val pctIncierto: Double = 0.05
    ) : ImageSegmentationRepository {

        override suspend fun inspectImagePreconditions(imagePath: String): Result<ImagePreconditionMetadata> =
            Result.success(metadataToReturn)

        override suspend fun segmentAndPersistImageColors(
            imagePath: String,
            sampleType: SampleType,
            analysisId: String?,
            alphaOpacity: Float,
            isolatedCategory: DiagnosticCategory?
        ): Result<ChromaticAnalysisResult> {
            val total = 90000 // 300x300
            val bands = listOf(
                ColorBandResult(
                    category = DiagnosticCategory.SANO,
                    pixelCount = (total * pctSano).toInt(),
                    ratio = pctSano,
                    percentage = (pctSano * 100.0).toFloat(),
                    label = DiagnosticCategory.SANO.biologicalSignalDescription(sampleType),
                    colorHex = DiagnosticCategory.SANO.officialHexColor(sampleType)
                ),
                ColorBandResult(
                    category = DiagnosticCategory.REGULAR,
                    pixelCount = (total * pctRegular).toInt(),
                    ratio = pctRegular,
                    percentage = (pctRegular * 100.0).toFloat(),
                    label = DiagnosticCategory.REGULAR.biologicalSignalDescription(sampleType),
                    colorHex = DiagnosticCategory.REGULAR.officialHexColor(sampleType)
                ),
                ColorBandResult(
                    category = DiagnosticCategory.DANADO,
                    pixelCount = (total * pctDanado).toInt(),
                    ratio = pctDanado,
                    percentage = (pctDanado * 100.0).toFloat(),
                    label = DiagnosticCategory.DANADO.biologicalSignalDescription(sampleType),
                    colorHex = DiagnosticCategory.DANADO.officialHexColor(sampleType)
                ),
                ColorBandResult(
                    category = DiagnosticCategory.INCIERTO,
                    pixelCount = (total * pctIncierto).toInt(),
                    ratio = pctIncierto,
                    percentage = (pctIncierto * 100.0).toFloat(),
                    label = DiagnosticCategory.INCIERTO.biologicalSignalDescription(sampleType),
                    colorHex = DiagnosticCategory.INCIERTO.officialHexColor(sampleType)
                )
            )
            return Result.success(
                ChromaticAnalysisResult(
                    analysisId = analysisId ?: "test-id",
                    sampleType = sampleType,
                    originalImagePath = imagePath,
                    segmentedOverlayImagePath = "${imagePath}_overlay.png",
                    totalPixelsAnalyzed = total,
                    bands = bands,
                    pctSano = pctSano,
                    pctRegular = pctRegular,
                    pctDanado = pctDanado,
                    pctIncierto = pctIncierto,
                    biologicalSeverityScore = 0.0,
                    severityLevel = BiologicalSeverityLevel.BAJA,
                    dominantCategory = DiagnosticCategory.SANO,
                    timestampMillis = 1760640000000L
                )
            )
        }

        override suspend fun renderFilteredOverlay(
            originalImagePath: String,
            sampleType: SampleType,
            alphaOpacity: Float,
            isolatedCategory: DiagnosticCategory?
        ): Result<String> = Result.success("${originalImagePath}_filtered.png")

        override fun observeLocalChromaticAnalyses(): Flow<List<ChromaticAnalysisResult>> = emptyFlow()
    }

    @Test
    fun `RN-07 RN-08 RN-09 AnalyzeImageColorsUseCase rechaza formato invalido, tamano mayor a 10MB o resolucion menor a 300x300`() = runTest {
        // 1. Formato no permitido (.gif) -> RN-07
        val badFormatRepo = FakeSegmentationRepository(
            ImagePreconditionMetadata("/tmp/tree.gif", "gif", 500_000L, 400, 400)
        )
        val resFormat = AnalyzeImageColorsUseCase(badFormatRepo)("/tmp/tree.gif", SampleType.CORTEZA)
        assertTrue(resFormat.isFailure)
        assertTrue(resFormat.exceptionOrNull()?.message?.contains("RN-07") == true)

        // 2. Tamaño > 10 MB -> RN-08
        val tooLargeRepo = FakeSegmentationRepository(
            ImagePreconditionMetadata("/tmp/tree.jpg", "jpg", 12L * 1024L * 1024L, 1000, 1000)
        )
        val resSize = AnalyzeImageColorsUseCase(tooLargeRepo)("/tmp/tree.jpg", SampleType.CORTEZA)
        assertTrue(resSize.isFailure)
        assertTrue(resSize.exceptionOrNull()?.message?.contains("RN-08") == true)

        // 3. Resolución < 300x300 px -> RN-09
        val lowResRepo = FakeSegmentationRepository(
            ImagePreconditionMetadata("/tmp/tree.jpg", "jpg", 200_000L, 250, 299)
        )
        val resLowRes = AnalyzeImageColorsUseCase(lowResRepo)("/tmp/tree.jpg", SampleType.CORTEZA)
        assertTrue(resLowRes.isFailure)
        assertTrue(resLowRes.exceptionOrNull()?.message?.contains("RN-09") == true)
    }

    @Test
    fun `RN-47 RN-48 AnalyzeImageColorsUseCase calcula severidad biologica ALTA y categoria dominante DANADO`() = runTest {
        val validRepo = FakeSegmentationRepository(
            metadataToReturn = ImagePreconditionMetadata("/tmp/corteza.jpg", "jpg", 1_200_000L, 600, 600),
            pctSano = 0.20,
            pctRegular = 0.10,
            pctDanado = 0.65,
            pctIncierto = 0.05
        )
        val useCase = AnalyzeImageColorsUseCase(validRepo)
        val result = useCase("/tmp/corteza.jpg", SampleType.CORTEZA)

        assertTrue(result.isSuccess)
        val analysis = result.getOrThrow()
        assertEquals(SampleType.CORTEZA, analysis.sampleType)
        assertEquals(DiagnosticCategory.DANADO, analysis.dominantCategory)
        assertEquals(BiologicalSeverityLevel.ALTA, analysis.severityLevel)
        assertEquals("#8B4513", analysis.bandFor(DiagnosticCategory.DANADO)?.colorHex)
    }

    @Test
    fun `RN-23 y RN-24 ClassificationOutcome soporta paridad con evaluar_test y modo estricto RN-24`() {
        // Probabilidad de infestación = 0.96 -> Confianza 96% >= 75% -> INFESTADO
        val (outcome1, conf1) = ClassificationOutcome.evaluateFromProbability(0.96)
        assertEquals(ClassificationOutcome.INFESTADO, outcome1)
        assertEquals(0.96, conf1, 0.0001)

        // Probabilidad de infestación = 0.10 -> Clase Sano con confianza 90% >= 75% -> SANO
        val (outcome2, conf2) = ClassificationOutcome.evaluateFromProbability(0.10)
        assertEquals(ClassificationOutcome.SANO, outcome2)
        assertEquals(0.90, conf2, 0.0001)

        // Paridad con evaluar_test.py (umbral 0.50): 0.62 >= 0.50 -> INFESTADO
        val (outcomeParity, _) = ClassificationOutcome.evaluateFromProbability(0.62, enforceStrictRn24 = false)
        assertEquals(ClassificationOutcome.INFESTADO, outcomeParity)

        // Modo estricto RN-24: 0.62 < 0.75 -> NO_DETERMINADO
        val (outcomeStrict, conf3) = ClassificationOutcome.evaluateFromProbability(0.62, enforceStrictRn24 = true)
        assertEquals(ClassificationOutcome.NO_DETERMINADO, outcomeStrict)
        assertEquals(0.62, conf3, 0.0001)
    }

    @Test
    fun `TargetAssociation garantiza exclusion mutua chk_arbol_o_zona entre Corteza y Follaje`() {
        val cortezaTarget: TargetAssociation = TargetAssociation.CortezaTarget.NewTree(
            nombreArbolNuevo = "Cedro Poniente #12",
            especieArbolNuevo = "Cedro blanco (Cupressus lusitanica)"
        )
        val follajeTarget: TargetAssociation = TargetAssociation.FollajeTarget.NewZone(
            nombreZonaNuevo = "Zona Aérea Ahuehuetes Lago Mayor",
            radioMetrosZona = 60.0
        )

        assertEquals(SampleType.CORTEZA, cortezaTarget.sampleType)
        assertEquals(SampleType.FOLLAJE, follajeTarget.sampleType)
    }
}
