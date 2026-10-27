package com.hermes.agent.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hermes.agent.data.hotfix.OtaDecision
import com.hermes.agent.data.update.OtaUpdateChecker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class OtaUpdateWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted params: WorkerParameters,
    private val checker: OtaUpdateChecker,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        // JX-01: HermesAppStartup cancels this unique work when OTA is disabled, but a run
        // already queued before the cancel could still execute once — refuse here too.
        if (!com.hermes.agent.BuildConfig.OTA_ENABLED) {
            Timber.tag("OtaWorker").i("OTA disabled in this build — skipping check")
            return Result.success()
        }
        Timber.tag("OtaWorker").d("checking for update")
        val decision = runCatching { checker.checkOffer() }
            .onFailure { Timber.tag("OtaWorker").w(it, "check failed") }
            .getOrNull() ?: return Result.success()

        // Only ever a notification: downloading and applying stay behind the user's tap.
        when (decision) {
            is OtaDecision.Patch -> {
                val m = decision.offer.manifest
                Timber.tag("OtaWorker").i("fix #%d available for %s", m.patchVersion, m.baseTinkerId)
                postNotification(
                    title = "Доступно исправление для Hermes",
                    text = "Откройте Hermes, чтобы применить его, — без переустановки, только перезапуск.",
                    bigText = m.notes.ifBlank { "Откройте Hermes, чтобы применить его, — без переустановки, только перезапуск." },
                    releaseUrl = "",
                )
            }
            is OtaDecision.FullApk -> {
                val update = decision.update
                Timber.tag("OtaWorker").i("update available: ${update.version}")
                postNotification(
                    title = "Доступен Hermes ${update.version}",
                    text = "Нажмите, чтобы открыть Hermes и установить обновление.",
                    bigText = update.releaseNotes.ifBlank { "Нажмите, чтобы открыть Hermes и установить обновление." },
                    releaseUrl = update.releaseUrl,
                )
            }
            OtaDecision.None -> Unit
        }
        return Result.success()
    }

    private fun postNotification(title: String, text: String, bigText: String, releaseUrl: String) {
        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Ensure channel exists.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Обновления приложения",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "Уведомления о доступных обновлениях Hermes" }
            nm.createNotificationChannel(channel)
        }

        // Open the app so the user can download & install in-app (Settings →
        // Updates), rather than sending them to the browser. Falls back to the
        // release page only if the launch intent can't be resolved.
        val launchIntent = appContext.packageManager
            .getLaunchIntentForPackage(appContext.packageName)
            ?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                // Deep-link straight to Settings (Updates section) — audit L5.
                putExtra(EXTRA_OPEN_UPDATES, true)
            }
            ?: Intent(Intent.ACTION_VIEW, Uri.parse(releaseUrl.ifBlank { "https://github.com/${com.hermes.agent.BuildConfig.UPDATE_REPO}/releases" }))

        val openIntent = PendingIntent.getActivity(
            appContext,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .build()

        nm.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val UNIQUE_NAME = "hermes.ota_update_check"

        /** Launch-intent extra: open Settings (Updates section) on start. */
        const val EXTRA_OPEN_UPDATES = "com.hermes.agent.OPEN_UPDATES"

        private const val CHANNEL_ID = "hermes_updates"
        private const val NOTIFICATION_ID = 9001
    }
}
