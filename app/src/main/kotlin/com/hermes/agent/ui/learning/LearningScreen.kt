package com.hermes.agent.ui.learning
import com.hermes.agent.domain.settings.*

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermes.agent.ui.theme.hermesFieldColors

/**
 * Settings → Learning. A glance-able view of the self-improvement loop, with
 * the ability to correct it: edit/delete learned facts and force a user-model
 * rebuild.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningScreen(
    onBack: () -> Unit,
    viewModel: LearningViewModel = hiltViewModel(),
) {
    val s by viewModel.uiState.collectAsStateWithLifecycle()
    val rebuilding by viewModel.rebuilding.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<FactItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Обучение") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionCard(title = "Ход обучения") {
                StatusLine(done = true, text = "Записано разговоров: ${s.conversationCount}")
                Spacer(Modifier.height(8.dp))
                val toGo = s.conversationsUntilRebuild
                Text(
                    if (toGo == 0) "Модель пользователя будет перестроена после следующего разговора."
                    else "До следующего перестроения модели пользователя осталось разговоров: $toGo${if (toGo == 1) "" else ""}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                val progress = ((s.rebuildEvery - toGo).toFloat() / s.rebuildEvery).coerceIn(0f, 1f)
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            }

            SectionCard(title = "Модель пользователя") {
                StatusLine(
                    done = s.userModel != null,
                    text = if (s.userModel != null) "Профиль построен" else "Ещё не построен",
                )
                if (s.userModel != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        s.userModel!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                } else {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Строится автоматически, когда вы расскажете о себе несколько фактов и проведёте разговоров: ${s.rebuildEvery}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(12.dp))
                FilledTonalButton(
                    onClick = viewModel::rebuildUserModelNow,
                    enabled = !rebuilding && s.factCount >= 3,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (rebuilding) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.size(8.dp))
                        Text("Перестраиваю…")
                    } else {
                        Text(if (s.userModel != null) "Перестроить сейчас" else "Построить сейчас")
                    }
                }
                if (s.factCount < 3) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Для построения нужно не меньше 3 выученных фактов.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SectionCard(title = "Выученные факты (${s.factCount})") {
                if (s.recentFacts.isEmpty()) {
                    Text(
                        "Пока нет выученных фактов. Расскажите Hermes о себе в чате.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        "Нажмите на факт, чтобы изменить его, или на значок корзины, чтобы удалить неверный.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    s.recentFacts.forEach { fact ->
                        FactRow(
                            fact = fact,
                            onEdit = { editing = fact },
                            onDelete = { viewModel.deleteFact(fact.id) },
                        )
                    }
                }
            }

            SectionCard(title = "Навыки, созданные Hermes (${s.autoSkills.size})") {
                if (s.autoSkills.isEmpty()) {
                    Text(
                        "Пока нет. Выполните задачу, в которой участвуют два инструмента или больше, и Hermes напишет навык для повторного использования.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    s.autoSkills.forEach { name ->
                        Text(
                            "• $name",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = 1.dp),
                        )
                    }
                }
            }
        }
    }

    editing?.let { fact ->
        EditFactDialog(
            initial = fact.content,
            onDismiss = { editing = null },
            onSave = { viewModel.editFact(fact.id, it); editing = null },
            onDelete = { viewModel.deleteFact(fact.id); editing = null },
        )
    }
}

@Composable
private fun FactRow(fact: FactItem, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            fact.content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onEdit)
                .padding(vertical = 6.dp),
        )
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = "Удалить факт",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun EditFactDialog(
    initial: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Изменить факт") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                colors = hermesFieldColors(),
            )
        },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text("Сохранить") } },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) { Text("Отмена") }
            }
        },
    )
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun StatusLine(done: Boolean, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (done) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (done) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
