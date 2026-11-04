package com.descortezadores.ecoverde.presentation.features.analysis

import android.graphics.Bitmap
import com.descortezadores.ecoverde.domain.model.ChromaticAnalysisResult
import com.descortezadores.ecoverde.domain.model.ClassificationOutcome
import com.descortezadores.ecoverde.domain.model.ClassificationResult
import com.descortezadores.ecoverde.domain.model.DailyJourneySummary
import com.descortezadores.ecoverde.domain.model.DiagnosticCategory
import com.descortezadores.ecoverde.domain.model.FoliageZoneRecord
import com.descortezadores.ecoverde.domain.model.InfestationFocus
import com.descortezadores.ecoverde.domain.model.SampleType
import com.descortezadores.ecoverde.domain.model.SyncStatus
import com.descortezadores.ecoverde.domain.model.TargetAssociation
import com.descortezadores.ecoverde.domain.model.TreeRecord
import com.descortezadores.ecoverde.domain.repository.SavedDetectionRecord

/**
 * Pestañas de presentación de resultados de corteza (Caso A - Nivel 2).
 */
enum class AnalysisResultTab(val title: String) {
    CLASIFICACION("Manejo de registro"),
    ANALISIS_COLOR("Análisis cromático")
}

/**
 * Secciones de navegación principal de EcoVerde (Hub & Spoke MD3).
 */
enum class EcoVerdeMainDestination(val label: String) {
    INICIO("Inicio"),
    MAPA("Mapa"),
    ANALIZAR("Captura"),
    ALERTAS("Alertas"),
    PERFIL("Perfil");

    companion object {
        val HISTORIAL: EcoVerdeMainDestination get() = ALERTAS
    }
}

/**
 * Pasos guiados del Asistente Lineal (Wizard) para la captura fitosanitaria (4 pasos).
 */
enum class WizardStep(val stepNumber: Int, val title: String) {
    MODALIDAD(1, "Modalidad"),
    VINCULACION(2, "Vinculación territorial"),
    CAPTURA(3, "Captura y encuadre"),
    RESULTADOS(4, "Resultados")
}

/**
 * Modo de asociación para capturas de corteza (RN-37 y chk_arbol_o_zona).
 */
enum class TargetAssociationMode(val label: String) {
    EXISTENTE("Árbol existente"),
    NUEVO("Dar de alta nuevo")
}

/**
 * Filtro de historial local y cola de sincronización (`CU-M08`, `CU-M22`).
 */
enum class HistoryFilterOption(val label: String) {
    TODOS("Todos"),
    INFESTADOS("Infestados"),
    SANOS("Sanos"),
    PENDIENTES_SYNC("Pendientes Sync")
}

/**
 * Filtro rápido de la lista de trabajo del día (Módulo 2 del Plan de Diseño):
 * Todos, Sanos, Infestados, Pendientes de sincronizar.
 */
enum class TreeHealthFilter(val label: String) {
    TODOS("Todos"),
    SANOS("Sanos"),
    INFESTADOS("Infestados"),
    PENDIENTES_SYNC("Pendientes de sincronizar")
}

/**
 * Resumen de progreso e historial fitosanitario de un árbol individual (Figuras 41, 42 y 46).
 * Agrupa todas las capturas tomadas a lo largo del tiempo dentro de la sección independiente del árbol.
 */
data class TreeProgressionSummary(
    val tree: TreeRecord,
    val captures: List<SavedDetectionRecord>,
    val totalCaptures: Int,
    val healthyCount: Int,
    val infestedCount: Int,
    val undeterminedCount: Int,
    val healthyHistoryPercentage: Int,
    val infestedHistoryPercentage: Int,
    val undeterminedHistoryPercentage: Int,
    val averageChromaticHealthyPct: Int,
    val averageChromaticDamagedPct: Int,
    val currentStatus: ClassificationOutcome,
    val lastCaptureMillis: Long?,
    val lastCaptureIso: String?
) {
    val isUnreviewed: Boolean
        get() = totalCaptures == 0 && currentStatus == ClassificationOutcome.NO_DETERMINADO

    val statusBadgeLabel: String
        get() = when {
            isUnreviewed -> "Sin determinar"
            currentStatus == ClassificationOutcome.INFESTADO -> "Infestado"
            currentStatus == ClassificationOutcome.SANO -> "Sano"
            else -> "Indeterminado"
        }

    val syncStatus: SyncStatus
        get() = when {
            captures.any { it.syncStatus == SyncStatus.ERROR } -> SyncStatus.ERROR
            captures.any { it.syncStatus == SyncStatus.PENDIENTE } -> SyncStatus.PENDIENTE
            captures.isNotEmpty() -> SyncStatus.SINCRONIZADO
            else -> SyncStatus.PENDIENTE
        }

    val lastCaptureFormattedTime: String
        get() {
            val iso = lastCaptureIso ?: return "Sin capturas"
            val timePart = iso.substringAfter("T", "").take(5)
            return if (timePart.isNotBlank()) "$timePart hrs" else iso.take(10)
        }
}

/**
 * Pestaña de trabajo en Inicio: Árboles individuales (Corteza) vs Zonas de Follaje (Dosel).
 */
enum class HomeWorkTab(val label: String) {
    ARBOLES("Árboles (Corteza)"),
    ZONAS_FOLLAJE("Zonas de Follaje (Dosel)")
}

/**
 * Resumen de progreso e historial fitosanitario de una Zona de Follaje (macro área / tomas aéreas).
 */
data class FoliageZoneProgressionSummary(
    val zone: FoliageZoneRecord,
    val captures: List<SavedDetectionRecord>,
    val totalCapturas: Int,
    val healthyCount: Int,
    val undeterminedCount: Int,
    val damagedCount: Int,
    val avgPctSano: Int,
    val avgPctRegular: Int,
    val avgPctDanado: Int,
    val currentDominantSignal: String,
    val lastCaptureMillis: Long?,
    val lastCaptureIso: String?
) {
    val statusBadgeLabel: String
        get() = when {
            totalCapturas == 0 -> "Sin tomas"
            avgPctDanado >= 35 -> "Necrosis / Dañado"
            avgPctRegular >= 30 -> "Estrés foliar"
            else -> "Follaje Sano"
        }

    val syncStatus: SyncStatus
        get() = when {
            captures.any { it.syncStatus == SyncStatus.ERROR } -> SyncStatus.ERROR
            captures.any { it.syncStatus == SyncStatus.PENDIENTE } -> SyncStatus.PENDIENTE
            captures.isNotEmpty() -> SyncStatus.SINCRONIZADO
            else -> SyncStatus.PENDIENTE
        }

    val lastCaptureFormattedTime: String
        get() {
            val iso = lastCaptureIso ?: return "Sin tomas"
            val timePart = iso.substringAfter("T", "").take(5)
            return if (timePart.isNotBlank()) "$timePart hrs" else iso.take(10)
        }
}

/**
 * Alerta de proximidad GPS a un Foco de Infestación Activo NOM-019 (`RN-27`, `RN-34`, `CU-M10`).
 */
data class ProximityAlertUi(
    val idFoco: String,
    val distanciaMetros: Double,
    val cantidadArbolesInfectados: Int,
    val mensajeAlerta: String
)

/**
 * Estado inmutable de UI para la pantalla de Análisis Fitosanitario y operación de campo.
 */
data class AnalysisResultUiState(
    val currentDestination: EcoVerdeMainDestination = EcoVerdeMainDestination.INICIO,
    val selectedTab: AnalysisResultTab = AnalysisResultTab.CLASIFICACION,
    val sampleType: SampleType = SampleType.CORTEZA,
    val wizardStep: WizardStep = WizardStep.MODALIDAD,
    val isProcessingVision: Boolean = false,
    val isRenderingOverlay: Boolean = false,
    val isSavingOffline: Boolean = false,
    val isSyncingBackend: Boolean = false,
    val isBackendReachable: Boolean = false,
    val openCvNativeVersion: String = "...",

    // Navegación a la Sección Independiente de un Árbol o Zona
    val homeWorkTab: HomeWorkTab = HomeWorkTab.ARBOLES,
    val selectedTreeDetailId: String? = null,
    val selectedZoneDetailId: String? = null,
    val isInspectingHistoricalRecord: Boolean = false,
    val homeTreeFilter: TreeHealthFilter = TreeHealthFilter.TODOS,

    // Bottom Sheet contextual para mapa de árboles y focos
    val selectedMapTree: TreeRecord? = null,
    val showMapBottomSheet: Boolean = false,
    val focusedMapTreeId: String? = null,
    val showUserLocationOnMap: Boolean = true,
    val mapTargetCoordinates: Pair<Double, Double>? = null,

    // Búsqueda rápida de árbol en Wizard Paso 2
    val treeSearchQuery: String = "",

    // Resultados de Visión por Computadora
    val originalBitmap: Bitmap? = null,
    val uncroppedBitmap: Bitmap? = null,
    val segmentedOverlayBitmap: Bitmap? = null,
    val classificationResult: ClassificationResult? = null,
    val chromaticResult: ChromaticAnalysisResult? = null,
    val processingElapsedMs: Long = 0L,

    // Controles interactivos de Análisis Cromático
    val selectedCategoryFilter: DiagnosticCategory? = null,
    val overlayOpacityAlpha: Float = 0.0f,
    val showOriginalComparison: Boolean = false,

    // Asociación a Árbol (Corteza - RN-37) o Zona (Follaje - chk_arbol_o_zona)
    val associationMode: TargetAssociationMode = TargetAssociationMode.EXISTENTE,
    val selectedTreeId: String? = null,
    val newTreeName: String = "Fresno - Calzada de los Poetas",
    val newTreeSpecies: String = "Fresno (Fraxinus uhdei)",
    val selectedZoneId: String? = null,
    val newZoneName: String = "Zona II - Calzada de los Poetas",
    val newZoneRadiusMeters: String = "55.0",
    val fieldObservations: String = "",
    val captureLatitude: Double = 19.42182,
    val captureLongitude: Double = -99.18203,
    val captureGpsAccuracyMeters: Float? = null,
    val captureHasExifGps: Boolean = false,
    val captureLocationSourceLabel: String = "Ubicación GPS del dispositivo",
    val captureExifDateTimeIso: String? = null,
    val captureExifCameraModel: String? = null,

    // Datos reactivos de Room SQLite y Backend
    val daySummary: DailyJourneySummary = DailyJourneySummary(
        totalCapturasHoy = 0,
        totalSanosHoy = 0,
        totalInfestadosHoy = 0,
        totalNoDeterminadosHoy = 0,
        pendientesSincronizacion = 0,
        erroresSincronizacion = 0,
        alertaEstacionalActiva = false,
        mensajeAlertaEstacional = null
    ),
    val allRecords: List<SavedDetectionRecord> = emptyList(),
    val historyFilter: HistoryFilterOption = HistoryFilterOption.TODOS,
    val availableTrees: List<TreeRecord> = emptyList(),
    val availableZones: List<FoliageZoneRecord> = emptyList(),
    val activeHotspots: List<InfestationFocus> = emptyList(),
    val proximityAlert: ProximityAlertUi? = null,

    // Mensajes informativos y de error
    val statusBannerMessage: String? = null,
    val errorMessage: String? = null,

    // Listados consolidados precalculados (Optimizados para 120Hz sin recalculación en recomposición)
    val treeProgressionList: List<TreeProgressionSummary> = computeTreeProgressions(availableTrees, allRecords),
    val zoneProgressionList: List<FoliageZoneProgressionSummary> = computeZoneProgressions(availableZones, allRecords)
) {
    val filteredHistoryRecords: List<SavedDetectionRecord>
        get() = when (historyFilter) {
            HistoryFilterOption.TODOS -> allRecords
            HistoryFilterOption.INFESTADOS -> allRecords.filter {
                it.classificationOutcome == ClassificationOutcome.INFESTADO
            }
            HistoryFilterOption.SANOS -> allRecords.filter {
                it.classificationOutcome == ClassificationOutcome.SANO
            }
            HistoryFilterOption.PENDIENTES_SYNC -> allRecords.filter {
                it.syncStatus != SyncStatus.SINCRONIZADO
            }
        }


    val filteredTreeProgressions: List<TreeProgressionSummary>
        get() = when (homeTreeFilter) {
            TreeHealthFilter.TODOS -> treeProgressionList
            TreeHealthFilter.SANOS -> treeProgressionList.filter {
                it.currentStatus == ClassificationOutcome.SANO
            }
            TreeHealthFilter.INFESTADOS -> treeProgressionList.filter {
                it.currentStatus == ClassificationOutcome.INFESTADO
            }
            TreeHealthFilter.PENDIENTES_SYNC -> treeProgressionList.filter {
                it.syncStatus != SyncStatus.SINCRONIZADO
            }
        }

    val selectedTreeProgression: TreeProgressionSummary?
        get() = selectedTreeDetailId?.let { id ->
            treeProgressionList.firstOrNull {
                it.tree.idArbol == id || it.tree.nombreEspecifico.equals(id, ignoreCase = true)
            }
        }



    val filteredZoneProgressions: List<FoliageZoneProgressionSummary>
        get() = when (homeTreeFilter) {
            TreeHealthFilter.TODOS -> zoneProgressionList
            TreeHealthFilter.SANOS -> zoneProgressionList.filter {
                it.avgPctSano >= 50 && it.avgPctDanado < 25
            }
            TreeHealthFilter.INFESTADOS -> zoneProgressionList.filter {
                it.avgPctDanado >= 35 || it.damagedCount > 0
            }
            TreeHealthFilter.PENDIENTES_SYNC -> zoneProgressionList.filter {
                it.syncStatus != SyncStatus.SINCRONIZADO
            }
        }

    val selectedZoneProgression: FoliageZoneProgressionSummary?
        get() = selectedZoneDetailId?.let { id ->
            zoneProgressionList.firstOrNull {
                it.zone.idZona == id || it.zone.nombreZona.equals(id, ignoreCase = true)
            }
        }

    val focusedMapTree: TreeRecord?
        get() = if (focusedMapTreeId != null) {
            treeProgressionList.firstOrNull { it.tree.idArbol == focusedMapTreeId }?.tree
                ?: availableTrees.firstOrNull { it.idArbol == focusedMapTreeId }
        } else null

    companion object {
        /**
         * Construye el listado consolidado de árboles con su progreso histórico de capturas.
         * Función pura ejecutable en hilos de background (Dispatchers.Default) para no bloquear la UI a 120Hz.
         */
        fun computeTreeProgressions(
            trees: List<TreeRecord>,
            allRecords: List<SavedDetectionRecord>
        ): List<TreeProgressionSummary> {
            val barkRecords = allRecords.filter { it.sampleType == SampleType.CORTEZA }
            val mergedTrees = trees.toMutableList()

            barkRecords.forEach { rec ->
                val (recId, recName, recSpecies) = when (val assoc = rec.targetAssociation) {
                    is TargetAssociation.CortezaTarget.ExistingTree ->
                        Triple(assoc.idArbol, assoc.nombreEspecifico, assoc.especie)
                    is TargetAssociation.CortezaTarget.NewTree ->
                        Triple(null, assoc.nombreArbolNuevo, assoc.especieArbolNuevo)
                    else -> Triple(null, "", "")
                }
                val alreadyPresent = mergedTrees.any { tree ->
                    (recId != null && tree.idArbol == recId) ||
                        tree.nombreEspecifico.trim().equals(recName.trim(), ignoreCase = true)
                }
                if (!alreadyPresent && recName.isNotBlank()) {
                    mergedTrees.add(
                        TreeRecord(
                            idArbol = recId ?: "virtual-${recName.trim().lowercase()}",
                            nombreEspecifico = recName.trim(),
                            especie = recSpecies.ifBlank { "Fresno (Fraxinus uhdei)" },
                            latitud = rec.latitud,
                            longitud = rec.longitud,
                            fechaRegistroIso = rec.fechaCapturaIso,
                            descripcion = rec.observaciones,
                            idCuenta = rec.brigadistaId,
                            idDispositivo = rec.idDispositivo,
                            ultimoEstadoSanitario = rec.classificationOutcome
                        )
                    )
                }
            }

            return mergedTrees.map { tree ->
                val treeCaptures = barkRecords.filter { rec ->
                    when (val assoc = rec.targetAssociation) {
                        is TargetAssociation.CortezaTarget.ExistingTree ->
                            assoc.idArbol == tree.idArbol ||
                                assoc.nombreEspecifico.trim().equals(tree.nombreEspecifico.trim(), ignoreCase = true)
                        is TargetAssociation.CortezaTarget.NewTree ->
                            assoc.nombreArbolNuevo.trim().equals(tree.nombreEspecifico.trim(), ignoreCase = true)
                        else -> false
                    }
                }.sortedByDescending { it.fechaCapturaMillis }

                val total = treeCaptures.size
                val healthy = treeCaptures.count { it.classificationOutcome == ClassificationOutcome.SANO }
                val infested = treeCaptures.count { it.classificationOutcome == ClassificationOutcome.INFESTADO }
                val undetermined = (total - healthy - infested).coerceAtLeast(0)

                val healthyPct = if (total > 0) kotlin.math.round((healthy * 100.0) / total).toInt() else 0
                val infestedPct = if (total > 0) kotlin.math.round((infested * 100.0) / total).toInt() else 0
                val undeterminedPct = if (total > 0) (100 - healthyPct - infestedPct).coerceAtLeast(0) else 0

                val avgChromaticHealthy = if (total > 0) {
                    kotlin.math.round(
                        treeCaptures.map { (it.chromaticResult?.pctSano ?: 0.0) * 100.0 }.average()
                    ).toInt()
                } else {
                    0
                }
                val avgChromaticDamaged = if (total > 0) {
                    kotlin.math.round(
                        treeCaptures.map {
                            ((it.chromaticResult?.pctDanado ?: 0.0) + (it.chromaticResult?.pctRegular ?: 0.0)) * 100.0
                        }.average()
                    ).toInt()
                } else {
                    0
                }

                val latestOutcome = treeCaptures.firstOrNull()?.classificationOutcome
                    ?: tree.ultimoEstadoSanitario
                    ?: ClassificationOutcome.NO_DETERMINADO

                TreeProgressionSummary(
                    tree = tree,
                    captures = treeCaptures,
                    totalCaptures = total,
                    healthyCount = healthy,
                    infestedCount = infested,
                    undeterminedCount = undetermined,
                    healthyHistoryPercentage = healthyPct,
                    infestedHistoryPercentage = infestedPct,
                    undeterminedHistoryPercentage = undeterminedPct,
                    averageChromaticHealthyPct = avgChromaticHealthy,
                    averageChromaticDamagedPct = avgChromaticDamaged,
                    currentStatus = latestOutcome,
                    lastCaptureMillis = treeCaptures.firstOrNull()?.fechaCapturaMillis,
                    lastCaptureIso = treeCaptures.firstOrNull()?.fechaCapturaIso ?: tree.fechaRegistroIso
                )
            }.sortedWith(
                compareByDescending<TreeProgressionSummary> { it.lastCaptureMillis ?: 0L }
                    .thenBy { it.tree.nombreEspecifico }
            )
        }

        /**
         * Construye el listado consolidado de zonas de follaje con su progreso de tomas.
         * Función pura ejecutable en hilos de background (Dispatchers.Default) para no bloquear la UI a 120Hz.
         */
        fun computeZoneProgressions(
            zones: List<FoliageZoneRecord>,
            allRecords: List<SavedDetectionRecord>
        ): List<FoliageZoneProgressionSummary> {
            val foliageRecords = allRecords.filter { it.sampleType == SampleType.FOLLAJE }
            val mergedZones = zones.toMutableList()

            foliageRecords.forEach { rec ->
                val (recId, recName, recRadius) = when (val assoc = rec.targetAssociation) {
                    is TargetAssociation.FollajeTarget.ExistingZone ->
                        Triple(assoc.idZona, assoc.nombreZona, assoc.radioMetros)
                    is TargetAssociation.FollajeTarget.NewZone ->
                        Triple(null, assoc.nombreZonaNuevo, assoc.radioMetrosZona)
                    else -> Triple(null, "", 50.0)
                }
                val alreadyPresent = mergedZones.any { zone ->
                    (recId != null && zone.idZona == recId) ||
                        zone.nombreZona.trim().equals(recName.trim(), ignoreCase = true)
                }
                if (!alreadyPresent && recName.isNotBlank()) {
                    mergedZones.add(
                        FoliageZoneRecord(
                            idZona = recId ?: "virtual-${recName.trim().lowercase()}",
                            nombreZona = recName.trim(),
                            latitudCentro = rec.latitud,
                            longitudCentro = rec.longitud,
                            radioMetros = recRadius,
                            fechaRegistroIso = rec.fechaCapturaIso,
                            descripcion = rec.observaciones ?: "Zona aérea registrada en campo",
                            idCuenta = rec.brigadistaId,
                            idDispositivo = rec.idDispositivo
                        )
                    )
                }
            }

            return mergedZones.map { zone ->
                val zoneCaptures = foliageRecords.filter { rec ->
                    when (val assoc = rec.targetAssociation) {
                        is TargetAssociation.FollajeTarget.ExistingZone ->
                            assoc.idZona == zone.idZona ||
                                assoc.nombreZona.trim().equals(zone.nombreZona.trim(), ignoreCase = true)
                        is TargetAssociation.FollajeTarget.NewZone ->
                            assoc.nombreZonaNuevo.trim().equals(zone.nombreZona.trim(), ignoreCase = true)
                        else -> false
                    }
                }.sortedByDescending { it.fechaCapturaMillis }

                val total = zoneCaptures.size
                val healthyCount = zoneCaptures.count { it.classificationOutcome == ClassificationOutcome.SANO }
                val damagedCount = zoneCaptures.count { it.classificationOutcome == ClassificationOutcome.INFESTADO }
                val undeterminedCount = (total - healthyCount - damagedCount).coerceAtLeast(0)

                val avgSano = if (total > 0) {
                    kotlin.math.round(zoneCaptures.map { (it.chromaticResult?.pctSano ?: 0.0) * 100.0 }.average()).toInt()
                } else 0
                val avgRegular = if (total > 0) {
                    kotlin.math.round(zoneCaptures.map { (it.chromaticResult?.pctRegular ?: 0.0) * 100.0 }.average()).toInt()
                } else 0
                val avgDanado = if (total > 0) {
                    kotlin.math.round(zoneCaptures.map { (it.chromaticResult?.pctDanado ?: 0.0) * 100.0 }.average()).toInt()
                } else 0

                val dominant = when {
                    avgDanado >= 35 -> "Necrosis / Dañado"
                    avgRegular >= 30 -> "Estrés foliar activo"
                    avgSano >= 45 -> "Follaje Sano y Vigoroso"
                    total == 0 -> "Sin tomas aéreas"
                    else -> "Monitoreo preventivo"
                }

                FoliageZoneProgressionSummary(
                    zone = zone,
                    captures = zoneCaptures,
                    totalCapturas = total,
                    healthyCount = healthyCount,
                    undeterminedCount = undeterminedCount,
                    damagedCount = damagedCount,
                    avgPctSano = avgSano,
                    avgPctRegular = avgRegular,
                    avgPctDanado = avgDanado,
                    currentDominantSignal = dominant,
                    lastCaptureMillis = zoneCaptures.firstOrNull()?.fechaCapturaMillis,
                    lastCaptureIso = zoneCaptures.firstOrNull()?.fechaCapturaIso ?: zone.fechaRegistroIso
                )
            }.sortedWith(
                compareByDescending<FoliageZoneProgressionSummary> { it.lastCaptureMillis ?: 0L }
                    .thenBy { it.zone.nombreZona }
            )
        }
    }
}
