package com.descortezadores.ecoverde.presentation.features.analysis.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.descortezadores.ecoverde.domain.model.ClassificationOutcome
import com.descortezadores.ecoverde.domain.model.SampleType
import com.descortezadores.ecoverde.domain.model.SyncStatus
import com.descortezadores.ecoverde.domain.model.TargetAssociation
import com.descortezadores.ecoverde.domain.model.TreeRecord
import com.descortezadores.ecoverde.domain.repository.SavedDetectionRecord
import com.descortezadores.ecoverde.presentation.features.analysis.AnalysisResultUiState
import com.descortezadores.ecoverde.presentation.features.analysis.HistoryFilterOption
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
    onCenterMapOnCoordinatesClick: (Double, Double) -> Unit,
    onFocusTreeOnMap: (TreeRecord) -> Unit = {},
    listState: LazyListState = rememberLazyListState()
) {
    val surfaceContainerColor = MaterialTheme.colorScheme.surfaceContainer
    val outlineVariantColor = MaterialTheme.colorScheme.outlineVariant
    val filterRowScrollState = rememberScrollState()

    val pendingOrErrorRecords = remember(uiState.allRecords) {
        uiState.allRecords.filter { it.syncStatus != SyncStatus.SINCRONIZADO }
    }
    val records = remember(uiState.allRecords, uiState.historyFilter) {
        uiState.filteredHistoryRecords
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp)
    ) {
        // ==========================================
        // 1. ALERTAS FITOSANITARIAS EN CAMPO
        // ==========================================
        item(key = "alerts_header", contentType = "card_header") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(surfaceContainerColor)
                    .drawBehind {
                        val stroke = 1.dp.toPx()
                        drawLine(outlineVariantColor, Offset(0f, stroke / 2f), Offset(size.width, stroke / 2f), stroke)
                        drawLine(outlineVariantColor, Offset(stroke / 2f, 0f), Offset(stroke / 2f, size.height), stroke)
                        drawLine(outlineVariantColor, Offset(size.width - stroke / 2f, 0f), Offset(size.width - stroke / 2f, size.height), stroke)
                    }
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 10.dp)
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
                        val totalNearbyAlerts = uiState.activeHotspots.size + uiState.nearbyInfestedTrees.size
                        Text(
                            text = "$totalNearbyAlerts alertas (<100 m)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = StatusInfestedRed,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        if (uiState.nearbyInfestedTrees.isNotEmpty()) {
            items(
                items = uiState.nearbyInfestedTrees,
                key = { "nearby-${it.idArbol}" },
                contentType = { "nearby_tree" }
            ) { nearbyTree ->
                CardSideWrapper(surfaceContainerColor, outlineVariantColor) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, StatusInfestedRed.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${nearbyTree.nombreArbol} • ${nearbyTree.distanciaMetros.toInt()} m",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusInfestedRed
                                )
                                Text(
                                    text = "Árbol infestado dentro de radio de 100 m (${String.format("%.5f, %.5f", nearbyTree.latitud, nearbyTree.longitud)})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    val targetTree = uiState.treeProgressionList
                                        .firstOrNull { it.tree.idArbol == nearbyTree.idArbol }
                                        ?.tree
                                        ?: TreeRecord(
                                            idArbol = nearbyTree.idArbol,
                                            nombreEspecifico = nearbyTree.nombreArbol,
                                            especie = "Fresno",
                                            latitud = nearbyTree.latitud,
                                            longitud = nearbyTree.longitud,
                                            descripcion = null,
                                            fechaRegistroIso = "",
                                            idCuenta = "",
                                            idDispositivo = "",
                                            ultimoEstadoSanitario = ClassificationOutcome.INFESTADO
                                        )
                                    onFocusTreeOnMap(targetTree)
                                },
                                shape = CircleShape
                            ) {
                                Text("Ver en Mapa", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        if (uiState.activeHotspots.isEmpty() && uiState.nearbyInfestedTrees.isEmpty()) {
            item(key = "alerts_empty", contentType = "empty_state") {
                CardSideWrapper(surfaceContainerColor, outlineVariantColor) {
                    Text(
                        text = "No hay árboles infestados ni focos activos reportados a menos de 100 metros de tu ubicación.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (uiState.activeHotspots.isNotEmpty()) {
            items(
                items = uiState.activeHotspots,
                key = { "hotspot-${it.idFoco}" },
                contentType = { "hotspot" }
            ) { hotspot ->
                CardSideWrapper(surfaceContainerColor, outlineVariantColor) {
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

        item(key = "alerts_footer", contentType = "card_footer") {
            CardFooter(surfaceContainerColor, outlineVariantColor)
            Spacer(modifier = Modifier.height(14.dp))
        }

        // ==========================================
        // 2. HISTORIAL COMPLETO DE CAPTURAS
        // ==========================================
        item(key = "history_header", contentType = "card_header") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(surfaceContainerColor)
                    .drawBehind {
                        val stroke = 1.dp.toPx()
                        drawLine(outlineVariantColor, Offset(0f, stroke / 2f), Offset(size.width, stroke / 2f), stroke)
                        drawLine(outlineVariantColor, Offset(stroke / 2f, 0f), Offset(stroke / 2f, size.height), stroke)
                        drawLine(outlineVariantColor, Offset(size.width - stroke / 2f, 0f), Offset(size.width - stroke / 2f, size.height), stroke)
                    }
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Historial General de Inspecciones",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(filterRowScrollState),
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

                    HorizontalDivider(color = outlineVariantColor)
                }
            }
        }

        if (records.isEmpty()) {
            item(key = "history_empty", contentType = "empty_state") {
                CardSideWrapper(surfaceContainerColor, outlineVariantColor) {
                    Text(
                        text = "No hay registros con el filtro seleccionado.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(
                items = records,
                key = { "history-${it.idAnalisis}" },
                contentType = { "history_row" }
            ) { record ->
                CardSideWrapper(surfaceContainerColor, outlineVariantColor) {
                    HistoricalRecordRow(
                        record = record,
                        onClick = { onInspectRecordClick(record) }
                    )
                }
            }
        }

        item(key = "history_footer", contentType = "card_footer") {
            CardFooter(surfaceContainerColor, outlineVariantColor)
            Spacer(modifier = Modifier.height(14.dp))
        }

        // ==========================================
        // 3. COLA DE SINCRONIZACIÓN (OFFLINE-FIRST)
        // ==========================================
        item(key = "sync_header", contentType = "card_header") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(surfaceContainerColor)
                    .drawBehind {
                        val stroke = 1.dp.toPx()
                        drawLine(outlineVariantColor, Offset(0f, stroke / 2f), Offset(size.width, stroke / 2f), stroke)
                        drawLine(outlineVariantColor, Offset(stroke / 2f, 0f), Offset(stroke / 2f, size.height), stroke)
                        drawLine(outlineVariantColor, Offset(size.width - stroke / 2f, 0f), Offset(size.width - stroke / 2f, size.height), stroke)
                    }
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                    "Sincronización manual"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (pendingOrErrorRecords.isNotEmpty()) {
                        Text(
                            text = "Registros en cola o con fallo de subida:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100)
                        )
                    }
                }
            }
        }

        if (pendingOrErrorRecords.isNotEmpty()) {
            items(
                items = pendingOrErrorRecords,
                key = { "pending-${it.idAnalisis}" },
                contentType = { "pending_record" }
            ) { rec ->
                CardSideWrapper(surfaceContainerColor, outlineVariantColor) {
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

        item(key = "sync_footer", contentType = "card_footer") {
            CardFooter(surfaceContainerColor, outlineVariantColor)
        }
    }
}

@Composable
private fun CardSideWrapper(
    surfaceContainerColor: Color,
    outlineVariantColor: Color,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceContainerColor)
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawLine(outlineVariantColor, Offset(stroke / 2f, 0f), Offset(stroke / 2f, size.height), stroke)
                drawLine(outlineVariantColor, Offset(size.width - stroke / 2f, 0f), Offset(size.width - stroke / 2f, size.height), stroke)
            }
            .padding(horizontal = 16.dp, vertical = 5.dp)
    ) {
        content()
    }
}

@Composable
private fun CardFooter(
    surfaceContainerColor: Color,
    outlineVariantColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            .background(surfaceContainerColor)
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawLine(outlineVariantColor, Offset(0f, size.height - stroke / 2f), Offset(size.width, size.height - stroke / 2f), stroke)
                drawLine(outlineVariantColor, Offset(stroke / 2f, 0f), Offset(stroke / 2f, size.height), stroke)
                drawLine(outlineVariantColor, Offset(size.width - stroke / 2f, 0f), Offset(size.width - stroke / 2f, size.height), stroke)
            }
    )
}

@Composable
private fun HistoricalRecordRow(
    record: SavedDetectionRecord,
    onClick: () -> Unit
) {
    val chrom = record.chromaticResult
    val sanoPct = ((chrom?.pctSano ?: 0.0) * 100).toInt()
    val stressPct = ((chrom?.pctRegular ?: 0.0) * 100).toInt()
    val damagedPct = ((chrom?.pctDanado ?: 0.0) * 100).toInt()

    val statusColor = if (record.sampleType == SampleType.CORTEZA) {
        when (record.classificationOutcome) {
            ClassificationOutcome.INFESTADO -> StatusInfestedRed
            ClassificationOutcome.SANO -> StatusHealthyGreen
            ClassificationOutcome.NO_DETERMINADO -> StatusUncertainAmber
        }
    } else {
        when {
            damagedPct >= 30 -> StatusInfestedRed
            stressPct >= 30 || damagedPct >= 15 -> StatusUncertainAmber
            else -> StatusHealthyGreen
        }
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
                            text = if (record.sampleType == SampleType.CORTEZA) {
                                "$targetLabel • ${record.classificationOutcome.displayTitle}"
                            } else {
                                "$targetLabel • Análisis de dosel"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    val subtitle = if (record.sampleType == SampleType.CORTEZA) {
                        "Certeza: ${(record.confidence * 100).toInt()}% • Fecha: ${record.fechaCapturaIso.take(16).replace('T', ' ')}"
                    } else {
                        "Dosel: $sanoPct% sano, $stressPct% estrés, $damagedPct% daño • ${record.fechaCapturaIso.take(16).replace('T', ' ')}"
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
