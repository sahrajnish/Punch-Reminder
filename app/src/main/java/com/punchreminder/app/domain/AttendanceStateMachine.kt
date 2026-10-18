package com.punchreminder.app.domain

/**
 * Enforces legal attendance state transitions.
 *
 * Valid transitions:
 *   NOT_SIGNED_IN  --(CONFIRM_SIGN_IN)--> SIGNED_IN
 *   SIGNED_IN      --(CONFIRM_SIGN_OUT)--> SIGNED_OUT   (early sign-out)
 *   SIGNED_IN      --(RESTORE_DUE)------> SIGN_OUT_DUE
 *   SIGNED_IN      --(DAY_ENDED)--------> NOT_SIGNED_OUT (forgot to sign out)
 *   SIGN_OUT_DUE   --(CONFIRM_SIGN_OUT)--> SIGNED_OUT
 *   SIGN_OUT_DUE   --(DAY_ENDED)--------> NOT_SIGNED_OUT (forgot to sign out)
 *   any            --(SKIP_TODAY)-------> SKIPPED
 *
 * Signed-out, not-signed-out and skipped sessions are terminal for the workday and cannot be
 * reopened, which prevents duplicate sessions/timers from being created.
 */
class AttendanceStateMachine {

    fun canTransition(current: AttendanceState, event: AttendanceEvent): Boolean {
        return try {
            transition(current, event)
            true
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    fun transition(current: AttendanceState, event: AttendanceEvent): AttendanceState {
        return when (current) {
            AttendanceState.NOT_SIGNED_IN -> when (event) {
                AttendanceEvent.CONFIRM_SIGN_IN -> AttendanceState.SIGNED_IN
                AttendanceEvent.SKIP_TODAY -> AttendanceState.SKIPPED
                else -> throw IllegalArgumentException(
                    "$event is not valid from ${AttendanceState.NOT_SIGNED_IN}")
            }

            AttendanceState.SIGNED_IN -> when (event) {
                AttendanceEvent.CONFIRM_SIGN_OUT -> AttendanceState.SIGNED_OUT
                AttendanceEvent.RESTORE_DUE -> AttendanceState.SIGN_OUT_DUE
                AttendanceEvent.DAY_ENDED -> AttendanceState.NOT_SIGNED_OUT
                AttendanceEvent.SKIP_TODAY -> AttendanceState.SKIPPED
                else -> throw IllegalArgumentException(
                    "$event is not valid from ${AttendanceState.SIGNED_IN}")
            }

            AttendanceState.SIGN_OUT_DUE -> when (event) {
                AttendanceEvent.CONFIRM_SIGN_OUT -> AttendanceState.SIGNED_OUT
                AttendanceEvent.DAY_ENDED -> AttendanceState.NOT_SIGNED_OUT
                AttendanceEvent.SKIP_TODAY -> AttendanceState.SKIPPED
                else -> throw IllegalArgumentException(
                    "$event is not valid from ${AttendanceState.SIGN_OUT_DUE}")
            }

            AttendanceState.SIGNED_OUT,
            AttendanceState.NOT_SIGNED_OUT ->
                throw IllegalArgumentException("No transitions are valid from $current")

            AttendanceState.SKIPPED ->
                throw IllegalArgumentException("No transitions are valid from ${AttendanceState.SKIPPED}")
        }
    }
}
