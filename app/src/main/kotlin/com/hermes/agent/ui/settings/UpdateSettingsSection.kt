package com.hermes.agent.ui.settings
import com.hermes.agent.domain.settings.*

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.hermes.agent.BuildConfig

@Composable
internal fun UpdateSection(
    state: UpdateUiState,
    canInstall: Boolean,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onManagePermission: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onDismiss: () -> Unit,
    onApplyFix: (com.hermes.agent.data.hotfix.PatchOffer) -> Unit = {},
    onChooseFullUpdate: () -> Unit = {},
) {
    // Keep the screen awake while an update is downloading. The download runs
    // in-app (OkHttp in the ViewModel, not DownloadManager/WorkManager), so
    // when the screen sleeps Doze throttles the socket and the download stalls.
    // View.keepScreenOn needs no permission and clears itself when the view
    // detaches; the DisposableEffect also clears it if the user navigates away
    // or the download finishes/fails.
    val isDownloading = state is UpdateUiState.Downloading
    val view = LocalView.current
    DisposableEffect(isDownloading) {
        if (isDownloading) view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Outlined.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text("Обновления по воздуху", style = MaterialTheme.typography.bodyLarge)
            }
            Text(
                "Текущая версия: ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            when (state) {
                is UpdateUiState.Idle, is UpdateUiState.Error -> {
                    if (state is UpdateUiState.Error) {
                        Text(
                            state.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    FilledTonalButton(
                        onClick = onCheck,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Проверить обновления")
                    }
                }
                is UpdateUiState.Checking -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("Проверка…", style = MaterialTheme.typography.bodySmall)
                    }
                }
                is UpdateUiState.UpToDate -> {
                    Text(
                        "У вас последняя версия.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("Скрыть")
                    }
                }
                is UpdateUiState.UpdateAvailable -> {
                    Text(
                        "Доступен Hermes ${state.version}!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (state.apkUrl.isNotBlank()) {
                        if (!canInstall) {
                            Text(
                                "Разрешите Hermes «установку неизвестных приложений», чтобы он мог " +
                                    "сам установить обновление.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            OutlinedButton(onClick = onManagePermission, modifier = Modifier.fillMaxWidth()) {
                                Text("Разрешить установку")
                            }
                        }
                        Button(
                            onClick = onDownload,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Скачать и установить")
                        }
                    } else {
                        Button(
                            onClick = { onOpenUrl(state.releaseUrl) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Открыть релиз")
                        }
                    }
                }
                is UpdateUiState.FixAvailable -> {
                    val m = state.offer.manifest
                    Text(
                        "Для этой версии есть исправление (№${m.patchVersion}, ${formatSize(m.size)}).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (m.notes.isNotBlank()) {
                        Text(m.notes, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        "Без переустановки: Hermes скачает исправление, проверит, что оно подписано тем же ключом, что и " +
                            "это приложение, и собрано именно для этой сборки, а затем попросит перезапуск. Оно вступит в силу " +
                            "при следующем запуске Hermes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = { onApplyFix(state.offer) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Применить исправление (нужен перезапуск)")
                    }
                    state.fullUpdate?.let { full ->
                        OutlinedButton(onClick = onChooseFullUpdate, modifier = Modifier.fillMaxWidth()) {
                            Text("Установить вместо этого полное обновление ${full.version}")
                        }
                    }
                    androidx.compose.material3.TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("Не сейчас")
                    }
                }
                is UpdateUiState.Downloading -> {
                    Text(
                        "Загрузка Hermes ${state.version}… ${state.percent}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LinearProgressIndicator(
                        progress = { state.percent / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "Экран не будет гаснуть, пока загрузка не закончится.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> "%.1f МБ".format(bytes / (1024.0 * 1024.0))
    bytes >= 1024 -> "${bytes / 1024} КБ"
    else -> "$bytes B"
}
