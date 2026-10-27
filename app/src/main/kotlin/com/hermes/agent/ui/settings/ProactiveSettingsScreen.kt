package com.hermes.agent.ui.settings
import com.hermes.agent.domain.settings.*

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermes.agent.domain.proactive.ProactiveSource

/**
 * Settings → Proactive. Per-capability consent (roadmap v0.12): each source
 * is an explicit opt-in switch, quiet hours and the daily cap are shown, and
 * a source muted by "Less of this" can be restored here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProactiveSettingsScreen(
    onBack: () -> Unit,
    viewModel: ProactiveSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Проактивность") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            InfoNote(
                "Когда Hermes пишет сам",
                "Hermes пишет сам, только если ниже включена соответствующая функция, вне тихих часов " +
                    "(${state.quietLabel}), не больше ${state.dailyCap} раз в день и никогда " +
                    "в режиме «Не беспокоить». Каждое такое сообщение — отправленное или подавленное — видно в разделе " +
                    "«Что сделал Hermes».",
            )
            Spacer(Modifier.height(16.dp))

            state.sources.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = row.source.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = when {
                                row.muted -> "Отключено кнопкой «Поменьше такого»"
                                row.source == ProactiveSource.SCHEDULED_TASK ->
                                    "Результаты ваших задач CRON"
                                row.source == ProactiveSource.DIGEST ->
                                    "Одна утренняя сводка (погода, календарь, дела)"
                                else -> "Напоминает об обещаниях, которые вы дали в чате"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (row.muted) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    if (row.muted) {
                        TextButton(onClick = { viewModel.unmute(row.source) }) { Text("Вернуть") }
                    }
                    Switch(
                        checked = row.consented,
                        onCheckedChange = { viewModel.setConsent(row.source, it) },
                    )
                }
                HorizontalDivider()
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DescribedTitle(
                    title = "Сводка уведомлений в дайджесте",
                    description = "Также нужно выдать Hermes доступ к уведомлениям в настройках " +
                        "системы. Захваченный текст попадает только в ваш дайджест — " +
                        "ИИ его никогда не видит. При выключении захваченные данные удаляются.",
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = state.notificationCapture,
                    onCheckedChange = { viewModel.setNotificationCapture(it) },
                )
            }
            val context = LocalContext.current
            TextButton(
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
                    )
                },
            ) { Text("Открыть доступ к уведомлениям") }
            HorizontalDivider()
        }
    }
}
