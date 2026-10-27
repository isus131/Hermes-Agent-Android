package com.hermes.agent.ui.settings
import com.hermes.agent.domain.settings.*

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TextButton
import androidx.compose.runtime.saveable.rememberSaveable
import com.hermes.agent.data.export.BackupSection
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermes.agent.ui.theme.hermesFieldColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val jsonBackupState by viewModel.jsonBackupState.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val privilegedStatus by viewModel.privilegedStatus.collectAsStateWithLifecycle()
    val retryGateStatus by viewModel.privilegedRetryGateStatus.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Дополнительно") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionHeader(text = "Привилегированная оболочка (Shizuku)")
            PrivilegedShellSection(
                enabled = settings.privilegedShellEnabled,
                status = privilegedStatus,
                gateStatus = retryGateStatus,
                onToggleEnabled = viewModel::setPrivilegedShellEnabled,
                onRequestPermission = viewModel::requestPrivilegedPermission,
                onRefresh = viewModel::refreshPrivilegedStatus,
                onResetGate = viewModel::resetPrivilegedGate,
            )

            SectionHeader(text = "Резервное копирование и восстановление")
            BackupRestoreSection(
                jsonState = jsonBackupState,
                onJsonBackup = { uri, sections, password, bots -> viewModel.exportJson(uri, sections, password, bots) },
                onJsonRestore = viewModel::importJson,
                onJsonDismiss = viewModel::dismissJsonBackupState,
            )
            AutoBackupSection()
            CloudBackupSection()

            SectionHeader(text = "Импорт чатов")
            ChatImportSection()

            SectionHeader(text = "Доступ к файлам и рабочей папке")
            FilesWorkspaceSection(
                rootUri = settings.filesRootUri,
                onUpdateRoot = viewModel::setFilesRootUri,
            )

            SectionHeader(text = "Саморазвитие")
            ExportSection(
                state = exportState,
                onExport = viewModel::exportSessions,
                onShare = { zip ->
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        zip,
                    )
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "application/zip"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(share, "Поделиться экспортом сессий"))
                },
                onDismiss = viewModel::dismissExportState,
            )

            SectionHeader(text = "Самовосстановление")
            SelfRepairSection()

        }
    }
}

@Composable
private fun FilesWorkspaceSection(
    rootUri: String,
    onUpdateRoot: (String) -> Unit,
) {
    val context = LocalContext.current
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            // Kept across restarts; without it the grant lapsed with the process.
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
            onUpdateRoot(uri.toString())
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Корневая рабочая папка",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = if (rootUri.isBlank()) {
                    "По умолчанию: внутренняя рабочая папка приложения"
                } else {
                    "Выбранная папка: ${com.hermes.agent.data.tools.WorkspaceRoots.pathOf(rootUri) ?: rootUri}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Button(
                    onClick = { launcher.launch(null) },
                    modifier = Modifier.weight(1f),
                    contentPadding = SettingsButtonPadding,
                ) {
                    ButtonLabel(if (rootUri.isBlank()) "Выбрать папку" else "Сменить папку")
                }
                if (rootUri.isNotBlank()) {
                    OutlinedButton(
                        onClick = { onUpdateRoot("") },
                        contentPadding = SettingsButtonPadding,
                    ) {
                        ButtonLabel("Отозвать / сбросить")
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivilegedShellSection(
    enabled: Boolean,
    status: com.hermes.agent.domain.device.PrivilegedShellBackend.PrivilegedStatus,
    gateStatus: com.hermes.agent.data.device.PrivilegedShellRetryGate.GateStatus,
    onToggleEnabled: (Boolean) -> Unit,
    onRequestPermission: () -> Unit,
    onRefresh: () -> Unit,
    onResetGate: () -> Unit,
) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DescribedTitle(
                    title = "Включить привилегированную оболочку",
                    description = "Позволяет инструменту shell работать с правами ADB (UID 2000) через Shizuku.",
                    modifier = Modifier.weight(1f),
                )
                androidx.compose.material3.Switch(
                    checked = enabled,
                    onCheckedChange = onToggleEnabled,
                )
            }

            androidx.compose.material3.HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Состояние Shizuku",
                    style = MaterialTheme.typography.titleSmall,
                )
                androidx.compose.material3.TextButton(onClick = onRefresh) {
                    Text("Проверить")
                }
            }

            when (status.status) {
                com.hermes.agent.domain.device.PrivilegedShellBackend.Status.READY -> {
                    Text(
                        text = "● Подключено (UID ${status.uid} · версия ${status.version})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                com.hermes.agent.domain.device.PrivilegedShellBackend.Status.PERMISSION_REQUIRED -> {
                    Text(
                        text = "⚠️ Нужно разрешение: Hermes нужен доступ к Shizuku.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Button(
                        onClick = onRequestPermission,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Выдать разрешение Shizuku")
                    }
                }
                com.hermes.agent.domain.device.PrivilegedShellBackend.Status.DEAD -> {
                    Text(
                        text = "⚠️ Служба Shizuku не запущена.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = "Запустите её через ADB:\n${com.hermes.agent.data.device.PrivilegedShellGateway.ADB_START_COMMAND}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Команда ADB", com.hermes.agent.data.device.PrivilegedShellGateway.ADB_START_COMMAND)
                            clipboard.setPrimaryClip(clip)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Копировать команду ADB")
                    }
                }
                com.hermes.agent.domain.device.PrivilegedShellBackend.Status.NOT_INSTALLED -> {
                    Text(
                        text = "Приложение Shizuku не установлено на этом устройстве.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (gateStatus.state == com.hermes.agent.data.device.PrivilegedShellRetryGate.State.DIRTY_UNWIND) {
                androidx.compose.material3.HorizontalDivider()
                Text(
                    text = "⚠️ Запуск заблокирован: ${gateStatus.reason}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                OutlinedButton(
                    onClick = onResetGate,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Сбросить блокировку")
                }
            }
        }
    }
}

@Composable
private fun ExportSection(
    state: ExportUiState,
    onExport: () -> Unit,
    onShare: (java.io.File) -> Unit,
    onDismiss: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Science,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text("Экспорт сессий для саморазвития", style = MaterialTheme.typography.bodyLarge)
            }
            InfoNote(
                "Как используется экспорт",
                "Экспортирует ваши разговоры в JSON-архив для офлайн-инструмента " +
                    "hermes-agent-self-evolution. Распакуйте в ~/.hermes/sessions/ " +
                    "на компьютере, затем запустите его с --eval-source sessiondb. " +
                    "Архив содержит текст чатов как есть — обращайтесь с ним как с конфиденциальными данными.",
            )

            when (state) {
                is ExportUiState.InProgress -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("Экспорт…", style = MaterialTheme.typography.bodySmall)
                    }
                }
                is ExportUiState.Ready -> {
                    Text(
                        "Экспортировано сессий: ${state.sessionCount} (сообщений: ${state.messageCount}).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(onClick = { onShare(state.zipFile) }, modifier = Modifier.weight(1f), contentPadding = SettingsButtonPadding) {
                            ButtonLabel("Поделиться архивом")
                        }
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), contentPadding = SettingsButtonPadding) {
                            ButtonLabel("Готово")
                        }
                    }
                }
                is ExportUiState.Error -> {
                    Text(
                        state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    FilledTonalButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
                        Text("Повторить экспорт")
                    }
                }
                is ExportUiState.Idle -> {
                    FilledTonalButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
                        Text("Экспортировать сессии")
                    }
                }
            }
        }
    }
}

