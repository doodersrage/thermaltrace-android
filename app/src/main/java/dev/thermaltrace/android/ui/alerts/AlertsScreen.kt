package dev.thermaltrace.android.ui.alerts

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.thermaltrace.android.data.model.AlertEventDto
import dev.thermaltrace.android.data.model.AlertSettingsDto
import dev.thermaltrace.android.ui.theme.brandTitle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AlertsScreen(viewModel: AlertsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(brandTitle()) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.tab == AlertsTab.Inbox,
                    onClick = { viewModel.selectTab(AlertsTab.Inbox) },
                    label = {
                        Text(
                            if (state.unackedCount > 0) "Inbox (${state.unackedCount})"
                            else "Inbox",
                        )
                    },
                )
                FilterChip(
                    selected = state.tab == AlertsTab.Settings,
                    onClick = { viewModel.selectTab(AlertsTab.Settings) },
                    label = { Text("Settings") },
                )
            }
            Spacer(Modifier.height(8.dp))
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.message?.let { Text(it, color = MaterialTheme.colorScheme.tertiary) }

            when {
                state.loading -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }

                state.tab == AlertsTab.Inbox -> InboxPane(
                    events = state.events,
                    saving = state.saving,
                    onAck = viewModel::acknowledge,
                    onTest = viewModel::sendTest,
                )

                else -> SettingsPane(viewModel = viewModel, settings = state.settings, saving = state.saving)
            }
        }
    }
}

@Composable
private fun InboxPane(
    events: List<AlertEventDto>,
    saving: Boolean,
    onAck: (Long) -> Unit,
    onTest: () -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Recent alerts", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "Acknowledge events and send a channel test.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onTest, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
                Text("Send test alert")
            }
        }
        if (events.isEmpty()) {
            item {
                Text("No alert events yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(events, key = { it.id }) { event ->
            Column(Modifier.fillMaxWidth()) {
                Text(event.title, fontWeight = FontWeight.SemiBold)
                Text(
                    listOfNotNull(event.kind, event.createdAt).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(event.body, style = MaterialTheme.typography.bodyMedium)
                if (event.channelsSent.isNotEmpty()) {
                    Text(
                        "Sent: ${event.channelsSent.joinToString()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (event.acknowledgedAt == null) {
                    TextButton(
                        onClick = { onAck(event.id) },
                        enabled = !saving,
                    ) { Text("Acknowledge") }
                } else {
                    Text(
                        "Acknowledged",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsPane(
    viewModel: AlertsViewModel,
    settings: AlertSettingsDto,
    saving: Boolean,
) {
    val s = settings
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Alert settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

        SettingSwitch("Alerts enabled", s.enabled) {
            viewModel.update { it.copy(enabled = !it.enabled) }
        }
        SettingSwitch("Email digest", s.digestEnabled) {
            viewModel.update { it.copy(digestEnabled = !it.digestEnabled) }
        }
        SettingSwitch("Forecast freeze alerts", s.forecastFreezeEnabled) {
            viewModel.update { it.copy(forecastFreezeEnabled = !it.forecastFreezeEnabled) }
        }
        SettingSwitch("NWS freeze alerts", s.nwsFreezeAlertsEnabled) {
            viewModel.update { it.copy(nwsFreezeAlertsEnabled = !it.nwsFreezeAlertsEnabled) }
        }
        SettingSwitch("Quiet hours", s.quietHoursEnabled) {
            viewModel.update { it.copy(quietHoursEnabled = !it.quietHoursEnabled) }
        }

        NumberField("Freeze threshold (°F)", s.freezeThresholdF) { v ->
            viewModel.update { it.copy(freezeThresholdF = v) }
        }
        NumberField("Humidity threshold (%)", s.humidityThreshold) { v ->
            viewModel.update { it.copy(humidityThreshold = v) }
        }
        NumberField("Rate-of-change (°F)", s.rateChangeF) { v ->
            viewModel.update { it.copy(rateChangeF = v) }
        }
        NumberField("Outage hours", s.outageHours) { v ->
            viewModel.update { it.copy(outageHours = v) }
        }

        Text("Channels", style = MaterialTheme.typography.titleMedium)
        SettingSwitch("Email", s.channelEmail) {
            viewModel.update { it.copy(channelEmail = !it.channelEmail) }
        }
        OutlinedTextField(
            value = s.email.orEmpty(),
            onValueChange = { v -> viewModel.update { it.copy(email = v.ifBlank { null }) } },
            label = { Text("Alert email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        SettingSwitch("Discord", s.channelDiscord) {
            viewModel.update { it.copy(channelDiscord = !it.channelDiscord) }
        }
        SettingSwitch("Telegram", s.channelTelegram) {
            viewModel.update { it.copy(channelTelegram = !it.channelTelegram) }
        }
        SettingSwitch("Slack", s.channelSlack) {
            viewModel.update { it.copy(channelSlack = !it.channelSlack) }
        }
        SettingSwitch("Push", s.channelPush) {
            viewModel.update { it.copy(channelPush = !it.channelPush) }
        }
        SettingSwitch("SMS", s.channelSms) {
            viewModel.update { it.copy(channelSms = !it.channelSms) }
        }

        Text("Snooze", style = MaterialTheme.typography.titleMedium)
        val snoozeUntil = s.snoozeUntil
        if (!snoozeUntil.isNullOrBlank()) {
            Text(
                "Snoozed until $snoozeUntil",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val vacationUntil = s.vacationUntil
        if (!vacationUntil.isNullOrBlank()) {
            Text(
                "Vacation until $vacationUntil",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            "Freeze and leak alerts still fire during snooze/vacation.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(4, 12, 24, 48).forEach { hours ->
                FilterChip(
                    selected = false,
                    onClick = { viewModel.snoozeHours(hours) },
                    label = { Text("${hours}h") },
                )
            }
            FilterChip(selected = false, onClick = { viewModel.vacationDays(3) }, label = { Text("Vacation 3d") })
            FilterChip(selected = false, onClick = { viewModel.vacationDays(7) }, label = { Text("Vacation 7d") })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = viewModel::clearSnooze) { Text("Clear snooze") }
            OutlinedButton(onClick = viewModel::clearVacation) { Text("Clear vacation") }
        }

        Button(
            onClick = viewModel::save,
            enabled = !saving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (saving) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.height(20.dp))
            else Text("Save alert settings")
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun NumberField(label: String, value: Double, onChange: (Double) -> Unit) {
    OutlinedTextField(
        value = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString(),
        onValueChange = { raw -> raw.toDoubleOrNull()?.let(onChange) },
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}
