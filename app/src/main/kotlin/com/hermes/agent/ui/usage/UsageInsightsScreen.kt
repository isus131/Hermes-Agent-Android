package com.hermes.agent.ui.usage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermes.agent.domain.usage.UsageSummary
import com.hermes.agent.domain.usage.UsageTimeWindow
import java.text.NumberFormat
import java.util.Locale

private val WINDOWS = listOf(
    UsageTimeWindow.TODAY to "Сегодня",
    UsageTimeWindow.LAST_7_DAYS to "7 дней",
    UsageTimeWindow.LAST_30_DAYS to "30 дней",
    UsageTimeWindow.ALL_TIME to "Всё время",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UsageInsightsScreen(
    onBack: () -> Unit,
    viewModel: UsageInsightsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Использование и расходы") },
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
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                WINDOWS.forEach { (window, label) ->
                    FilterChip(
                        selected = state.window == window,
                        onClick = { viewModel.load(window) },
                        label = { Text(label) },
                    )
                }
            }

            when {
                state.isLoading -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) { CircularProgressIndicator() }

                state.error != null -> Text(
                    text = state.error!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )

                state.summary != null -> SummaryBody(state.summary!!)
            }
        }
    }
}

@Composable
private fun SummaryBody(summary: UsageSummary) {
    val nf = NumberFormat.getIntegerInstance(Locale.US)
    val cf = NumberFormat.getCurrencyInstance(Locale.US).apply { maximumFractionDigits = 4 }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StatRow("Разговоры", nf.format(summary.totalSessions))
            StatRow("Сообщения", nf.format(summary.totalMessages))
            StatRow("Токены", nf.format(summary.totalTokens))
            StatRow(
                "  запрос / ответ",
                "${nf.format(summary.promptTokens)} / ${nf.format(summary.completionTokens)}",
            )
            HorizontalDivider()
            StatRow("Примерная стоимость", cf.format(summary.estimatedCostUsd), emphasise = true)
            Text(
                "Оценка по локальному подсчёту токенов и встроенной таблице цен, а не счёт. " +
                    "Ходы на устройстве ничего не стоят и считаются отдельно ниже.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (summary.modelBreakdowns.isNotEmpty()) {
        Text("По моделям", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                summary.modelBreakdowns.forEach { m ->
                    Column {
                        Text(m.model, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "запросов: ${nf.format(m.totalRequests)} · токенов: ${nf.format(m.totalTokens)} · " +
                                cf.format(m.estimatedCostUsd),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    if (summary.toolBreakdowns.isNotEmpty()) {
        Text("По инструментам", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                summary.toolBreakdowns.forEach { t ->
                    val failureNote = if (t.failureCount > 0) " · ошибок: ${t.failureCount}" else ""
                    Column {
                        Text(t.toolName, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "${nf.format(t.totalInvocations)} calls$failureNote",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (t.failureCount > 0) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
        }
    }

    if (summary.totalMessages == 0) {
        Text(
            "За этот период активности нет.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun StatRow(label: String, value: String, emphasise: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = if (emphasise) {
                MaterialTheme.typography.titleMedium
            } else {
                MaterialTheme.typography.bodyLarge
            },
            color = if (emphasise) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}
