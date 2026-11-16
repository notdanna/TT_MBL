
Esta es la **especificación técnica completa y definitiva** de todos los endpoints del backend que debe conocer e integrar la aplicación móvil Android (**EcoVerde**).

La arquitectura móvil es **Offline-First**: la aplicación trabaja primordialmente contra la base local SQLite/Room y consume estos endpoints para autenticación, sincronización por lotes (`WorkManager`), descarga OTA de modelos TFLite y alertas geoespaciales.

---

## 1. Diagnóstico y Conectividad

### `GET /api/v1/health`
* **Propósito en App:** Comprobar conectividad real con el servidor antes de lanzar tareas en segundo plano (`WorkManager`).
* **Autenticación:** Pública (No requiere token).
* **Parámetros de entrada:** Ninguno.
* **Respuesta Exitosa (`200 OK`):**
```json
{
  "status": "UP",
  "service": "backend-descortezadores",
  "version": "1.0.0",
  "timestamp": "2026-10-16T19:09:14.503755Z"
}
```

---

## 2. Autenticación, Sesión y Perfil de Usuario

### `POST /login`
* **Propósito en App:** Inicio de sesión del biólogo de campo (`CU-G01`).
* **Autenticación:** Pública.
* **Headers:** `Content-Type: application/json`
* **Cuerpo de la Petición (Request Body):**
```json
{
  "username": "experto@chapultepec.gob.mx", // String (obligatorio): Correo de la cuenta
  "password": "Password456!"                 // String (obligatorio): Contraseña
}
```
* **Respuesta Exitosa (`200 OK`):**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiJ9...", // String: Token JWT Bearer
  "token_type": "Bearer",                    // String: Tipo de autorización
  "expires_in": 3600,                        // Long: Segundos de vigencia (1 hora)
  "username": "experto@chapultepec.gob.mx",  // String: Correo del usuario
  "roles": ["Experto de campo"]              // Array<String>: Rol asignado
}
```
* **Respuestas de Error:**
  * `401 Unauthorized`: `{"message": "Credenciales inválidas"}`.

---

### `POST /api/v1/auth/logout`
* **Propósito en App:** Cierre formal de sesión (`CU-G04`).
* **Autenticación:** Requerida (`Authorization: Bearer <token>`).
* **Parámetros:** Ninguno.
* **Respuesta Exitosa (`200 OK`):**
```json
{
  "mensaje": "Sesión finalizada correctamente.",
  "usuario": "experto@chapultepec.gob.mx"
}
```

---

### `POST /api/v1/auth/recuperar-password`
* **Propósito en App:** Solicitar código/enlace de recuperación (`CU-G02`, `RN-04`, `RN-06`).
* **Autenticación:** Pública.
* **Headers:** `Content-Type: application/json`
* **Cuerpo de la Petición:**
```json
{
  "correo": "experto@chapultepec.gob.mx" // String (obligatorio)
}
```
* **Respuesta Exitosa (`200 OK`):**
```json
{
  "mensaje": "Se ha enviado el enlace o código de recuperación a su correo electrónico.",
  "codigoRecuperacion": "92DE051B",      // String: Código alfanumérico generado
  "vigenciaHoras": 24                    // Int: Horas de vigencia según RN-04
}
```
* **Respuestas de Error:**
  * `400 Bad Request`: `{"codigo": "MSG1", "mensaje": "Dato requerido faltante."}`
  * `429 Too Many Requests`: `{"codigo": "RN-06", "mensaje": "Ha excedido el límite de 3 solicitudes cada 30 días."}`

---

### `POST /api/v1/auth/restablecer-password`
* **Propósito en App:** Restablecer la contraseña con el código recibido (`CU-G03`, `RN-05`).
* **Autenticación:** Pública.
* **Headers:** `Content-Type: application/json`
* **Cuerpo de la Petición:**
```json
{
  "token": "92DE051B",                         // String (obligatorio): Código de recuperación
  "nuevaContrasena": "NuevaPassword123!",       // String (obligatorio): Mín. 8 caracteres, mayús, minús, número y especial
  "confirmacionContrasena": "NuevaPassword123!" // String (obligatorio): Debe coincidir exactamente
}
```
* **Respuesta Exitosa (`200 OK`):**
```json
{
  "mensaje": "La contraseña fue restablecida correctamente."
}
```
* **Respuestas de Error:**
  * `400 Bad Request`: `{"codigo": "ERROR_RESTABLECER", "mensaje": "Token de recuperación inválido o ya utilizado."}` (cumpliendo `RN-05`).

---

### `GET /api/v1/auth/perfil`
* **Propósito en App:** Consultar los datos del usuario en la pantalla de perfil (`CU-M11`).
* **Autenticación:** Requerida (`Authorization: Bearer <token>`).
* **Respuesta Exitosa (`200 OK`):**
```json
{
  "idCuenta": "a0000000-0000-0000-0000-000000000002", // UUID: Identificador de la cuenta
  "correo": "experto@chapultepec.gob.mx",              // String
  "nombreCompleto": "Biólogo de Campo",                // String
  "rol": "Experto de campo",                           // String
  "estadoCuenta": "Activa",                            // String
  "fechaCreacion": "2026-10-16T19:06:59.996180Z"      // Instant ISO-8601
}
```

---

## 3. Sincronización Móvil Offline-First (Núcleo de Datos)

### `POST /api/v1/sync/detecciones` *(o alias `/sync`)*
* **Propósito en App:** Endpoint principal invocado por el `WorkManager` de Android para subir en lote todas las capturas realizadas en campo (`RF-15`, `PI-01`, `§8.2.5`).
* **Autenticación:** Requerida (`Authorization: Bearer <token>`).
* **Headers:** `Content-Type: application/json`
* **Cuerpo de la Petición:** Array de objetos `SyncItemDTO`:
```json
[
  {
    "idAnalisisLocal": "4a2317ab-e579-43a9-84a8-b3f40f0fcefe", // UUID (obligatorio): ID generado en SQLite local
    "latitud": 19.42182,                                       // Double (obligatorio): Coordenadas GPS
    "longitud": -99.18203,                                     // Double (obligatorio)
    "fechaCaptura": "2026-10-16T19:17:47.377Z",                // Instant ISO-8601 (obligatorio)
    "tipoImagen": "corteza",                                   // String: "corteza" (árbol) o "follaje" (dron/copa)
    "resultadoClasificacion": "infestado",                     // String: "sano", "infestado" o "no_determinado"
    "confianza": 0.96,                                         // Double: 0.0 a 1.0 (si < 0.75, backend fuerza no_determinado)
    "observaciones": "Orificios con resina fresca",            // String (opcional)
    
    // --- Opciones para tipoImagen = "corteza" ---
    "idArbol": "b9ac7a0e-523b-4783-9162-91e112600b07",        // UUID (opcional si es árbol ya conocido)
    "nombreArbolNuevo": "Cedro Blanco #56",                    // String (opcional si se creó nuevo en campo)
    "especieArbolNuevo": "Cedro blanco (Cupressus lusitanica)",// String (opcional)

    // --- Opciones para tipoImagen = "follaje" (Dron) ---
    "idZona": "3281fc7b-2452-4385-a4ce-013fdbf5cbe1",         // UUID (opcional si es zona ya existente)
    "nombreZonaNuevo": "Zona Aérea Ahuehuetes Lago Mayor",     // String (opcional si es zona nueva)
    "radioMetrosZona": 60.0,                                   // Double (opcional, default 50.0)

    // --- Análisis Cromático HSV Complementario (RN-47, RN-48) ---
    "pctSano": 0.20,                                           // Double (0.0 a 1.0)
    "pctDanado": 0.65,                                         // Double (0.0 a 1.0)
    "pctRegular": 0.10,                                        // Double (0.0 a 1.0)
    "pctIncierto": 0.05,                                       // Double (0.0 a 1.0)

    // --- Evidencia Fotográfica (RN-07 a RN-09) ---
    "imagenBase64": "/9j/4AAQSkZJRgABAQE...",                  // String Base64 (opcional, JPG/PNG)

    // --- Dispositivo móvil ---
    "idDispositivo": "c1000000-0000-0000-0000-000000000001"    // UUID (opcional)
  }
]
```
* **Respuesta Exitosa (`200 OK`):**
```json
{
  "totalRecibidos": 1,
  "totalSincronizados": 1,
  "idsSincronizados": [
    "4a2317ab-e579-43a9-84a8-b3f40f0fcefe" // UUIDs locales que la app debe marcar como "SINCRONIZADO" en Room
  ],
  "errores": []                             // Array<String> con detalles si alguno falló
}
```

---

### `POST /api/v1/evidencias/upload`
* **Propósito en App:** Subida individual de fotografía vía `multipart/form-data` (si la app decide separar la subida binaria de la de metadatos).
* **Autenticación:** Requerida (`Authorization: Bearer <token>`).
* **Headers:** `Content-Type: multipart/form-data`
* **Parámetros Multipart:**
  * `file`: Archivo binario de la imagen (`image/jpeg` o `image/png`, máx 10 MB).
* **Respuesta Exitosa (`200 OK`):**
```json
{
  "filename": "f47ac10b-58cc-4372-a567-0e02b2c3d479.jpg",
  "url": "/api/v1/evidencias/f47ac10b-58cc-4372-a567-0e02b2c3d479.jpg",
  "sizeBytes": "1284720"
}
```

---

### `GET /api/v1/evidencias/{filename}`
* **Propósito en App:** Descargar o visualizar una fotografía previa guardada en el servidor.
* **Autenticación:** Pública.
* **Parámetros Path:** `filename` (ej. `foto.jpg`).
* **Respuesta Exitosa (`200 OK`):** Flujo binario de bytes con cabecera `Content-Type: image/jpeg`.
* **Error:** `404 Not Found`.

---

## 4. Distribución y Actualización OTA de Modelos TFLite

### `GET /api/v1/modelos/latest`
* **Propósito en App:** Comprobar si existe una nueva versión del modelo TensorFlow Lite en el servidor antes de descargar (`PT-15`, `PI-07`).
* **Autenticación:** Pública.
* **Respuesta Exitosa (`200 OK`):**
```json
{
  "version": "1.0.0",                                                    // String: Versión del modelo
  "nombreArchivo": "light_classifier.tflite",                             // String: Nombre del archivo
  "tamanoBytes": 21577384,                                               // Long: 21.5 MB
  "sha256": "0a861b6c224729c50eae1fb1cf6c3b985146002a33b9eb419217ecf...",// String: Hash SHA-256 para verificar integridad
  "urlDescarga": "/api/v1/modelos/descargar/light_classifier.tflite",     // String: Ruta relativa o URL absoluta a S3
  "fechaPublicacion": "2026-05-01T00:00:00Z"                             // Instant ISO-8601
}
```

---

### `GET /api/v1/modelos/descargar/{nombreArchivo}`
* **Propósito en App:** Descargar el archivo binario `.tflite` para inferencia local en el teléfono (`PT-15`, `PI-08`).
* **Autenticación:** Pública.
* **Parámetros Path:** `nombreArchivo` (ej. `light_classifier.tflite`).
* **Respuesta Exitosa (`200 OK` o `302 Redirect` a S3):**
  * Cabeceras: `Content-Type: application/octet-stream`, `Content-Disposition: attachment; filename="light_classifier.tflite"`
  * Cuerpo: Bytes del modelo MobileNetV2 (~21.5 MB).

---

## 5. Inferencia y Diagnóstico en la Nube (Modo Online)

### `POST /api/v1/analisis/clasificar`
* **Propósito en App:** Diagnosticar una fotografía de corteza utilizando el modelo de alta precisión (**EfficientNet-B3 Float32 completo**) en la nube cuando el dispositivo cuenta con conexión a Internet activa (`Modo Online`). Si el dispositivo no tiene red (`Modo Offline`), la app utiliza inferencia local con `light_classifier.tflite`.
* **Autenticación:** Pública / Opcional con Token Bearer.
* **Headers:**
  * Opción recomendada: `Content-Type: multipart/form-data`
  * Opción alternativa: `Content-Type: application/json`
* **Parámetros de Entrada:**
  * **Caso 1 (`multipart/form-data`):**
    * Campo `imagen` (o `file`): Archivo de imagen binario (`image/jpeg` o `image/png`).
  * **Caso 2 (`application/json`):**
    ```json
    {
      "imagenBase64": "/9j/4AAQSkZJRgABAQEASABIAAD/4gHYSUNDX1BST0ZJTEUAA..."
    }
    ```
* **Respuesta Exitosa (`200 OK`):**
```json
{
  "resultado": "sano",                     // String: "sano" o "infestado"
  "confianza": 0.9019,                     // Double: Nivel de certeza del diagnóstico (0.0 a 1.0)
  "probabilidadInfestado": 0.0981,         // Double: Probabilidad bruta de infestación (0.0 a 1.0)
  "umbral": 0.5,                           // Double: Umbral de decisión aplicado
  "tiempoInferenciaMs": 210,               // Integer: Tiempo de ejecución de inferencia en ms
  "dimensionesOriginales": "3072x4080",    // String: Resolución de la imagen original analizada
  "modelo": "classifier_full (EfficientNet-B3 Full Precision)", // String: Identificador del modelo
  "tipoInferencia": "ONLINE_CLOUD_FULL_PRECISION",              // String: Modalidad de ejecución
  "version": "1.0.0"                       // String: Versión del servicio de IA
}
```
* **Respuestas de Error:**
  * `400 Bad Request`: `{"error": "No se recibió ninguna imagen válida en la petición"}`.
  * `500 Internal Server Error`: `{"error": "Error en inferencia: <detalle>"}`.

---

## 6. Catálogo de Árboles y Zonas de Follaje

### `GET /api/v1/arboles`
* **Propósito en App:** Descargar o actualizar el catálogo de árboles en la base SQLite local (`CU-M09`).
* **Autenticación:** Requerida (`Authorization: Bearer <token>`).
* **Respuesta Exitosa (`200 OK`):**
```json
[
  {
    "idArbol": "b9ac7a0e-523b-4783-9162-91e112600b07",
    "nombreEspecifico": "Fresno Histórico #102",
    "especie": "Fresno (Fraxinus uhdei)",
    "latitud": 19.4215,
    "longitud": -99.1824,
    "fechaRegistro": "2026-10-16T19:13:31.683218Z",
    "descripcion": "Árbol centenario cerca del Lago Mayor",
    "idCuenta": "a0000000-0000-0000-0000-000000000002",
    "idDispositivo": null
  }
]
```

---

### `POST /api/v1/arboles`
* **Propósito en App:** Registrar un árbol directamente si el usuario tiene conexión en ese momento (`RF-19`).
* **Autenticación:** Requerida (`Authorization: Bearer <token>`).
* **Headers:** `Content-Type: application/json`
* **Cuerpo de la Petición:**
```json
{
  "nombreEspecifico": "Fresno #105",               // String (obligatorio)
  "especie": "Fresno (Fraxinus uhdei)",             // String (obligatorio)
  "latitud": 19.4219,                              // Double (opcional)
  "longitud": -99.1825,                            // Double (opcional)
  "descripcion": "Zona poniente de la calzada"     // String (opcional)
}
```
* **Respuesta Exitosa (`201 Created`):** Objeto `ArbolDTO` completo con su `idArbol` generado.

---

### `PUT /api/v1/arboles/{id}`
* **Propósito en App:** Editar o actualizar la descripción y observaciones de un árbol (`RF-23`, `RN-41`).
* **Autenticación:** Requerida (`Authorization: Bearer <token>`).
* **Parámetros Path:** `id` (UUID del árbol).
* **Cuerpo de la Petición:** `ArbolDTO` con los datos actualizados.
* **Respuesta Exitosa (`200 OK`):** Objeto `ArbolDTO` actualizado.
* **Error:** `404 Not Found` si el árbol no existe.

---

### `GET /api/v1/zonas-follaje`
* **Propósito en App:** Consultar las zonas macro de follaje registradas para tomas aéreas/dron.
* **Autenticación:** Requerida (`Authorization: Bearer <token>`).
* **Respuesta Exitosa (`200 OK`):**
```json
[
  {
    "idZona": "3281fc7b-2452-4385-a4ce-013fdbf5cbe1",
    "latitudCentro": 19.4225,
    "longitudCentro": -99.1830,
    "fechaRegistro": "2026-10-16T19:17:27.751493Z",
    "descripcion": "Sobreflotación con dron a 30m de altura",
    "nombreZona": "Zona Aérea Ahuehuetes Lago Mayor",
    "radioMetros": 60.0,
    "idDispositivo": null,
    "idCuenta": "a0000000-0000-0000-0000-000000000002"
  }
]
```

---

## 7. Jornada Actual, Historial y Alertas NOM-019

### `GET /api/v1/analisis/jornada`
* **Propósito en App:** Resumen de capturas realizadas en el día en curso (00:00 - 23:59) (`RF-12`, `RN-30`, `RN-31`, `CU-M05`).
* **Autenticación:** Requerida (`Authorization: Bearer <token>`).
* **Respuesta Exitosa (`200 OK`):**
```json
[
  {
    "idAnalisis": "6c394b87-70e7-4bb4-946d-6f64aa9da755",
    "rutaEvidencia": "/api/v1/evidencias/f47ac10b.jpg",
    "latitudCaptura": 19.4218,
    "longitudCaptura": -99.1820,
    "fechaCaptura": "2026-10-16T19:13:44.795744Z",
    "fechaSincronizacion": "2026-10-16T19:13:44.795747Z",
    "estadoSincronizacion": "sincronizado",
    "tipoImagen": "corteza",
    "resultadoClasificacion": "infestado",
    "confianza": 0.94,
    "observaciones": "Presencia de grumos de resina fresca en fuste",
    "idArbol": "6cdd392f-a4e8-49b1-a948-8f7bc4c7f81d",
    "idZona": null,
    "cromatico": {
      "idAnalisisCromatico": "f65e94a5-442c-4553-8a2e-302e294d9154",
      "pctIncierto": 0.05,
      "pctDanado": 0.65,
      "pctRegular": 0.10,
      "pctSano": 0.20
    }
  }
]
```

---

### `GET /api/v1/analisis/{id}`
* **Propósito en App:** Consultar el detalle individual completo de una captura previa (`CU-W10`).
* **Autenticación:** Requerida (`Authorization: Bearer <token>`).
* **Parámetros Path:** `id` (UUID del análisis).
* **Respuesta Exitosa (`200 OK`):** Objeto `AnalisisDetalleDTO`.
* **Error:** `404 Not Found`.

---

### `GET /api/v1/estadisticas/focos`
* **Propósito en App:** Obtener los **Focos de Infestación Activos** calculados por el backend según la norma NOM-019-SEMARNAT-2017 (`RF-14`, `RN-25`, `RN-27`).
* **Uso Offline:** La aplicación descarga esta lista periódicamente y la guarda en SQLite para que el servicio de geolocalización en segundo plano dispare la **Alerta de Proximidad** al acercarse a menos de 50 metros, **sin necesidad de tener internet** (`RN-28`, `RN-29`).
* **Autenticación:** Requerida (`Authorization: Bearer <token>`).
* **Respuesta Exitosa (`200 OK`):**
```json
[
  {
    "idFoco": "FOCO-1",                                     // String: Clave del foco activo
    "latitudCentro": 19.421823,                             // Double: Centroide geoespacial
    "longitudCentro": -99.182003,                           // Double: Centroide geoespacial
    "radioMetros": 17.84,                                   // Double: Radio de afectación (1,000 m2)
    "cantidadArbolesInfectados": 3,                         // Int: Árboles en el cluster (>= 3 según RN-25)
    "idsArboles": [                                         // Array<UUID>: IDs de los árboles afectados
      "6cdd392f-a4e8-49b1-a948-8f7bc4c7f81d",
      "970ca6c9-b37d-4eec-936f-337ad0415911",
      "7c52cab9-8eea-45d6-9970-4ae5a1ad8a15"
    ],
    "nombresArboles": [                                     // Array<String>
      "Cedro Blanco #55",
      "Cedro Blanco #56",
      "Cedro Blanco #57"
    ]
  }
]
```

---

### `GET /api/v1/estadisticas/dashboard`
* **Propósito en App:** Mostrar el banner informativo de alerta estacional de la NOM-019 en el dashboard principal (`RN-44`).
* **Autenticación:** Requerida (`Authorization: Bearer <token>`).
* **Respuesta Exitosa (`200 OK`):**
```json
{
  "totalArboles": 4,
  "totalSanos": 0,
  "totalInfestados": 3,
  "totalNoDeterminado": 1,
  "totalAnalisis": 3,
  "totalZonasFollaje": 1,
  "totalFocosActivos": 1,
  "distribucionPorEspecie": {
    "Cedro blanco (Cupressus lusitanica)": 3,
    "Fresno (Fraxinus uhdei)": 1
  },
  "distribucionPorEstado": {
    "sano": 0,
    "infestado": 3,
    "no_determinado": 1
  },
  "alertaEstacionalActiva": true,                            // Boolean: Activa en Ene-May y Sep-Nov
  "mensajeAlertaEstacional": "ALERTA ESTACIONAL NOM-019..."  // String: Mensaje oficial a desplegar en UI
}
```

---

## 8. Tabla Resumen para Retrofit (Android)

| Endpoint | Método | Auth | DTO Request | DTO Response |
| :--- | :---: | :---: | :--- | :--- |
| `/api/v1/health` | `GET` | No | — | `Map<String, Object>` |
| `/login` | `POST` | No | `LoginRequest` | `LoginResponse` |
| `/api/v1/auth/logout` | `POST` | Sí | — | `Map<String, String>` |
| `/api/v1/auth/recuperar-password` | `POST` | No | `RecuperarPasswordDTO` | `RecuperarPasswordResponse` |
| `/api/v1/auth/restablecer-password` | `POST` | No | `RestablecerPasswordDTO` | `Map<String, String>` |
| `/api/v1/auth/perfil` | `GET` | Sí | — | `PerfilUsuarioDTO` |
| `/api/v1/sync/detecciones` | `POST` | Sí | `List<SyncItemDTO>` | `SyncBatchResponse` |
| `/api/v1/evidencias/upload` | `POST` | Sí | `MultipartBody.Part` | `UploadResponse` |
| `/api/v1/evidencias/{file}` | `GET` | No | — | `ResponseBody` |
| `/api/v1/modelos/latest` | `GET` | No | — | `ModeloDTO` |
| `/api/v1/modelos/descargar/{file}` | `GET` | No | — | `ResponseBody` (Stream) |
| `/api/v1/analisis/clasificar` | `POST` | No/Opc | `MultipartBody.Part` (o `ClasificarRequestDTO`) | `ClasificarResponseDTO` |
| `/api/v1/arboles` | `GET` | Sí | — | `List<ArbolDTO>` |
| `/api/v1/arboles` | `POST` | Sí | `ArbolDTO` | `ArbolDTO` |
| `/api/v1/arboles/{id}` | `PUT` | Sí | `ArbolDTO` | `ArbolDTO` |
| `/api/v1/zonas-follaje` | `GET` | Sí | — | `List<ZonaFollajeDTO>` |
| `/api/v1/analisis/jornada` | `GET` | Sí | — | `List<AnalisisDetalleDTO>` |
| `/api/v1/analisis/{id}` | `GET` | Sí | — | `AnalisisDetalleDTO` |
| `/api/v1/estadisticas/focos` | `GET` | Sí | — | `List<FocoInfestacionDTO>` |
| `/api/v1/estadisticas/dashboard` | `GET` | Sí | — | `DashboardEstadisticasDTO` |