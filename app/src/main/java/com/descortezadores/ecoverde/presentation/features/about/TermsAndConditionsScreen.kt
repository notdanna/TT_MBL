package com.descortezadores.ecoverde.presentation.features.about

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.descortezadores.ecoverde.presentation.theme.GreenPrimary

/**
 * Pantalla de Términos y Condiciones, Políticas de Privacidad y Acerca de EcoVerde.
 *
 * Expone de manera transparente:
 * 1. Uso de la geolocalización satelital (GPS) y su almacenamiento vinculado a las evidencias.
 * 2. Uso de las imágenes capturadas para el reentrenamiento y mejora continua de los modelos de IA
 *    con garantía explícita de NO compartición con terceros ni comercialización.
 * 3. Protocolos de actuación bajo la NOM-019-SEMARNAT-2017 y carácter orientativo de los diagnósticos.
 * 4. Operación Offline-First y responsabilidad de sincronización.
 * 5. Trazabilidad de cuentas y seguridad institucional.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsAndConditionsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Términos y Privacidad",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Políticas de datos y Acerca de",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // -----------------------------------------------------------------
            // HERO CARD: Acerca del Proyecto EcoVerde
            // -----------------------------------------------------------------
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(GreenPrimary.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forest,
                                contentDescription = null,
                                tint = GreenPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "EcoVerde Mobile",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = GreenPrimary
                            )
                            Text(
                                text = "Versión 1.0.0 • TT 2026-B161",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = GreenPrimary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "NOM-019",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = GreenPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "Sistema móvil para la detección temprana, evaluación fitosanitaria y mapeo de insectos descortezadores (Dendroctonus spp. e Ips spp.) en coníferas mexicanas, desarrollado con arquitectura limpia, IA Edge (TFLite) y Cloud (EfficientNet-B3).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // -----------------------------------------------------------------
            // CLÁUSULA 1: Geolocalización y Registro Satelital GPS
            // -----------------------------------------------------------------
            TermsClauseCard(
                icon = Icons.Default.MyLocation,
                iconTint = Color(0xFF1565C0),
                title = "1. Uso y Almacenamiento de Ubicación GPS",
                summary = "Uso imprescindible de coordenadas para georreferenciación y trazabilidad fitosanitaria.",
                content = "La aplicación recopila las coordenadas geográficas de alta precisión del dispositivo (latitud, longitud satelital con al menos 5 decimales y margen de precisión en metros) al registrar árboles, tomas de follaje y capturas de corteza.\n\n" +
                    "• Almacenamiento: Estas coordenadas se almacenan en la base de datos local del dispositivo (Room SQLite) y se transmiten al servidor centralizado (PostgreSQL / PostGIS) durante el proceso de sincronización.\n\n" +
                    "• Finalidad Fitosanitaria Obligatoria: De acuerdo con la norma oficial mexicana NOM-019-SEMARNAT-2017, la geolocalización es obligatoria para delimitar automáticamente la zona de alerta de 100 metros a la redonda ante cualquier diagnóstico de infestación, permitiendo inspeccionar árboles hospederos adyacentes y planificar brigadas de saneamiento en campo."
            )

            // -----------------------------------------------------------------
            // CLÁUSULA 2: Uso de Imágenes y Entrenamiento Continuo del Modelo
            // -----------------------------------------------------------------
            TermsClauseCard(
                icon = Icons.Default.Psychology,
                iconTint = GreenPrimary,
                title = "2. Uso de Imágenes y Mejora del Modelo de IA",
                summary = "Tus fotografías mejoran los algoritmos de detección. No se comparten ni comercializan.",
                content = "Las fotografías de corteza y dosel capturadas o importadas por el biólogo o brigadista se emplean para el análisis fitosanitario inmediato y son resguardadas en el almacenamiento seguro de la plataforma (Amazon S3 / Servidor institucional).\n\n" +
                    "• Reentrenamiento de Modelos: Las evidencias fotográficas válidas se utilizarán para el aprendizaje continuo, calibración de umbrales y mejora de precisión de los modelos de visión artificial (EfficientNet-B3 y LiteRT).\n\n" +
                    "• Privacidad y No Compartición con Terceros: Garantizamos que tus imágenes fotográficas NUNCA serán vendidas, alquiladas, compartidas con terceros, redes sociales, empresas de publicidad ni entidades con fines de lucro. Su uso es estrictamente institucional para la conservación y salud forestal."
            )

            // -----------------------------------------------------------------
            // CLÁUSULA 3: Carácter Asistencial y Alcance de los Diagnósticos
            // -----------------------------------------------------------------
            TermsClauseCard(
                icon = Icons.Default.Security,
                iconTint = Color(0xFFE65100),
                title = "3. Alcance Técnico y Dictamen Biológico",
                summary = "La IA asiste la toma de decisiones, pero no sustituye la notificación legal oficial.",
                content = "Los diagnósticos generados (Sano, Infestado, No Determinado) y los índices cromáticos de degradación de follaje son herramientas de apoyo a la decisión técnica.\n\n" +
                    "• Supervisión Oficial: Las alertas generadas no constituyen por sí solas una Notificación de Saneamiento Forestal oficial emitida por SEMARNAT o CONAFOR. Cualquier acción de derribo, troceo, descortezado o aplicación de químicos debe ser validada por el biólogo o técnico responsable en campo."
            )

            // -----------------------------------------------------------------
            // CLÁUSULA 4: Modo Offline y Sincronización de Datos
            // -----------------------------------------------------------------
            TermsClauseCard(
                icon = Icons.Default.CloudUpload,
                iconTint = Color(0xFF00897B),
                title = "4. Operación Offline-First y Sincronización",
                summary = "Tus datos se guardan de forma local y segura hasta conectarse a la red.",
                content = "EcoVerde Mobile está diseñada para trabajar sin cobertura en alta montaña. Todas las inspecciones, fichas técnicas y fotografías se almacenan localmente en la memoria privada de la aplicación.\n\n" +
                    "• Responsabilidad de Sincronización: El brigadista debe efectuar la sincronización de datos ('Sincronizar ahora') al reingresar a una zona con conexión Wi-Fi o datos móviles para integrar las capturas al inventario forestal central y al tablero analítico."
            )

            // -----------------------------------------------------------------
            // CLÁUSULA 5: Seguridad de Credenciales y Trazabilidad de Auditoría
            // -----------------------------------------------------------------
            TermsClauseCard(
                icon = Icons.Default.VerifiedUser,
                iconTint = Color(0xFF5E35B1),
                title = "5. Credenciales y Trazabilidad Institucional",
                summary = "Cuentas personales e intransferibles para garantizar la integridad del censo.",
                content = "El acceso mediante correo institucional y token seguro JWT es de carácter personal para los biólogos y brigadistas asignados.\n\n" +
                    "• Cada árbol y análisis registrado queda asociado al identificador único de dispositivo y al biólogo que realizó la inspección para auditoría interna y trazabilidad fitosanitaria conforme a los protocolos del Bosque de Chapultepec y autoridades ambientales."
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Botón de conformidad / regreso
            Button(
                onClick = onBackClick,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "Entendido y regresar",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Tarjeta modular para desplegar cada cláusula de los términos y condiciones.
 */
@Composable
private fun TermsClauseCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    summary: String,
    content: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.25f
            )
        }
    }
}
