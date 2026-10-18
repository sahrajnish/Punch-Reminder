package com.punchreminder.app.domain

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WorkdayCycleTest {

    private val zone = ZoneId.of("Asia/Kolkata")

    private fun millis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDate.of(year, month, day)
            .atTime(LocalTime.of(hour, minute))
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

    @Test
    fun `end of day is the last millisecond of the local date`() {
        val date = LocalDate.of(2026, 10, 19)
        val endOfDay = WorkdayCycle.endOfDayMillis(date, zone)
        val nextMidnight = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        assertEquals(nextMidnight - 1L, endOfDay)
    }

    @Test
    fun `only signed in and sign out due count as open`() {
        assertTrue(WorkdayCycle.isOpen(AttendanceState.SIGNED_IN))
        assertTrue(WorkdayCycle.isOpen(AttendanceState.SIGN_OUT_DUE))
        assertFalse(WorkdayCycle.isOpen(AttendanceState.NOT_SIGNED_IN))
        assertFalse(WorkdayCycle.isOpen(AttendanceState.SIGNED_OUT))
        assertFalse(WorkdayCycle.isOpen(AttendanceState.NOT_SIGNED_OUT))
        assertFalse(WorkdayCycle.isOpen(AttendanceState.SKIPPED))
    }

    @Test
    fun `forgotten sign out is recorded at the end of its work date`() {
        val workDate = LocalDate.of(2026, 10, 19)
        val signIn = millis(2026, 10, 19, 10, 30)
        val rolloverTime = millis(2026, 10, 20, 0, 1)

        val session = AttendanceSession(
            workDate = workDate.toString(),
            state = AttendanceState.SIGNED_IN,
            confirmedSignInAt = signIn,
            scheduledSignOutAt = millis(2026, 10, 19, 19, 30),
            nextReminderAt = millis(2026, 10, 19, 19, 30),
            nextReminderKind = ReminderKind.SIGN_OUT
        )

        val closed = WorkdayCycle.closeAtEndOfDay(session, zone, rolloverTime)

        assertEquals(AttendanceState.NOT_SIGNED_OUT, closed.state)
        assertEquals(WorkdayCycle.endOfDayMillis(workDate, zone), closed.confirmedSignOutAt)
        assertEquals(signIn, closed.confirmedSignInAt)
        assertNull(closed.nextReminderAt)
        assertNull(closed.nextReminderKind)
        assertEquals(rolloverTime, closed.updatedAt)
    }

    @Test
    fun `sign out due session is closed the same way`() {
        val workDate = LocalDate.of(2026, 10, 19)
        val session = AttendanceSession(
            workDate = workDate.toString(),
            state = AttendanceState.SIGN_OUT_DUE,
            confirmedSignInAt = millis(2026, 10, 19, 9, 0)
        )

        val closed = WorkdayCycle.closeAtEndOfDay(
            session,
            zone,
            nowMillis = millis(2026, 10, 20, 0, 1)
        )

        assertEquals(AttendanceState.NOT_SIGNED_OUT, closed.state)
        assertEquals(
            WorkdayCycle.endOfDayMillis(workDate, zone) - millis(2026, 10, 19, 9, 0),
            closed.confirmedSignOutAt!! - closed.confirmedSignInAt!!
        )
    }

    @Test
    fun `sign out instant never precedes sign in`() {
        val workDate = LocalDate.of(2026, 10, 19)
        val endOfDay = WorkdayCycle.endOfDayMillis(workDate, zone)
        val corruptSignIn = endOfDay + 5_000

        val closed = WorkdayCycle.closeAtEndOfDay(
            AttendanceSession(
                workDate = workDate.toString(),
                state = AttendanceState.SIGNED_IN,
                confirmedSignInAt = corruptSignIn
            ),
            zone,
            nowMillis = millis(2026, 10, 20, 0, 1)
        )

        assertEquals(corruptSignIn, closed.confirmedSignOutAt)
    }
}
