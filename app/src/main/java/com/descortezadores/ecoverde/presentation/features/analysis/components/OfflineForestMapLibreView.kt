package com.descortezadores.ecoverde.presentation.features.analysis.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.view.View
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import org.maplibre.android.maps.MapLibreMapOptions
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
private const val ICON_TREE_RED = "marker-tree-red"
private const val ICON_TREE_AMBER = "marker-tree-amber"

private const val UNSELECTED_TREE_ICON_SCALE = 0.72f
private const val FOCUSED_TREE_ICON_SCALE = 1.32f

private const val SAME_LOCATION_THRESHOLD_DEG = 0.00005 // ~5 metros
private const val SPREAD_LON_STEP_DEG = 0.00034 // Separación horizontal entre iconos de árbol en el mismo punto
private const val SPREAD_LAT_STEP_DEG = 0.00030 // Separación vertical si hay más de 5 en el mismo punto

@Volatile
private var cachedPreparedStyleJson: String? = null

@Volatile
private var cachedPersonMarkerBitmap: Bitmap? = null

@Volatile
private var cachedTreeMarkerBitmaps = mutableMapOf<String, Bitmap>()

// Coordenadas fijas para que el mapa siempre inicie en el Bosque de Chapultepec (1a a 4a Secc.)
private const val CHAPULTEPEC_LAT = 19.4195
private const val CHAPULTEPEC_LON = -99.1895
private const val CHAPULTEPEC_ZOOM = 14.0

/**
 * Pre-calienta en segundo plano el estilo offline y los archivos `cdmx.pmtiles`/`glyphs`
 * para que la primera apertura de la pestaña de Mapa sea instantánea.
 */
suspend fun prewarmOfflineMapStyle(context: Context) {
    withContext(Dispatchers.IO) {
        runCatching { prepareOfflineMapStyle(context.applicationContext) }
    }
}

/**
 * Vista de mapa vectorial Offline con MapLibre GL consumiendo `asset://style.json` y `cdmx.pmtiles`.
 *
 * Mejoras:
 * - Usa `textureMode(true)` para participar de forma fluida en transiciones Compose a 120 Hz.
 * - Soporta retención en memoria mediante [isVisible] sin destruir ni recargar el mapa al cambiar de tab.
 * - Calcula GeoJSON y clustering de pines en `Dispatchers.Default` fuera del hilo principal.
 */
@Composable
fun OfflineForestMapLibreView(
    currentLat: Double,
    currentLon: Double,
    showUserLocation: Boolean = true,
    isVisible: Boolean = true,
    focusedTree: TreeRecord? = null,
    targetCoordinates: Pair<Double, Double>? = null,
    mapCameraNonce: Long = 0L,
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
        val options = MapLibreMapOptions.createFromAttributes(context)
            .textureMode(true)
        MapView(context, options).apply {
            onCreate(null)
        }
    }

    var mapInstance by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleReady by remember { mutableStateOf(false) }
    var initialPositionCentered by remember { mutableStateOf(false) }

    // Animación spring 120Hz para destacar el árbol seleccionado mucho más grande que los demás
    var targetFocusedScale by remember { mutableStateOf(UNSELECTED_TREE_ICON_SCALE) }
    LaunchedEffect(focusedTree?.idArbol) {
        if (focusedTree != null) {
            targetFocusedScale = UNSELECTED_TREE_ICON_SCALE
            kotlinx.coroutines.yield()
            targetFocusedScale = FOCUSED_TREE_ICON_SCALE
        } else {
            targetFocusedScale = UNSELECTED_TREE_ICON_SCALE
        }
    }
    val animatedFocusedScale by animateFloatAsState(
        targetValue = targetFocusedScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "focusedTreeScale"
    )
    LaunchedEffect(animatedFocusedScale, styleReady, isVisible) {
        if (!styleReady || !isVisible) return@LaunchedEffect
        mapInstance?.style?.getLayerAs<SymbolLayer>(LAYER_FOCUSED_TREE_ICON)
            ?.setProperties(PropertyFactory.iconSize(animatedFocusedScale))
    }

    DisposableEffect(lifecycleOwner, mapView) {
        var isStarted = false
        var isResumed = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    if (!isStarted) {
                        mapView.onStart()
                        isStarted = true
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (isVisible && !isResumed) {
                        mapView.onResume()
                        isResumed = true
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    if (isResumed) {
                        mapView.onPause()
                        isResumed = false
                    }
                }
                Lifecycle.Event.ON_STOP -> {
                    if (isStarted) {
                        mapView.onStop()
                        isStarted = false
                    }
                }
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (isResumed) mapView.onPause()
            if (isStarted) mapView.onStop()
            mapView.onDestroy()
        }
    }

    // Pausar el render loop OpenGL cuando la pestaña Mapa no está visible, manteniendo la instancia viva
    LaunchedEffect(isVisible, mapView, lifecycleOwner) {
        mapView.visibility = if (isVisible) View.VISIBLE else View.GONE
        val currentState = lifecycleOwner.lifecycle.currentState
        if (isVisible) {
            if (currentState.isAtLeast(Lifecycle.State.STARTED)) {
                mapView.onStart()
            }
            if (currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                mapView.onResume()
            }
        } else {
            mapView.onPause()
        }
    }

    // Actualizar solo el marcador del usuario ante ticks frecuentes del GPS (en Dispatchers.Default)
    LaunchedEffect(styleReady, currentLat, currentLon, showUserLocation) {
        val map = mapInstance ?: return@LaunchedEffect
        val style = map.style ?: return@LaunchedEffect
        if (!styleReady) return@LaunchedEffect

        val userGeoJson = withContext(Dispatchers.Default) {
            buildUserLocationGeoJson(currentLat, currentLon, showUserLocation)
        }
        style.getSourceAs<GeoJsonSource>(SOURCE_USER)?.setGeoJson(userGeoJson)
    }

    // Actualizar focos NOM-019 solo cuando cambia la lista de focos
    LaunchedEffect(styleReady, hotspots) {
        val map = mapInstance ?: return@LaunchedEffect
        val style = map.style ?: return@LaunchedEffect
        if (!styleReady) return@LaunchedEffect

        val hotspotsGeoJson = withContext(Dispatchers.Default) {
            buildHotspotsGeoJson(hotspots)
        }
        style.getSourceAs<GeoJsonSource>(SOURCE_HOTSPOTS)?.setGeoJson(hotspotsGeoJson)
    }

    // Actualizar árboles y árbol enfocado en hilo de cómputo (Dispatchers.Default)
    LaunchedEffect(styleReady, focusedTree, trees, records) {
        val map = mapInstance ?: return@LaunchedEffect
        val style = map.style ?: return@LaunchedEffect
        if (!styleReady) return@LaunchedEffect

        val (pointsGeoJson, focusedGeoJson) = withContext(Dispatchers.Default) {
            val spreadCoords = computeSpreadMapCoordinates(trees, records)
            val pts = buildTreesAndRecordsGeoJson(trees, records, focusedTree?.idArbol, spreadCoords)
            val foc = buildFocusedTreeGeoJson(focusedTree, spreadCoords)
            pts to foc
        }
        style.getSourceAs<GeoJsonSource>(SOURCE_POINTS)?.setGeoJson(pointsGeoJson)
        style.getSourceAs<GeoJsonSource>(SOURCE_FOCUSED_TREE)?.setGeoJson(focusedGeoJson)

        if (!initialPositionCentered) {
            val initTarget = targetCoordinates
            map.cameraPosition = CameraPosition.Builder()
                .target(
                    LatLng(
                        initTarget?.first ?: CHAPULTEPEC_LAT,
                        initTarget?.second ?: CHAPULTEPEC_LON
                    )
                )
                .zoom(if (initTarget != null) 16.0 else CHAPULTEPEC_ZOOM)
                .build()
            initialPositionCentered = true
        }
    }

    // Animar cámara cuando cambia targetCoordinates o mapCameraNonce al estar visible el mapa
    LaunchedEffect(targetCoordinates, mapCameraNonce, styleReady, isVisible) {
        val target = targetCoordinates ?: return@LaunchedEffect
        val map = mapInstance ?: return@LaunchedEffect
        if (!styleReady || !isVisible) return@LaunchedEffect
        kotlinx.coroutines.delay(50)
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

        val initialData = withContext(Dispatchers.Default) {
            val spreadCoords = computeSpreadMapCoordinates(trees, records)
            InitialMapGeoJson(
                hotspots = buildHotspotsGeoJson(hotspots),
                points = buildTreesAndRecordsGeoJson(trees, records, focusedTree?.idArbol, spreadCoords),
                focused = buildFocusedTreeGeoJson(focusedTree, spreadCoords),
                user = buildUserLocationGeoJson(currentLat, currentLon, showUserLocation)
            )
        }

        map.setStyle(Style.Builder().fromJson(styleJson)) { loadedStyle ->
            setupMapLayers(
                context = context,
                style = loadedStyle,
                initialGeoJson = initialData
            )
            styleReady = true
        }
    }

    val latestTrees by rememberUpdatedState(trees)
    val latestOnTreeSelected by rememberUpdatedState(onTreeSelected)
    val latestOnMapCoordinateClicked by rememberUpdatedState(onMapCoordinateClicked)

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
                        val currentTrees = latestTrees
                        val clickedTree = currentTrees.firstOrNull { it.idArbol == clickedTreeId }
                            ?: currentTrees.firstOrNull { it.nombreEspecifico == clickedTreeTitle }

                        if (clickedTree != null) {
                            latestOnTreeSelected(clickedTree)
                        } else {
                            latestOnMapCoordinateClicked(latLng.latitude, latLng.longitude)
                        }
                        true
                    }
                }
            }
        }
    )
}

private data class InitialMapGeoJson(
    val hotspots: String,
    val points: String,
    val focused: String,
    val user: String
)

private fun setupMapLayers(
    context: Context,
    style: Style,
    initialGeoJson: InitialMapGeoJson
) {
    // 1. Registrar iconos en el estilo de MapLibre
    if (style.getImage(ICON_USER_PERSON) == null) {
        style.addImage(ICON_USER_PERSON, createPersonMarkerBitmap(context))
    }
    if (style.getImage(ICON_TREE_GREEN) == null) {
        style.addImage(ICON_TREE_GREEN, createTreeMarkerBitmap(context, "#2E7D32"))
    }
    if (style.getImage(ICON_TREE_RED) == null) {
        style.addImage(ICON_TREE_RED, createTreeMarkerBitmap(context, "#C62828"))
    }
    if (style.getImage(ICON_TREE_AMBER) == null) {
        style.addImage(ICON_TREE_AMBER, createTreeMarkerBitmap(context, "#EF6C00"))
    }

    // 2. Focos de Infestación Activos NOM-019 (círculos de afectación roja)
    if (style.getSource(SOURCE_HOTSPOTS) == null) {
        style.addSource(GeoJsonSource(SOURCE_HOTSPOTS, initialGeoJson.hotspots))
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

    // 3. Árboles y capturas estándar: todos usan el icono con forma de árbol (tamaño compacto)
    if (style.getSource(SOURCE_POINTS) == null) {
        style.addSource(GeoJsonSource(SOURCE_POINTS, initialGeoJson.points))
        style.addLayer(
            SymbolLayer(LAYER_POINTS, SOURCE_POINTS).withProperties(
                PropertyFactory.iconImage(Expression.get("icon")),
                PropertyFactory.iconSize(UNSELECTED_TREE_ICON_SCALE),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true),
                PropertyFactory.iconAnchor(Property.ICON_ANCHOR_CENTER)
            )
        )
    }

    // 4. Árbol seleccionado/enfocado: Icono de Árbol animado más grande + Título destacado flotando encima
    if (style.getSource(SOURCE_FOCUSED_TREE) == null) {
        style.addSource(GeoJsonSource(SOURCE_FOCUSED_TREE, initialGeoJson.focused))
        style.addLayer(
            SymbolLayer(LAYER_FOCUSED_TREE_ICON, SOURCE_FOCUSED_TREE).withProperties(
                PropertyFactory.iconImage(Expression.get("icon")),
                PropertyFactory.iconSize(FOCUSED_TREE_ICON_SCALE),
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
                PropertyFactory.textColor(Expression.get("color")),
                PropertyFactory.textHaloColor("#FFFFFF"),
                PropertyFactory.textHaloWidth(2.5f),
                PropertyFactory.textAnchor(Property.TEXT_ANCHOR_BOTTOM),
                PropertyFactory.textOffset(arrayOf(0f, -2.35f)),
                PropertyFactory.textAllowOverlap(true),
                PropertyFactory.textIgnorePlacement(true)
            )
        )
    }

    // 5. Ubicación actual del usuario: Icono de Persona (se oculta cuando showUserLocation = false)
    if (style.getSource(SOURCE_USER) == null) {
        style.addSource(GeoJsonSource(SOURCE_USER, initialGeoJson.user))
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

private fun buildFocusedTreeGeoJson(
    focusedTree: TreeRecord?,
    spreadCoords: Map<String, Pair<Double, Double>> = emptyMap()
): String {
    val features = JSONArray()
    if (focusedTree != null && focusedTree.latitud != null && focusedTree.longitud != null) {
        val (displayLat, displayLon) = spreadCoords["tree:${focusedTree.idArbol}"]
            ?: (focusedTree.latitud to focusedTree.longitud)
        val isInfested = focusedTree.ultimoEstadoSanitario == ClassificationOutcome.INFESTADO
        features.put(
            createPointFeature(
                lat = displayLat,
                lon = displayLon,
                colorHex = if (isInfested) "#C62828" else "#0B2613",
                title = focusedTree.nombreEspecifico,
                id = focusedTree.idArbol,
                icon = if (isInfested) ICON_TREE_RED else ICON_TREE_GREEN
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
    focusedTreeId: String? = null,
    spreadCoords: Map<String, Pair<Double, Double>> = emptyMap()
): String {
    val features = JSONArray()

    trees.forEach { tree ->
        // Si es el árbol enfocado, no se incluye aquí para evitar duplicación con la capa animada
        if (tree.idArbol == focusedTreeId) return@forEach
        val lat = tree.latitud ?: return@forEach
        val lon = tree.longitud ?: return@forEach
        val (displayLat, displayLon) = spreadCoords["tree:${tree.idArbol}"] ?: (lat to lon)
        val (color, icon) = when (tree.ultimoEstadoSanitario) {
            ClassificationOutcome.INFESTADO -> "#C62828" to ICON_TREE_RED
            ClassificationOutcome.SANO -> "#2E7D32" to ICON_TREE_GREEN
            else -> "#EF6C00" to ICON_TREE_AMBER
        }
        features.put(
            createPointFeature(
                lat = displayLat,
                lon = displayLon,
                colorHex = color,
                title = tree.nombreEspecifico,
                id = tree.idArbol,
                icon = icon
            )
        )
    }

    return JSONObject()
        .put("type", "FeatureCollection")
        .put("features", features)
        .toString()
}

/**
 * Agrupa pines que comparten la misma ubicación (~5 m) y los distribuye uno al lado del otro
 * para evitar que queden encimados en el mapa.
 */
private fun computeSpreadMapCoordinates(
    trees: List<TreeRecord>,
    records: List<SavedDetectionRecord>
): Map<String, Pair<Double, Double>> {
    data class RawPin(val key: String, val lat: Double, val lon: Double)

    val rawPins = mutableListOf<RawPin>()
    trees.forEach { tree ->
        val lat = tree.latitud ?: return@forEach
        val lon = tree.longitud ?: return@forEach
        rawPins.add(RawPin(key = "tree:${tree.idArbol}", lat = lat, lon = lon))
    }

    val clusters = mutableListOf<MutableList<RawPin>>()
    rawPins.forEach { pin ->
        val matchingCluster = clusters.firstOrNull { cluster ->
            val anchor = cluster.first()
            kotlin.math.abs(pin.lat - anchor.lat) <= SAME_LOCATION_THRESHOLD_DEG &&
                kotlin.math.abs(pin.lon - anchor.lon) <= SAME_LOCATION_THRESHOLD_DEG
        }
        if (matchingCluster != null) {
            matchingCluster.add(pin)
        } else {
            clusters.add(mutableListOf(pin))
        }
    }

    val result = HashMap<String, Pair<Double, Double>>(rawPins.size)
    val maxPerRow = 5
    clusters.forEach { cluster ->
        if (cluster.size == 1) {
            val single = cluster.first()
            result[single.key] = single.lat to single.lon
        } else {
            val sorted = cluster.sortedBy { it.key }
            val baseLat = sorted.map { it.lat }.average()
            val baseLon = sorted.map { it.lon }.average()
            val totalRows = (sorted.size + maxPerRow - 1) / maxPerRow
            sorted.forEachIndexed { index, pin ->
                val row = index / maxPerRow
                val col = index % maxPerRow
                val itemsInRow = if (row < totalRows - 1) maxPerRow else sorted.size - row * maxPerRow
                val displayLon = baseLon + (col - (itemsInRow - 1) / 2.0) * SPREAD_LON_STEP_DEG
                val displayLat = baseLat - (row - (totalRows - 1) / 2.0) * SPREAD_LAT_STEP_DEG
                result[pin.key] = displayLat to displayLon
            }
        }
    }
    return result
}

private fun createPointFeature(
    lat: Double,
    lon: Double,
    colorHex: String,
    title: String,
    id: String = "",
    icon: String = ""
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
    if (icon.isNotBlank()) {
        properties.put("icon", icon)
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
 * Genera un Bitmap con icono de árbol estilizado (silueta de pino sobre fondo circular blanco con anillo de estado).
 */
private fun createTreeMarkerBitmap(context: Context, hexColor: String): Bitmap {
    cachedTreeMarkerBitmaps[hexColor]?.let { return it }

    val mainColor = Color.parseColor(hexColor)
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

    // 3. Anillo exterior (verde o rojo según estado sanitario)
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * density
        color = mainColor
    }
    canvas.drawCircle(cx, cy, 17f * density, strokePaint)

    // 4. Silueta de Árbol / Pino
    val treePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = mainColor
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

    cachedTreeMarkerBitmaps[hexColor] = bitmap
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
