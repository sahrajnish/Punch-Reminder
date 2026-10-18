package com.punchreminder.app.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AttendanceStateMachineTest {

    private val machine = AttendanceStateMachine()

    // Valid transition: NOT_SIGNED_IN -> SIGNED_IN (confirm sign-in)
    @Test
    fun `not signed in can confirm sign in`() {
        assertTrue(machine.canTransition(AttendanceState.NOT_SIGNED_IN, AttendanceEvent.CONFIRM_SIGN_IN))
        assertEquals(AttendanceState.SIGNED_IN, machine.transition(AttendanceState.NOT_SIGNED_IN, AttendanceEvent.CONFIRM_SIGN_IN))
    }

    // Valid transition: NOT_SIGNED_IN -> SKIPPED (skip today)
    @Test
    fun `not signed in can skip today`() {
        assertTrue(machine.canTransition(AttendanceState.NOT_SIGNED_IN, AttendanceEvent.SKIP_TODAY))
        assertEquals(AttendanceState.SKIPPED, machine.transition(AttendanceState.NOT_SIGNED_IN, AttendanceEvent.SKIP_TODAY))
    }

    // Valid transition: SIGNED_IN -> SIGNED_OUT (early sign-out)
    @Test
    fun `signed in can confirm sign out`() {
        assertTrue(machine.canTransition(AttendanceState.SIGNED_IN, AttendanceEvent.CONFIRM_SIGN_OUT))
        assertEquals(AttendanceState.SIGNED_OUT, machine.transition(AttendanceState.SIGNED_IN, AttendanceEvent.CONFIRM_SIGN_OUT))
    }

    // Valid transition: SIGNED_IN -> SIGN_OUT_DUE (duration elapsed)
    @Test
    fun `signed in transitions to sign out due`() {
        assertTrue(machine.canTransition(AttendanceState.SIGNED_IN, AttendanceEvent.RESTORE_DUE))
        assertEquals(AttendanceState.SIGN_OUT_DUE, machine.transition(AttendanceState.SIGNED_IN, AttendanceEvent.RESTORE_DUE))
    }

    // Valid transition: SIGNED_IN -> SKIPPED
    @Test
    fun `signed in can skip today`() {
        assertTrue(machine.canTransition(AttendanceState.SIGNED_IN, AttendanceEvent.SKIP_TODAY))
        assertEquals(AttendanceState.SKIPPED, machine.transition(AttendanceState.SIGNED_IN, AttendanceEvent.SKIP_TODAY))
    }

    // Valid transition: SIGN_OUT_DUE -> SIGNED_OUT (confirm sign out)
    @Test
    fun `sign out due can confirm sign out`() {
        assertTrue(machine.canTransition(AttendanceState.SIGN_OUT_DUE, AttendanceEvent.CONFIRM_SIGN_OUT))
        assertEquals(AttendanceState.SIGNED_OUT, machine.transition(AttendanceState.SIGN_OUT_DUE, AttendanceEvent.CONFIRM_SIGN_OUT))
    }

    // Valid transition: SIGN_OUT_DUE -> SKIPPED
    @Test
    fun `sign out due can skip today`() {
        assertTrue(machine.canTransition(AttendanceState.SIGN_OUT_DUE, AttendanceEvent.SKIP_TODAY))
        assertEquals(AttendanceState.SKIPPED, machine.transition(AttendanceState.SIGN_OUT_DUE, AttendanceEvent.SKIP_TODAY))
    }

    // Invalid: NOT_SIGNED_IN -> CONFIRM_SIGN_OUT (can't sign out without signing in)
    @Test
    fun `not signed in cannot confirm sign out`() {
        assertFalse(machine.canTransition(AttendanceState.NOT_SIGNED_IN, AttendanceEvent.CONFIRM_SIGN_OUT))
    }

    // Invalid: NOT_SIGNED_IN -> RESTORE_DUE (can't be due without signing in)
    @Test
    fun `not signed in cannot transition to sign out due`() {
        assertFalse(machine.canTransition(AttendanceState.NOT_SIGNED_IN, AttendanceEvent.RESTORE_DUE))
    }

    // Invalid: SIGNED_OUT -> anything (terminal state)
    @Test
    fun `signed out is terminal no transitions`() {
        assertFalse(machine.canTransition(AttendanceState.SIGNED_OUT, AttendanceEvent.CONFIRM_SIGN_IN))
        assertFalse(machine.canTransition(AttendanceState.SIGNED_OUT, AttendanceEvent.CONFIRM_SIGN_OUT))
        assertFalse(machine.canTransition(AttendanceState.SIGNED_OUT, AttendanceEvent.RESTORE_DUE))
        assertFalse(machine.canTransition(AttendanceState.SIGNED_OUT, AttendanceEvent.DAY_ENDED))
        assertFalse(machine.canTransition(AttendanceState.SIGNED_OUT, AttendanceEvent.SKIP_TODAY))
    }

    // Valid transition: SIGNED_IN -> NOT_SIGNED_OUT (day ended without a sign-out)
    @Test
    fun `signed in closes as not signed out when day ends`() {
        assertTrue(machine.canTransition(AttendanceState.SIGNED_IN, AttendanceEvent.DAY_ENDED))
        assertEquals(
            AttendanceState.NOT_SIGNED_OUT,
            machine.transition(AttendanceState.SIGNED_IN, AttendanceEvent.DAY_ENDED)
        )
    }

    // Valid transition: SIGN_OUT_DUE -> NOT_SIGNED_OUT (reminder ignored until midnight)
    @Test
    fun `sign out due closes as not signed out when day ends`() {
        assertTrue(machine.canTransition(AttendanceState.SIGN_OUT_DUE, AttendanceEvent.DAY_ENDED))
        assertEquals(
            AttendanceState.NOT_SIGNED_OUT,
            machine.transition(AttendanceState.SIGN_OUT_DUE, AttendanceEvent.DAY_ENDED)
        )
    }

    // Invalid: NOT_SIGNED_IN -> DAY_ENDED (nothing was open to close)
    @Test
    fun `not signed in does not auto close at day end`() {
        assertFalse(machine.canTransition(AttendanceState.NOT_SIGNED_IN, AttendanceEvent.DAY_ENDED))
    }

    // Invalid: NOT_SIGNED_OUT -> anything (terminal state)
    @Test
    fun `not signed out is terminal no transitions`() {
        assertFalse(machine.canTransition(AttendanceState.NOT_SIGNED_OUT, AttendanceEvent.CONFIRM_SIGN_IN))
        assertFalse(machine.canTransition(AttendanceState.NOT_SIGNED_OUT, AttendanceEvent.CONFIRM_SIGN_OUT))
        assertFalse(machine.canTransition(AttendanceState.NOT_SIGNED_OUT, AttendanceEvent.RESTORE_DUE))
        assertFalse(machine.canTransition(AttendanceState.NOT_SIGNED_OUT, AttendanceEvent.DAY_ENDED))
        assertFalse(machine.canTransition(AttendanceState.NOT_SIGNED_OUT, AttendanceEvent.SKIP_TODAY))
    }

    // Invalid: SKIPPED -> anything (terminal state)
    @Test
    fun `skipped is terminal no transitions`() {
        assertFalse(machine.canTransition(AttendanceState.SKIPPED, AttendanceEvent.CONFIRM_SIGN_IN))
        assertFalse(machine.canTransition(AttendanceState.SKIPPED, AttendanceEvent.CONFIRM_SIGN_OUT))
        assertFalse(machine.canTransition(AttendanceState.SKIPPED, AttendanceEvent.RESTORE_DUE))
        assertFalse(machine.canTransition(AttendanceState.SKIPPED, AttendanceEvent.SKIP_TODAY))
    }

    // Full happy path transition chain
    @Test
    fun `full attendance lifecycle`() {
        // NOT_SIGNED_IN -> SIGNED_IN
        var state = machine.transition(AttendanceState.NOT_SIGNED_IN, AttendanceEvent.CONFIRM_SIGN_IN)
        assertEquals(AttendanceState.SIGNED_IN, state)

        // SIGNED_IN -> SIGN_OUT_DUE
        state = machine.transition(state, AttendanceEvent.RESTORE_DUE)
        assertEquals(AttendanceState.SIGN_OUT_DUE, state)

        // SIGN_OUT_DUE -> SIGNED_OUT
        state = machine.transition(state, AttendanceEvent.CONFIRM_SIGN_OUT)
        assertEquals(AttendanceState.SIGNED_OUT, state)

        // No more transitions
        assertFalse(machine.canTransition(state, AttendanceEvent.CONFIRM_SIGN_IN))
    }

    // Early sign-out path
    @Test
    fun `early sign out path`() {
        // NOT_SIGNED_IN -> SIGNED_IN -> SIGNED_OUT
        var state = machine.transition(AttendanceState.NOT_SIGNED_IN, AttendanceEvent.CONFIRM_SIGN_IN)
        assertEquals(AttendanceState.SIGNED_IN, state)

        state = machine.transition(state, AttendanceEvent.CONFIRM_SIGN_OUT)
        assertEquals(AttendanceState.SIGNED_OUT, state)
    }
}
