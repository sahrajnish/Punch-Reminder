package com.punchreminder.app.domain

/**
 * Events that can move a session between states.
 *
 * Note: snoozing is intentionally NOT a state transition -- a snooze only moves
 * the next reminder time and never changes the session deadline or sign-in timestamp.
 */
enum class AttendanceEvent {
    CONFIRM_SIGN_IN,
    CONFIRM_SIGN_OUT,
    RESTORE_DUE,   // session duration has elapsed; SIGNED_IN -> SIGN_OUT_DUE
    DAY_ENDED,     // local day ended before a sign-out was confirmed; auto-closes the session
    SKIP_TODAY,    // user skipped today
    SNOOZE,        // handled outside the state machine (no state change)
}
