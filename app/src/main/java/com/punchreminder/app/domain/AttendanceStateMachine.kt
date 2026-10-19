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
 *   SKIPPED        --(RESUME_TODAY)-----> NOT_SIGNED_IN
 *
 * Signed-out and not-signed-out sessions are terminal for the workday and cannot be reopened.
 * A skipped session can be resumed before the day ends.
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

            AttendanceState.SKIPPED -> when (event) {
                AttendanceEvent.RESUME_TODAY -> AttendanceState.NOT_SIGNED_IN
                else -> throw IllegalArgumentException(
                    "$event is not valid from ${AttendanceState.SKIPPED}")
            }
        }
    }
}
