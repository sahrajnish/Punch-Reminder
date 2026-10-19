# Punch Reminder

**A local-first Android attendance reminder for greytHR.**

Punch Reminder helps you remember to sign in and sign out of greytHR, records your
confirmed attendance timestamps locally, and closes each workday cleanly at midnight.

> This is a personal reminder utility, not an official greytHR product. Timestamps stored
> here are locally confirmed records and are not official attendance records.

---

## Overview

Punch Reminder is a small, privacy-friendly Android app built around one problem:
forgetting to punch in or out. It schedules exact reminders, keeps a local attendance
history, and gets out of the way.

Everything runs on-device. There is no account, no cloud sync, no analytics, and no
network permission.

## Download

A signed release APK is included in this repository:

- [**Punch Reminder 1.2.0 APK**](releases/Punch-Reminder-v1.2.0.apk)
- SHA-256: `d343e7b4b0107215a31ed305ccf779608a5d753b1ab565779400483ab29f83cd`

```bash
adb install -r releases/Punch-Reminder-v1.2.0.apk
```

See [`releases/README.md`](releases/README.md) for signing details and checksums.

## Features

- **Sign-in reminder** — a notification at your configured time on your selected workdays
  (default 10:30 AM, Monday–Friday).
- **Sign-out reminder** — scheduled from your confirmed sign-in plus your configured
  session duration (default 9 hours).
- **Automatic repetition** — ignored reminders notify again after the repeat interval.
- **Snooze** — postpone a reminder from the notification; the label matches your configured
  snooze duration.
- **Actionable notifications** — open Punch Reminder, confirm sign-in, confirm sign-out, snooze, or pause today.
- **Home quick actions** — confirm sign-in or sign-out directly on the Home screen. The sign-in action appears once your configured sign-in time arrives.
- **Pause today** — pause reminders for a PTO day, company holiday, sick leave, or other reason. The pause resets automatically at midnight.
- **Day-bounded cycle** — the workday runs 12:00 AM–11:59 PM local time. If you forget to
  sign out, the session is closed automatically at the end of the day and recorded as
  **Not signed out**.
- **Attendance history** — locally stored sign-in/sign-out times, durations, skipped days,
  and auto-closed days, with swipe-to-delete.
- **Dark and light themes** — an app-wide theme switch that persists across restarts.
- **Sensible settings** — reminders, workdays, sign-in time, session duration, repeat
  interval, snooze, notification sound, and vibration. Settings that only affect reminders
  are disabled while reminders are off, without losing their values.
- **Resilient scheduling** — exact alarms survive app closure; boot, time-change, and
  time-zone changes rebuild the schedule; a daily refresh keeps the chain alive.

## How it works

1. At the configured time, Punch Reminder notifies you to sign in.
2. After signing in to greytHR, you tap **I've signed in** in the app or notification.
3. The app records the sign-in instant and schedules the sign-out reminder for the end of
   your configured session.
4. When the reminder arrives, you tap **I've signed out** and the day is complete.
5. If you're off for the day, tap **Pause reminders for today** and choose PTO, Company
   Holiday, Sick Leave, or Other. Reminders resume automatically tomorrow.
6. If you forget to sign out, the day closes at midnight and appears in history as
   **Not signed out**.
7. The next workday starts fresh with a new reminder.

## Screens

| Screen | Purpose |
|--------|---------|
| **Home** | Live clock, today's status, sign-in/sign-out times, and next reminder |
| **History** | Locally saved attendance records with delete actions |
| **Settings** | Theme, reminder schedule, workdays, repetition, snooze, sound, and vibration |

## Tech stack

- **Kotlin** + **Jetpack Compose** + **Material 3**
- **Room** for local attendance history
- **SharedPreferences** for settings
- **AlarmManager** for exact, reboot-resilient reminders
- **Kotlin coroutines / Flow** for reactive state
- **Gradle** (AGP 8.2, Kotlin 1.9.22, Compose BOM 2024.01)

## Project structure

```
app/src/main/java/com/punchreminder/app/
├── data/       Room database, DAO, repositories, settings
├── domain/     Pure scheduling/state logic, workday cycle, greytHR launcher interface
├── service/    Alarm receivers, notification helper, boot/daily refresh receivers
└── ui/         Compose screens, view models, and theme

app/src/test/   Pure-JVM unit tests for the domain logic
```

Key domain pieces:

- `AttendanceStateMachine` — legal state transitions for a workday.
- `NextReminderCalculator` — pure, testable reminder scheduling engine.
- `WorkdayCycle` — closes forgotten sign-outs at the end of the local day.
- `AttendanceRepository` — coordinates state, persistence, alarms, and day rollover.

## Getting started

### Prerequisites

- Android Studio (or the Android SDK + command line)
- **JDK 17** — required by this project's Kotlin/AGP versions
- Android SDK API 34 (compile) and a device/emulator running API 24+

> Newer JDKs (21/25) may fail with Kotlin 1.9.22. If the build complains about the Java
> version, point `JAVA_HOME` at a JDK 17 installation.

### Build

```bash
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

### Release build

Release builds use a local signing key configured in `keystore.properties` (gitignored).
Copy `keystore.properties.example`, point it at your keystore, then run:

```bash
./gradlew assembleRelease
```

The signed APK is written to `app/build/outputs/apk/release/app-release.apk`. R8 and
resource shrinking are enabled for release builds.

> Keep your release keystore and `keystore.properties` backed up. Android will reject
> future updates signed with a different key.

### Install

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

If `local.properties` is missing, create it with your SDK path:

```properties
sdk.dir=/path/to/Android/Sdk
```

## Permissions

| Permission | Why it is needed |
|------------|------------------|
| Notifications (Android 13+) | Show sign-in/sign-out reminders |
| Exact alarms (Android 12+) | Deliver reminders at the configured time |
| Vibrate | Optional vibration on reminders |
| Receive boot completed | Rebuild the reminder schedule after reboot |

The app does **not** request the `INTERNET` permission.

## Testing

Run the pure-JVM unit tests:

```bash
./gradlew testDebugUnitTest
```

The tests cover state transitions, reminder scheduling, sign-out calculations, workday
rollover/auto-close behavior, and reminder cancellation.

## Privacy

- All attendance history and settings stay on your device.
- No accounts, analytics, tracking, or cloud backup.
- No network permission is declared.
- The app does not store greytHR credentials.

## greytHR integration & limitations

Punch Reminder can:

- Open the installed greytHR app (or its Play Store listing).

Punch Reminder does **not**:

- Fetch attendance records from greytHR.
- Read notifications or screen content.
- Sign in to greytHR on your behalf.
- Store greytHR passwords or bypass multi-factor authentication.
- Call unofficial or scraped greytHR endpoints.
- Automatically submit attendance swipes.

Because there is no public, employee-facing greytHR attendance API, the app relies on your
explicit confirmations. Official greytHR APIs are organization-scoped and require an API
user created by your employer.

Other limitations:

- Exact alarms may be delayed by aggressive OEM battery management.
- There is no holiday calendar; skip non-working days manually.
- Data is local to one device and is not backed up.

## Disclaimer

Punch Reminder is an independent personal project. It is not affiliated with, endorsed by,
or supported by greytHR. Use it as a reminder aid, not as an official attendance record.

## Author

Made with care by **Rajnish**.
