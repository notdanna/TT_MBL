package com.descortezadores.ecoverde.presentation.features.analysis.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.descortezadores.ecoverde.domain.model.ClassificationOutcome
import com.descortezadores.ecoverde.domain.model.SampleType
import com.descortezadores.ecoverde.domain.model.SyncStatus
import com.descortezadores.ecoverde.domain.model.TargetAssociation
import com.descortezadores.ecoverde.domain.repository.SavedDetectionRecord
import com.descortezadores.ecoverde.presentation.features.analysis.AnalysisResultUiState
import com.descortezadores.ecoverde.presentation.features.analysis.HistoryFilterOption
import com.descortezadores.ecoverde.presentation.theme.EcoVerdeMotion
import com.descortezadores.ecoverde.presentation.theme.GreenPrimary
import com.descortezadores.ecoverde.presentation.theme.StatusHealthyGreen
import com.descortezadores.ecoverde.presentation.theme.StatusInfestedRed
import com.descortezadores.ecoverde.presentation.theme.StatusUncertainAmber

/**
 * Módulo 7: Centro de Alertas y Sincronización.
 *
 * Separación clara en dos secciones lógicas:
 * 1. Alertas fitosanitarias en campo (proximidad NOM-019 a menos de 100 m de focos activos).
 * 2. Cola de sincronización (Offline-First) con contador de pendientes y reintentos.
 * 3. Historial local con filtros.
 */
@Composable
fun AlertsAndSyncSection(
    uiState: AnalysisResultUiState,
    onSyncNowClick: () -> Unit,
    onHistoryFilterChanged: (HistoryFilterOption) -> Unit,
    onInspectRecordClick: (SavedDetectionRecord) -> Unit,
    onCenterMapOnCoordinatesClick: (Double, Double) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = EcoVerdeMotion.standardSpatialSpring()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ==========================================
        // 1. ALERTAS FITOSANITARIAS EN CAMPO
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(StatusInfestedRed.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = StatusInfestedRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Alertas Fitosanitarias en Campo",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Avisos de proximidad a focos fitosanitarios",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = StatusInfestedRed.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "${uiState.activeHotspots.size} focos activos",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = StatusInfestedRed,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Alerta activa de proximidad (menos de 100 metros)
                uiState.proximityAlert?.let { alert ->
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = Color(0xFFB71C1C),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "¡ALERTA DE PROXIMIDAD A FOCO ACTIVO!",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = alert.mensajeAlerta,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.95f)
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    val hotspot = uiState.activeHotspots.firstOrNull { it.idFoco == alert.idFoco }
                                    if (hotspot != null) {
                                        onCenterMapOnCoordinatesClick(hotspot.latitudCentro, hotspot.longitudCentro)
                                    }
                                },
                                shape = MaterialTheme.shapes.medium,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = BorderStroke(1.dp, Color.White)
                            ) {
                                Text("Ver en Mapa", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // Lista de focos activos en el bosque
                if (uiState.activeHotspots.isEmpty()) {
                    Text(
                        text = "No hay focos de infestación activos reportados a menos de 100 metros en este sector.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    uiState.activeHotspots.forEach { hotspot ->
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, StatusInfestedRed.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Foco #${hotspot.idFoco.take(8)} • ${hotspot.cantidadArbolesInfectados} árboles infestados",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusInfestedRed
                                    )
                                    Text(
                                        text = "Centro: ${String.format("%.5f, %.5f", hotspot.latitudCentro, hotspot.longitudCentro)} • Radio: ${hotspot.radioMetros.toInt()} m",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        onCenterMapOnCoordinatesClick(hotspot.latitudCentro, hotspot.longitudCentro)
                                    },
                                    shape = CircleShape
                                ) {
                                    Text("Centrar", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 2. COLA DE SINCRONIZACIÓN (OFFLINE-FIRST)
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GreenPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = GreenPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Cola de Sincronización",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Almacén local en el dispositivo",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Contador de elementos pendientes
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (uiState.daySummary.pendientesSincronizacion > 0) {
                            StatusUncertainAmber.copy(alpha = 0.2f)
                        } else {
                            StatusHealthyGreen.copy(alpha = 0.15f)
                        }
                    ) {
                        Text(
                            text = if (uiState.daySummary.pendientesSincronizacion > 0) {
                                "${uiState.daySummary.pendientesSincronizacion} pendientes"
                            } else {
                                "Al día (0 pendientes)"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.daySummary.pendientesSincronizacion > 0) {
                                Color(0xFFE65100)
                            } else {
                                StatusHealthyGreen
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Botón explícito "Sincronizar ahora"
                Button(
                    onClick = onSyncNowClick,
                    enabled = !uiState.isSyncingBackend,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (uiState.isSyncingBackend) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sincronizando con servidor...")
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.daySummary.pendientesSincronizacion > 0) {
                                "Sincronizar ahora (${uiState.daySummary.pendientesSincronizacion} pendientes)"
                            } else {
                                "Forzar reintento de sincronización"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Lista de capturas que tuvieron fallo de subida o pendientes con motivo
                val pendingOrErrorRecords = uiState.allRecords.filter { it.syncStatus != SyncStatus.SINCRONIZADO }
                if (pendingOrErrorRecords.isNotEmpty()) {
                    Text(
                        text = "Registros en cola o con fallo de subida:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100)
                    )

                    pendingOrErrorRecords.forEach { rec ->
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    val treeLabel = when (val assoc = rec.targetAssociation) {
                                        is TargetAssociation.CortezaTarget.ExistingTree -> assoc.nombreEspecifico
                                        is TargetAssociation.CortezaTarget.NewTree -> assoc.nombreArbolNuevo
                                        is TargetAssociation.FollajeTarget.ExistingZone -> assoc.nombreZona
                                        is TargetAssociation.FollajeTarget.NewZone -> assoc.nombreZonaNuevo
                                    }
                                    Text(
                                        text = "$treeLabel • ${rec.fechaCapturaIso.take(16).replace('T', ' ')}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val reason = when (rec.syncStatus) {
                                        SyncStatus.ERROR -> "Error de subida: Tiempo de espera agotado / Fallo de red"
                                        SyncStatus.PENDIENTE -> "Pendiente: En cola local esperando conexión"
                                        else -> "Sincronizado"
                                    }
                                    Text(
                                        text = reason,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (rec.syncStatus == SyncStatus.ERROR) StatusInfestedRed else Color(0xFFE65100)
                                    )
                                }

                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = if (rec.syncStatus == SyncStatus.ERROR) StatusInfestedRed.copy(alpha = 0.15f) else Color(0xFFE65100).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = rec.syncStatus.displayLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (rec.syncStatus == SyncStatus.ERROR) StatusInfestedRed else Color(0xFFE65100),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 3. HISTORIAL COMPLETO DE CAPTURAS
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Historial General de Inspecciones",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Chips de filtro
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HistoryFilterOption.entries.forEach { filter ->
                        FilterChip(
                            selected = uiState.historyFilter == filter,
                            onClick = { onHistoryFilterChanged(filter) },
                            shape = CircleShape,
                            label = { Text(filter.label) }
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                val records = uiState.filteredHistoryRecords
                if (records.isEmpty()) {
                    Text(
                        text = "No hay registros con el filtro seleccionado.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    records.forEach { record ->
                        HistoricalRecordRow(
                            record = record,
                            onClick = { onInspectRecordClick(record) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoricalRecordRow(
    record: SavedDetectionRecord,
    onClick: () -> Unit
) {
    val statusColor = when (record.classificationOutcome) {
        ClassificationOutcome.INFESTADO -> StatusInfestedRed
        ClassificationOutcome.SANO -> StatusHealthyGreen
        ClassificationOutcome.NO_DETERMINADO -> StatusUncertainAmber
    }

    val targetLabel = when (val assoc = record.targetAssociation) {
        is TargetAssociation.CortezaTarget.ExistingTree -> assoc.nombreEspecifico
        is TargetAssociation.CortezaTarget.NewTree -> assoc.nombreArbolNuevo
        is TargetAssociation.FollajeTarget.ExistingZone -> assoc.nombreZona
        is TargetAssociation.FollajeTarget.NewZone -> assoc.nombreZonaNuevo
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (record.sampleType == SampleType.CORTEZA) GreenPrimary.copy(alpha = 0.15f) else Color(0xFF00796B).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (record.sampleType == SampleType.CORTEZA) "Corteza" else "Follaje",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (record.sampleType == SampleType.CORTEZA) GreenPrimary else Color(0xFF00796B),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "$targetLabel • ${record.classificationOutcome.displayTitle}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    val subtitle = if (record.sampleType == SampleType.CORTEZA) {
                        "Certeza: ${(record.confidence * 100).toInt()}% • Fecha: ${record.fechaCapturaIso.take(16).replace('T', ' ')}"
                    } else {
                        val chrom = record.chromaticResult
                        val s = ((chrom?.pctSano ?: 0.0) * 100).toInt()
                        val r = ((chrom?.pctRegular ?: 0.0) * 100).toInt()
                        val d = ((chrom?.pctDanado ?: 0.0) * 100).toInt()
                        "Dosel: $s% sano, $r% estrés, $d% daño • ${record.fechaCapturaIso.take(16).replace('T', ' ')}"
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = MaterialTheme.shapes.small,
                color = when (record.syncStatus) {
                    SyncStatus.SINCRONIZADO -> StatusHealthyGreen.copy(alpha = 0.15f)
                    SyncStatus.PENDIENTE -> Color(0xFFE65100).copy(alpha = 0.15f)
                    SyncStatus.ERROR -> StatusInfestedRed.copy(alpha = 0.15f)
                }
            ) {
                Text(
                    text = record.syncStatus.displayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = when (record.syncStatus) {
                        SyncStatus.SINCRONIZADO -> StatusHealthyGreen
                        SyncStatus.PENDIENTE -> Color(0xFFE65100)
                        SyncStatus.ERROR -> StatusInfestedRed
                    },
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
