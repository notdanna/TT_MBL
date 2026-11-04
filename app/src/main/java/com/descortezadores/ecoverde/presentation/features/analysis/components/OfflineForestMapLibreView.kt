package com.descortezadores.ecoverde.presentation.features.analysis.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
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
import com.descortezadores.ecoverde.domain.model.TargetAssociation
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
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource

private const val SOURCE_HOTSPOTS = "ecoverde-hotspots-source"
private const val LAYER_HOTSPOTS = "ecoverde-hotspots-layer"

private const val SOURCE_POINTS = "ecoverde-points-source"
private const val LAYER_POINTS = "ecoverde-points-layer"

private const val SOURCE_FOCUSED_TREE = "ecoverde-focused-tree-source"
private const val LAYER_FOCUSED_TREE_ICON = "ecoverde-focused-tree-icon"
private const val LAYER_FOCUSED_TREE_LABEL = "ecoverde-focused-tree-label"

private const val SOURCE_USER = "ecoverde-user-source"
private const val LAYER_USER = "ecoverde-user-layer"

private const val ICON_USER_PERSON = "marker-user-person"
private const val ICON_TREE_GREEN = "marker-tree-green"

@Volatile
private var cachedPreparedStyleJson: String? = null

@Volatile
private var cachedPersonMarkerBitmap: Bitmap? = null

@Volatile
private var cachedGreenTreeMarkerBitmap: Bitmap? = null

// Coordenadas fijas para que el mapa siempre inicie en el Bosque de Chapultepec (1a a 4a Secc.)
private const val CHAPULTEPEC_LAT = 19.4195
private const val CHAPULTEPEC_LON = -99.1895
private const val CHAPULTEPEC_ZOOM = 14.0

/**
 * Vista de mapa vectorial Offline con MapLibre GL consumiendo `asset://style.json` y `cdmx.pmtiles`.
 *
 * Mejoras:
 * - Los árboles normales se renderizan sin etiqueta para evitar sobreposición visual.
 * - Solo el árbol seleccionado/marcado para ir al mapa muestra un icono de árbol verde y su título flotando.
 * - La ubicación del usuario se renderiza con un icono de persona y se oculta cuando se enfoca un árbol.
 */
@Composable
fun OfflineForestMapLibreView(
    currentLat: Double,
    currentLon: Double,
    showUserLocation: Boolean = true,
    focusedTree: TreeRecord? = null,
    targetCoordinates: Pair<Double, Double>? = null,
    trees: List<TreeRecord>,
    records: List<SavedDetectionRecord>,
    hotspots: List<InfestationFocus>,
    onMapCoordinateClicked: (Double, Double) -> Unit,
    onTreeSelected: (TreeRecord) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

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

    // Actualizar GeoJSON dinámico
    LaunchedEffect(styleReady, currentLat, currentLon, showUserLocation, focusedTree, trees, records, hotspots) {
        val map = mapInstance ?: return@LaunchedEffect
        val style = map.style ?: return@LaunchedEffect
        if (!styleReady) return@LaunchedEffect

        updateGeoJsonSources(
            style = style,
            currentLat = currentLat,
            currentLon = currentLon,
            showUserLocation = showUserLocation,
            focusedTree = focusedTree,
            trees = trees,
            records = records,
            hotspots = hotspots
        )

        if (!initialPositionCentered) {
            map.cameraPosition = CameraPosition.Builder()
                .target(LatLng(CHAPULTEPEC_LAT, CHAPULTEPEC_LON))
                .zoom(CHAPULTEPEC_ZOOM)
                .build()
            initialPositionCentered = true
        }
    }

    // Animar cámara cuando cambia targetCoordinates (al tocar "Ir al mapa" o "Mi GPS")
    LaunchedEffect(targetCoordinates) {
        val target = targetCoordinates ?: return@LaunchedEffect
        val map = mapInstance ?: return@LaunchedEffect
        if (!styleReady) return@LaunchedEffect
        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(target.first, target.second),
                16.0
            ),
            650
        )
    }

    // Cargar estilo offline y configurar capas con iconos vectoriales
    LaunchedEffect(mapInstance) {
        val map = mapInstance ?: return@LaunchedEffect
        if (styleReady) return@LaunchedEffect

        val styleJson = withContext(Dispatchers.IO) {
            prepareOfflineMapStyle(context)
        }

        map.setStyle(Style.Builder().fromJson(styleJson)) { loadedStyle ->
            setupMapLayers(
                context = context,
                style = loadedStyle,
                currentLat = currentLat,
                currentLon = currentLon,
                showUserLocation = showUserLocation,
                focusedTree = focusedTree,
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
                setOnTouchListener { v, _ ->
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                    false
                }
                getMapAsync { map ->
                    mapInstance = map
                    map.uiSettings.isAttributionEnabled = false
                    map.uiSettings.isLogoEnabled = false
                    map.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(CHAPULTEPEC_LAT, CHAPULTEPEC_LON))
                        .zoom(CHAPULTEPEC_ZOOM)
                        .build()

                    map.addOnMapClickListener { latLng ->
                        val pointF = map.projection.toScreenLocation(latLng)
                        val density = context.resources.displayMetrics.density
                        val touchBuffer = 28 * density
                        val rect = RectF(
                            pointF.x - touchBuffer,
                            pointF.y - touchBuffer,
                            pointF.x + touchBuffer,
                            pointF.y + touchBuffer
                        )
                        val features = map.queryRenderedFeatures(
                            rect,
                            LAYER_FOCUSED_TREE_ICON,
                            LAYER_FOCUSED_TREE_LABEL,
                            LAYER_POINTS
                        )
                        val clickedTreeId = features.firstOrNull()?.getStringProperty("id")
                        val clickedTreeTitle = features.firstOrNull()?.getStringProperty("title")
                        val clickedTree = trees.firstOrNull { it.idArbol == clickedTreeId }
                            ?: trees.firstOrNull { it.nombreEspecifico == clickedTreeTitle }

                        if (clickedTree != null) {
                            onTreeSelected(clickedTree)
                        } else {
                            onMapCoordinateClicked(latLng.latitude, latLng.longitude)
                        }
                        true
                    }
                }
            }
        }
    )
}

private fun setupMapLayers(
    context: Context,
    style: Style,
    currentLat: Double,
    currentLon: Double,
    showUserLocation: Boolean,
    focusedTree: TreeRecord?,
    trees: List<TreeRecord>,
    records: List<SavedDetectionRecord>,
    hotspots: List<InfestationFocus>
) {
    // 1. Registrar iconos en el estilo de MapLibre
    if (style.getImage(ICON_USER_PERSON) == null) {
        style.addImage(ICON_USER_PERSON, createPersonMarkerBitmap(context))
    }
    if (style.getImage(ICON_TREE_GREEN) == null) {
        style.addImage(ICON_TREE_GREEN, createGreenTreeMarkerBitmap(context))
    }

    // 2. Focos de Infestación Activos NOM-019 (círculos de afectación roja)
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

    // 3. Puntos de árboles y capturas estándar (Círculos sin etiquetas para evitar sobreposiciones)
    if (style.getSource(SOURCE_POINTS) == null) {
        style.addSource(GeoJsonSource(SOURCE_POINTS, buildTreesAndRecordsGeoJson(trees, records, focusedTree?.idArbol)))
        style.addLayer(
            CircleLayer(LAYER_POINTS, SOURCE_POINTS).withProperties(
                PropertyFactory.circleRadius(8.5f),
                PropertyFactory.circleColor(Expression.get("color")),
                PropertyFactory.circleStrokeWidth(2f),
                PropertyFactory.circleStrokeColor("#FFFFFF")
            )
        )
    }

    // 4. Árbol seleccionado/enfocado: Icono de Árbol Verde + Título destacado flotando encima
    if (style.getSource(SOURCE_FOCUSED_TREE) == null) {
        style.addSource(GeoJsonSource(SOURCE_FOCUSED_TREE, buildFocusedTreeGeoJson(focusedTree)))
        style.addLayer(
            SymbolLayer(LAYER_FOCUSED_TREE_ICON, SOURCE_FOCUSED_TREE).withProperties(
                PropertyFactory.iconImage(ICON_TREE_GREEN),
                PropertyFactory.iconSize(1.0f),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true),
                PropertyFactory.iconAnchor(Property.ICON_ANCHOR_CENTER)
            )
        )
        style.addLayer(
            SymbolLayer(LAYER_FOCUSED_TREE_LABEL, SOURCE_FOCUSED_TREE).withProperties(
                PropertyFactory.textField(Expression.get("title")),
                PropertyFactory.textFont(arrayOf("Noto Sans Bold")),
                PropertyFactory.textSize(12.5f),
                PropertyFactory.textColor("#0B2613"),
                PropertyFactory.textHaloColor("#FFFFFF"),
                PropertyFactory.textHaloWidth(2.5f),
                PropertyFactory.textAnchor(Property.TEXT_ANCHOR_BOTTOM),
                PropertyFactory.textOffset(arrayOf(0f, -1.8f)),
                PropertyFactory.textAllowOverlap(true),
                PropertyFactory.textIgnorePlacement(true)
            )
        )
    }

    // 5. Ubicación actual del usuario: Icono de Persona (se oculta cuando showUserLocation = false)
    if (style.getSource(SOURCE_USER) == null) {
        style.addSource(GeoJsonSource(SOURCE_USER, buildUserLocationGeoJson(currentLat, currentLon, showUserLocation)))
        style.addLayer(
            SymbolLayer(LAYER_USER, SOURCE_USER).withProperties(
                PropertyFactory.iconImage(ICON_USER_PERSON),
                PropertyFactory.iconSize(1.0f),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true),
                PropertyFactory.iconAnchor(Property.ICON_ANCHOR_CENTER)
            )
        )
    }
}

private fun updateGeoJsonSources(
    style: Style,
    currentLat: Double,
    currentLon: Double,
    showUserLocation: Boolean,
    focusedTree: TreeRecord?,
    trees: List<TreeRecord>,
    records: List<SavedDetectionRecord>,
    hotspots: List<InfestationFocus>
) {
    (style.getSourceAs<GeoJsonSource>(SOURCE_HOTSPOTS))?.setGeoJson(buildHotspotsGeoJson(hotspots))
    (style.getSourceAs<GeoJsonSource>(SOURCE_POINTS))?.setGeoJson(buildTreesAndRecordsGeoJson(trees, records, focusedTree?.idArbol))
    (style.getSourceAs<GeoJsonSource>(SOURCE_FOCUSED_TREE))?.setGeoJson(buildFocusedTreeGeoJson(focusedTree))
    (style.getSourceAs<GeoJsonSource>(SOURCE_USER))?.setGeoJson(buildUserLocationGeoJson(currentLat, currentLon, showUserLocation))
}

private fun buildUserLocationGeoJson(lat: Double, lon: Double, showUser: Boolean): String {
    val features = JSONArray()
    if (showUser) {
        features.put(createPointFeature(lat = lat, lon = lon, colorHex = "#0288D1", title = "Ubicación Actual"))
    }
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
                title = "Foco #${focus.idFoco.take(6)}",
                id = focus.idFoco
            )
        )
    }
    return JSONObject()
        .put("type", "FeatureCollection")
        .put("features", features)
        .toString()
}

private fun buildFocusedTreeGeoJson(focusedTree: TreeRecord?): String {
    val features = JSONArray()
    if (focusedTree != null && focusedTree.latitud != null && focusedTree.longitud != null) {
        features.put(
            createPointFeature(
                lat = focusedTree.latitud,
                lon = focusedTree.longitud,
                colorHex = "#2E7D32",
                title = focusedTree.nombreEspecifico,
                id = focusedTree.idArbol
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
    records: List<SavedDetectionRecord>,
    focusedTreeId: String? = null
): String {
    val features = JSONArray()

    trees.forEach { tree ->
        // Si es el árbol enfocado, no se incluye aquí para evitar duplicación con el icono de árbol verde
        if (tree.idArbol == focusedTreeId) return@forEach
        val lat = tree.latitud ?: return@forEach
        val lon = tree.longitud ?: return@forEach
        val color = when (tree.ultimoEstadoSanitario) {
            ClassificationOutcome.INFESTADO -> "#C62828"
            ClassificationOutcome.SANO -> "#2E7D32"
            else -> "#EF6C00"
        }
        features.put(
            createPointFeature(
                lat = lat,
                lon = lon,
                colorHex = color,
                title = tree.nombreEspecifico,
                id = tree.idArbol
            )
        )
    }

    val existingTreeIds = trees.map { it.idArbol }.toSet()
    records.forEach { rec ->
        val treeId = when (val target = rec.targetAssociation) {
            is TargetAssociation.CortezaTarget.ExistingTree -> target.idArbol
            else -> null
        }
        if (treeId == null || !existingTreeIds.contains(treeId)) {
            val color = when (rec.classificationOutcome) {
                ClassificationOutcome.INFESTADO -> "#C62828"
                ClassificationOutcome.SANO -> "#2E7D32"
                ClassificationOutcome.NO_DETERMINADO -> "#FFA000"
            }
            features.put(
                createPointFeature(
                    lat = rec.latitud,
                    lon = rec.longitud,
                    colorHex = color,
                    title = "",
                    id = rec.idAnalisis
                )
            )
        }
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
    title: String,
    id: String = ""
): JSONObject {
    val coords = JSONArray().put(lon).put(lat)
    val geometry = JSONObject()
        .put("type", "Point")
        .put("coordinates", coords)
    val properties = JSONObject()
        .put("color", colorHex)
        .put("title", title)
    if (id.isNotBlank()) {
        properties.put("id", id)
    }
    return JSONObject()
        .put("type", "Feature")
        .put("geometry", geometry)
        .put("properties", properties)
}

/**
 * Genera un Bitmap con icono de persona (silueta con cabeza y torso) sobre círculo blanco.
 */
private fun createPersonMarkerBitmap(context: Context): Bitmap {
    cachedPersonMarkerBitmap?.let { return it }
    val density = context.resources.displayMetrics.density
    val size = (38 * density).toInt().coerceAtLeast(32)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = size / 2f
    val cy = size / 2f

    // 1. Sombra suave
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#40000000")
    }
    canvas.drawCircle(cx, cy + 1.5f * density, 16.5f * density, shadowPaint)

    // 2. Fondo circular blanco
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }
    canvas.drawCircle(cx, cy, 15.5f * density, bgPaint)

    // 3. Anillo perimetral azul
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * density
        color = Color.parseColor("#0288D1")
    }
    canvas.drawCircle(cx, cy, 14.5f * density, strokePaint)

    // 4. Silueta de la persona (Cabeza + Hombros)
    val personPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#0288D1")
    }
    // Cabeza
    canvas.drawCircle(cx, cy - 4.5f * density, 4.5f * density, personPaint)

    // Hombros / torso
    val torsoPath = Path().apply {
        moveTo(cx - 7.5f * density, cy + 9.5f * density)
        cubicTo(
            cx - 7.5f * density, cy + 2.5f * density,
            cx + 7.5f * density, cy + 2.5f * density,
            cx + 7.5f * density, cy + 9.5f * density
        )
        close()
    }
    canvas.drawPath(torsoPath, personPaint)

    cachedPersonMarkerBitmap = bitmap
    return bitmap
}

/**
 * Genera un Bitmap con icono de árbol verde estilizado (silueta de pino sobre fondo circular blanco con anillo verde).
 */
private fun createGreenTreeMarkerBitmap(context: Context): Bitmap {
    cachedGreenTreeMarkerBitmap?.let { return it }
    val density = context.resources.displayMetrics.density
    val size = (44 * density).toInt().coerceAtLeast(36)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = size / 2f
    val cy = size / 2f

    // 1. Sombra exterior
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#44000000")
    }
    canvas.drawCircle(cx, cy + 2f * density, 19f * density, shadowPaint)

    // 2. Fondo circular blanco
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }
    canvas.drawCircle(cx, cy, 18f * density, bgPaint)

    // 3. Anillo verde bosque exterior
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * density
        color = Color.parseColor("#2E7D32")
    }
    canvas.drawCircle(cx, cy, 17f * density, strokePaint)

    // 4. Silueta de Árbol / Pino en verde vivo
    val treePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#2E7D32")
    }

    // Copa superior
    val topPath = Path().apply {
        moveTo(cx, cy - 10f * density)
        lineTo(cx + 7.5f * density, cy - 2.5f * density)
        lineTo(cx - 7.5f * density, cy - 2.5f * density)
        close()
    }
    canvas.drawPath(topPath, treePaint)

    // Copa inferior
    val bottomPath = Path().apply {
        moveTo(cx, cy - 4f * density)
        lineTo(cx + 10.5f * density, cy + 5.5f * density)
        lineTo(cx - 10.5f * density, cy + 5.5f * density)
        close()
    }
    canvas.drawPath(bottomPath, treePaint)

    // Tronco marrón
    val trunkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#6D4C41")
    }
    canvas.drawRect(
        cx - 2f * density,
        cy + 5.5f * density,
        cx + 2f * density,
        cy + 10.5f * density,
        trunkPaint
    )

    cachedGreenTreeMarkerBitmap = bitmap
    return bitmap
}

/**
 * Garantiza que `cdmx.pmtiles` resida en el almacenamiento interno (`filesDir`) para permitir
 * lecturas por rangos de bytes (POSIX pread) por parte de MapLibre Native.
 */
private fun prepareOfflineMapStyle(context: Context): String {
    cachedPreparedStyleJson?.let { return it }
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

    val glyphsDir = File(context.filesDir, "glyphs")
    copyAssetDirRecursively(context, "glyphs", glyphsDir)

    val processedStyleJson = rawStyleJson
        .replace(
            "pmtiles://asset://cdmx.pmtiles",
            "pmtiles://file://${targetFile.absolutePath}"
        )
        .replace(
            "asset://glyphs/{fontstack}/{range}.pbf",
            "file://${glyphsDir.absolutePath}/{fontstack}/{range}.pbf"
        )
    cachedPreparedStyleJson = processedStyleJson
    return processedStyleJson
}

private fun copyAssetDirRecursively(context: Context, assetPath: String, targetDir: File) {
    val list = context.assets.list(assetPath) ?: return
    if (!targetDir.exists()) {
        targetDir.mkdirs()
    }
    for (item in list) {
        val subAsset = if (assetPath.isEmpty()) item else "$assetPath/$item"
        val subTarget = File(targetDir, item)
        val children = context.assets.list(subAsset)
        if (!children.isNullOrEmpty()) {
            copyAssetDirRecursively(context, subAsset, subTarget)
        } else {
            if (!subTarget.exists() || subTarget.length() == 0L) {
                try {
                    context.assets.open(subAsset).use { input ->
                        FileOutputStream(subTarget).use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }
}
