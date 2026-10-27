package com.hermes.agent.ui.settings
import com.hermes.agent.domain.llm.*
import com.hermes.agent.domain.settings.*

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.ui.Alignment
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.FilterChip
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermes.agent.R
import com.hermes.agent.ui.theme.hermesFieldColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantSettingsScreen(
    onBack: () -> Unit,
    onOpenProviders: () -> Unit,
    onOpenTalk: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val placeFeedback by viewModel.placeFeedback.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ассистент") },
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
            SectionHeader(text = "Модели")
            Card(modifier = Modifier.fillMaxWidth()) {
                NavRow(
                    icon = Icons.Outlined.Cloud,
                    title = "Провайдеры",
                    subtitle = "Облачные API-ключи, доступные модели и автоматическое переключение",
                    onClick = onOpenProviders,
                )
            }

            SectionHeader(text = "ИИ на устройстве (локальный движок)")
            OnDeviceAiCard(settings = settings, viewModel = viewModel)
            ToolCallerCard(settings = settings, viewModel = viewModel)

            SectionHeader(text = "Чат")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ToggleRow(
                        title = "Показывать вызовы инструментов",
                        subtitle = "Видеть, что агент делает по ходу ответа (поиск в интернете, календарь и т. п.), " +
                            "а не только итоговый ответ",
                        checked = settings.showToolCalls,
                        onCheckedChange = viewModel::setShowToolCalls,
                    )
                }
            }

            SectionHeader(text = "Голос")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onOpenTalk) {
                        Text("Открыть режим разговора без рук")
                    }
                    InfoNote(
                        "О режиме разговора",
                        "Непрерывный голосовой разговор: слушает, отвечает вслух и замолкает, " +
                            "как только вы начинаете говорить.",
                    )
                }
            }

            SectionHeader(text = "Постоянные инструкции")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val standingTextField = rememberSettingText(settings.standingInstructions)
                    var standingText by standingTextField::text
                    OutlinedTextField(
                        value = standingText,
                        onValueChange = {
                            standingText = it
                            viewModel.setStandingInstructions(it)
                        },
                        label = { Text("Всегда соблюдать") },
                        supportingText = {
                            Text("Добавляются в каждый разговор, например «отвечай по-русски». Только контекст — инструментов не даёт. Не больше 4000 символов.")
                        },
                        minLines = 3,
                        colors = hermesFieldColors(),
                        modifier = Modifier.fillMaxWidth().settingFocus(standingTextField),
                    )
                }
            }

            SectionHeader(text = "Уведомления")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ToggleRow(
                        title = "Разрешить агенту читать мои уведомления",
                        subtitle = "Нужно в дополнение к системному доступу к уведомлениям. " +
                            "Текст проверяется и обрезается до того, как его увидит модель, а " +
                            "уведомления, похожие на попытку внедрения команд, отбрасываются.",
                        checked = settings.notificationsAgentReadEnabled,
                        onCheckedChange = viewModel::setNotificationsAgentReadEnabled,
                    )
                }
            }

            SectionHeader(text = "Присутствие")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ToggleRow(
                        title = "Фоновое присутствие",
                        subtitle = "Агент примерно знает, где вы находитесь (по вашим собственным названиям " +
                            "мест), двигаетесь ли вы и состояние питания. Координаты " +
                            "никогда не сохраняются и не отправляются — только название. Проверка каждые 15 минут.",
                        checked = settings.presenceEnabled,
                        onCheckedChange = viewModel::setPresenceEnabled,
                    )
                    if (settings.presenceEnabled) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        var placeLabel by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = placeLabel,
                            onValueChange = { placeLabel = it },
                            label = { Text("Название этого места (например, Дом)") },
                            colors = hermesFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        TextButton(
                            enabled = placeLabel.isNotBlank(),
                            onClick = {
                                viewModel.addCurrentLocationAsPlace(placeLabel.trim())
                                placeLabel = ""
                            },
                        ) { Text("Сохранить текущее местоположение как это место") }
                        placeFeedback?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }

            SectionHeader(text = "Фоновая проверка")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ToggleRow(
                        title = "Фоновая проверка",
                        subtitle = "Будит агента по расписанию, чтобы выполнять ваши постоянные поручения. " +
                            "Пропускает цикл в режиме энергосбережения или при заряде ниже 15% и " +
                            "молчит, когда сообщать нечего.",
                        checked = settings.heartbeatEnabled,
                        onCheckedChange = viewModel::setHeartbeatEnabled,
                    )
                    if (settings.heartbeatEnabled) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text("Проверять каждые ${settings.heartbeatIntervalMinutes} мин", style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(15, 30, 60, 180).forEach { minutes ->
                                FilterChip(
                                    selected = settings.heartbeatIntervalMinutes == minutes,
                                    onClick = { viewModel.setHeartbeatIntervalMinutes(minutes) },
                                    label = { Text(if (minutes < 60) "${minutes}m" else "${minutes / 60}h") },
                                )
                            }
                        }
                    }
                }
            }

            SectionHeader(text = "Действия и одобрения")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ToggleRow(
                        title = "Автоодобрение действий на телефоне",
                        subtitle = "Будильники, навигация, звонки, медиа, календарь, запуск приложений, " +
                            "управление устройством, а также создание и удаление ботов главным ботом " +
                            "без вопросов каждый раз. Shell, Termux, системные настройки " +
                            "и фоновые действия остаются защищёнными.",
                        checked = settings.autoApprovePhoneActions,
                        onCheckedChange = viewModel::setAutoApprovePhoneActions,
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    ToggleRow(
                        title = "Автоодобрение управления Home Assistant",
                        subtitle = "Включать и выключать свет, розетки и климат без вопросов каждый " +
                            "раз. Чтение состояния не спрашивает никогда. Замки, сигнализации, шторы и пылесосы " +
                            "всегда спрашивают, даже если это включено.",
                        checked = settings.autoApproveHomeAssistantControl,
                        onCheckedChange = viewModel::setAutoApproveHomeAssistantControl,
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    ToggleRow(
                        title = "Доверенные фоновые действия",
                        subtitle = "Разрешить по расписанию и в фоне действия с календарём, будильниками, связью, медиа " +
                            "и управлением устройством. Для включения нужен отпечаток пальца или код телефона.",
                        checked = settings.trustedBackgroundPhoneActions,
                        onCheckedChange = viewModel::setTrustedBackgroundPhoneActions,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnDeviceAiCard(
    settings: com.hermes.agent.domain.settings.UserSettings,
    viewModel: SettingsViewModel,
) {
    val isDownloaded by viewModel.isModelDownloaded.collectAsStateWithLifecycle()
    val isDownloading by viewModel.isModelDownloading.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.modelDownloadProgress.collectAsStateWithLifecycle()
    val downloadError by viewModel.modelDownloadError.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val selectedModel = com.hermes.agent.data.llm.ModelCatalog.byId(settings.selectedModelId)

    // Storage-access state; re-checked when the user returns from the grant flow.
    val storage = rememberStorageAccess(viewModel)
    val hasStorage = storage.granted
    fun requestStorageAccess() = storage.request()

    val customPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            // takePersistableUriPermission throws SecurityException for some
            // providers/URIs; an uncaught throw here (activity-result callback,
            // main thread) crashes the app on model pick. Persisting is
            // best-effort — the URI still works for this session either way.
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            viewModel.setLocalModelUri(uri.toString())
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DescribedTitle(
                    title = "Локальная модель",
                    description = "Если выключено, запасной вариант на устройстве не используется — только облако и понятная ошибка " +
                        "вместо тихого перехода на локальную модель, когда облако недоступно.",
                    modifier = Modifier.weight(1f),
                    titleStyle = MaterialTheme.typography.titleSmall,
                )
                Switch(
                    checked = settings.localLlmEnabled,
                    onCheckedChange = viewModel::setLocalLlmEnabled,
                )
            }

            if (!settings.localLlmEnabled) return@Column

            InfoNote(
                "Как используется локальная модель",
                "Если облачные модели включены, используются они. Эта модель на устройстве — приватный " +
                    "офлайн-запасной вариант: выберите модель, место сохранения и скачайте её.",
            )

            // ── Model dropdown ──────────────────────────────────────────────
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { if (!isDownloading) expanded = it },
            ) {
                OutlinedTextField(
                    value = "${selectedModel.displayName} · ${selectedModel.sizeLabel}",
                    onValueChange = {},
                    readOnly = true,
                    enabled = !isDownloading,
                    label = { Text("Модель") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = hermesFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    viewModel.modelCatalog.forEach { model ->
                        DropdownMenuItem(
                            text = { Text("${model.displayName} · ${model.sizeLabel}") },
                            onClick = {
                                viewModel.setSelectedModelId(model.id)
                                expanded = false
                            },
                        )
                    }
                }
            }

            // ── RAM Preflight status ─────────────────────────────────────────
            val preflight = remember(settings.selectedModelId, settings.localModelUri) {
                viewModel.evaluatePreflightForSelectedModel(settings)
            }
            when (preflight.level) {
                com.hermes.agent.data.llm.PreflightLevel.BLOCKED -> {
                    Text(
                        text = "⚠️ Предварительная проверка: ${preflight.detail}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                com.hermes.agent.data.llm.PreflightLevel.WARNING -> {
                    Text(
                        text = "ℹ️ Предварительная проверка: ${preflight.detail}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
                com.hermes.agent.data.llm.PreflightLevel.OPTIMAL -> Unit
            }

            // ── Download folder ─────────────────────────────────────────────
            // Every half-typed path here names a folder with no models in it,
            // and committing one unloads the chat and tool-caller models both.
            // Hold the text locally and commit once the edit is finished — on
            // Done, or when the field loses focus. Blank still means "default".
            val focusManager = LocalFocusManager.current
            var dirText by remember(settings.modelDownloadDir) { mutableStateOf(settings.modelDownloadDir) }
            OutlinedTextField(
                value = dirText,
                onValueChange = { dirText = it },
                enabled = !isDownloading,
                label = { Text("Папка для загрузки") },
                placeholder = { Text("/storage/emulated/0/${viewModel.defaultModelDirName}") },
                supportingText = {
                    Text("Оставьте пустым, чтобы использовать папку по умолчанию «${viewModel.defaultModelDirName}» во внутренней памяти.")
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                colors = hermesFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focus ->
                        if (!focus.isFocused && dirText.trim() != settings.modelDownloadDir) {
                            viewModel.setModelDownloadDir(dirText)
                        }
                    },
            )

            // ── Storage permission gate ─────────────────────────────────────
            if (!hasStorage) {
                Text(
                    text = "Чтобы сохранить модель в папку, которую вы видите, нужен доступ к хранилищу. " +
                        "Без него загрузки туда сохранить нельзя.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                androidx.compose.material3.OutlinedButton(
                    onClick = { requestStorageAccess() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Выдать доступ к хранилищу")
                }
            }

            // ── Error surface (L-007) ───────────────────────────────────────
            if (downloadError.isNotBlank()) {
                Text(
                    text = downloadError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                androidx.compose.material3.TextButton(onClick = { viewModel.clearModelDownloadError() }) {
                    Text("Скрыть")
                }
            }

            // ── State-driven actions ────────────────────────────────────────
            when {
                settings.localModelUri.isNotBlank() -> {
                    Text(
                        text = "Используется своя модель из памяти устройства.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    androidx.compose.material3.Button(
                        onClick = { viewModel.setLocalModelUri("") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                        ),
                    ) { Text("Убрать свою модель") }
                }
                isDownloading -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Загрузка… (${(downloadProgress * 100).toInt()}%)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            IconButton(
                                onClick = { viewModel.cancelModelDownload() },
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Отменить загрузку",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                isDownloaded -> {
                    Text(
                        text = "${selectedModel.displayName} скачана и готова.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    androidx.compose.material3.OutlinedButton(
                        onClick = { customPicker.launch(arrayOf("application/octet-stream")) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Выбрать свою модель (.gguf)") }
                }
                else -> {
                    androidx.compose.material3.Button(
                        onClick = { viewModel.downloadLocalModel(dirText) },
                        enabled = hasStorage,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Скачать ${selectedModel.displayName} (${selectedModel.sizeLabel})") }
                    androidx.compose.material3.OutlinedButton(
                        onClick = { customPicker.launch(arrayOf("application/octet-stream")) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Выбрать свою модель (.gguf)") }
                }
            }
        }
    }
}

/**
 * External-storage grant state, shared by the two model-download cards.
 *
 * Both cards download into the same folder and so need the same grant; the
 * launchers have to be registered per-composable, but the dance around them is
 * identical and worth having in one place.
 */
private class StorageAccess(val granted: Boolean, val request: () -> Unit)

@Composable
private fun rememberStorageAccess(viewModel: SettingsViewModel): StorageAccess {
    var granted by remember { mutableStateOf(viewModel.hasStorageAccess()) }
    val allFilesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        granted = viewModel.hasStorageAccess()
        viewModel.onStorageAccessMaybeChanged()
    }
    val writePermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        granted = viewModel.hasStorageAccess()
        viewModel.onStorageAccessMaybeChanged()
    }
    return StorageAccess(granted) {
        val intent = viewModel.allFilesAccessIntent()
        if (intent != null) allFilesLauncher.launch(intent)
        else writePermLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
}

/**
 * The on-device tool caller: a small model that answers device commands before
 * the cloud is asked, and hands the turn on when it is not confident.
 *
 * Its own card rather than a row inside [OnDeviceAiCard] because the two are
 * independent — this one runs on tool turns whether or not the chat fallback is
 * enabled, and it has its own model, its own download and its own RAM cost.
 */
@Composable
private fun ToolCallerCard(
    settings: com.hermes.agent.domain.settings.UserSettings,
    viewModel: SettingsViewModel,
) {
    val isDownloaded by viewModel.isToolCallerDownloaded.collectAsStateWithLifecycle()
    val isDownloading by viewModel.isModelDownloading.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.modelDownloadProgress.collectAsStateWithLifecycle()
    val storage = rememberStorageAccess(viewModel)
    val model = viewModel.toolCallerModel

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DescribedTitle(
                    title = "Исполнитель команд (экспериментально)",
                    description = "Модель на ${model.sizeLabel}, которая выполняет команды устройства прямо на телефоне, раньше " +
                        "облака, и передаёт ход дальше, если не уверена.",
                    modifier = Modifier.weight(1f),
                    titleStyle = MaterialTheme.typography.titleSmall,
                )
                Switch(
                    checked = settings.onDeviceToolCallerEnabled,
                    onCheckedChange = viewModel::setOnDeviceToolCallerEnabled,
                )
            }

            if (settings.onDeviceToolCallerEnabled) {
                InfoNote(
                    "Что она делает",
                    "Только команды с инструментами — включить фонарик, таймер, заметку. Обычный " +
                        "разговор не затрагивается и идёт к вашей основной модели. Команда, " +
                        "которую она поняла неверно или в которой не уверена, уходит в облако, так что цена " +
                        "ошибки — задержка, а не неправильное действие.",
                )
            }

            // Download state stays visible with the switch off, so the model can
            // be fetched on wifi ahead of time rather than only at the moment
            // someone turns the feature on.

            val preflight = remember(model.id) { viewModel.evaluateToolCallerPreflight() }
            when (preflight.level) {
                com.hermes.agent.data.llm.PreflightLevel.BLOCKED -> Text(
                    text = "⚠️ Предварительная проверка: ${preflight.detail}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                com.hermes.agent.data.llm.PreflightLevel.WARNING -> Text(
                    text = "ℹ️ Предварительная проверка: ${preflight.detail}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                com.hermes.agent.data.llm.PreflightLevel.OPTIMAL -> Unit
            }

            when {
                isDownloaded -> Text(
                    text = "${model.displayName} скачана и готова.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                isDownloading -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Deliberately not "downloading the tool caller": both models
                    // share one download coordinator, so this flag cannot say
                    // which fetch is running.
                    Text(
                        text = "Идёт загрузка модели… (${(downloadProgress * 100).toInt()}%)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                !storage.granted -> {
                    Text(
                        text = "Чтобы сохранить модель в папку, которую вы видите, нужен доступ к хранилищу.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    androidx.compose.material3.OutlinedButton(
                        onClick = { storage.request() },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Выдать доступ к хранилищу") }
                }
                else -> {
                    androidx.compose.material3.Button(
                        onClick = { viewModel.downloadToolCaller() },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Скачать ${model.displayName} (${model.sizeLabel})") }
                    InfoNote(
                        "Об этой загрузке",
                        "Сохраняется в ту же папку «${viewModel.defaultModelDirName}», что и " +
                            "локальная модель чата. Включение исполнителя команд само запускает " +
                            "загрузку — ${model.sizeLabel}, так что лучше через Wi-Fi. Пока она не " +
                            "закончится, команды устройства обрабатываются как сейчас.",
                    )
                }
            }
        }
    }
}

@Deprecated("Legacy single-provider UI is no longer presented; provider cards are the source of truth.")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LegacyCloudSectionRemoved(
    settings: com.hermes.agent.domain.settings.UserSettings,
    viewModel: SettingsViewModel,
) {
    val primaryDiscovery by viewModel.primaryModelDiscovery.collectAsStateWithLifecycle()
    val specialistDiscovery by viewModel.specialistModelDiscovery.collectAsStateWithLifecycle()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ToggleRow(
                title = stringResource(R.string.settings_cloud_enabled),
                subtitle = "Если облако настроено, сначала используется оно; модель на устройстве — офлайн-запасной вариант.",
                checked = settings.cloudEnabled,
                onCheckedChange = viewModel::setCloudEnabled,
            )
            HorizontalDivider()

            Text(
                "Свой сервер — основной",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val baseUrlField = rememberSettingText(settings.cloudBaseUrl)
            var baseUrl by baseUrlField::text
            OutlinedTextField(
                value = baseUrl,
                onValueChange = {
                    baseUrl = it
                    viewModel.setCloudBaseUrl(it)
                },
                label = { Text(stringResource(R.string.settings_cloud_base_url)) },
                supportingText = { Text("Модели загружаются автоматически с адреса /models этого URL.") },
                singleLine = true,
                colors = hermesFieldColors(),
                modifier = Modifier.fillMaxWidth().settingFocus(baseUrlField),
            )

            val apiKeyField = rememberSettingText(settings.cloudApiKey)
            var apiKey by apiKeyField::text
            OutlinedTextField(
                value = apiKey,
                onValueChange = {
                    apiKey = it
                    viewModel.setCloudApiKey(it)
                },
                label = { Text(stringResource(R.string.settings_cloud_api_key)) },
                supportingText = { Text(stringResource(R.string.settings_cloud_api_key_subtitle)) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                colors = hermesFieldColors(),
                modifier = Modifier.fillMaxWidth().settingFocus(apiKeyField),
            )

            CloudModelSelector(
                label = stringResource(R.string.settings_cloud_model),
                selectedModel = settings.cloudModel,
                state = primaryDiscovery,
                onSelect = viewModel::setCloudModel,
                onRetry = viewModel::refreshCloudModels,
            )

            HorizontalDivider()

            Text(
                "Свой сервер — специалист",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val auxBaseUrlField = rememberSettingText(settings.auxBaseUrl)
            var auxBaseUrl by auxBaseUrlField::text
            OutlinedTextField(
                value = auxBaseUrl,
                onValueChange = {
                    auxBaseUrl = it
                    viewModel.setAuxBaseUrl(it)
                },
                label = { Text("Базовый URL специалиста (необязательно)") },
                supportingText = { Text("Оставьте пустым, чтобы использовать основного провайдера и его список моделей.") },
                singleLine = true,
                colors = hermesFieldColors(),
                modifier = Modifier.fillMaxWidth().settingFocus(auxBaseUrlField),
            )

            val auxApiKeyField = rememberSettingText(settings.auxApiKey)
            var auxApiKey by auxApiKeyField::text
            OutlinedTextField(
                value = auxApiKey,
                onValueChange = {
                    auxApiKey = it
                    viewModel.setAuxApiKey(it)
                },
                label = { Text("API-ключ специалиста (необязательно)") },
                supportingText = { Text("Оставьте пустым, чтобы использовать ключ основного провайдера.") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                colors = hermesFieldColors(),
                modifier = Modifier.fillMaxWidth().settingFocus(auxApiKeyField),
            )

            CloudModelSelector(
                label = stringResource(R.string.settings_specialised_model),
                selectedModel = settings.auxModel,
                state = specialistDiscovery,
                onSelect = viewModel::setAuxModel,
                onRetry = viewModel::refreshCloudModels,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CloudModelSelector(
    label: String,
    selectedModel: String,
    state: ModelDiscoveryUiState,
    onSelect: (String) -> Unit,
    onRetry: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    if (state is ModelDiscoveryUiState.Ready) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedTextField(
                value = selectedModel,
                onValueChange = {},
                readOnly = true,
                label = { Text(label) },
                supportingText = {
                    if (selectedModel !in state.models) {
                        Text("Сохранённой модели нет на этом сервере. Выберите доступную модель.")
                    } else {
                        Text("Доступно моделей: ${state.models.size}")
                    }
                },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = hermesFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                state.models.forEach { model ->
                    DropdownMenuItem(
                        text = { Text(model) },
                        onClick = {
                            onSelect(model)
                            expanded = false
                        },
                    )
                }
            }
        }
        return
    }

    OutlinedTextField(
        value = selectedModel,
        onValueChange = {},
        readOnly = true,
        enabled = false,
        label = { Text(label) },
        supportingText = {
            Text(
                when (state) {
                    ModelDiscoveryUiState.Idle -> "Включите облако и введите URL API, чтобы загрузить модели."
                    ModelDiscoveryUiState.Loading -> "Загрузка доступных моделей…"
                    ModelDiscoveryUiState.Empty -> "Этот сервер не вернул ни одной модели. Проверьте URL или провайдера."
                    is ModelDiscoveryUiState.Error -> state.message
                    is ModelDiscoveryUiState.Ready -> ""
                }
            )
        },
        trailingIcon = {
            if (state is ModelDiscoveryUiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            }
        },
        colors = hermesFieldColors(),
        modifier = Modifier.fillMaxWidth(),
    )

    if (state is ModelDiscoveryUiState.Error || state is ModelDiscoveryUiState.Empty) {
        androidx.compose.material3.OutlinedButton(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Повторить загрузку моделей")
        }
    }
}
