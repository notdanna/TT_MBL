package com.descortezadores.ecoverde.data.sync

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.descortezadores.ecoverde.domain.repository.ForestMonitoringRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Worker de `WorkManager` (`DeteccionesSyncWorker` - Fase 4, `RF-15`, `RN-32`).
 *
 * Sincroniza en segundo plano la cola local de detecciones pendientes (`POST /api/v1/sync/detecciones`)
 * en cuanto el dispositivo recupera conectividad de red (`NetworkType.CONNECTED`), previa verificación
 * de `/api/v1/health`.
 */
class DeteccionesSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SyncWorkerEntryPoint {
        fun forestMonitoringRepository(): ForestMonitoringRepository
    }

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                SyncWorkerEntryPoint::class.java
            )
            val repository = entryPoint.forestMonitoringRepository()
            val syncResult = repository.syncPendingDetectionsWithBackend()
            syncResult.fold(
                onSuccess = { summary ->
                    Log.i(
                        TAG,
                        "Sincronización WorkManager completada: ${summary.totalSynced}/${summary.totalPendingFound} sincronizados."
                    )
                    Result.success()
                },
                onFailure = { error ->
                    Log.w(TAG, "Sincronización pospuesta: ${error.message}")
                    Result.retry()
                }
            )
        } catch (t: Throwable) {
            Log.e(TAG, "Error en DeteccionesSyncWorker", t)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "DeteccionesSyncWorker"
        const val UNIQUE_WORK_NAME = "ecoverde_sync_detecciones_work"

        /**
         * Encola una tarea de sincronización inmediata condicionada a conectividad de red (`RN-32`).
         */
        fun enqueueImmediateSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<DeteccionesSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
