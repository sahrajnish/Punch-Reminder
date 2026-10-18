package com.punchreminder.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.punchreminder.app.DiContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Accessibility service that monitors the GreytHR app for attendance sign-in/sign-out
 * confirmation screens.
 *
 * When the user completes a sign-in or sign-out flow inside GreytHR, the app typically
 * shows a "success" toast or confirmation screen. This service detects those patterns
 * and automatically updates Punch Reminder's attendance state.
 *
 * The user must enable this service in system Settings > Accessibility > Punch Reminder.
 */
class GreytHrAttendanceMonitorService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "GreytHR-Monitor"
        private const val GREYTHR_PACKAGE = "com.greytip.ghress"

        /** Keywords that indicate a successful sign-in event. */
        private val SIGN_IN_KEYWORDS = listOf(
            "check.in", "check-in", "checkin", "signed in", "signed_in",
            "clock.in", "clock-in", "clockin", "punch.in", "punch-in",
            "attendance.marked", "attendance marked", "marked attendance",
            "successfully", "check.in successful", "punch.success"
        )

        /** Keywords that indicate a successful sign-out event. */
        private val SIGN_OUT_KEYWORDS = listOf(
            "check.out", "check-out", "checkout", "signed out", "signed_out",
            "clock.out", "clock-out", "clockout", "punch.out", "punch-out",
            "sign.out", "sign-out", "signout"
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                AccessibilityEvent.TYPE_VIEW_CLICKED

            // Only monitor the GreytHR package
            packageNames = arrayOf(GREYTHR_PACKAGE)

            // We need to read screen content to detect success messages
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS

            notificationTimeout = 100
        }
        this.serviceInfo = info
        Log.i(TAG, "Accessibility service connected, monitoring $GREYTHR_PACKAGE")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.packageName != GREYTHR_PACKAGE) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                // A new screen opened in GreytHR - check for confirmation dialogs
                val source = event.source ?: return
                val root = rootInActiveWindow ?: return
                try {
                    if (containsSignInConfirmation(root)) {
                        scope.launch {
                            DiContainer.attendanceRepository.confirmSignIn()
                        }
                    } else if (containsSignOutConfirmation(root)) {
                        scope.launch {
                            DiContainer.attendanceRepository.confirmSignOut()
                        }
                    }
                } finally {
                    root.recycle()
                    source.recycle()
                }
            }

            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // Content updated - check for toast/snackbar success messages
                val root = rootInActiveWindow ?: return
                try {
                    checkForSuccessText(root)
                } finally {
                    root.recycle()
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.i(TAG, "Accessibility service interrupted")
    }

    private fun containsSignInConfirmation(node: AccessibilityNodeInfo): Boolean {
        val text = node.text?.toString()?.lowercase() ?: ""
        val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""

        if (text.containsAny(SIGN_IN_KEYWORDS) || contentDesc.containsAny(SIGN_IN_KEYWORDS)) {
            Log.i(TAG, "Detected sign-in confirmation: '$text' '$contentDesc'")
            return true
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try {
                if (containsSignInConfirmation(child)) return true
            } finally {
                child.recycle()
            }
        }
        return false
    }

    private fun containsSignOutConfirmation(node: AccessibilityNodeInfo): Boolean {
        val text = node.text?.toString()?.lowercase() ?: ""
        val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""

        if (text.containsAny(SIGN_OUT_KEYWORDS) || contentDesc.containsAny(SIGN_OUT_KEYWORDS)) {
            Log.i(TAG, "Detected sign-out confirmation: '$text' '$contentDesc'")
            return true
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try {
                if (containsSignOutConfirmation(child)) return true
            } finally {
                child.recycle()
            }
        }
        return false
    }

    private fun checkForSuccessText(node: AccessibilityNodeInfo) {
        val text = node.text?.toString()?.lowercase() ?: ""
        val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""

        // Check for toast/snackbar messages like "Attendance marked successfully"
        if (text.containsAny(SIGN_IN_KEYWORDS) || contentDesc.containsAny(SIGN_IN_KEYWORDS)) {
            Log.i(TAG, "Detected sign-in success toast: '$text'")
            scope.launch {
                DiContainer.attendanceRepository.confirmSignIn()
            }
        }
        if (text.containsAny(SIGN_OUT_KEYWORDS) || contentDesc.containsAny(SIGN_OUT_KEYWORDS)) {
            Log.i(TAG, "Detected sign-out success toast: '$text'")
            scope.launch {
                DiContainer.attendanceRepository.confirmSignOut()
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try {
                checkForSuccessText(child)
            } finally {
                child.recycle()
            }
        }
    }

    private fun String.containsAny(keywords: List<String>): Boolean =
        keywords.any { this.contains(it, ignoreCase = true) }
}