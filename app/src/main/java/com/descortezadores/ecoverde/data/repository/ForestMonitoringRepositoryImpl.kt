package com.descortezadores.ecoverde.data.repository

import android.util.Base64
import androidx.work.WorkManager
import com.descortezadores.ecoverde.core.common.SystemMessages
import com.descortezadores.ecoverde.data.local.EcoVerdeDatabase
import com.descortezadores.ecoverde.data.local.dao.AnalisisCromaticoDao
import com.descortezadores.ecoverde.data.local.dao.ForestMonitoringDao
import com.descortezadores.ecoverde.data.local.entity.AnalisisConCromaticoRelation
import com.descortezadores.ecoverde.data.local.entity.AnalisisCromaticoEntity
import com.descortezadores.ecoverde.data.local.entity.AnalisisEntity
import com.descortezadores.ecoverde.data.local.entity.ArbolEntity
import com.descortezadores.ecoverde.data.local.entity.FocoInfestacionEntity
import com.descortezadores.ecoverde.data.local.entity.ZonaFollajeEntity
import com.descortezadores.ecoverde.data.remote.api.AuthApiService
import com.descortezadores.ecoverde.data.remote.api.MonitoringApiService
import com.descortezadores.ecoverde.data.remote.dto.ArbolDto
import com.descortezadores.ecoverde.data.remote.dto.SyncItemDto
import com.descortezadores.ecoverde.data.remote.dto.ZonaFollajeDto
import com.descortezadores.ecoverde.di.IoDispatcher
import com.descortezadores.ecoverde.domain.model.ChromaticAnalysisResult
import com.descortezadores.ecoverde.domain.model.ClassificationOutcome
import com.descortezadores.ecoverde.domain.model.ClassificationResult
import com.descortezadores.ecoverde.domain.model.DailyJourneySummary
import com.descortezadores.ecoverde.domain.model.FoliageZoneRecord
import com.descortezadores.ecoverde.domain.model.InfestationFocus
import com.descortezadores.ecoverde.domain.model.SampleType
import com.descortezadores.ecoverde.domain.model.SyncStatus
import com.descortezadores.ecoverde.domain.model.TargetAssociation
import com.descortezadores.ecoverde.domain.model.TreeRecord
import com.descortezadores.ecoverde.domain.repository.BatchSyncSummary
import com.descortezadores.ecoverde.domain.repository.ForestMonitoringRepository
import com.descortezadores.ecoverde.domain.repository.SavedDetectionRecord
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import android.content.Context
import com.descortezadores.ecoverde.core.common.DeviceIdentifierProvider
import com.descortezadores.ecoverde.data.local.session.SessionPreferencesManager
import com.descortezadores.ecoverde.data.remote.dto.LoginRequestDto
import com.descortezadores.ecoverde.data.sync.DeteccionesSyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación Offline-First de [ForestMonitoringRepository] (Fase 4).
 *
 * Trabaja primordialmente contra SQLite/Room (`EcoVerdeDatabase`) y sincroniza en lote
 * con el backend local/nube mediante [MonitoringApiService].
 */
@Singleton
class ForestMonitoringRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ecoVerdeDatabase: EcoVerdeDatabase,
    private val forestMonitoringDao: ForestMonitoringDao,
    private val analisisCromaticoDao: AnalisisCromaticoDao,
    private val monitoringApiService: MonitoringApiService,
    private val authApiService: AuthApiService,
    private val sessionPreferencesManager: SessionPreferencesManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ForestMonitoringRepository {

    override suspend fun saveFieldDetectionOffline(
        brigadistaId: String,
        idDispositivo: String,
        sampleType: SampleType,
        classificationResult: ClassificationResult,
        chromaticResult: ChromaticAnalysisResult,
        latitud: Double,
        longitud: Double,
        precisionGpsMetros: Float?,
        observaciones: String?,
        targetAssociation: TargetAssociation
    ): Result<SavedDetectionRecord> = withContext(ioDispatcher) {
        try {
            val idAnalisis = chromaticResult.analysisId.ifBlank { UUID.randomUUID().toString() }
            val nowInstant = Instant.now()
            val nowIso = nowInstant.toString()
            val nowMillis = nowInstant.toEpochMilli()

            // Extraer campos respetando estrictamente la exclusión mutua chk_arbol_o_zona (RN-49)
            var idArbol: String? = null
            var nombreArbolNuevo: String? = null
            var especieArbolNuevo: String? = null
            var idZona: String? = null
            var nombreZonaNuevo: String? = null
            var radioMetrosZona: Double? = null

            when (targetAssociation) {
                is TargetAssociation.CortezaTarget.ExistingTree -> {
                    idArbol = targetAssociation.idArbol
                    nombreArbolNuevo = targetAssociation.nombreEspecifico
                    especieArbolNuevo = targetAssociation.especie
                    val existingTree = forestMonitoringDao.getArbolById(idArbol)
                    if (existingTree != null) {
                        forestMonitoringDao.upsertArbol(
                            existingTree.copy(
                                ultimoEstadoFitosanitario = classificationResult.outcome.apiValue
                            )
                        )
                    }
                }

                is TargetAssociation.CortezaTarget.NewTree -> {
                    val cleanName = targetAssociation.nombreArbolNuevo.trim().ifBlank { "Árbol Chapultepec" }
                    val cleanSpecies = targetAssociation.especieArbolNuevo.trim().ifBlank { "Cedro blanco (Cupressus lusitanica)" }
                    nombreArbolNuevo = cleanName
                    especieArbolNuevo = cleanSpecies

                    // Si ya existe un árbol con ese mismo identificador (ej. "Fresno95"), reutilizar su sección/UUID
                    val existingByName = forestMonitoringDao.getArbolByNombre(cleanName)
                    val localTreeId = existingByName?.idArbol ?: UUID.randomUUID().toString()
                    idArbol = localTreeId

                    forestMonitoringDao.upsertArbol(
                        ArbolEntity(
                            idArbol = localTreeId,
                            nombreEspecifico = cleanName,
                            especie = existingByName?.especie ?: cleanSpecies,
                            latitud = existingByName?.latitud ?: latitud,
                            longitud = existingByName?.longitud ?: longitud,
                            descripcion = targetAssociation.descripcion
                                ?: observaciones
                                ?: existingByName?.descripcion
                                ?: "Registrado en captura de corteza offline",
                            fechaRegistroIso = existingByName?.fechaRegistroIso ?: nowIso,
                            ultimoEstadoFitosanitario = classificationResult.outcome.apiValue,
                            idCuenta = existingByName?.idCuenta ?: brigadistaId,
                            idDispositivo = existingByName?.idDispositivo ?: idDispositivo,
                            pendienteSincronizar = existingByName?.pendienteSincronizar ?: true
                        )
                    )
                }

                is TargetAssociation.FollajeTarget.ExistingZone -> {
                    idZona = targetAssociation.idZona
                    nombreZonaNuevo = targetAssociation.nombreZona
                    radioMetrosZona = targetAssociation.radioMetros
                }

                is TargetAssociation.FollajeTarget.NewZone -> {
                    nombreZonaNuevo = targetAssociation.nombreZonaNuevo
                    radioMetrosZona = targetAssociation.radioMetrosZona

                    val localZoneId = UUID.randomUUID().toString()
                    idZona = localZoneId
                    forestMonitoringDao.upsertZonaFollaje(
                        ZonaFollajeEntity(
                            idZona = localZoneId,
                            nombreZona = nombreZonaNuevo,
                            latitudCentro = latitud,
                            longitudCentro = longitud,
                            radioMetros = radioMetrosZona,
                            descripcion = targetAssociation.descripcion
                                ?: observaciones
                                ?: "Zona aérea registrada en campo",
                            fechaRegistroIso = nowIso,
                            idCuenta = brigadistaId,
                            idDispositivo = idDispositivo,
                            pendienteSincronizar = true
                        )
                    )
                }
            }

            val analisisEntity = AnalisisEntity(
                idAnalisis = idAnalisis,
                brigadistaId = brigadistaId,
                idDispositivo = idDispositivo,
                tipoImagen = sampleType.apiValue,
                resultadoClasificacion = classificationResult.outcome.apiValue,
                confianza = classificationResult.confidence,
                probabilidadCrudaTflite = classificationResult.rawInfestationProbability,
                latitudCaptura = latitud,
                longitudCaptura = longitud,
                precisionGpsMetros = precisionGpsMetros,
                fechaCapturaIso = nowIso,
                fechaCapturaMillis = nowMillis,
                observaciones = observaciones,
                idArbol = idArbol,
                nombreArbolNuevo = nombreArbolNuevo,
                especieArbolNuevo = especieArbolNuevo,
                idZona = idZona,
                nombreZonaNuevo = nombreZonaNuevo,
                radioMetrosZona = radioMetrosZona,
                rutaEvidenciaLocal = chromaticResult.originalImagePath,
                rutaOverlayLocal = chromaticResult.segmentedOverlayImagePath,
                estadoSincronizacion = SyncStatus.PENDIENTE.name
            )

            val cromaticoEntity = AnalisisCromaticoEntity.fromDomainModel(
                result = chromaticResult.copy(analysisId = idAnalisis),
                parentAnalisisId = idAnalisis,
                syncStatus = SyncStatus.PENDIENTE
            )

            forestMonitoringDao.saveFullAnalysisTransaction(
                analisis = analisisEntity,
                cromatico = cromaticoEntity
            )

            Result.success(
                AnalisisConCromaticoRelation(
                    analisis = analisisEntity,
                    cromatico = cromaticoEntity
                ).toDomainRecord()
            )
        } catch (t: Throwable) {
            Result.failure(IllegalStateException(SystemMessages.MSG33, t))
        }
    }

    override fun observeAllDetections(): Flow<List<SavedDetectionRecord>> =
        forestMonitoringDao.observeAllAnalysesWithChromatic().map { list ->
            list.map { it.toDomainRecord() }
        }

    override fun observeDailyJourneySummary(): Flow<DailyJourneySummary> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val startOfDayMillis = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val endOfDayMillis = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1L

        return forestMonitoringDao.observeDailyJourneyAnalyses(
            startOfDayMillis = startOfDayMillis,
            endOfDayMillis = endOfDayMillis
        ).map { records ->
            var sanos = 0
            var infestados = 0
            var noDeterminados = 0
            var pendientes = 0
            var errores = 0

            records.forEach { rel ->
                when (ClassificationOutcome.fromApiValue(rel.analisis.resultadoClasificacion)) {
                    ClassificationOutcome.SANO -> sanos++
                    ClassificationOutcome.INFESTADO -> infestados++
                    ClassificationOutcome.NO_DETERMINADO -> noDeterminados++
                }
                when {
                    rel.analisis.estadoSincronizacion.equals(SyncStatus.ERROR.name, ignoreCase = true) -> {
                        errores++
                        pendientes++
                    }
                    !rel.analisis.estadoSincronizacion.equals(SyncStatus.SINCRONIZADO.name, ignoreCase = true) -> {
                        pendientes++
                    }
                }
            }

            DailyJourneySummary(
                totalCapturasHoy = records.size,
                totalSanosHoy = sanos,
                totalInfestadosHoy = infestados,
                totalNoDeterminadosHoy = noDeterminados,
                pendientesSincronizacion = pendientes,
                erroresSincronizacion = errores,
                alertaEstacionalActiva = isSeasonalNom019Month(today.monthValue),
                mensajeAlertaEstacional = SystemMessages.MSG17
            )
        }
    }

    override fun observePendingSyncCount(): Flow<Int> =
        forestMonitoringDao.observePendingSyncCount()

    override fun observeTrees(
        searchQuery: String,
        healthStatusFilter: ClassificationOutcome?
    ): Flow<List<TreeRecord>> =
        forestMonitoringDao.observeFilteredArboles(
            searchQuery = searchQuery.trim(),
            healthStatusFilter = healthStatusFilter?.apiValue
        ).map { entities ->
            entities.map { it.toDomainModel() }
        }

    override fun observeFoliageZones(): Flow<List<FoliageZoneRecord>> =
        forestMonitoringDao.observeAllZonasFollaje().map { entities ->
            entities.map { it.toDomainModel() }
        }

    override fun observeActiveInfestationFoci(): Flow<List<InfestationFocus>> =
        forestMonitoringDao.observeActiveFocosInfestacion().map { entities ->
            entities.map { it.toDomainModel() }
        }

    override suspend fun registerTree(
        nombreEspecifico: String,
        especie: String,
        latitud: Double,
        longitud: Double,
        descripcion: String?
    ): Result<TreeRecord> = withContext(ioDispatcher) {
        try {
            val cleanName = nombreEspecifico.trim().ifBlank { "Árbol Chapultepec" }
            val cleanSpecies = especie.trim().ifBlank { "Cedro blanco (Cupressus lusitanica)" }
            val existingByName = forestMonitoringDao.getArbolByNombre(cleanName)
            if (existingByName != null) {
                return@withContext Result.success(existingByName.toDomainModel())
            }

            val localId = UUID.randomUUID().toString()

            // Intentar primero en el backend si hay conectividad (POST /api/v1/arboles)
            val dtoRequest = ArbolDto(
                idArbol = localId,
                nombreEspecifico = cleanName,
                especie = cleanSpecies,
                latitud = latitud,
                longitud = longitud,
                descripcion = descripcion?.trim()
            )

            val remoteResponse = runCatching {
                monitoringApiService.createArbol(dtoRequest)
            }.getOrNull()

            if (remoteResponse != null && remoteResponse.isSuccessful && remoteResponse.body() != null) {
                val createdDto = remoteResponse.body()!!
                val entity = ArbolEntity(
                    idArbol = createdDto.idArbol ?: localId,
                    nombreEspecifico = createdDto.nombreEspecifico,
                    especie = createdDto.especie,
                    latitud = createdDto.latitud ?: latitud,
                    longitud = createdDto.longitud ?: longitud,
                    descripcion = createdDto.descripcion ?: descripcion,
                    fechaRegistroIso = createdDto.fechaRegistro ?: Instant.now().toString(),
                    ultimoEstadoFitosanitario = ClassificationOutcome.NO_DETERMINADO.apiValue,
                    idCuenta = createdDto.idCuenta,
                    idDispositivo = createdDto.idDispositivo ?: DeviceIdentifierProvider.getHardwareDeviceUuid(context),
                    pendienteSincronizar = false
                )
                forestMonitoringDao.upsertArbol(entity)
                return@withContext Result.success(entity.toDomainModel())
            }

            // Fallback Offline-First: Guardar en SQLite local con pendienteSincronizar = true
            val offlineEntity = ArbolEntity(
                idArbol = localId,
                nombreEspecifico = cleanName,
                especie = cleanSpecies,
                latitud = latitud,
                longitud = longitud,
                descripcion = descripcion?.trim(),
                fechaRegistroIso = Instant.now().toString(),
                ultimoEstadoFitosanitario = ClassificationOutcome.NO_DETERMINADO.apiValue,
                idDispositivo = DeviceIdentifierProvider.getHardwareDeviceUuid(context),
                pendienteSincronizar = true
            )
            forestMonitoringDao.upsertArbol(offlineEntity)
            Result.success(offlineEntity.toDomainModel())
        } catch (t: Throwable) {
            Result.failure(IllegalStateException(SystemMessages.MSG33, t))
        }
    }

    override suspend fun updateTreeDescription(
        idArbol: String,
        nuevaDescripcion: String
    ): Result<TreeRecord> = withContext(ioDispatcher) {
        try {
            val existing = forestMonitoringDao.getArbolById(idArbol)
                ?: return@withContext Result.failure(IllegalArgumentException(SystemMessages.MSG18))

            val updatedLocal = existing.copy(
                descripcion = nuevaDescripcion.trim(),
                pendienteSincronizar = true
            )
            forestMonitoringDao.upsertArbol(updatedLocal)

            // Intentar actualizar en servidor (PUT /api/v1/arboles/{id}) si hay conexión
            val dto = ArbolDto(
                idArbol = existing.idArbol,
                nombreEspecifico = existing.nombreEspecifico,
                especie = existing.especie,
                latitud = existing.latitud,
                longitud = existing.longitud,
                descripcion = nuevaDescripcion.trim()
            )
            val response = runCatching {
                monitoringApiService.updateArbol(idArbol, dto)
            }.getOrNull()

            if (response != null && response.isSuccessful) {
                val synced = updatedLocal.copy(pendienteSincronizar = false)
                forestMonitoringDao.upsertArbol(synced)
                return@withContext Result.success(synced.toDomainModel())
            }

            Result.success(updatedLocal.toDomainModel())
        } catch (t: Throwable) {
            Result.failure(IllegalStateException(SystemMessages.MSG33, t))
        }
    }

    override suspend fun syncPendingDetectionsWithBackend(): Result<BatchSyncSummary> =
        withContext(ioDispatcher) {
            try {
                // 1. Ping previo a /api/v1/health
                val healthResp = runCatching { authApiService.checkHealth() }.getOrNull()
                if (healthResp == null || !healthResp.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException(SystemMessages.MSG16))
                }

                // 1b. Garantizar que el dispositivo actual esté auto-registrado y enlazado en el backend via POST /login
                val hardwareDeviceUuid = DeviceIdentifierProvider.getHardwareDeviceUuid(context)
                runCatching {
                    val creds = sessionPreferencesManager.getCredentialsForAutoRefresh()
                    if (creds != null) {
                        val loginResp = authApiService.login(
                            LoginRequestDto(
                                username = creds.first,
                                password = creds.second,
                                idDispositivo = hardwareDeviceUuid,
                                nombreDispositivo = DeviceIdentifierProvider.getDeviceModelName(),
                                identificadorUnico = DeviceIdentifierProvider.getRealAndroidId(context)
                            )
                        )
                        val newToken = loginResp.body()?.accessToken
                        if (loginResp.isSuccessful && !newToken.isNullOrBlank()) {
                            sessionPreferencesManager.updateAccessToken(newToken)
                        }
                    }
                }

                // 1.5. Sincronizar árboles y zonas pendientes en Room para garantizar integridad de llaves foráneas
                val pendingTrees = forestMonitoringDao.getPendingSyncArboles()
                for (tree in pendingTrees) {
                    runCatching {
                        val treeDto = ArbolDto(
                            idArbol = tree.idArbol,
                            nombreEspecifico = tree.nombreEspecifico,
                            especie = tree.especie,
                            latitud = tree.latitud,
                            longitud = tree.longitud,
                            descripcion = tree.descripcion
                        )
                        val resp = monitoringApiService.createArbol(treeDto)
                        if (resp.isSuccessful) {
                            forestMonitoringDao.markArbolesSynced(listOf(tree.idArbol))
                        }
                    }
                }

                val pendingZonas = forestMonitoringDao.getPendingSyncZonas()
                for (zona in pendingZonas) {
                    runCatching {
                        val zonaDto = ZonaFollajeDto(
                            idZona = zona.idZona,
                            latitudCentro = zona.latitudCentro,
                            longitudCentro = zona.longitudCentro,
                            nombreZona = zona.nombreZona,
                            radioMetros = zona.radioMetros,
                            descripcion = zona.descripcion
                        )
                        val resp = monitoringApiService.createZonaFollaje(zonaDto)
                        if (resp.isSuccessful) {
                            forestMonitoringDao.markZonasSynced(listOf(zona.idZona))
                        }
                    }
                }

                // 2. Obtener detecciones pendientes en Room
                val pendingList = forestMonitoringDao.getPendingSyncAnalyses()
                var totalSynced = 0
                val syncedIds = mutableListOf<String>()
                val syncErrors = mutableListOf<String>()

                if (pendingList.isNotEmpty()) {
                    val syncDtos = pendingList.map { rel ->
                        val a = rel.analisis
                        val c = rel.cromatico
                        val isCorteza = a.tipoImagen.equals("corteza", ignoreCase = true)

                        // Obtener metadatos del árbol local para que el backend pueda auto-crearlo
                        // con su mismo UUID si aún no existe en PostgreSQL (SyncDeteccionUseCase auto-reparador)
                        val localTree = if (isCorteza && !a.idArbol.isNullOrBlank()) {
                            forestMonitoringDao.getArbolById(a.idArbol)
                        } else {
                            null
                        }

                        val effectiveIdArbol = if (isCorteza) a.idArbol?.takeIf { it.isNotBlank() } else null
                        val effectiveNombreArbolNuevo = if (isCorteza) {
                            a.nombreArbolNuevo?.takeIf { it.isNotBlank() }
                                ?: localTree?.nombreEspecifico
                                ?: "Árbol Chapultepec #${a.idAnalisis.take(4)}"
                        } else {
                            null
                        }
                        val effectiveEspecieArbolNuevo = if (isCorteza) {
                            a.especieArbolNuevo?.takeIf { it.isNotBlank() }
                                ?: localTree?.especie
                                ?: "Cedro blanco (Cupressus lusitanica)"
                        } else {
                            null
                        }

                        SyncItemDto(
                            idAnalisisLocal = a.idAnalisis,
                            latitud = a.latitudCaptura,
                            longitud = a.longitudCaptura,
                            fechaCaptura = a.fechaCapturaIso,
                            tipoImagen = a.tipoImagen,
                            resultadoClasificacion = a.resultadoClasificacion,
                            confianza = a.confianza,
                            observaciones = a.observaciones,
                            // Exclusión mutua chk_arbol_o_zona + soporte auto-reparador en SyncDeteccionUseCase
                            idArbol = effectiveIdArbol,
                            nombreArbolNuevo = effectiveNombreArbolNuevo,
                            especieArbolNuevo = effectiveEspecieArbolNuevo,
                            idZona = if (!isCorteza) a.idZona?.takeIf { it.isNotBlank() } else null,
                            nombreZonaNuevo = if (!isCorteza) {
                                a.nombreZonaNuevo?.takeIf { it.isNotBlank() }
                                    ?: "Zona Follaje #${a.idAnalisis.take(4)}"
                            } else {
                                null
                            },
                            radioMetrosZona = if (!isCorteza) (a.radioMetrosZona ?: 50.0) else null,
                            pctSano = c?.pctSano ?: 0.0,
                            pctDanado = c?.pctDanado ?: 0.0,
                            pctRegular = c?.pctRegular ?: 0.0,
                            pctIncierto = c?.pctIncierto ?: 0.0,
                            imagenBase64 = encodeFileToBase64OrNull(a.rutaEvidenciaLocal),
                            idDispositivo = DeviceIdentifierProvider.getHardwareDeviceUuid(context)
                        )
                    }

                    val syncResponse = monitoringApiService.syncDetecciones(syncDtos)
                    if (syncResponse.isSuccessful && syncResponse.body() != null) {
                        val body = syncResponse.body()!!
                        totalSynced = body.totalSincronizados
                        syncedIds.addAll(body.idsSincronizados)
                        syncErrors.addAll(body.errores)

                        if (syncedIds.isNotEmpty()) {
                            val nowIso = Instant.now().toString()
                            forestMonitoringDao.markAnalysesSynced(
                                ids = syncedIds,
                                newStatus = SyncStatus.SINCRONIZADO.name,
                                syncedAtIso = nowIso
                            )
                            analisisCromaticoDao.updateSyncStatusByParentIds(
                                parentAnalisisIds = syncedIds,
                                newStatus = SyncStatus.SINCRONIZADO.name
                            )
                        }

                        if (totalSynced == 0 && syncErrors.isNotEmpty()) {
                            return@withContext Result.failure(
                                IllegalStateException("Error en sincronización: ${syncErrors.joinToString("; ")}")
                            )
                        }
                    } else {
                        val errMsg = "HTTP ${syncResponse.code()}: ${syncResponse.message()}"
                        syncErrors.add(errMsg)
                        forestMonitoringDao.markAnalysesFailed(
                            ids = pendingList.map { it.analisis.idAnalisis },
                            errorMessage = errMsg
                        )
                        return@withContext Result.failure(
                            IllegalStateException("${SystemMessages.MSG25} ($errMsg)")
                        )
                    }
                }

                // 3. Actualizar catálogos de árboles, zonas de follaje y focos NOM-019
                val catalogsOk = refreshRemoteCatalogsAndFoci().getOrDefault(false)

                // 4. Consultar estadísticas y alerta estacional NOM-019 (GET /api/v1/estadisticas/dashboard)
                val dashResp = runCatching {
                    monitoringApiService.getDashboardEstadisticas()
                }.getOrNull()
                val dashBody = dashResp?.body()

                Result.success(
                    BatchSyncSummary(
                        totalPendingFound = pendingList.size,
                        totalSynced = totalSynced,
                        syncedIds = syncedIds,
                        errors = syncErrors,
                        catalogsRefreshed = catalogsOk,
                        seasonalAlertActive = dashBody?.alertaEstacionalActiva ?: false,
                        seasonalAlertMessage = dashBody?.mensajeAlertaEstacional
                    )
                )
            } catch (t: Throwable) {
                Result.failure(IllegalStateException(SystemMessages.MSG25, t))
            }
        }

    override suspend fun refreshRemoteCatalogsAndFoci(): Result<Boolean> =
        withContext(ioDispatcher) {
            try {
                var anyUpdated = false

                // 1. Descargar catálogo de árboles (GET /api/v1/arboles)
                val arbolesResp = runCatching { monitoringApiService.getArboles() }.getOrNull()
                if (arbolesResp != null && arbolesResp.isSuccessful && arbolesResp.body() != null) {
                    val remoteTrees = arbolesResp.body()!!.mapNotNull { dto ->
                        val id = dto.idArbol ?: return@mapNotNull null
                        val existing = forestMonitoringDao.getArbolById(id)
                        ArbolEntity(
                            idArbol = id,
                            nombreEspecifico = dto.nombreEspecifico,
                            especie = dto.especie,
                            latitud = dto.latitud ?: 19.4204,
                            longitud = dto.longitud ?: -99.1819,
                            descripcion = dto.descripcion,
                            fechaRegistroIso = dto.fechaRegistro ?: Instant.now().toString(),
                            ultimoEstadoFitosanitario = existing?.ultimoEstadoFitosanitario
                                ?: ClassificationOutcome.SANO.apiValue,
                            idCuenta = dto.idCuenta,
                            idDispositivo = dto.idDispositivo,
                            pendienteSincronizar = false
                        )
                    }
                    if (remoteTrees.isNotEmpty()) {
                        forestMonitoringDao.upsertAllArboles(remoteTrees)
                        anyUpdated = true
                    }
                }

                // 2. Descargar zonas de follaje (GET /api/v1/zonas-follaje)
                val zonasResp = runCatching { monitoringApiService.getZonasFollaje() }.getOrNull()
                if (zonasResp != null && zonasResp.isSuccessful && zonasResp.body() != null) {
                    val remoteZones = zonasResp.body()!!.map { dto ->
                        ZonaFollajeEntity(
                            idZona = dto.idZona,
                            nombreZona = dto.nombreZona,
                            latitudCentro = dto.latitudCentro,
                            longitudCentro = dto.longitudCentro,
                            radioMetros = dto.radioMetros ?: 50.0,
                            descripcion = dto.descripcion,
                            fechaRegistroIso = dto.fechaRegistro ?: Instant.now().toString(),
                            idCuenta = dto.idCuenta,
                            idDispositivo = dto.idDispositivo,
                            pendienteSincronizar = false
                        )
                    }
                    if (remoteZones.isNotEmpty()) {
                        forestMonitoringDao.upsertAllZonasFollaje(remoteZones)
                        anyUpdated = true
                    }
                }

                // 3. Descargar focos de infestación activos NOM-019 (GET /api/v1/estadisticas/focos)
                val focosResp = runCatching {
                    monitoringApiService.getFocosInfestacionActivos()
                }.getOrNull()
                if (focosResp != null && focosResp.isSuccessful && focosResp.body() != null) {
                    val remoteFoci = focosResp.body()!!.map { dto ->
                        FocoInfestacionEntity(
                            idFoco = dto.idFoco,
                            latitudCentro = dto.latitudCentro,
                            longitudCentro = dto.longitudCentro,
                            radioMetros = dto.radioMetros,
                            cantidadArbolesInfectados = dto.cantidadArbolesInfectados.coerceAtLeast(3),
                            idsArbolesCsv = dto.idsArboles.joinToString("|"),
                            nombresArbolesCsv = dto.nombresArboles.joinToString("|"),
                            fechaActualizacionMillis = System.currentTimeMillis()
                        )
                    }
                    forestMonitoringDao.replaceAllFocosInfestacion(remoteFoci)
                    anyUpdated = true
                }

                Result.success(anyUpdated)
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }

    override suspend fun deleteTree(idArbol: String): Result<Unit> =
        withContext(ioDispatcher) {
            try {
                val analyses = forestMonitoringDao.getAnalysesByTreeList(idArbol)
                analyses.forEach { rel ->
                    runCatching { File(rel.rutaEvidenciaLocal).delete() }
                    runCatching { File(rel.rutaOverlayLocal).delete() }
                }

                forestMonitoringDao.deleteTreeAndAssociatedData(idArbol)

                // Si hay conectividad con backend, eliminar también allá
                runCatching {
                    monitoringApiService.deleteArbol(idArbol)
                }

                Result.success(Unit)
            } catch (t: Throwable) {
                Result.failure(IllegalStateException("No se pudo eliminar el árbol seleccionado.", t))
            }
        }

    override suspend fun deleteZonaFollaje(idZona: String): Result<Unit> =
        withContext(ioDispatcher) {
            try {
                val analyses = forestMonitoringDao.getAnalysesByZonaList(idZona)
                analyses.forEach { rel ->
                    runCatching { File(rel.rutaEvidenciaLocal).delete() }
                    runCatching { File(rel.rutaOverlayLocal).delete() }
                }

                forestMonitoringDao.deleteZonaAndAssociatedData(idZona)

                // Si hay conectividad con backend, eliminar también allá
                runCatching {
                    monitoringApiService.deleteZonaFollaje(idZona)
                }

                Result.success(Unit)
            } catch (t: Throwable) {
                Result.failure(IllegalStateException("No se pudo eliminar la zona seleccionada.", t))
            }
        }

    override suspend fun updateZonaFollaje(
        idZona: String,
        nombreZona: String,
        radioMetros: Double,
        nuevaDescripcion: String?
    ): Result<Unit> = withContext(ioDispatcher) {
        try {
            val existing = forestMonitoringDao.getZonaById(idZona)
                ?: return@withContext Result.failure(IllegalArgumentException("Zona no encontrada: $idZona"))

            val updated = existing.copy(
                nombreZona = nombreZona.trim(),
                radioMetros = radioMetros,
                descripcion = nuevaDescripcion?.trim(),
                pendienteSincronizar = true
            )
            forestMonitoringDao.upsertZonaFollaje(updated)

            val dto = ZonaFollajeDto(
                idZona = existing.idZona,
                latitudCentro = existing.latitudCentro,
                longitudCentro = existing.longitudCentro,
                nombreZona = nombreZona.trim(),
                radioMetros = radioMetros,
                descripcion = nuevaDescripcion?.trim()
            )
            val resp = runCatching {
                monitoringApiService.updateZonaFollaje(idZona, dto)
            }.getOrNull()

            if (resp != null && resp.isSuccessful) {
                forestMonitoringDao.markZonasSynced(listOf(idZona))
            }

            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(IllegalStateException("No se pudo actualizar la zona de follaje.", t))
        }
    }

    override suspend fun clearAllLocalDatabase(): Result<Unit> =
        withContext(ioDispatcher) {
            try {
                // 1. Cancelar cualquier sincronización pendiente en WorkManager para evitar que re-descargue o sincronice datos obsoletos
                runCatching {
                    WorkManager.getInstance(context).cancelUniqueWork(DeteccionesSyncWorker.UNIQUE_WORK_NAME)
                }

                // 2. Limpieza total de Room (invalida trackers y vacía tablas)
                runCatching {
                    ecoVerdeDatabase.clearAllTables()
                }
                forestMonitoringDao.clearAllLocalMonitoringData()

                // 3. Eliminar fotografías locales de disco y capturas en caché
                runCatching {
                    val dir = File(context.filesDir, "chromatic_analyses")
                    if (dir.exists()) {
                        dir.deleteRecursively()
                    }
                    val cacheCaptures = File(context.cacheDir, "camera_captures")
                    if (cacheCaptures.exists()) {
                        cacheCaptures.deleteRecursively()
                    }
                }

                // 4. Si el backend está en línea, limpiar también el catálogo remoto para que no vuelva a auto-descargar las categorías eliminadas
                runCatching {
                    monitoringApiService.deleteAllArboles()
                }
                runCatching {
                    monitoringApiService.deleteAllZonasFollaje()
                }

                Result.success(Unit)
            } catch (t: Throwable) {
                Result.failure(IllegalStateException("No se pudo limpiar la base de datos local.", t))
            }
        }

    private fun isSeasonalNom019Month(month: Int): Boolean {
        // RN-44: Ene-May (1..5) y Sep-Nov (9..11)
        return month in 1..5 || month in 9..11
    }

    private fun encodeFileToBase64OrNull(filePath: String): String? {
        return runCatching {
            val file = File(filePath)
            if (!file.exists() || !file.canRead() || file.length() > 8_000_000L) {
                null
            } else {
                Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
            }
        }.getOrNull()
    }

    private fun AnalisisConCromaticoRelation.toDomainRecord(): SavedDetectionRecord {
        val sampleType = SampleType.fromApiValue(analisis.tipoImagen)
        val outcome = ClassificationOutcome.fromApiValue(analisis.resultadoClasificacion)
        val syncStatus = runCatching {
            SyncStatus.valueOf(analisis.estadoSincronizacion.uppercase())
        }.getOrDefault(SyncStatus.PENDIENTE)

        val targetAssociation: TargetAssociation = if (sampleType == SampleType.CORTEZA) {
            if (!analisis.idArbol.isNullOrBlank()) {
                TargetAssociation.CortezaTarget.ExistingTree(
                    idArbol = analisis.idArbol,
                    nombreEspecifico = analisis.nombreArbolNuevo ?: "Árbol Catalogado",
                    especie = analisis.especieArbolNuevo ?: "Fresno (Fraxinus uhdei)"
                )
            } else {
                TargetAssociation.CortezaTarget.NewTree(
                    nombreArbolNuevo = analisis.nombreArbolNuevo?.takeIf { it.isNotBlank() } ?: "Árbol Local",
                    especieArbolNuevo = analisis.especieArbolNuevo?.takeIf { it.isNotBlank() } ?: "Cedro blanco (Cupressus lusitanica)",
                    descripcion = analisis.observaciones
                )
            }
        } else {
            if (!analisis.idZona.isNullOrBlank()) {
                TargetAssociation.FollajeTarget.ExistingZone(
                    idZona = analisis.idZona,
                    nombreZona = analisis.nombreZonaNuevo ?: "Zona Catalogada",
                    radioMetros = analisis.radioMetrosZona ?: 50.0
                )
            } else {
                TargetAssociation.FollajeTarget.NewZone(
                    nombreZonaNuevo = analisis.nombreZonaNuevo?.takeIf { it.isNotBlank() } ?: "Zona Follaje Local",
                    radioMetrosZona = (analisis.radioMetrosZona ?: 50.0).coerceAtLeast(1.0),
                    descripcion = analisis.observaciones
                )
            }
        }

        return SavedDetectionRecord(
            idAnalisis = analisis.idAnalisis,
            brigadistaId = analisis.brigadistaId,
            idDispositivo = analisis.idDispositivo,
            sampleType = sampleType,
            classificationOutcome = outcome,
            confidence = analisis.confianza,
            rawTfliteProbability = analisis.probabilidadCrudaTflite,
            latitud = analisis.latitudCaptura,
            longitud = analisis.longitudCaptura,
            fechaCapturaIso = analisis.fechaCapturaIso,
            fechaCapturaMillis = analisis.fechaCapturaMillis,
            observaciones = analisis.observaciones,
            targetAssociation = targetAssociation,
            originalImagePath = analisis.rutaEvidenciaLocal,
            overlayImagePath = analisis.rutaOverlayLocal,
            syncStatus = syncStatus,
            chromaticResult = cromatico?.toDomainModel()
        )
    }
}
