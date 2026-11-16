# AGENTS.md — Guía para Agentes y Desarrolladores (EcoVerde Mobile)

> **EcoVerde Mobile** es la aplicación móvil nativa (Android) para la detección temprana, evaluación fitosanitaria y mapeo territorial del escarabajo descortezador (*Dendroctonus spp.* y *Ips spp.*) en bosques de coníferas (*Pinus spp.*), alineada con los protocolos de la **NOM-019-SEMARNAT-2017**.

---

## 1. Visión General del Proyecto

- **Nombre del Paquete**: `com.descortezadores.ecoverde`
- **Plataforma**: Android Nativo (Kotlin 2.0+)
- **Min SDK**: 26 (Android 8.0 Oreo) | **Target/Compile SDK**: 35 (Android 15)
- **Paradigma de UI**: Jetpack Compose con Material Design 3 (MD3 Expressive)
- **Arquitectura**: Clean Architecture + MVI/MVVM reactivo con Unidirectional Data Flow (UDF)
- **Estrategia Operativa**: **Offline-First estricto** (el biólogo puede operar 100% desconectado en alta montaña)
- **Hardware de Referencia**: Google Pixel 7 Pro (Pantalla LTPO a 120 Hz, sensor fotográfico de alta resolución y GPS integrado)

---

## 2. Pila Tecnológica (Tech Stack)

| Capa | Tecnologías Clave |
|---|---|
| **Lenguaje** | Kotlin 2.0.21, Coroutines, StateFlow, Kotlinx Serialization |
| **Inyección de Dependencias** | Dagger Hilt 2.51.1 |
| **Interfaz de Usuario** | Jetpack Compose (BOM 2024.10.00), Material 3, Compose Navigation |
| **Persistencia Local** | Room Database 2.6.1 con SQLite embebido (entidades offline-first) |
| **Red y API REST** | Retrofit 2.11.0, OkHttp 3 con `JwtAuthInterceptor` y timeouts de 30s |
| **Cartografía Offline** | MapLibre GL Native Android SDK (renderizado de mosaicos vectoriales sin conexión) |
| **Inteligencia Artificial** | TensorFlow Lite (`light_classifier.tflite` para inferencia local a 300x300 px) |
| **Procesamiento de Imagen** | Android Graphics Bitmap, RenderScript/Intrinsics, balance cromático subcortical y canopia |
| **Testing** | JUnit 4, Kotlin Coroutines Test, MockK, Robolectric |

---

## 3. Estructura de Directorios

```text
TT_MBL/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── assets/
│   │   │   │   ├── light_classifier.tflite    # Modelo local cuantizado TFLite (300x300)
│   │   │   │   └── labels.txt                 # Etiquetas: sano, infestado
│   │   │   ├── java/com/descortezadores/ecoverde/
│   │   │   │   ├── MainActivity.kt            # Entrada principal, configuración de 120Hz
│   │   │   │   ├── EcoVerdeApp.kt             # Application class (@HiltAndroidApp)
│   │   │   │   ├── data/                      # Capa de Datos (Data Layer)
│   │   │   │   │   ├── classification/        # Inferencia local con TFLite
│   │   │   │   │   ├── local/                 # Room DB, DAOs y entidades offline
│   │   │   │   │   ├── remote/                # Retrofit API, DTOs e interceptores JWT
│   │   │   │   │   └── repository/            # Implementación de repositorios
│   │   │   │   ├── domain/                    # Capa de Dominio (Domain Layer)
│   │   │   │   │   ├── model/                 # Modelos de dominio y enums fitosanitarios
│   │   │   │   │   ├── repository/            # Interfaces de repositorios
│   │   │   │   │   └── usecase/               # Casos de uso de negocio
│   │   │   │   ├── di/                        # Módulos de inyección Dagger Hilt
│   │   │   │   └── presentation/              # Capa de Presentación (UI)
│   │   │   │       ├── features/
│   │   │   │       │   ├── analysis/          # Pantalla principal, wizard, mapa y resultados
│   │   │   │       │   │   ├── AnalysisResultScreen.kt
│   │   │   │       │   │   ├── AnalysisResultViewModel.kt
│   │   │   │       │   │   ├── AnalysisResultUiState.kt
│   │   │   │       │   │   └── components/    # Componentes modulares y visor MapLibre
│   │   │   │       │   └── auth/              # Autenticación, sesión y recuperación
│   │   │   │       └── theme/                 # Paleta M3, tipografía, formas y movimiento (120Hz)
│   │   │   └── res/                           # Drawables, layouts nativos, valores XML
│   │   └── test/                              # Suite de pruebas unitarias (Phase 1 a 5)
├── docs/                                      # Documentación técnica y biológica del proyecto
│   ├── Base_Conocimiento_TT_Descortezadores.md# Marco teórico, biología y NOM-019-SEMARNAT-2017
│   ├── Guia de uso de APIs del backend.md    # Especificación de endpoints y contratos JSON
│   └── Plan de Implementación integral.md     # Fases de implementación histórica
├── AGENTS.md                                  # Este archivo de instrucciones para agentes
├── README.md                                  # Guía de inicio rápido para desarrolladores
└── build.gradle.kts                           # Configuración Gradle raíz y de módulo
```

---

## 4. Comandos de Compilación, Pruebas y Despliegue

Todos los comandos deben ejecutarse desde la raíz de `TT_MBL`:

```bash
# Compilar código Kotlin en modo debug
./gradlew compileDebugKotlin

# Ejecutar suite completa de pruebas unitarias
./gradlew testDebugUnitTest

# Generar APK de depuración
./gradlew assembleDebug

# Instalar directamente en el dispositivo físico conectado por ADB
./gradlew installDebug

# Lanzar la aplicación en el dispositivo físico
adb shell am start -n com.descortezadores.ecoverde/.MainActivity

# Tomar captura de pantalla de verificación
adb exec-out screencap -p > /tmp/screen_check.png
```

---

## 5. Reglas de Diseño UI y Sistema de Movimiento (120 Hz)

1. **Fondo General y Encabezados**:
   - El fondo principal de la aplicación debe ser siempre **blanco puro** (`MaterialTheme.colorScheme.background = #FFFFFF`).
   - El encabezado superior (TopBar) no debe llevar contenedor verde; es blanco puro integrado, con el saludo alineado a la izquierda y notificaciones/estado de AWS a la derecha.

2. **Tarjetas Contenedoras de Sección (`surfaceContainer`)**:
   - Cada sección principal (Resumen de Jornada, Lista de Trabajo, Mapa, Perfil, Alertas, Wizard) utiliza **Gris Claro Neutro Visible** (`MaterialTheme.colorScheme.surfaceContainer = #F1F3F5`).
   - Toda tarjeta de sección **debe incluir borde sutil de delimitación**: `border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)`.
   - Los elementos interiores (ítems de lista de árboles/zonas) usan `surfaceContainerLow = #FFFFFF` para resaltar limpios sobre el fondo gris.

3. **Sistema de Movimiento a 120 Hz sin Efecto Fantasma**:
   - Definido en [`EcoVerdeMotion`](app/src/main/java/com/descortezadores/ecoverde/presentation/theme/Theme.kt).
   - **Navbar Transitions**: Usar `EcoVerdeMotion.navDestinationTransform()`. Salida rápida (75 ms) y entrada suave (140 ms) sin desplazamientos que traslapen pantallas salientes con entrantes.
   - **Reseteo de Scroll**: Al cambiar de pestaña en el navbar, ejecutar `LaunchedEffect(uiState.currentDestination) { mainScrollState.scrollTo(0) }` para prevenir saltos de scroll.
   - **Pestañas Internas**: Usar `EcoVerdeMotion.cleanCrossfade()` para alternar entre Árboles y Zonas o Clasificación y Análisis Cromático.

4. **Preservación de Resolución y Pantalla a 120 Hz**:
   - En `MainActivity.kt`, forzar los 120 Hz utilizando exclusivamente `window.attributes.preferredRefreshRate = 120f`.
   - **NUNCA** modificar `preferredDisplayModeId` directamente en tiempo de ejecución, ya que en dispositivos como el Pixel 7 Pro forzará la resolución máxima (1440p) alterando el escalado de DPI y achicando la interfaz a modo tablet.

---

## 6. Lógica de Dominio y Protocolos Biológicos

### Bifurcación del Análisis Fitosanitario

1. **Muestra de Corteza (`SampleType.CORTEZA`)**:
   - Diagnóstico binario: `SANO`, `INFESTADO` o `NO_DETERMINADO` (si la certeza es menor al umbral de confianza del 75%).
   - Estrategia híbrida: Intenta primero inferencia de alta precisión en la nube (`POST /api/v1/analisis/clasificar` con timeout de 7s). Si no hay red o expira, ejecuta inmediatamente el modelo local TFLite (`light_classifier.tflite`).
   - Protocolo NOM-019-SEMARNAT-2017: Si es `INFESTADO`, se activan recomendaciones de derribo, troceo, descortezado o aplicación de químicos en un radio de 100 m.

2. **Muestra de Follaje / Dosel (`SampleType.FOLLAJE`)**:
   - Diagnóstico cromático territorial: Porcentajes de verde sano (`% Sano`), clorosis / estrés inicial (`% Estrés`) y necrosis / follaje muerto (`% Dañado`).
   - Asociación a **Zonas de Follaje** (`ZonaFollajeEntity`): Coordenadas GPS del centroide, nombre de zona y radio de observación aérea.

### Sincronización Offline-First

- Toda captura se guarda inmediatamente en **Room Database** con estado `SyncStatus.PENDIENTE`.
- Al detectar conectividad a internet o cuando el usuario presiona *Sincronizar ahora*:
  1. Se sube la imagen binaria a Amazon S3 mediante `POST /api/v1/evidencias/upload`.
  2. Se envía la ficha completa mediante `POST /api/v1/sincronizacion/batch`.
  3. El registro local se actualiza a `SyncStatus.SINCRONIZADO`.

### Cartografía Local con MapLibre GL

- El mapa opera de forma completamente offline mediante archivos locales de estilo y teselas vectoriales.
- Iconos del mapa:
  - **Ubicación del usuario**: Icono de persona.
  - **Árboles registrados**: Icono de árbol verde con título visible sobre el pin.
  - **Foco centrado**: Al usar la opción *Ir a esa ubicación* desde el catálogo, se centra la cámara en dicho árbol y se oculta el punto azul de ubicación actual para evitar sobreposiciones.

---

## 7. Instrucciones para Agentes al Modificar Código

1. **Integridad de Documentación**: Conserva siempre los comentarios y docstrings existentes.
2. **Pruebas Automatizadas**: Antes de finalizar cualquier tarea, corre `./gradlew testDebugUnitTest` y confirma que los 19 tests pasen al 100%.
3. **Instalación y Verificación en Dispositivo**: Si hay un dispositivo físico conectado (`adb devices`), instala con `./gradlew installDebug` y captura pantalla con `adb exec-out screencap` para verificar visualmente que no existan regresiones.
4. **Commits en Git**: Realiza commits atómicos y descriptivos siguiendo la convención Conventional Commits (`feat:`, `fix:`, `style:`, `perf:`, `test:`, `docs:`).
5. **Consultas de Dominio**: Si requieres detalles botánicos o de normativa forestal mexicana, consulta [`docs/Base_Conocimiento_TT_Descortezadores.md`](docs/Base_Conocimiento_TT_Descortezadores.md).
