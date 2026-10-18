package com.punchreminder.app.domain

/**
 * A single workday's attendance session.
 *
 * All timestamps are epoch milliseconds (UTC instants). Storing instants -- rather than local
 * wall-clock strings -- guarantees that time-zone changes or device clock changes cannot corrupt
 * a recorded sign-in/sign-out moment. Local time is only used for display and for computing the
 * daily recurring sign-in reminder time.
 */
data class AttendanceSession(
    val id: Long = -1,
    val workDate: String, // ISO-8601 LocalDate, e.g. "2026-10-18"
    val state: AttendanceState = AttendanceState.NOT_SIGNED_IN,
    /** Confirmed sign-in instant (epoch millis), null until the user confirms. */
    val confirmedSignInAt: Long? = null,
    /**
     * Sign-out instant (epoch millis). Normally set when the user confirms sign-out; for an
     * auto-closed [AttendanceState.NOT_SIGNED_OUT] session it is set to the end of the work date.
     */
    val confirmedSignOutAt: Long? = null,
    /**
     * Calculated sign-out reminder due time = confirmedSignInAt + sessionDuration (epoch millis).
     * Never set unless a sign-in has been confirmed.
     */
    val scheduledSignOutAt: Long? = null,
    /** Next pending reminder fire instant (epoch millis), null when no reminder is pending. */
    val nextReminderAt: Long? = null,
    /** The kind of the pending reminder, matching [nextReminderAt]. */
    val nextReminderKind: ReminderKind? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)
