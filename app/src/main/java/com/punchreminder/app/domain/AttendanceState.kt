package com.punchreminder.app.domain

/**
 * The attendance session state machine.
 *
 * States model the lifecycle of a single workday. The cycle is bounded by the local calendar
 * day (00:00 to 23:59:59.999): a session that is still open when the day ends is closed
 * automatically as [NOT_SIGNED_OUT] so the next day always starts fresh.
 *
 * NOT_SIGNED_IN   -> no sign-in confirmation recorded today
 * SIGNED_IN       -> sign-in confirmed; sign-out reminder pending
 * SIGN_OUT_DUE    -> work-session duration elapsed; sign-out reminder active
 * SIGNED_OUT      -> sign-out confirmed; session complete
 * NOT_SIGNED_OUT  -> day ended while still signed in; closed automatically (terminal)
 * SKIPPED         -> user explicitly skipped reminders for today
 *
 * This app is a reminder utility. These states describe locally recorded
 * confirmations only; they are never official greytHR attendance records.
 */
enum class AttendanceState {
    NOT_SIGNED_IN,
    SIGNED_IN,
    SIGN_OUT_DUE,
    SIGNED_OUT,
    NOT_SIGNED_OUT,
    SKIPPED;
}
