package com.punchreminder.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.punchreminder.app.DiContainer
import com.punchreminder.app.domain.AttendanceSession
import com.punchreminder.app.domain.AttendanceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Default workday length used when the user has not configured a session duration. */
private const val DEFAULT_SESSION_MINUTES = 9 * 60L

data class HistoryItem(
    /** Raw ISO work date (yyyy-MM-dd) — used as the stable key and for deletion. */
    val workDate: String,
    /** Human-readable date shown in the list. */
    val date: String,
    val state: String,
    val signIn: String?,
    val signOut: String?,
    val totalDuration: String?,
    val note: String?
)

class HistoryViewModel : ViewModel() {

    private val repo = DiContainer.attendanceRepository
    private val settingsRepository = DiContainer.settingsRepository
    private val clock = DiContainer.clock

    private val _items = MutableStateFlow<List<HistoryItem>>(emptyList())
    val items: StateFlow<List<HistoryItem>> = _items

    private val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.getDefault())
    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

    init {
        // Reactively observe all sessions — updates instantly when a notification action
        // modifies the database (e.g. sign-out from notification).
        viewModelScope.launch {
            repo.observeAllSessions().collectLatest { sessions ->
                val settings = settingsRepository.settings.first()
                val zone = clock.timeZone.toZoneId()
                val requiredMillis = requiredDurationMillis(settings)
                _items.value = sessions.map { session ->
                    val workedMillis = workedDurationMillis(session)
                    HistoryItem(
                        workDate = session.workDate,
                        date = formatDate(session.workDate),
                        state = displayState(session.state, workedMillis, requiredMillis),
                        signIn = session.confirmedSignInAt?.let { formatInstant(it, zone) },
                        signOut = session.confirmedSignOutAt?.let { formatInstant(it, zone) },
                        totalDuration = workedMillis?.let { formatDuration(it) },
                        note = when (session.state) {
                            AttendanceState.SKIPPED -> session.pauseReason ?: "Paused by user"
                            AttendanceState.NOT_SIGNED_OUT -> "Auto-closed at end of day"
                            else -> null
                        }
                    )
                }
            }
        }
    }

    /** The configured work-session length in millis (the target for a full day). */
    private fun requiredDurationMillis(settings: com.punchreminder.app.data.AppSettings): Long {
        val minutes = settings.sessionDurationHours * 60L + settings.sessionDurationMinutes
        val effectiveMinutes = if (minutes > 0L) minutes else DEFAULT_SESSION_MINUTES
        return effectiveMinutes * 60_000L
    }

    /** Actual worked time between confirmed sign-in and sign-out, or null when either is missing. */
    private fun workedDurationMillis(session: AttendanceSession): Long? {
        val signIn = session.confirmedSignInAt ?: return null
        val signOut = session.confirmedSignOutAt ?: return null
        return (signOut - signIn).coerceAtLeast(0L)
    }

    private fun formatDuration(millis: Long): String {
        val totalMinutes = millis / 60_000L
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        return "${hours}h ${minutes}m"
    }

    private fun formatDate(isoDate: String): String {
        return try {
            java.time.LocalDate.parse(isoDate).format(dateFormatter)
        } catch (_: Exception) {
            isoDate
        }
    }

    /**
     * Human-friendly label for a session. A completed session counts as "Completed" only when the
     * worked time reached the configured work-session length; otherwise it is "Incomplete".
     * A session the day-cycle closed because no sign-out was confirmed is "Not signed out".
     */
    private fun displayState(
        state: AttendanceState,
        workedMillis: Long?,
        requiredMillis: Long
    ): String = when (state) {
        AttendanceState.NOT_SIGNED_IN -> "Not signed in"
        AttendanceState.SIGNED_IN -> "In progress"
        AttendanceState.SIGN_OUT_DUE -> "Sign-out due"
        AttendanceState.SIGNED_OUT ->
            if (workedMillis != null && workedMillis < requiredMillis) "Incomplete" else "Completed"
        AttendanceState.NOT_SIGNED_OUT -> "Not signed out"
        AttendanceState.SKIPPED -> "Paused"
    }

    private fun formatInstant(millis: Long, zone: ZoneId): String =
        Instant.ofEpochMilli(millis).atZone(zone).format(timeFormatter)

    fun clearHistory() {
        viewModelScope.launch {
            repo.clearHistory()
            // observeAllSessions() will emit the empty list reactively
        }
    }

    /** Delete a single history record by its date string. */
    fun deleteItem(workDate: String) {
        viewModelScope.launch {
            repo.deleteSession(workDate)
            // observeAllSessions() will emit the updated list reactively
        }
    }
}