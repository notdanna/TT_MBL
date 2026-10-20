package com.descortezadores.ecoverde.presentation.features.analysis.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.descortezadores.ecoverde.domain.model.ClassificationOutcome
import com.descortezadores.ecoverde.domain.model.InfestationFocus
import com.descortezadores.ecoverde.domain.model.TreeRecord
import com.descortezadores.ecoverde.domain.repository.SavedDetectionRecord
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource

private const val SOURCE_HOTSPOTS = "ecoverde-hotspots-source"
private const val LAYER_HOTSPOTS = "ecoverde-hotspots-layer"

private const val SOURCE_POINTS = "ecoverde-points-source"
private const val LAYER_POINTS = "ecoverde-points-layer"

private const val SOURCE_USER = "ecoverde-user-source"
private const val LAYER_USER = "ecoverde-user-layer"

// Coordenadas fijas para que el mapa siempre inicie en el Bosque de Chapultepec (1a a 4a Secc.)
private const val CHAPULTEPEC_LAT = 19.4195
private const val CHAPULTEPEC_LON = -99.1895
private const val CHAPULTEPEC_ZOOM = 14.0

/**
 * Vista de mapa vectorial Offline con MapLibre GL consumiendo `asset://style.json` y `cdmx.pmtiles`.
 * Renderiza en sus coordenadas reales:
 * - La ubicación exacta de la captura / brigadista (`currentLat`, `currentLon`).
 * - Los árboles y análisis registrados (`trees`, `records`) coloreados por estado sanitario.
 * - Los Focos de Infestación Activos NOM-019 (`hotspots`).
 */
@Composable
fun OfflineForestMapLibreView(
    currentLat: Double,
    currentLon: Double,
    trees: List<TreeRecord>,
    records: List<SavedDetectionRecord>,
    hotspots: List<InfestationFocus>,
    onMapCoordinateClicked: (Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Inicialización previa de MapLibre en el contexto antes de instanciar MapView
    remember(context) {
        MapLibre.getInstance(context)
    }

    val mapView = remember(context) {
        MapView(context).apply {
            onCreate(null)
        }
    }

    var mapInstance by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleReady by remember { mutableStateOf(false) }
    var initialPositionCentered by remember { mutableStateOf(false) }
    var lastInteractiveLat by remember { mutableStateOf<Double?>(null) }
    var lastInteractiveLon by remember { mutableStateOf<Double?>(null) }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    // Actualizar GeoJSON y posición de cámara. El mapa siempre inicia en Bosque de Chapultepec.
    LaunchedEffect(styleReady, currentLat, currentLon, trees, records, hotspots) {
        val map = mapInstance ?: return@LaunchedEffect
        val style = map.style ?: return@LaunchedEffect
        if (!styleReady) return@LaunchedEffect

        updateGeoJsonSources(
            style = style,
            currentLat = currentLat,
            currentLon = currentLon,
            trees = trees,
            records = records,
            hotspots = hotspots
        )

        if (!initialPositionCentered) {
            // Posicionamiento inicial forzoso en Bosque de Chapultepec
            map.cameraPosition = CameraPosition.Builder()
                .target(LatLng(CHAPULTEPEC_LAT, CHAPULTEPEC_LON))
                .zoom(CHAPULTEPEC_ZOOM)
                .build()
            initialPositionCentered = true
            lastInteractiveLat = currentLat
            lastInteractiveLon = currentLon
        } else if (lastInteractiveLat != currentLat || lastInteractiveLon != currentLon) {
            // Animar solo si el usuario interactúa o cambia deliberadamente de ubicación
            lastInteractiveLat = currentLat
            lastInteractiveLon = currentLon
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(currentLat, currentLon),
                    15.2
                ),
                650
            )
        }
    }

    // Cargar el estilo offline garantizando acceso POSIX pread a cdmx.pmtiles
    LaunchedEffect(mapInstance) {
        val map = mapInstance ?: return@LaunchedEffect
        if (styleReady) return@LaunchedEffect

        val styleJson = withContext(Dispatchers.IO) {
            prepareOfflineMapStyle(context)
        }

        map.setStyle(Style.Builder().fromJson(styleJson)) { loadedStyle ->
            setupMapLayers(
                style = loadedStyle,
                currentLat = currentLat,
                currentLon = currentLon,
                trees = trees,
                records = records,
                hotspots = hotspots
            )
            styleReady = true
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            mapView.apply {
                // Evitar que el scroll vertical padre intercepte el paneo del mapa al tocarlo
                setOnTouchListener { v, _ ->
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                    false
                }
                getMapAsync { map ->
                    mapInstance = map
                    map.uiSettings.isAttributionEnabled = false
                    map.uiSettings.isLogoEnabled = false
                    // Vista inicial centrada en el Bosque de Chapultepec
                    map.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(CHAPULTEPEC_LAT, CHAPULTEPEC_LON))
                        .zoom(CHAPULTEPEC_ZOOM)
                        .build()

                    map.addOnMapClickListener { latLng ->
                        onMapCoordinateClicked(latLng.latitude, latLng.longitude)
                        true
                    }
                }
            }
        }
    )
}

private fun setupMapLayers(
    style: Style,
    currentLat: Double,
    currentLon: Double,
    trees: List<TreeRecord>,
    records: List<SavedDetectionRecord>,
    hotspots: List<InfestationFocus>
) {
    if (style.getSource(SOURCE_HOTSPOTS) == null) {
        style.addSource(GeoJsonSource(SOURCE_HOTSPOTS, buildHotspotsGeoJson(hotspots)))
        style.addLayer(
            CircleLayer(LAYER_HOTSPOTS, SOURCE_HOTSPOTS).withProperties(
                PropertyFactory.circleRadius(26f),
                PropertyFactory.circleColor("#C62828"),
                PropertyFactory.circleOpacity(0.28f),
                PropertyFactory.circleStrokeWidth(2.5f),
                PropertyFactory.circleStrokeColor("#EF5350")
            )
        )
    }

    if (style.getSource(SOURCE_POINTS) == null) {
        style.addSource(GeoJsonSource(SOURCE_POINTS, buildTreesAndRecordsGeoJson(trees, records)))
        style.addLayer(
            CircleLayer(LAYER_POINTS, SOURCE_POINTS).withProperties(
                PropertyFactory.circleRadius(8.5f),
                PropertyFactory.circleColor(Expression.get("color")),
                PropertyFactory.circleStrokeWidth(2f),
                PropertyFactory.circleStrokeColor("#FFFFFF")
            )
        )
    }

    if (style.getSource(SOURCE_USER) == null) {
        style.addSource(GeoJsonSource(SOURCE_USER, buildUserLocationGeoJson(currentLat, currentLon)))
        style.addLayer(
            CircleLayer(LAYER_USER, SOURCE_USER).withProperties(
                PropertyFactory.circleRadius(9.5f),
                PropertyFactory.circleColor("#0288D1"),
                PropertyFactory.circleStrokeWidth(3f),
                PropertyFactory.circleStrokeColor("#FFFFFF")
            )
        )
    }
}

private fun updateGeoJsonSources(
    style: Style,
    currentLat: Double,
    currentLon: Double,
    trees: List<TreeRecord>,
    records: List<SavedDetectionRecord>,
    hotspots: List<InfestationFocus>
) {
    (style.getSourceAs<GeoJsonSource>(SOURCE_HOTSPOTS))?.setGeoJson(buildHotspotsGeoJson(hotspots))
    (style.getSourceAs<GeoJsonSource>(SOURCE_POINTS))?.setGeoJson(buildTreesAndRecordsGeoJson(trees, records))
    (style.getSourceAs<GeoJsonSource>(SOURCE_USER))?.setGeoJson(buildUserLocationGeoJson(currentLat, currentLon))
}

private fun buildUserLocationGeoJson(lat: Double, lon: Double): String {
    val features = JSONArray()
    features.put(createPointFeature(lat = lat, lon = lon, colorHex = "#0288D1", title = "Ubicación Actual"))
    return JSONObject()
        .put("type", "FeatureCollection")
        .put("features", features)
        .toString()
}

private fun buildHotspotsGeoJson(hotspots: List<InfestationFocus>): String {
    val features = JSONArray()
    hotspots.forEach { focus ->
        features.put(
            createPointFeature(
                lat = focus.latitudCentro,
                lon = focus.longitudCentro,
                colorHex = "#C62828",
                title = "Foco #${focus.idFoco.take(6)}"
            )
        )
    }
    return JSONObject()
        .put("type", "FeatureCollection")
        .put("features", features)
        .toString()
}

private fun buildTreesAndRecordsGeoJson(
    trees: List<TreeRecord>,
    records: List<SavedDetectionRecord>
): String {
    val features = JSONArray()

    trees.forEach { tree ->
        val lat = tree.latitud ?: return@forEach
        val lon = tree.longitud ?: return@forEach
        val color = when (tree.ultimoEstadoSanitario) {
            ClassificationOutcome.INFESTADO -> "#C62828"
            ClassificationOutcome.SANO -> "#2E7D32"
            else -> "#EF6C00"
        }
        features.put(createPointFeature(lat = lat, lon = lon, colorHex = color, title = tree.nombreEspecifico))
    }

    records.forEach { rec ->
        val color = when (rec.classificationOutcome) {
            ClassificationOutcome.INFESTADO -> "#C62828"
            ClassificationOutcome.SANO -> "#2E7D32"
            ClassificationOutcome.NO_DETERMINADO -> "#FFA000"
        }
        features.put(createPointFeature(lat = rec.latitud, lon = rec.longitud, colorHex = color, title = rec.idAnalisis.take(6)))
    }

    return JSONObject()
        .put("type", "FeatureCollection")
        .put("features", features)
        .toString()
}

private fun createPointFeature(
    lat: Double,
    lon: Double,
    colorHex: String,
    title: String
): JSONObject {
    val coords = JSONArray().put(lon).put(lat)
    val geometry = JSONObject()
        .put("type", "Point")
        .put("coordinates", coords)
    val properties = JSONObject()
        .put("color", colorHex)
        .put("title", title)
    return JSONObject()
        .put("type", "Feature")
        .put("geometry", geometry)
        .put("properties", properties)
}

/**
 * Garantiza que `cdmx.pmtiles` resida en el almacenamiento interno (`filesDir`) para permitir
 * lecturas por rangos de bytes (POSIX pread) por parte de MapLibre Native, resolviendo el bug
 * de falta de soporte para byte-ranges en AssetManagerFileSource (Issue #4360).
 */
private fun prepareOfflineMapStyle(context: Context): String {
    val targetFile = File(context.filesDir, "cdmx.pmtiles")

    val assetFd = try {
        context.assets.openFd("cdmx.pmtiles")
    } catch (_: Exception) {
        null
    }
    val expectedLength = assetFd?.length ?: -1L
    assetFd?.close()

    val needsCopy = !targetFile.exists() || targetFile.length() == 0L ||
        (expectedLength > 0 && targetFile.length() != expectedLength)

    if (needsCopy) {
        val tempFile = File(context.filesDir, "cdmx.pmtiles.tmp")
        context.assets.open("cdmx.pmtiles").use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        if (tempFile.exists()) {
            if (targetFile.exists()) {
                targetFile.delete()
            }
            tempFile.renameTo(targetFile)
        }
    }

    val rawStyleJson = context.assets.open("style.json").bufferedReader().use { it.readText() }

    // Copiar glifos al almacenamiento interno para soporte offline completo de etiquetas de texto
    val glyphsDir = File(context.filesDir, "glyphs")
    copyAssetDirRecursively(context, "glyphs", glyphsDir)

    return rawStyleJson
        .replace(
            "pmtiles://asset://cdmx.pmtiles",
            "pmtiles://file://${targetFile.absolutePath}"
        )
        .replace(
            "asset://glyphs/{fontstack}/{range}.pbf",
            "file://${glyphsDir.absolutePath}/{fontstack}/{range}.pbf"
        )
}

private fun copyAssetDirRecursively(context: Context, assetPath: String, targetDir: File) {
    try {
        val list = context.assets.list(assetPath) ?: return
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }
        for (item in list) {
            val subAssetPath = if (assetPath.isEmpty()) item else "$assetPath/$item"
            val subTarget = File(targetDir, item)
            val subList = context.assets.list(subAssetPath)
            if (subList != null && subList.isNotEmpty()) {
                copyAssetDirRecursively(context, subAssetPath, subTarget)
            } else {
                if (!subTarget.exists() || subTarget.length() == 0L) {
                    context.assets.open(subAssetPath).use { input ->
                        FileOutputStream(subTarget).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
    } catch (_: Exception) {}
}

