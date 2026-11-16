package com.descortezadores.ecoverde

import com.descortezadores.ecoverde.data.local.entity.AnalisisCromaticoEntity
import com.descortezadores.ecoverde.data.local.entity.AnalisisEntity
import com.descortezadores.ecoverde.data.local.entity.ArbolEntity
import com.descortezadores.ecoverde.data.local.entity.FocoInfestacionEntity
import com.descortezadores.ecoverde.domain.model.BiologicalSeverityLevel
import com.descortezadores.ecoverde.domain.model.ChromaticAnalysisResult
import com.descortezadores.ecoverde.domain.model.ClassificationOutcome
import com.descortezadores.ecoverde.domain.model.ColorBandResult
import com.descortezadores.ecoverde.domain.model.DiagnosticCategory
import com.descortezadores.ecoverde.domain.model.InfestationFocus
import com.descortezadores.ecoverde.domain.model.SampleType
import com.descortezadores.ecoverde.domain.model.SyncStatus
import com.descortezadores.ecoverde.domain.model.TreeRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias de la Fase 4: Persistencia Local Offline (Room Entities - RN-50)
 * y Reglas de Integridad Relacional (`chk_arbol_o_zona`, `RN-25`..`RN-29`).
 */
class Phase4PersistenceAndSyncTest {

    @Test
    fun `AnalisisCromaticoEntity preserves HSV percentages and image paths on round-trip (RN-50)`() {
        val sampleType = SampleType.CORTEZA
        val bands = listOf(
            ColorBandResult(
                category = DiagnosticCategory.DANADO,
                pixelCount = 58500,
                ratio = 0.65,
                percentage = 65.0f,
                label = DiagnosticCategory.DANADO.biologicalSignalDescription(sampleType),
                colorHex = DiagnosticCategory.DANADO.officialHexColor(sampleType)
            ),
            ColorBandResult(
                category = DiagnosticCategory.REGULAR,
                pixelCount = 9000,
                ratio = 0.10,
                percentage = 10.0f,
                label = DiagnosticCategory.REGULAR.biologicalSignalDescription(sampleType),
                colorHex = DiagnosticCategory.REGULAR.officialHexColor(sampleType)
            ),
            ColorBandResult(
                category = DiagnosticCategory.SANO,
                pixelCount = 18000,
                ratio = 0.20,
                percentage = 20.0f,
                label = DiagnosticCategory.SANO.biologicalSignalDescription(sampleType),
                colorHex = DiagnosticCategory.SANO.officialHexColor(sampleType)
            ),
            ColorBandResult(
                category = DiagnosticCategory.INCIERTO,
                pixelCount = 4500,
                ratio = 0.05,
                percentage = 5.0f,
                label = DiagnosticCategory.INCIERTO.biologicalSignalDescription(sampleType),
                colorHex = DiagnosticCategory.INCIERTO.officialHexColor(sampleType)
            )
        )

        val originalDomain = ChromaticAnalysisResult(
            analysisId = "4a2317ab-e579-43a9-84a8-b3f40f0fcefe",
            sampleType = sampleType,
            originalImagePath = "/data/user/0/com.descortezadores.ecoverde/files/chromatic_analyses/orig_4a23.jpg",
            segmentedOverlayImagePath = "/data/user/0/com.descortezadores.ecoverde/files/chromatic_analyses/ovl_4a23.jpg",
            totalPixelsAnalyzed = 90000,
            bands = bands,
            pctSano = 0.20,
            pctRegular = 0.10,
            pctDanado = 0.65,
            pctIncierto = 0.05,
            biologicalSeverityScore = 0.7025,
            severityLevel = BiologicalSeverityLevel.ALTA,
            dominantCategory = DiagnosticCategory.DANADO,
            timestampMillis = 1760640000000L
        )

        val entity = AnalisisCromaticoEntity.fromDomainModel(
            result = originalDomain,
            parentAnalisisId = originalDomain.analysisId,
            processingTimeMs = 42L,
            syncStatus = SyncStatus.PENDIENTE
        )

        assertEquals("corteza", entity.tipoMuestra)
        assertEquals(0.65, entity.pctDanado, 0.0001)
        assertEquals(0.10, entity.pctRegular, 0.0001)
        assertEquals(0.20, entity.pctSano, 0.0001)
        assertEquals(0.05, entity.pctIncierto, 0.0001)

        val restored = entity.toDomainModel()
        assertEquals(originalDomain.analysisId, restored.analysisId)
        assertEquals(SampleType.CORTEZA, restored.sampleType)
        assertEquals(BiologicalSeverityLevel.ALTA, restored.severityLevel)
        assertEquals(originalDomain.originalImagePath, restored.originalImagePath)
        assertEquals(originalDomain.segmentedOverlayImagePath, restored.segmentedOverlayImagePath)
        assertEquals(4, restored.bands.size)
    }

    @Test
    fun `AnalisisEntity enforces mutual exclusion chk_arbol_o_zona`() {
        assertThrows(IllegalArgumentException::class.java) {
            AnalisisEntity(
                idAnalisis = "uuid-invalid",
                brigadistaId = "brig-1",
                idDispositivo = "dev-1",
                tipoImagen = "corteza",
                resultadoClasificacion = "infestado",
                confianza = 0.94,
                probabilidadCrudaTflite = 0.94,
                latitudCaptura = 19.4218,
                longitudCaptura = -99.1820,
                fechaCapturaIso = "2026-10-16T20:00:00Z",
                fechaCapturaMillis = 1760640000000L,
                idArbol = "tree-uuid-1",
                idZona = "zone-uuid-1", // Violación de chk_arbol_o_zona
                rutaEvidenciaLocal = "/files/orig.jpg",
                rutaOverlayLocal = "/files/ovl.jpg"
            )
        }
    }

    @Test
    fun `ArbolEntity and FocoInfestacionEntity map accurately between Room and Domain`() {
        val tree = TreeRecord(
            idArbol = "b9ac7a0e-523b-4783-9162-91e112600b07",
            nombreEspecifico = "Fresno Histórico #102",
            especie = "Fresno (Fraxinus uhdei)",
            latitud = 19.4215,
            longitud = -99.1824,
            fechaRegistroIso = "2026-10-16T19:13:31Z",
            descripcion = "Árbol centenario cerca del Lago Mayor",
            idCuenta = "a0000000-0000-0000-0000-000000000002",
            idDispositivo = null,
            ultimoEstadoSanitario = ClassificationOutcome.INFESTADO
        )
        val treeEntity = ArbolEntity.fromDomainModel(tree, pendienteSincronizar = false)
        val restoredTree = treeEntity.toDomainModel()
        assertEquals(tree.idArbol, restoredTree.idArbol)
        assertEquals(ClassificationOutcome.INFESTADO, restoredTree.ultimoEstadoSanitario)

        val focus = InfestationFocus(
            idFoco = "FOCO-1",
            latitudCentro = 19.421823,
            longitudCentro = -99.182003,
            radioMetros = 17.84,
            cantidadArbolesInfectados = 3,
            idsArboles = listOf("id1", "id2", "id3"),
            nombresArboles = listOf("Cedro #55", "Cedro #56", "Cedro #57")
        )
        val focusEntity = FocoInfestacionEntity.fromDomainModel(focus)
        val restoredFocus = focusEntity.toDomainModel()
        assertEquals("FOCO-1", restoredFocus.idFoco)
        assertEquals(3, restoredFocus.cantidadArbolesInfectados)
        assertTrue(restoredFocus.nombresArboles.contains("Cedro #56"))
    }
}
