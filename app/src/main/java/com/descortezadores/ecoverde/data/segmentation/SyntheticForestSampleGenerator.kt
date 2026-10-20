package com.descortezadores.ecoverde.data.segmentation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.descortezadores.ecoverde.domain.model.SampleType

/**
 * Generador de muestras sintéticas calibradas en espacio HSV (300×300 px, RN-09) para pruebas
 * de campo y validación en tiempo real del motor OpenCV + TensorFlow Lite (Fase 3 y Fase 6).
 */
object SyntheticForestSampleGenerator {

    /**
     * Genera un [Bitmap] ARGB_8888 de 300×300 píxeles con regiones bien definidas de
     * tejido Dañado, Regular, Sano e Incierto según la Tabla Oficial de Calibración HSV.
     */
    fun createCalibratedSampleBitmap(
        sampleType: SampleType,
        width: Int = 300,
        height: Int = 300
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

        when (sampleType) {
            SampleType.CORTEZA -> {
                // Fondo base: Corteza natural grisácea/mate (Sano: H=20°, S=15%, V=45%)
                paint.color = Color.HSVToColor(floatArrayOf(20f, 0.15f, 0.45f))
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

                // Galerías subcorticales y exudados café-anaranjados (Dañado: H=32° -> OpenCV H=16, S=75%, V=55%)
                paint.color = Color.HSVToColor(floatArrayOf(32f, 0.75f, 0.55f))
                canvas.drawRect(25f, 30f, width * 0.78f, height * 0.58f, paint)
                canvas.drawCircle(width * 0.65f, height * 0.72f, 48f, paint)

                // Grumos frescos de resina blanca/crema (Regular: H=58° -> OpenCV H=29, S=18%, V=90%)
                paint.color = Color.HSVToColor(floatArrayOf(58f, 0.18f, 0.90f))
                canvas.drawCircle(width * 0.30f, height * 0.76f, 36f, paint)
                canvas.drawCircle(width * 0.75f, height * 0.25f, 28f, paint)

                // Sombra profunda en esquina inferior (Incierto: V=10%)
                paint.color = Color.HSVToColor(floatArrayOf(0f, 0.05f, 0.10f))
                canvas.drawRect(0f, height * 0.88f, width * 0.28f, height.toFloat(), paint)
            }

            SampleType.FOLLAJE -> {
                // Fondo base: Follaje vigoroso verde vivo (Sano: H=115° -> OpenCV H=57, S=70%, V=68%)
                paint.color = Color.HSVToColor(floatArrayOf(115f, 0.70f, 0.68f))
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

                // Clorosis verde-amarillento limón (Regular: H=56° -> OpenCV H=28, S=68%, V=78%)
                paint.color = Color.HSVToColor(floatArrayOf(56f, 0.68f, 0.78f))
                canvas.drawCircle(width * 0.32f, height * 0.42f, 62f, paint)

                // Follaje necrótico café-rojizo (Dañado: H=16° -> OpenCV H=8, S=76%, V=58% y H=348° -> OpenCV H=174)
                paint.color = Color.HSVToColor(floatArrayOf(16f, 0.76f, 0.58f))
                canvas.drawCircle(width * 0.70f, height * 0.55f, 54f, paint)
                paint.color = Color.HSVToColor(floatArrayOf(348f, 0.72f, 0.55f))
                canvas.drawCircle(width * 0.62f, height * 0.26f, 34f, paint)

                // Cielo abierto / sombra (Incierto: H=210° -> OpenCV H=105, S=20%, V=90%)
                paint.color = Color.HSVToColor(floatArrayOf(210f, 0.20f, 0.90f))
                canvas.drawRect(0f, 0f, width * 0.25f, height * 0.18f, paint)
            }
        }

        return bitmap
    }
}
