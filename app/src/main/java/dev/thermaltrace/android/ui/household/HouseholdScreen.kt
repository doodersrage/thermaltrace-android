package dev.thermaltrace.android.ui.household

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import dev.thermaltrace.android.ui.theme.brandTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseholdScreen(viewModel: HouseholdViewModel) {
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
                    Text("Household", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Members and invites for your active household.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
                state.message?.let { item { Text(it, color = MaterialTheme.colorScheme.tertiary) } }

                if (state.households.size > 1) {
                    item {
                        Text("Switch household", style = MaterialTheme.typography.titleMedium)
                    }
                    items(state.households, key = { "hh-${it.householdId}" }) { hh ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(hh.name.ifBlank { "Household" }, fontWeight = FontWeight.Medium)
                                Text(
                                    hh.role,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (hh.householdId == state.householdId) {
                                Text("Active", color = MaterialTheme.colorScheme.primary)
                            } else {
                                TextButton(
                                    onClick = { viewModel.switchHousehold(hh.householdId) },
                                    enabled = !state.saving,
                                ) { Text("Switch") }
                            }
                        }
                    }
                }

                item {
                    Text("Name", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = state.draftName,
                        onValueChange = viewModel::onDraftName,
                        label = { Text("Household name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = viewModel::saveName,
                        enabled = !state.saving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    ) {
                        if (state.saving) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.height(18.dp))
                        else Text("Save name")
                    }
                }

                item { Text("Members", style = MaterialTheme.typography.titleMedium) }
                if (state.members.isEmpty()) {
                    item {
                        Text("No members listed.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                items(state.members, key = { it.id ?: it.userId ?: it.hashCode().toString() }) { member ->
                    Column {
                        Text(
                            member.email?.takeIf { it.isNotBlank() }
                                ?: member.userId?.take(8)?.let { "User $it…" }
                                ?: "Member",
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            member.role,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                item { Text("Pending invites", style = MaterialTheme.typography.titleMedium) }
                if (state.invites.isEmpty()) {
                    item {
                        Text("None", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                items(state.invites, key = { it.id }) { invite ->
                    Column {
                        Text(invite.email, fontWeight = FontWeight.Medium)
                        Text(
                            invite.role,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                item {
                    Text("Invite someone", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = state.inviteEmail,
                        onValueChange = viewModel::onInviteEmail,
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    )
                    Row(
                        Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf("viewer", "member").forEach { role ->
                            FilterChip(
                                selected = state.inviteRole == role,
                                onClick = { viewModel.onInviteRole(role) },
                                label = { Text(role.replaceFirstChar { it.uppercase() }) },
                            )
                        }
                    }
                    Button(
                        onClick = viewModel::sendInvite,
                        enabled = !state.saving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    ) { Text("Send invite") }
                }
            }
        }
    }
}
