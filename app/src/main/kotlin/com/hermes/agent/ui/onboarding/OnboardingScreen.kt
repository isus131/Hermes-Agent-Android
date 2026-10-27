package com.hermes.agent.ui.onboarding
import com.hermes.agent.domain.settings.*

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.hermes.agent.data.device.DeviceProfile
import com.hermes.agent.ui.home.HermesPersona.Mood
import com.hermes.agent.ui.bloub.ExpressionId
import com.hermes.agent.ui.bloub.HermesBot
import com.hermes.agent.core.theme.Geist
import com.hermes.agent.core.theme.GeistMono

/**
 * Multi-step setup journey: welcome → profile → device scan.
 * Collected profile + scanned device capabilities are committed to the agent's
 * memory on finish (see [OnboardingViewModel]).
 */
@Composable
fun OnboardingScreen(
    onCompleted: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val step by viewModel.step.collectAsStateWithLifecycle()
    val completed by viewModel.completed.collectAsStateWithLifecycle()

    if (completed) {
        onCompleted()
        return
    }

    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
            .background(
                Brush.radialGradient(
                    colors = listOf(scheme.primary.copy(alpha = 0.13f), scheme.background),
                    radius = 1000f,
                )
            )
            .padding(horizontal = 24.dp, vertical = 26.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            StepDots(current = step, total = OnboardingViewModel.DEVICE + 1)
            Spacer(Modifier.height(18.dp))
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (step) {
                    OnboardingViewModel.WELCOME -> WelcomeStep()
                    OnboardingViewModel.RESTORE -> RestoreStep(viewModel)
                    OnboardingViewModel.PROFILE -> ProfileStep(viewModel)
                    OnboardingViewModel.PERMISSIONS -> PermissionsStep()
                    else -> DeviceStep(viewModel)
                }
            }
            Spacer(Modifier.height(16.dp))
            NavBar(step = step, viewModel = viewModel)
        }
    }
}

@Composable
private fun StepDots(current: Int, total: Int) {
    val scheme = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { i ->
            Box(
                modifier = Modifier
                    .height(4.dp)
                    .width(if (i == current) 22.dp else 10.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (i <= current) scheme.primary else scheme.surfaceVariant),
            )
        }
    }
}

@Composable
private fun WelcomeStep() {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // The assistant introduces itself in person: the same face used across
        // the app, greeting the user before it asks them for anything.
        HermesBot(
            mood = Mood.HAPPY,
            size = 176.dp,
            // Pleased to meet you: it arrives with the turn — the eyes travel a
            // full circuit of the ball, so it reads as spinning on the spot — and
            // settles on the wide, eager eyes of the `excite` pose.
            expression = ExpressionId.EXCITE,
            arrival = true,
            label = "Hermes",
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "Давайте настроим\nвашего ассистента",
            fontFamily = Geist,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 36.sp,
            color = scheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "Немного данных о вас, разрешения, которые нужны Hermes, и быстрый осмотр телефона — " +
                "всё сохраняется в память, чтобы агент знал вас и ваше устройство.",
            style = MaterialTheme.typography.bodyLarge,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 300.dp),
        )
    }
}

@Composable
private fun RestoreStep(viewModel: OnboardingViewModel) {
    val scheme = MaterialTheme.colorScheme
    var restorePassword by androidx.compose.runtime.saveable.rememberSaveable {
        androidx.compose.runtime.mutableStateOf("")
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.restoreBackup(uri, restorePassword.ifBlank { null })
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        androidx.compose.material3.Icon(
            imageVector = Icons.Outlined.Restore,
            contentDescription = null,
            tint = scheme.primary,
            modifier = Modifier.size(52.dp)
        )
        Spacer(Modifier.height(22.dp))
        Text(
            "Восстановить резервную копию?",
            fontFamily = Geist,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = scheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "Если на этом устройстве есть файл резервной копии, его можно восстановить сейчас. "
                + "Остальная настройка будет пропущена.",
            style = MaterialTheme.typography.bodyLarge,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 300.dp),
        )
        Spacer(Modifier.height(20.dp))
        // Without this an encrypted backup would be a dead end at first run,
        // with no way forward but to skip setup and go to Settings.
        androidx.compose.material3.OutlinedTextField(
            value = restorePassword,
            onValueChange = { restorePassword = it },
            label = { Text("Пароль (только если он есть у копии)") },
            singleLine = true,
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            modifier = Modifier.widthIn(max = 320.dp),
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { launcher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
            shape = MaterialTheme.shapes.medium,
        ) {
            Text("Выбрать файл копии")
        }
    }
}

@Composable
private fun ProfileStep(viewModel: OnboardingViewModel) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    StepHeader("О вас", "Hermes запоминает это, чтобы действовать от вашего имени.")
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Spacer(Modifier.height(76.dp)) // clear the header
        Field("Имя", profile.name) { v -> viewModel.update { it.copy(name = v) } }
        Field("Домашний адрес", profile.address) { v -> viewModel.update { it.copy(address = v) } }
        Field("Номер телефона", profile.phone, KeyboardType.Phone) { v -> viewModel.update { it.copy(phone = v) } }
        Field("Эл. почта", profile.email, KeyboardType.Email) { v -> viewModel.update { it.copy(email = v) } }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) {
                Field("Время подъёма (например, 07:00)", profile.wakeTime) { v -> viewModel.update { it.copy(wakeTime = v) } }
            }
            Box(Modifier.weight(1f)) {
                Field("Время отхода ко сну (например, 23:00)", profile.sleepTime) { v -> viewModel.update { it.copy(sleepTime = v) } }
            }
        }
        Field("Что ещё Hermes стоит знать", profile.notes, singleLine = false) { v ->
            viewModel.update { it.copy(notes = v) }
        }
        Spacer(Modifier.height(8.dp))
    }
}



@Composable
private fun DeviceStep(viewModel: OnboardingViewModel) {
    val scheme = MaterialTheme.colorScheme
    val device by viewModel.device.collectAsStateWithLifecycle()
    val scanning by viewModel.scanning.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    StepHeader("Ваше устройство", "Hermes проверяет возможности телефона, чтобы подстроить функции под ваше устройство.")
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(76.dp))
        when {
            scanning -> Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.size(12.dp))
                Text("Сканирую оборудование…", color = scheme.onSurfaceVariant)
            }
            error != null && device == null -> Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Не удалось просканировать: $error",
                    color = scheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.scanDevice() },
                    shape = MaterialTheme.shapes.medium
                ) { Text("Повторить сканирование") }
            }
            device == null -> Button(
                onClick = { viewModel.scanDevice() },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            ) { Text("Просканировать устройство") }
            else -> DeviceCard(device!!)
        }
    }
}

@Composable
private fun DeviceCard(d: DeviceProfile) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(scheme.surface)
            .border(1.dp, scheme.outline.copy(alpha = 0.25f), MaterialTheme.shapes.medium)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        SpecRow("Устройство", "${d.manufacturer} ${d.model}")
        SpecRow("Android", "${d.androidRelease} (API ${d.sdkInt})")
        SpecRow("Чипсет", d.soc)
        SpecRow("Процессор", "ядер: ${d.cpuCores} · ${d.abi}")
        SpecRow("ОЗУ", "${oneDp(d.totalRamGb)} ГБ")
        SpecRow("Память", "свободно ${oneDp(d.freeStorageGb)} ГБ из ${oneDp(d.totalStorageGb)} ГБ")
        SpecRow("Графика", d.gpuRenderer.ifBlank { "unknown" })
        SpecRow("Экран", d.screen)
        if (d.batteryPct in 0..100) SpecRow("Батарея", "${d.batteryPct}%")
        SpecRow("Датчики", "${d.sensors.size} detected")
        Spacer(Modifier.height(2.dp))
        Text(
            "Сохранено в память — по этим данным агент решает, что запускать на устройстве.",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.outline,
        )
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    val scheme = MaterialTheme.colorScheme
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(label, fontFamily = GeistMono, fontSize = 12.sp, color = scheme.outline, modifier = Modifier.width(74.dp))
        Spacer(Modifier.size(10.dp))
        Text(value, fontSize = 13.sp, color = scheme.onSurface, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun PermissionRow(title: String, why: String, permission: String) {
    PermissionGroupRow(title, why, listOf(permission))
}

@Composable
private fun PermissionGroupRow(title: String, why: String, permissions: List<String>) {
    val scheme = MaterialTheme.colorScheme
    val context = androidx.compose.ui.platform.LocalContext.current
    var isGranted by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(
            permissions.all { permission ->
                androidx.core.content.ContextCompat.checkSelfPermission(context, permission) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        )
    }
    
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        isGranted = permissions.all { grants[it] == true }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(scheme.surface)
            .border(1.dp, scheme.outline.copy(alpha = 0.2f), MaterialTheme.shapes.small)
            .padding(horizontal = 13.dp, vertical = 11.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = scheme.onSurface)
                Text(why, fontSize = 12.5.sp, color = scheme.onSurfaceVariant)
            }
            if (isGranted) {
                Text("Разрешено", color = scheme.primary, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            } else {
                TextButton(onClick = { launcher.launch(permissions.toTypedArray()) }) {
                    Text("Разрешить")
                }
            }
        }
    }
}

@Composable
private fun PermissionsStep() {
    val scheme = MaterialTheme.colorScheme

    StepHeader("Разрешения", "Выдайте то, что нужно Hermes для помощи, — изменить это можно позже в настройках системы.")
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Spacer(Modifier.height(76.dp))
        PermissionRow("Микрофон", "Голосовой ввод и команды", Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionRow("Уведомления", "Напоминания и новости о задачах", Manifest.permission.POST_NOTIFICATIONS)
        }
        PermissionRow("Местоположение", "Ответы и напоминания с учётом местоположения", Manifest.permission.ACCESS_FINE_LOCATION)
        PermissionRow("Контакты", "Поиск людей по имени и сообщения им", Manifest.permission.READ_CONTACTS)
        PermissionGroupRow(
            "Календарь",
            "Чтение и создание событий",
            listOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR),
        )
        PermissionRow("Камера", "Съёмка и анализ изображений по запросу", Manifest.permission.CAMERA)
        PermissionRow("Команды Termux", "Позволить Hermes запускать полного агента в Termux", "com.termux.permission.RUN_COMMAND")
        Spacer(Modifier.height(6.dp))
        Text(
            "Каждое разрешение запрашивается отдельно — одобрите те, которые вас устраивают.",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.outline,
        )
    }
}

@Composable
private fun StepHeader(title: String, subtitle: String) {
    val scheme = MaterialTheme.colorScheme
    Column {
        Text(title, fontFamily = Geist, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = scheme.onBackground)
        Spacer(Modifier.height(5.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun NavBar(step: Int, viewModel: OnboardingViewModel) {
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    
    // K09: this row sits at the bottom of the screen, so on gesture navigation
    // the system bar overlapped "Skip setup" and "Get started" and clipped their
    // labels. The inset has to be consumed here rather than on the scrolling
    // content above it, which is why it is not handled by the parent Scaffold.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
    ) {
        if (error != null) {
            Text(
                text = error ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (step > OnboardingViewModel.WELCOME) {
                TextButton(onClick = { viewModel.back() }, enabled = !saving) { Text("Назад") }
            } else {
                TextButton(onClick = { viewModel.skip() }, enabled = !saving) { Text("Пропустить настройку") }
            }
            Spacer(Modifier.weight(1f))
            if (step == OnboardingViewModel.RESTORE) {
                Button(onClick = { viewModel.next() }, shape = MaterialTheme.shapes.medium, enabled = !saving) {
                    Text("Пропустить восстановление")
                }
            } else if (step < OnboardingViewModel.DEVICE) {
                Button(onClick = { viewModel.next() }, shape = MaterialTheme.shapes.medium, enabled = !saving) {
                    Text(if (step == OnboardingViewModel.WELCOME) "Начать" else "Далее")
                }
            } else {
                Button(onClick = { viewModel.finish() }, shape = MaterialTheme.shapes.medium, enabled = !saving) {
                    if (saving) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("Сохранение...")
                    } else {
                        Text(if (error != null) "Повторить" else "Завершить настройку")
                    }
                }
            }
        }
    }
}

private fun oneDp(v: Double) = (Math.round(v * 10) / 10.0)
