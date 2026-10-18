package com.punchreminder.app.domain

import android.content.Context
import android.app.PendingIntent
import java.time.Instant

/**
 * Schedules, reschedules, and cancels attendance reminders using the system alarm manager.
 *
 * Exactly one pending alarm is kept per [ReminderKind]; scheduling a kind cancels any previously
 * pending alarm of that kind before inserting the new one, which prevents duplicate notifications.
 */
interface ReminderScheduler {
    val context: Context
    val clock: SchedulingClock

    /** Schedule (or reschedule) a single reminder kind at the given instant. */
    fun schedule(kind: ReminderKind, at: Instant)

    /** Cancel all pending attendance alarms and clear pending-reminder state. */
    fun cancelAll()

    /** Cancel a single reminder kind. */
    fun cancel(kind: ReminderKind)

    /** Request code used to distinguish pending intents per reminder kind. */
    fun requestCode(kind: ReminderKind): Int

    fun createPendingIntent(context: Context, kind: ReminderKind, action: String): PendingIntent
}
