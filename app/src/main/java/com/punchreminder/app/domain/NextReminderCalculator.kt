package com.punchreminder.app.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Pure scheduling engine.
 *
 * Given the current instant, today's date, user settings, and the current session, this computes
 * what reminder (if any) should be pending right now. It has no Android dependencies so it can be
 * unit-tested deterministically with an injectable clock.
 *
 * Rules implemented:
 *   - No reminder when reminders are disabled or today is not a workday.
 *   - NOT_SIGNED_IN: remind at today's sign-in time (or now if already past), repeating per interval.
 *   - SIGNED_IN: remind at sign-in + session duration (the due time).
 *   - SIGN_OUT_DUE: keep reminding per interval until confirmed.
 *   - SIGNED_OUT / NOT_SIGNED_OUT / SKIPPED: no reminder.
 *   - A snooze only moves [nextReminderAt]; it never changes the sign-in timestamp or due time.
 */
object NextReminderCalculator {

    data class Plan(
        val fireAt: Instant?,
        val kind: ReminderKind?,
        val action: Action
    ) {
        enum class Action { SCHEDULE, CANCEL }
    }

    fun compute(
        now: Instant,
        today: LocalDate,
        settings: ReminderSettings,
        session: AttendanceSession
    ): Plan {
        if (!settings.remindersEnabled) {
            return Plan(null, null, Plan.Action.CANCEL)
        }
        if (!Workdays.isWorkday(settings.workdays, today.dayOfWeek)) {
            return Plan(null, null, Plan.Action.CANCEL)
        }

        val signInToday = today.atTime(
            LocalTime.of(settings.signInHour, settings.signInMinute)
        ).atZone(settings.zoneId).toInstant()

        return when (session.state) {
            AttendanceState.NOT_SIGNED_IN -> {
                val nextReminderInstant = session.nextReminderAt
                    ?.let { ms -> Instant.ofEpochMilli(ms) }
                // Before today's configured sign-in time, always aim at that time so a changed
                // setting takes effect immediately. Once it has passed, keep the pending
                // repeat/snooze anchor so the interval chain is preserved.
                val fire = when {
                    signInToday.isAfter(now) -> signInToday
                    nextReminderInstant != null && nextReminderInstant.isAfter(now) -> nextReminderInstant
                    else -> now
                }
                Plan(fire, ReminderKind.SIGN_IN, Plan.Action.SCHEDULE)
            }

            AttendanceState.SIGNED_IN -> {
                val due = session.scheduledSignOutAt
                    ?.let { ms -> Instant.ofEpochMilli(ms) }
                    ?: return Plan(null, null, Plan.Action.CANCEL)
                val fire = session.nextReminderAt
                    ?.let { ms -> Instant.ofEpochMilli(ms) }
                    ?.takeIf { it.isAfter(now) }
                    ?: due
                Plan(fire, ReminderKind.SIGN_OUT, Plan.Action.SCHEDULE)
            }

            AttendanceState.SIGN_OUT_DUE -> {
                val fire = session.nextReminderAt
                    ?.let { ms -> Instant.ofEpochMilli(ms) }
                    ?.takeIf { it.isAfter(now) }
                    ?: now
                Plan(fire, ReminderKind.SIGN_OUT, Plan.Action.SCHEDULE)
            }

            AttendanceState.SIGNED_OUT,
            AttendanceState.NOT_SIGNED_OUT,
            AttendanceState.SKIPPED -> Plan(null, null, Plan.Action.CANCEL)
        }
    }
}

/** User-facing reminder settings (a subset of [AppSettings] used by the calculator). */
data class ReminderSettings(
    val remindersEnabled: Boolean,
    val workdays: Int,
    val signInHour: Int,
    val signInMinute: Int,
    val sessionDurationHours: Int,
    val sessionDurationMinutes: Int = 0,
    val repeatIntervalMinutes: Int,
    val snoozeMinutes: Int,
    val zoneId: ZoneId
)
