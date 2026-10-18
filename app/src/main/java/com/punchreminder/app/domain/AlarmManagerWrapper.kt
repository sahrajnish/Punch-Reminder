package com.punchreminder.app.domain

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context

/**
 * Thin abstraction over [AlarmManager] so the scheduling engine can be tested without the Android
 * framework and so exact-alarm permission handling is centralized.
 */
interface AlarmManagerWrapper {
    fun setExactAndAllowWhileIdle(
        type: Int,
        triggerAtMillis: Long,
        operation: PendingIntent
    )
    fun setWindow(
        type: Int,
        windowStartMillis: Long,
        windowLengthMillis: Long,
        operation: PendingIntent
    )

    /**
     * Schedules a user-visible alarm clock. Unlike the while-idle family this is exact and is not
     * deferred by Doze, battery saver, or app-standby policy, so it is used for the reminders.
     */
    fun setAlarmClock(
        triggerAtMillis: Long,
        showIntent: PendingIntent,
        operation: PendingIntent
    )
    fun cancel(operation: PendingIntent)
    fun canScheduleExactAlarms(): Boolean
}

class SystemAlarmManagerWrapper(private val context: Context) : AlarmManagerWrapper {
    private val alarmManager: AlarmManager by lazy {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    override fun setExactAndAllowWhileIdle(
        type: Int,
        triggerAtMillis: Long,
        operation: PendingIntent
    ) = alarmManager.setExactAndAllowWhileIdle(type, triggerAtMillis, operation)

    override fun setWindow(
        type: Int,
        windowStartMillis: Long,
        windowLengthMillis: Long,
        operation: PendingIntent
    ) = alarmManager.setWindow(type, windowStartMillis, windowLengthMillis, operation)

    override fun setAlarmClock(
        triggerAtMillis: Long,
        showIntent: PendingIntent,
        operation: PendingIntent
    ) = alarmManager.setAlarmClock(
        AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent),
        operation
    )

    override fun cancel(operation: PendingIntent) = alarmManager.cancel(operation)

    override fun canScheduleExactAlarms(): Boolean = alarmManager.canScheduleExactAlarms()
}
