package com.hermes.agent.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.hermes.agent.data.remote.TailnetStatus
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermes.agent.domain.mcp.McpServerConfig
import com.hermes.agent.domain.mcp.McpTransportType
import com.hermes.agent.domain.settings.UserSettings
import com.hermes.agent.service.ApiServerController
import com.hermes.agent.ui.components.DestructiveActionDialog
import com.hermes.agent.ui.theme.hermesFieldColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionsSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Подключения") },
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
            SectionHeader(text = "Локальный API-сервер")
            ApiServerSection(
                settings = settings,
                onToggle = { enabled ->
                    viewModel.setApiServerEnabled(enabled) {
                        if (enabled) ApiServerController.start(context) else ApiServerController.stop(context)
                    }
                },
                onAllowLan = viewModel::setApiServerAllowLan,
                onRegenerateKey = {
                    viewModel.regenerateApiServerKey {
                        if (ApiServerController.status.value.running) ApiServerController.restart(context)
                    }
                },
            )

            SectionHeader(text = "Удалённая оболочка")
            RemoteShellSection(
                settings = settings,
                onHost = viewModel::setSshHost,
                onPort = viewModel::setSshPort,
                onUser = viewModel::setSshUser,
                onPassword = viewModel::setSshPassword,
                onHostFingerprint = viewModel::setSshHostFingerprint,
            )

            SectionHeader(text = "Home Assistant")
            HomeAssistantSection(
                settings = settings,
                onUrl = viewModel::setHomeAssistantUrl,
                onToken = viewModel::setHomeAssistantToken,
                onDashboardPath = viewModel::setHomeAssistantDashboardPath,
                onDashboardEnabled = viewModel::setHomeAssistantDashboardEnabled,
                onTestConnection = viewModel::testHomeAssistantConnection,
            )

            SectionHeader(text = "Tailnet (экспериментально)")
            TailnetSection(viewModel = viewModel)

            SectionHeader(text = "Удалённый шлюз")
            RemoteGatewaySection(
                settings = settings,
                onToggle = viewModel::setRemoteGatewayEnabled,
                onUrl = viewModel::setRemoteGatewayUrl,
                onApiKey = viewModel::setRemoteGatewayApiKey,
                onTestConnection = viewModel::testRemoteGatewayConnection,
            )

            SectionHeader(text = "MCP-серверы")
            McpServersSection()
        }
    }
}

@Composable
private fun ApiServerSection(
    settings: UserSettings,
    onToggle: (Boolean) -> Unit,
    onAllowLan: (Boolean) -> Unit,
    onRegenerateKey: () -> Unit,
) {
    val status by ApiServerController.status.collectAsStateWithLifecycle()
    val clipboard = LocalContext.current.getSystemService(android.content.ClipboardManager::class.java)
    var tokenVisible by remember { mutableStateOf(false) }
    var confirmRegeneration by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ToggleRow(
                title = "Запустить локальный API-сервер",
                subtitle = "Открывает адрес, совместимый с OpenAI, чтобы другие приложения (Open WebUI, " +
                    "LobeChat, скрипты) могли использовать Hermes как бэкенд.",
                checked = settings.apiServerEnabled,
                onCheckedChange = onToggle,
            )

            if (settings.apiServerEnabled) {
                HorizontalDivider()

                val reachable = if (status.running) status.baseUrl
                else "http://${if (settings.apiServerAllowLan) "0.0.0.0" else "127.0.0.1"}:${settings.apiServerPort}/v1"
                InfoRow(title = "Состояние", value = if (status.running) "Работает" else "Запуск…")
                InfoRow(title = "Адрес", value = reachable)
                status.error?.let {
                    Text(
                        "Ошибка: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                OutlinedTextField(
                    value = settings.apiServerKey,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Токен доступа") },
                    supportingText = { Text("Передавать так: Authorization: Bearer <токен>") },
                    singleLine = true,
                    visualTransformation = if (tokenVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = { RevealToggle(tokenVisible) { tokenVisible = !tokenVisible } },
                    colors = hermesFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(
                    onClick = {
                        clipboard?.setPrimaryClip(
                            android.content.ClipData.newPlainText("API-ключ Hermes", settings.apiServerKey),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Копировать токен") }
                OutlinedButton(
                    onClick = { confirmRegeneration = true },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Создать новый токен") }

                HorizontalDivider()
                ToggleRow(
                    title = "Разрешить доступ из локальной сети",
                    subtitle = "Выкл.: доступен только с этого устройства (127.0.0.1). " +
                        "Вкл.: доступен с других устройств в вашей сети Wi-Fi — держите токен в секрете.",
                    checked = settings.apiServerAllowLan,
                    onCheckedChange = onAllowLan,
                )
                InfoNote(
                    "Когда применяются изменения",
                    "Изменение доступа из сети или порта вступает в силу, когда вы в следующий раз выключите и включите сервер.",
                )
            }
        }
    }

    if (confirmRegeneration) {
        DestructiveActionDialog(
            title = "Создать новый токен?",
            message = "Приложения с текущим токеном потеряют доступ, пока вы не обновите в них настройки подключения.",
            confirmLabel = "Создать новый токен",
            onConfirm = {
                onRegenerateKey()
                tokenVisible = false
                confirmRegeneration = false
            },
            onDismiss = { confirmRegeneration = false },
        )
    }
}

@Composable
private fun RemoteShellSection(
    settings: UserSettings,
    onHost: (String) -> Unit,
    onPort: (Int) -> Unit,
    onUser: (String) -> Unit,
    onPassword: (String) -> Unit,
    onHostFingerprint: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            InfoNote(
                "Об удалённой оболочке",
                "Позволяет инструменту shell выполнять команды на удалённом компьютере по SSH " +
                    "(target='remote'). Через SSH также доступен Docker на этом компьютере " +
                    "(docker exec …). Оставьте адрес пустым, чтобы оболочка работала только на устройстве.",
            )

            val hostField = rememberSettingText(settings.sshHost)
            var host by hostField::text
            OutlinedTextField(
                value = host,
                onValueChange = { host = it; onHost(it) },
                label = { Text("Адрес") },
                placeholder = { Text("192.168.1.10 или example.com") },
                singleLine = true,
                colors = hermesFieldColors(),
                modifier = Modifier.fillMaxWidth().settingFocus(hostField),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val userField = rememberSettingText(settings.sshUser)
                var user by userField::text
                OutlinedTextField(
                    value = user,
                    onValueChange = { user = it; onUser(it) },
                    label = { Text("Пользователь") },
                    singleLine = true,
                    colors = hermesFieldColors(),
                    modifier = Modifier.weight(1f).settingFocus(userField),
                )
                val portTextField = rememberSettingText(settings.sshPort.toString())
                var portText by portTextField::text
                OutlinedTextField(
                    value = portText,
                    onValueChange = {
                        portText = it.filter(Char::isDigit).take(5)
                        portText.toIntOrNull()?.let(onPort)
                    },
                    label = { Text("Порт") },
                    singleLine = true,
                    colors = hermesFieldColors(),
                    modifier = Modifier.width(96.dp).settingFocus(portTextField),
                )
            }

            val passwordField = rememberSettingText(settings.sshPassword)
            var password by passwordField::text
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; onPassword(it) },
                label = { Text("Пароль") },
                supportingText = { Text("Хранится зашифрованным на устройстве. Для входа также нужен отпечаток ключа компьютера ниже.") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                colors = hermesFieldColors(),
                modifier = Modifier.fillMaxWidth().settingFocus(passwordField),
            )

            val hostFingerprintField = rememberSettingText(settings.sshHostFingerprint)
            var hostFingerprint by hostFingerprintField::text
            OutlinedTextField(
                value = hostFingerprint,
                onValueChange = { hostFingerprint = it; onHostFingerprint(it) },
                label = { Text("Отпечаток ключа компьютера") },
                placeholder = { Text("SHA256:… или aa:bb:…") },
                supportingText = { Text("Перед сохранением сверьте этот отпечаток по другому каналу.") },
                singleLine = true,
                colors = hermesFieldColors(),
                modifier = Modifier.fillMaxWidth().settingFocus(hostFingerprintField),
            )
        }
    }
}

@Composable
private fun HomeAssistantSection(
    settings: UserSettings,
    onUrl: (String) -> Unit,
    onToken: (String) -> Unit,
    onDashboardPath: (String) -> Unit,
    onDashboardEnabled: (Boolean) -> Unit,
    onTestConnection: ((Boolean, String) -> Unit) -> Unit,
) {
    var tokenVisible by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var testing by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            InfoNote(
                "Об управлении Home Assistant",
                "Управляйте светом, розетками, климатом и сценами через ваш локальный или удалённый Home Assistant.",
            )

            val urlField = rememberSettingText(settings.homeAssistantUrl)
            var url by urlField::text
            OutlinedTextField(
                value = url,
                onValueChange = { url = it; onUrl(it) },
                label = { Text("Базовый URL") },
                placeholder = { Text("http://homeassistant.local:8123") },
                singleLine = true,
                colors = hermesFieldColors(),
                modifier = Modifier.fillMaxWidth().settingFocus(urlField),
            )

            val tokenField = rememberSettingText(settings.homeAssistantToken)
            var token by tokenField::text
            OutlinedTextField(
                value = token,
                onValueChange = { token = it; onToken(it) },
                label = { Text("Долгосрочный токен доступа") },
                supportingText = { Text("Создаётся в Home Assistant: Профиль -> Безопасность -> Долгосрочные токены доступа.") },
                visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = { RevealToggle(tokenVisible) { tokenVisible = !tokenVisible } },
                singleLine = true,
                colors = hermesFieldColors(),
                modifier = Modifier.fillMaxWidth().settingFocus(tokenField),
            )

            OutlinedButton(
                onClick = {
                    testing = true
                    testResult = null
                    onTestConnection { success, message ->
                        testing = false
                        testResult = success to message
                    }
                },
                enabled = !testing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (testing) "Проверка…" else "Проверить подключение")
            }

            testResult?.let { (success, message) ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            val dashboardField = rememberSettingText(settings.homeAssistantDashboardPath)
            var dashboard by dashboardField::text
            OutlinedTextField(
                value = dashboard,
                onValueChange = { dashboard = it; onDashboardPath(it) },
                label = { Text("Путь к панели (необязательно)") },
                placeholder = { Text("lovelace/0") },
                supportingText = { Text("Относительно базового URL. Оставьте пустым для панели по умолчанию.") },
                singleLine = true,
                colors = hermesFieldColors(),
                modifier = Modifier.fillMaxWidth().settingFocus(dashboardField),
            )
            ToggleRow(
                title = "Показывать на главном экране",
                subtitle = "Добавить на главный экран плитку Home Assistant, которая открывает эту панель в приложении.",
                checked = settings.homeAssistantDashboardEnabled,
                onCheckedChange = onDashboardEnabled,
            )
        }
    }
}

/**
 * SPIKE: the in-app Tailscale node. With it running, the gateway URL below is reached over
 * the tailnet with no Tailscale app installed and no VPN permission.
 */
@Composable
private fun TailnetSection(viewModel: SettingsViewModel) {
    val context = LocalContext.current
    var status by remember { mutableStateOf<TailnetStatus?>(null) }
    var busy by remember { mutableStateOf(false) }
    var logs by remember { mutableStateOf("") }
    // Sign-in link already opened for the user's own Start-node tap; keyed by URL so a new
    // link (a fresh login attempt) is opened again, but the same link is not reopened on every poll.
    var startedByUser by remember { mutableStateOf(false) }
    var openedUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            viewModel.refreshTailnet { status = it }
            kotlinx.coroutines.delay(2000)
        }
    }

    val pendingUrl = status?.authUrl
    LaunchedEffect(pendingUrl, startedByUser) {
        if (startedByUser && pendingUrl != null && pendingUrl != openedUrl) {
            openedUrl = pendingUrl
            runCatching {
                context.startActivity(
                    android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(pendingUrl))
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val current = status
            Text(
                text = buildString {
                    append("Состояние: ").append(current?.state ?: "…")
                    current?.hostname?.let { append("\nУзел: ").append(it) }
                    current?.addresses?.takeIf { it.isNotEmpty() }?.let { append("\nАдрес: ").append(it.joinToString(", ")) }
                    current?.error?.let { append("\nОшибка: ").append(it) }
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (current?.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = {
                        busy = true
                        if (current?.running == true) {
                            viewModel.stopTailnet { status = it; busy = false }
                        } else {
                            startedByUser = true
                            openedUrl = null
                            viewModel.startTailnet { status = it; busy = false }
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                    contentPadding = SettingsButtonPadding,
                ) {
                    ButtonLabel(if (current?.running == true) "Остановить узел" else "Запустить узел")
                }
                OutlinedButton(
                    onClick = { viewModel.tailnetLogs { logs = it.takeLast(2000) } },
                    modifier = Modifier.weight(1f),
                    contentPadding = SettingsButtonPadding,
                ) {
                    ButtonLabel("Показать журналы")
                }
            }

            // Sign-in is shown whenever the node needs it, including after an app restart with
            // the node left on. Tailscale's server takes several seconds to return the link, so
            // say so instead of showing nothing.
            if (current?.state == "NeedsLogin") {
                val url = current.authUrl
                if (url == null) {
                    Text(
                        "Получаю ссылку для входа в Tailscale…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Text(
                        "Войдите, чтобы завершить подключение этого устройства к вашей сети tailnet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Button(
                        onClick = {
                            openedUrl = url
                            runCatching {
                                context.startActivity(
                                    android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Войти в Tailscale")
                    }
                    OutlinedButton(
                        onClick = {
                            context.getSystemService(android.content.ClipboardManager::class.java)
                                ?.setPrimaryClip(android.content.ClipData.newPlainText("Ссылка для входа в Tailscale", url))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = SettingsButtonPadding,
                    ) {
                        ButtonLabel("Копировать ссылку")
                    }
                }
            }

            if (logs.isNotBlank()) {
                Text(logs, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            InfoNote(
                "Об этом узле",
                "Экспериментально. Узел работает только внутри этого приложения — без разрешения VPN, и " +
                    "остальной трафик телефона не перенаправляется. Ключ хранится в памяти этого приложения, " +
                    "поэтому войти нужно всего один раз.",
            )
        }
    }
}

@Composable
private fun RemoteGatewaySection(
    settings: UserSettings,
    onToggle: (Boolean) -> Unit,
    onUrl: (String) -> Unit,
    onApiKey: (String) -> Unit,
    onTestConnection: (String, String, (Boolean, String) -> Unit) -> Unit,
) {
    var keyVisible by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var testing by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ToggleRow(
                title = "Использовать удалённый шлюз",
                subtitle = "Передать всю работу агента шлюзу Hermes на ПК. " +
                    "ПК — основное хранилище разговоров; телефон — чат и " +
                    "место для одобрений. Вступает в силу после перезапуска приложения.",
                checked = settings.remoteGatewayEnabled,
                onCheckedChange = onToggle,
            )

            // URL and key stay visible with thin-client mode off: the desktop_bots tool uses them too.
            HorizontalDivider()

            InfoNote(
                "Какой адрес указывать",
                "Укажите шлюз Hermes на вашем ПК. На шлюзе запущен " +
                    "`hermes gateway`, он открывает API-сервер (порт по умолчанию 8642). " +
                    "Используйте имя, которое телефон может распознать: имя mDNS " +
                    "(http://hermes-pc.local:8642) в локальной сети или имя Tailscale " +
                    "MagicDNS (http://mymachine.tailnet.ts.net:8642). " +
                    "Для этих адресов разрешён обычный HTTP; для голого IP-адреса " +
                    "используйте HTTPS, Tailscale Serve или обратный прокси.",
            )

            // Seeded once, not re-keyed on the stored value: persisting per keystroke and
            // re-seeding from the settings flow reorders characters while typing. The settings
            // flow's first emission is the empty default, though, so adopt the stored URL when it
            // arrives and the field has not been edited — otherwise the blank seed was written
            // back over a saved URL.
            var url by rememberSaveable { mutableStateOf(settings.remoteGatewayUrl) }
            var urlEdited by rememberSaveable { mutableStateOf(false) }
            LaunchedEffect(settings.remoteGatewayUrl) {
                if (!urlEdited && url != settings.remoteGatewayUrl) url = settings.remoteGatewayUrl
            }
            OutlinedTextField(
                value = url,
                onValueChange = { url = it; urlEdited = true },
                label = { Text("URL шлюза") },
                placeholder = { Text("http://hermes-pc.local:8642") },
                singleLine = true,
                colors = hermesFieldColors(),
                modifier = Modifier.fillMaxWidth().onFocusChanged { focusState ->
                    if (!focusState.isFocused && url != settings.remoteGatewayUrl) onUrl(url)
                },
            )

            var key by remember(settings.remoteGatewayApiKey) {
                mutableStateOf(settings.remoteGatewayApiKey)
            }
            OutlinedTextField(
                value = key,
                onValueChange = { key = it },
                label = { Text("API-ключ шлюза") },
                supportingText = { Text("API_SERVER_KEY на ПК (задаётся в ~/.hermes/.env).") },
                visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = { RevealToggle(keyVisible) { keyVisible = !keyVisible } },
                singleLine = true,
                colors = hermesFieldColors(),
                modifier = Modifier.fillMaxWidth().onFocusChanged { focusState ->
                    // Persist on focus loss, not per keystroke: the API key
                    // is encrypted with the hardware Keystore on every write,
                    // which is far too slow to run for each character typed.
                    if (!focusState.isFocused && key != settings.remoteGatewayApiKey) {
                        onApiKey(key)
                    }
                },
            )

            val focusManager = LocalFocusManager.current
            SaveBar(
                dirty = url != settings.remoteGatewayUrl || key != settings.remoteGatewayApiKey,
                onSave = {
                    if (url != settings.remoteGatewayUrl) onUrl(url)
                    if (key != settings.remoteGatewayApiKey) onApiKey(key)
                    focusManager.clearFocus()
                },
            )

            // Leaving the screen with a field still focused never reports focus loss, so what was
            // typed was silently dropped. Commit any pending edit on the way out.
            val latestUrl by rememberUpdatedState(url)
            val latestKey by rememberUpdatedState(key)
            val latestSettings by rememberUpdatedState(settings)
            DisposableEffect(Unit) {
                onDispose {
                    if (latestUrl != latestSettings.remoteGatewayUrl) onUrl(latestUrl)
                    if (latestKey != latestSettings.remoteGatewayApiKey) onApiKey(latestKey)
                }
            }

            OutlinedButton(
                onClick = {
                    // The field saves on focus loss; save explicitly too
                    // in case the button press consumed the focus event. The URL as
                    // well: this used to save only the key, so a test could report
                    // "Connected" for a URL that was never stored.
                    if (url != settings.remoteGatewayUrl) onUrl(url)
                    if (key != settings.remoteGatewayApiKey) onApiKey(key)
                    testing = true
                    testResult = null
                    onTestConnection(url, key) { success, message ->
                        testing = false
                        testResult = success to message
                    }
                },
                enabled = !testing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (testing) "Проверка…" else "Проверить подключение")
            }

            testResult?.let { (success, message) ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }

            HorizontalDivider()
            InfoNote(
                "Что делает удалённый шлюз",
                "Когда включено, разговоры на этом телефоне связываются с сессиями шлюза " +
                    "на ПК. Сообщение, отправленное с телефона, появляется на ПК, и " +
                    "наоборот. Запросы на одобрение инструментов приходят в обычное " +
                    "окно одобрения на телефоне.",
            )
            HorizontalDivider()
            InfoNote(
                "Безопасность",
                "Телефон получает все полномочия шлюза " +
                    "на ПК — терминал, работу с файлами, всё, что умеет " +
                    "агент. API-ключ хранится в Android Keystore и " +
                    "не покидает устройство, но любой, у кого есть ключ, может управлять " +
                    "агентом. Для ограниченного подключения настройте отдельный " +
                    "профиль шлюза (например, /p/phone/) с урезанным набором инструментов " +
                    "и укажите здесь URL с префиксом этого профиля. Ограничения " +
                    "применяет шлюз — телефон не может сам ограничить инструменты, " +
                    "которые ПК уже запустил.",
                textColor = MaterialTheme.colorScheme.error,
            )
        }
    }
}


/**
 * Registry for Model Context Protocol servers.
 *
 * This is the only place a server can be added, so without it the `mcp_servers`
 * table stayed empty for every install: no MCP tool was ever registered, and the
 * tool-search bridge never had anything to defer. Adding a server syncs it
 * immediately so a bad URL is visible here rather than as silence in chat.
 */
@Composable
private fun McpServersSection(
    viewModel: McpSettingsViewModel = hiltViewModel(),
) {
    val servers by viewModel.servers.collectAsStateWithLifecycle()
    val toolCounts by viewModel.toolCounts.collectAsStateWithLifecycle()
    val busyServerId by viewModel.busyServerId.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<McpServerConfig?>(null) }
    var banner by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            InfoNote(
                "О MCP-серверах",
                "Подключайте серверы Model Context Protocol по HTTP или SSE. Их инструменты " +
                    "становятся доступны агенту, с отдельным пространством имён для каждого сервера и подтверждением перед " +
                    "запуском.",
            )

            if (servers.isEmpty()) {
                Text(
                    "Серверы не настроены. Пока вы их не добавите, у агента нет MCP-инструментов.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            servers.forEach { server ->
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(server.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            server.url,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        val count = toolCounts[server.id] ?: 0
                        Text(
                            "${server.transport.name} - инструментов: $count",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        server.lastError?.let { err ->
                            Text(
                                "Последняя ошибка: $err",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    Switch(
                        checked = server.enabled,
                        onCheckedChange = { viewModel.setEnabled(server.id, it) },
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            banner = null
                            viewModel.sync(server.id) { ok, message -> banner = ok to message }
                        },
                        enabled = server.enabled && busyServerId == null,
                        modifier = Modifier.weight(1f),
                        contentPadding = SettingsButtonPadding,
                    ) {
                        ButtonLabel(if (busyServerId == server.id) "Синхронизация..." else "Синхронизировать инструменты")
                    }
                    OutlinedButton(
                        onClick = { pendingDelete = server },
                        modifier = Modifier.weight(1f),
                        contentPadding = SettingsButtonPadding,
                    ) {
                        ButtonLabel("Удалить")
                    }
                }
            }

            banner?.let { (ok, message) ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }

            HorizontalDivider()
            OutlinedButton(
                onClick = { banner = null; showAddDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Добавить MCP-сервер")
            }
        }
    }

    if (showAddDialog) {
        AddMcpServerDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, url, transport, headerName, headerValue ->
                viewModel.addServer(name, url, transport, headerName, headerValue) { ok, message ->
                    banner = ok to message
                    if (ok) showAddDialog = false
                }
            },
        )
    }

    pendingDelete?.let { server ->
        DestructiveActionDialog(
            title = "Удалить ${server.name}?",
            message = "Его инструменты будут отключены, а сохранённый каталог удалён. Сам сервер " +
                "не затрагивается.",
            confirmLabel = "Удалить",
            onConfirm = {
                viewModel.deleteServer(server.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun AddMcpServerDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, McpTransportType, String, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var transport by remember { mutableStateOf(McpTransportType.HTTP) }
    var headerName by remember { mutableStateOf("") }
    var headerValue by remember { mutableStateOf("") }
    var headerVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить MCP-сервер") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Имя") },
                    placeholder = { Text("context7") },
                    singleLine = true,
                    colors = hermesFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") },
                    placeholder = { Text("https://example.com/mcp") },
                    singleLine = true,
                    colors = hermesFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                InfoNote(
                    "Об адресе",
                    "Адрес HTTP или SSE. Серверы, которые работают как локальный процесс, " +
                        "в песочнице приложения не поддерживаются — запустите их в Termux и укажите здесь " +
                        "их порт на localhost.",
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    McpTransportType.entries.forEach { option ->
                        FilterChip(
                            selected = transport == option,
                            onClick = { transport = option },
                            label = { Text(option.name) },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                OutlinedTextField(
                    value = headerName,
                    onValueChange = { headerName = it },
                    label = { Text("Имя заголовка авторизации (необязательно)") },
                    placeholder = { Text("Authorization") },
                    singleLine = true,
                    colors = hermesFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = headerValue,
                    onValueChange = { headerValue = it },
                    label = { Text("Значение заголовка авторизации (необязательно)") },
                    visualTransformation = if (headerVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    singleLine = true,
                    colors = hermesFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = { headerVisible = !headerVisible }) {
                    Text(if (headerVisible) "Скрыть значение" else "Показать значение")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(name, url, transport, headerName, headerValue) }) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}
