package com.hermes.agent.ui

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/**
 * Shown on the launch after a crash. The report stays on the phone unless the user shares it, and
 * the whole text is shown so they can see exactly what would be sent.
 */
@Composable
fun CrashReportDialog(
    report: String,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
    /** Files it (redacted) to the private repair repo; null when no token is set. */
    onSendForRepair: (() -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("В прошлый раз Hermes аварийно завершился") },
        text = {
            Column {
                Text(
                    "Этот отчёт сохранён на вашем телефоне. В нём версия приложения, модель устройства " +
                        "и трассировка ошибки, но нет чатов, настроек и ключей. Ничего не отправляется, пока вы сами им не поделитесь.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    report,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .heightIn(max = 220.dp)
                        .verticalScroll(rememberScrollState()),
                )
            }
        },
        confirmButton = {
            androidx.compose.foundation.layout.Row {
                onSendForRepair?.let { TextButton(onClick = it) { Text("Отправить на исправление") } }
                TextButton(onClick = onShare) { Text("Поделиться") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Удалить") } },
    )
}
