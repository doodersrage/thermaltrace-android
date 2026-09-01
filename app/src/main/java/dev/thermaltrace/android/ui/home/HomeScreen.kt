package dev.thermaltrace.android.ui.home

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.thermaltrace.android.data.insights.HeatingInsight
import dev.thermaltrace.android.data.model.LiveSensor
import dev.thermaltrace.android.data.model.NwsAlert
import dev.thermaltrace.android.data.model.NightAtRisk
import dev.thermaltrace.android.ui.theme.brandTitle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(brandTitle()) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.loading -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                val readings = state.readings
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Text(
                            text = "Live readings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = readings?.updatedAt?.let { "Updated $it" }
                                ?: "From thermaltrace.dev",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        state.outdoorTempF?.let { outdoor ->
                            Text(
                                text = "Outdoor ~${"%.0f".format(outdoor)}°F",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    state.ingestStatus?.takeIf { it.waiting }?.let { ingest ->
                        item {
                            IngestWaitingBanner(ingest)
                        }
                    }

                    state.homeInsights?.nwsAlerts?.takeIf { it.isNotEmpty() }?.let { alerts ->
                        item {
                            Text(
                                "Weather alerts",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        items(alerts, key = { it.headline + it.event }) { alert ->
                            NwsAlertCard(alert)
                        }
                    }

                    state.homeInsights?.nightsAtRisk?.takeIf { it.isNotEmpty() }?.let { nights ->
                        item {
                            Text(
                                "Nights at risk",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "Forecast freeze risk for the next 7 nights.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        items(nights, key = { it.dateLabel }) { night ->
                            NightAtRiskRow(night)
                        }
                    }

                    state.doorSessions.takeIf { it.isNotEmpty() }?.let { sessions ->
                        item {
                            Text(
                                "Door open sessions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        items(sessions, key = { it.label + it.openedAt }) { session ->
                            DoorSessionRow(session)
                        }
                    }

                    if (state.insights.isNotEmpty()) {
                        item {
                            Text(
                                "Heating insights",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "Temperature and humidity vs outdoor conditions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        items(state.insights, key = { it.label + it.detail }) { insight ->
                            InsightCard(insight)
                        }
                    }

                    val spaces = readings?.spaces.orEmpty()
                    if (spaces.isNotEmpty()) {
                        item {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                FilterChip(
                                    selected = state.selectedSpace == null,
                                    onClick = { viewModel.selectSpace(null) },
                                    label = { Text("All spaces") },
                                )
                                spaces.forEach { space ->
                                    FilterChip(
                                        selected = state.selectedSpace == space,
                                        onClick = { viewModel.selectSpace(space) },
                                        label = { Text(space) },
                                    )
                                }
                            }
                        }
                    }

                    state.error?.let { message ->
                        item {
                            Text(
                                text = message,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }

                    val sensors = readings?.sensors.orEmpty()
                    if (sensors.isEmpty() && state.error == null) {
                        item {
                            Text(
                                text = "No sensors yet. Add devices on thermaltrace.dev.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    items(sensors, key = { "${it.deviceId}:${it.key}:${it.kind}" }) { sensor ->
                        SensorCard(sensor)
                    }

                    if (state.refreshing) {
                        item {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                CircularProgressIndicator(strokeWidth = 2.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightCard(insight: HeatingInsight) {
    val warning = insight.severity == HeatingInsight.Severity.Warning
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (warning) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = RoundedCornerShape(8.dp),
            )
            .padding(12.dp),
    ) {
        Text(insight.label, fontWeight = FontWeight.SemiBold)
        Text(
            insight.detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun SensorCard(sensor: LiveSensor) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = sensor.label.ifBlank { sensor.key },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = listOfNotNull(sensor.deviceName, sensor.space).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = formatSensorValue(sensor),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        AssistChip(
            onClick = {},
            enabled = false,
            label = { Text(sensor.kind) },
        )
    }
}

private fun formatSensorValue(sensor: LiveSensor): String {
    sensor.temp?.let { sample ->
        val humidity = if (sample.h > 0) " · ${sample.h.toInt()}% RH" else ""
        return "${formatNumber(sample.f)}°F (${formatNumber(sample.c)}°C)$humidity"
    }
    sensor.valueBool?.let { return if (it) "Open / On" else "Closed / Off" }
    sensor.valueText?.takeIf { it.isNotBlank() }?.let { return it }
    sensor.valueNum?.let { num ->
        val unit = sensor.unit?.takeIf { it.isNotBlank() }?.let { " $it" } ?: ""
        return "${formatNumber(num)}$unit"
    }
    return "—"
}

private fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else String.format("%.1f", value)

@Composable
private fun IngestWaitingBanner(ingest: dev.thermaltrace.android.data.model.IngestStatusResponse) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.tertiary,
                shape = RoundedCornerShape(8.dp),
            )
            .padding(12.dp),
    ) {
        Text("Waiting for first POST", fontWeight = FontWeight.SemiBold)
        Text(
            "${ingest.waitingCount} device(s) created but no data received yet.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        ingest.devices.take(3).forEach { device ->
            Text(
                "${device.name}: ${device.sensorKeys.joinToString()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NwsAlertCard(alert: NwsAlert) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.error,
                shape = RoundedCornerShape(8.dp),
            )
            .padding(12.dp),
    ) {
        Text(alert.event, fontWeight = FontWeight.SemiBold)
        Text(alert.headline, style = MaterialTheme.typography.bodySmall)
        Text(
            listOfNotNull(alert.severity, alert.expires?.let { "Expires $it" }).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NightAtRiskRow(night: NightAtRisk) {
    Text(
        "${night.dateLabel}: ${"%.1f".format(night.minTempF)}°F" +
            if (night.atRisk) " — at risk" else "",
        color = if (night.atRisk) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun DoorSessionRow(session: dev.thermaltrace.android.data.model.DoorSession) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(session.label, fontWeight = FontWeight.Medium)
        Text(
            buildString {
                append("Opened ${session.openedAt}")
                if (session.stillOpen) append(" · still open")
                session.durationMs?.let { append(" · ${formatDurationMs(it)}") }
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatDurationMs(ms: Long): String {
    val minutes = ms / 60_000
    val hours = minutes / 60
    return when {
        hours > 0 -> "${hours}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m"
        else -> "${ms / 1000}s"
    }
}
