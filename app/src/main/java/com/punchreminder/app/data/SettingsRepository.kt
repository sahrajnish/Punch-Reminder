package com.punchreminder.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class SettingsRepository private constructor(private val prefs: SharedPreferences) {

    private val KEY_REMINDERS_ENABLED = "reminders_enabled"
    private val KEY_WORKDAYS = "workdays"
    private val KEY_SIGN_IN_HOUR = "sign_in_hour"
    private val KEY_SIGN_IN_MINUTE = "sign_in_minute"
    private val KEY_SESSION_DURATION_HOURS = "session_duration_hours"
    private val KEY_SESSION_DURATION_MINUTES = "session_duration_minutes"
    private val KEY_REPEAT_INTERVAL_MINUTES = "repeat_interval_minutes"
    private val KEY_SNOOZE_MINUTES = "snooze_minutes"
    private val KEY_USE_CUSTOM_SOUND = "use_custom_sound"
    private val KEY_SOUND_URI = "notification_sound_uri"
    private val KEY_VIBRATE = "vibrate_enabled"
    private val KEY_TIME_ZONE_ID = "time_zone_id"
    private val KEY_DARK_THEME = "dark_theme"

    val defaultSettings = AppSettings()

    /**
     * Hot settings stream. It always reflects the values currently persisted in
     * [SharedPreferences], so screens can observe updates instead of reading a stale snapshot.
     */
    private val _settings = MutableStateFlow(readSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun readSettings(): AppSettings = AppSettings(
        remindersEnabled = prefs.getBoolean(KEY_REMINDERS_ENABLED, defaultSettings.remindersEnabled),
        workdays = prefs.getInt(KEY_WORKDAYS, defaultSettings.workdays),
        signInHour = prefs.getInt(KEY_SIGN_IN_HOUR, defaultSettings.signInHour),
        signInMinute = prefs.getInt(KEY_SIGN_IN_MINUTE, defaultSettings.signInMinute),
        sessionDurationHours = prefs.getInt(KEY_SESSION_DURATION_HOURS, defaultSettings.sessionDurationHours),
        sessionDurationMinutes = prefs.getInt(KEY_SESSION_DURATION_MINUTES, defaultSettings.sessionDurationMinutes),
        repeatIntervalMinutes = prefs.getInt(KEY_REPEAT_INTERVAL_MINUTES, defaultSettings.repeatIntervalMinutes),
        snoozeMinutes = prefs.getInt(KEY_SNOOZE_MINUTES, defaultSettings.snoozeMinutes),
        useCustomSound = prefs.getBoolean(KEY_USE_CUSTOM_SOUND, defaultSettings.useCustomSound),
        soundUri = prefs.getString(KEY_SOUND_URI, null)?.takeIf { it.isNotEmpty() },
        vibrateEnabled = prefs.getBoolean(KEY_VIBRATE, defaultSettings.vibrateEnabled),
        timeZoneId = prefs.getString(KEY_TIME_ZONE_ID, defaultSettings.timeZoneId) ?: defaultSettings.timeZoneId,
        darkTheme = prefs.getBoolean(KEY_DARK_THEME, defaultSettings.darkTheme)
    )

    suspend fun saveSettings(settings: AppSettings) {
        withContext(Dispatchers.IO) {
            prefs.edit().apply {
                putBoolean(KEY_REMINDERS_ENABLED, settings.remindersEnabled)
                putInt(KEY_WORKDAYS, settings.workdays)
                putInt(KEY_SIGN_IN_HOUR, settings.signInHour)
                putInt(KEY_SIGN_IN_MINUTE, settings.signInMinute)
                putInt(KEY_SESSION_DURATION_HOURS, settings.sessionDurationHours)
                putInt(KEY_SESSION_DURATION_MINUTES, settings.sessionDurationMinutes)
                putInt(KEY_REPEAT_INTERVAL_MINUTES, settings.repeatIntervalMinutes)
                putInt(KEY_SNOOZE_MINUTES, settings.snoozeMinutes)
                putBoolean(KEY_USE_CUSTOM_SOUND, settings.useCustomSound)
                putString(KEY_SOUND_URI, settings.soundUri.orEmpty())
                putBoolean(KEY_VIBRATE, settings.vibrateEnabled)
                putString(KEY_TIME_ZONE_ID, settings.timeZoneId)
                putBoolean(KEY_DARK_THEME, settings.darkTheme)
                apply()
            }
        }
        // Publish the newly persisted values so all observers stay in sync.
        _settings.value = settings
    }

    suspend fun restoreDefaults() {
        withContext(Dispatchers.IO) {
            prefs.edit().apply {
                putBoolean(KEY_REMINDERS_ENABLED, defaultSettings.remindersEnabled)
                putInt(KEY_WORKDAYS, defaultSettings.workdays)
                putInt(KEY_SIGN_IN_HOUR, defaultSettings.signInHour)
                putInt(KEY_SIGN_IN_MINUTE, defaultSettings.signInMinute)
                putInt(KEY_SESSION_DURATION_HOURS, defaultSettings.sessionDurationHours)
                putInt(KEY_SESSION_DURATION_MINUTES, defaultSettings.sessionDurationMinutes)
                putInt(KEY_REPEAT_INTERVAL_MINUTES, defaultSettings.repeatIntervalMinutes)
                putInt(KEY_SNOOZE_MINUTES, defaultSettings.snoozeMinutes)
                putBoolean(KEY_USE_CUSTOM_SOUND, defaultSettings.useCustomSound)
                putString(KEY_SOUND_URI, "")
                putBoolean(KEY_VIBRATE, defaultSettings.vibrateEnabled)
                putString(KEY_TIME_ZONE_ID, defaultSettings.timeZoneId)
                putBoolean(KEY_DARK_THEME, defaultSettings.darkTheme)
                apply()
            }
        }
        // Publish the restored defaults so all observers stay in sync.
        _settings.value = defaultSettings
    }

    companion object {
        fun initialize(context: Context): SettingsRepository {
            val prefs: SharedPreferences = context.getSharedPreferences("punch_reminder_settings", Context.MODE_PRIVATE)
            return SettingsRepository(prefs)
        }
    }
}
