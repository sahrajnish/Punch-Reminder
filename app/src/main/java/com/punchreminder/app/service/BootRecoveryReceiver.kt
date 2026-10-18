package com.punchreminder.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.punchreminder.app.DiContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Rebuilds pending reminders after a device restart, time change, or time-zone change so an
 * active session is never lost when the process is gone.
 */
class BootRecoveryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                DiContainer.attendanceRepository.rescheduleAll()
            } finally {
                result.finish()
            }
        }
    }
}
