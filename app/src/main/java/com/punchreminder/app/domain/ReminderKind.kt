package com.punchreminder.app.domain

/** The kind of a pending reminder alarm. Used to keep exactly one pending alarm per kind. */
enum class ReminderKind {
    SIGN_IN,
    SIGN_OUT;
}
