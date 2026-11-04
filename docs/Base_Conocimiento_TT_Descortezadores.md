# Base de Conocimiento Maestra — Trabajo Terminal
## Sistema móvil de visión por computadora para la detección de insectos descortezadores en el Bosque de Chapultepec (Ciudad de México)

> Documento generado a partir de `TT1_completo.tex`. Cubre íntegramente los Capítulos 1 a 8, Conclusiones, Trabajo a futuro y Glosario. Diseñado para ser 100% autocontenido como contexto técnico de referencia.

---

## Índice

1. [Resumen ejecutivo y contexto del proyecto](#1-resumen-ejecutivo-y-contexto-del-proyecto)
2. [Estado del arte y delimitación del alcance](#2-estado-del-arte-y-delimitación-del-alcance)
3. [Fundamentación matemática y glosario de notación](#3-fundamentación-matemática-y-glosario-de-notación)
4. [Metodología y desarrollo por módulos (Capítulos 4–8)](#4-metodología-y-desarrollo-por-módulos-capítulos-4-8)
5. [Tablas de datos, experimentos y resultados](#5-tablas-de-datos-experimentos-y-resultados)
6. [Conclusiones y trabajo futuro](#6-conclusiones-y-trabajo-futuro)
7. [Glosario general (por capítulo)](#7-glosario-general-por-capítulo)

---

## 1. Resumen ejecutivo y contexto del proyecto

### 1.1 Problemática

El mantenimiento de los bosques de la Ciudad de México presenta deficiencias notables por la escasez de especialistas y la falta de tecnologías de monitoreo. Estos ecosistemas enfrentan amenazas constantes de plagas, en particular **insectos descortezadores**, que en 2022 afectaron en promedio al **30% de los árboles** de varios bosques de la ciudad. Según la FAO, los bosques urbanos contribuyen al equilibrio ecológico y a la calidad de vida (calidad del aire, temperatura, recreación).

Los descortezadores, al alimentarse de la corteza, interrumpen el flujo de nutrientes, debilitando y frecuentemente matando al árbol. El impacto trasciende lo ambiental hacia lo cultural y recreativo (los bosques reciben miles de visitantes diarios y albergan museos, monumentos y sitios históricos).

La **detección temprana** es esencial para prevenir daños irreversibles y reducir costos de recuperación forestal, pero los métodos tradicionales dependen de inspección visual por especialistas: lenta, costosa y limitada por la extensión de los bosques.

**Propuesta:** desarrollar un sistema móvil que administre y monitoree la plaga en árboles afectados, usando visión por computadora para identificar automáticamente infestaciones, dando seguimiento a la evolución de la plaga y apoyando a especialistas forestales. El sistema busca ser replicable a otros espacios verdes del país como modelo de gestión ambiental urbana.

### 1.2 Objetivos

**Objetivo general:**
Desarrollar un sistema móvil de visión por computadora, basado en aprendizaje automático, para identificar automáticamente la presencia de plagas de insectos descortezadores y clasificar el estado sanitario del arbolado en un bosque específico de la Ciudad de México, usando imágenes recolectadas en campo, permitiendo la detección temprana de focos de infestación.

**Objetivos específicos:**
1. Identificar el bosque en la Ciudad de México más apto para la construcción del dataset.
2. Construir y etiquetar datos tomados directamente en campo, identificando casos sanos e infectados.
3. Diseñar una arquitectura en la nube que permita almacenar, procesar y entrenar el modelo de manera eficiente.
4. Entrenar un modelo de visión por computadora capaz de reconocer síntomas de infestación por descortezadores.
5. Desarrollar una aplicación móvil para la captura de imágenes y seguimiento de resultados.

### 1.3 Justificación

Conforme a la CONAFOR: "Los descortezadores son pequeños escarabajos que viven debajo de la corteza del árbol y se alimentan del tejido que conduce los nutrientes de este, provocando frecuentemente la muerte del arbolado." Esta plaga es el segundo problema forestal nacional después de los incendios forestales. La CONAFOR considera en *muy alto riesgo* a Ciudad de México, Chihuahua, Durango, Jalisco, Estado de México y Michoacán.

Casos documentados:
- **Bosque de Chapultepec:** ~200 árboles con afecciones según SEDEMA (Karla Mora, 2022), principalmente fresnos.
- **Fresnos (Hylesinus sp.):** incidencia de 2.4% (Reséndiz et al., 2019).
- **Cedros C. lusitanica (Phloeosinus sp.):** incidencia de 4.08%.
- **Parque República de Líbano:** 500–600 árboles perdidos entre 2017–2020; 80–90 árboles solo entre marzo y agosto de 2024.

El problema estructural es que **no existe una estrategia colectiva** de atención: el monitoreo se hace por secciones y con enfoque reactivo, no preventivo.

**Propuesta de valor del sistema:**
- **Replicable:** adaptable a otros bosques/especies/plagas del país.
- **Escalable:** basado en servicios en la nube, permite crecer de un sitio a varios.
- **Adaptativo:** los modelos pueden reentrenarse con nuevos datasets, migrando el enfoque a otras especies/sitios.
- **Accesible:** implementado como app móvil, disponible tanto a especialistas como a personal del bosque.

### 1.4 Metodología de gestión del proyecto (SCRUM)

Se adopta la metodología ágil **Scrum** por su ligereza y adaptabilidad, adecuada para equipos pequeños y proyectos con entrega incremental de valor, dada la necesidad de coordinar múltiples tecnologías, servicios en la nube y módulos interdependientes.

**Ciclo Scrum adoptado:**
1. Reunión inicial de Stakeholders para definir la visión del proyecto.
2. El Product Owner define el *Product Backlog* con los requerimientos en forma de casos de uso.
3. Cada sprint inicia con una reunión de planificación para seleccionar los casos de uso prioritarios.
4. Se crea el Sprint Backlog con las tareas del sprint.
5. Daily Standups para revisar avances y obstáculos.
6. Sprint Review al finalizar cada sprint.
7. El Product Owner valida y acepta los entregables.
8. Sprint Retrospective para identificar mejoras.
9. El ciclo se repite desde el paso 3 hasta concluir el proyecto.

**Planificación de 5 sprints en dos fases (Trabajo Terminal I y II):**

| Fase | Sprint | Enfoque | Entregables |
|---|---|---|---|
| TT I | Sprint 1 | Análisis comparativo de bosques de la CDMX; recolección manual de fotografías; clasificación preliminar; entorno de trabajo; diseño preliminar de arquitectura híbrida | Dataset en bruto, diseño preliminar de arquitectura, documentación básica |
| TT I | Sprint 2 | Análisis de modelos de ML; preparación de arquitectura híbrida; organización del dataset; portabilidad/despliegue; planificación de interfaces móviles | Dataset organizado, arquitectura preparada |
| TT II | Sprint 3 | Limpieza y preparación final del dataset; normalización; división en subconjuntos; entrenamiento inicial del modelo; optimización; inicio de backend | Modelo entrenado inicial, backend en desarrollo |
| TT II | Sprint 4 | Integración y consumo de la app móvil; ajuste de módulos; despliegue completo; validación final con imágenes de prueba; documentación consolidada | Sistema integrado y desplegado, documentación final |


---

## 2. Estado del arte y delimitación del alcance

### 2.1 Softwares y trabajos similares (estado del arte, Cap. 1)

| Software / Trabajo | Características | Tecnologías |
|---|---|---|
| *Progress in developing a bark beetle identification tool* (Marais et al., 2025) | Clasificación de géneros de escarabajos descortezadores a partir de imágenes con múltiples individuos. Fotografía macro reproducible. F1-score 0.99–1.0 en datos de prueba. | Arquitectura MaxViT (Vision Transformer con atención local y global). Dataset en HuggingFace. Transfer learning sobre backbone preentrenado. |
| *Classification of Bark Beetle-Induced Forest Tree Mortality Using Deep Learning* (Huo et al., 2022) | Clasifica cuatro etapas de ataque de descortezadores a nivel de árbol individual con imágenes RGB de UAV. Aborda desbalance de clases con aumentación afín. Accuracy 98.95%. | RetinaNet con backbone preentrenado para detección de copas. Imágenes RGB de hexacóptero en bosques del norte de México. |
| *Lightweight YOLOv8 for Apple Leaf Disease Detection* (Zhang et al., 2024) | Detecta enfermedades en hojas de manzano en entornos naturales, optimizado para móviles/embebidos. Modifica YOLOv8n reduciendo parámetros sin sacrificar precisión. | YOLOv8n + GhostConv + atención global GAM. DOI: 10.3390/app14156710. |
| *Tree Disease Detection Using Image Classification* (IEEE ICCCNT, 2023) | Compara tres CNNs para detección de enfermedades en hojas de árbol. Accuracies 80–83%. Contexto de bosques reales. | InceptionV3, ResNet, MobileNetV2. DOI: 10.1109/ICCCNT56998.2023.10307097. |
| *Apple Leaf Disease Diagnosis & Mobile App with DeepLabV3+* (2023) | Segmentación semántica de lesiones en hojas de manzano para medir severidad. Desplegado en smartphone, 9s/imagen. MIoU 83.85%. | DeepLabV3+, app móvil. DOI: 10.3390/plants12040786. |

**Enfoque diferenciador del proyecto:** sistema móvil de visión por computadora con modelo de aprendizaje profundo entrenado local y en la nube, para detección automática de descortezadores en árboles de un bosque de la CDMX, con monitoreo constante, historial digitalizado y enfoque en la toma de decisiones de gestión forestal (a diferencia de la inspección visual tradicional).

### 2.2 Contexto y caracterización del caso de estudio (Capítulo 2)

#### 2.2.1 Justificación de la selección del área de interés

Se seleccionó el **Bosque de Chapultepec** frente a otras alternativas:
- **San Juan de Aragón:** ~2,500 árboles infestados, 60% en tratamiento activo (colapso avanzado).
- **Milpa Alta:** más de 2,100 árboles muertos documentados.
- **Chapultepec:** plaga presente pero sin colapso crítico → escenario idóneo para **detección temprana**.

Criterios de selección:
- **Incidencia de la plaga:** 2.4% en fresnos (Hylesinus sp.), 4.08% en cedros (Phloeosinus sp.); promedio general de afectación urbano en CDMX ~30%.
- **Diversidad de muestras:** densidad de hospederos y variabilidad en estadios de daño permite capturar imágenes representativas para generalización del modelo.
- **Accesibilidad y logística:** Secciones II y III abiertas 24h; Sección I con horario extendido martes-domingo, facilitando recolección bajo distintas condiciones de luz.
- **Impacto ecológico y social:** calidad del aire, temperatura y servicios ecosistémicos para millones de habitantes/visitantes.

#### 2.2.2 Descripción general del área de estudio

- **Localización:** alcaldías Miguel Hidalgo (Secciones I, II, III) y Álvaro Obregón (Sección IV).
- **Extensión:** ~866.37 hectáreas — uno de los pulmones verdes más grandes de América Latina.
- **Valor social:** museos, centros deportivos, lagos, monumentos nacionales; miles de visitantes diarios.

#### 2.2.3 Caracterización de los huéspedes (insectos descortezadores)

Familia **Curculionidae**, ciclo de vida subcortical (0.1–0.6 cm de tamaño). ~3,000 especies descritas mundialmente; 870 presentes en México en 87 géneros. Menos de 20 especies pueden matar árboles por sí solas, principalmente géneros **Dendroctonus** e **Ips**.

**Mecanismo de colonización y daño:**
1. La hembra adulta perfora la corteza y se establece en el floema.
2. Señales químicas atraen al macho; ambos se reproducen dentro del árbol.
3. La hembra deposita huevos en una galería central.
4. Las larvas excavan galerías individuales ramificadas al alimentarse.
5. Se interrumpe progresivamente el transporte de nutrientes, comprometiendo la viabilidad del árbol.

Cada especie genera un **patrón de galerías propio**; el sistema no busca identificar la especie, pero la presencia de estos patrones es la evidencia visual directa de infestación que el modelo aprende a reconocer (clase "con infestación").

**El papel de la resina como defensa:** ante la perforación, el árbol libera resina (sustancia viscosa bajo presión en el xilema) que expulsa al insecto y sella el orificio. Un árbol debilitado (sequía, edad, estrés ambiental) reduce o cesa esta producción, quedando vulnerable.

#### 2.2.4 Caracterización de los hospederos

- **Fresnos (*Fraxinus uhdei*):** mayor incidencia documentada; infestados por *Hylesinus sp.*, con patrones de daño visibles en corteza.
- **Cedros Blancos (*Cupressus lusitanica*):** infestaciones ligadas a estrés hídrico, colonizados por *Phloeosinus sp.*, con alteraciones visuales en textura/color de corteza.

**Condiciones de vulnerabilidad urbana:** el estrés hídrico agravado por impermeabilización del suelo, contaminación y cambio climático reproduce el patrón de vulnerabilidad documentado en bosques templados bajo sequía (ciclo de retroalimentación negativa: cambio climático → debilitamiento del arbolado → mayor actividad de descortezadores).

La detección oportuna de descortezadores secundarios (Hylesinus, Phloeosinus) actúa como **barrera preventiva** frente a géneros de alto impacto (Dendroctonus, Ips), cuya presencia sería de difícil o imposible reversión.

#### 2.2.5 Progresión visual de la infestación

Dos niveles de observación complementarios, cada uno correspondiente a un tipo de imagen del dataset:

**A. Señales en el follaje (captura aérea/dron):**
| Fase | Color del follaje | Señales adicionales |
|---|---|---|
| 1 — Colonización reciente | Verde, sin alteración visible | Grumos frescos de resina blanca/rojiza en tronco/ramas; aserrín en la base |
| 2 — Deterioro activo | Verde-amarillento / verde limón | Infestación activa y consolidada |
| 3 — Muerte del árbol | Rojo / café-rojizo | Múltiples orificios de salida; corteza se desprende con facilidad |

*Limitación clave:* en Fase 1 el follaje aún no muestra alteración cromática detectable desde el dosel → limita la detección aérea temprana → refuerza la necesidad del análisis de corteza como complemento.

**B. Señales en la corteza (superficie exterior, escala de árbol individual):**
| Fase | Color / señal |
|---|---|
| 1 — Respuesta defensiva activa | Grumos frescos de resina blanca/crema/rojiza + aserrín |
| 2 — Colonización avanzada | Exudados café-anaranjados; orificios de entrada/salida visibles |
| 3 — Árbol muerto y abandonado | Corteza café-grisácea uniforme, sin exudados activos; orificios abundantes |

**C. Tejido subcortical expuesto (remoción controlada):** galerías con tonos café-rojizos y anaranjados sobre fondo de madera beige/crema — señal más directa y confiable de infestación activa, de mayor interés para el entrenamiento del modelo.

**Implicaciones para el análisis de color del dataset:** el análisis cromático se plantea **separado** para follaje y corteza (espacios visuales distintos), sin mezclar señales. Las señales de corteza aparecen **antes** que los cambios de follaje, por lo que son más útiles para detección temprana, aunque ambos niveles se incorporan de forma complementaria.

### 2.3 Delimitación computacional del problema (Capítulo 3)

#### 2.3.1 Naturaleza de la información de entrada

La entrada del sistema son **imágenes de árboles recolectadas en campo** en el Bosque de Chapultepec. El sistema debe identificar patrones en la apariencia externa: textura de la corteza, coloración, irregularidades superficiales o signos de daño. El análisis debe interpretar información visual **compleja, no estructurada** y sensible a variaciones de iluminación, distancia, ángulo, fondo y calidad de imagen.

#### 2.3.2 La visión por computadora como área de estudio

**Definición (Microsoft Azure):** campo de la IA que permite a las máquinas procesar, describir, analizar e interpretar entradas visuales (imágenes/video), apoyándose en aprendizaje automático o profundo.

**Tareas de visión por computadora (IBM):** reconocimiento de imágenes, clasificación de imágenes, detección de objetos, segmentación de imágenes, seguimiento de objetos, comprensión de la escena, reconocimiento facial, estimación de pose, OCR, generación de imágenes, inspección visual.

**Herramientas y marcos de trabajo (IBM):** Keras, OpenCV, Scikit-image, TensorFlow, Torchvision.
- OpenCV y Scikit-image → procesamiento digital de imágenes, detección de objetos, segmentación, extracción de características.
- Keras → interfaz de alto nivel para diseño/entrenamiento/despliegue de modelos, ejecutable sobre TensorFlow y PyTorch; Torchvision ofrece transformaciones, datasets y modelos preentrenados.

**Flujo de trabajo común (IBM):**
1. Recopilación de datos (cámaras/sensores).
2. Preprocesamiento (ajuste de brillo, contraste, tamaño).
3. Selección de modelos (comparativa de candidatos según objetivos/calidad).
4. Entrenamiento del modelo (ajuste iterativo de parámetros).
5. Generación de la salida conforme a la tarea entrenada.

#### 2.3.3 Justificación del uso de visión por computadora

Tres tareas candidatas evaluadas:
- **Clasificación de imágenes:** asigna categoría a la imagen completa → determina si un árbol presenta o no infestación.
- **Detección de objetos:** localiza y clasifica objetos mediante cajas delimitadoras → requiere etiquetado humano vía interfaz de anotación.
- **Segmentación de imágenes:** delimitación a nivel de píxel → etiquetado "extremadamente laborioso" (Bergmann), aún más costoso que detección.

**Decisión:** dado que detección y segmentación exigen anotaciones más detalladas y mayores requerimientos computacionales, el sistema se plantea como **tarea de clasificación de imágenes** (sano vs. infestado).


---

## 3. Fundamentación matemática y glosario de notación

### 3.1 De aprendizaje automático tradicional a aprendizaje profundo (Capítulo 4)

#### 3.1.1 Aprendizaje automático supervisado y clasificación

El aprendizaje automático es una rama de la IA orientada a modelos que identifican patrones y realizan predicciones sin programación explícita para cada caso. Paradigmas principales: **aprendizaje supervisado, no supervisado y por refuerzo**.

El proyecto usa **aprendizaje supervisado**: "técnica de machine learning que utiliza conjuntos de datos etiquetados para entrenar modelos de IA para identificar patrones y relaciones subyacentes" (Belcic y Stryker). Dentro de este paradigma:
- **Regresión:** predicción de un valor continuo.
- **Clasificación:** asignación de una muestra a una categoría/clase predefinida — **este es el enfoque del proyecto** (imagen de árbol → sano/infestado).

Tipos de clasificación: **binaria** (dos clases — el caso de este proyecto), **multiclase** (varias opciones), multietiqueta (fuera de alcance).

#### 3.1.2 Principales algoritmos de clasificación tradicionales

- **Regresión logística:** estima la probabilidad de pertenencia a una clase; común en clasificación binaria.
- **Árboles de decisión:** reglas jerárquicas a partir de características de entrada.
- **Bosque aleatorio (Random Forest):** combina múltiples árboles para mejorar generalización.
- **Máquina de vectores de soporte (SVM):** encuentra una frontera de separación óptima entre clases.
- **k-vecinos más cercanos (k-NN):** clasifica según cercanía a muestras etiquetadas.
- **Bayes ingenuo (Naive Bayes):** enfoque probabilístico basado en ocurrencia de características por clase.

#### 3.1.3 Limitaciones de los enfoques tradicionales en escenarios reales

Requieren preprocesamiento previo (segmentación, mejora, refinamiento) y **extracción manual de características**. El desempeño depende de la calidad de esa representación construida a mano; tienen dificultad para representar estructuras de nivel medio/alto y semántica compleja; son costosos en tiempo de procesamiento en contextos no controlados. **Relevancia para el proyecto:** las imágenes de campo (iluminación, ángulo, fondo variables) hacen insuficiente un enfoque basado en extracción manual.

#### 3.1.4 Aprendizaje profundo: definición y ventajas

El aprendizaje profundo es una rama del aprendizaje automático basada en **redes neuronales artificiales con múltiples capas**, que aprende automáticamente las representaciones más relevantes a partir de los datos (sin ingeniería manual de características), integrando extracción de características y clasificación en un mismo esquema end-to-end.

**Ventajas clave:**
- Mayor precisión en tareas de clasificación, segmentación, detección, SLAM.
- Flexibilidad: reentrenable para nuevos contextos.
- Utilidad en escenarios con grandes volúmenes de datos visuales anotados.
- Extensión a visión 3D, percepción robótica y sistemas híbridos.

**Importancia de los datos anotados:** la calidad del modelo depende de datos etiquetados; el etiquetado manual puede tomar >90 minutos por imagen en tareas complejas (segmentación urbana, imágenes médicas). El **aprendizaje activo profundo** busca priorizar las muestras más informativas para reducir la carga de anotación.

#### 3.1.5 Comparación ML tradicional vs. Deep Learning

| Aspecto | ML tradicional | Deep Learning |
|---|---|---|
| Representación de datos | Ingeniería de características manual | Aprendizaje automático de representaciones (capas jerárquicas) |
| Tipo de datos ideal | Estructurados (tablas, variables numéricas/categóricas) | No estructurados (imágenes, audio, texto) |
| Requerimiento de datos/cómputo | Menor volumen, menor infraestructura | Mayor volumen, mayor tiempo de cómputo e infraestructura |
| Capacidad representacional | Limitada a relaciones definidas explícitamente | Alta capacidad para relaciones no lineales y patrones complejos |

**Justificación de Deep Learning para el proyecto:** las imágenes de campo contienen información visual compleja (color, textura, bordes, forma, relaciones espaciales) con variabilidad real de iluminación/ángulo/fondo. El aprendizaje profundo permite aprender representaciones directamente de la imagen, siendo más consistente ante esta diversidad visual que un enfoque de ingeniería manual de características.

### 3.2 Fundamentos matemáticos de las Redes Neuronales Convolucionales (Capítulo 5)

Las CNN son arquitecturas de aprendizaje profundo especializadas en imágenes/video: las primeras capas detectan bordes; las capas subsecuentes reconocen texturas, partes y objetos completos. Hito histórico: LeNet-5 (LeCun, 1989) → AlexNet (2012), que redujo el error top-5 en ImageNet de ~26% a ~15%.

**Flujo general de una CNN:**

```
Imagen --[Conv+BN+ReLU+Pool] × n--> Features --[Flatten]--> Vector --[FC]--> Logits --[Softmax]--> Probabilidades
```

#### 3.2.1 Convolución

Operación central: un filtro/kernel recorre la imagen realizando producto punto con los píxeles adyacentes, produciendo un **mapa de características** (*feature map*):

$$S(i,j) = (I * K)(i,j) = \sum_m \sum_n I(i+m, j+n) \cdot K(m,n)$$

- $I$: imagen de entrada.
- $K$: kernel/filtro convolucional.
- $S(i,j)$: valor del mapa de características en la posición $(i,j)$, que indica la intensidad del patrón detectado en esa región.

#### 3.2.2 Normalización por lotes (Batch Normalization)

Normaliza las salidas de cada capa convolucional para evitar activaciones muy grandes/pequeñas, resolviendo el *internal covariate shift* (Ioffe & Szegedy, 2015):

$$\hat{x} = \frac{x - \mu}{\sqrt{\sigma^2 + \epsilon}}$$

- $\mu$: media del lote (*batch*) actual.
- $\sigma^2$: varianza del lote actual.
- $\epsilon$: constante pequeña para evitar división entre cero.
- $\hat{x}$: valor normalizado.

#### 3.2.3 Función de activación ReLU

Introduce no linealidad, esencial para aprender patrones complejos:

$$f(x) = \max(0, x)$$

Convierte valores negativos a cero y conserva los positivos. Sin no linealidad, apilar capas convolucionales equivaldría a una sola capa lineal (la composición de funciones lineales sigue siendo lineal).

#### 3.2.4 Agrupamiento (Pooling)

Reduce dimensiones espaciales tomando valores representativos por región (comúnmente **Max Pooling**):

$$P(i,j) = \max_{(m,n) \in R_{ij}} f(m,n)$$

- $R_{ij}$: región (ventana) sobre la que se calcula el máximo.
- Disminuye el costo computacional y aporta invarianza traslacional.

#### 3.2.5 Aplanado (Flatten)

Convierte la matriz bidimensional en un vector unidimensional, puente entre extracción de características y clasificación:

$$\text{Tensor}(H \times W \times C) \rightarrow \text{Vector}(H \cdot W \cdot C)$$

Ejemplo: un tensor $7 \times 7 \times 512$ se transforma en un vector de 25,088 valores.

#### 3.2.6 Capas Densas (Fully Connected)

Cada neurona se conecta con todas las activaciones de la capa anterior:

$$y = W \cdot x + b$$

- $W$: matriz de pesos.
- $x$: vector de entrada.
- $b$: vector de sesgos (bias).
- $y$: salida de la capa.

#### 3.2.7 Clasificación mediante Softmax

En la capa de salida (multiclase), convierte logits (valores continuos) en probabilidades que suman 1.

### 3.3 Glosario de notación matemática (consolidado)

| Símbolo | Significado |
|---|---|
| $I$ | Imagen de entrada (matriz de píxeles) |
| $K$ | Kernel / filtro convolucional |
| $S(i,j)$ | Valor del mapa de características (feature map) en $(i,j)$ |
| $\mu$ | Media del batch |
| $\sigma^2$ | Varianza del batch |
| $\epsilon$ | Constante de estabilidad numérica |
| $\hat{x}$ | Valor normalizado (batch norm) |
| $f(x)=\max(0,x)$ | Función de activación ReLU |
| $R_{ij}$ | Región/ventana de pooling |
| $P(i,j)$ | Valor tras max pooling |
| $H, W, C$ | Alto, ancho y canales de un tensor |
| $W$ (en capa densa) | Matriz de pesos |
| $x$ | Vector de entrada a la capa densa |
| $b$ | Vector de sesgo (bias) |
| $y$ | Salida de la capa densa (logits antes de softmax) |


---

## 4. Metodología y desarrollo por módulos (Capítulos 4–8)

### 4.1 Selección de arquitectura de Deep Learning (Capítulo 5)

#### 4.1.1 Familias de arquitecturas relevantes

- **CNNs clásicas:** estándar histórico para clasificación de imágenes; capas de nodos (entrada, ocultas, salida) con pesos/umbrales; aprovechan álgebra lineal (multiplicación de matrices); computacionalmente exigentes (requieren GPU).
- **Arquitecturas basadas en regiones (YOLO):** seccionan la imagen en regiones y aplican múltiples CNNs para detectar objetos individuales — útil con múltiples elementos de interés simultáneos.
- **Transformers de Visión (ViT):** aplican atención (mecanismo de Transformers) sobre proyecciones lineales de parches de imagen, en vez de convoluciones sobre píxeles adyacentes.

#### 4.1.2 Relevancia para dispositivos móviles

Restricciones clave: memoria, procesamiento, batería. Factores relevantes: **tamaño del modelo** (MB, número de parámetros) y **latencia de inferencia**. ResNet-50 supera 90 MB; MobileNet/EfficientNet-B0 reducen tamaño manteniendo precisión competitiva mediante **convoluciones separables en profundidad** (*depthwise separable convolutions*, Howard et al. 2017). TensorFlow Lite convierte modelos Keras a formato `.tflite` compatible con Android.

#### 4.1.3 Modelos de referencia evaluados

- **ResNet-50:** CNN profunda de 50 capas, ~25M parámetros, ~98 MB. Baseline útil pero poco viable para despliegue móvil directo.
- **MobileNetV3:** evolución de MobileNet vía NAS (Neural Architecture Search); capas de atención ligeras y activaciones h-swish; <5 MB; sólida para Android.
- **EfficientNet-B0/B1/B3:** escalado compuesto (profundidad, ancho, resolución balanceados). B0 desde 5.3 MB, precisión superior a MobileNetV3 en ImageNet con tamaño comparable.
- **MobileViT:** híbrida (convoluciones locales + atención tipo Transformer); más ligera que ViT estándar pero más costosa que MobileNetV3/EfficientNet; soporte TFLite menos maduro.

#### 4.1.4 Transfer learning y fine-tuning

**Transfer learning:** reutilizar un modelo preentrenado en un dataset de gran escala como punto de partida, heredando representaciones ya aprendidas (bordes, texturas, formas) en vez de inicializar pesos aleatoriamente — reduce tiempo de entrenamiento y datos necesarios. Es especialmente relevante aquí porque el dataset de campo es limitado (entrenar desde cero causaría sobreajuste).

**Estrategia de dos fases adoptada:**
1. **Feature extraction:** pesos del modelo base congelados; se entrena solo la cabeza de clasificación.
2. **Fine-tuning:** se descongelan las últimas capas del modelo base; entrenamiento continúa con tasa de aprendizaje reducida, adaptando el modelo a las características visuales específicas del dominio.

#### 4.1.5 Comparación de modelos candidatos

| Modelo | Parámetros | Tamaño | Top-1 ImageNet | Soporte TFLite |
|---|---|---|---|---|
| ResNet-50 | 25.6 M | 98 MB | 76.0% | Parcial |
| MobileNetV3 | 5.4 M | 4.9 MB | 75.2% | Nativo |
| EfficientNet-B0 | 5.3 M | 5.3 MB | 77.1% | Nativo |
| EfficientNet-B1 | 7.8 M | 7.8 MB | 79.1% | Nativo |
| **EfficientNet-B3** | **12.3 M** | **48 MB** | **81.6%** | **Nativo** |
| MobileViT-S | 5.6 M | 22 MB | 78.4% | Limitado |

#### 4.1.6 Modelo seleccionado: EfficientNet-B3

**Decisión:** EfficientNet-B3, por ofrecer la mayor precisión en ImageNet entre los candidatos viables para móvil, con 48 MB manejable vía TFLite en Android. Disponible preentrenado en `tf.keras.apps`, facilitando transfer learning + fine-tuning.

**Elementos clave de diseño que distinguen a EfficientNet-B3 de una CNN convencional:**
1. **MBConv** (*Mobile Inverted Bottleneck Convolution*): separa la detección de patrones espaciales de la combinación de canales, logrando la misma capacidad representativa con menor costo computacional.
2. **Squeeze-and-Excitation:** pondera adaptativamente la importancia de cada canal según el contenido de la imagen, amplificando características relevantes y suprimiendo irrelevantes.
3. **Búsqueda de arquitectura neuronal (NAS):** garantiza que el punto de partida (EfficientNet-B0) sea óptimo antes del escalado.

**Compound Scaling:** desde B0, EfficientNet-B3 se obtiene escalando **simultáneamente** profundidad (número de capas), ancho (número de filtros por capa) y resolución de entrada, en proporción balanceada — a diferencia de enfoques tradicionales que escalan solo una dimensión. Esto explica por qué B3 alcanza 81.6% de precisión con solo 12.3M de parámetros.

**Flujo de EfficientNet-B3:** imágenes de entrada pasan por capas sucesivas que extraen patrones visuales progresivos (bordes/texturas → representaciones complejas), produciendo finalmente una clasificación (ver también §3.2 para el flujo genérico de CNN y las fórmulas asociadas a cada operación: convolución, batch norm, ReLU, pooling, flatten, capas densas y softmax).


### 4.2 Conjunto de datos y preprocesamiento (Capítulo 6)

#### 4.2.1 Recopilación de datos

Dos fuentes:
1. **Capturas propias en campo** en bosques de la CDMX, con énfasis en Chapultepec.
2. **iNaturalist:** plataforma de ciencia ciudadana con filtrado taxonómico para recuperar imágenes de la familia de insectos descortezadores.

**Dos tipos de imagen según nivel de observación:**
- **Follaje** (perspectiva aérea): usadas para identificar zonas con posibles afecciones vía análisis cromático (§2.2.5). Se mantienen en **una sola clase** (no requieren separación por etapa).
- **Corteza** (tomadas sobre el tronco): alimentan el modelo de clasificación. Organizadas en **dos clases: sano / infestado**.

Todas las imágenes estandarizadas a **300 × 300 píxeles**, formato JPG (tamaño de entrada de EfficientNet-B3).

#### 4.2.2 Etiquetado de imágenes

El etiquetado se define por la **estructura de carpetas**, compatible con `image_dataset_from_directory` de TensorFlow/Keras (infiere la etiqueta del nombre de carpeta). No se requirió LabelImg/Roboflow, ya que la tarea es clasificación de imagen completa (no hay recorte de regiones).

**Criterio de asignación de clase por fuente:**
- iNaturalist: identificación taxonómica ya asociada, validada en limpieza de dataset.
- Campo: confirmadas con apoyo de un experto, verificando señales visuales de infestación (grumos de resina, galerías, alteraciones de color).

#### 4.2.3 Preprocesamiento de imágenes

**Dos etapas:**

1. **Estandarización:** todas las imágenes redimensionadas a 300×300 px, formato JPG; valores de píxel normalizados al rango $[0,1]$ (dividiendo entre 255) — práctica estándar para estabilizar el entrenamiento (regla RN-09).

2. **Aumento de datos (*data augmentation*):** transformaciones aleatorias aplicadas **en tiempo real durante el entrenamiento** (mediante `tf.keras.layers`, no sobre el dataset en disco):
   - **Flip horizontal y vertical:** los patrones de galerías son reconocibles en cualquier orientación.
   - **Rotaciones** de hasta 90°: simulan distintos ángulos de captura.
   - **Variaciones de brillo y contraste:** simulan condiciones de iluminación natural del bosque.
   - **Zoom leve:** simula variaciones en la distancia de captura.
   
   No se aplican deformaciones fuertes, ya que podrían perder los patrones generados por larvas en la corteza.

#### 4.2.4 Análisis complementario de color

Objetivo: identificar tonalidades asociadas a las fases de infestación (§2.2.5) como información adicional a la clasificación del modelo. Se ejecuta **localmente en el dispositivo**, concurrente al modelo TensorFlow Lite, **sin conexión a internet** (RN-49).

**Espacio de color: HSV** (Hue, Saturation, Value) — permite aislar el tono cromático en un solo canal, facilitando rangos de color precisos ante variaciones de brillo/iluminación de campo (a diferencia de RGB).

Para corteza, los píxeles se clasifican en rangos correspondientes a:
- Blanco/crema (respuesta defensiva activa).
- Café-anaranjado (colonización avanzada).
- Café-rojizo/grisáceo (daño severo / muerte).

El resultado se expresa como **porcentaje de píxeles** por rango sobre el total analizado (RN-51).

#### 4.2.5 Organización de datos: entrenamiento, validación y prueba

División **80/10/10** (entrenamiento/validación/prueba) del dataset de corteza:
- **Entrenamiento:** ajusta los parámetros del modelo.
- **Validación:** monitorea desempeño al final de cada época; detecta sobreajuste.
- **Prueba:** reservado hasta la evaluación final, garantizando desempeño ante datos no vistos.

La división es **aleatoria pero estratificada**, manteniendo proporción equivalente entre clases *sano* / *infestado* en los tres subconjuntos.


### 4.3 Análisis del sistema (Capítulo 7)

El sistema se compone de **dos subsistemas complementarios**:
1. **Aplicación móvil Android** (offline-first), núcleo del prototipo para trabajo en campo.
2. **Plataforma web**, orientada a consulta y supervisión de la información recolectada.

Ambos se relacionan mediante una **base de datos central en la nube**. La app móvil almacena en **SQLite local** y sincroniza ascendentemente hacia la nube al recuperar conexión. La plataforma web consulta la información consolidada pero **no modifica** los registros operativos generados desde los dispositivos móviles.

#### 4.3.1 Requerimientos funcionales (RF)

Ámbitos: **MOV** (app móvil), **WEB** (plataforma web), **MOV/WEB** (ambos).

| ID | Ámbito | Descripción |
|---|---|---|
| RF-01 | MOV/WEB | Registro e inicio de sesión con correo y contraseña. |
| RF-02 | MOV/WEB | Recuperación de contraseña vía enlace enviado al correo. |
| RF-03 | MOV | Entrada de imágenes por cámara o galería del dispositivo. |
| RF-04 | MOV | Herramientas de edición básicas antes de enviar la imagen a procesar. |
| RF-05 | MOV | Registro automático de ubicación geográfica (metadatos de foto o GPS). |
| RF-06 | MOV | Registro local de todas las imágenes ingresadas, con resultado, timestamp y ubicación, para sincronización posterior. |
| RF-07 | MOV | Sincronización de registros locales a la nube con conexión, conservando fecha/hora original de captura. |
| RF-08 | MOV/WEB | App móvil: modelo TensorFlow Lite embebido para clasificación local offline. App web: modelo Keras3 para clasificación en la nube. |
| RF-09 | MOV | Clasificación inmediata en pantalla (sano/infectado) generada por el modelo local, sin internet. |
| RF-10 | WEB | Agrupar y dar seguimiento a muestras específicas en el tiempo (historial de salud por ejemplar). |
| RF-11 | MOV/WEB | Alertas/indicadores visuales con el análisis complementario de color. |
| RF-12 | MOV | Resumen estadístico de la jornada actual (fotos totales, sanos, posibles infecciones). |
| RF-13 | MOV | Acceso directo y visible a "Nueva Captura". |
| RF-14 | MOV | Notificaciones al acercarse a una zona catalogada como foco de infestación. |
| RF-15 | MOV | Sincronización manual disponible solo con errores de sincronización y conexión activa. |
| RF-16 | WEB | Generar reporte básico en PDF de las capturas realizadas. |
| RF-17 | WEB | Descargar el reporte generado. |
| RF-18 | MOV | Conservar en almacenamiento local los registros asociados a imágenes capturadas (excepto las imágenes mismas). |
| RF-19 | MOV | Asociar una nueva captura a un árbol existente o crear un nuevo registro de árbol. |
| RF-20 | WEB | Historial completo y permanente de todos los árboles registrados, sin restricción de tiempo. |
| RF-21 | WEB | Estadísticas generales: totales por estado sanitario, distribución por especie, evolución temporal de focos. |
| RF-22 | MOV/WEB | Análisis complementario de color sobre la imagen, mostrado en ambas apps. |
| RF-23 | MOV | Edición de información de un registro de árbol (estado sanitario, observaciones). |
| RF-24 | WEB | Mapa general con ubicación de todos los árboles, filtrable por estado, especie, fecha, dispositivo. |
| RF-25 | MOV | Mapa general de árboles del dispositivo, filtrable por estado, especie, fecha. |
| RF-26 | WEB | Administrador gestiona cuentas de usuario (creación, edición, desactivación, roles). |
| RF-27 | MOV/WEB | Una cuenta de experto puede asociarse a uno o varios dispositivos; cada dispositivo mantiene su base SQLite independiente; al reconectar, sincroniza conservando la identificación del dispositivo de origen. |
| RF-28 | MOV | Dos dispositivos con árboles del mismo nombre en la misma cuenta no se asumen como el mismo árbol físico. |

#### 4.3.2 Requerimientos no funcionales (RNF)

| ID | Categoría | Descripción |
|---|---|---|
| RNF-01 | Rendimiento | Resultado del análisis (sano/infectado) en tiempo máximo de X segundos tras cargar la imagen en la nube. |
| RNF-02 | Portabilidad | Tamaño de la app instalada ≤ 50 MB. |
| RNF-03 | Usabilidad | Transiciones entre pantallas ≤ X segundos. |
| RNF-04 | Eficiencia | Optimización para no ejecutar procesos innecesarios en segundo plano (ahorro de batería). |
| RNF-05 | Seguridad | Toda comunicación app–nube vía HTTPS (credenciales, imágenes, coordenadas). |
| RNF-06 | Seguridad | Contraseñas almacenadas con algoritmos de hashing. |
| RNF-07 | Seguridad | Cierre automático de sesión tras inactividad prolongada (p.ej. 24h). |
| RNF-08 | Precisión | Precisión mínima de X% en clasificación de árboles infestados. |
| RNF-09 | Arquitectura | Arquitectura cliente-servidor; el procesamiento pesado del modelo se delega a la nube. |
| RNF-10 | Persistencia | Base de datos optimizada para consultas rápidas del historial forestal y referencias de imágenes. |
| RNF-11 | Implementación | App cliente nativa Android en **Kotlin** (acceso a cámara, GPS, almacenamiento). |
| RNF-12 | Implementación | Modelo de visión por computadora programado en **Python** (TensorFlow/PyTorch). |
| RNF-13 | Rendimiento | Backend en Python maneja peticiones de forma **asíncrona** (no bloquea la UI de Kotlin). |
| RNF-14 | Mantenibilidad | Proyecto móvil usa **Gradle**; entorno Python usa **pip/conda**. |
| RNF-15 | Persistencia | Cliente Android implementa **SQLite** (preferentemente vía **Room**) para almacenamiento local offline. |
| RNF-16 | Persistencia | Backend usa **PostgreSQL** como motor relacional central (credenciales, historial sanitario). |
| RNF-17 | Integridad | Registros SQLite cambian a "Sincronizado" o se eliminan localmente tras confirmación del backend, evitando duplicidad. |

#### 4.3.3 Reglas de negocio (RN) — resumen completo

| ID | Tipo | Descripción |
|---|---|---|
| RN-01 | Acceso | Solo rol **Administrador** crea cuentas nuevas; registro no abierto al público. |
| RN-02 | Acceso | **Experto de campo**: app móvil y web. **Administrador**: solo app web. |
| RN-03 | Sesión | Sesión móvil persiste hasta cierre manual; no expira por inactividad. |
| RN-04 | Seguridad | Enlace de recuperación de contraseña válido máx. 24 horas. |
| RN-05 | Seguridad | Enlace de recuperación de un solo uso. |
| RN-06 | Seguridad | Máx. 3 solicitudes de recuperación de contraseña por usuario cada 30 días. |
| RN-07 | Imagen | Formatos aceptados: JPG, JPEG, PNG, WEBP, BMP, HEIC. |
| RN-08 | Imagen | Tamaño máximo de imagen: 10 MB. |
| RN-09 | Imagen | Resolución mínima 300×300 px (entrada de EfficientNet-B3); menor resolución se rechaza. |
| RN-10 | Edición | Herramienta de edición disponible: recorte de imagen. |
| RN-11 | Edición | La edición solo se aplica antes del análisis; no editable después. |
| RN-12 | Edición | Edición opcional; se puede omitir. |
| RN-13 | Ubicación | Si la imagen (galería) tiene metadatos de ubicación, se usan esas coordenadas. |
| RN-14 | Ubicación | Si no hay metadatos, se usa el GPS del dispositivo al registrar. |
| RN-15 | Ubicación | Sin ninguna de las dos vías, se guarda sin coordenadas ("Ubicación no disponible"), sin bloquear la captura. |
| RN-16 | Registros | Eliminación manual de registro local antes de sincronizar: irreversible, requiere confirmación. |
| RN-17 | Registros | Estados posibles: Pendiente de sincronización, Sincronizado, Error de sincronización. |
| RN-18 | Registros | Mínimo por registro: imagen, resultado del modelo, timestamp, coordenadas GPS, árbol asociado. |
| RN-19 | Sincronización | Automática al detectar conexión (datos móviles o Wi-Fi). |
| RN-20 | Sincronización | Reintento automático hasta 3 veces antes de marcar error. |
| RN-21 | Sincronización | Fecha/hora de sincronización se almacena separada del timestamp original; ambas visibles en web. |
| RN-22 | Modelo | Resultado mostrado inmediatamente tras captura, etiquetado explícitamente **"Offline"**. |
| RN-23 | Modelo | Clasificación en dos categorías únicamente: **Sano** o **Infestado**. |
| RN-24 | Modelo | El modelo TFLite solo clasifica si la confianza supera **75%**; si no, resultado "No determinado", se recomienda repetir captura. |
| RN-25 | Alertas | Según NOM-019-SEMARNAT-2017: **foco de infestación activo** = 3+ árboles infectados en 1,000 m². |
| RN-26 | Interfaz | Código de color: **verde**=sano, **rojo**=infestado, **amarillo**=no determinado. |
| RN-27 | Alertas | Proximidad a foco = 100 m o menos (según NOM-019-SEMARNAT-2017). |
| RN-28 | Alertas | Alerta de proximidad usa solo GPS + registros locales SQLite, sin internet. |
| RN-29 | Alertas | Máximo 1 alerta por foco cada 60 minutos. |
| RN-30 | Jornada | Resumen de jornada considera solo timestamps del día en curso (00:00–23:59). |
| RN-31 | Jornada | Resumen se actualiza en tiempo real con nuevas capturas. |
| RN-32 | Sincronización | Reintento manual disponible con conexión activa para registros en "Error de sincronización". |
| RN-33 | Reportes | Reportes web incluyen historial sincronizado de un periodo o zona específica. |
| RN-34 | Reportes | Todo reporte incluye mínimo: ID árbol, nombre, fecha/hora, resultado, coordenadas GPS, nombre del experto. |
| RN-35 | Almacenamiento | Eliminación automática de local solo aplica a registros "Sincronizado"; "Pendiente"/"Error" no se eliminan automáticamente. |
| RN-36 | Historial | Historial móvil muestra solo registros del día actual. |
| RN-37 | Árboles | No puede existir captura sin árbol asociado (paso obligatorio). |
| RN-38 | Árboles | Al crear árbol: ID único automático; usuario asigna nombre y especie de lista predefinida (Fresno *Fraxinus uhdei*, Cedro blanco *Cupressus lusitanica*, y otras gestionables por el Administrador). |
| RN-39 | Árboles | El nombre puede complementarse con descripción libre opcional. |
| RN-40 | Usuarios | Solo **Administrador** crea, edita, desactiva y asigna roles a usuarios. |
| RN-41 | Usuarios | **Experto de campo** puede editar registros de árboles y agregar observaciones en móvil. |
| RN-42 | Historial | Historial web sin restricción de tiempo (todos los registros sincronizados desde el inicio). |
| RN-43 | Alertas | Radio de proximidad (RN-27/29) configurable solo desde web por el Administrador. |
| RN-44 | Alertas | Según NOM-019-SEMARNAT-2017: periodos de mayor riesgo = enero–mayo y septiembre–noviembre; alertas estacionales posibles en web. |
| RN-45 | Modelo | Evidencias visuales de infestación buscadas: grumos de resina rojiza en fuste, cambio de color de follaje verde→verde amarillento/rojizo, galerías/daños visibles en corteza. |
| RN-46 | Análisis cromático | Se ejecuta local, concurrente al modelo TFLite, usando **OpenCV**, sin internet. |
| RN-47 | Análisis cromático | Follaje: 3 rangos — verde, verde-amarillento/limón, café-rojizo. Resultado en % por rango. |
| RN-48 | Análisis cromático | Corteza: rangos blanco/crema/rojizo, café-anaranjado, café-grisáceo. Resultado en % por rango. |
| RN-49 | Análisis cromático | Análisis independiente por tipo de imagen (follaje vs. corteza); sin mezclar/agregar entre tipos. |
| RN-50 | Almacenamiento | SQLite local **no almacena imágenes como binarios permanentes**; solo metadatos y referencia/ruta/URL. |
| RN-51 | Historial | Evidencia fotográfica completa disponible en web solo tras sincronización correcta. |
| RN-52 | Sincronización | Solo ascendente (dispositivo → nube); no hay descarga automática hacia otros dispositivos. |
| RN-53 | Historial | Web consulta y analiza, pero **no modifica** registros operativos de la app móvil. |
| RN-54 | Usuarios | CRUD del Administrador aplica solo a datos administrativos/configuración en la nube. |

#### 4.3.4 Catálogo de mensajes del sistema (MSG)

| Código | Mensaje |
|---|---|
| MSG1 | Dato requerido faltante. |
| MSG2 | El correo electrónico no está registrado. |
| MSG3 | Contraseña incorrecta. |
| MSG4 | La cuenta se encuentra desactivada. |
| MSG5 | El dispositivo no está autorizado para esta cuenta. |
| MSG6 | No fue posible enviar el correo de recuperación. |
| MSG7 | El enlace o código de recuperación no es válido. |
| MSG8 | El enlace o código de recuperación ha expirado. |
| MSG9 | Las contraseñas no coinciden. |
| MSG10 | La contraseña no cumple con los requisitos de seguridad. |
| MSG11 | No existe una sesión activa. |
| MSG12 | No fue posible cerrar la sesión correctamente. |
| MSG13 | No existen registros para la jornada actual. |
| MSG14 | No fue posible cargar la información local. |
| MSG15 | No existen registros con el estado sanitario seleccionado. |
| MSG16 | No fue posible cargar los registros recientes. |
| MSG17 | No fue posible acceder a la cámara. |
| MSG18 | No fue posible importar la imagen seleccionada. |
| MSG19 | Imagen no válida para el análisis. |
| MSG20 | No fue posible registrar la ubicación GPS. |
| MSG21 | No fue posible guardar el análisis en la base local. |
| MSG22 | No fue posible obtener la clasificación del estado sanitario. |
| MSG23 | No fue posible recortar la imagen. |
| MSG24 | Área de recorte no válida. |
| MSG25 | No fue posible sincronizar el registro. |
| MSG26 | No existen registros sincronizados disponibles. |
| MSG27 | No fue posible cargar la información consolidada. |
| MSG28 | No fue posible aplicar los filtros seleccionados. |
| MSG29 | No se cuenta con permisos de administración. |
| MSG30 | No fue posible cargar la información de usuarios. |
| MSG31 | El correo electrónico ya se encuentra registrado. |
| MSG32 | No fue posible guardar los cambios de la cuenta. |
| MSG33 | El dispositivo ya se encuentra asociado a una cuenta. |
| MSG34 | No fue posible asociar el dispositivo. |
| MSG35 | No fue posible desasociar el dispositivo. |

#### 4.3.5 Actores del sistema

**1. Experto de campo** — usa la app móvil en campo y la web para consultar información sincronizada. Acciones: iniciar sesión, recuperar/restablecer contraseña, capturar/importar imágenes, recortar/repetir captura, asociar imagen a árbol nuevo/existente, registrar/modificar datos de árboles, obtener clasificación, consultar análisis cromático, consultar registros recientes/historial local/mapa local, filtrar registros, consultar notificaciones, reintentar sincronización, consultar información consolidada en web (historial, mapa, estadísticas, perfil), cerrar sesión.

**2. Administrador** — gestiona cuentas y dispositivos desde la web; no modifica registros operativos. Acciones: iniciar sesión web, recuperar/restablecer contraseña, crear/editar/desactivar cuentas, asignar roles, gestionar/asociar/desasociar/consultar dispositivos, cerrar sesión.

#### 4.3.6 Métricas de evaluación del proyecto

**A. Métricas de desempeño del modelo:**
- **Exactitud (Accuracy):** % de predicciones correctas sobre el total.
- **Precisión (Precision):** proporción de positivos predichos que son realmente positivos.
- **Sensibilidad (Recall):** capacidad de detectar correctamente casos con daño/riesgo.
- **F1-score:** combina precisión y sensibilidad.
- **Matriz de confusión:** aciertos/errores por clase.

**B. Métricas de desempeño del sistema:**
- Tiempo de respuesta del análisis (captura → resultado).
- Tiempo de guardado local (SQLite).
- Tiempo de sincronización (dispositivo → nube).
- Tasa de sincronización exitosa.
- Disponibilidad offline.
- Consistencia de datos (local vs. nube).

**C. Criterios de aceptación del prototipo:**
- Login funcional según rol.
- Captura/importación de imágenes para análisis.
- Selección de tipo de análisis (follaje/corteza).
- Asociación de análisis de corteza a árbol; de follaje a zona.
- Generación de resultado de clasificación + nivel de confianza.
- Almacenamiento local sin conexión.
- Sincronización de pendientes con conexión.
- Consulta de registros, mapas, historial y estadísticas en web.
- Evidencia fotográfica disponible en web tras sincronización.

#### 4.3.7 Análisis de riesgos y restricciones

**Riesgos del dataset:**
- **Sesgo de dominio:** datasets preliminares (*bark-complete*, *bark_infected*) de condiciones distintas a Chapultepec (especies, ángulos, iluminación, etapas).
- **Desbalance entre etapas de infestación:** riesgo de detectar solo casos evidentes, fallando en infestaciones tempranas (las más relevantes).
- **Variabilidad estacional/ambiental:** dataset de una sola temporada puede no representar otras condiciones.

**Riesgos técnicos de implementación:**
- **Variabilidad en captura:** distancia/ángulo/enfoque/exposición de campo → requiere validación de imagen previa a inferencia.
- **Rendimiento en gama baja:** `bark_classifier.tflite` optimizado con **cuantización float16**; dispositivos sin NPU pueden tener inferencia lenta.
- **Deriva del modelo (model drift):** poblaciones/patrones cambian con el tiempo → requiere reentrenamiento periódico.
- **Integración backend-web:** latencias/errores de sincronización afectan trazabilidad histórica.

**Restricciones del prototipo:**
- Dependencia de condiciones de captura mínimas (sin validación robusta en tiempo real aún).
- Ausencia de uso sostenido por usuarios reales (posibles deficiencias no visibles en pruebas controladas).
- Cobertura geográfica restringida a Chapultepec; extensión a otras áreas requiere validación adicional.

**Medidas de mitigación:**
- Campañas de captura en campo (especies, etapas, iluminación, temporada variadas).
- Aumento de datos orientado al dominio (brillo, rotación, recortes, color).
- Validación de imagen previa a inferencia (nitidez, exposición, dimensiones).
- Modo offline con sincronización posterior.
- Pruebas con usuarios reales en campo (técnicos forestales).


#### 4.3.8 Plan general de pruebas

**A. Pruebas del modelo de clasificación (resultados preliminares — ver también §5.2):**
Prueba binaria con EfficientNet-B3 sobre 1,500 imágenes por clase (corteza sana/infestada), balanceadas manualmente, para validar viabilidad y establecer línea base (no modelo definitivo).

**B. Pruebas del sistema (funcionales):**

*Herramientas:* **Espresso** (UI Android, flujos de navegación, captura, resultados, comportamiento offline); **Postman + Newman** (endpoints REST de API Gateway, automatizado en GitHub Actions).

| ID | Caso de prueba | Pasos | Resultado esperado |
|---|---|---|---|
| PT-01 | Login exitoso (móvil) | Ingresar credenciales válidas | Autentica y muestra pantalla principal |
| PT-02 | Login fallido | Credenciales incorrectas | Muestra MSG03, no permite acceso |
| PT-03 | Captura desde cámara | "Nueva Captura" + foto | Se procesa con TFLite y muestra clasificación |
| PT-04 | Clasificación offline | Sin conexión, capturar imagen | Resultado TFLite etiquetado "Offline" |
| PT-05 | Guardado local SQLite | Captura sin conexión | Registro con estado "Pendiente de sincronización" |
| PT-06 | Sincronización automática | Activar conexión con pendientes | Sincroniza y actualiza a "Sincronizado" |
| PT-07 | Sincronización manual | Registros en error, reintento | Reintenta y actualiza estado |
| PT-08 | Alerta de proximidad | Acercarse a foco registrado | Notificación sin internet |
| PT-09 | Resumen de jornada | Consultar resumen del día | Muestra totales correctos |
| PT-10 | Login exitoso (web) | Credenciales válidas | Cognito emite JWT, Angular redirige a dashboard |
| PT-11 | Consulta historial web | Acceder a historial completo | Muestra todos los registros sincronizados |
| PT-12 | Generación reporte PDF | Rango de fechas + generar | Genera y permite descargar PDF |
| PT-13 | Visualización de mapa | Consultar mapa general | Marcadores correctos con estado sanitario |
| PT-14 | Gestión de usuarios (admin) | Crear cuenta de experto | Cuenta creada, login funcional |
| PT-15 | Descarga de modelo TFLite | Conexión con modelo desactualizado | Descarga nueva versión desde Amazon S3 |

**C. Pruebas de integración:**

*Herramientas:* **Micronaut Test** (contexto completo backend), **Testcontainers** (PostgreSQL real en Docker), **JUnit 5**, **Newman** (Postman en CI/CD).

| ID | Caso de prueba | Componentes | Resultado esperado |
|---|---|---|---|
| PI-01 | Sincronización móvil → API Gateway | App (Retrofit) → API Gateway | POST /sync llega con JWT en header |
| PI-02 | Validación JWT | API Gateway → Cognito | Rechaza sin token con HTTP 401 |
| PI-03 | API Gateway → Lambda | — | Invocación y procesamiento < 5s incl. cold start |
| PI-04 | Lambda → RDS (escritura) | Lambda → RDS PostgreSQL | Persistencia vía Micronaut Data JPA + Testcontainers |
| PI-05 | Lambda → S3 (imagen) | Lambda → S3 | Carga y retorno de URL pública |
| PI-06 | Lambda → RDS (lectura) | Lambda → RDS | Historial retornado correctamente |
| PI-07 | SageMaker → S3 (publicación) | SageMaker → S3 | `.tflite` publicado y accesible |
| PI-08 | App móvil → S3 (descarga) | App → S3 | Nueva versión descargada y reemplazada |
| PI-09 | Angular → API Gateway (protegida) | Angular (interceptor) → API GW | JWT adjuntado, HTTP 200 |
| PI-10 | CloudFront → S3 (frontend) | CloudFront → S3 | Estáticos servidos con HTTPS y caché |
| PI-11 | Flujo completo de sincronización | App → API GW → Lambda → RDS → S3 | Ciclo sin errores, estado "Sincronizado" |
| PI-12 | Flujo completo de consulta web | Angular → API GW → Lambda → RDS | Historial mostrado correctamente |
| PI-13 | TFLite + OpenCV en dispositivo | TFLite (EfficientNet-B3) + OpenCV | Concurrente, < 3s, sin internet |
| PI-14 | SQLite → Room (escritura) | Room → SQLite | Persistencia con estado, coordenadas, timestamp |
| PI-15 | SQLite → Room (lectura/filtrado) | Room → SQLite | Retorna solo Pendiente/Error |
| PI-16 | Token refresh Angular | Interceptor → Cognito | Refresh automático sin interrumpir sesión |
| PI-17 | D3.js con datos reales | Angular (D3.js) → API GW → Lambda → RDS | Visualizaciones correctas de focos/evolución |

#### 4.3.9 Recursos y herramientas por capa

| Capa | Herramientas |
|---|---|
| Modelo de clasificación | **TensorFlow + Keras** (entrenamiento; EfficientNet-B3), **TensorFlow Lite** (conversión `.tflite` para móvil), **Scikit-learn** (métricas: precisión, recall, F1, matriz de confusión), **Amazon SageMaker** (registro/versionado de modelo) |
| Procesamiento de imágenes | **OpenCV** (redimensionamiento, espacios de color, normalización, transformaciones geométricas; análisis cromático local en móvil) |
| Aplicación móvil | **Kotlin**, **Jetpack Compose** (UI declarativa), **TensorFlow Lite** (inferencia local), **Room** (persistencia SQLite), **MapLibre GL Android** (mapas offline vía `.pmtiles`), **Retrofit + OkHttp** (cliente REST), **JWT propio** (autenticación) |
| Aplicación web | **Angular** (SPA), **Angular Material** (componentes UI), **D3.js** (visualización de datos), **MapLibre GL JS** (mapas web), **HttpClient** (consumo de API), **Amazon Cognito** (autenticación), **AWS Certificate Manager** (SSL/TLS) |
| Infraestructura en la nube | Ver §4.4 (Diseño del sistema) |

**Recursos disponibles del equipo:**
- 3 desarrolladores estudiantes de Ingeniería en Sistemas Computacionales.
- Equipos de cómputo personales.
- Servidor local Raspberry Pi + disco duro externo ($5,000 MXN).
- Dron para captura aérea ($3,000 MXN).
- Dispositivo Android para pruebas de campo.
- Software: herramientas open-source o con licencia académica gratuita (Android Studio, IntelliJ IDEA Community, Python, TensorFlow, OpenCV, Scikit-learn, Angular CLI, GitHub Actions).


#### 4.3.10 Infraestructura en la nube (AWS)

Modelo **serverless** sobre AWS. Servicios principales:

- **Amazon API Gateway:** punto de entrada REST unificado (móvil y web); enrutamiento a Lambda, control de acceso y rate limiting.
- **AWS Lambda:** aloja el backend (Micronaut); escalado automático, sin gestión de servidores; despliegue vía CI/CD (GitHub Actions).
- **Amazon RDS (PostgreSQL):** motor relacional central (árboles, capturas, clasificaciones, GPS, usuarios).
- **Amazon S3:** almacenamiento de objetos (imágenes, artefactos estáticos Angular, modelo `.tflite`).
- **Amazon CloudFront:** CDN para distribuir el frontend Angular.
- **Amazon Cognito:** identidad/autenticación web (login, recuperación de contraseña, JWT).
- **Amazon SageMaker:** exclusivamente **Model Registry** (no inferencia en tiempo real); versiona el modelo antes de publicarlo en S3.
- **AWS Certificate Manager (ACM):** certificados SSL/TLS automáticos para CloudFront y API Gateway.
- **Amazon CloudWatch:** monitoreo de logs Lambda, métricas de API Gateway (latencia, errores 4xx/5xx, tráfico), alarmas.
- **GitHub Actions:** pipeline CI/CD (pruebas, build, despliegue a Lambda).
- **Raspberry Pi:** servidor local de pruebas (staging on-premise, arquitectura ARM).

**Capa gratuita de AWS aplicable al prototipo:**

| Servicio | Límite gratuito | Tipo |
|---|---|---|
| AWS Lambda | 1M invocaciones y 400,000 GB-seg/mes | Siempre gratuito |
| Amazon API Gateway | 1M llamadas REST/mes | Siempre gratuito |
| Amazon S3 | 5 GB, 20,000 GET, 2,000 PUT/mes | Siempre gratuito |
| Amazon CloudFront | 1 TB transferencia, 10M solicitudes/mes | Siempre gratuito |
| Amazon Cognito | 50,000 usuarios activos/mes | Siempre gratuito |
| Amazon RDS (PostgreSQL) | 750 hrs/mes db.t2.micro | 12 meses |
| Amazon SageMaker | 250 hrs/mes t2.micro | 2 meses |
| AWS Certificate Manager | SSL/TLS ilimitado | Siempre gratuito |
| Amazon CloudWatch | 5 GB logs, 10 métricas/mes | Siempre gratuito |

Costo total con capa gratuita durante los primeros 12 meses: **$0.00 USD** (hasta 3 dispositivos móviles activos).

**Justificación del proveedor: AWS sobre Azure y GCP**

- Cuota de mercado 2025: AWS 31%, Azure 25%, GCP 11% (Synergy Research Group).
- Madurez del ecosistema serverless: AWS Lambda (2014) es 2 años anterior a Azure Functions y Google Cloud Functions (2016).
- Integración nativa más madura con GitHub Actions, Micronaut, y SageMaker Model Registry vs. Vertex AI/Azure ML.
- CloudFront ofrece 1 TB gratis permanente vs. Azure CDN (15 GB/12 meses) y Cloud CDN (10 GB/mes).
- Cognito: 50,000 MAU gratis indefinido, integración directa con API Gateway/Lambda.
- Reserved Instances de RDS: hasta 72% de descuento sobre demanda.
- Programas académicos: AWS Educate / AWS Academy.
- Azure es óptimo solo dentro del ecosistema Microsoft (no aplica al stack Java/Micronaut/Angular/PostgreSQL/GitHub Actions).
- GCP destaca en BigQuery/GKE/Vertex AI, no relevantes para un sistema REST con base relacional.

**Proyección a ambiente productivo** (100+ dispositivos, 200+ usuarios, 50,000+ imágenes/mes):

*Serverless (Lambda) vs. Contenedores (Docker+EKS):*

| Criterio | AWS Lambda (serverless) | Docker + Kubernetes (EKS) |
|---|---|---|
| Administración | Nula (AWS gestiona) | Alta (config/mantenimiento de cluster) |
| Escalado | Automático e instantáneo por invocación | Automático pero requiere HPA/nodos |
| Cold starts | 2–5s (Micronaut, primer arranque) | Sin cold starts |
| Costo base mensual | $0 sin tráfico | ~$150 USD (cluster EKS) independiente del tráfico |
| Load balancer | No necesario (API Gateway) | Necesario (ALB, ~$20 USD/mes) |
| Límite de ejecución | 15 min máximo | Sin límite |
| Ideal para | Tráfico intermitente/impredecible | Tráfico constante de alto volumen |
| Complejidad operativa | Baja | Alta |

**Decisión:** Lambda continúa siendo óptimo para el escenario productivo dado el tráfico intermitente de sincronización. Migración a EKS+ALB+Auto Scaling sería la evolución natural solo si se escala a *miles* de dispositivos con sincronización frecuente/continua o procesos batch de larga duración; Micronaut soporta ambos sin cambios significativos de código.

*Instancias RDS PostgreSQL evaluadas (us-east-1, 2026):*

| Instancia | vCPUs | RAM (GB) | Single-AZ (USD/mes) | Multi-AZ (USD/mes) | Caso de uso |
|---|---|---|---|---|---|
| db.t3.micro | 2 | 1 | $15.33 | $30.66 | Desarrollo/pruebas, no recomendado en producción |
| db.t3.small | 2 | 2 | $28.49 | $56.98 | Producción ligera, hasta 50 usuarios concurrentes |
| **db.t3.medium** | **2** | **4** | **$52.56** | **$105.12** | **Producción estándar, 100–200 usuarios (seleccionada)** |
| db.t3.large | 2 | 8 | $105.12 | $210.24 | Alta concurrencia, 500+ usuarios |
| db.m5.large | 2 | 8 | $138.00 | $276.00 | Producción intensiva, cargas sostenidas |

**db.t3.medium** seleccionada por: memoria suficiente para caché de consultas frecuentes, CPU con bursting (modo *unlimited* de familia T3), costo equilibrado con reservación anual, y ruta de escalabilidad hacia Amazon Aurora PostgreSQL (compatible sin cambios de código).

*Estimación de costos mensuales en producción (AWS Pricing Calculator, us-east-1, mayo 2026, tipo de cambio $17.50 MXN/USD):*

| Servicio | Configuración | Upfront (USD) | Mensual (USD) | Mensual (MXN) |
|---|---|---|---|---|
| AWS Lambda | 5M invocaciones, ARM64, 512 MB, 500ms | $0.00 | $0.80 | $14.00 |
| Amazon API Gateway | 5M llamadas REST/mes | $0.00 | $17.50 | $306.25 |
| Amazon RDS PostgreSQL | db.t3.medium, Reserved 1 año, 100 GB gp3 | $845.00 | $63.15 | $1,105.13 |
| Amazon S3 | 500 GB estándar | $0.00 | $11.83 | $206.93 |
| Amazon CloudFront | Pro Plan | $0.00 | $15.00 | $262.50 |
| Amazon CloudWatch | Logs y métricas | $0.00 | $3.00 | $52.50 |
| AWS WAF | 1 Web ACL, reglas básicas | $0.00 | $13.00 | $227.50 |
| Amazon Cognito | 200+ usuarios activos/mes | $0.00 | $10.00 | $175.00 |
| **Total** | | **$845.00** | **$134.28** | **$2,349.90** |

Costo total primeros 12 meses en producción: **$2,456.36 USD ($42,986.30 MXN)** (incluye pago anticipado de RDS). RDS Reserved genera ahorro ~27% vs. on-demand ($1,602.80/año vs. $1,260 USD/año on-demand nominal comparado bajo el escenario descrito).

**Estrategias de optimización de costos:** Reserved Instances RDS (-27%), Lambda ARM64/Graviton2 (-20% cómputo), S3 Intelligent-Tiering (-40% en datos fríos), caché agresivo en CloudFront.


### 4.4 Diseño del sistema (Capítulo 8)

#### 4.4.1 Arquitectura general del sistema

Tres componentes principales: **aplicación móvil**, **plataforma web** e **infraestructura AWS**.

- Cliente móvil: estrategia **offline-first**; captura e inferencia (TensorFlow Lite) locales; sincronización eventual y segura al recuperar conectividad.
- Nube: enfoque **serverless** (API Gateway + Lambda), autenticación con Cognito, ciclo de vida del modelo con SageMaker, persistencia en S3/RDS.
- Cliente web: distribuido vía CloudFront.
- Transversal: ACM (SSL/TLS), GitHub Actions (CI/CD), CloudWatch (monitoreo centralizado).

#### 4.4.2 Estrategia de seguridad y control de acceso

- **Datos en tránsito:** ACM aprovisiona certificados SSL/TLS en API Gateway y CloudFront → HTTPS obligatorio.
- **Autenticación híbrida:**
  - **Web:** Amazon Cognito (grupo de administradores), emite tokens.
  - **Móvil:** backend Micronaut genera/valida **JWT propio** (dado el enfoque offline), aligerando la carga del dispositivo.
  - Ambos tipos de token son validados por API Gateway antes de invocar cualquier Lambda.

**Almacenamiento y persistencia políglota:**
1. **Amazon RDS (PostgreSQL):** fuente de la verdad transaccional — usuarios, inventario de árboles, metadata de reportes, configuraciones. Elegido por robustez ACID y soporte de consultas geoespaciales futuras.
2. **Amazon S3:** almacenamiento de objetos — imágenes fotográficas, build de Angular, versiones del modelo `.tflite`.

**Integración con Amazon SageMaker:** exclusivamente como **Model Registry**, no para inferencia en tiempo real (que ocurre on-device). El entrenamiento intensivo (EfficientNet-B3/YOLO) se ejecuta en cómputo local; tras evaluación con Scikit-learn, se sube a SageMaker para versionado formal; SageMaker orquesta la exportación a `.tflite` y su depósito en S3.

**Observabilidad y monitoreo:** Amazon CloudWatch centraliza logs de Lambda (Java/Micronaut) y métricas de API Gateway (latencia, errores 4xx/5xx, tráfico); alarmas automatizadas ante anomalías (picos de cold start, accesos denegados masivos).

**CI/CD:** Docker local para PostgreSQL; Raspberry Pi como staging on-premise (ARM); GitHub Actions ejecuta compilación, pruebas unitarias, empaquetado Java 21 y actualización de AWS Lambda vía AWS CLI.

**Flujo de sincronización móvil → nube:**
1. Dispositivo detecta conexión y lee registros locales SQLite.
2. Retrofit realiza petición REST a API Gateway con JWT propio.
3. API Gateway verifica la firma del token y canaliza el cuerpo (texto + imágenes) a Lambda.
4. Lambda (Micronaut) decodifica imágenes y las transmite a S3, obteniendo una URL permanente.
5. Lambda construye el objeto de dominio, asocia la URL de S3 y persiste en RDS (transaccional).
6. Lambda retorna HTTP 200 OK; la app marca los registros locales como "sincronizados".

**Estructura interna del backend (Clean Architecture, Java 21 + Micronaut):**

Micronaut se eligió por resolver la inyección de dependencias en **tiempo de compilación (AoT)**, eliminando sobrecarga en runtime (vs. Spring) — vital para baja latencia serverless.

Capas concéntricas (dependencias apuntan hacia adentro):
- **Domain:** entidades puras (`Deteccion`, `Arbol`, `Usuario`, `Captura`) e interfaces de repositorio abstractas (inversión de dependencias).
- **Use Cases:** orquestan el flujo (`SyncDeteccionUseCase`, `RegistrarDeteccionUseCase`, `ConsultarHistorialUseCase`).
- **Interface Adapters:** controladores REST (`SyncController`, `DetectionController`, `TreeController`, `UserController`) y mappers DTO.
- **Infrastructure:** Micronaut Data JPA/Hibernate (PostgreSQL/RDS), SDK AWS (S3), validación JWT, configuración de CloudWatch.

Estructura de paquetes:
```
com.descortezadores.backend/
  domain/
    entities/          (Deteccion, Arbol, Usuario, Captura)
    repositories/      (interfaces abstractas)
    rules/             (invariantes y reglas de negocio)
  usecases/
    sync/              (SyncDeteccionUseCase)
    detection/         (RegistrarDeteccionUseCase)
    history/           (ConsultarHistorialUseCase)
  adapters/
    controllers/       (SyncController, DetectionController)
    mappers/           (DeteccionMapper, ArbolMapper)
    repositories/      (DeteccionRepositoryImpl, S3ImageRepository)
  infrastructure/
    persistence/       (Micronaut Data JPA, Hibernate)
    aws/               (S3Client, CognitoValidator)
    security/          (JwtValidator)
    config/            (MicronautConfig, DataSourceConfig)
```

#### 4.4.3 Arquitectura de la aplicación móvil

- **Kotlin** como lenguaje principal (estándar Google Android).
- **Jetpack Compose** (UI declarativa, sin XML) para reactividad y menor código.
- Patrón **MVVM** (Model-View-ViewModel): separación entre presentación (Compose), ViewModels y Repositories.

**Inferencia local (on-device, ejecución única por fotografía):**
1. **Preprocesamiento con OpenCV para Android:** análisis cromático, filtrado de ruido, redimensionamiento/normalización de tensores.
2. **Inferencia con TensorFlow Lite:** evaluación por EfficientNet-B3/YOLO en formato `.tflite`, aprovechando aceleración de hardware; resultado asociado permanentemente al registro.

**Offline-First y sincronización:**
- Persistencia local con **SQLite vía Room** (información de árbol, coordenadas, fecha, diagnóstico único), consultas verificadas en compilación, flujos asíncronos (Coroutines/Flows).
- Imágenes en almacenamiento interno del dispositivo.
- **Background worker** sincroniza al detectar conexión estable.
- **Retrofit + OkHttp:** peticiones REST + inyección automática de JWT en headers; tras HTTP 200 del backend, se actualiza el estado local a "sincronizado".

#### 4.4.4 Arquitectura de la aplicación web

- **Angular** (SPA) — carga única de HTML, actualización dinámica sin recarga.
- **Angular Material** — componentes UI consistentes y responsivos.

**Distribución estática:** build de Angular (HTML/CSS/JS) en bucket S3 de alojamiento estático; **CloudFront** como CDN con caché en nodos perimetrales; integración con **ACM** para HTTPS.

**Consumo de API:** **HttpClient** nativo de Angular hacia endpoints de API Gateway. **D3.js** para visualizaciones interactivas (distribución espacial de árboles infectados, series de tiempo de propagación, mapas de calor) — manipulación directa del DOM para gráficos altamente personalizados.

**Autenticación y seguridad en el cliente:**
1. Login contra **User Pool** de Cognito.
2. Cognito devuelve ID Token y Access Token (JWT), almacenados de forma segura en el navegador.
3. **HttpInterceptor** de Angular adjunta automáticamente el Access Token en cabecera `Authorization`.
4. **Route Guards** (`CanActivate`) verifican validez del token antes de permitir vistas administrativas; redirige a login si expiró/no autenticado.

#### 4.4.5 Arquitectura del modelo de clasificación y análisis cromático

**Flujo completo de inferencia:**
1. Imagen de corteza capturada en móvil → preprocesado: redimensionamiento a 300×300 px + normalización vía `efficientnet.preprocess_input`.
2. Evaluación de conectividad:
   - **Sin conexión:** inferencia local con modelo TFLite embebido.
   - **Con conexión:** imagen enviada a Amazon SageMaker (mayor cómputo, facilita reentrenamiento).
3. En ambos casos, la imagen atraviesa **EfficientNet-B3** (convolución + pooling → capas densas → clasificación binaria → probabilidad de infestación).
4. **Análisis de color** (independiente del origen del resultado): si se clasifica como infestado, se analiza la distribución de tonalidades en busca de patrones de daño (p.ej. pérdida de resina).
5. Resultado combinado consolidado en la salida final (sano/infestado + información cromática).

#### 4.4.6 Entorno de desarrollo y pruebas locales

- **Local:** Docker para PostgreSQL; backend Micronaut/Gradle en local; Angular CLI para frontend; pruebas de OpenCV/TFLite con bancos de imágenes en emulador/dispositivo.
- **Staging on-premise (Raspberry Pi):** arquitectura ARM (consistencia con Lambda); despliega contenedores de BD, backend pre-lanzamiento y frontend; valida sincronización asíncrona en red local real.
- **Orquestación (GitHub Actions):** pipeline como guardián de calidad — si fallan pruebas unitarias/integración, se aborta el despliegue (*no deploy*); si aprueba, bifurca artefactos: backend → Lambda (RDS/S3); frontend estático → S3/CloudFront/ACM.


#### 4.4.7 Casos de uso del sistema

**Nomenclatura de etiquetado:**
- **CU-GX — General:** elementos compartidos entre app móvil y web.
- **CU-MX — App móvil:** funcionalidades del Experto en Android.
- **CU-WX — Plataforma web:** funcionalidades del Experto en la web.
- **CU-AX — Administración:** funciones exclusivas del Administrador en la web.

**Diagramas de casos de uso (agrupados por módulo):**

| Diagrama | Casos de uso | Descripción funcional |
|---|---|---|
| Autenticación móvil/web | CU-G01–CU-G04 | Iniciar sesión, recuperar contraseña (extiende a login), restablecer contraseña, cerrar sesión. Experto: móvil y web; Administrador: solo web. |
| Dashboard móvil | CU-M05–CU-M12 | Resumen de jornada, nuevo análisis, registros recientes, historial local, mapa local, notificaciones, perfil, configuración. |
| Captura y análisis móvil | CU-M06, CU-M13–CU-M21 | Captura/importación de imagen, recorte/repetición, asociación a árbol (nuevo/existente), clasificación, análisis cromático. |
| Historial móvil | CU-M08, CU-M22–CU-M23 | Consulta de análisis de la jornada, filtrado, reintento manual de sincronización (base local únicamente). |
| Mapa local móvil | CU-M09, CU-M24–CU-M25 | Consulta de árboles del dispositivo, filtros, detalle de marcador (base local únicamente). |
| Perfil/configuración móvil | CU-M11–CU-M12, CU-M26–CU-M27, CU-G04 | Consultar/editar perfil, configuración, preferencias, cerrar sesión. |
| Notificaciones móvil | CU-M10, CU-M28–CU-M30 | Notificaciones generales, de sincronización, sanitarias, marcar como leídas. |
| Dashboard web | CU-W05–CU-W09 | Resumen general, historial general, mapa general, estadísticas, perfil (información consolidada en la nube). |
| Historial general web | CU-W06, CU-W10–CU-W15 | Registros sincronizados, filtros, historial por árbol (evidencia, clasificación, análisis cromático, gráfica de evolución). Solo consulta. |
| Mapa general web | CU-W07, CU-W16–CU-W18 | Árboles sincronizados de todos los dispositivos de la cuenta, filtros, focos de infestación activos. |
| Estadísticas web | CU-W08, CU-W19–CU-W23 | Distribución por estado sanitario, por especie, evolución temporal, por dispositivo de origen. Solo consulta. |
| Perfil web | CU-W09, CU-G04 | Consultar perfil, cerrar sesión (sin edición). |
| Administración web | CU-A01–CU-A09 | Gestionar/crear/editar/desactivar cuentas, asignar roles, gestionar/asociar/desasociar dispositivos. |

**Especificación textual detallada — Casos de uso generales (CU-G01 a CU-G04):**

**CU-G01 Iniciar sesión**
- **Actor:** Experto de campo, Administrador.
- **Propósito:** acceso al sistema mediante credenciales válidas según rol y plataforma.
- **Entradas:** correo electrónico, contraseña.
- **Salidas:** pantalla principal correspondiente al rol/plataforma.
- **Precondiciones:** cuenta registrada y activa; en móvil, dispositivo asociado a la cuenta.
- **Postcondiciones:** acceso con permisos del rol correspondiente.
- **Errores:** MSG1 (dato faltante), MSG2 (correo no registrado), MSG3 (contraseña incorrecta), MSG4 (cuenta desactivada), MSG5 (dispositivo no autorizado).
- **Trayectoria principal:** abrir app/web → pantalla de login → ingresar correo → ingresar contraseña → pulsar [Iniciar sesión] → validar campos no vacíos (si vacío → Alt. A) → verificar correo existente (si no existe → Alt. B) → verificar contraseña (si incorrecta → Alt. C) → verificar cuenta activa (si desactivada → Alt. D) → si móvil, verificar dispositivo autorizado (si no → Alt. E) → identificar rol → mostrar pantalla correspondiente (Dashboard móvil / Dashboard web / Módulo de administración).
- **Trayectorias alternativas:** A) campos vacíos → MSG1; B) correo no registrado → MSG2; C) contraseña incorrecta → MSG3; D) cuenta desactivada → MSG4; E) dispositivo no autorizado → MSG5.

**CU-G02 Recuperar contraseña / CU-G03 Restablecer contraseña / CU-G04 Cerrar sesión:** siguen el mismo formato de especificación (actor, propósito, entradas/salidas, pre/postcondiciones, errores, trayectorias principal y alternativas), gobernadas por las reglas de negocio RN-04 a RN-06 (recuperación) y RN-03 (persistencia de sesión móvil).

**Casos de uso consolidados (para evitar especificación repetitiva):**

**CU-M00 — Operación general de la aplicación móvil** (agrupa CU-M05 a CU-M30)
- **Actor:** Experto de campo.
- **Propósito:** acceder desde el dashboard móvil a captura/análisis, consulta local, mapa, notificaciones, perfil y configuración.
- **Precondiciones:** sesión iniciada; dispositivo asociado; SQLite disponible.
- **Postcondiciones:** función ejecutada; análisis generado se almacena localmente pendiente de sincronización; sincronización ascendente si hay conexión.
- **Funciones incluidas:** CU-M05 (resumen de jornada), CU-M06 (nuevo análisis), CU-M07 (registros recientes por estado), CU-M08 (historial local), CU-M09 (mapa local), CU-M10 (notificaciones), CU-M11 (perfil), CU-M12 (configuración).
- **Errores:** MSG13, MSG14, MSG17, MSG18, MSG19, MSG21, MSG25.
- **Observación clave:** operación offline-first; cada dispositivo tiene SQLite independiente; sincronización únicamente ascendente.
- **Trayectorias alternativas:** A) sin registros locales → valores en cero; B) error al consultar SQLite → mensaje de error; C) error de sincronización → marca error, permite reintento manual.

**CU-W00 — Consulta general de la plataforma web** (agrupa CU-W05 a CU-W23)
- **Actor:** Experto de campo.
- **Propósito:** consultar información consolidada en la nube desde el dashboard web.
- **Precondiciones:** sesión web iniciada; deben existir registros sincronizados.
- **Postcondiciones:** visualización sin modificar registros operativos.
- **Funciones incluidas:** CU-W05 (resumen general), CU-W06 (historial general), CU-W07 (mapa general), CU-W08 (estadísticas), CU-W09 (perfil).
- **Errores:** MSG26 (sin registros sincronizados), MSG27 (error al cargar), MSG28 (error al aplicar filtros).
- **Observación clave:** plataforma web = vista de consulta/supervisión, sin edición (sincronización solo ascendente).
- **Trayectorias alternativas:** A) sin registros sincronizados → MSG26; B) error al cargar → MSG27; C) error al aplicar filtros → MSG28, conserva vista anterior.

**CU-A00 — Gestionar cuentas y dispositivos desde la plataforma web** (agrupa CU-A01 a CU-A09)
- **Actor:** Administrador.
- **Propósito:** gestión administrativa de cuentas, roles y dispositivos.
- **Alcance:** no modifica registros operativos capturados desde móvil (solo información de acceso, roles y dispositivos autorizados); mantiene la sincronización estrictamente ascendente (sin necesidad de sincronización descendente hacia SQLite).
- **Funciones incluidas (referenciadas en diagramas):** CU-A01 (gestión general de cuentas), CU-A02 (crear cuenta), CU-A03 (editar cuenta), CU-A04 (desactivar cuenta), CU-A05 (asignar rol), CU-A06 (gestionar dispositivos asociados), CU-A07 (asociar dispositivo), CU-A08 (desasociar dispositivo), CU-A09 (consultar dispositivos asociados).
- **Errores relacionados:** MSG29–MSG35 (permisos, carga de usuarios, correo ya registrado, cambios de cuenta, dispositivo ya asociado/errores de asociación-desasociación).

#### 4.4.8 Diseño de interfaces (UI/UX)

**Aplicación móvil "EcoVerde"** — Material Design 3:
- **Paleta:** verde bosque `#2d6a3e` como color principal; código de color de estado (verde=sano, rojo=infestado, naranja=sin resultado definitivo/no determinado).
- **Tipografía:** Roboto Flex (oficial Material Design 3).
- Soporta tema claro y oscuro.
- **Navegación en 3 niveles:**
  1. Autenticación (correo/contraseña o cuenta Google; onboarding de 3 pasos).
  2. Navegación principal: barra inferior (Inicio, Mapa, Alertas, Perfil) + botón central de nueva captura.
  3. Flujo de captura (4 pasos): selección de tipo de muestra (corteza/follaje) → asociación a árbol (solo corteza) → captura con guía de encuadre → presentación de resultado.
- **Presentación de resultados:**
  - *Corteza:* clasificación (Sano/Infestado/Sin revisar) + índice de confianza + señales visuales detectadas + recomendación + pestaña de análisis cromático.
  - *Follaje:* solo análisis cromático (distribución % de superficie sana/regular/dañada/incierta), ya que es captura de zona, no de árbol individual; ubicación registrada en encabezado.

**Plataforma web:** misma paleta cromática e identidad visual que la app móvil (Material Design). Módulos: Autenticación (login/recuperación minimalista), Panel administrativo (tablas + modales para CRUD de cuentas/roles/dispositivos), Dashboard con sidebar (resumen, historial, mapa, estadísticas), Mapa interactivo con filtrado dinámico de focos de infestación, Visualizaciones D3.js (evolución temporal, distribución por especie/estado, análisis detallado por árbol con evidencia fotográfica y mapas de calor cromáticos).

#### 4.4.9 Diseño de base de datos

**Modelo Entidad-Relación (MER):** seis entidades principales:
- **cuenta:** usuarios del sistema y su rol.
- **dispositivo:** equipos móviles autorizados.
- **cuenta_dispositivo:** vincula cuentas con dispositivos y su estado de asociación.
- **árbol:** registros individuales del arbolado (corteza).
- **zona_follaje:** áreas geográficas de análisis macro (follaje).
- **análisis:** resultados de clasificación (corteza a nivel árbol, follaje a nivel zona).
- **análisis_cromático:** complementa cada análisis con porcentajes de coloración (OpenCV), según rangos NOM-019-SEMARNAT-2017.

**Esquema relacional:** 7 tablas — `CUENTA`, `DISPOSITIVO`, `CUENTA_DISPOSITIVO`, `ARBOL`, `ZONA_FOLLAJE`, `ANALISIS`, `ANALISIS_CROMATICO`. `ANALISIS` se asocia condicionalmente a `ARBOL` o `ZONA_FOLLAJE` según el tipo de captura.

**Diccionario de datos completo:**

*Tabla CUENTA*
| Campo | Tipo | Descripción |
|---|---|---|
| id_cuenta | UUID | PK. Identificador único de la cuenta. |
| correo | VARCHAR | Correo electrónico de login; único. |
| contrasena_hash | VARCHAR | Contraseña cifrada/hasheada. |
| rol | VARCHAR | Experto de campo o Administrador. |
| estado_cuenta | VARCHAR | Activa o desactivada. |
| fecha_creacion | TIMESTAMP | Fecha/hora de registro de la cuenta. |
| nombre_completo | VARCHAR | Nombre completo del usuario. |

*Tabla DISPOSITIVO*
| Campo | Tipo | Descripción |
|---|---|---|
| id_dispositivo | UUID | PK. Identificador único del dispositivo. |
| identificador_unico | VARCHAR | Identificador técnico del dispositivo móvil. |
| nombre_dispositivo | VARCHAR | Nombre descriptivo. |
| token_dispositivo | VARCHAR | Token de identificación técnica/notificaciones. |
| estado_dispositivo | VARCHAR | Activo, inactivo o bloqueado. |
| fecha_registro | TIMESTAMP | Fecha/hora de registro del dispositivo. |

*Tabla CUENTA_DISPOSITIVO*
| Campo | Tipo | Descripción |
|---|---|---|
| id_cuenta_dispositivo | UUID | PK. |
| id_cuenta | UUID | FK → CUENTA. |
| id_dispositivo | UUID | FK → DISPOSITIVO. |
| fecha_asociacion | TIMESTAMP | Fecha/hora de asociación. |
| fecha_desasociacion | TIMESTAMP | Fecha/hora de desasociación (si aplica). |
| estado_cuenta_dispositivo | VARCHAR | Activa o desasociada. |

*Tabla ARBOL*
| Campo | Tipo | Descripción |
|---|---|---|
| id_arbol | UUID | PK. |
| nombre_especifico | VARCHAR | Nombre/identificador asignado por el Experto. |
| especie | VARCHAR | Especie del árbol, si es conocida. |
| longitud | FLOAT | Coordenada de longitud. |
| latitud | FLOAT | Coordenada de latitud. |
| fecha_registro | TIMESTAMP | Fecha/hora de registro del árbol. |
| descripcion | TEXT | Observaciones generales. |
| id_cuenta | UUID | FK → CUENTA. |
| id_dispositivo | UUID | FK → DISPOSITIVO (dispositivo de registro). |

*Tabla ZONA_FOLLAJE*
| Campo | Tipo | Descripción |
|---|---|---|
| id_zona | UUID | PK. |
| latitud_centro | FLOAT | Latitud del punto central de la zona. |
| longitud_centro | FLOAT | Longitud del punto central de la zona. |
| fecha_registro | TIMESTAMP | Fecha/hora de registro/generación. |
| descripcion | TEXT | Descripción general de la zona. |
| nombre_zona | VARCHAR | Nombre descriptivo. |
| radio_metros | FLOAT | Radio aproximado de cobertura (metros). |
| id_dispositivo | UUID | FK → DISPOSITIVO. |
| id_cuenta | UUID | FK → CUENTA. |

*Tabla ANALISIS*
| Campo | Tipo | Descripción |
|---|---|---|
| id_analisis | UUID | PK. |
| ruta_evidencia | VARCHAR | Ruta de la imagen (local en SQLite / remota en la nube). |
| latitud_captura | FLOAT | Latitud exacta de captura. |
| longitud_captura | FLOAT | Longitud exacta de captura. |
| fecha_captura | TIMESTAMP | Fecha/hora de captura. |
| fecha_sincronizacion | TIMESTAMP | Fecha/hora de sincronización (vacío si pendiente). |
| estado_sincronizacion | VARCHAR | Pendiente, sincronizado o error. |
| tipo_imagen | VARCHAR | Follaje o corteza. |
| resultado_clasificacion | VARCHAR | Resultado del modelo/análisis. |
| confianza | FLOAT | Nivel de confianza del resultado. |
| observaciones | TEXT | Comentarios del Experto. |
| id_arbol | UUID | FK opcional → ARBOL (si es análisis de corteza). |
| id_zona | UUID | FK opcional → ZONA_FOLLAJE (si es análisis de follaje). |

*Tabla ANALISIS_CROMATICO*
| Campo | Tipo | Descripción |
|---|---|---|
| id_analisis_cromatico | UUID | PK. |
| pct_incierto | FLOAT | % de imagen clasificado como incierto. |
| pct_danado | FLOAT | % clasificado como dañado. |
| pct_regular | FLOAT | % clasificado como regular. |
| pct_sano | FLOAT | % clasificado como sano. |
| id_analisis | UUID | FK → ANALISIS. |

**Nota de diseño:** en `ANALISIS`, `id_arbol` e `id_zona` son mutuamente excluyentes y condicionales según el tipo de captura (corteza vs. follaje), evitando duplicar estructuras de tabla.


---

## 5. Tablas de datos, experimentos y resultados

### 5.1 Comparación de arquitecturas candidatas (resumen)

| Modelo | Parámetros | Tamaño | Top-1 ImageNet | TFLite |
|---|---|---|---|---|
| ResNet-50 | 25.6 M | 98 MB | 76.0% | Parcial |
| MobileNetV3 | 5.4 M | 4.9 MB | 75.2% | Nativo |
| EfficientNet-B0 | 5.3 M | 5.3 MB | 77.1% | Nativo |
| EfficientNet-B1 | 7.8 M | 7.8 MB | 79.1% | Nativo |
| **EfficientNet-B3 (seleccionado)** | **12.3 M** | **48 MB** | **81.6%** | **Nativo** |
| MobileViT-S | 5.6 M | 22 MB | 78.4% | Limitado |

### 5.2 Resultados de la prueba preliminar del modelo (Capítulo 7)

**Configuración:** EfficientNet-B3, clasificación binaria, 1,500 imágenes por clase (corteza sana / infestada), balanceadas manualmente. Objetivo: validar viabilidad del enfoque y establecer línea base (no modelo definitivo).

**Curvas de entrenamiento:** exactitud y AUC de entrenamiento mejoran sosteniblemente en ambas fases (feature extraction + fine-tuning), superando 90% en entrenamiento. Las métricas de validación muestran tendencia positiva pero con **brecha visible** respecto a entrenamiento (especialmente en fine-tuning) — indica aprendizaje útil pero **generalización limitada por volumen/diversidad de datos**, no por limitación de la arquitectura.

**Evaluación sobre conjunto de validación:**

| Métrica | Valor |
|---|---|
| Exactitud | 72% |
| Precisión | 80% |
| Recall (sensibilidad) | 40% |

**Interpretación:** cuando el modelo predice infestación, acierta 4 de 5 veces (precisión alta), pero solo detecta la mitad de los árboles realmente infestados (recall bajo) — modelo conservador, reduce falsos positivos a costa de más falsos negativos. **En este dominio, el recall es prioritario** (un árbol infestado no detectado implica riesgo de propagación).

**Predicciones sobre imágenes no vistas:** de 25 muestras, 7 clasificadas incorrectamente, todas con **confianza baja**. Aciertos con confianza 68–85%; errores concentrados cerca del umbral de decisión (51–62%) — el modelo "duda" en casos ambiguos en vez de cometer errores categóricos (señal positiva de representaciones parcialmente discriminativas).

**Proyección:** incrementar el dataset a **10,000–20,000 imágenes por clase** permitiría mejorar generalización, exactitud y recall.

### 5.3 Costos de infraestructura física del prototipo

| Concepto | Cantidad | Costo unitario (MXN) | Total (MXN) |
|---|---|---|---|
| Raspberry Pi + disco duro externo | 1 | $5,000.00 | $5,000.00 |
| Dron para captura de imágenes | 1 | $3,000.00 | $3,000.00 |
| Transporte a Chapultepec (metro, por integrante/visita) | 3 × 8 visitas | $10.00 | $240.00 |
| Consumo eléctrico (3 equipos, 8h/día, 8 meses) | — | — | $2,400.00 |
| Consumo eléctrico Raspberry Pi (continuo, 8 meses) | — | — | $320.00 |
| **Total** | | | **$10,960.00** |

*Supuestos:* 150W/equipo, 8h/día, ~200 días, $1.00 MXN/kWh (CFE doméstica); Raspberry Pi 5W continuos, 8 meses.

### 5.4 Costos de servicios AWS

**Capa gratuita aplicable (12 meses):** costo total **$0.00 USD** (ver detalle en §4.3.10).

**Estimación sin capa gratuita (AWS Pricing Calculator, us-east-1, 2026):**

| Servicio | Configuración | Mensual (USD) | Mensual (MXN) |
|---|---|---|---|
| AWS Lambda | ARM64, 100K invocaciones, 128 MB, 300ms | $0.00 | $0.00 |
| Amazon API Gateway | 100K llamadas REST/mes | $0.10 | $1.75 |
| Amazon RDS PostgreSQL | db.t2.micro, Single-AZ, 20 GB gp2 | $24.64 | $431.20 |
| Amazon S3 | 5 GB, 20K GET, 2K PUT | $0.13 | $2.28 |
| Amazon CloudFront | Free Plan | $0.00 | $0.00 |
| Amazon Cognito | 10 usuarios activos/mes | $1.00 | $17.50 |
| Amazon CloudWatch | 1 GB logs, 5 métricas | $2.00 | $35.00 |
| **Total mensual** | | **$27.87** | **$487.73** |
| **Total 12 meses** | | **$334.44** | **$5,852.70** |

RDS representa 88% del gasto mensual sin capa gratuita ($24.64/mes); con RDS cubierto por free tier, el costo real esperado es de **$3.23 USD/mes ($56.53 MXN/mes)**.

### 5.5 Resumen de costos totales del prototipo

| Categoría | Total estimado (MXN) |
|---|---|
| Infraestructura física (hardware) | $8,000.00 |
| Transporte y logística | $240.00 |
| Consumo eléctrico | $2,720.00 |
| Licencias de software | $0.00 |
| Servicios en la nube (AWS Free Tier) | $0.00 |
| **Total general** | **$10,960.00 MXN** |

≈ $3,653.33 MXN por integrante (equipo de 3).

### 5.6 Comparativa de proveedores de nube

**Servicios equivalentes:**

| Componente | AWS | Microsoft Azure | Google Cloud (GCP) |
|---|---|---|---|
| Funciones serverless | AWS Lambda | Azure Functions | Cloud Functions |
| API Gateway | Amazon API Gateway | Azure API Management | Cloud API Gateway / Apigee |
| Base de datos relacional | Amazon RDS | Azure SQL Database | Cloud SQL |
| Almacenamiento de objetos | Amazon S3 | Azure Blob Storage | Cloud Storage |
| CDN | Amazon CloudFront | Azure CDN | Cloud CDN |
| Autenticación | Amazon Cognito | Azure AD B2C | Firebase Authentication |
| ML / Versionado de modelos | Amazon SageMaker | Azure Machine Learning | Vertex AI |
| CI/CD | GitHub Actions + CodeDeploy | Azure DevOps | Cloud Build |
| Certificados SSL/TLS | AWS Certificate Manager | App Service Managed Certs | Google-managed SSL |
| Monitoreo | Amazon CloudWatch | Azure Monitor | Cloud Monitoring |
| WAF | AWS WAF | Azure WAF | Cloud Armor |

**Capa gratuita comparada:**

| Servicio | AWS | Azure | GCP |
|---|---|---|---|
| Funciones serverless | 1M invoc/mes siempre gratis | 1M/mes (12 meses) | 2M/mes siempre gratis |
| Almacenamiento objetos | 5 GB siempre gratis | 5 GB (12 meses) | 5 GB siempre gratis |
| Base de datos relacional | 750h db.t2.micro (12 meses) | 250 GB Azure SQL (12 meses) | 1 instancia db-f1-micro siempre gratis |
| CDN | 1 TB siempre gratis | 15 GB (12 meses) | 10 GB/mes siempre gratis |
| Autenticación | 50,000 MAU siempre gratis | 50,000 MAU siempre gratis | 10,000/mes (Firebase) |
| API Gateway | 1M llamadas/mes siempre gratis | Sin capa gratuita específica | 2M/mes (Cloud Endpoints) |
| Certificado SSL | Gratis ilimitado con AWS | Gratis solo en App Service | Gratis con servicios GCP |

### 5.7 Requerimientos, reglas de negocio y mensajes del sistema

> Ver tablas completas en §4.3.1 (Requerimientos Funcionales RF-01 a RF-28), §4.3.2 (Requerimientos No Funcionales RNF-01 a RNF-17), §4.3.3 (Reglas de Negocio RN-01 a RN-54) y §4.3.4 (Catálogo de Mensajes MSG1 a MSG35).

### 5.8 Diccionario de datos

> Ver tablas completas en §4.4.9 (Diseño de base de datos): CUENTA, DISPOSITIVO, CUENTA_DISPOSITIVO, ARBOL, ZONA_FOLLAJE, ANALISIS, ANALISIS_CROMATICO.


---

## 6. Conclusiones y trabajo futuro

### 6.1 Conclusiones

- El trabajo estableció las bases conceptuales, técnicas y metodológicas para un prototipo de sistema móvil de detección de infestación por insectos descortezadores mediante visión por computadora y aprendizaje profundo, respondiendo a la limitación de los métodos tradicionales de inspección visual (extensión de áreas verdes, falta de personal especializado, rapidez del deterioro).
- La selección del **Bosque de Chapultepec** permitió delimitar un escenario relevante ecológica, social y culturalmente, con infestación documentada pero no crítica (ideal para detección temprana). La caracterización de hospederos (fresnos, cedros blancos), agentes causantes (Hylesinus sp., Phloeosinus sp.) y señales visuales en follaje/corteza definió los elementos observables aprovechables computacionalmente.
- La **visión por computadora** es una alternativa viable para complementar el monitoreo forestal; el **aprendizaje profundo** es adecuado por su capacidad de aprender características visuales complejas desde datos etiquetados, particularmente en clasificación de imágenes.
- El análisis comparativo de arquitecturas identificó modelos de referencia adecuados (culminando en la selección de **EfficientNet-B3**) considerando precisión, eficiencia computacional e integrabilidad móvil. La definición del dataset, preprocesamiento y organización train/val/test son fundamentales para el desempeño del modelo.
- El diseño del sistema estableció una arquitectura de **app móvil + plataforma web + infraestructura AWS**, con requerimientos funcionales/no funcionales, reglas de negocio, actores, casos de uso, flujos de operación y diseño de base de datos, estructurando una solución integral tanto para análisis técnico como para implementación futura.
- **Viabilidad técnica confirmada**, condicionada a la calidad/cantidad/diversidad del dataset recolectado y a la validación con especialistas forestales. Limitaciones a atender: condiciones de iluminación natural, ángulo de captura, variabilidad entre especies, similitud visual con otros tipos de daño.
- Los resultados de esta primera etapa (Trabajo Terminal I) sientan las bases para el **Trabajo Terminal II**: implementación, integración y validación del prototipo (entrenamiento definitivo, desarrollo de app y web, despliegue en la nube, ejecución del plan de pruebas).

### 6.2 Trabajo a futuro (Sprints 3 y 4 — Trabajo Terminal II)

**1. Optimización del modelo y mitigación de riesgos:**
- Incrementar el dataset a **10,000–20,000 imágenes por clase**, mediante campañas intensivas de captura en distintas temporadas del año, condiciones de iluminación y ángulos, para reducir la brecha de generalización observada.
- Implementar **fine-tuning** adicional sobre EfficientNet-B3, priorizando la mejora del **recall** (sensibilidad) para minimizar falsos negativos en detección temprana.

**2. Implementación de la arquitectura Cloud y Offline-First:**
- Despliegue completo de la infraestructura serverless en AWS: API REST en Micronaut/Java 21 con Clean Architecture, persistencia políglota en RDS (PostgreSQL) y S3.
- Consolidación de la estrategia Offline-First en el cliente móvil: modelo de clasificación y análisis de color con OpenCV operando concurrentemente; sincronización en segundo plano con detección automática de red.

**3. Escalabilidad y proyección a ambiente productivo:**
- Transición desde los límites del AWS Free Tier hacia instancias de mayor capacidad (p.ej. db.t3.medium).
- Implementación de AWS WAF para protección ante amenazas externas.
- Arquitectura modular preparada para reentrenamiento en Amazon SageMaker con datos de todas las temporadas del año, habilitando replicabilidad en otros bosques y pulmones urbanos del país.

**4. Validación integral y documentación:**
- Ejecución del plan general de pruebas funcionales, de integración y de sistema.
- Sesiones de prueba con personal forestal operativo para identificar mejoras de usabilidad en la app Android y la plataforma web.


---

## 7. Glosario general (por capítulo)

### Capítulo 1. Planteamiento del proyecto
- **Plaga forestal:** organismo o conjunto de organismos que afectan negativamente la salud, crecimiento o supervivencia del arbolado.
- **Insectos descortezadores:** escarabajos que viven y se desarrollan debajo de la corteza de los árboles, alimentándose de tejidos conductores y provocando debilitamiento o muerte del árbol.
- **Detección temprana:** identificación de señales iniciales de una afectación antes de que el daño sea severo o irreversible.
- **Arbolado:** conjunto de árboles presentes en una zona determinada.
- **Monitoreo:** proceso de observación, registro y seguimiento continuo del estado de un fenómeno, sistema o recurso natural.
- **Visión por computadora:** área de la IA que permite a sistemas analizar e interpretar imágenes o videos para extraer información útil.
- **Inteligencia artificial:** campo de la computación orientado a sistemas capaces de tareas que normalmente requieren inteligencia humana.
- **Procesamiento de imágenes:** técnicas para modificar, mejorar o analizar imágenes digitales.
- **Aplicación móvil:** software diseñado para ejecutarse en dispositivos móviles.
- **Dataset:** conjunto de datos organizado usado para entrenar, validar o probar un modelo computacional.
- **Scrum:** metodología ágil de gestión de proyectos basada en ciclos iterativos (sprints).
- **Sprint:** periodo corto de trabajo dentro de Scrum.
- **Product Backlog:** lista priorizada de requerimientos/funcionalidades/tareas pendientes.
- **Sprint Backlog:** conjunto de tareas seleccionadas para un sprint específico.

### Capítulo 2. Contexto y caracterización del caso de estudio
- **Bosque urbano:** área verde con presencia significativa de árboles dentro o cerca de una zona urbana.
- **Servicios ecosistémicos:** beneficios de los ecosistemas a la sociedad (regulación de temperatura, calidad del aire, recreación).
- **Infestación:** presencia y desarrollo de organismos dañinos dentro o sobre un árbol.
- **Hospedero:** árbol/planta que sirve como alimento, refugio o sitio de reproducción a un organismo.
- **Agente biótico:** organismo vivo que influye sobre otro organismo o ecosistema.
- **Coleóptero:** insecto del orden Coleoptera, con élitros (alas anteriores endurecidas).
- **Curculionidae:** familia de coleópteros a la que pertenecen varios descortezadores.
- **Dendroctonus:** género de descortezadores de alta relevancia forestal, capaz de matar árboles.
- **Ips:** género de descortezadores asociado al ataque de árboles debilitados/estresados.
- **Hylesinus sp.:** descortezador asociado a fresnos.
- **Phloeosinus sp.:** descortezador asociado a cedros/Cupressaceae.
- **Fraxinus uhdei:** nombre científico del fresno (Chapultepec).
- **Cupressus lusitanica:** nombre científico del cedro blanco.
- **Corteza:** capa externa del tronco/ramas que protege tejidos internos.
- **Tejido subcortical:** región debajo de la corteza donde se forman galerías.
- **Floema:** tejido que transporta nutrientes de la fotosíntesis.
- **Xilema:** tejido que transporta agua y minerales desde raíces.
- **Resina:** sustancia viscosa de defensa ante heridas/ataques.
- **Grumos de resina:** acumulaciones visibles que indican perforaciones/ataques.
- **Galerías subcorticales:** túneles excavados por insectos bajo la corteza.
- **Orificios de entrada / salida:** perforaciones de ingreso / de emergencia de adultos.
- **Estrés hídrico:** condición de agua insuficiente para funciones fisiológicas normales.
- **Colonización:** proceso de ingreso y establecimiento de insectos en un hospedero.
- **Follaje:** conjunto de hojas/acículas, cuya coloración indica estado sanitario.
- **Dosel:** capa superior formada por las copas de los árboles.
- **Infestación activa:** estado con insectos vivos alimentándose/reproduciéndose.
- **Fase de infestación:** etapa distinguible por señales visuales (color de follaje/corteza).

### Capítulo 3. Delimitación computacional del problema
- **Información de entrada:** datos recibidos por el sistema (imágenes de árboles).
- **Imagen digital:** representación visual compuesta por píxeles.
- **Píxel:** unidad mínima de una imagen digital.
- **Clasificación de imágenes:** tarea que asigna una imagen a una categoría.
- **Detección de objetos:** tarea que localiza e identifica objetos en una imagen.
- **Segmentación de imágenes:** división de una imagen en regiones significativas.
- **Características visuales:** rasgos (color, textura, forma, patrones) usados para análisis.
- **Extracción de características:** identificación/obtención de rasgos relevantes.
- **Modelo computacional:** representación matemática/algorítmica que procesa datos.
- **Flujo de trabajo:** secuencia organizada de pasos para desarrollar/entrenar/evaluar/implementar un sistema.

### Capítulo 4. Fundamentación del enfoque de aprendizaje profundo
- **Aprendizaje automático:** rama de la IA que aprende patrones a partir de datos.
- **Aprendizaje supervisado:** entrenamiento con datos previamente etiquetados.
- **Clasificación:** asignación de una entrada a una clase definida.
- **Clase:** categoría/etiqueta esperada como resultado de predicción.
- **Aprendizaje profundo:** subcampo del ML basado en redes neuronales de múltiples capas.
- **Red neuronal:** modelo computacional inspirado en el cerebro, compuesto por neuronas artificiales.
- **Red neuronal convolucional:** red especializada en análisis de imágenes mediante convolución.
- **Datos anotados:** datos con etiquetas que indican la respuesta esperada de entrenamiento.
- **Entrenamiento:** ajuste de parámetros del modelo para aprender patrones.
- **Validación:** evaluación del modelo durante el desarrollo.
- **Prueba:** evaluación final con datos no usados en entrenamiento.
- **Generalización:** capacidad de producir resultados correctos ante datos nuevos.
- **Sobreajuste (overfitting):** el modelo aprende demasiado los datos de entrenamiento y pierde capacidad de generalizar.
- **Precisión:** proporción de predicciones correctas.
- **Predicción:** resultado generado por un modelo al analizar una entrada.
- **Inferencia:** uso de un modelo entrenado para generar predicciones sobre nuevos datos.

### Capítulo 5. Análisis comparativo de arquitecturas y selección del modelo
- **Arquitectura de modelo:** estructura interna definida por capas, conexiones y operaciones.
- **ResNet-50:** red profunda basada en conexiones residuales.
- **MobileNetV3:** red optimizada para móviles/recursos limitados.
- **EfficientNet:** familia que equilibra precisión y eficiencia computacional.
- **EfficientNet-B3:** variante de mayor capacidad de la familia, usada en este proyecto.
- **MobileViT:** combina CNNs con mecanismos tipo Transformer.
- **Transfer learning:** reutilización de un modelo preentrenado para una nueva tarea.
- **Fine-tuning:** ajuste fino de un modelo preentrenado con datos específicos.
- **Backbone:** red base que extrae características generales.
- **Modelo preentrenado:** modelo entrenado previamente con un dataset amplio, reutilizable.
- **ImageNet:** dataset ampliamente usado para entrenar/evaluar clasificadores de imágenes.
- **Convolución:** operación que detecta patrones locales (bordes, texturas, formas).
- **Batch Normalization:** normaliza valores intermedios para estabilizar/acelerar el entrenamiento.
- **ReLU:** función de activación que anula negativos y conserva positivos.
- **Pooling:** reduce el tamaño de mapas de características conservando información relevante.
- **Flatten:** convierte una matriz/mapa de características en un vector.
- **Capa densa:** capa donde cada neurona se conecta con todas las de la capa anterior.
- **Softmax:** convierte salidas del modelo en probabilidades por clase.
- **Parámetros del modelo:** valores internos ajustados durante el entrenamiento.
- **Escalado compuesto:** estrategia de EfficientNet para aumentar balanceadamente profundidad/ancho/resolución.

### Capítulo 6. Conjunto de datos y preprocesamiento
- **Recopilación de datos:** proceso de obtener imágenes/información para el dataset.
- **Conjunto de datos:** colección organizada para entrenar/validar/probar un modelo.
- **Etiquetado de imágenes:** asignación de clase/categoría a cada imagen.
- **Preprocesamiento:** operaciones aplicadas a los datos antes del entrenamiento.
- **Normalización:** ajuste de valores de imagen a un rango común.
- **Redimensionamiento:** modificación del tamaño de una imagen.
- **Aumentación de datos:** generación de variaciones de imágenes para incrementar diversidad.
- **Conjunto de entrenamiento / validación / prueba:** partes del dataset con sus respectivos propósitos.
- **Análisis cromático:** evaluación de colores para identificar patrones asociados al estado del árbol.
- **Coloración del follaje:** cambio visual en hojas/copa que indica fase de deterioro/infestación.

### Capítulo 7. Análisis del sistema
- **Requerimiento funcional / no funcional:** comportamiento requerido / condición de calidad del sistema.
- **Regla de negocio:** condición/restricción de comportamiento del sistema.
- **Actor del sistema:** usuario/entidad/componente externo que interactúa con el sistema.
- **Métrica de evaluación:** criterio cuantitativo para medir desempeño del modelo/sistema.
- **Criterio de aceptación:** condición para considerar válida una funcionalidad/entregable.
- **Riesgo técnico:** posible problema de implementación, funcionamiento o integración.
- **Mitigación:** acción planificada para reducir probabilidad/impacto de un riesgo.
- **Prueba funcional / de integración:** verifica una función / la comunicación entre módulos.
- **Infraestructura en la nube:** recursos computacionales de un proveedor de nube.
- **AWS / Azure / GCP:** plataformas de servicios en la nube (Amazon / Microsoft / Google).
- **Amazon SageMaker:** servicio AWS para desarrollo/entrenamiento/despliegue de modelos de ML.
- **AWS Lambda:** servicio serverless de AWS.
- **Amazon RDS:** servicio AWS para bases de datos relacionales.
- **PostgreSQL:** SGBD relacional de código abierto.
- **Serverless:** modelo de cómputo donde el proveedor administra la infraestructura.
- **Contenedor:** unidad de software que empaqueta una app y sus dependencias.
- **Capa gratuita:** servicios/recursos sin costo (limitados) de un proveedor de nube.
- **AWS Pricing Calculator:** herramienta de AWS para estimar costos.

### Capítulo 8. Diseño del sistema
- **Arquitectura del sistema:** organización general de componentes y sus relaciones.
- **Backend / Frontend:** lógica de negocio/procesamiento/BD / parte visual con la que interactúa el usuario.
- **API:** interfaz de comunicación entre componentes de software.
- **Clean Architecture:** patrón que organiza el software en capas independientes.
- **CI/CD:** integración continua y despliegue continuo (automatización de pruebas, build, release).
- **Observabilidad / Monitoreo centralizado:** conocer el estado interno del sistema mediante métricas/logs/trazas.
- **Offline-First:** estrategia que permite funcionar sin conexión a internet.
- **Sincronización:** actualización de datos locales/remotos para mantener consistencia.
- **Inferencia local:** ejecución del modelo directamente en el dispositivo móvil.
- **SPA:** aplicación web de una sola página, actualización dinámica sin recarga completa.
- **Autenticación / Control de acceso:** verificación de identidad / determinación de acciones/recursos permitidos.
- **Dashboard:** interfaz con información resumida, indicadores o accesos principales.
- **UI / UX:** interfaz de usuario / experiencia de usuario.
- **Caso de uso:** descripción de interacción entre un actor y el sistema para lograr un objetivo.
- **Modelo entidad-relación / Esquema relacional / Diccionario de datos:** representación conceptual / lógica (tablas) / documento descriptivo de una base de datos.
- **Entidad / Atributo:** objeto del mundo real representado en BD / propiedad de una entidad.
- **Llave primaria / Llave foránea:** identificador único de registro / relación entre tablas.
- **Persistencia / Persistencia políglota:** almacenamiento duradero de datos / uso de distintas tecnologías de almacenamiento según necesidad.
- **Análisis cromático:** evalúa información de color para apoyar la clasificación del estado del árbol.
- **Registro:** entrada almacenada con información de un árbol, análisis o usuario.

