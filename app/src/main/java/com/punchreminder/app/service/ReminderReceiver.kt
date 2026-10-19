package com.punchreminder.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.punchreminder.app.DiContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val ACTION_SIGN_IN_REMINDER = "com.punchreminder.app.action.SIGN_IN_REMINDER"
private const val ACTION_SIGN_OUT_REMINDER = "com.punchreminder.app.action.SIGN_OUT_REMINDER"

/**
 * Handles reminder alarms and notification action taps.
 *
 * All work is deferred to a short-lived coroutine so the broadcast completes cleanly. The
 * receiver itself is thin: the repository decides what state transition/scheduling applies, and
 * the notification helper renders the notification.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val result = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                handle(action, context)
            } finally {
                result.finish()
            }
        }
    }

    private suspend fun handle(action: String, context: Context) {
        val repo = DiContainer.attendanceRepository
        val notif = DiContainer.notificationHelper
        val launcher = DiContainer.greytHrLauncher

        val openIntent = NotificationHelper.openAppIntent(DiContainer.context)

        when (action) {
            ACTION_SIGN_IN_REMINDER -> {
                val shouldNotify = repo.handleSignInAlarmFired()
                if (shouldNotify) {
                    val confirm = NotificationHelper.broadcastIntent(DiContainer.context, NotificationHelper.CONFIRM_SIGN_IN)
                    val snooze = NotificationHelper.broadcastIntent(DiContainer.context, NotificationHelper.SNOOZE_SIGN_IN)
                    val settings = DiContainer.settingsRepository.settings.first()
                    notif.showSignIn(
                        openIntent = openIntent,
                        confirmIntent = confirm,
                        snoozeIntent = snooze,
                        pauseIntent = NotificationHelper.pauseTodayIntent(DiContainer.context),
                        snoozeMinutes = settings.snoozeMinutes
                    )
                }
            }
            ACTION_SIGN_OUT_REMINDER -> {
                val shouldNotify = repo.handleSignOutAlarmFired()
                if (shouldNotify) {
                    val confirm = NotificationHelper.broadcastIntent(DiContainer.context, NotificationHelper.CONFIRM_SIGN_OUT)
                    val snooze = NotificationHelper.broadcastIntent(DiContainer.context, NotificationHelper.SNOOZE_SIGN_OUT)
                    val settings = DiContainer.settingsRepository.settings.first()
                    val durationText = buildDurationText(settings.sessionDurationHours, settings.sessionDurationMinutes)
                    notif.showSignOut(
                        openIntent = openIntent,
                        confirmIntent = confirm,
                        snoozeIntent = snooze,
                        sessionDurationText = durationText,
                        snoozeMinutes = settings.snoozeMinutes
                    )
                }
            }
            NotificationHelper.CONFIRM_SIGN_IN -> {
                repo.confirmSignIn()
                notif.cancelSignIn()
            }
            NotificationHelper.CONFIRM_SIGN_OUT -> {
                repo.confirmSignOut()
                notif.cancelAll()
            }
            NotificationHelper.SNOOZE_SIGN_IN -> {
                val settings = DiContainer.settingsRepository.settings.first()
                repo.snoozeSignIn(settings.snoozeMinutes)
                notif.cancelSignIn()
            }
            NotificationHelper.SNOOZE_SIGN_OUT -> {
                val settings = DiContainer.settingsRepository.settings.first()
                repo.snoozeSignOut(settings.snoozeMinutes)
                notif.cancelSignOut()
            }
            NotificationHelper.OPEN_GREYTHR -> {
                launcher.openGreytHR(context)
            }
        }
    }

    /** Builds a user-readable duration string like "9-hour" or "8-hour 30-minute". */
    private fun buildDurationText(hours: Int, minutes: Int): String = when {
        hours > 0 && minutes > 0 -> "${hours}-hour ${minutes}-minute"
        hours > 0 -> "${hours}-hour"
        minutes > 0 -> "${minutes}-minute"
        else -> ""
    }
}
