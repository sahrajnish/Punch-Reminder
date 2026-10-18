package com.punchreminder.app.service

import android.annotation.SuppressLint
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.punchreminder.app.DiContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Notification listener that intercepts GreytHR attendance notifications.
 *
 * When GreytHR sends a notification confirming a successful sign-in or sign-out,
 * this listener parses the notification text and auto-updates the attendance state
 * in Punch Reminder.
 *
 * The user must grant notification access in system settings:
 *   Settings > Apps > Special app access > Notification access
 */
@SuppressLint("OverrideAbstract")
class GreytHrNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "GreytHR-Notif"
        private const val GREYTHR_PACKAGE = "com.greytip.ghress"

        /** Notification text patterns that indicate a successful sign-in. */
        private val SIGN_IN_PATTERNS = listOf(
            "check.in", "check-in", "checkin", "signed in", "signed_in",
            "clock.in", "clock-in", "clockin", "punch.in", "punch-in",
            "attendance.marked", "attendance marked", "marked attendance",
            "check in successful", "punch successful",
            "attendance recorded", "attendance submitted",
            "marked successfully", "sign in successful"
        )

        /** Notification text patterns that indicate a successful sign-out. */
        private val SIGN_OUT_PATTERNS = listOf(
            "check.out", "check-out", "checkout", "signed out", "signed_out",
            "clock.out", "clock-out", "clockout", "punch.out", "punch-out",
            "sign.out", "sign-out", "signout",
            "check out successful", "sign out successful",
            "attendance completed", "shift ended", "work complete"
        )

        /** Keywords for notification category/tag that suggest attendance-related content. */
        private val ATTENDANCE_TAGS = listOf(
            "attendance", "check", "punch", "sign", "clock", "shift"
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != GREYTHR_PACKAGE) return

        val notification = sbn.notification
        val title = notification.extras?.getString("android.title")?.lowercase() ?: ""
        val text = notification.extras?.getString("android.text")?.lowercase() ?: ""
        val tickerText = notification.tickerText?.toString()?.lowercase() ?: ""

        Log.d(TAG, "GreytHR notification: title='$title' text='$text'")

        val fullText = "$title $text $tickerText"

        // Check if this notification relates to attendance
        if (!fullText.containsAny(ATTENDANCE_TAGS)) return

        if (fullText.containsAny(SIGN_IN_PATTERNS)) {
            Log.i(TAG, "Auto-detected sign-in from GreytHR notification")
            scope.launch {
                DiContainer.attendanceRepository.confirmSignIn()
            }
        } else if (fullText.containsAny(SIGN_OUT_PATTERNS)) {
            Log.i(TAG, "Auto-detected sign-out from GreytHR notification")
            scope.launch {
                DiContainer.attendanceRepository.confirmSignOut()
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Not needed
    }

    override fun onListenerConnected() {
        Log.i(TAG, "Notification listener connected - monitoring $GREYTHR_PACKAGE")
    }

    private fun String.containsAny(patterns: List<String>): Boolean =
        patterns.any { this.contains(it, ignoreCase = true) }
}