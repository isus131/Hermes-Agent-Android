package com.hermes.agent.ui.plugins
import com.hermes.agent.domain.settings.*

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermes.agent.domain.plugin.PluginInstance
import com.hermes.agent.domain.plugin.PluginState
import com.hermes.agent.ui.components.DestructiveActionDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginsScreen(
    viewModel: PluginsViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val plugins = uiState.plugins
    var pendingUninstall by remember { mutableStateOf<PluginInstance?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Плагины") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().semantics { liveRegion = LiveRegionMode.Polite },
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null) {
                Column(
                    modifier = Modifier.fillMaxSize().semantics { liveRegion = LiveRegionMode.Polite },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(text = "Ошибка: ${uiState.error}", color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = viewModel::loadPlugins) {
                        Text("Повторить")
                    }
                }
            } else if (plugins.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics { contentDescription = "Плагины не установлены." },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Плагины не установлены.", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Text(
                    text = "Плагинов: ${plugins.size} · " +
                        "активно: ${plugins.count { it.state == PluginState.ACTIVE }}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(plugins, key = { it.manifest.id }) { plugin ->
                        PluginRow(
                            plugin = plugin,
                            onToggle = { enabled ->
                                if (enabled) viewModel.activate(plugin.manifest.id)
                                else viewModel.suspend_(plugin.manifest.id)
                            },
                            onUninstall = { pendingUninstall = plugin },
                        )
                    }
                }
            }
        }
    }

    pendingUninstall?.let { plugin ->
        DestructiveActionDialog(
            title = "Удалить «${plugin.manifest.displayName}»?",
            message = "Плагин и его локальные настройки будут удалены. Чтобы снова им пользоваться, его нужно будет установить заново.",
            confirmLabel = "Удалить плагин",
            onConfirm = {
                viewModel.uninstall(plugin.manifest.id)
                pendingUninstall = null
            },
            onDismiss = { pendingUninstall = null },
        )
    }
}

@Composable
private fun PluginRow(
    plugin: PluginInstance,
    onToggle: (Boolean) -> Unit,
    onUninstall: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plugin.manifest.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "v${plugin.manifest.versionName} · ${plugin.manifest.author}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = plugin.state == PluginState.ACTIVE,
                    onCheckedChange = onToggle,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = plugin.manifest.capabilities.joinToString(" · ") { it.name },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Разрешения: " + plugin.manifest.permissions.joinToString(", ") { it.type.name },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            plugin.lastError?.let { err ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ошибка: $err",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onUninstall) { Text("Удалить") }
            }
        }
    }
}
