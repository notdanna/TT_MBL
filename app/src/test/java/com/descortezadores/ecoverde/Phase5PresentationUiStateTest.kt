package com.descortezadores.ecoverde

import com.descortezadores.ecoverde.domain.model.ClassificationOutcome
import com.descortezadores.ecoverde.domain.model.SampleType
import com.descortezadores.ecoverde.domain.model.SyncStatus
import com.descortezadores.ecoverde.domain.model.TargetAssociation
import com.descortezadores.ecoverde.domain.repository.SavedDetectionRecord
import com.descortezadores.ecoverde.presentation.features.analysis.AnalysisResultTab
import com.descortezadores.ecoverde.presentation.features.analysis.AnalysisResultUiState
import com.descortezadores.ecoverde.presentation.features.analysis.EcoVerdeMainDestination
import com.descortezadores.ecoverde.presentation.features.analysis.HistoryFilterOption
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pruebas unitarias de la Fase 5:
 * - Estado de UI (`AnalysisResultUiState`), pestañas (`[Clasificación]` / `[Análisis de color]`)
 *   y filtrado del historial local (`CU-M08`, `CU-M22`).
 */
class Phase5PresentationUiStateTest {

    @Test
    fun `filteredHistoryRecords filtra correctamente por Infestados, Sanos y Pendientes Sync`() {
        val sampleRecords = listOf(
            SavedDetectionRecord(
                idAnalisis = "rec-1",
                brigadistaId = "brig-1",
                idDispositivo = "dev-1",
                sampleType = SampleType.CORTEZA,
                classificationOutcome = ClassificationOutcome.INFESTADO,
                confidence = 0.95,
                rawTfliteProbability = 0.95,
                latitud = 19.4218,
                longitud = -99.1820,
                fechaCapturaIso = "2026-10-16T20:00:00Z",
                fechaCapturaMillis = 1760640000000L,
                observaciones = "Galerías expuestas",
                targetAssociation = TargetAssociation.CortezaTarget.NewTree(
                    nombreArbolNuevo = "Cedro #1",
                    especieArbolNuevo = "Cedro blanco (Cupressus lusitanica)"
                ),
                originalImagePath = "/tmp/1.jpg",
                overlayImagePath = "/tmp/ovl_1.jpg",
                syncStatus = SyncStatus.SINCRONIZADO,
                chromaticResult = null
            ),
            SavedDetectionRecord(
                idAnalisis = "rec-2",
                brigadistaId = "brig-1",
                idDispositivo = "dev-1",
                sampleType = SampleType.CORTEZA,
                classificationOutcome = ClassificationOutcome.SANO,
                confidence = 0.92,
                rawTfliteProbability = 0.08,
                latitud = 19.4219,
                longitud = -99.1821,
                fechaCapturaIso = "2026-10-16T20:05:00Z",
                fechaCapturaMillis = 1760640300000L,
                observaciones = "Corteza íntegra",
                targetAssociation = TargetAssociation.CortezaTarget.NewTree(
                    nombreArbolNuevo = "Fresno #2",
                    especieArbolNuevo = "Fresno (Fraxinus uhdei)"
                ),
                originalImagePath = "/tmp/2.jpg",
                overlayImagePath = "/tmp/ovl_2.jpg",
                syncStatus = SyncStatus.PENDIENTE,
                chromaticResult = null
            )
        )

        val baseState = AnalysisResultUiState(allRecords = sampleRecords)

        assertEquals(2, baseState.copy(historyFilter = HistoryFilterOption.TODOS).filteredHistoryRecords.size)
        assertEquals(1, baseState.copy(historyFilter = HistoryFilterOption.INFESTADOS).filteredHistoryRecords.size)
        assertEquals("rec-1", baseState.copy(historyFilter = HistoryFilterOption.INFESTADOS).filteredHistoryRecords.first().idAnalisis)
        assertEquals(1, baseState.copy(historyFilter = HistoryFilterOption.SANOS).filteredHistoryRecords.size)
        assertEquals("rec-2", baseState.copy(historyFilter = HistoryFilterOption.SANOS).filteredHistoryRecords.first().idAnalisis)
        assertEquals(1, baseState.copy(historyFilter = HistoryFilterOption.PENDIENTES_SYNC).filteredHistoryRecords.size)
        assertEquals("rec-2", baseState.copy(historyFilter = HistoryFilterOption.PENDIENTES_SYNC).filteredHistoryRecords.first().idAnalisis)
    }

    @Test
    fun `AnalysisResultUiState inicia en pestana Clasificacion y destino Inicio (Tus arboles)`() {
        val state = AnalysisResultUiState()
        assertEquals(EcoVerdeMainDestination.INICIO, state.currentDestination)
        assertEquals(AnalysisResultTab.CLASIFICACION, state.selectedTab)
        assertEquals(SampleType.CORTEZA, state.sampleType)
    }

    @Test
    fun `treeProgressionList agrupa capturas por arbol (ej Fresno95) y calcula porcentaje historico Sano vs Infestado`() {
        val fresno95 = com.descortezadores.ecoverde.domain.model.TreeRecord(
            idArbol = "tree-fresno-95",
            nombreEspecifico = "Fresno95",
            especie = "Fresno (Fraxinus uhdei)",
            latitud = 19.4218,
            longitud = -99.1820,
            fechaRegistroIso = "2026-10-16T18:00:00Z",
            descripcion = "Sección independiente Fresno95",
            idCuenta = "brig-1",
            idDispositivo = "dev-1",
            ultimoEstadoSanitario = ClassificationOutcome.SANO
        )

        val captures = listOf(
            SavedDetectionRecord(
                idAnalisis = "cap-1",
                brigadistaId = "brig-1",
                idDispositivo = "dev-1",
                sampleType = SampleType.CORTEZA,
                classificationOutcome = ClassificationOutcome.INFESTADO,
                confidence = 0.94,
                rawTfliteProbability = 0.94,
                latitud = 19.4218,
                longitud = -99.1820,
                fechaCapturaIso = "2026-10-16T18:10:00Z",
                fechaCapturaMillis = 1000L,
                observaciones = "Captura inicial",
                targetAssociation = TargetAssociation.CortezaTarget.ExistingTree(
                    idArbol = "tree-fresno-95",
                    nombreEspecifico = "Fresno95",
                    especie = "Fresno (Fraxinus uhdei)"
                ),
                originalImagePath = "/tmp/c1.jpg",
                overlayImagePath = "/tmp/o1.jpg",
                syncStatus = SyncStatus.SINCRONIZADO,
                chromaticResult = null
            ),
            SavedDetectionRecord(
                idAnalisis = "cap-2",
                brigadistaId = "brig-1",
                idDispositivo = "dev-1",
                sampleType = SampleType.CORTEZA,
                classificationOutcome = ClassificationOutcome.SANO,
                confidence = 0.91,
                rawTfliteProbability = 0.09,
                latitud = 19.4218,
                longitud = -99.1820,
                fechaCapturaIso = "2026-10-16T19:10:00Z",
                fechaCapturaMillis = 2000L,
                observaciones = "Segunda captura de progreso",
                targetAssociation = TargetAssociation.CortezaTarget.ExistingTree(
                    idArbol = "tree-fresno-95",
                    nombreEspecifico = "Fresno95",
                    especie = "Fresno (Fraxinus uhdei)"
                ),
                originalImagePath = "/tmp/c2.jpg",
                overlayImagePath = "/tmp/o2.jpg",
                syncStatus = SyncStatus.SINCRONIZADO,
                chromaticResult = null
            ),
            SavedDetectionRecord(
                idAnalisis = "cap-3",
                brigadistaId = "brig-1",
                idDispositivo = "dev-1",
                sampleType = SampleType.CORTEZA,
                classificationOutcome = ClassificationOutcome.SANO,
                confidence = 0.96,
                rawTfliteProbability = 0.04,
                latitud = 19.4218,
                longitud = -99.1820,
                fechaCapturaIso = "2026-10-16T20:10:00Z",
                fechaCapturaMillis = 3000L,
                observaciones = "Tercera captura de progreso",
                targetAssociation = TargetAssociation.CortezaTarget.ExistingTree(
                    idArbol = "tree-fresno-95",
                    nombreEspecifico = "Fresno95",
                    especie = "Fresno (Fraxinus uhdei)"
                ),
                originalImagePath = "/tmp/c3.jpg",
                overlayImagePath = "/tmp/o3.jpg",
                syncStatus = SyncStatus.PENDIENTE,
                chromaticResult = null
            )
        )

        val state = AnalysisResultUiState(
            availableTrees = listOf(fresno95),
            allRecords = captures,
            selectedTreeDetailId = "tree-fresno-95"
        )

        val prog = state.selectedTreeProgression
        org.junit.Assert.assertNotNull(prog)
        assertEquals("Fresno95", prog!!.tree.nombreEspecifico)
        assertEquals(3, prog.totalCaptures)
        assertEquals(2, prog.healthyCount)
        assertEquals(1, prog.infestedCount)
        assertEquals(67, prog.healthyHistoryPercentage)
        assertEquals(33, prog.infestedHistoryPercentage)
        assertEquals(ClassificationOutcome.SANO, prog.currentStatus)
    }
}
