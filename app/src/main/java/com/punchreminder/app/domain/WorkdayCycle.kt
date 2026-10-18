package com.punchreminder.app.domain

import java.time.LocalDate
import java.time.ZoneId

/**
 * The day-bounded attendance cycle.
 *
 * A cycle runs from 00:00 to 23:59:59.999 in the device's local time zone. If a session is still
 * [AttendanceState.SIGNED_IN] or [AttendanceState.SIGN_OUT_DUE] when the next day begins, no
 * sign-out was confirmed: the session is closed automatically and recorded as
 * [AttendanceState.NOT_SIGNED_OUT] with a sign-out instant at the last millisecond of its work
 * date. That keeps the recorded duration inside the day and lets the next day start cleanly.
 */
object WorkdayCycle {

    /** True for states that represent an active session which has not finished yet. */
    fun isOpen(state: AttendanceState): Boolean =
        state == AttendanceState.SIGNED_IN || state == AttendanceState.SIGN_OUT_DUE

    /** The last millisecond of [date] in [zone] (00:00 of the next day minus 1 ms). */
    fun endOfDayMillis(date: LocalDate, zone: ZoneId): Long =
        date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1L

    /**
     * Close [session] at the end of its work date as [AttendanceState.NOT_SIGNED_OUT].
     *
     * The recorded sign-out is the final millisecond of the work date, so the duration reflects
     * the whole 00:00–23:59 cycle. Reminder fields are cleared so nothing carries into the new day.
     * [nowMillis] is when the rollover was actually processed (may be later than the day boundary,
     * for example when the device was switched off overnight).
     */
    fun closeAtEndOfDay(
        session: AttendanceSession,
        zone: ZoneId,
        nowMillis: Long
    ): AttendanceSession {
        require(isOpen(session.state)) { "Only an open session can be closed: ${session.state}" }

        val endOfDay = runCatching {
            endOfDayMillis(LocalDate.parse(session.workDate), zone)
        }.getOrElse { nowMillis }

        // Guard against a corrupted sign-in instant that is later than the day boundary.
        val signOutAt = session.confirmedSignInAt?.let { maxOf(it, endOfDay) } ?: endOfDay

        return session.copy(
            state = AttendanceState.NOT_SIGNED_OUT,
            confirmedSignOutAt = signOutAt,
            nextReminderAt = null,
            nextReminderKind = null,
            updatedAt = nowMillis
        )
    }
}
