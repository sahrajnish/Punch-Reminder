# Technical Report: Punch Reminder

## Reference Repository Analysis

The reference repository (https://github.com/BlackyHawky/Clock, GPLv3-licensed) was inspected for alarm, timer, notification, and persistence patterns. Key findings:

### Alarm Representation
- Alarms are represented with hour/minute fields plus a `Weekdays` bitmask for recurring schedules
- One-time alarms store year/month/day; repeating alarms use the `Weekdays` bitset
- Each alarm has a timezone field; defaults to device default
- `getNextAlarmTime(currentTime)` calculates the next firing occurrence, handling DST transitions by resetting hour/minute after day adjustment
- `AlarmInstance` represents a specific firing with state (SNOOZE_STATE, NOTIFICATION_STATE, etc.)

### Scheduling
- `AlarmManager` with `setExactAndAllowWhileIdle` (RTC_WAKEUP) for precise alarms
- `AlarmInitReceiver` handles BOOT_COMPLETED, LOCKED_BOOT_COMPLETED, TIME_SET, TIMEZONE_CHANGED, LOCALE_CHANGED
- Receiver uses `goAsync()` and a partial wake lock to complete work after broadcast returns
- `fixAlarmInstances()` rebuilds alarm instances after system events
- `PackageReplacedReceiver` handles app updates (MY_PACKAGE_REPLACED)

### Notifications
- Notification channels with importance, sound, vibration, lights
- Actionable notifications with PendingIntent broadcast actions
- Fixed notification IDs prevent duplicates
- `FLAG_IMMUTABLE` on PendingIntent (Android 12+ requirement)
- Full-screen intent permission for lock screen display

### Persistence
- SQLite database via `ClockDatabaseHelper` / `ClockProvider` (ContentProvider)
- Alarm and instance tables with full schema
- Type-safe column index constants

### Tests
- The reference repository did not include unit tests in the inspected source set

## Patterns Used as Inspiration

| Pattern | How It Was Used in Punch Reminder |
|---------|-----------------------------------|
| `Weekdays` bitmask for recurring schedules | Adapted into `Workdays.kt` with the same Monday=bit0 encoding |
| `getNextAlarmTime()` next-occurrence calculation | Adapted into `NextReminderCalculator.compute()` - pure, testable scheduling |
| Boot/time-change recovery receiver | `BootRecoveryReceiver` rebuilds pending reminders on BOOT_COMPLETED, TIME_SET, TIMEZONE_CHANGED |
| `AlarmManager.setAlarmClock()` | Used for user-visible, Doze-resistant reminder alarms; the daily refresh uses exact/window fallback |
| `FLAG_IMMUTABLE` PendingIntent | Used on all pending intents (Android 12+) |
| Notification channels per reminder type | Separate channels for sign-in and sign-out reminders, with sound/vibration variants |
| Fixed notification IDs to prevent duplicates | Sign-in (ID 1001) and sign-out (ID 1002) use fixed IDs |
| `goAsync()` + wake lock in receiver | `ReminderReceiver` uses `goAsync()` for async work |
| Day-bounded work cycle | `WorkdayCycle` auto-closes forgotten sign-outs at the end of the local day |

## How Punch Reminder Differs from the Reference

| Aspect | Reference (Clock) | Punch Reminder |
|--------|-------------------|----------------|
| Purpose | General-purpose clock (alarms, timers, stopwatch, world clock) | Personal attendance reminder for greytHR |
| UI toolkit | Android Views (XML layouts) | Jetpack Compose + Material 3 |
| Language | Java | Kotlin |
| Persistence | SQLite + ContentProvider | Room + SharedPreferences |
| Scheduling logic | Tightly coupled to Android | Pure `NextReminderCalculator` with injectable clock (JVM-testable) |
| Alarm model | Year/month/day/hour/minute + Weekdays | Epoch millis instants + session state machine |
| State model | Alarm enabled/enabled + instance state | AttendanceStateMachine: NOT_SIGNED_IN → SIGNED_IN → SIGN_OUT_DUE → SIGNED_OUT / NOT_SIGNED_OUT / SKIPPED |
| Day boundary | N/A | `WorkdayCycle` closes any still-open session at 11:59:59.999 PM and records it as NOT_SIGNED_OUT |
| Sign-out calculation | N/A | Sign-in instant + session duration = due time (never fixed clock time) |
| greytHR integration | N/A | Launcher interface (installed app or Play Store listing), no API |
| Background service | KeepAliveService, AlarmService (permanent FGS) | No permanent service; purely alarm-driven |
| Testing | No unit tests found | 43 unit tests covering scheduling, the state machine, and the workday cycle |
| License | GPLv3 | Original work (no code copied) |

## Licensing Note

The reference repository is GPLv3-licensed. This application was implemented as **original work** with no source code, artwork, icons, or branding copied from the reference repository. Only design patterns and architectural concepts (which are not copyrightable) were used as inspiration. No GPLv3 obligations apply to this application.

The launcher icon is original artwork for this project. It uses the modern adaptive-icon model: vector background (deep near-black blue radial gradient), vector foreground (an open clock-cycle arc completed by a check mark), and a monochrome layer for Android 13+ themed icons. Raster fallbacks (`mipmap-*/ic_launcher.png`, `ic_launcher_round.png`) cover API 24-25. Notification icons use a separate white-on-transparent `drawable-*/ic_notification.png`, since notification small icons must be monochrome silhouettes.
