# EcoVerde Mobile — Sistema Móvil de Monitoreo Fitosanitario

Aplicación móvil nativa en Android (Jetpack Compose + Material 3) para la detección temprana de plagas de escarabajo descortezador (*Dendroctonus spp.* y *Ips spp.*) en coníferas mexicanas, bajo la **NOM-019-SEMARNAT-2017**.

---

## Características Principales

- **Operación Offline-First Estricta**: Permite levantar muestras, clasificar imágenes y registrar coordenadas en campo sin señal de red.
- **Inferencia de Visión Híbrida**: Clasificación local con TensorFlow Lite y opcional de alta precisión en la nube (EfficientNet-B3).
- **Análisis Bifurcado**:
  - **Corteza**: Diagnóstico de galerías y exudaciones subcorticales.
  - **Follaje**: Análisis cromático del dosel con cuantificación de clorosis y necrosis.
- **Cartografía Offline con MapLibre**: Mapeo de ejemplares y focos activos sin conexión a internet.
- **Rendimiento a 120 Hz**: Interfaz fluida optimizada para paneles LTPO (Google Pixel 7 Pro) con transiciones sin ghosting.

---

## Inicio Rápido

### Requisitos
- Android Studio Ladybug / Koala o superior
- JDK 17 o 21
- Android SDK 35 (Android 15)

### Compilación y Ejecución
```bash
# Compilar proyecto
./gradlew assembleDebug

# Ejecutar pruebas unitarias
./gradlew testDebugUnitTest

# Instalar en dispositivo conectado
./gradlew installDebug
```

---

## Documentación del Proyecto

- [`AGENTS.md`](AGENTS.md): Guía de desarrollo, arquitectura y estándares para agentes de IA y desarrolladores.
- [`docs/Base_Conocimiento_TT_Descortezadores.md`](docs/Base_Conocimiento_TT_Descortezadores.md): Marco teórico biológico, especies de pino y NOM-019-SEMARNAT-2017.
- [`docs/Guia de uso de APIs del backend.md`](docs/Guia de uso de APIs del backend.md): Contratos de la API REST del backend.
- [`docs/Plan de Implementación integral de la app movil.md`](docs/Plan de Implementación integral de la app movil.md): Fases de desarrollo de la app móvil.
