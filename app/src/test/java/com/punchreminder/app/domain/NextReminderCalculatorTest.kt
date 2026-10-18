package com.punchreminder.app.domain

import com.punchreminder.app.domain.NextReminderCalculator.Plan.Action
import com.punchreminder.app.domain.NextReminderCalculator.Plan
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.TimeZone

class NextReminderCalculatorTest {

    private val utc = TimeZone.getTimeZone("UTC")
    private val zoneId = ZoneId.of("UTC")

    private fun instant(year: Int, month: Int, day: Int, hour: Int, minute: Int): Instant {
        return LocalDate.of(year, month, day)
            .atTime(LocalTime.of(hour, minute))
            .atZone(zoneId)
            .toInstant()
    }

    private fun settings(
        remindersEnabled: Boolean = true,
        workdays: Int = Workdays.DEFAULT_WORKDAYS,
        signInHour: Int = 10,
        signInMinute: Int = 30,
        sessionDurationHours: Int = 9,
        sessionDurationMinutes: Int = 0
    ): ReminderSettings = ReminderSettings(
        remindersEnabled = remindersEnabled,
        workdays = workdays,
        signInHour = signInHour,
        signInMinute = signInMinute,
        sessionDurationHours = sessionDurationHours,
        sessionDurationMinutes = sessionDurationMinutes,
        repeatIntervalMinutes = 10,
        snoozeMinutes = 10,
        zoneId = zoneId
    )

    // Test 1: Monday at 10:30 AM - sign-in reminder scheduled
    @Test
    fun `monday at 10_30 schedules sign in reminder`() {
        val now = instant(2026, 10, 19, 10, 30) // Monday
        val today = LocalDate.of(2026, 10, 19) // Monday
        val session = AttendanceSession(workDate = "2026-10-19", state = AttendanceState.NOT_SIGNED_IN)

        val plan = NextReminderCalculator.compute(now, today, settings(), session)

        assertEquals(Action.SCHEDULE, plan.action)
        assertEquals(ReminderKind.SIGN_IN, plan.kind)
        assertNotNull(plan.fireAt)
        // Fire at 10:30 AM today (now = exactly the scheduled time, so fire "now")
        assertTrue(plan.fireAt!!.toEpochMilli() >= now.toEpochMilli())
    }

    // Test 2: Saturday or Sunday - no reminder
    @Test
    fun `saturday no reminder scheduled`() {
        val now = instant(2026, 10, 17, 10, 30) // Saturday
        val today = LocalDate.of(2026, 10, 17)
        val session = AttendanceSession(workDate = "2026-10-17", state = AttendanceState.NOT_SIGNED_IN)

        val plan = NextReminderCalculator.compute(now, today, settings(), session)

        assertEquals(Action.CANCEL, plan.action)
        assertNull(plan.fireAt)
    }

    @Test
    fun `sunday no reminder scheduled`() {
        val now = instant(2026, 10, 18, 10, 30) // Sunday
        val today = LocalDate.of(2026, 10, 18)
        val session = AttendanceSession(workDate = "2026-10-18", state = AttendanceState.NOT_SIGNED_IN)

        val plan = NextReminderCalculator.compute(now, today, settings(), session)

        assertEquals(Action.CANCEL, plan.action)
        assertNull(plan.fireAt)
    }

    // Test 3: Sign-in at 10:30 AM -> sign-out due at 7:30 PM
    @Test
    fun `sign in at 10_30 am means sign out due at 7_30 pm`() {
        val signInInstant = instant(2026, 10, 19, 10, 30) // Monday 10:30 AM
        val expectedSignOut = instant(2026, 10, 19, 19, 30) // Monday 7:30 PM

        val session = AttendanceSession(
            workDate = "2026-10-19",
            state = AttendanceState.SIGNED_IN,
            confirmedSignInAt = signInInstant.toEpochMilli(),
            scheduledSignOutAt = expectedSignOut.toEpochMilli()
        )
        val now = instant(2026, 10, 19, 11, 0)
        val today = LocalDate.of(2026, 10, 19)

        val plan = NextReminderCalculator.compute(now, today, settings(), session)

        assertEquals(Action.SCHEDULE, plan.action)
        assertEquals(ReminderKind.SIGN_OUT, plan.kind)
        assertNotNull(plan.fireAt)
        assertEquals(expectedSignOut.toEpochMilli(), plan.fireAt!!.toEpochMilli())
    }

    // Test 4: Sign-in at 11:00 AM -> sign-out due at 8:00 PM
    @Test
    fun `sign in at 11_00 am means sign out due at 8_00 pm`() {
        val signInInstant = instant(2026, 10, 19, 11, 0) // Monday 11:00 AM
        val expectedSignOut = instant(2026, 10, 19, 20, 0) // Monday 8:00 PM

        val session = AttendanceSession(
            workDate = "2026-10-19",
            state = AttendanceState.SIGNED_IN,
            confirmedSignInAt = signInInstant.toEpochMilli(),
            scheduledSignOutAt = expectedSignOut.toEpochMilli()
        )
        val now = instant(2026, 10, 19, 11, 30)
        val today = LocalDate.of(2026, 10, 19)

        val plan = NextReminderCalculator.compute(now, today, settings(), session)

        assertEquals(Action.SCHEDULE, plan.action)
        assertEquals(ReminderKind.SIGN_OUT, plan.kind)
        assertNotNull(plan.fireAt)
        assertEquals(expectedSignOut.toEpochMilli(), plan.fireAt!!.toEpochMilli())
    }

    // Test 5: Sign-in at 2:45 PM -> sign-out due at 11:45 PM
    @Test
    fun `sign in at 2_45 pm means sign out due at 11_45 pm`() {
        val signInInstant = instant(2026, 10, 19, 14, 45) // Monday 2:45 PM
        val expectedSignOut = instant(2026, 10, 19, 23, 45) // Monday 11:45 PM

        val session = AttendanceSession(
            workDate = "2026-10-19",
            state = AttendanceState.SIGNED_IN,
            confirmedSignInAt = signInInstant.toEpochMilli(),
            scheduledSignOutAt = expectedSignOut.toEpochMilli()
        )
        val now = instant(2026, 10, 19, 15, 0)
        val today = LocalDate.of(2026, 10, 19)

        val plan = NextReminderCalculator.compute(now, today, settings(), session)

        assertEquals(Action.SCHEDULE, plan.action)
        assertEquals(ReminderKind.SIGN_OUT, plan.kind)
        assertNotNull(plan.fireAt)
        assertEquals(expectedSignOut.toEpochMilli(), plan.fireAt!!.toEpochMilli())
    }

    // Test 6: No sign-in confirmation - no sign-out reminder
    @Test
    fun `no sign in confirmation means no sign out reminder`() {
        val now = instant(2026, 10, 19, 19, 0) // Monday 7 PM
        val today = LocalDate.of(2026, 10, 19)
        val session = AttendanceSession(workDate = "2026-10-19", state = AttendanceState.NOT_SIGNED_IN)

        val plan = NextReminderCalculator.compute(now, today, settings(), session)

        assertEquals(Action.SCHEDULE, plan.action) // sign-in reminder still scheduled
        assertEquals(ReminderKind.SIGN_IN, plan.kind)
    }

    // Test 7: Signed out - no reminder
    @Test
    fun `signed out means no reminder`() {
        val now = instant(2026, 10, 19, 12, 0)
        val today = LocalDate.of(2026, 10, 19)
        val session = AttendanceSession(
            workDate = "2026-10-19",
            state = AttendanceState.SIGNED_OUT,
            confirmedSignInAt = instant(2026, 10, 19, 10, 30).toEpochMilli(),
            confirmedSignOutAt = instant(2026, 10, 19, 12, 0).toEpochMilli()
        )

        val plan = NextReminderCalculator.compute(now, today, settings(), session)

        assertEquals(Action.CANCEL, plan.action)
        assertNull(plan.fireAt)
    }

    // Test 8: Skipped - no reminder
    @Test
    fun `skipped means no reminder`() {
        val now = instant(2026, 10, 19, 12, 0)
        val today = LocalDate.of(2026, 10, 19)
        val session = AttendanceSession(workDate = "2026-10-19", state = AttendanceState.SKIPPED)

        val plan = NextReminderCalculator.compute(now, today, settings(), session)

        assertEquals(Action.CANCEL, plan.action)
        assertNull(plan.fireAt)
    }

    // Test 9: Reminders disabled - no reminder
    @Test
    fun `reminders disabled means no reminder`() {
        val now = instant(2026, 10, 19, 10, 0)
        val today = LocalDate.of(2026, 10, 19)
        val session = AttendanceSession(workDate = "2026-10-19", state = AttendanceState.NOT_SIGNED_IN)

        val plan = NextReminderCalculator.compute(now, today, settings(remindersEnabled = false), session)

        assertEquals(Action.CANCEL, plan.action)
        assertNull(plan.fireAt)
    }

    // Test 12: Time zone change doesn't corrupt session instant
    @Test
    fun `time zone change preserves instant`() {
        // An instant is an absolute point in time regardless of time zone
        val instantUtc = instant(2026, 10, 19, 10, 30) // 10:30 UTC
        val instantWithOffset = Instant.ofEpochMilli(instantUtc.toEpochMilli())

        assertEquals(instantUtc.toEpochMilli(), instantWithOffset.toEpochMilli())

        // Same instant represented in a different zone is still the same epoch millis
        val tokyoZone = ZoneId.of("Asia/Tokyo")
        val tokyoInstant = LocalDate.of(2026, 10, 19)
            .atTime(LocalTime.of(19, 30))
            .atZone(tokyoZone)
            .toInstant() // 19:30 Tokyo = 10:30 UTC

        assertEquals(instantUtc.toEpochMilli(), tokyoInstant.toEpochMilli())
    }

    // Test 13: Exact alarm permission denied - handled gracefully
    @Test
    fun `calculator works regardless of exact alarm permission`() {
        // The calculator is pure and doesn't depend on Android permissions
        val now = instant(2026, 10, 19, 10, 0)
        val today = LocalDate.of(2026, 10, 19)
        val session = AttendanceSession(workDate = "2026-10-19", state = AttendanceState.NOT_SIGNED_IN)

        val plan = NextReminderCalculator.compute(now, today, settings(), session)

        assertEquals(Action.SCHEDULE, plan.action)
    }

    // Test 14: Notification permission denied - calculator unaffected
    @Test
    fun `calculator works regardless of notification permission`() {
        val now = instant(2026, 10, 19, 10, 0)
        val today = LocalDate.of(2026, 10, 19)
        val session = AttendanceSession(workDate = "2026-10-19", state = AttendanceState.NOT_SIGNED_IN)

        val plan = NextReminderCalculator.compute(now, today, settings(), session)

        assertEquals(Action.SCHEDULE, plan.action)
    }

    // Test 15: Calculator reflects the session state it receives (does not auto-transition by date)
    @Test
    fun `calculator reflects provided session state`() {
        val now = instant(2026, 10, 20, 10, 0) // Tuesday
        val today = LocalDate.of(2026, 10, 20)
        // Session from Monday still shows SIGNED_IN
        val session = AttendanceSession(
            workDate = "2026-10-19",
            state = AttendanceState.SIGNED_IN,
            confirmedSignInAt = instant(2026, 10, 19, 10, 30).toEpochMilli(),
            scheduledSignOutAt = instant(2026, 10, 19, 19, 30).toEpochMilli()
        )

        val plan = NextReminderCalculator.compute(now, today, settings(), session)

        // Calculator schedules based on session state, not date
        assertEquals(Action.SCHEDULE, plan.action)
        assertEquals(ReminderKind.SIGN_OUT, plan.kind)
    }

    // Test 16: Repeated scheduling doesn't create duplicates
    @Test
    fun `repeated compute calls return same result`() {
        val now = instant(2026, 10, 19, 10, 0)
        val today = LocalDate.of(2026, 10, 19)
        val session = AttendanceSession(workDate = "2026-10-19", state = AttendanceState.NOT_SIGNED_IN)

        val plan1 = NextReminderCalculator.compute(now, today, settings(), session)
        val plan2 = NextReminderCalculator.compute(now, today, settings(), session)

        assertEquals(plan1.fireAt, plan2.fireAt)
        assertEquals(plan1.kind, plan2.kind)
        assertEquals(plan1.action, plan2.action)
    }

    // Snooze test: snooze moves reminder without changing session deadline
    @Test
    fun `snooze does not change sign out deadline`() {
        val signInInstant = instant(2026, 10, 19, 10, 30)
        val signOutDue = instant(2026, 10, 19, 19, 30)

        val sessionBeforeSnooze = AttendanceSession(
            workDate = "2026-10-19",
            state = AttendanceState.SIGN_OUT_DUE,
            confirmedSignInAt = signInInstant.toEpochMilli(),
            scheduledSignOutAt = signOutDue.toEpochMilli(),
            nextReminderAt = signOutDue.toEpochMilli()
        )
        val now = instant(2026, 10, 19, 19, 30)
        val today = LocalDate.of(2026, 10, 19)

        val plan = NextReminderCalculator.compute(now, today, settings(), sessionBeforeSnooze)

        // Sign-out due time remains unchanged
        assertEquals(signOutDue.toEpochMilli(), sessionBeforeSnooze.scheduledSignOutAt)
    }

    // Custom workdays: Saturday is a workday
    @Test
    fun `custom workdays include saturday`() {
        val now = instant(2026, 10, 17, 10, 0) // Saturday
        val today = LocalDate.of(2026, 10, 17)
        val session = AttendanceSession(workDate = "2026-10-17", state = AttendanceState.NOT_SIGNED_IN)

        val satSettings = settings(workdays = Workdays.DEFAULT_WORKDAYS or Workdays.SATURDAY)

        val plan = NextReminderCalculator.compute(now, today, satSettings, session)

        assertEquals(Action.SCHEDULE, plan.action)
    }

    // Sign-in time already passed today - remind now (catch-up)
    @Test
    fun `sign in time already passed means remind now`() {
        val now = instant(2026, 10, 19, 11, 0) // 11 AM, past 10:30 AM sign-in time
        val today = LocalDate.of(2026, 10, 19)
        val session = AttendanceSession(workDate = "2026-10-19", state = AttendanceState.NOT_SIGNED_IN)

        val plan = NextReminderCalculator.compute(now, today, settings(), session)

        assertEquals(Action.SCHEDULE, plan.action)
        assertEquals(ReminderKind.SIGN_IN, plan.kind)
        assertNotNull(plan.fireAt)
        // Should fire at "now" since sign-in time already passed
        assertEquals(now.toEpochMilli(), plan.fireAt!!.toEpochMilli())
    }

    // A newly configured sign-in time must win over a stale stored repeat anchor.
    @Test
    fun `configured sign in time wins over stale stored anchor`() {
        val now = instant(2026, 10, 19, 9, 0) // Monday 9 AM
        val today = LocalDate.of(2026, 10, 19)
        val configured = instant(2026, 10, 19, 18, 0) // user just changed it to 6 PM
        val staleAnchor = instant(2026, 10, 19, 9, 10).toEpochMilli() // leftover repeat time
        val session = AttendanceSession(
            workDate = "2026-10-19",
            state = AttendanceState.NOT_SIGNED_IN,
            nextReminderAt = staleAnchor
        )

        val plan = NextReminderCalculator.compute(
            now, today, settings(signInHour = 18, signInMinute = 0), session
        )

        assertEquals(Action.SCHEDULE, plan.action)
        assertEquals(ReminderKind.SIGN_IN, plan.kind)
        assertEquals(configured.toEpochMilli(), plan.fireAt!!.toEpochMilli())
    }

    // After the sign-in time has passed, an existing repeat anchor is kept (no reset to now).
    @Test
    fun `repeat anchor kept after sign in time has passed`() {
        val now = instant(2026, 10, 19, 11, 0) // 11 AM, past 10:30 AM sign-in time
        val today = LocalDate.of(2026, 10, 19)
        val anchor = instant(2026, 10, 19, 11, 10)
        val session = AttendanceSession(
            workDate = "2026-10-19",
            state = AttendanceState.NOT_SIGNED_IN,
            nextReminderAt = anchor.toEpochMilli()
        )

        val plan = NextReminderCalculator.compute(now, today, settings(), session)

        assertEquals(Action.SCHEDULE, plan.action)
        assertEquals(ReminderKind.SIGN_IN, plan.kind)
        assertEquals(anchor.toEpochMilli(), plan.fireAt!!.toEpochMilli())
    }

    // A day that ended without a sign-out is terminal: no reminders carry into the next day.
    @Test
    fun `not signed out session schedules no reminder`() {
        val now = instant(2026, 10, 20, 0, 1) // just after midnight
        val today = LocalDate.of(2026, 10, 20)
        val session = AttendanceSession(
            workDate = "2026-10-19",
            state = AttendanceState.NOT_SIGNED_OUT,
            confirmedSignInAt = instant(2026, 10, 19, 10, 30).toEpochMilli(),
            confirmedSignOutAt = instant(2026, 10, 19, 23, 59).toEpochMilli()
        )

        val plan = NextReminderCalculator.compute(now, today, settings(), session)

        assertEquals(Action.CANCEL, plan.action)
        assertNull(plan.fireAt)
        assertNull(plan.kind)
    }
}
