package com.descortezadores.ecoverde

import android.Manifest
import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.descortezadores.ecoverde.data.local.EcoVerdeDatabase
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

/**
 * Pruebas End-to-End (E2E) instrumentadas sobre [MainActivity] con:
 * - Hilt real + Room en memoria ([TestAppModule])
 * - Inferencia offline real con LiteRT (`light_classifier.tflite`) y OpenCV (`OpenCvColorSegmenter`)
 * - Mapa vectorial offline con MapLibre GL (`style.json` + `cdmx.pmtiles`)
 */
@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class EcoVerdeEndToEndTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        *buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            add(Manifest.permission.CAMERA)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(Manifest.permission.ACCESS_MEDIA_LOCATION)
            }
        }.toTypedArray()
    )

    @get:Rule(order = 2)
    val composeRule = createEmptyComposeRule()

    @Inject
    lateinit var database: EcoVerdeDatabase

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    private lateinit var appContext: Context

    @Before
    fun setUp() {
        appContext = ApplicationProvider.getApplicationContext()
        hiltRule.inject()
        database.clearAllTables()
        runBlocking {
            dataStore.edit { it.clear() }
        }
        Intents.init()
    }

    @After
    fun tearDown() {
        runCatching { Intents.release() }
    }

    @Test
    fun e2e01_offlineLoginAndMainDestinationsNavigation() {
        ActivityScenario.launch(MainActivity::class.java).use {
            composeRule.waitUntilText("Acceso Rápido Brigadista Offline")
            composeRule.onNodeWithText("Acceso Rápido Brigadista Offline").performClick()

            // Verifica entrada al Dashboard (Inicio)
            composeRule.waitUntilText("RESUMEN DE LA JORNADA")
            composeRule.onNodeWithText("Lista de Trabajo del Día").assertIsDisplayed()

            // Navegación a Mapa (MapLibre offline)
            composeRule.onNodeWithText("Mapa").performClick()
            composeRule.waitUntilText("Mi GPS")
            composeRule.onNodeWithText("Mi GPS").assertIsDisplayed()

            // Navegación a Alertas
            composeRule.onNodeWithText("Alertas").performClick()
            composeRule.waitUntilText("Alertas Fitosanitarias en Campo")
            composeRule.onNodeWithText("Cola de Sincronización").assertIsDisplayed()

            // Navegación a Perfil y apertura de Términos y Privacidad
            composeRule.onNodeWithText("Perfil").performClick()
            composeRule.waitUntilText("Resumen Operativo")
            composeRule.scrollToText("Términos, Privacidad y Acerca de")
            composeRule.onNodeWithText("Términos, Privacidad y Acerca de").performClick()

            composeRule.waitUntilText("Términos y Privacidad")
            composeRule.onNodeWithText("1. Uso y Almacenamiento de Ubicación GPS").assertIsDisplayed()
        }
    }

    @Test
    fun e2e02_barkCaptureOfflineInferenceWithLiteRtAndRoomPersistence() {
        val sampleUri = createSampleImageUri(isBarkSample = true)
        stubGalleryPickerWithUri(sampleUri)

        ActivityScenario.launch(MainActivity::class.java).use {
            loginQuickOffline()

            // Paso 1: Modalidad -> Corteza
            composeRule.waitUntilText("Nueva Captura", useUnmergedTree = true)
            composeRule.onNodeWithText("Nueva Captura", useUnmergedTree = true).performClick()
            composeRule.waitUntilText("Selecciona la modalidad de captura")
            composeRule.onNodeWithText("Continuar con Corteza de árbol →").performClick()

            // Paso 2: Alta de nuevo árbol
            composeRule.waitUntilText("Vinculación con Árbol")
            composeRule.onNodeWithText("Dar de alta nuevo").performClick()
            composeRule.onNodeWithText("Identificador / Nombre del árbol")
                .performTextInput("Pino E2E Corteza")
            composeRule.onNodeWithText("Capturar foto →")
                .performScrollTo()
                .performClick()

            // Paso 3: Cargar imagen desde galería interceptada y ejecutar inferencia offline
            composeRule.waitUntilText("Desde galería")
            composeRule.onNodeWithText("Desde galería")
                .performScrollTo()
                .performClick()

            composeRule.waitUntilText("Analizar imagen →")
            composeRule.onNodeWithText("Analizar imagen →")
                .performScrollTo()
                .performClick()

            // Paso 4: Confirmar inferencia local (LiteRT + OpenCV), cambiar pestaña cromática y guardar en Room
            composeRule.waitUntilText("LOCAL", timeoutMillis = 15_000)
            composeRule.onNodeWithText("LOCAL").assertIsDisplayed()
            composeRule.onNodeWithText("Análisis cromático").performClick()
            composeRule.waitUntilText("Distribución cromática subcortical", timeoutMillis = 10_000)
            composeRule.onNodeWithText("Manejo de registro").performClick()

            composeRule.onNodeWithText("Guardar y finalizar")
                .performScrollTo()
                .performClick()

            // Verificar persistencia en Dashboard (Inicio) y cola en Alertas
            composeRule.waitUntilText("Pino E2E Corteza", timeoutMillis = 10_000)
            composeRule.onNodeWithText("Pino E2E Corteza").assertIsDisplayed()

            composeRule.onNodeWithText("Alertas").performClick()
            composeRule.waitUntilText("Historial General de Inspecciones")
            composeRule.waitUntil(timeoutMillis = 10_000) {
                composeRule.onAllNodes(hasText("Pino E2E Corteza", substring = true))
                    .fetchSemanticsNodes().isNotEmpty()
            }
        }
    }

    @Test
    fun e2e03_foliageCanopyOfflineAnalysisAndZoneDetail() {
        val sampleUri = createSampleImageUri(isBarkSample = false)
        stubGalleryPickerWithUri(sampleUri)

        ActivityScenario.launch(MainActivity::class.java).use {
            loginQuickOffline()

            // Paso 1: Modalidad -> Follaje / Dosel
            composeRule.waitUntilText("Nueva Captura", useUnmergedTree = true)
            composeRule.onNodeWithText("Nueva Captura", useUnmergedTree = true).performClick()
            composeRule.waitUntilText("Seleccionar Follaje / Dosel")
            composeRule.onNodeWithText("Seleccionar Follaje / Dosel").performClick()

            // Paso 2: Alta de nueva zona de follaje
            composeRule.waitUntilText("Vinculación con Zona de Follaje")
            composeRule.onNodeWithText("Dar de alta nueva zona").performClick()
            composeRule.onAllNodes(hasSetTextAction()).onFirst()
                .performTextReplacement("Zona E2E Dosel Norte")
            composeRule.onNodeWithText("Capturar foto →")
                .performScrollTo()
                .performClick()

            // Paso 3: Cargar imagen y segmentar con OpenCV
            composeRule.waitUntilText("Desde galería")
            composeRule.onNodeWithText("Desde galería")
                .performScrollTo()
                .performClick()

            composeRule.waitUntilText("Analizar imagen →")
            composeRule.onNodeWithText("Analizar imagen →")
                .performScrollTo()
                .performClick()

            // Paso 4: Guardar zona -> navega al detalle de la zona y luego regresa a Lista de trabajo
            composeRule.waitUntilText("Guardar zona y finalizar", timeoutMillis = 15_000)
            composeRule.onNodeWithText("Guardar zona y finalizar")
                .performScrollTo()
                .performClick()

            composeRule.waitUntilText("Ficha Técnica de la Zona", timeoutMillis = 10_000)
            composeRule.onNodeWithText("ESTADO DEL DOSEL").assertIsDisplayed()
            composeRule.onAllNodesWithText("Zona E2E Dosel Norte").onFirst().assertIsDisplayed()

            composeRule.onNodeWithText("← Lista de trabajo").performClick()
            composeRule.waitUntilText("Lista de Trabajo del Día", timeoutMillis = 10_000)
            composeRule.onNodeWithText("Zonas de Follaje (Dosel)").performClick()
            composeRule.waitUntilText("Zona E2E Dosel Norte", timeoutMillis = 10_000)
        }
    }

    @Test
    fun e2e04_offlineMapLoadsWithRegisteredTreePinAndCatalog() {
        val sampleUri = createSampleImageUri(isBarkSample = true)
        stubGalleryPickerWithUri(sampleUri)

        ActivityScenario.launch(MainActivity::class.java).use {
            loginQuickOffline()
            registerBarkTreeOffline(treeName = "Árbol E2E Mapa")

            // Abrir detalle del árbol y pulsar "Ver en mapa"
            composeRule.onNodeWithText("Árbol E2E Mapa").performClick()
            composeRule.waitUntilText("Ficha Técnica del Árbol")
            composeRule.scrollToText("Ver en mapa")
            composeRule.onNodeWithText("Ver en mapa").performClick()

            // Verificar MapLibre offline, conteo de árboles y expansión de catálogo
            composeRule.waitUntil(timeoutMillis = 10_000) {
                composeRule.onAllNodes(hasText("Árboles: 1", substring = true))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithText("Mi GPS").assertIsDisplayed()
            composeRule.onNodeWithText("Catálogo (1)").performClick()
            composeRule.waitUntilText("Catálogo de Árboles (1)")
            composeRule.onNodeWithText("Árbol E2E Mapa").assertIsDisplayed()
        }
    }

    @Test
    fun e2e05_alertsFiltersTreeObservationsEditAndDelete() {
        val sampleUri = createSampleImageUri(isBarkSample = true)
        stubGalleryPickerWithUri(sampleUri)

        ActivityScenario.launch(MainActivity::class.java).use {
            loginQuickOffline()
            registerBarkTreeOffline(treeName = "Árbol E2E Ciclo")

            // Verificar filtros en Alertas
            composeRule.onNodeWithText("Alertas").performClick()
            composeRule.waitUntilText("Historial General de Inspecciones")
            composeRule.onNodeWithText("Pendientes Sync").performClick()
            composeRule.waitUntil(timeoutMillis = 5_000) {
                composeRule.onAllNodes(hasText("Árbol E2E Ciclo", substring = true))
                    .fetchSemanticsNodes().isNotEmpty()
            }

            // Volver a Inicio, editar observaciones y guardar
            composeRule.onNodeWithText("Inicio").performClick()
            composeRule.waitUntilText("Árbol E2E Ciclo")
            composeRule.onNodeWithText("Árbol E2E Ciclo").performClick()
            composeRule.waitUntilText("Ficha Técnica del Árbol")

            composeRule.scrollToNode(hasSetTextAction())
            composeRule.onAllNodes(hasSetTextAction()).onFirst()
                .performTextReplacement("Resina fresca observada en cara norte")
            composeRule.scrollToText("Guardar observaciones")
            composeRule.onNodeWithText("Guardar observaciones").performClick()

            // Reabrir detalle para validar persistencia en Room
            composeRule.scrollToText("← Lista de trabajo")
            composeRule.onNodeWithText("← Lista de trabajo").performClick()
            composeRule.waitUntilText("Árbol E2E Ciclo")
            composeRule.onNodeWithText("Árbol E2E Ciclo").performClick()
            composeRule.waitUntil(timeoutMillis = 5_000) {
                composeRule.onAllNodes(hasText("Resina fresca observada en cara norte"))
                    .fetchSemanticsNodes().isNotEmpty()
            }

            // Eliminar árbol con confirmación y verificar que desaparece
            composeRule.onNodeWithContentDescription("Eliminar árbol").performClick()
            composeRule.waitUntilText("¿Eliminar árbol?")
            composeRule.onNodeWithText("Eliminar").performClick()

            composeRule.waitUntilText("No hay árboles que coincidan con el filtro seleccionado.", timeoutMillis = 10_000)
        }
    }

    private fun loginQuickOffline() {
        composeRule.waitUntilText("Acceso Rápido Brigadista Offline")
        composeRule.onNodeWithText("Acceso Rápido Brigadista Offline").performClick()
        composeRule.waitUntilText("RESUMEN DE LA JORNADA")
    }

    private fun registerBarkTreeOffline(treeName: String) {
        composeRule.waitUntilText("Nueva Captura", useUnmergedTree = true)
        composeRule.onNodeWithText("Nueva Captura", useUnmergedTree = true).performClick()
        composeRule.waitUntilText("Continuar con Corteza de árbol →")
        composeRule.onNodeWithText("Continuar con Corteza de árbol →").performClick()
        composeRule.waitUntilText("Dar de alta nuevo")
        composeRule.onNodeWithText("Dar de alta nuevo").performClick()
        composeRule.onNodeWithText("Identificador / Nombre del árbol").performTextInput(treeName)
        composeRule.onNodeWithText("Capturar foto →").performScrollTo().performClick()
        composeRule.waitUntilText("Desde galería")
        composeRule.onNodeWithText("Desde galería").performScrollTo().performClick()
        composeRule.waitUntilText("Analizar imagen →")
        composeRule.onNodeWithText("Analizar imagen →").performScrollTo().performClick()
        composeRule.waitUntilText("Guardar y finalizar", timeoutMillis = 15_000)
        composeRule.onNodeWithText("Guardar y finalizar").performScrollTo().performClick()
        composeRule.waitUntilText(treeName, timeoutMillis = 10_000)
    }

    private fun stubGalleryPickerWithUri(uri: Uri) {
        val resultData = Intent().apply { data = uri }
        Intents.intending(hasAction(Intent.ACTION_PICK))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, resultData))
    }

    private fun createSampleImageUri(isBarkSample: Boolean): Uri {
        val dir = File(appContext.cacheDir, "camera_captures").apply { mkdirs() }
        val file = File(dir, if (isBarkSample) "e2e_bark.jpg" else "e2e_foliage.jpg")
        val bitmap = Bitmap.createBitmap(360, 360, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        if (isBarkSample) {
            canvas.drawColor(Color.rgb(112, 84, 62))
            val paint = Paint().apply { color = Color.rgb(185, 118, 52) }
            canvas.drawCircle(180f, 180f, 70f, paint)
            paint.color = Color.rgb(235, 224, 195)
            canvas.drawCircle(110f, 120f, 28f, paint)
        } else {
            canvas.drawColor(Color.rgb(46, 125, 50))
            val paint = Paint().apply { color = Color.rgb(175, 196, 52) }
            canvas.drawRect(40f, 40f, 160f, 160f, paint)
            paint.color = Color.rgb(168, 74, 42)
            canvas.drawCircle(240f, 220f, 55f, paint)
        }
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        bitmap.recycle()
        return FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", file)
    }

    private fun ComposeTestRule.scrollToText(text: String) {
        scrollToNode(hasText(text))
    }

    private fun ComposeTestRule.scrollToNode(matcher: androidx.compose.ui.test.SemanticsMatcher) {
        val scrollables = onAllNodes(hasScrollToNodeAction()).fetchSemanticsNodes()
        if (scrollables.isNotEmpty()) {
            onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(matcher)
        }
    }

    private fun ComposeTestRule.waitUntilText(
        text: String,
        timeoutMillis: Long = 10_000,
        useUnmergedTree: Boolean = false
    ) {
        waitUntil(timeoutMillis = timeoutMillis) {
            onAllNodesWithText(text, substring = false, useUnmergedTree = useUnmergedTree)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}
