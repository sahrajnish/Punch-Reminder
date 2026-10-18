package com.punchreminder.app.ui.settings

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.punchreminder.app.data.AppSettings
import com.punchreminder.app.domain.Workdays
import com.punchreminder.app.ui.components.FieldLabel
import com.punchreminder.app.ui.components.PunchCard
import com.punchreminder.app.ui.components.ScreenHeader
import com.punchreminder.app.ui.components.SectionLabel
import com.punchreminder.app.ui.theme.LocalPunchAccentColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Width of the compact time dropdowns. */
private const val TIME_FIELD_WIDTH = 68

/** Width of the AM/PM dropdown. */
private const val AM_PM_FIELD_WIDTH = 68

/** Alpha applied to settings that are inactive because reminders are disabled. */
private const val DISABLED_ALPHA = 0.45f

private val HOUR_OPTIONS_12 = (1..12).toList()
private val HOUR_OPTIONS_24 = (0..23).toList()
private val MINUTE_OPTIONS = (0..59).toList()
private val INTERVAL_OPTIONS = (1..60).toList()

/** Formats a time component with a leading zero (for example "05"). */
private fun twoDigits(value: Int): String = value.toString().padStart(2, '0')

/** Keeps an unusual stored value selectable even when it is not in the standard list. */
private fun optionsWith(current: Int, options: List<Int>): List<Int> =
    if (current in options) options else (options + current).sorted()

/** Converts a 24-hour value (0-23) to a 12-hour display value (1-12). */
private fun to12Hour(hour24: Int): Int = if (hour24 % 12 == 0) 12 else hour24 % 12

/** Converts a 12-hour value (1-12) plus AM/PM flag back to a 24-hour value (0-23). */
private fun to24Hour(hour12: Int, isPm: Boolean): Int {
    val base = hour12 % 12
    return base + if (isPm) 12 else 0
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Wait for stored settings before creating the editable copy, otherwise the
    // first composition would write defaults over the user's saved values.
    if (!uiState.isLoaded) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    SettingsContent(
        settings = uiState.settings,
        viewModel = viewModel,
        scrollState = scrollState,
        modifier = modifier
    )
}

@Composable
private fun SettingsContent(
    settings: AppSettings,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState()
) {
    var workdays by remember { mutableIntStateOf(settings.workdays) }
    var signInHour12 by remember { mutableIntStateOf(to12Hour(settings.signInHour)) }
    var signInMinute by remember { mutableIntStateOf(settings.signInMinute) }
    var signInPm by remember { mutableStateOf(settings.signInHour >= 12) }
    var durationHours by remember { mutableIntStateOf(settings.sessionDurationHours) }
    var durationMinutes by remember { mutableIntStateOf(settings.sessionDurationMinutes) }
    var repeatMinutes by remember { mutableIntStateOf(settings.repeatIntervalMinutes) }
    var snoozeMinutes by remember { mutableIntStateOf(settings.snoozeMinutes) }
    var vibrate by remember { mutableStateOf(settings.vibrateEnabled) }
    var remindersEnabled by remember { mutableStateOf(settings.remindersEnabled) }
    var useCustomSound by remember { mutableStateOf(settings.useCustomSound) }
    var darkTheme by remember { mutableStateOf(settings.darkTheme) }

    val accents = LocalPunchAccentColors.current

    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    var soundTitle by remember { mutableStateOf<String?>(null) }

    // Opens the system ringtone picker, limited to alarm/clock tones.
    val pickTune = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            @Suppress("DEPRECATION")
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) viewModel.updateSoundUri(uri.toString())
        }
    }

    // Resolve a friendly title for the selected tone (off the main thread).
    LaunchedEffect(settings.soundUri) {
        val uri = settings.soundUri
        soundTitle = if (uri == null) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    RingtoneManager.getRingtone(context, Uri.parse(uri))?.getTitle(context)
                }.getOrNull()
            }
        }
    }

    val openTunePicker: () -> Unit = {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select reminder tune")
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            settings.soundUri?.let {
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(it))
            }
        }
        pickTune.launch(intent)
    }

    LaunchedEffect(workdays) { viewModel.updateWorkdays(workdays) }
    LaunchedEffect(signInHour12, signInMinute, signInPm) {
        viewModel.updateSignInTime(to24Hour(signInHour12, signInPm), signInMinute)
    }
    LaunchedEffect(durationHours, durationMinutes) {
        viewModel.updateSessionDuration(durationHours, durationMinutes)
    }
    LaunchedEffect(useCustomSound) { viewModel.setCustomSoundEnabled(useCustomSound) }
    LaunchedEffect(repeatMinutes) { viewModel.updateRepeatInterval(repeatMinutes) }
    LaunchedEffect(snoozeMinutes) { viewModel.updateSnooze(snoozeMinutes) }
    LaunchedEffect(vibrate) { viewModel.toggleVibrate(vibrate) }
    LaunchedEffect(remindersEnabled) { viewModel.toggleReminders(remindersEnabled) }
    LaunchedEffect(darkTheme) { viewModel.setDarkTheme(darkTheme) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenHeader(
            title = "Settings",
            subtitle = "Make reminders work for your routine"
        )

        // ----------------------------------------------------------------- THEME
        PunchCard {
            SectionLabel("THEME")
            Spacer(Modifier.height(14.dp))
            SwitchRow(
                icon = if (darkTheme) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                title = if (darkTheme) "Dark theme" else "Light theme",
                description = if (darkTheme) {
                    "Turn off to use the light color palette."
                } else {
                    "Turn on to use the dark color palette."
                },
                checked = darkTheme,
                onCheckedChange = { darkTheme = it }
            )
        }

        // ------------------------------------------------------------- REMINDERS
        PunchCard {
            SectionLabel("REMINDERS")
            Spacer(Modifier.height(14.dp))
            SwitchRow(
                icon = Icons.AutoMirrored.Filled.Notes,
                title = "Enable reminders",
                description = "Turn all attendance reminders on or off.",
                checked = remindersEnabled,
                onCheckedChange = { remindersEnabled = it }
            )
            if (!remindersEnabled) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Reminders are off. The settings below keep their saved values but " +
                        "stay disabled until you turn reminders back on.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(20.dp))
            SwitchRow(
                icon = Icons.Filled.Vibration,
                title = "Vibrate on reminders",
                description = "Vibrate when a reminder fires.",
                checked = vibrate,
                onCheckedChange = { vibrate = it },
                enabled = remindersEnabled
            )
            Spacer(Modifier.height(20.dp))
            SwitchRow(
                icon = Icons.Filled.MusicNote,
                title = "Custom reminder tune",
                description = "Off uses the system default reminder sound.",
                checked = useCustomSound,
                onCheckedChange = { useCustomSound = it },
                enabled = remindersEnabled
            )
            if (useCustomSound) {
                Spacer(Modifier.height(12.dp))
                TuneRow(
                    title = soundTitle ?: "System default tune",
                    onClick = openTunePicker,
                    enabled = remindersEnabled
                )
            }
        }

        // -------------------------------------------------------------- WORKDAYS
        PunchCard(
            modifier = Modifier.alpha(if (remindersEnabled) 1f else DISABLED_ALPHA)
        ) {
            SectionLabel("WORKDAYS")
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val days = listOf(
                    "M" to Workdays.MONDAY,
                    "T" to Workdays.TUESDAY,
                    "W" to Workdays.WEDNESDAY,
                    "T" to Workdays.THURSDAY,
                    "F" to Workdays.FRIDAY,
                    "S" to Workdays.SATURDAY,
                    "S" to Workdays.SUNDAY
                )
                days.forEach { (label, bit) ->
                    DayChip(
                        label = label,
                        selected = (workdays and bit) != 0,
                        enabled = remindersEnabled,
                        onClick = {
                            workdays = if ((workdays and bit) != 0) {
                                workdays and bit.inv()
                            } else {
                                workdays or bit
                            }
                        }
                    )
                }
            }
        }

        // -------------------------------------------------------------- SCHEDULE
        PunchCard(
            modifier = Modifier.alpha(if (remindersEnabled) 1f else DISABLED_ALPHA)
        ) {
            SectionLabel("SCHEDULE")
            Spacer(Modifier.height(14.dp))

            // Sign-in reminder: label on the left, 12-hour time + AM/PM on the right.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FieldLabel(
                    icon = Icons.Filled.Schedule,
                    text = "Sign-in reminder",
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DropdownField(
                        value = signInHour12,
                        options = HOUR_OPTIONS_12,
                        onSelect = { signInHour12 = it },
                        modifier = Modifier.width(TIME_FIELD_WIDTH.dp),
                        enabled = remindersEnabled
                    )
                    Text(
                        text = ":",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    DropdownField(
                        value = signInMinute,
                        options = MINUTE_OPTIONS,
                        onSelect = { signInMinute = it },
                        optionLabel = ::twoDigits,
                        modifier = Modifier.width(TIME_FIELD_WIDTH.dp),
                        enabled = remindersEnabled
                    )
                    Spacer(Modifier.width(8.dp))
                    DropdownField(
                        value = if (signInPm) "PM" else "AM",
                        options = listOf("AM", "PM"),
                        onSelect = { signInPm = it == "PM" },
                        optionLabel = { it },
                        modifier = Modifier.width(AM_PM_FIELD_WIDTH.dp),
                        enabled = remindersEnabled
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Work-session duration: label on the left, hours + minutes on the right.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FieldLabel(
                    icon = Icons.Filled.CalendarToday,
                    text = "Work-session duration",
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DropdownField(
                        value = durationHours,
                        options = HOUR_OPTIONS_24,
                        onSelect = { durationHours = it },
                        modifier = Modifier.width(TIME_FIELD_WIDTH.dp),
                        enabled = remindersEnabled
                    )
                    Spacer(Modifier.width(6.dp))
                    UnitLabel("hrs")
                    Spacer(Modifier.width(8.dp))
                    DropdownField(
                        value = durationMinutes,
                        options = MINUTE_OPTIONS,
                        onSelect = { durationMinutes = it },
                        optionLabel = ::twoDigits,
                        modifier = Modifier.width(TIME_FIELD_WIDTH.dp),
                        enabled = remindersEnabled
                    )
                    Spacer(Modifier.width(6.dp))
                    UnitLabel("min")
                }
            }
        }

        // ------------------------------------------------------------ PERMISSIONS
        val hasNotificationPermission = remember {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        val hasExactAlarmPermission = remember {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                am.canScheduleExactAlarms()
            } else true
        }

        // Permission prompts only matter while reminders are enabled.
        if (remindersEnabled && (!hasNotificationPermission || !hasExactAlarmPermission)) {
            PunchCard {
                SectionLabel("PERMISSIONS")
                Spacer(Modifier.height(14.dp))

                if (!hasNotificationPermission) {
                    PermissionRow(
                        icon = Icons.Filled.NotificationsNone,
                        title = "Notification permission",
                        description = "Reminders need notification permission. Open system settings to enable.",
                        status = "Blocked",
                        statusColor = MaterialTheme.colorScheme.error,
                        onClick = {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            context.startActivity(intent)
                        }
                    )
                }

                if (!hasNotificationPermission && !hasExactAlarmPermission) {
                    Spacer(Modifier.height(12.dp))
                }

                if (!hasExactAlarmPermission) {
                    PermissionRow(
                        icon = Icons.Filled.Security,
                        title = "Exact alarm permission",
                        description = "Exact alarms ensure reminders arrive on time, even in Doze mode.",
                        status = "Required",
                        statusColor = accents.warning,
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                    )
                }

                Spacer(Modifier.height(4.dp))
            }
        }

        // ------------------------------------------------------------ REPETITION
        PunchCard(
            modifier = Modifier.alpha(if (remindersEnabled) 1f else DISABLED_ALPHA)
        ) {
            SectionLabel("REPETITION")
            Spacer(Modifier.height(10.dp))
            Text(
                text = "An ignored reminder repeats after the repeat interval. Snooze postpones " +
                    "a reminder when you tap the Snooze action.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            // Repeat interval: label on the left, minutes box on the right.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FieldLabel(
                    icon = Icons.Filled.NotificationsNone,
                    text = "Repeat interval",
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DropdownField(
                        value = repeatMinutes,
                        options = optionsWith(repeatMinutes, INTERVAL_OPTIONS),
                        onSelect = { repeatMinutes = it },
                        modifier = Modifier.width(TIME_FIELD_WIDTH.dp),
                        enabled = remindersEnabled
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "minutes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Snooze duration: label on the left, minutes box on the right.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FieldLabel(
                    icon = Icons.Filled.Snooze,
                    text = "Snooze duration",
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DropdownField(
                        value = snoozeMinutes,
                        options = optionsWith(snoozeMinutes, INTERVAL_OPTIONS),
                        onSelect = { snoozeMinutes = it },
                        modifier = Modifier.width(TIME_FIELD_WIDTH.dp),
                        enabled = remindersEnabled
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "minutes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ---------------------------------------------------------------- CREDIT
        Text(
            text = "Made with care by Rajnish",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 8.dp)
        )
    }
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else DISABLED_ALPHA),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.secondary,
                checkedBorderColor = MaterialTheme.colorScheme.secondary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = Color.Transparent,
                uncheckedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        )
    }
}

/** A tappable row that shows the currently selected reminder tune. */
@Composable
private fun TuneRow(title: String, onClick: () -> Unit, enabled: Boolean = true) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clip(MaterialTheme.shapes.small)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Reminder tune",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun DayChip(
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val accents = LocalPunchAccentColors.current
    Box(
        modifier = Modifier
            .size(40.dp)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clip(CircleShape)
            .background(
                if (selected) MaterialTheme.colorScheme.primary else accents.chipUnselected
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                accents.onChipUnselected
            },
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * A compact selector. Tapping the field opens a dropdown of [options]; the current value is
 * always shown even when it is outside the standard list.
 */
@Composable
private fun <T> DropdownField(
    value: T,
    options: List<T>,
    onSelect: (T) -> Unit,
    optionLabel: (T) -> String = { it.toString() },
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    var anchorWidthPx by remember { mutableIntStateOf(0) }
    val anchorWidth = with(LocalDensity.current) { anchorWidthPx.toDp() }

    // Never leave the menu open when the control becomes disabled.
    LaunchedEffect(enabled) {
        if (!enabled) expanded = false
    }

    Box(modifier) {
        Row(
            modifier = Modifier
                .onSizeChanged { anchorWidthPx = it.width }
                .fillMaxWidth()
                .height(56.dp)
                .clip(MaterialTheme.shapes.small)
                .border(
                    width = 1.dp,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.outline
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    },
                    shape = MaterialTheme.shapes.small
                )
                .clickable(
                    enabled = enabled,
                    role = Role.DropdownList,
                    onClick = { expanded = true }
                )
                .padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = optionLabel(value),
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(anchorWidth)
                .heightIn(max = 280.dp)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

/** Compact unit caption shown next to a numeric box (for example "hrs"). */
@Composable
private fun UnitLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** A tappable row that shows a permission status and opens system settings on tap. */
@Composable
private fun PermissionRow(
    icon: ImageVector,
    title: String,
    description: String,
    status: String,
    statusColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall,
            color = statusColor,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

