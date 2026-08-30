package dev.thermaltrace.android.ui.share

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.thermaltrace.android.ui.theme.brandTitle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ShareScreen(
    viewModel: ShareViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(brandTitle()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
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
        when {
            state.loading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            else -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Share links", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "Read-only links for family or short guest/renter visits. Pro + household manager.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!state.canCreate) {
                    Text(
                        "Upgrade to Pro to create share links.",
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                state.message?.let { Text(it, color = MaterialTheme.colorScheme.tertiary) }

                Text("Create link", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = state.label,
                    onValueChange = viewModel::setLabel,
                    label = { Text("Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Scope", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("live", "history", "metrics").forEach { scope ->
                        FilterChip(
                            selected = state.scope == scope,
                            onClick = { viewModel.setScope(scope) },
                            label = { Text(scope.replaceFirstChar { it.uppercase() }) },
                        )
                    }
                }
                Text("Expires", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        0 to "Never",
                        2 to "2 days",
                        7 to "7 days (guest)",
                        14 to "14 days",
                        30 to "30 days",
                    ).forEach { (days, label) ->
                        FilterChip(
                            selected = state.expiresDays == days,
                            onClick = { viewModel.setExpiresDays(days) },
                            label = { Text(label) },
                        )
                    }
                }
                Button(
                    onClick = viewModel::create,
                    enabled = state.canCreate && !state.busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.busy) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.height(18.dp))
                    else Text("Create share link")
                }

                Text("Your links", style = MaterialTheme.typography.titleMedium)
                if (state.links.isEmpty()) {
                    Text("No share links yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                state.links.forEach { link ->
                    val url = viewModel.publicUrl(link.token)
                    Column(Modifier.fillMaxWidth()) {
                        Text(
                            link.label?.takeIf { it.isNotBlank() } ?: link.scope,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            listOfNotNull(
                                link.scope,
                                link.expiresAt?.let { "expires $it" } ?: "never expires",
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            url,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val clipboard =
                                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Share link", url))
                                },
                            ) { Text("Copy") }
                            OutlinedButton(
                                onClick = { viewModel.revoke(link.id) },
                                enabled = !state.busy,
                            ) { Text("Revoke") }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
