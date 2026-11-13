package com.descortezadores.ecoverde.core.common

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorSpace
import android.graphics.Matrix
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.abs

/**
 * Resultado unificado al cargar una fotografía desde Cámara directa o desde Galería,
 * incluyendo el [Bitmap] en ARGB_8888 (corregido por orientación EXIF) y sus metadatos extraídos.
 */
data class ExtractedImagePayload(
    val bitmap: Bitmap,
    val latitude: Double,
    val longitude: Double,
    val gpsAccuracyMeters: Float?,
    val hasExifGps: Boolean,
    val hasExifMetadata: Boolean,
    val locationSourceLabel: String,
    val exifDateTimeIso: String?,
    val exifCameraModel: String?,
    val imageWidthPx: Int,
    val imageHeightPx: Int
)

/**
 * Ubicación GPS obtenida del hardware del dispositivo.
 */
data class DeviceGpsFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val isLiveGps: Boolean
)

/**
 * Utilidad central para:
 * 1. Captura de fotografías en resolución completa mediante [FileProvider] conservando metadatos EXIF.
 * 2. Extracción de metadatos EXIF (coordenadas GPS `latLong`, fecha/hora `DateTimeOriginal`, modelo de cámara y orientación)
 *    tanto de fotos tomadas directamente con la cámara como de imágenes seleccionadas de la galería.
 * 3. Fallback automático a la ubicación GPS actual real del dispositivo (`FusedLocationProviderClient` / `LocationManager`)
 *    cuando una imagen subida desde galería o tomada con cámara no contiene etiquetas GPS en su EXIF.
 */
object ImageMetadataAndLocationHelper {

    private const val DEFAULT_CHAPULTEPEC_LAT = 19.42182
    private const val DEFAULT_CHAPULTEPEC_LON = -99.18203

    const val MIN_IMAGE_DIMENSION_PX = 300
    const val MAX_IMAGE_SIZE_BYTES = 10L * 1024L * 1024L

    private val ALLOWED_IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "bmp", "heic")
    private val ALLOWED_IMAGE_MIME_TYPES = setOf(
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/webp",
        "image/bmp",
        "image/x-ms-bmp",
        "image/heic",
        "image/heif"
    )

    /**
     * Valida las reglas RN-07, RN-08 y RN-09 para un URI seleccionado de galería:
     * - Formato permitido (`jpg, jpeg, png, webp, bmp, heic`)
     * - Tamaño máximo de 10 MB
     * - Resolución mínima de 300×300 px
     * @return `null` si es válida, o mensaje descriptivo con prefijo [SystemMessages.MSG19].
     */
    suspend fun validateGalleryUriPreconditions(
        context: Context,
        uri: Uri
    ): String? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri)?.lowercase(Locale.ROOT)
        var displayName: String? = null
        var sizeBytes: Long? = null

        runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIdx >= 0) displayName = cursor.getString(nameIdx)
                        if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) sizeBytes = cursor.getLong(sizeIdx)
                    }
                }
        }

        if (sizeBytes == null) {
            sizeBytes = runCatching {
                resolver.openAssetFileDescriptor(uri, "r")?.use { it.length }
            }.getOrNull()?.takeIf { it > 0L }
        }

        val extFromName = displayName?.substringAfterLast('.', "")?.lowercase(Locale.ROOT)
            ?.takeIf { it.isNotBlank() && displayName?.contains('.') == true }
        val extFromMime = mimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
            ?.lowercase(Locale.ROOT)

        val formatValid = (mimeType != null && mimeType in ALLOWED_IMAGE_MIME_TYPES) ||
            (extFromName != null && extFromName in ALLOWED_IMAGE_EXTENSIONS) ||
            (extFromMime != null && extFromMime in ALLOWED_IMAGE_EXTENSIONS)

        if (!formatValid && (mimeType != null || extFromName != null)) {
            return@withContext "${SystemMessages.MSG19} Formato no soportado (usa JPG, JPEG, PNG, WebP, BMP o HEIC)."
        }

        val resolvedSize = sizeBytes
        if (resolvedSize != null && resolvedSize > MAX_IMAGE_SIZE_BYTES) {
            val sizeMb = String.format(Locale.US, "%.1f", resolvedSize / (1024.0 * 1024.0))
            return@withContext "${SystemMessages.MSG19} La imagen supera el límite de 10 MB ($sizeMb MB)."
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val decodedBounds = runCatching {
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
                bounds.outWidth > 0 && bounds.outHeight > 0
            } ?: false
        }.getOrDefault(false)

        if (!decodedBounds) {
            return@withContext "${SystemMessages.MSG19} No se pudieron leer las dimensiones de la imagen."
        }
        if (bounds.outWidth < MIN_IMAGE_DIMENSION_PX || bounds.outHeight < MIN_IMAGE_DIMENSION_PX) {
            return@withContext "${SystemMessages.MSG19} Resolución insuficiente (${bounds.outWidth}×${bounds.outHeight} px; mínimo ${MIN_IMAGE_DIMENSION_PX}×${MIN_IMAGE_DIMENSION_PX} px)."
        }
        null
    }

    /**
     * Valida RN-07, RN-08 y RN-09 para un archivo capturado en disco.
     */
    suspend fun validateImageFilePreconditions(file: File): String? = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() <= 0L) {
            return@withContext "${SystemMessages.MSG18} El archivo de captura está vacío o no existe."
        }
        val ext = file.extension.lowercase(Locale.ROOT)
        if (ext.isNotBlank() && ext !in ALLOWED_IMAGE_EXTENSIONS) {
            return@withContext "${SystemMessages.MSG19} Formato .$ext no compatible."
        }
        if (file.length() > MAX_IMAGE_SIZE_BYTES) {
            val sizeMb = String.format(Locale.US, "%.1f", file.length() / (1024.0 * 1024.0))
            return@withContext "${SystemMessages.MSG19} La imagen supera el límite de 10 MB ($sizeMb MB)."
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return@withContext "${SystemMessages.MSG19} Archivo de imagen inválido o dañado."
        }
        if (bounds.outWidth < MIN_IMAGE_DIMENSION_PX || bounds.outHeight < MIN_IMAGE_DIMENSION_PX) {
            return@withContext "${SystemMessages.MSG19} Resolución insuficiente (${bounds.outWidth}×${bounds.outHeight} px; mínimo ${MIN_IMAGE_DIMENSION_PX}×${MIN_IMAGE_DIMENSION_PX} px)."
        }
        null
    }

    /**
     * Crea un archivo temporal JPEG en `cacheDir/camera_captures/` y devuelve su par `(File, Uri)`
     * listo para usarse con `ActivityResultContracts.TakePicture()`.
     */
    fun createTempCameraCaptureFileAndUri(context: Context): Pair<File, Uri> {
        val dir = File(context.cacheDir, "camera_captures").apply {
            if (!exists()) mkdirs()
        }
        val file = File(dir, "ecoverde_capture_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return file to uri
    }

    /**
     * Verifica si la aplicación cuenta con permiso de ubicación concedido (`FINE` o `COARSE`).
     */
    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /**
     * Verifica si los permisos de lectura de imágenes y metadatos EXIF (`READ_MEDIA_IMAGES` /
     * `READ_EXTERNAL_STORAGE` y `ACCESS_MEDIA_LOCATION`) están concedidos.
     */
    fun hasMediaLocationPermission(context: Context): Boolean {
        val readImagesGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
        val mediaLocationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_MEDIA_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        return readImagesGranted && mediaLocationGranted
    }

    /**
     * Verifica si la aplicación cuenta con permiso de cámara concedido.
     */
    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Observa actualizaciones continuas de ubicación GPS en tiempo real mediante
     * [FusedLocationProviderClient] para reflejar movimiento en el mapa y alertas de proximidad.
     */
    @SuppressLint("MissingPermission")
    fun observeLocationUpdates(
        context: Context,
        intervalMs: Long = 3000L,
        minDistanceMeters: Float = 2f
    ): Flow<DeviceGpsFix> = callbackFlow {
        if (!hasLocationPermission(context)) {
            close()
            return@callbackFlow
        }

        val fusedClient = runCatching {
            LocationServices.getFusedLocationProviderClient(context)
        }.getOrNull()

        if (fusedClient == null) {
            close()
            return@callbackFlow
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(1500L)
            .setMinUpdateDistanceMeters(minDistanceMeters)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                if (isValidCoordinate(loc.latitude, loc.longitude)) {
                    trySend(
                        DeviceGpsFix(
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            accuracyMeters = if (loc.hasAccuracy()) loc.accuracy else null,
                            isLiveGps = true
                        )
                    )
                }
            }
        }

        runCatching {
            fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        }.onFailure {
            close(it)
        }

        awaitClose {
            runCatching { fusedClient.removeLocationUpdates(callback) }
        }
    }

    /**
     * Obtiene la ubicación GPS actual del dispositivo (o la última ubicación conocida del proveedor)
     * utilizando `FusedLocationProviderClient` y `LocationManager` como respaldo.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentDeviceLocation(
        context: Context,
        fallbackLat: Double = DEFAULT_CHAPULTEPEC_LAT,
        fallbackLon: Double = DEFAULT_CHAPULTEPEC_LON
    ): DeviceGpsFix = withContext(Dispatchers.IO) {
        if (!hasLocationPermission(context)) {
            return@withContext DeviceGpsFix(
                latitude = fallbackLat,
                longitude = fallbackLon,
                accuracyMeters = null,
                isLiveGps = false
            )
        }

        val fusedClient = runCatching {
            LocationServices.getFusedLocationProviderClient(context)
        }.getOrNull()

        // 1. Intentar obtener posición fresca de alta precisión (timeout rápido de 2500 ms para no bloquear en interiores)
        val liveLocation: Location? = if (fusedClient != null) {
            withTimeoutOrNull(2500L) {
                suspendCancellableCoroutine { cont ->
                    val tokenSource = CancellationTokenSource()
                    cont.invokeOnCancellation { tokenSource.cancel() }
                    fusedClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        tokenSource.token
                    ).addOnSuccessListener { loc ->
                        if (cont.isActive) cont.resume(loc)
                    }.addOnFailureListener {
                        if (cont.isActive) cont.resume(null)
                    }
                }
            }
        } else {
            null
        }

        if (liveLocation != null && isValidCoordinate(liveLocation.latitude, liveLocation.longitude)) {
            return@withContext DeviceGpsFix(
                latitude = liveLocation.latitude,
                longitude = liveLocation.longitude,
                accuracyMeters = if (liveLocation.hasAccuracy()) liveLocation.accuracy else null,
                isLiveGps = true
            )
        }

        // 2. Respaldo: última ubicación conocida en FusedLocationProviderClient
        val lastFusedLocation: Location? = if (fusedClient != null) {
            withTimeoutOrNull(1200L) {
                suspendCancellableCoroutine { cont ->
                    fusedClient.lastLocation
                        .addOnSuccessListener { loc ->
                            if (cont.isActive) cont.resume(loc)
                        }
                        .addOnFailureListener {
                            if (cont.isActive) cont.resume(null)
                        }
                }
            }
        } else {
            null
        }

        if (lastFusedLocation != null && isValidCoordinate(lastFusedLocation.latitude, lastFusedLocation.longitude)) {
            return@withContext DeviceGpsFix(
                latitude = lastFusedLocation.latitude,
                longitude = lastFusedLocation.longitude,
                accuracyMeters = if (lastFusedLocation.hasAccuracy()) lastFusedLocation.accuracy else null,
                isLiveGps = true
            )
        }

        // 3. Respaldo: LocationManager nativo (GPS_PROVIDER / NETWORK_PROVIDER)
        val managerLocation = runCatching {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )
            providers.mapNotNull { provider ->
                runCatching { lm?.getLastKnownLocation(provider) }.getOrNull()
            }.maxByOrNull { it.time }
        }.getOrNull()

        if (managerLocation != null && isValidCoordinate(managerLocation.latitude, managerLocation.longitude)) {
            return@withContext DeviceGpsFix(
                latitude = managerLocation.latitude,
                longitude = managerLocation.longitude,
                accuracyMeters = if (managerLocation.hasAccuracy()) managerLocation.accuracy else null,
                isLiveGps = true
            )
        }

        DeviceGpsFix(
            latitude = fallbackLat,
            longitude = fallbackLon,
            accuracyMeters = null,
            isLiveGps = false
        )
    }

    /**
     * Procesa una imagen seleccionada desde la **Galería** (`Uri`):
     * 1. Extrae los metadatos EXIF (`latLong`, fecha/hora de captura, modelo de dispositivo y orientación).
     * 2. Si la imagen de galería tiene coordenadas GPS en sus metadatos EXIF, utiliza esas coordenadas.
     * 3. Si la imagen NO tiene coordenadas GPS en su EXIF, obtiene y utiliza la ubicación GPS actual del teléfono.
     */
    suspend fun processGalleryUri(
        context: Context,
        uri: Uri,
        fallbackLat: Double,
        fallbackLon: Double
    ): ExtractedImagePayload? = withContext(Dispatchers.IO) {
        val rawExif = readExifFromUri(context, uri)
        val decodedBitmap = decodeBitmapFromUri(context, uri, rawExif?.orientation ?: ExifInterface.ORIENTATION_NORMAL)
            ?: return@withContext null

        val validExifGps = rawExif?.latLong?.takeIf { isValidCoordinate(it[0], it[1]) }

        val (finalLat, finalLon, accuracy, sourceLabel) = if (validExifGps != null) {
            Quadruple(
                validExifGps[0],
                validExifGps[1],
                null,
                "Metadatos EXIF de la imagen (Galería)"
            )
        } else {
            val gpsFix = getCurrentDeviceLocation(
                context = context,
                fallbackLat = fallbackLat,
                fallbackLon = fallbackLon
            )
            val label = if (gpsFix.isLiveGps) {
                "Ubicación GPS actual (imagen sin GPS en EXIF)"
            } else {
                "Ubicación activa en mapa (imagen sin GPS en EXIF)"
            }
            Quadruple(gpsFix.latitude, gpsFix.longitude, gpsFix.accuracyMeters, label)
        }

        ExtractedImagePayload(
            bitmap = decodedBitmap,
            latitude = finalLat,
            longitude = finalLon,
            gpsAccuracyMeters = accuracy,
            hasExifGps = validExifGps != null,
            hasExifMetadata = rawExif?.hasAnyExifTag == true,
            locationSourceLabel = sourceLabel,
            exifDateTimeIso = rawExif?.dateTimeIso,
            exifCameraModel = rawExif?.cameraModel,
            imageWidthPx = decodedBitmap.width,
            imageHeightPx = decodedBitmap.height
        )
    }

    /**
     * Procesa una fotografía tomada directamente con la **Cámara** (`File`):
     * 1. Lee los metadatos EXIF generados por la cámara en el archivo JPEG.
     * 2. Si la cámara no incrustó coordenadas GPS o fecha en el EXIF, obtiene la ubicación GPS actual
     *    del dispositivo e incrusta los metadatos EXIF (`GPSLatitude`, `GPSLongitude`, `DateTimeOriginal`, `Make`, `Model`)
     *    directamente en el archivo capturado.
     * 3. Decodifica el `Bitmap` en `ARGB_8888` respetando la orientación EXIF.
     */
    suspend fun processCameraCaptureFile(
        context: Context,
        captureFile: File,
        fallbackLat: Double,
        fallbackLon: Double
    ): ExtractedImagePayload? = withContext(Dispatchers.IO) {
        if (!captureFile.exists() || captureFile.length() == 0L) return@withContext null

        val initialExif = runCatching { ExifInterface(captureFile) }.getOrNull()
        val validCameraExifGps = initialExif?.latLong?.takeIf { isValidCoordinate(it[0], it[1]) }
        val hasCameraExifGps = validCameraExifGps != null

        val gpsFix = if (validCameraExifGps == null) {
            getCurrentDeviceLocation(
                context = context,
                fallbackLat = fallbackLat,
                fallbackLon = fallbackLon
            )
        } else {
            null
        }

        val finalLat = validCameraExifGps?.get(0) ?: gpsFix?.latitude ?: fallbackLat
        val finalLon = validCameraExifGps?.get(1) ?: gpsFix?.longitude ?: fallbackLon

        // Asegurar que el archivo JPEG de cámara tenga todos sus metadatos EXIF grabados
        runCatching {
            val exif = ExifInterface(captureFile)
            if (!hasCameraExifGps && isValidCoordinate(finalLat, finalLon)) {
                exif.setLatLong(finalLat, finalLon)
            }
            if (exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL).isNullOrBlank()) {
                val exifNow = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).format(Date())
                exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, exifNow)
                exif.setAttribute(ExifInterface.TAG_DATETIME, exifNow)
            }
            if (exif.getAttribute(ExifInterface.TAG_MAKE).isNullOrBlank()) {
                exif.setAttribute(ExifInterface.TAG_MAKE, Build.MANUFACTURER)
            }
            if (exif.getAttribute(ExifInterface.TAG_MODEL).isNullOrBlank()) {
                exif.setAttribute(ExifInterface.TAG_MODEL, Build.MODEL)
            }
            exif.saveAttributes()
        }

        val updatedExif = readExifFromFile(captureFile)
        val decodedBitmap = decodeBitmapFromFile(
            file = captureFile,
            orientation = updatedExif?.orientation ?: ExifInterface.ORIENTATION_NORMAL
        ) ?: return@withContext null

        val sourceLabel = when {
            hasCameraExifGps -> "Metadatos EXIF de Cámara"
            gpsFix?.isLiveGps == true -> "GPS actual del dispositivo (grabado en EXIF de Cámara)"
            else -> "Ubicación activa (grabada en EXIF de Cámara)"
        }

        ExtractedImagePayload(
            bitmap = decodedBitmap,
            latitude = finalLat,
            longitude = finalLon,
            gpsAccuracyMeters = gpsFix?.accuracyMeters,
            hasExifGps = hasCameraExifGps || (gpsFix?.isLiveGps == true),
            hasExifMetadata = true,
            locationSourceLabel = sourceLabel,
            exifDateTimeIso = updatedExif?.dateTimeIso ?: Instant.now().toString(),
            exifCameraModel = updatedExif?.cameraModel ?: DeviceIdentifierProvider.getDeviceModelName(),
            imageWidthPx = decodedBitmap.width,
            imageHeightPx = decodedBitmap.height
        )
    }

    /**
     * Escribe y preserva los metadatos EXIF (coordenadas GPS, fecha/hora de captura y modelo de cámara)
     * dentro del archivo JPEG final guardado en el almacenamiento interno (`orig_<analysisId>.jpg`).
     */
    fun writeExifMetadataToFile(
        targetJpgFile: File,
        latitude: Double,
        longitude: Double,
        captureTimestampIso: String?,
        cameraModel: String?
    ) {
        if (!targetJpgFile.exists()) return
        runCatching {
            val exif = ExifInterface(targetJpgFile)
            if (isValidCoordinate(latitude, longitude)) {
                exif.setLatLong(latitude, longitude)
            }
            val exifDateStr = captureTimestampIso?.let { isoToExifDateString(it) }
                ?: SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).format(Date())
            exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, exifDateStr)
            exif.setAttribute(ExifInterface.TAG_DATETIME, exifDateStr)
            exif.setAttribute(ExifInterface.TAG_MAKE, Build.MANUFACTURER)
            exif.setAttribute(
                ExifInterface.TAG_MODEL,
                cameraModel?.takeIf { it.isNotBlank() } ?: Build.MODEL
            )
            exif.saveAttributes()
        }
    }

    private data class RawExifData(
        val latLong: DoubleArray?,
        val dateTimeIso: String?,
        val cameraModel: String?,
        val orientation: Int,
        val hasAnyExifTag: Boolean
    )

    private data class Quadruple<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )

    private fun readExifFromUri(context: Context, uri: Uri): RawExifData? {
        // En Android 10+ (API 29+), MediaStore.setRequireOriginal permite leer las etiquetas GPS sin censura
        // cuando el permiso ACCESS_MEDIA_LOCATION ha sido concedido o la foto proviene del selector.
        val effectiveUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching { MediaStore.setRequireOriginal(uri) }.getOrDefault(uri)
        } else {
            uri
        }

        val exif = runCatching {
            context.contentResolver.openFileDescriptor(effectiveUri, "r")?.use { pfd ->
                ExifInterface(pfd.fileDescriptor)
            }
        }.getOrNull() ?: openExifInputStream(context, effectiveUri)?.use { stream ->
            runCatching { ExifInterface(stream) }.getOrNull()
        } ?: runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                ExifInterface(pfd.fileDescriptor)
            }
        }.getOrNull() ?: openExifInputStream(context, uri)?.use { stream ->
            runCatching { ExifInterface(stream) }.getOrNull()
        } ?: return null

        val extracted = extractRawExifData(exif)
        android.util.Log.i(
            "ImageMetadataHelper",
            "readExifFromUri uri=$uri latLong=${extracted.latLong?.joinToString(",")} date=${extracted.dateTimeIso}"
        )
        return extracted
    }

    private fun openExifInputStream(context: Context, uri: Uri): InputStream? {
        return runCatching {
            context.contentResolver.openInputStream(uri)
        }.getOrNull()
    }

    private fun readExifFromFile(file: File): RawExifData? {
        val exif = runCatching { ExifInterface(file) }.getOrNull() ?: return null
        return extractRawExifData(exif)
    }

    private fun extractRawExifData(exif: ExifInterface): RawExifData {
        val latLon = exif.latLong
        val rawDate = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            ?.takeIf { it.isNotBlank() }
            ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
                ?.takeIf { it.isNotBlank() }
            ?: exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED)
                ?.takeIf { it.isNotBlank() }

        val dateTimeIso = rawDate?.let { parseExifDateToIso(it) }
        val make = exif.getAttribute(ExifInterface.TAG_MAKE)?.trim().orEmpty()
        val model = exif.getAttribute(ExifInterface.TAG_MODEL)?.trim().orEmpty()
        val cameraModel = when {
            make.isNotBlank() && model.isNotBlank() && !model.startsWith(make, ignoreCase = true) -> "$make $model"
            model.isNotBlank() -> model
            make.isNotBlank() -> make
            else -> null
        }
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        val hasAnyExif = latLon != null || rawDate != null || cameraModel != null ||
            orientation != ExifInterface.ORIENTATION_NORMAL

        return RawExifData(
            latLong = latLon,
            dateTimeIso = dateTimeIso,
            cameraModel = cameraModel,
            orientation = orientation,
            hasAnyExifTag = hasAnyExif
        )
    }

    private fun decodeBitmapFromUri(context: Context, uri: Uri, orientation: Int): Bitmap? {
        val rawBitmap = runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val options = BitmapFactory.Options().apply {
                    inScaled = false
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                    inPreferredColorSpace = ColorSpace.get(ColorSpace.Named.SRGB)
                }
                BitmapFactory.decodeStream(stream, null, options)
            }
        }.getOrNull() ?: return null

        return applyExifOrientationAndEnsureArgb(rawBitmap, orientation)
    }

    private fun decodeBitmapFromFile(file: File, orientation: Int): Bitmap? {
        val rawBitmap = runCatching {
            val options = BitmapFactory.Options().apply {
                inScaled = false
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inPreferredColorSpace = ColorSpace.get(ColorSpace.Named.SRGB)
            }
            BitmapFactory.decodeFile(file.absolutePath, options)
        }.getOrNull() ?: return null

        return applyExifOrientationAndEnsureArgb(rawBitmap, orientation)
    }

    private fun applyExifOrientationAndEnsureArgb(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.preScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.preScale(-1f, 1f)
            }
        }

        val rotated = if (!matrix.isIdentity) {
            runCatching {
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            }.getOrDefault(bitmap)
        } else {
            bitmap
        }

        return if (rotated.config != Bitmap.Config.ARGB_8888) {
            rotated.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            rotated
        }
    }

    private fun isValidCoordinate(lat: Double, lon: Double): Boolean {
        if (lat.isNaN() || lon.isNaN()) return false
        if (abs(lat) < 0.0001 && abs(lon) < 0.0001) return false
        return lat in -90.0..90.0 && lon in -180.0..180.0
    }

    private fun parseExifDateToIso(exifDate: String): String? {
        return runCatching {
            // Formato estándar EXIF: "yyyy:MM:dd HH:mm:ss"
            val clean = exifDate.trim()
            val formatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss", Locale.US)
            val localDateTime = LocalDateTime.parse(clean, formatter)
            localDateTime.atZone(ZoneId.systemDefault()).toInstant().toString()
        }.getOrElse {
            exifDate.takeIf { it.isNotBlank() }
        }
    }

    private fun isoToExifDateString(isoString: String): String? {
        return runCatching {
            val instant = Instant.parse(isoString)
            val formatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss", Locale.US)
                .withZone(ZoneId.systemDefault())
            formatter.format(instant)
        }.getOrNull()
    }
}
