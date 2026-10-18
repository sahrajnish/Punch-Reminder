# Changelog & Implementation Report

## Date: 2026-10-18

## REM Investigation Summary

### Device: OnePlus CPH2491 (OPPO/OnePlus)
### Android Version: 16 (API 36)
### Native Clock App: `com.oneplus.deskclock`

### Verified Clock App Behaviors
| Behavior | Confirmed |
|----------|-----------|
| Uses `AlarmManager.setAlarmClock()` with `AlarmClockInfo` | ✅ (via ADB alarm dump) |
| Uses RTC_WAKEUP trigger type | ✅ |
| Schedules 2 alarms per event (`change_state` + `indicator`) | ✅ |
| Has `AlarmStateManager` broadcast receiver | ✅ |
| Uses `SET_ALARM` intent for system integration | ✅ |
| Uses `SNOOZE_ALARM` / `DISMISS_ALARM` intents | ✅ |
| Uses `BOOT_COMPLETED` receiver for reboot recovery | ✅ |
| Has `USE_EXACT_ALARM` permission granted | ✅ |
| Has `POST_NOTIFICATIONS` permission | ✅ |
| Notification channel with `IMPORTANCE_HIGH` | ✅ |

### Key Architecture Patterns (Already Present in Existing App)
- `AlarmManager.setAlarmClock()` for exact, Doze-resistant scheduling ✅
- `RTC_WAKEUP` trigger type ✅
- Boot/Time-Change recovery via `BootRecoveryReceiver` ✅
- `FLAG_IMMUTABLE` on `PendingIntent` ✅
- Notification channels with `IMPORTANCE_HIGH` ✅
- `lockscreenVisibility = VISIBILITY_PUBLIC` ✅
- Sound + vibration on notification ✅
- `goAsync()` in broadcast receivers ✅

---

## Changes Made

### 1. SettingsViewModel.kt
**File:** `app/src/main/java/com/punchreminder/app/ui/settings/SettingsViewModel.kt`
**Change:** Added `reschedule = true` to `updateWorkdays()`
**Reason:** Previously, changing workdays from the Settings screen did not immediately reschedule today's reminders. If a user removed today as a workday while the app was open, the pending alarm would remain. Now it triggers `rescheduleAll()` to cancel/update the current day's schedule.

### 2. SettingsScreen.kt
**File:** `app/src/main/java/com/punchreminder/app/ui/settings/SettingsScreen.kt`
**Changes:**
- Added permission status cards that appear when notification or exact-alarm permissions are missing
- Added `PermissionRow` composable for tappable permission items
- Added icons: `Security` (for exact alarm section)
- Added imports: `NotificationManagerCompat`, `AlarmManager`, `Settings`, `Manifest`

### 3. NotificationHelper.kt
**File:** `app/src/main/java/com/punchreminder/app/service/NotificationHelper.kt`
**Change:** Updated `showSignOut()` to accept optional `sessionDurationText` parameter
**Reason:** The notification body previously hardcoded "nine-hour" in the sign-out message. Now it uses the user's configured session duration dynamically.
- Removed unused `ReminderKind` import

### 4. ReminderReceiver.kt
**File:** `app/src/main/java/com/punchreminder/app/service/ReminderReceiver.kt`
**Change:** Passes the configured session duration to `showSignOut()` notification
- Added `buildDurationText()` helper function

### 5. strings.xml
**File:** `app/src/main/res/values/strings.xml`
**Change:** 
- Updated `notif_body_sign_out` to remove hardcoded "nine-hour"
- Added `notif_body_sign_out_dynamic` with `%1$s` format placeholder

### 6. AttendanceRepository.kt
**File:** `app/src/main/java/com/punchreminder/app/data/AttendanceRepository.kt`
**Change:** Updated `confirmSignIn()` to handle the edge case where the calculated sign-out time is already in the past (e.g., signing in at 11 PM with a 9-hour session). Instead of scheduling a past alarm, it schedules one 5 seconds in the future to trigger the `SIGN_OUT_DUE` transition promptly.

---

## Existing Architecture Verification

### What Already Worked (No Changes Needed)

| Requirement | Implementation | Verified |
|-------------|---------------|----------|
| Configurable sign-in reminder time (default 10:30 AM) | `AppSettings.signInHour/Minute` | ✅ |
| Workday selection (M-F default) | `Workdays.kt` bitmask + UI chips | ✅ |
| Exact-time alarm via `AlarmManager` | `setAlarmClock()` in `DefaultReminderScheduler` | ✅ |
| Sign-in timestamp recording | `AttendanceRepository.confirmSignIn()` | ✅ |
| Dynamic sign-out calculation | `confirmedSignInAt + sessionDuration` | ✅ |
| Late sign-in = later sign-out | Calculated from actual timestamp | ✅ |
| Snooze doesn't corrupt recurring schedule | Separate one-time snooze alarm | ✅ |
| Dismiss doesn't disable future reminders | State machine unaffected by snooze/dismiss | ✅ |
| Boot recovery | `BootRecoveryReceiver` (`BOOT_COMPLETED`) | ✅ |
| Time zone/clock change recovery | `BootRecoveryReceiver` (`TIME_SET`, `TIMEZONE_CHANGED`) | ✅ |
| Daily safety net | `DailyRefreshReceiver` at 00:05 | ✅ |
| GreytHR app launch | `DefaultGreytHrLauncher` (installed app / Play Store) | ✅ |
| Attendance state machine | `AttendanceStateMachine.kt` | ✅ |
| Room database persistence | `AttendanceDatabase` with `AttendanceSessionEntity` | ✅ |
| Unit tests (32 tests) | `AttendanceStateMachineTest` + `NextReminderCalculatorTest` | ✅ |
| Pure scheduling logic (testable) | `NextReminderCalculator.compute()` | ✅ |
| No continuous foreground service | Purely alarm-driven | ✅ |

### Device Verification Results

| Test | Result | Notes |
|------|--------|-------|
| App installs and launches | ✅ | Installed via ADB |
| Notification channels created | ✅ | `punch_reminder.sign_in` and `punch_reminder.sign_out` (both IMPORTANCE_HIGH) |
| Daily refresh alarm scheduled | ✅ | `DailyRefreshReceiver` at 00:05 |
| `USE_EXACT_ALARM` permission granted | ✅ | Auto-granted on API 34+ |
| `RECEIVE_BOOT_COMPLETED` permission granted | ✅ | |
| `POST_NOTIFICATIONS` permission | ⚠️ | Denied on initial install (OPPO/OnePlus additional notification control) |
| App can send notification | ⚠️ | Blocked by OPPO's `POST_NOTIFICATION` app op |
| Exact alarm scheduling | ✅ | `USE_EXACT_ALARM` auto-granted on API 36 |

### Known Limitations

1. **OPPO/OnePlus Notification Control**: This device has an additional manufacturer-specific notification permission layer (`POST_NOTIFICATION` app op). Users must manually enable notifications in the system settings. The app now shows a permission card directing users there.

2. **`setAlarmClock()` Behavior**: The app uses `AlarmManager.setAlarmClock()` which is exact and Doze-resistant. This is the same mechanism used by the native Clock app. However, some Chinese OEM ROMs may impose additional restrictions on non-system apps for alarm scheduling.

3. **No GreytHR API Integration**: The app relies on user-confirmed sign-in/sign-out timestamps. It cannot automatically detect a completed GreytHR attendance event because no public API exists.

4. **No Java JDK Available**: The development environment lacks a Java JDK, so the project cannot be compiled from the CLI. Install OpenJDK 17+ and use `./gradlew assembleDebug` to build.

---

## Unit Test Summary

The existing 32 unit tests cover:
- All valid/invalid state machine transitions (10 tests)
- Sign-in reminder scheduling on workdays/non-workdays (6 tests)
- Dynamic sign-out calculation (5 tests)
- Time zone preservation (1 test)
- Permission-independent scheduling (2 tests)
- Repeat anchor preservation (2 tests)
- Snooze behavior (1 test)
- Repeated calls don't duplicate (1 test)
- Custom workday configuration (1 test)
- Full lifecycle and edge cases (3 tests)

All tests are pure JVM tests (no Android dependencies) and can be run with `./gradlew test` once Java is installed.

---

## Date: 2026-10-19

### 1. History screen — swipe-to-delete fixed
**File:** `ui/history/HistoryScreen.kt`

- Replaced the px-based `detectHorizontalDragGestures` implementation with `Modifier.draggable` + `Animatable`. The old open threshold (120 px) was larger than the maximum drag distance (100 px), so a row could never stay open.
- Added a real full-height Delete button (icon + label) that opens the confirmation dialog. Previously `onDelete` was never invoked.
- Added dp-based sizing (`96.dp`), fling support, snap animations, tap-to-close, and single-open-row coordination.

### 2. History screen — delete actually deletes
**Files:** `ui/history/HistoryViewModel.kt`, `ui/history/HistoryScreen.kt`, `data/AttendanceRepository.kt`

- The UI was passing the formatted date (`"Mon, Oct 19, 2026"`) to `deleteItem`, while the database deletes by ISO `work_date` (`"2026-10-19"`), so no row matched.
- `HistoryItem` now carries the raw `workDate` for keys and deletion; the friendly `date` is display-only.
- Deleting today's record no longer calls `rescheduleAll()` (which immediately re-inserted a fresh today row); it cancels reminders instead.

### 3. Day-bounded attendance cycle (forgot sign-out)
**Files:** `domain/WorkdayCycle.kt` (new), `domain/AttendanceState.kt`, `domain/AttendanceEvent.kt`, `domain/AttendanceStateMachine.kt`, `domain/NextReminderCalculator.kt`, `data/AttendanceDao.kt`, `data/AttendanceRepository.kt`, `service/DailyRefreshReceiver.kt`, `ui/home/HomeViewModel.kt`, `ui/history/HistoryViewModel.kt`

- The attendance cycle now runs 12:00 AM–11:59 PM local time.
- At the day boundary, any session left `SIGNED_IN` or `SIGN_OUT_DUE` is auto-closed as the new terminal state `NOT_SIGNED_OUT`, with a recorded sign-out at the last millisecond of its work date. Its duration therefore ends with the day.
- Pending reminder fields are cleared and alarms from the previous day are cancelled; the new day's sign-in reminder is scheduled as usual.
- `DailyRefreshReceiver` now runs at 00:01 (was 00:05) and dismisses stale reminder notifications when a rollover closed a session.
- `HomeViewModel` now watches the current date and switches its session observation at midnight, so the Home screen resets without needing an app restart.
- History shows the auto-closed row as **Not signed out** (amber pill) with the recorded times/duration.
- Added `WorkdayCycleTest` plus new state-machine and calculator tests.

### 4. Settings — disable irrelevant options when reminders are off
**Files:** `ui/settings/SettingsScreen.kt`, `ui/settings/SettingsViewModel.kt`

- When **Enable reminders** is switched off, the controls that only affect reminders are now disabled and dimmed: vibrate, custom tune/tune picker, workdays, sign-in time, work-session duration, repeat interval, and snooze duration.
- Their saved values are preserved, so turning reminders back on restores the previous configuration.
- Added a short explanatory message in the REMINDERS card while reminders are off.
- The notification/exact-alarm permission prompts are hidden while reminders are disabled, since they are irrelevant.
- Turning reminders off now also dismisses any reminder notification that is already showing, in addition to cancelling pending alarms.

### 5. Settings — time values are now dropdowns
**File:** `ui/settings/SettingsScreen.kt`

- Replaced the numeric text boxes with compact tap-to-select dropdowns for:
  - Sign-in hour (1–12), minute (00–59), and AM/PM
  - Work-session duration hours (0–23) and minutes (00–59)
  - Repeat interval (1–60 minutes)
  - Snooze duration (1–60 minutes)
- Added a shared compact `DropdownField` so all selectors have the same look and touch target.
- The currently saved value is always shown and kept selectable even if it falls outside the standard list (handled by `optionsWith`).
- Dropdown menus are capped in height and scroll, so long lists such as 60 minutes remain usable.
- Each popup is measured and constrained to the exact width of the field it opens from, rather than Material's default 112dp minimum menu width.

### 6. Repeat interval clarified and fixed
**Files:** `data/AttendanceRepository.kt`, `ui/settings/SettingsScreen.kt`

- The repeat interval is the automatic re-reminder for an **ignored** notification; snooze is the manual postponement when the user taps **Snooze**. They are intentionally separate settings.
- Sign-out reminders now honour the configured repeat interval. Previously `handleSignOutAlarmFired()` re-armed the sign-out alarm at "now" (roughly every second) instead of waiting for the interval.
- Added explanatory text to the REPETITION settings card.
- Removed the unused `repeatSignIn()` / `repeatSignOut()` helpers (their logic is inline in the alarm handlers).

### 7. Theme section (dark / light)
**Files:** `data/AppSettings.kt`, `data/SettingsRepository.kt`, `ui/settings/SettingsScreen.kt`, `ui/settings/SettingsViewModel.kt`, `ui/MainActivity.kt`, `ui/theme/Theme.kt`, `ui/theme/Color.kt`, `ui/home/HomeScreen.kt`, `ui/history/HistoryScreen.kt`

- Added a persisted **Dark theme** toggle under a new **THEME** section in Settings (default: on).
- `MainActivity` now observes the saved setting and applies the matching Material color scheme app-wide, with no app restart required.
- Added `PunchAccentColors` / `LocalPunchAccentColors` with light and dark variants for the semantic accents used outside Material roles (status card, success/warning/danger, weekday chips).
- Updated Home, History and Settings to read these theme-aware accents, fixing light-mode readability (status card text, delete action, pills, chips, icon colors).
- System bars now use edge-to-edge styling with theme-appropriate icon colors, and the window background matches the selected theme so there is no dark flash on launch.
- The selected theme persists across restarts via `SharedPreferences`.

### 8. Notification options corrected
**Files:** `service/NotificationHelper.kt`, `service/ReminderReceiver.kt`, `DiContainer.kt`, `res/values/strings.xml`

- The notification **Snooze** action label now reflects the configured snooze duration instead of the hardcoded "Snooze 10 minutes" (e.g. "Snooze 1 minute" / "Snooze 25 minutes").
- Wired the **Vibrate on reminders** setting to notifications. Android O+ locks a channel's vibration setting, so a `.novibe` channel variant is created when vibration is disabled; pre-O notifications use `setVibrate`.
- The existing notification body/action audit confirmed the sign-out body still uses the configured session duration and the action intents use the current settings when tapped.

### 9. New minimal adaptive launcher icon
**Files:** `res/drawable/ic_launcher_background.xml`, `ic_launcher_foreground.xml`, `ic_launcher_monochrome.xml`, `res/mipmap-anydpi-v26/*`, `res/mipmap-*/*.png`

- Replaced the old "P + clock" artwork with a minimal mark: an open clock-cycle arc completed by a check.
- Uses the app's blue accent (`#8AB4F8`) and off-white check on a deep near-black blue radial background.
- Rebuilt as proper adaptive-icon vector layers (background, foreground) with a monochrome layer for Android 13+ themed icons.
- Regenerated the legacy PNG launcher/round icons for API 24-25 and removed the obsolete raster layers.

### 10. Signed release build
**Files:** `app/build.gradle`, `app/proguard-rules.pro`, `keystore.properties.example`, `releases/`, `README.md`

- Added release signing support through a local, gitignored `keystore.properties`.
- Enabled R8 code shrinking and resource shrinking for release builds.
- Built and verified the signed **1.0.0** release APK and added it under `releases/`.
- Documented download, installation, checksums, and release-key handling.

### 11. Version 1.0.1 — remove sensitive services
**Files:** `app/src/main/AndroidManifest.xml`, removed `GreytHrAttendanceMonitorService.kt`, `GreytHrNotificationListener.kt`, `accessibility_service_config.xml`, `res/values/strings.xml`, `app/build.gradle`, `README.md`, `releases/`

- Removed the optional accessibility service and notification-listener service.
- Removed the related `BIND_ACCESSIBILITY_SERVICE` permission, service declarations, config, and strings.
- Rebuilt as version **1.0.1** (versionCode 2) and republished the signed APK.
- This avoids Google Play Protect blocking sideloaded installs with the "This app can request access to sensitive data" warning. Core reminder/history/theme functionality is unchanged.

### 12. Version 1.1.0 — Home quick actions
**Files:** `ui/home/HomeScreen.kt`, `ui/home/HomeViewModel.kt`, `app/build.gradle`, `README.md`, `releases/`

- Added a primary **I've signed in** action to Home.
- The sign-in action appears once the configured sign-in time has passed; before then, Home shows "Sign-in action available after <time>".
- Added an **I've signed out** action while a session is open.
- Home actions dismiss the matching notification when used.
- Bumped to version **1.1.0** (versionCode 3).
