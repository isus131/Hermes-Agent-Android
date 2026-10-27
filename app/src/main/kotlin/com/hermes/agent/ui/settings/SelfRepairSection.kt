package com.hermes.agent.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hermes.agent.BuildConfig
import com.hermes.agent.data.diagnostics.RepairReporter
import com.hermes.agent.data.diagnostics.ReportRedactor
import com.hermes.agent.data.log.LogManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SelfRepairViewModel @Inject constructor(
    private val reporter: RepairReporter,
    private val logManager: LogManager,
) : ViewModel() {

    data class State(
        val configured: Boolean,
        val autoRepair: Boolean,
        val autoSendCrashes: Boolean = false,
        val sending: Boolean = false,
        val message: String? = null,
    )

    private val _state = MutableStateFlow(State(reporter.isConfigured, reporter.autoRepair, reporter.autoSendCrashes))
    val state = _state.asStateFlow()

    fun saveToken(token: String) {
        reporter.setToken(token)
        _state.value = _state.value.copy(configured = reporter.isConfigured, message = null)
    }

    fun setAutoSendCrashes(on: Boolean) {
        reporter.autoSendCrashes = on
        _state.value = _state.value.copy(autoSendCrashes = on)
    }

    fun setAutoRepair(on: Boolean) {
        reporter.autoRepair = on
        _state.value = _state.value.copy(autoRepair = on)
    }

    /** The exact text that would be sent: description plus the last log lines, redacted. */
    fun preview(description: String, includeLogs: Boolean): String {
        val logs = if (includeLogs) logManager.readRecent(12_000).lines().takeLast(120).joinToString("\n") else ""
        return ReportRedactor.redact(
            RepairReporter.body("Hermes", description, logs, BuildConfig.VERSION_NAME),
        )
    }

    fun send(title: String, body: String) = viewModelScope.launch {
        _state.value = _state.value.copy(sending = true, message = null)
        val result = reporter.file(ReportRedactor.redact(title), body)
        _state.value = _state.value.copy(
            sending = false,
            message = result.fold({ "Отправлено: $it" }, { "Не удалось отправить: ${it.message}" }),
        )
    }
}

/**
 * Settings → Advanced → Self-repair. Reports go to the private repair repo; with auto-repair
 * on, the PC tries a fix and opens a draft PR for review. Nothing is installed automatically.
 */
@Composable
fun SelfRepairSection(viewModel: SelfRepairViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var token by remember { mutableStateOf("") }
    var reporting by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Отчёты о проблемах", style = MaterialTheme.typography.bodyLarge)
            InfoNote(
                "Как используются отчёты",
                "Отчёты попадают в приватный репозиторий GitHub ${RepairReporter.REPO}. Почта, номера, ключи, " +
                    "адреса и параметры запросов сначала удаляются, и вы видите точный текст до " +
                    "отправки. Если автоисправление включено, ваш ПК пробует исправить ошибку и открывает черновой " +
                    "pull request для вашей проверки. Без вас ничего не принимается и не устанавливается.",
            )
            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                label = { Text(if (state.configured) "Токен GitHub (сохранён)" else "Токен GitHub для ${RepairReporter.REPO}") },
                placeholder = { Text("Детальный токен: Issues чтение/запись только для этого репозитория") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.saveToken(token); token = "" }, enabled = token.isNotBlank()) {
                    Text("Сохранить токен")
                }
                if (state.configured) {
                    OutlinedButton(onClick = { viewModel.saveToken("") }) { Text("Удалить токен") }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Автоматически запускать исправление", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Добавляет метку repair, чтобы ПК взялся за работу.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = state.autoRepair, onCheckedChange = viewModel::setAutoRepair)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Автоматически отправлять отчёты о сбоях", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "В обезличенном виде, при следующем запуске, без вопросов. Включайте на устройствах с тестовыми сборками.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = state.autoSendCrashes, onCheckedChange = viewModel::setAutoSendCrashes, enabled = state.configured)
            }
            Button(onClick = { reporting = true }, enabled = state.configured && !state.sending) {
                Text(if (state.sending) "Отправка…" else "Сообщить о проблеме")
            }
            state.message?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    if (reporting) {
        ReportProblemDialog(
            preview = viewModel::preview,
            onSend = { title, body -> viewModel.send(title, body); reporting = false },
            onDismiss = { reporting = false },
        )
    }
}

@Composable
private fun ReportProblemDialog(
    preview: (String, Boolean) -> String,
    onSend: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var description by remember { mutableStateOf("") }
    var includeLogs by remember { mutableStateOf(true) }
    var reviewing by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (reviewing == null) "Сообщить о проблеме" else "Будет отправлено вот это") },
        text = {
            val shown = reviewing
            if (shown == null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Что пошло не так?") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = includeLogs, onCheckedChange = { includeLogs = it })
                        Text("Приложить последние строки журнала", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                Text(
                    shown,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                )
            }
        },
        confirmButton = {
            val shown = reviewing
            if (shown == null) {
                TextButton(onClick = { reviewing = preview(description, includeLogs) }, enabled = description.isNotBlank()) {
                    Text("Проверить")
                }
            } else {
                TextButton(onClick = { onSend(description.lineSequence().first().take(100), shown) }) { Text("Отправить") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}
