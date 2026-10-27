package com.hermes.agent.ui.settings
import com.hermes.agent.domain.settings.*

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.SettingsEthernet
import androidx.compose.material.icons.outlined.Stars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hermes.agent.R
import com.hermes.agent.ui.components.SlimTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigate: (String) -> Unit = {},
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    fun nav(route: String): () -> Unit = { onNavigate(route) }

    Scaffold(
        modifier = Modifier.imePadding(),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.nav_settings),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.statusBarsPadding().padding(start = 8.dp, top = 12.dp, bottom = 8.dp),
            )
            SettingsGroup(
                "Ассистент и оформление",
                listOf(
                    NavItem(Icons.Outlined.AccountCircle, "Ассистент", "Провайдеры, локальная модель, голос, постоянные инструкции", nav("settings_assistant")),
                    NavItem(Icons.Outlined.ColorLens, "Оформление", "Тема, тёмный режим и акцентный цвет", nav("settings_appearance")),
                ),
            )

            SettingsGroup(
                "Подключения и автоматизация",
                listOf(
                    NavItem(Icons.Outlined.SettingsEthernet, "Подключения", "Home Assistant, удалённая оболочка, MCP-серверы, API-сервер", nav("settings_connections")),
                    NavItem(Icons.Outlined.Link, "Мессенджеры", "Шлюзы Telegram, Discord, Signal, WhatsApp", nav("connect")),
                    NavItem(Icons.Outlined.Schedule, "CRON-задачи", "Повторяющиеся задачи агента по расписанию", nav("schedule")),
                    NavItem(Icons.AutoMirrored.Outlined.Send, "Поручения", "Фоновые задачи агента и их результаты", nav("delegate")),
                    NavItem(Icons.Outlined.Notifications, "Проактивность", "Дайджест, напоминания, тихие часы, лимит сообщений", nav("settings_proactive")),
                    NavItem(Icons.Outlined.Extension, "Модули", "Скачать проверенные модули из общего репозитория", nav("settings_modules")),
                ),
            )

            SettingsGroup(
                "Знания и навыки",
                listOf(
                    NavItem(Icons.Outlined.Psychology, "Память", "Просмотр воспоминаний агента и управление ими", nav("memory")),
                    NavItem(Icons.Outlined.AutoAwesome, "Обучение", "Выученные факты, ваш профиль, автоматические навыки", nav("learning")),
                    NavItem(Icons.AutoMirrored.Outlined.LibraryBooks, "База знаний", "Документы и файлы, проиндексированные для поиска", nav("documents")),
                    NavItem(Icons.Outlined.Stars, "Навыки и инструменты", "Просмотр навыков и инструментов агента и управление ими", nav("skills")),
                    NavItem(Icons.Outlined.Science, "Улучшение навыков", "Улучшить навык по тому, как он на самом деле использовался", nav("refine_skills")),
                    NavItem(Icons.Outlined.Description, "Рабочие заметки агентов", "Выученные подсказки поверх промпта каждого агента", nav("refine_prompts")),
                    NavItem(Icons.Outlined.AutoAwesome, "Развитие функций", "Улучшения по вашему использованию, собранные и проверенные вашими ботами", nav("evolution")),
                ),
            )

            SettingsGroup(
                "Активность и диагностика",
                listOf(
                    NavItem(Icons.Outlined.Insights, "Использование и расходы", "Токены, примерные траты, число вызовов инструментов", nav("usage_insights")),
                    NavItem(Icons.Outlined.History, "Что сделал Hermes", "Журнал действий: запуски инструментов и поручённые задачи", nav("activity_ledger")),
                    NavItem(Icons.Outlined.Science, "A/B-эксперимент", "Сравнить две модели на одном запросе", nav("experiment")),
                    NavItem(Icons.Outlined.Article, "Журналы", "Просмотр, копирование и отправка журналов приложения для диагностики", nav("logs")),
                ),
            )

            SettingsGroup(
                "Устройство и безопасность",
                listOf(
                    NavItem(Icons.Outlined.Accessibility, "Управление приложениями", "Спец. возможности — позволяют агенту управлять другими приложениями") {
                        context.startActivity(android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    NavItem(Icons.Outlined.Build, "Дополнительно", "Резервные копии, обновления, доступ к файлам, саморазвитие", nav("settings_advanced")),
                    NavItem(Icons.Outlined.Info, "О приложении, разрешения и безопасность", "Версия, разрешения приложения, сопутствующие приложения, проверка безопасности", nav("settings_about")),
                ),
            )
        }
    }
}
