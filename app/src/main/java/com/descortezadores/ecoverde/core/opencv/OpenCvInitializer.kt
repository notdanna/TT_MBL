package com.descortezadores.ecoverde.core.opencv

import android.util.Log
import com.descortezadores.ecoverde.di.DefaultDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.opencv.android.OpenCVLoader
import org.opencv.core.Core
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Estado del motor nativo OpenCV (librerías .so JNI).
 */
sealed interface OpenCvStatus {
    data object Uninitialized : OpenCvStatus
    data object Initializing : OpenCvStatus
    data class Ready(
        val version: String,
        val nativeLibraryLoaded: Boolean = true
    ) : OpenCvStatus
    data class Error(val reason: String) : OpenCvStatus
}

/**
 * Inicializador asíncrono y seguro para el SDK de OpenCV Android (RN-46).
 * Verifica la carga de las librerías nativas (.so) mediante OpenCVLoader.initLocal().
 */
@Singleton
class OpenCvInitializer @Inject constructor(
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {
    private val mutex = Mutex()
    private val _status = MutableStateFlow<OpenCvStatus>(OpenCvStatus.Uninitialized)
    val status: StateFlow<OpenCvStatus> = _status.asStateFlow()

    val isReady: Boolean
        get() = _status.value is OpenCvStatus.Ready

    /**
     * Inicializa las librerías nativas de OpenCV de manera asíncrona en [Dispatchers.Default]
     * si aún no han sido cargadas.
     */
    suspend fun ensureInitialized(): Boolean = withContext(defaultDispatcher) {
        if (_status.value is OpenCvStatus.Ready) return@withContext true

        mutex.withLock {
            if (_status.value is OpenCvStatus.Ready) return@withLock true
            _status.value = OpenCvStatus.Initializing

            return@withLock try {
                val loaded = OpenCVLoader.initLocal()
                if (loaded) {
                    val version = Core.VERSION
                    _status.value = OpenCvStatus.Ready(version = version)
                    Log.i(TAG, "OpenCV Android SDK inicializado exitosamente. Versión nativa: $version")
                    true
                } else {
                    val msg = "OpenCVLoader.initLocal() retornó false al enlazar librerías .so"
                    _status.value = OpenCvStatus.Error(msg)
                    Log.e(TAG, msg)
                    false
                }
            } catch (t: Throwable) {
                val msg = t.message ?: "Excepción JNI al cargar librerías nativas de OpenCV"
                _status.value = OpenCvStatus.Error(msg)
                Log.e(TAG, "Error crítico inicializando OpenCV", t)
                false
            }
        }
    }

    companion object {
        private const val TAG = "OpenCvInitializer"
    }
}
