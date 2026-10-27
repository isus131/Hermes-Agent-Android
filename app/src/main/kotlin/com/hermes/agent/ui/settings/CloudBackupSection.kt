package com.hermes.agent.ui.settings

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.hermes.agent.data.export.CloudBackupPolicy
import com.hermes.agent.data.export.CloudBackupScheduler
import com.hermes.agent.data.export.CloudBackupService
import com.hermes.agent.data.export.CloudBackupStore
import com.hermes.agent.data.export.CloudFile
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject

data class CloudBackupUi(
    val repo: String = "",
    val device: String = "",
    val auto: Boolean = false,
    val hasToken: Boolean = false,
    val hasPassword: Boolean = false,
    val working: String? = null,
    val message: String = "",
    val isError: Boolean = false,
    val files: List<CloudFile>? = null,
    val staged: Boolean = false,
    val lastLine: String = "",
)

@HiltViewModel
class CloudBackupViewModel @Inject constructor(
    private val store: CloudBackupStore,
    private val service: CloudBackupService,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _ui = MutableStateFlow(read())
    val ui: StateFlow<CloudBackupUi> = _ui.asStateFlow()

    private fun read() = CloudBackupUi(
        repo = store.repo,
        device = store.device,
        auto = store.auto,
        hasToken = store.token() != null,
        hasPassword = store.password() != null,
        lastLine = if (store.lastAt == 0L) "" else {
            "Последняя выгрузка ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(store.lastAt))}: ${store.lastResult}"
        },
    )

    /** Saves what was typed; blank token and password fields keep what is already saved. */
    fun save(repo: String, device: String, token: String, password: String) {
        if (!CloudBackupPolicy.isRepo(repo)) {
            _ui.update { it.copy(message = "Укажите репозиторий в виде владелец/имя, например yourname/app-backups.", isError = true) }
            return
        }
        store.repo = repo
        store.device = device.ifBlank { android.os.Build.MODEL.orEmpty() }
        if (token.isNotBlank()) store.setToken(token)
        if (password.isNotEmpty()) store.setPassword(password)
        CloudBackupScheduler.apply(context, store, replace = true)
        _ui.update { read().copy(message = "Сохранено.", isError = false, files = it.files) }
    }

    fun setAuto(on: Boolean) {
        store.auto = on
        CloudBackupScheduler.apply(context, store, replace = true)
        _ui.update { read().copy(files = it.files) }
    }

    fun upload(password: String) {
        val pw = password.ifEmpty { store.password() }
        if (pw.isNullOrEmpty()) {
            _ui.update { it.copy(message = "Сначала введите пароль для копий.", isError = true) }
            return
        }
        launchWork("Создание копии и выгрузка…") {
            service.upload(pw).fold(
                onSuccess = { m -> _ui.update { read().copy(message = m, isError = false, files = it.files) } },
                onFailure = { e -> _ui.update { read().copy(message = e.message ?: "Выгрузка не удалась.", isError = true, files = it.files) } },
            )
        }
    }

    fun refresh() = launchWork("Чтение облачных копий…") {
        service.list().fold(
            onSuccess = { list -> _ui.update { it.copy(files = list, message = if (list.isEmpty()) "В репозитории пока нет копий." else "", isError = false, working = null) } },
            onFailure = { e -> _ui.update { it.copy(message = e.message ?: "Не удалось прочитать репозиторий.", isError = true, working = null) } },
        )
    }

    fun restore(file: CloudFile, password: String) {
        val pw = password.ifEmpty { store.password() }
        if (pw.isNullOrEmpty()) {
            _ui.update { it.copy(message = "Сначала введите пароль для копий.", isError = true) }
            return
        }
        launchWork("Загрузка и проверка…") {
            service.stage(file, pw).fold(
                onSuccess = { s ->
                    val note = if (!s.sameDevice) " Она с другого устройства, поэтому вход в Tailscale из неё не применится." else ""
                    _ui.update { it.copy(staged = true, message = "Копия проверена. Закройте Hermes и откройте снова, чтобы завершить восстановление.$note", isError = false, working = null) }
                },
                onFailure = { e -> _ui.update { it.copy(message = e.message ?: "Не удалось прочитать резервную копию.", isError = true, working = null) } },
            )
        }
    }

    private fun launchWork(what: String, block: suspend () -> Unit) {
        if (_ui.value.working != null) return
        _ui.update { it.copy(working = what) }
        viewModelScope.launch {
            block()
            _ui.update { it.copy(working = null) }
        }
    }
}

/** Keeps encrypted full backups in a private GitHub repo, so several devices can share them. */
@Composable
fun CloudBackupSection(viewModel: CloudBackupViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var repo by remember(ui.repo) { mutableStateOf(ui.repo) }
    var device by remember(ui.device) { mutableStateOf(ui.device) }
    var token by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<CloudFile?>(null) }
    val hidden = if (show) VisualTransformation.None else PasswordVisualTransformation()

    confirm?.let { file ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("Заменить всё этой резервной копией?") },
            text = {
                Text(
                    "Чаты, настройки, API-ключи и файлы будут заменены копией ${file.name} с устройства ${file.device}. " +
                        "Текущая база данных сохранится как страховочная копия. Чтобы завершить, Hermes нужно закрыть и открыть снова.",
                )
            },
            confirmButton = { TextButton(onClick = { viewModel.restore(file, password); confirm = null }) { Text("Восстановить") } },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Отмена") } },
        )
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Облачные копии (GitHub)", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Хранит зашифрованные полные копии в приватном репозитории, по папке на устройство, чтобы планшет и телефон могли восстанавливаться из копий друг друга.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = ui.auto, onCheckedChange = viewModel::setAuto, enabled = ui.hasToken && ui.hasPassword && CloudBackupPolicy.isRepo(ui.repo))
            }
            OutlinedTextField(
                value = repo, onValueChange = { repo = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                label = { Text("Приватный репозиторий (владелец/имя)") },
            )
            OutlinedTextField(
                value = device, onValueChange = { device = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                label = { Text("Имя этого устройства") },
            )
            OutlinedTextField(
                value = token, onValueChange = { token = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                label = { Text(if (ui.hasToken) "Токен GitHub (сохранён; введите, чтобы заменить)" else "Токен GitHub (Contents: чтение и запись)") },
                visualTransformation = hidden,
            )
            OutlinedTextField(
                value = password, onValueChange = { password = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                label = { Text(if (ui.hasPassword) "Пароль копий (сохранён; введите, чтобы заменить)" else "Пароль копий") },
                visualTransformation = hidden,
                trailingIcon = { TextButton(onClick = { show = !show }) { Text(if (show) "Скрыть" else "Показать") } },
            )
            Text(
                "Токен и пароль хранятся на этом устройстве и защищены ключом, который не может его покинуть. Используйте один и тот же " +
                    "пароль на всех устройствах, которые должны читать эти копии; восстановить его нельзя. Каждая копия " +
                    "шифруется до того, как покинет устройство.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { viewModel.save(repo, device, token, password); token = ""; if (password.length >= 8) password = "" },
                    enabled = repo.isNotBlank() && (password.isEmpty() || password.length >= 8),
                    modifier = Modifier.weight(1f),
                ) { Text("Сохранить") }
                Button(
                    onClick = { viewModel.upload(password) },
                    enabled = ui.working == null && ui.hasToken && (ui.hasPassword || password.length >= 8),
                    modifier = Modifier.weight(1f),
                ) { Text("Выгрузить сейчас") }
            }
            OutlinedButton(
                onClick = viewModel::refresh,
                enabled = ui.working == null && ui.hasToken,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Показать облачные копии") }
            // Upload and Show stay off until a token is saved; say so instead of leaving two dead buttons.
            val missing = when {
                !CloudBackupPolicy.isRepo(ui.repo) -> "Укажите репозиторий в виде владелец/имя и нажмите «Сохранить»."
                !ui.hasToken -> "Вставьте токен GitHub и нажмите «Сохранить», чтобы включить выгрузку и просмотр облачных копий."
                !ui.hasPassword -> "Перед выгрузкой введите пароль копий и нажмите «Сохранить»."
                else -> null
            }
            missing?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }

            ui.working?.let {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (ui.message.isNotEmpty()) {
                Text(
                    ui.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (ui.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
            if (ui.staged) {
                Button(onClick = { closeHermes(context) }, modifier = Modifier.fillMaxWidth()) { Text("Закрыть Hermes сейчас") }
            }
            ui.files?.forEach { file ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(file.device, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${file.name.removePrefix("hermes-").removeSuffix(CloudBackupPolicy.SUFFIX)} · ${file.size / 1024} КБ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedButton(onClick = { confirm = file }, enabled = ui.working == null && (ui.hasPassword || password.isNotEmpty())) { Text("Восстановить") }
                }
            }
            if (ui.lastLine.isNotEmpty()) {
                Text(ui.lastLine, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
