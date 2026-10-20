package com.descortezadores.ecoverde.data.classification

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorSpace
import android.os.SystemClock
import android.util.Log
import com.descortezadores.ecoverde.BuildConfig
import com.descortezadores.ecoverde.core.common.SystemMessages
import com.descortezadores.ecoverde.di.DefaultDispatcher
import com.descortezadores.ecoverde.domain.model.ClassificationOutcome
import com.descortezadores.ecoverde.domain.model.ClassificationResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.floor
import kotlin.math.min

/**
 * Motor de inferencia local con TensorFlow Lite (`TfliteBarkClassifier` - Fase 3).
 *
 * Replica de forma matemáticamente exacta el pipeline de `models/evaluar_test.py`:
 * 1. El modelo `light_classifier.tflite` (EfficientNet-B3) YA CONTIENE internamente la capa
 *    `Rescaling(scale=1/255)` (`0.00392156862745098`) y `Normalization` de ImageNet como primeras
 *    subcapas del grafo. Por lo tanto, el tensor de entrada DEBE entregarse en el rango `[0.0f, 255.0f]`
 *    en `FLOAT32` (si se dividiera entre 255 afuera, se dividiría dos veces: `x / 65025`).
 * 2. En `evaluar_test.py`:
 *    ```python
 *    img_tensor = tf.convert_to_tensor(img_array_bruto, dtype=tf.float32)
 *    img_tensor = tf.image.resize(img_tensor, [300, 300])
 *    ```
 *    Para obtener el mismo resultado que `tf.image.resize` en Python, aquí implementamos la
 *    interpolación bilineal exacta de TensorFlow (`half_pixel_centers = true`) directamente en
 *    punto flotante `Float32` sobre la imagen original (evitando `Bitmap.createScaledBitmap`, el
 *    cual usa el filtro de Skia y trunca los valores intermedios a enteros de 8 bits).
 */
@Singleton
class TfliteBarkClassifier @Inject constructor(
    @ApplicationContext private val context: Context,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {
    private val interpreterMutex = Mutex()
    private var interpreter: Interpreter? = null

    /**
     * Clasifica un [Bitmap] en memoria usando interpolación bilineal exacta de TensorFlow (`tf.image.resize`).
     */
    suspend fun classifyBitmap(sourceBitmap: Bitmap): Result<ClassificationResult> =
        withContext(defaultDispatcher) {
            try {
                val startTime = SystemClock.elapsedRealtime()
                val activeInterpreter = getOrCreateInterpreter()

                val inputBuffer = preprocessBitmapLikeTfImageResize(sourceBitmap)
                val outputBuffer = Array(1) { FloatArray(1) }

                interpreterMutex.withLock {
                    activeInterpreter.run(inputBuffer, outputBuffer)
                }

                val inferenceTimeMs = (SystemClock.elapsedRealtime() - startTime).coerceAtLeast(1L)
                val rawProb = outputBuffer[0][0].toDouble().coerceIn(0.0, 1.0)

                // Paridad 1:1 con evaluar_test.py: prob_tflite >= 0.5 -> Infestado else Sano
                val (outcome, classConfidence) = ClassificationOutcome.evaluateFromProbability(
                    infestationProbability = rawProb,
                    decisionThreshold = DECISION_THRESHOLD,
                    enforceStrictRn24 = false
                )

                Log.i(
                    TAG,
                    "TFLite inferencia completada en ${inferenceTimeMs}ms -> " +
                        "TFLite(%)=${String.format(java.util.Locale.US, "%.2f", rawProb * 100.0)}%, " +
                        "Clase=${outcome.displayTitle}"
                )

                val visualEvidences = buildVisualEvidences(outcome, rawProb)
                val recommendation = buildRecommendation(outcome, classConfidence, rawProb)

                Result.success(
                    ClassificationResult(
                        outcome = outcome,
                        confidence = classConfidence,
                        rawInfestationProbability = rawProb,
                        isOfflineExecution = true,
                        executionBadgeLabel = "Offline", // RN-22
                        visualEvidencesDetected = visualEvidences,
                        recommendation = recommendation,
                        inferenceTimeMs = inferenceTimeMs
                    )
                )
            } catch (t: Throwable) {
                Log.e(TAG, "Error en inferencia TFLite", t)
                Result.failure(IllegalStateException(SystemMessages.MSG22, t))
            }
        }

    /**
     * Clasifica una fotografía desde su ruta en almacenamiento local, decodificándola en sRGB
     * sin escalado de densidad (idéntico a `Image.open(ruta).convert("RGB")` en Python PIL).
     */
    suspend fun classifyImageFile(imagePath: String): Result<ClassificationResult> =
        withContext(defaultDispatcher) {
            val file = File(imagePath)
            if (!file.exists() || !file.canRead()) {
                return@withContext Result.failure(IllegalArgumentException(SystemMessages.MSG19))
            }
            val options = BitmapFactory.Options().apply {
                inScaled = false
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inPreferredColorSpace = ColorSpace.get(ColorSpace.Named.SRGB)
            }
            val bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
                ?: return@withContext Result.failure(IllegalArgumentException(SystemMessages.MSG19))
            classifyBitmap(bitmap)
        }

    suspend fun reloadModel() = withContext(defaultDispatcher) {
        interpreterMutex.withLock {
            interpreter?.close()
            interpreter = null
        }
    }

    private suspend fun getOrCreateInterpreter(): Interpreter {
        interpreter?.let { return it }
        return interpreterMutex.withLock {
            interpreter?.let { return@withLock it }
            val modelBuffer = loadModelMappedBuffer()
            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }
            Interpreter(modelBuffer, options).also { created ->
                interpreter = created
                Log.i(TAG, "Intérprete TFLite (EfficientNet-B3) cargado exitosamente.")
            }
        }
    }

    private fun loadModelMappedBuffer(): MappedByteBuffer {
        val otaModelFile = File(context.filesDir, "models/${BuildConfig.TFLITE_MODEL_FILENAME}")
        if (otaModelFile.exists() && otaModelFile.length() > 1_000_000L) {
            FileInputStream(otaModelFile).use { inputStream ->
                return inputStream.channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    0L,
                    otaModelFile.length()
                )
            }
        }

        context.assets.openFd(BuildConfig.TFLITE_MODEL_FILENAME).use { assetFd ->
            FileInputStream(assetFd.fileDescriptor).use { inputStream ->
                return inputStream.channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    assetFd.startOffset,
                    assetFd.declaredLength
                )
            }
        }
    }

    /**
     * Implementa de forma exacta `tf.image.resize(img_tensor, [300, 300], method='bilinear', half_pixel_centers=True)`
     * sobre los píxeles RGB originales en `Float32` `[0.0f, 255.0f]`.
     *
     * A diferencia de `Bitmap.createScaledBitmap`, no cuantiza los valores interpolados a `uint8`,
     * produciendo exactamente el mismo tensor `float32` que `preprocesar_imagen()` en `evaluar_test.py`.
     */
    private fun preprocessBitmapLikeTfImageResize(sourceBitmap: Bitmap): ByteBuffer {
        val argbBitmap = if (sourceBitmap.config == Bitmap.Config.ARGB_8888) {
            sourceBitmap
        } else {
            sourceBitmap.copy(Bitmap.Config.ARGB_8888, false)
        }

        val inWidth = argbBitmap.width
        val inHeight = argbBitmap.height
        val srcPixels = IntArray(inWidth * inHeight)
        argbBitmap.getPixels(srcPixels, 0, inWidth, 0, 0, inWidth, inHeight)

        val byteBuffer = ByteBuffer.allocateDirect(1 * INPUT_SIZE_PX * INPUT_SIZE_PX * CHANNELS * BYTES_PER_FLOAT)
        byteBuffer.order(ByteOrder.nativeOrder())

        // Si ya es 300x300, copia directa en Float32 [0.0f, 255.0f]
        if (inWidth == INPUT_SIZE_PX && inHeight == INPUT_SIZE_PX) {
            for (pixel in srcPixels) {
                byteBuffer.putFloat(((pixel shr 16) and 0xFF).toFloat())
                byteBuffer.putFloat(((pixel shr 8) and 0xFF).toFloat())
                byteBuffer.putFloat((pixel and 0xFF).toFloat())
            }
            byteBuffer.rewind()
            return byteBuffer
        }

        // Algoritmo exacto de TensorFlow C++ `ResizeBilinearOp` con `half_pixel_centers = true`
        val scaleY = inHeight.toFloat() / INPUT_SIZE_PX.toFloat()
        val scaleX = inWidth.toFloat() / INPUT_SIZE_PX.toFloat()

        val xLeftIndices = IntArray(INPUT_SIZE_PX)
        val xRightIndices = IntArray(INPUT_SIZE_PX)
        val xLerps = FloatArray(INPUT_SIZE_PX)

        for (x in 0 until INPUT_SIZE_PX) {
            val inX = (x.toFloat() + 0.5f) * scaleX - 0.5f
            if (inX < 0.0f) {
                xLeftIndices[x] = 0
                xRightIndices[x] = 0
                xLerps[x] = 0.0f
            } else {
                val left = floor(inX).toInt().coerceAtMost(inWidth - 1)
                val right = min(left + 1, inWidth - 1)
                xLeftIndices[x] = left
                xRightIndices[x] = right
                xLerps[x] = inX - left.toFloat()
            }
        }

        for (y in 0 until INPUT_SIZE_PX) {
            val inY = (y.toFloat() + 0.5f) * scaleY - 0.5f
            val topY: Int
            val bottomY: Int
            val yLerp: Float
            if (inY < 0.0f) {
                topY = 0
                bottomY = 0
                yLerp = 0.0f
            } else {
                topY = floor(inY).toInt().coerceAtMost(inHeight - 1)
                bottomY = min(topY + 1, inHeight - 1)
                yLerp = inY - topY.toFloat()
            }

            val topRowOffset = topY * inWidth
            val bottomRowOffset = bottomY * inWidth

            for (x in 0 until INPUT_SIZE_PX) {
                val leftX = xLeftIndices[x]
                val rightX = xRightIndices[x]
                val xLerp = xLerps[x]

                val pTopLeft = srcPixels[topRowOffset + leftX]
                val pTopRight = srcPixels[topRowOffset + rightX]
                val pBottomLeft = srcPixels[bottomRowOffset + leftX]
                val pBottomRight = srcPixels[bottomRowOffset + rightX]

                // Canal R
                val rTL = ((pTopLeft shr 16) and 0xFF).toFloat()
                val rTR = ((pTopRight shr 16) and 0xFF).toFloat()
                val rBL = ((pBottomLeft shr 16) and 0xFF).toFloat()
                val rBR = ((pBottomRight shr 16) and 0xFF).toFloat()
                val rTop = rTL + (rTR - rTL) * xLerp
                val rBot = rBL + (rBR - rBL) * xLerp
                val rVal = rTop + (rBot - rTop) * yLerp

                // Canal G
                val gTL = ((pTopLeft shr 8) and 0xFF).toFloat()
                val gTR = ((pTopRight shr 8) and 0xFF).toFloat()
                val gBL = ((pBottomLeft shr 8) and 0xFF).toFloat()
                val gBR = ((pBottomRight shr 8) and 0xFF).toFloat()
                val gTop = gTL + (gTR - gTL) * xLerp
                val gBot = gBL + (gBR - gBL) * xLerp
                val gVal = gTop + (gBot - gTop) * yLerp

                // Canal B
                val bTL = (pTopLeft and 0xFF).toFloat()
                val bTR = (pTopRight and 0xFF).toFloat()
                val bBL = (pBottomLeft and 0xFF).toFloat()
                val bBR = (pBottomRight and 0xFF).toFloat()
                val bTop = bTL + (bTR - bTL) * xLerp
                val bBot = bBL + (bBR - bBL) * xLerp
                val bVal = bTop + (bBot - bTop) * yLerp

                byteBuffer.putFloat(rVal)
                byteBuffer.putFloat(gVal)
                byteBuffer.putFloat(bVal)
            }
        }

        byteBuffer.rewind()
        return byteBuffer
    }

    private fun buildVisualEvidences(outcome: ClassificationOutcome, rawProb: Double): List<String> =
        when (outcome) {
            ClassificationOutcome.INFESTADO -> listOf(
                "Patrones texturales compatibles con galerías subcorticales o perforaciones en fuste (RN-45).",
                "Posibles exudados/grumos de resina o alteración cromática asociada a colonización por descortezadores.",
                "Salida TFLite (%): ${String.format(java.util.Locale.US, "%.2f", rawProb * 100.0)}% (>= 50.0%)."
            )
            ClassificationOutcome.SANO -> listOf(
                "Corteza con textura continua sin evidencia dominante de galerías ni grumos de resina (RN-45).",
                "Ausencia de señales visuales críticas de colonización activa.",
                "Salida TFLite (%): ${String.format(java.util.Locale.US, "%.2f", rawProb * 100.0)}% (< 50.0%)."
            )
            ClassificationOutcome.NO_DETERMINADO -> listOf(
                "La confianza del clasificador no alcanzó el umbral mínimo del 75% establecido por RN-24.",
                "Posible variación de iluminación, sombras, desenfoque o distancia de captura."
            )
        }

    private fun buildRecommendation(
        outcome: ClassificationOutcome,
        confidence: Double,
        rawProb: Double
    ): String {
        val lowConfidenceNotice = if (confidence < ClassificationOutcome.MIN_CONFIDENCE_THRESHOLD) {
            " (Nota RN-24: Confianza ${(confidence * 100).toInt()}% < 75%; se sugiere segunda toma de confirmación)."
        } else {
            ""
        }
        return when (outcome) {
            ClassificationOutcome.INFESTADO ->
                "Clasificado como INFESTADO (TFLite: ${String.format(java.util.Locale.US, "%.2f", rawProb * 100.0)}%). Inspeccionar el ejemplar conforme a NOM-019-SEMARNAT-2017 y revisar árboles en un radio de 100 m.$lowConfidenceNotice"
            ClassificationOutcome.SANO ->
                "Clasificado como SANO (TFLite: ${String.format(java.util.Locale.US, "%.2f", rawProb * 100.0)}%). Mantener monitoreo preventivo periódico.$lowConfidenceNotice"
            ClassificationOutcome.NO_DETERMINADO ->
                "Resultado No determinado. Se recomienda repetir la captura acercando el encuadre a la corteza con iluminación uniforme (RN-24)."
        }
    }

    companion object {
        private const val TAG = "TfliteBarkClassifier"
        const val INPUT_SIZE_PX = 300 // RN-09: Entrada de EfficientNet-B3 (300x300)
        private const val CHANNELS = 3
        private const val BYTES_PER_FLOAT = 4
        private const val DECISION_THRESHOLD = 0.50
    }
}
