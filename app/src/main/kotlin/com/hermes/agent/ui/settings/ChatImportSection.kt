package com.hermes.agent.ui.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.hermes.agent.data.export.ChatExportImporter
import com.hermes.agent.data.export.ImportMode
import com.hermes.agent.data.export.JsonBackup
import com.hermes.agent.data.export.JsonBackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ChatImportViewModel @Inject constructor(
    private val backups: JsonBackupManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    private val _working = MutableStateFlow(false)
    val working: StateFlow<Boolean> = _working.asStateFlow()

    fun import(uri: Uri) {
        if (_working.value) return
        _working.value = true
        _message.value = null
        viewModelScope.launch {
            _message.value = try {
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: error("Не удалось открыть файл.")
                }
                val parsed = withContext(Dispatchers.Default) { ChatExportImporter.parse(bytes) }
                if (parsed.conversations.isEmpty()) {
                    "В этом файле не найдено чатов с сообщениями."
                } else {
                    val report = backups.import(
                        JsonBackup(conversations = parsed.conversations),
                        ImportMode.SKIP_EXISTING,
                    )
                    buildString {
                        append("Импортировано чатов ${parsed.source.label}: ${report.added}${if (report.added == 1) "" else ""}")
                        if (report.skipped > 0) append(", уже были здесь: ${report.skipped}")
                        append(". Переносится только текст ваших сообщений и ответов, без изображений и файлов.")
                    }
                }
            } catch (e: ChatExportImporter.ImportFailure) {
                e.message ?: "Этот файл не удалось прочитать."
            } catch (e: Exception) {
                "Импорт не удался: ${e.message ?: e.javaClass.simpleName}"
            }
            _working.value = false
        }
    }
}

/** Brings chats in from a ChatGPT or Claude "export your data" file (the zip or its conversations.json). */
@Composable
fun ChatImportSection(viewModel: ChatImportViewModel = hiltViewModel()) {
    val message by viewModel.message.collectAsStateWithLifecycle()
    val working by viewModel.working.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.import(uri)
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Импорт чатов из ChatGPT или Claude", style = MaterialTheme.typography.titleMedium)
            Text(
                "В ChatGPT или Claude запросите в настройках «Экспорт данных», затем выберите полученный zip-архив (или его conversations.json). " +
                    "Уже импортированные чаты пропускаются, так что один и тот же файл можно импортировать повторно.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (working) {
                CircularProgressIndicator()
            } else {
                OutlinedButton(
                    onClick = { picker.launch(arrayOf("application/zip", "application/json", "application/octet-stream", "text/plain")) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Выбрать файл экспорта") }
            }
            message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
