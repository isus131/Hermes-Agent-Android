package com.hermes.agent.ui.chat.components
import com.hermes.agent.domain.settings.*

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.ViewColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hermes.agent.core.theme.GeistMono

data class SlashCommand(
    val command: String,
    val syntax: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val template: String,
    /** Set for a command the app carries out itself instead of prefilling a prompt. */
    val action: SlashAction? = null,
)

/**
 * Commands that are app actions, not prompts. /export used to prefill "Export this conversation
 * trajectory" for the model, which has no tool that exports a chat and said so.
 */
enum class SlashAction { EXPORT }

val HERMES_SLASH_COMMANDS = listOf(
    SlashCommand(
        command = "/plan",
        syntax = "/plan <задача>",
        title = "План проекта",
        description = "Запросить структурированный пошаговый план",
        icon = Icons.Outlined.AutoAwesome,
        template = "Составь пошаговый план для: ",
    ),
    SlashCommand(
        command = "/research",
        syntax = "/research <запрос>",
        title = "Глубокое исследование",
        description = "Многошаговый поиск по интернету и документам",
        icon = Icons.Outlined.Search,
        template = "Тщательно исследуй это и укажи источники: ",
    ),
    SlashCommand(
        command = "/kanban",
        syntax = "/kanban <задача>",
        title = "Разбивка задачи на канбан",
        description = "Разбить сложную цель на задачи на канбан-доске",
        icon = Icons.Outlined.ViewColumn,
        template = "Разбей на задачи для канбан-доски: ",
    ),
    SlashCommand(
        command = "/model ultrabrain",
        syntax = "/model ultrabrain",
        title = "Maestro Ultrabrain",
        description = "Отправить задачу самой мощной облачной модели",
        icon = Icons.Outlined.Psychology,
        template = "[ultrabrain] ",
    ),
    SlashCommand(
        command = "/model quick",
        syntax = "/model quick",
        title = "Maestro быстро / локально",
        description = "Сразу перейти к сверхбыстрому ответу с малой задержкой",
        icon = Icons.Outlined.Bolt,
        template = "[quick] ",
    ),
    SlashCommand(
        command = "/delegate",
        syntax = "/delegate <задача>",
        title = "Поручить субагенту",
        description = "Запустить отдельную фоновую задачу субагента",
        icon = Icons.Outlined.Send,
        // "Delegate task: " made the model run the subtask inside this turn, so nothing reached
        // the Delegate screen. Asking for the background run is what queues it there.
        template = "Поручи это как фоновую задачу (delegate с background=true): ",
    ),
    SlashCommand(
        command = "/memory",
        syntax = "/memory <запрос>",
        title = "Поиск в памяти",
        description = "Вспомнить прошлые факты, привычки и контекст пользователя",
        icon = Icons.Outlined.Psychology,
        template = "Найди в памяти: ",
    ),
    SlashCommand(
        command = "/export",
        syntax = "/export",
        title = "Экспорт переписки",
        description = "Поделиться этим разговором в формате Markdown",
        icon = Icons.Outlined.Description,
        template = "",
        action = SlashAction.EXPORT,
    ),
)

/**
 * Autocomplete popup palette shown when the user types '/' or 'ulw-' in the chat composer.
 */
@Composable
fun SlashCommandPalette(
    currentQuery: String,
    onSelectCommand: (SlashCommand) -> Unit,
    modifier: Modifier = Modifier,
    commands: List<SlashCommand> = HERMES_SLASH_COMMANDS,
) {
    val isVisible = currentQuery.startsWith("/") || currentQuery.startsWith("ulw-")
    val queryToken = when {
        currentQuery.startsWith("/") -> currentQuery.removePrefix("/")
        currentQuery.startsWith("ulw-") -> currentQuery.removePrefix("ulw-")
        else -> ""
    }.trim().lowercase()

    val filteredCommands = if (queryToken.isEmpty()) {
        commands
    } else {
        commands.filter {
            it.command.lowercase().contains(queryToken) ||
                it.title.lowercase().contains(queryToken) ||
                it.description.lowercase().contains(queryToken)
        }
    }

    AnimatedVisibility(
        visible = isVisible && filteredCommands.isNotEmpty(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "КОМАНДЫ HERMES",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp,
                    )
                    Text(
                        text = "${filteredCommands.size} available",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp),
                ) {
                    items(filteredCommands, key = { it.command }) { cmd ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectCommand(cmd) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    imageVector = cmd.icon,
                                    contentDescription = cmd.title,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(8.dp),
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 12.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text = cmd.command,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = GeistMono ?: FontFamily.Monospace,
                                            fontWeight = FontWeight.SemiBold,
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = "• ${cmd.title}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    text = cmd.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
