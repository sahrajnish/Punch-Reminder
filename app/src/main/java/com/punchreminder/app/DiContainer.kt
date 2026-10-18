package com.punchreminder.app

import android.content.Context
import android.net.Uri
import com.punchreminder.app.domain.AlarmManagerWrapper
import com.punchreminder.app.domain.DefaultGreytHrLauncher
import com.punchreminder.app.domain.GreytHrLauncher
import com.punchreminder.app.domain.ReminderScheduler
import com.punchreminder.app.domain.SchedulingClock
import com.punchreminder.app.domain.SystemAlarmManagerWrapper
import com.punchreminder.app.domain.SystemSchedulingClock
import com.punchreminder.app.data.AttendanceRepository
import com.punchreminder.app.data.AppSettings
import com.punchreminder.app.data.SettingsRepository
import com.punchreminder.app.service.DefaultReminderScheduler
import com.punchreminder.app.service.DailyRefreshReceiver
import com.punchreminder.app.service.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Lightweight dependency container.
 *
 * The application wires real implementations here. Tests can replace individual fields with fakes
 * to exercise the scheduling engine and state machine deterministically.
 */
object DiContainer {
    lateinit var context: Context
        private set

    val clock: SchedulingClock = SystemSchedulingClock
    lateinit var alarmManagerWrapper: AlarmManagerWrapper
    lateinit var notificationHelper: NotificationHelper
    lateinit var settingsRepository: SettingsRepository
    lateinit var attendanceRepository: AttendanceRepository
    lateinit var reminderScheduler: ReminderScheduler
    lateinit var greytHrLauncher: GreytHrLauncher

    private var isInitialized = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun init(appContext: Context) {
        if (isInitialized) return
        isInitialized = true

        context = appContext.applicationContext
        alarmManagerWrapper = SystemAlarmManagerWrapper(context)
        notificationHelper = NotificationHelper(context)
        settingsRepository = SettingsRepository.initialize(context)
        reminderScheduler = DefaultReminderScheduler(context, clock, alarmManagerWrapper)
        attendanceRepository = AttendanceRepository.create(context, reminderScheduler, settingsRepository)
        greytHrLauncher = DefaultGreytHrLauncher()

        // Apply the saved sound and vibration preferences immediately, then keep them in sync.
        val initialSettings = settingsRepository.settings.value
        notificationHelper.setReminderSound(reminderSound(initialSettings))
        notificationHelper.setVibrationEnabled(initialSettings.vibrateEnabled)
        scope.launch {
            settingsRepository.settings.collect { settings ->
                notificationHelper.setReminderSound(reminderSound(settings))
                notificationHelper.setVibrationEnabled(settings.vibrateEnabled)
            }
        }

        // Arm the daily safety net so reminders are rebuilt every day.
        DailyRefreshReceiver.schedule(context)
    }

    /** The custom tone when enabled, otherwise null (system default). */
    private fun reminderSound(settings: AppSettings): Uri? =
        if (settings.useCustomSound) settings.soundUri?.let(Uri::parse) else null
}
