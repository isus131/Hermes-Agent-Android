package com.hermes.agent.data.evolution

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.hermes.agent.data.plugin.evolution.EvolutionEvents
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Posts "the agent improved itself" notifications so automatic skill
 * refinements never happen invisibly. Tapping opens the app — the change is
 * reviewable under Settings → Features → Refine skills / Skills & Tools.
 *
 * Also the host side of feature evolution's [EvolutionEvents]: new proposals,
 * a module ready to review, and an override that was reverted automatically.
 */
@Singleton
class EvolutionNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) : EvolutionEvents {

    private fun manager(): NotificationManager {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Развитие навыков",
                    NotificationManager.IMPORTANCE_LOW, // informative, not urgent
                ).apply { description = "Уведомляет, когда Hermes улучшает свои навыки или предлагает доработки на основе вашего использования" },
            )
        }
        return nm
    }

    private fun contentIntent(): PendingIntent? {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        return launchIntent?.let {
            PendingIntent.getActivity(
                context, 0, it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }

    private fun post(id: Int, title: String, text: String, detail: String) {
        val nm = manager()
        val intent = contentIntent()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detail))
            .apply { intent?.let { setContentIntent(it) } }
            .setAutoCancel(true)
            .build()
        runCatching { nm.notify(id, notification) }
    }

    override fun proposalsReady(count: Int) {
        if (count <= 0) return
        post(
            PROPOSALS_ID,
            if (count == 1) "Предложено 1 улучшение" else "Предложено улучшений: $count",
            "Найдено по тому, как вы пользуетесь Hermes. Нажмите, чтобы посмотреть.",
            "Hermes проанализировал ваше недавнее использование и предложил улучшений: $count. Ничего не изменится, пока вы " +
                "не одобрите это в разделе «Настройки → Развитие функций».",
        )
    }

    override fun moduleReady(title: String) {
        post(
            READY_ID,
            "Готово к проверке: $title",
            "Создано и проверено вашими ботами. Нажмите, чтобы проверить и установить.",
            "Боты-сборщик и проверяющий закончили «$title». Проверьте код, разрешения и результаты тестов в разделе " +
                "«Настройки → Развитие функций» перед установкой.",
        )
    }

    override fun overrideReverted(toolName: String, moduleId: String, reason: String) {
        post(
            REVERTED_ID,
            "Исправление для $toolName отменено",
            "Оно постоянно давало сбои, поэтому Hermes вернулся к встроенному инструменту.",
            "Модуль развития $moduleId заменил $toolName, но постоянно давал сбои ($reason). Он отключён, " +
                "и встроенный инструмент возвращён. См. «Настройки → Развитие функций».",
        )
    }

    fun notifySkillsImproved(skillNames: List<String>) {
        if (skillNames.isEmpty()) return
        val nm = manager()
        val contentIntent = contentIntent()

        val title = if (skillNames.size == 1) {
            "Навык улучшен: ${skillNames.first()}"
        } else {
            "Улучшено навыков: ${skillNames.size}"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentTitle(title)
            .setContentText("Доработано по тому, как вы ими пользовались. Нажмите, чтобы посмотреть.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Hermes доработал ${skillNames.joinToString(", ")} на основе вашего недавнего " +
                        "использования. Посмотреть: «Настройки → Навыки и инструменты».",
                ),
            )
            .apply { contentIntent?.let { setContentIntent(it) } }
            .setAutoCancel(true)
            .build()

        nm.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CHANNEL_ID = "hermes_evolution"
        private const val NOTIFICATION_ID = 9002
        private const val PROPOSALS_ID = 9003
        private const val READY_ID = 9004
        private const val REVERTED_ID = 9005
    }
}
