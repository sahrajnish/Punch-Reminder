package com.punchreminder.app.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.punchreminder.app.DiContainer
import com.punchreminder.app.domain.SchedulingClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * A once-a-day safety net that closes the previous day and rebuilds the reminder schedule.
 *
 * The sign-in/sign-out reminder chain only lives for the current day. Running just after midnight
 * lets the repository auto-close any session that was left open (a forgotten sign-out) and arm
 * today's reminders even on a day with no reminder chain. It also re-arms itself for the next day.
 */
class DailyRefreshReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val autoClosed = DiContainer.attendanceRepository.rescheduleAll()
                if (autoClosed > 0) {
                    // A reminder shown for yesterday is obsolete now that the day has rolled over.
                    DiContainer.notificationHelper.cancelAll()
                }
            } finally {
                schedule(context)
                result.finish()
            }
        }
    }

    companion object {
        private const val ACTION = "com.punchreminder.app.action.DAILY_REFRESH"
        private const val REQUEST_CODE = 2001
        private const val REFRESH_HOUR = 0
        private const val REFRESH_MINUTE = 1
        private const val FALLBACK_WINDOW_MILLIS = 15L * 60 * 1000

        /** Arm the next daily refresh. Safe to call repeatedly; it replaces any pending refresh. */
        fun schedule(context: Context) {
            val wrapper = DiContainer.alarmManagerWrapper
            val pi = pendingIntent(context)
            wrapper.cancel(pi)
            val at = nextRefreshMillis(DiContainer.clock)
            if (wrapper.canScheduleExactAlarms()) {
                wrapper.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                wrapper.setWindow(AlarmManager.RTC_WAKEUP, at, FALLBACK_WINDOW_MILLIS, pi)
            }
        }

        private fun pendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, DailyRefreshReceiver::class.java).setAction(ACTION)
            return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        /** The next 00:01 local time strictly after now. */
        private fun nextRefreshMillis(clock: SchedulingClock): Long {
            val zone = clock.zoneId
            val today = clock.today()
            var candidate = today.atTime(REFRESH_HOUR, REFRESH_MINUTE).atZone(zone).toInstant()
            if (!candidate.isAfter(clock.now)) {
                candidate = today.plusDays(1)
                    .atTime(REFRESH_HOUR, REFRESH_MINUTE)
                    .atZone(zone)
                    .toInstant()
            }
            return candidate.toEpochMilli()
        }
    }
}
