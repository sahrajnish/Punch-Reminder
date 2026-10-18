package com.punchreminder.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.punchreminder.app.R

private const val CHANNEL_SIGN_IN = "punch_reminder.sign_in"
private const val CHANNEL_SIGN_OUT = "punch_reminder.sign_out"
private const val GROUP_KEY = "punch_reminder.group"

internal const val NOTIFICATION_ID_SIGN_IN = 1001
internal const val NOTIFICATION_ID_SIGN_OUT = 1002

/**
 * Builds notification channels and actionable attendance reminders.
 *
 * Fixed notification IDs are used so showing a notification replaces any previous one of the same
 * kind -- no duplicate notifications are ever created.
 *
 * The reminder sound follows the user's settings. Because Android (O+) locks a channel's sound
 * after it is created, each distinct sound gets its own channel id.
 */
class NotificationHelper(private val context: Context) {

    private val manager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    /** The active reminder tone, or null to use the system default notification sound. */
    private var soundUri: Uri? = null

    /** Whether notifications should vibrate. Mirrors the user's settings. */
    private var vibrateEnabled: Boolean = true

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        ensureChannel(signInChannelId(), R.string.notif_channel_sign_in, R.string.notif_channel_sign_in_desc)
        ensureChannel(signOutChannelId(), R.string.notif_channel_sign_out, R.string.notif_channel_sign_out_desc)
    }

    /**
     * Updates the reminder tone. Passing null restores the system default. New channels are created
     * as needed so the change takes effect on Android O+.
     */
    fun setReminderSound(uri: Uri?) {
        soundUri = uri
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ensureChannel(signInChannelId(), R.string.notif_channel_sign_in, R.string.notif_channel_sign_in_desc)
            ensureChannel(signOutChannelId(), R.string.notif_channel_sign_out, R.string.notif_channel_sign_out_desc)
        }
    }

    /**
     * Applies the user's vibration preference. Android O+ locks a channel's vibration setting after
     * creation, so a separate channel id is used when vibration is disabled.
     */
    fun setVibrationEnabled(enabled: Boolean) {
        if (vibrateEnabled == enabled) return
        vibrateEnabled = enabled
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ensureChannel(signInChannelId(), R.string.notif_channel_sign_in, R.string.notif_channel_sign_in_desc)
            ensureChannel(signOutChannelId(), R.string.notif_channel_sign_out, R.string.notif_channel_sign_out_desc)
        }
    }

    private fun signInChannelId(): String = channelIdFor(CHANNEL_SIGN_IN)
    private fun signOutChannelId(): String = channelIdFor(CHANNEL_SIGN_OUT)

    /**
     * Distinct channel id per sound and vibration state so both preferences are honoured on
     * Android O+, where channel settings are immutable after creation.
     */
    private fun channelIdFor(base: String): String {
        val soundId = soundUri?.let { "$base.${it.toString().hashCode()}" } ?: base
        return if (vibrateEnabled) soundId else "$soundId.novibe"
    }

    /** The sound to use, falling back to the system default notification tone. */
    private fun resolvedSoundUri(): Uri? =
        soundUri ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    /** Create the channel if it does not already exist (no-op on pre-O). */
    private fun ensureChannel(id: String, nameRes: Int, descRes: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (manager.getNotificationChannel(id) != null) return

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()
        val channel = NotificationChannel(
            id,
            context.getString(nameRes),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(descRes)
            setSound(resolvedSoundUri(), attrs)
            enableLights(true)
            enableVibration(vibrateEnabled)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    fun cancelSignIn() = manager.cancel(NOTIFICATION_ID_SIGN_IN)
    fun cancelSignOut() = manager.cancel(NOTIFICATION_ID_SIGN_OUT)
    fun cancelAll() {
        cancelSignIn()
        cancelSignOut()
    }

    /** Show the sign-in reminder notification. */
    fun showSignIn(
        openIntent: PendingIntent,
        confirmIntent: PendingIntent,
        snoozeIntent: PendingIntent,
        snoozeMinutes: Int
    ) {
        val notification = buildNotification(
            id = NOTIFICATION_ID_SIGN_IN,
            channel = signInChannelId(),
            title = context.getString(R.string.notif_title_sign_in),
            body = context.getString(R.string.notif_body_sign_in),
            contentIntent = openIntent,
            actions = listOf(
                action(context.getString(R.string.notif_action_open), OPEN_GREYTHR, openIntent),
                action(
                    context.getString(R.string.notif_action_confirm_sign_in),
                    CONFIRM_SIGN_IN,
                    confirmIntent
                ),
                action(snoozeActionLabel(snoozeMinutes), SNOOZE_SIGN_IN, snoozeIntent)
            )
        )
        manager.notify(NOTIFICATION_ID_SIGN_IN, notification)
    }

    /** Show the sign-out reminder notification with the session duration info. */
    fun showSignOut(
        openIntent: PendingIntent,
        confirmIntent: PendingIntent,
        snoozeIntent: PendingIntent,
        sessionDurationText: String? = null,
        snoozeMinutes: Int
    ) {
        val body = if (sessionDurationText != null) {
            context.getString(R.string.notif_body_sign_out_dynamic, sessionDurationText)
        } else {
            context.getString(R.string.notif_body_sign_out)
        }
        val notification = buildNotification(
            id = NOTIFICATION_ID_SIGN_OUT,
            channel = signOutChannelId(),
            title = context.getString(R.string.notif_title_sign_out),
            body = body,
            contentIntent = openIntent,
            actions = listOf(
                action(context.getString(R.string.notif_action_open), OPEN_GREYTHR, openIntent),
                action(
                    context.getString(R.string.notif_action_confirm_sign_out),
                    CONFIRM_SIGN_OUT,
                    confirmIntent
                ),
                action(snoozeActionLabel(snoozeMinutes), SNOOZE_SIGN_OUT, snoozeIntent)
            )
        )
        manager.notify(NOTIFICATION_ID_SIGN_OUT, notification)
    }

    /** "Snooze 10 minutes" / "Snooze 1 minute", matching the configured duration. */
    private fun snoozeActionLabel(minutes: Int): String =
        context.resources.getQuantityString(
            R.plurals.notif_action_snooze_duration,
            minutes,
            minutes
        )

    private fun buildNotification(
        id: Int,
        channel: String,
        title: String,
        body: String,
        contentIntent: PendingIntent,
        actions: List<NotificationCompat.Action>
    ): Notification {
        return NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setGroup(GROUP_KEY)
            .setGroupSummary(false)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(contentIntent)
            // Repeating reminders must re-alert every time the same kind fires again.
            .setOnlyAlertOnce(false)
            .setSound(resolvedSoundUri())
            .setVibrate(
                if (vibrateEnabled) longArrayOf(0, 250, 250, 250) else longArrayOf(0)
            )
            .apply { actions.forEach { addAction(it) } }
            .build()
    }

    private fun action(
        title: CharSequence,
        action: String,
        pendingIntent: PendingIntent
    ): NotificationCompat.Action {
        return NotificationCompat.Action.Builder(
            R.drawable.ic_notification,
            title,
            pendingIntent
        ).build()
    }

    companion object {
        const val OPEN_GREYTHR = "com.punchreminder.app.action.OPEN_GREYTHR"
        const val CONFIRM_SIGN_IN = "com.punchreminder.app.action.CONFIRM_SIGN_IN"
        const val CONFIRM_SIGN_OUT = "com.punchreminder.app.action.CONFIRM_SIGN_OUT"
        const val SNOOZE_SIGN_IN = "com.punchreminder.app.action.SNOOZE_SIGN_IN"
        const val SNOOZE_SIGN_OUT = "com.punchreminder.app.action.SNOOZE_SIGN_OUT"

        fun broadcastIntent(context: Context, action: String): PendingIntent {
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                setAction(action)
            }
            return PendingIntent.getBroadcast(
                context,
                requestCodeFor(action),
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        private fun requestCodeFor(action: String): Int = when (action) {
            CONFIRM_SIGN_IN -> 1001
            CONFIRM_SIGN_OUT -> 1002
            SNOOZE_SIGN_IN -> 1003
            SNOOZE_SIGN_OUT -> 1004
            OPEN_GREYTHR -> 1005
            else -> 0
        }
    }
}
