package com.hermes.agent.ui.navigation
import com.hermes.agent.domain.settings.*

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Stars
import androidx.compose.material.icons.outlined.ViewColumn
import androidx.compose.ui.graphics.vector.ImageVector

enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    HOME(         route = "home",                 label = "Главная",       icon = Icons.Outlined.Home),
    CONVERSATIONS(route = "conversations",         label = "Чаты",      icon = Icons.Outlined.Chat),
    CHAT(         route = "chat/{conversationId}", label = "Чат",       icon = Icons.Outlined.Chat),
    DOCUMENTS(    route = "documents",             label = "Артефакты",      icon = Icons.Outlined.Description),
    SKILLS(       route = "skills",               label = "Навыки и инструменты", icon = Icons.Outlined.Stars),
    CONNECT(      route = "connect",              label = "Мессенджеры",      icon = Icons.Outlined.Link),
    SCHEDULE(     route = "schedule",             label = "CRON",           icon = Icons.Outlined.Schedule),
    DELEGATE(     route = "delegate",             label = "Поручения",   icon = Icons.AutoMirrored.Outlined.Send),
    EXPERIMENT(   route = "experiment",           label = "Эксперимент", icon = Icons.Outlined.Science),
    SETTINGS(     route = "settings",             label = "Настройки",   icon = Icons.Outlined.Settings),
    MEMORY(       route = "memory",               label = "Память",     icon = Icons.Outlined.Psychology),
    KANBAN(       route = "kanban",               label = "Доска",      icon = Icons.Outlined.ViewColumn),
    TICKET(       route = "ticket/{ticketId}",     label = "Задача",     icon = Icons.Outlined.ViewColumn);

    companion object {
        fun chatRoute(conversationId: String) = "chat/$conversationId"
        fun ticketRoute(ticketId: String) = "ticket/$ticketId"
    }
}
