package dev.thermaltrace.android.ui.history

import android.content.Intent
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.thermaltrace.android.data.model.ChartPointDto
import dev.thermaltrace.android.ui.theme.BrandTrace
import dev.thermaltrace.android.ui.theme.brandTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.claimsFile) {
        val file = state.claimsFile ?: return@LaunchedEffect
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "text/html")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(Intent.createChooser(intent, "Open claims pack")) }
        viewModel.clearClaimsFile()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(brandTitle()) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
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
            state.loading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text("History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 7, 30).forEach { days ->
                            FilterChip(
                                selected = state.days == days,
                                onClick = { viewModel.refresh(days) },
                                label = {
                                    Text(
                                        when (days) {
                                            1 -> "24h"
                                            7 -> "7d"
                                            else -> "30d"
                                        },
                                    )
                                },
                            )
                        }
                    }
                }
                state.error?.let {
                    item { Text(it, color = MaterialTheme.colorScheme.error) }
                }
                item {
                    Text("Claims / insurance pack", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Printable HTML for the selected window (Pro). Open and Print → Save as PDF.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = viewModel::downloadClaimsPack,
                        enabled = !state.claimsBusy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (state.claimsBusy) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.height(18.dp))
                        } else {
                            Text("Download claims pack")
                        }
                    }
                    state.claimsMessage?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
                item {
                    Text("Temperature (°F)", style = MaterialTheme.typography.titleMedium)
                    if (state.points.isEmpty()) {
                        Text("No chart points in this window.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Sparkline(state.points)
                        val temps = state.points.map { it.tempf }
                        Text(
                            "Min ${temps.minOrNull()?.let { "%.1f".format(it) }}°F · Max ${temps.maxOrNull()?.let { "%.1f".format(it) }}°F",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                item { Text("Recent readings", style = MaterialTheme.typography.titleMedium) }
                if (state.readings.isEmpty()) {
                    item {
                        Text("No readings.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                items(state.readings, key = { "${it.timestamp}-${it.probeKey}-${it.tempf}" }) { row ->
                    Column {
                        Text(
                            listOfNotNull(row.probeLabel, row.feedName).joinToString(" · ")
                                .ifBlank { "Reading" },
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            "${"%.1f".format(row.tempf)}°F · ${row.humidity.toInt()}% RH · ${row.timestamp}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun Sparkline(points: List<ChartPointDto>) {
    val color = BrandTrace
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(140.dp)
            .padding(vertical = 8.dp),
    ) {
        if (points.size < 2) return@Canvas
        val temps = points.map { it.tempf }
        val min = temps.minOrNull() ?: return@Canvas
        val max = temps.maxOrNull() ?: return@Canvas
        val range = (max - min).takeIf { it > 0.01 } ?: 1.0
        val path = Path()
        points.forEachIndexed { index, point ->
            val x = size.width * index / (points.size - 1).toFloat()
            val y = size.height * (1f - ((point.tempf - min) / range).toFloat())
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 4f, cap = StrokeCap.Round),
        )
        val last = points.last()
        val lastX = size.width
        val lastY = size.height * (1f - ((last.tempf - min) / range).toFloat())
        drawCircle(color = color, radius = 6f, center = Offset(lastX, lastY))
    }
}
