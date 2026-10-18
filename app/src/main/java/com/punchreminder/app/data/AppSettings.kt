package com.punchreminder.app.data

import com.punchreminder.app.domain.ReminderSettings
import com.punchreminder.app.domain.Workdays
import java.util.TimeZone

/**
 * User-configured reminder settings. Defaults match the product specification:
 * workdays Monday-Friday, sign-in 10:30 AM, 9-hour session, 10-minute repetition.
 */
data class AppSettings(
    val remindersEnabled: Boolean = true,
    val workdays: Int = Workdays.DEFAULT_WORKDAYS,
    val signInHour: Int = 10,
    val signInMinute: Int = 30,
    val sessionDurationHours: Int = 9,
    val sessionDurationMinutes: Int = 0,
    val repeatIntervalMinutes: Int = 10,
    val snoozeMinutes: Int = 10,
    val useCustomSound: Boolean = false,
    val soundUri: String? = null,
    val vibrateEnabled: Boolean = true,
    val timeZoneId: String = TimeZone.getDefault().id,
    /** App-wide appearance: true = dark theme, false = light theme. */
    val darkTheme: Boolean = true
) {
    fun toReminderSettings(): ReminderSettings =
        ReminderSettings(
            remindersEnabled = remindersEnabled,
            workdays = workdays,
            signInHour = signInHour,
            signInMinute = signInMinute,
            sessionDurationHours = sessionDurationHours,
            sessionDurationMinutes = sessionDurationMinutes,
            repeatIntervalMinutes = repeatIntervalMinutes,
            snoozeMinutes = snoozeMinutes,
            zoneId = java.time.ZoneId.of(timeZoneId)
        )
}
