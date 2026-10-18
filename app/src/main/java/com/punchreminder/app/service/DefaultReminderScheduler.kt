package com.punchreminder.app.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.punchreminder.app.domain.AlarmManagerWrapper
import com.punchreminder.app.domain.ReminderKind
import com.punchreminder.app.domain.ReminderScheduler
import com.punchreminder.app.domain.SchedulingClock
import java.time.Instant

private const val ACTION_SIGN_IN_REMINDER = "com.punchreminder.app.action.SIGN_IN_REMINDER"
private const val ACTION_SIGN_OUT_REMINDER = "com.punchreminder.app.action.SIGN_OUT_REMINDER"
private const val EXTRA_KIND = "com.punchreminder.app.extra.kind"
private const val SHOW_INTENT_REQUEST_CODE = 9001

/**
 * Concrete alarm scheduling backed by [AlarmManager].
 *
 * Exactly one pending alarm is kept per [ReminderKind]. When exact alarms are unavailable the
 * scheduler falls back to an inexact window and the app reports the limitation to the user.
 */
class DefaultReminderScheduler(
    override val context: Context,
    override val clock: SchedulingClock,
    private val alarmManagerWrapper: AlarmManagerWrapper
) : ReminderScheduler {

    override fun schedule(kind: ReminderKind, at: Instant) {
        val pi = alarmPendingIntent(kind)
        alarmManagerWrapper.cancel(pi)

        val triggerAtMillis = at.toEpochMilli()
        val nowMillis = System.currentTimeMillis()
        // Always aim at least a moment into the future; a due-now reminder fires almost at once.
        val effectiveAt = maxOf(triggerAtMillis, nowMillis + 1_000)

        // A user-visible alarm clock is exact and is not deferred by Doze, battery saver, or
        // app-standby batching, so reminders land at the configured time. setExactAndAllowWhileIdle
        // is still used for the non-time-critical daily refresh.
        alarmManagerWrapper.setAlarmClock(effectiveAt, showIntent(), pi)
    }

    override fun cancel(kind: ReminderKind) {
        alarmManagerWrapper.cancel(alarmPendingIntent(kind))
    }

    override fun cancelAll() {
        cancel(ReminderKind.SIGN_IN)
        cancel(ReminderKind.SIGN_OUT)
    }

    override fun createPendingIntent(
        context: Context,
        kind: ReminderKind,
        action: String
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            setAction(action)
            putExtra(EXTRA_KIND, kind.name)
        }
        return PendingIntent.getBroadcast(
            context, requestCodeForNotification(action), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun alarmPendingIntent(kind: ReminderKind): PendingIntent {
        val action = when (kind) {
            ReminderKind.SIGN_IN -> ACTION_SIGN_IN_REMINDER
            ReminderKind.SIGN_OUT -> ACTION_SIGN_OUT_REMINDER
        }
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            setAction(action)
            putExtra(EXTRA_KIND, kind.name)
        }
        return PendingIntent.getBroadcast(
            context, requestCodeForAlarm(kind), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    override fun requestCode(kind: ReminderKind): Int = requestCodeForAlarm(kind)

    /** Intent opened when the user taps the system alarm icon (shows this app). */
    private fun showIntent(): PendingIntent {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(Intent.ACTION_MAIN).setPackage(context.packageName)
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(
            context,
            SHOW_INTENT_REQUEST_CODE,
            launch,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun requestCodeForAlarm(kind: ReminderKind): Int = when (kind) {
        ReminderKind.SIGN_IN -> 1
        ReminderKind.SIGN_OUT -> 2
    }

    private fun requestCodeForNotification(action: String): Int = when (action) {
        NotificationHelper.CONFIRM_SIGN_IN -> 1001
        NotificationHelper.CONFIRM_SIGN_OUT -> 1002
        NotificationHelper.SNOOZE_SIGN_IN -> 1003
        NotificationHelper.SNOOZE_SIGN_OUT -> 1004
        NotificationHelper.OPEN_GREYTHR -> 1005
        else -> 0
    }
}
