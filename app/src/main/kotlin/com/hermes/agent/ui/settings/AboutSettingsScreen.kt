package com.hermes.agent.ui.settings
import com.hermes.agent.domain.settings.*

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermes.agent.BuildConfig
import com.hermes.agent.R
import com.hermes.agent.ui.theme.hermesSwitchColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val hotfixViewModel: HotfixViewModel = hiltViewModel()
    val hotfixStatus by hotfixViewModel.status.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("О приложении, разрешения и безопасность") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionHeader(text = "Разрешения приложения")
            PermissionsCard()

            SectionHeader(text = "Сопутствующие приложения")
            CompanionAppsCard()

            SectionHeader(text = stringResource(R.string.settings_section_security))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    InfoRow(
                        title = stringResource(R.string.settings_keystore_status),
                        value = "Аппаратная защита (Android Keystore)",
                    )
                }
            }
            ExpandableCard(
                title = "Проверка безопасности",
                subtitle = securityAuditSummary,
            ) {
                SecurityAuditRows()
            }

            SectionHeader(text = stringResource(R.string.settings_section_about))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    InfoRow(title = "Приложение", value = "Hermes")
                    InfoRow(
                        title = stringResource(R.string.settings_app_version),
                        value = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    )
                    InfoRow(title = "Тип сборки", value = BuildConfig.BUILD_TYPE)
                }
            }

            if (BuildConfig.OTA_ENABLED) {
                SectionHeader(text = "Обновления")
                UpdateSection(
                    state = updateState,
                    canInstall = viewModel.canInstallPackages(),
                    onCheck = viewModel::checkForUpdate,
                    onDownload = viewModel::downloadAndInstall,
                    onManagePermission = viewModel::promptInstallPermission,
                    onOpenUrl = { url ->
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        viewModel.dismissUpdateState()
                    },
                    onDismiss = viewModel::dismissUpdateState,
                    onApplyFix = { offer ->
                        hotfixViewModel.apply(offer)
                        viewModel.dismissUpdateState()
                    },
                    onChooseFullUpdate = viewModel::chooseFullUpdate,
                )
                HotfixStatusCard(
                    status = hotfixStatus,
                    onRestart = hotfixViewModel::restartNow,
                    onRemove = hotfixViewModel::remove,
                )
                var testBuilds by remember { androidx.compose.runtime.mutableStateOf(viewModel.otaTestBuilds) }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Тестовые сборки", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Также предлагать исправления, которые система самовосстановления выпустила как тестовые сборки, до того как они " +
                                    "дойдут до всех. Сбой в тестовой сборке откатывает её.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = testBuilds, onCheckedChange = { testBuilds = it; viewModel.otaTestBuilds = it })
                    }
                }
            }
        }
    }
}

// --- Permissions ---------------------------------------------------------------

/**
 * A user-facing permission the app can use. [runtimePermission] is set for the
 * ones granted through a system dialog; the rest are "special access" toggled on
 * their own settings screen. [isGranted]/[grantIntent] resolve the real state
 * and destination — the `requestedPermissionsFlags` bit is unreliable for
 * special-access grants (e.g. All files access), which is why each entry checks
 * the actual platform API instead.
 */
private class PermissionEntry(
    val label: String,
    val description: String,
    val manifestName: String,
    val runtimePermission: String? = null,
    val isGranted: (Context) -> Boolean,
    val grantIntent: (Context) -> Intent,
)

private fun appDetailsIntent(context: Context) = Intent(
    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
    Uri.fromParts("package", context.packageName, null),
)

private fun runtimeGranted(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

private val PERMISSION_ENTRIES: List<PermissionEntry> = listOf(
    PermissionEntry(
        "Микрофон", "Голосовой ввод и режим разговора без рук.",
        "android.permission.RECORD_AUDIO", "android.permission.RECORD_AUDIO",
        { runtimeGranted(it, "android.permission.RECORD_AUDIO") }, ::appDetailsIntent,
    ),
    PermissionEntry(
        "Камера", "Позволяет агенту делать или прикреплять фото.",
        "android.permission.CAMERA", "android.permission.CAMERA",
        { runtimeGranted(it, "android.permission.CAMERA") }, ::appDetailsIntent,
    ),
    PermissionEntry(
        "Показ уведомлений", "Ответы, результаты задач и напоминания.",
        "android.permission.POST_NOTIFICATIONS", "android.permission.POST_NOTIFICATIONS",
        {
            Build.VERSION.SDK_INT < 33 ||
                runtimeGranted(it, "android.permission.POST_NOTIFICATIONS")
        },
        ::appDetailsIntent,
    ),
    PermissionEntry(
        "Точное местоположение", "Определение присутствия и ответы с учётом местоположения.",
        "android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_FINE_LOCATION",
        { runtimeGranted(it, "android.permission.ACCESS_FINE_LOCATION") }, ::appDetailsIntent,
    ),
    PermissionEntry(
        "Чтение контактов", "Поиск имён при звонках и сообщениях.",
        "android.permission.READ_CONTACTS", "android.permission.READ_CONTACTS",
        { runtimeGranted(it, "android.permission.READ_CONTACTS") }, ::appDetailsIntent,
    ),
    PermissionEntry(
        "Календарь", "Чтение и создание событий календаря.",
        "android.permission.READ_CALENDAR", "android.permission.READ_CALENDAR",
        { runtimeGranted(it, "android.permission.READ_CALENDAR") }, ::appDetailsIntent,
    ),
    PermissionEntry(
        "Отправка SMS", "Отправка сообщений, которые вы поручили агенту.",
        "android.permission.SEND_SMS", "android.permission.SEND_SMS",
        { runtimeGranted(it, "android.permission.SEND_SMS") }, ::appDetailsIntent,
    ),
    PermissionEntry(
        "Доступ ко всем файлам", "Сохранять скачанные модели в папку, которую можно открыть.",
        "android.permission.MANAGE_EXTERNAL_STORAGE",
        isGranted = { Build.VERSION.SDK_INT >= 30 && Environment.isExternalStorageManager() },
        grantIntent = {
            Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.fromParts("package", it.packageName, null),
            )
        },
    ),
    PermissionEntry(
        "Поверх других приложений", "Показывать окно ассистента поверх других приложений.",
        "android.permission.SYSTEM_ALERT_WINDOW",
        isGranted = { Settings.canDrawOverlays(it) },
        grantIntent = {
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.fromParts("package", it.packageName, null),
            )
        },
    ),
    PermissionEntry(
        "Изменение системных настроек", "Менять яркость, громкость и похожие настройки устройства.",
        "android.permission.WRITE_SETTINGS",
        isGranted = { Settings.System.canWrite(it) },
        grantIntent = {
            Intent(
                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.fromParts("package", it.packageName, null),
            )
        },
    ),
    PermissionEntry(
        "Без оптимизации батареи", "Надёжная работа фоновых задач.",
        "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
        isGranted = {
            (it.getSystemService(Context.POWER_SERVICE) as PowerManager)
                .isIgnoringBatteryOptimizations(it.packageName)
        },
        grantIntent = { Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS) },
    ),
    PermissionEntry(
        "Установка неизвестных приложений", "Установка обновлений, скачанных с GitHub.",
        "android.permission.REQUEST_INSTALL_PACKAGES",
        isGranted = { it.packageManager.canRequestPackageInstalls() },
        grantIntent = {
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.fromParts("package", it.packageName, null),
            )
        },
    ),
    PermissionEntry(
        "Управление режимом «Не беспокоить»", "Позволяет агенту отключать и возвращать уведомления.",
        "android.permission.ACCESS_NOTIFICATION_POLICY",
        isGranted = {
            (it.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .isNotificationPolicyAccessGranted
        },
        grantIntent = { Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS) },
    ),
)

@Composable
private fun PermissionsCard() {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }

    val declaredNames: Set<String> = remember {
        runCatching {
            context.packageManager
                .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
                .requestedPermissions
                ?.toSet()
                ?: emptySet()
        }.getOrDefault(emptySet())
    }
    val entries = remember(declaredNames) {
        PERMISSION_ENTRIES.filter { entry ->
            entry.manifestName in declaredNames &&
                (entry.manifestName != "android.permission.MANAGE_EXTERNAL_STORAGE" || Build.VERSION.SDK_INT >= 30)
        }
    }

    val requestLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { refresh++ }
    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { refresh++ }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            entries.forEachIndexed { index, entry ->
                if (index > 0) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                val granted = remember(refresh, entry) { entry.isGranted(context) }
                PermissionToggleRow(
                    label = entry.label,
                    description = entry.description,
                    granted = granted,
                    onToggle = {
                        when {
                            granted -> settingsLauncher.launch(appDetailsIntent(context))
                            entry.runtimePermission != null ->
                                requestLauncher.launch(entry.runtimePermission)
                            else -> settingsLauncher.launch(entry.grantIntent(context))
                        }
                    },
                )
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            InfoNote(
                "Как работают переключатели",
                "Переключатель открывает системный экран, где выдаётся разрешение, — Android не даёт приложению менять собственные разрешения напрямую.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun PermissionToggleRow(
    label: String,
    description: String,
    granted: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DescribedTitle(title = label, description = description, modifier = Modifier.weight(1f))
        Switch(
            checked = granted,
            onCheckedChange = { onToggle() },
            colors = hermesSwitchColors(),
        )
    }
}

// --- Companion apps ----------------------------------------------------------

private const val TERMUX_PKG = "com.termux"
private const val SHIZUKU_PKG = "moe.shizuku.privileged.api"

@Composable
private fun CompanionAppsCard() {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }

    fun installed(pkg: String): Boolean = runCatching {
        context.packageManager.getPackageInfo(pkg, 0); true
    }.getOrDefault(false)

    fun openOrGet(pkg: String) {
        val launch = context.packageManager.getLaunchIntentForPackage(pkg)
        if (launch != null) {
            context.startActivity(launch)
        } else {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://f-droid.org/en/packages/$pkg/")),
            )
        }
        refresh++
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            CompanionToggleRow(
                "Termux",
                "Локальная оболочка Linux — работает инструмент termux (пакеты, python, git).",
                remember(refresh) { installed(TERMUX_PKG) },
                { openOrGet(TERMUX_PKG) },
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            CompanionToggleRow(
                "Shizuku",
                "Мост с правами ADB — позволяет инструменту shell работать с повышенными правами.",
                remember(refresh) { installed(SHIZUKU_PKG) },
                { openOrGet(SHIZUKU_PKG) },
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            InfoNote(
                "Как работают переключатели",
                "Вкл. → открывает приложение. Выкл. → открывает его страницу в F-Droid для установки. Удаляется через лаунчер.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun CompanionToggleRow(
    name: String,
    subtitle: String,
    isInstalled: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DescribedTitle(title = name, description = subtitle, modifier = Modifier.weight(1f))
        Switch(
            checked = isInstalled,
            onCheckedChange = { onToggle() },
            colors = hermesSwitchColors(),
        )
    }
}
