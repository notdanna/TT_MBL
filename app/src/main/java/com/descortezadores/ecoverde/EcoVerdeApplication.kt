package com.descortezadores.ecoverde

import android.app.Application
import com.descortezadores.ecoverde.core.opencv.OpenCvInitializer
import com.descortezadores.ecoverde.data.classification.TfliteBarkClassifier
import com.descortezadores.ecoverde.di.ApplicationScope
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Punto de entrada de la aplicación EcoVerde.
 * Inicializa Hilt y dispara de forma asíncrona la carga de las librerías nativas (.so)
 * de OpenCV mediante [OpenCvInitializer] y el intérprete TFLite mediante [TfliteBarkClassifier].
 */
@HiltAndroidApp
class EcoVerdeApplication : Application() {

    @Inject
    lateinit var openCvInitializer: OpenCvInitializer

    @Inject
    lateinit var tfliteBarkClassifier: TfliteBarkClassifier

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            openCvInitializer.ensureInitialized()
        }
        applicationScope.launch {
            tfliteBarkClassifier.prewarm()
        }
    }
}
