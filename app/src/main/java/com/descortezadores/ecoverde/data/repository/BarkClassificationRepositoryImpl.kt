package com.descortezadores.ecoverde.data.repository

import com.descortezadores.ecoverde.data.classification.TfliteBarkClassifier
import com.descortezadores.ecoverde.domain.model.ClassificationResult
import com.descortezadores.ecoverde.domain.repository.BarkClassificationRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación del repositorio de clasificación local con TensorFlow Lite (RF-08, RF-09, RN-22 a RN-24).
 */
@Singleton
class BarkClassificationRepositoryImpl @Inject constructor(
    private val tfliteBarkClassifier: TfliteBarkClassifier
) : BarkClassificationRepository {

    override suspend fun classifyImageOffline(imagePath: String): Result<ClassificationResult> {
        return tfliteBarkClassifier.classifyImageFile(imagePath)
    }
}
