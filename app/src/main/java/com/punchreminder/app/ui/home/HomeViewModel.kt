package com.punchreminder.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.punchreminder.app.DiContainer
import com.punchreminder.app.domain.AttendanceState
import com.punchreminder.app.domain.AttendanceSession
import com.punchreminder.app.domain.ReminderKind
import com.punchreminder.app.domain.Workdays
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.punchreminder.app.data.SettingsRepository
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class HomeUiState(
    val currentDate: String = "",
    val currentTime: String = "",
    val statusTitle: String = "",
    val statusSubtitle: String = "",
    val actualSignIn: String? = null,
    val actualSignOut: String? = null,
    val nextReminderText: String = "",
    val signInTimeText: String = "",
    val pauseReason: String? = null,
    val state: AttendanceState = AttendanceState.NOT_SIGNED_IN,
    /** Show "I've signed in" on Home: not signed in yet and the configured time has passed. */
    val canSignIn: Boolean = false,
    /** Show "I've signed out" on Home while the session is open. */
    val canSignOut: Boolean = false,
    val remindersEnabled: Boolean = true,
    val isWorkday: Boolean = true,
    val isSkipped: Boolean = false,
)

class HomeViewModel : ViewModel() {

    private val repo = DiContainer.attendanceRepository
    private val settingsRepository: SettingsRepository = DiContainer.settingsRepository
    private val clock = DiContainer.clock

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    private val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())

    /** General times (sign-in/sign-out/next reminder) — no seconds. */
    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

    /** The live clock on the card — includes seconds. */
    private val clockFormatter = DateTimeFormatter.ofPattern("h:mm:ss a", Locale.getDefault())

    /** Time zone of the latest emitted settings, used by the ticker to format the clock. */
    private var latestZone: ZoneId = clock.zoneId

    /** Latest settings snapshot, kept in sync for UI rebuilds. */
    private var latestSettings: com.punchreminder.app.data.AppSettings? = null

    /** The day currently being observed; changes exactly once at local midnight. */
    private val currentDay = MutableStateFlow(clock.today())

    init {
        // Re-arm reminders, and close yesterday if the app is opened after midnight.
        viewModelScope.launch { rescheduleAndCleanUp() }

        // Observe settings changes (time zone, workdays, times).
        viewModelScope.launch {
            settingsRepository.settings.collect { saved ->
                latestZone = saved.toReminderSettings().zoneId
                latestSettings = saved
                rebuildUi(saved)
            }
        }

        // Observe the current day's session reactively. Re-subscribes when the date changes at
        // midnight, so the Home screen never keeps showing the previous day's session.
        viewModelScope.launch {
            currentDay.collectLatest { day ->
                repo.observeSession(day.toString()).collect { session ->
                    val settings = latestSettings ?: settingsRepository.settings.first()
                    latestSettings = settings
                    _uiState.value = buildUiState(session, settings)
                }
            }
        }

        // Tick every second so the on-screen clock tracks the system time exactly.
        viewModelScope.launch {
            while (true) {
                val now = clock.now
                val today = clock.today()
                if (today != currentDay.value) {
                    // Midnight rollover: finalize yesterday, switch the observed session and arm
                    // today's reminders without waiting for the daily safety-net alarm.
                    currentDay.value = today
                    rescheduleAndCleanUp()
                }
                val zoned = now.atZone(latestZone)
                val snapshot = _uiState.value
                val canSignInNow = snapshot.state == AttendanceState.NOT_SIGNED_IN &&
                    latestSettings?.let { settings ->
                        val signInInstant = zoned.toLocalDate()
                            .atTime(settings.signInHour, settings.signInMinute)
                            .atZone(latestZone)
                            .toInstant()
                        !now.isBefore(signInInstant)
                    } == true
                _uiState.value = snapshot.copy(
                    currentDate = zoned.format(dateFormatter),
                    currentTime = zoned.format(clockFormatter),
                    canSignIn = canSignInNow
                )
                delay(1_000)
            }
        }
    }

    /**
     * Close any session left open on a previous day and rebuild today's reminders. When the
     * rollover actually closed something, stale reminder notifications from yesterday are removed.
     */
    private suspend fun rescheduleAndCleanUp() {
        val autoClosed = repo.rescheduleAll()
        if (autoClosed > 0) {
            DiContainer.notificationHelper.cancelAll()
        }
    }

    /** Rebuild the full UI state (used on settings change). */
    private suspend fun rebuildUi(settings: com.punchreminder.app.data.AppSettings) {
        val session = repo.findTodaysSession()
        _uiState.value = buildUiState(session, settings)
    }

    private suspend fun buildUiState(
        session: AttendanceSession?,
        settings: com.punchreminder.app.data.AppSettings
    ): HomeUiState {
        val now = clock.now
        val today = clock.today()
        val zone = settings.toReminderSettings().zoneId
        val nowMillis = now.toEpochMilli()

        val currentDate = now.atZone(zone).format(dateFormatter)
        val currentTime = now.atZone(zone).format(clockFormatter)

        val isWorkdayToday = Workdays.isWorkday(settings.workdays, today.dayOfWeek)
        val signInTimeText = formatLocalTime(today, settings.signInHour, settings.signInMinute, zone)
        val currentState = session?.state ?: AttendanceState.NOT_SIGNED_IN
        val signInInstant = today
            .atTime(settings.signInHour, settings.signInMinute)
            .atZone(zone)
            .toInstant()
        val canSignIn = currentState == AttendanceState.NOT_SIGNED_IN && !now.isBefore(signInInstant)
        val canSignOut = currentState == AttendanceState.SIGNED_IN ||
            currentState == AttendanceState.SIGN_OUT_DUE

        // Headline and supporting line for the status card.
        val statusTitle: String
        val statusSubtitle: String
        when (session?.state) {
            AttendanceState.SIGNED_OUT -> {
                statusTitle = "Today\u2019s session is complete"
                statusSubtitle = "You\u2019re all caught up."
            }
            AttendanceState.NOT_SIGNED_OUT -> {
                statusTitle = "No sign-out recorded"
                statusSubtitle = "This day was closed automatically at midnight."
            }
            AttendanceState.SIGNED_IN -> {
                statusTitle = "You\u2019re signed in"
                statusSubtitle = "Sign-out reminder is scheduled."
            }
            AttendanceState.SIGN_OUT_DUE -> {
                statusTitle = "Time to sign out"
                statusSubtitle = "Remember to sign out of greytHR."
            }
            AttendanceState.SKIPPED -> {
                statusTitle = "Reminders paused today"
                val reason = session.pauseReason
                statusSubtitle = if (!reason.isNullOrBlank()) {
                    "$reason \u00b7 Resumes tomorrow."
                } else {
                    "They\u2019ll resume tomorrow."
                }
            }
            else -> {
                if (!settings.remindersEnabled) {
                    statusTitle = "Reminders are off"
                    statusSubtitle = "Turn them back on in Settings."
                } else if (!isWorkdayToday) {
                    statusTitle = "No reminders today"
                    statusSubtitle = "Today isn\u2019t one of your workdays."
                } else {
                    statusTitle = "Not signed in yet"
                    statusSubtitle = "Sign-in reminder at $signInTimeText"
                }
            }
        }

        var actualSignIn: String? = null
        var actualSignOut: String? = null
        var nextReminderText = if (settings.remindersEnabled && isWorkdayToday) {
            "Sign-in at $signInTimeText"
        } else {
            "No reminders remaining today"
        }

        session?.let { s ->
            s.confirmedSignInAt?.let { actualSignIn = formatInstant(it, zone) }
            s.confirmedSignOutAt?.let { actualSignOut = formatInstant(it, zone) }

            when (s.state) {
                AttendanceState.NOT_SIGNED_IN -> {
                    nextReminderText = if (settings.remindersEnabled && isWorkdayToday) {
                        "Sign-in at $signInTimeText"
                    } else {
                        "No reminders remaining today"
                    }
                }
                AttendanceState.SIGNED_IN -> {
                    val due = s.scheduledSignOutAt
                    nextReminderText = if (due != null && due > nowMillis) {
                        "Sign-out at ${formatInstant(due, zone)}"
                    } else {
                        "No reminders remaining today"
                    }
                }
                AttendanceState.SIGN_OUT_DUE -> {
                    nextReminderText = "Due now"
                }
                AttendanceState.SIGNED_OUT,
                AttendanceState.NOT_SIGNED_OUT,
                AttendanceState.SKIPPED -> {
                    nextReminderText = "No reminders remaining today"
                }
            }
        }

        return HomeUiState(
            currentDate = currentDate,
            currentTime = currentTime,
            statusTitle = statusTitle,
            statusSubtitle = statusSubtitle,
            actualSignIn = actualSignIn,
            actualSignOut = actualSignOut,
            nextReminderText = nextReminderText,
            signInTimeText = signInTimeText,
            pauseReason = session?.pauseReason,
            state = currentState,
            canSignIn = canSignIn,
            canSignOut = canSignOut,
            remindersEnabled = settings.remindersEnabled,
            isWorkday = isWorkdayToday,
            isSkipped = session?.state == AttendanceState.SKIPPED,
        )
    }

    private suspend fun formatInstant(millis: Long, zone: ZoneId): String =
        java.time.Instant.ofEpochMilli(millis).atZone(zone).format(timeFormatter)

    private suspend fun formatLocalTime(day: java.time.LocalDate, hour: Int, minute: Int, zone: ZoneId): String =
        day.atTime(hour, minute).atZone(zone).format(timeFormatter)

    /** The user confirmed sign-in from the home screen button. */
    fun confirmSignIn() {
        viewModelScope.launch {
            repo.confirmSignIn()
            DiContainer.notificationHelper.cancelSignIn()
            // UI updates reactively via observeSession()
        }
    }

    /** The user confirmed sign-out from the home screen button. */
    fun confirmSignOut() {
        viewModelScope.launch {
            repo.confirmSignOut()
            DiContainer.notificationHelper.cancelAll()
            // UI updates reactively via observeSession()
        }
    }

    fun pauseToday(reason: String) {
        viewModelScope.launch {
            if (_uiState.value.state != AttendanceState.NOT_SIGNED_IN) return@launch
            repo.skipToday(reason)
            DiContainer.notificationHelper.cancelAll()
            // UI updates reactively via observeSession()
        }
    }

    fun resumeToday() {
        viewModelScope.launch {
            repo.resumeToday()
            // UI updates reactively via observeSession()
        }
    }

    fun snoozeSignIn() {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            repo.snoozeSignIn(settings.snoozeMinutes)
            // UI updates reactively via observeSession()
        }
    }

    fun snoozeSignOut() {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            repo.snoozeSignOut(settings.snoozeMinutes)
            // UI updates reactively via observeSession()
        }
    }

    fun openGreytHR() {
        DiContainer.greytHrLauncher.openGreytHR(DiContainer.context)
    }

    fun reschedule() {
        viewModelScope.launch { rescheduleAndCleanUp() }
    }
}