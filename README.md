# EcoVerde Mobile — Sistema Móvil de Monitoreo Fitosanitario

[![Android CI](https://github.com/notdanna/TT_MBL/actions/workflows/android-ci.yml/badge.svg)](https://github.com/notdanna/TT_MBL/actions/workflows/android-ci.yml)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![Android](https://img.shields.io/badge/Android-SDK%2035-3DDC84?logo=android&logoColor=white)
![TensorFlow Lite](https://img.shields.io/badge/TFLite-Offline%20AI-FF6F00?logo=tensorflow&logoColor=white)

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

- [`docs/Manual_Usuario_App_Movil.md`](docs/Manual_Usuario_App_Movil.md): Manual de usuario de la aplicación móvil EcoVerde (onboarding, análisis en campo, mapa offline, sincronización y solución de problemas).
- [`docs/Manual_Usuario_Plataforma_Web.md`](docs/Manual_Usuario_Plataforma_Web.md): Manual de usuario de la plataforma web EcoVerde (`Sheets TT`).
- [`AGENTS.md`](AGENTS.md): Guía de desarrollo, arquitectura y estándares para agentes de IA y desarrolladores.
- [`docs/Base_Conocimiento_TT_Descortezadores.md`](docs/Base_Conocimiento_TT_Descortezadores.md): Marco teórico biológico, especies de pino y NOM-019-SEMARNAT-2017.
- [`docs/Guia de uso de APIs del backend.md`](docs/Guia de uso de APIs del backend.md): Contratos de la API REST del backend.
- [`docs/Plan de Implementación integral de la app movil.md`](docs/Plan de Implementación integral de la app movil.md): Fases de desarrollo de la app móvil.
