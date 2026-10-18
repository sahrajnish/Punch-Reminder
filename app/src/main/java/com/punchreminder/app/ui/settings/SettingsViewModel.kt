package com.punchreminder.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.punchreminder.app.DiContainer
import com.punchreminder.app.data.AppSettings
import com.punchreminder.app.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SettingsViewModel : ViewModel() {

    private val settingsRepository: SettingsRepository = DiContainer.settingsRepository
    private val repo = DiContainer.attendanceRepository

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    /**
     * Serializes read-modify-write updates so concurrent edits (the screen issues one save per
     * changed field) cannot clobber each other's values.
     */
    private val saveMutex = Mutex()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { saved ->
                _uiState.value = _uiState.value.copy(settings = saved, isLoaded = true)
            }
        }
    }

    data class SettingsUiState(
        val settings: AppSettings = AppSettings(),
        /** True once stored settings have been read at least once. */
        val isLoaded: Boolean = false,
        val isLoading: Boolean = false,
        val message: String? = null
    )

    /**
     * Applies [transform] to the latest persisted settings, saves the result, and refreshes the
     * in-memory state. Runs under [saveMutex] so overlapping calls build on each other.
     */
    private fun update(
        message: String? = null,
        reschedule: Boolean = false,
        transform: (AppSettings) -> AppSettings
    ) {
        viewModelScope.launch {
            saveMutex.withLock {
                val updated = transform(settingsRepository.settings.value)
                settingsRepository.saveSettings(updated)
                if (reschedule) DiContainer.attendanceRepository.rescheduleAll()
                _uiState.value = _uiState.value.copy(
                    settings = updated,
                    isLoaded = true,
                    message = message ?: _uiState.value.message
                )
            }
        }
    }

    fun updateWorkdays(bits: Int) =
        update(message = "Workdays updated", reschedule = true) { it.copy(workdays = bits) }

    fun updateSignInTime(hour: Int, minute: Int) =
        update(message = "Sign-in time updated", reschedule = true) {
            // Changing the normal sign-in time does not alter an active session's recorded time.
            it.copy(signInHour = hour, signInMinute = minute)
        }

    fun updateSessionDuration(hours: Int, minutes: Int) =
        update(message = "Session duration updated") {
            it.copy(sessionDurationHours = hours, sessionDurationMinutes = minutes)
        }

    fun updateRepeatInterval(minutes: Int) =
        update(message = "Repeat interval updated") { it.copy(repeatIntervalMinutes = minutes) }

    fun updateSnooze(minutes: Int) =
        update(message = "Snooze duration updated") { it.copy(snoozeMinutes = minutes) }

    fun toggleReminders(enabled: Boolean) {
        if (!enabled) {
            // With reminders off, any notification already on screen is obsolete.
            DiContainer.notificationHelper.cancelAll()
        }
        update(reschedule = true) { it.copy(remindersEnabled = enabled) }
    }

    fun toggleVibrate(enabled: Boolean) =
        update { it.copy(vibrateEnabled = enabled) }

    /** App-wide appearance toggle (dark vs light). */
    fun setDarkTheme(enabled: Boolean) =
        update { it.copy(darkTheme = enabled) }

    /**
     * Turns the custom reminder tune on or off. Enabling it with no tune selected yet defaults to
     * the system's default alarm (clock) tone.
     */
    fun setCustomSoundEnabled(enabled: Boolean) = update {
        if (enabled) {
            val defaultTone = android.media.RingtoneManager
                .getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                ?.toString()
            it.copy(
                useCustomSound = true,
                soundUri = it.soundUri ?: defaultTone
            )
        } else {
            it.copy(useCustomSound = false)
        }
    }

    fun updateSoundUri(uri: String?) =
        update(message = "Reminder tune updated") { it.copy(soundUri = uri) }

    fun restoreDefaults() {
        viewModelScope.launch {
            settingsRepository.restoreDefaults()
            DiContainer.attendanceRepository.rescheduleAll()
            _uiState.value = _uiState.value.copy(
                settings = settingsRepository.settings.value,
                message = "Settings restored to defaults"
            )
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repo.clearHistory()
            _uiState.value = _uiState.value.copy(message = "Local history cleared")
        }
    }
}
