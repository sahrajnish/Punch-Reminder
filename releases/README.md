# Releases

## Punch Reminder 1.2.0

| | |
|---|---|
| File | `Punch-Reminder-v1.2.0.apk` |
| Version | 1.2.0 (versionCode 5) |
| Min SDK | 24 |
| Target SDK | 34 |
| Size | ~3.0 MB |
| SHA-256 | `d343e7b4b0107215a31ed305ccf779608a5d753b1ab565779400483ab29f83cd` |
| Signing certificate SHA-256 | `4836a72d857e4feac3d8873c434e2248bd8528c1d5ac7873c5907ed8ea75409e` |

### What's new in 1.2.0

- **Pause reminders for today** from Home, with a reason:
  - PTO
  - Company Holiday
  - Sick Leave
  - Other
- The pause cancels today's reminders and **resets automatically at midnight**.
- **Resume reminders** restores the day's schedule if you paused by mistake.
- History shows a **PAUSED** pill with the reason under the date.
- Notification actions:
  - Sign-in reminder: **I've signed in** · **Snooze X** · **Pause today**
  - Sign-out reminder: **I've signed out** · **Snooze X**
- Tapping the notification body now opens **Punch Reminder** instead of greytHR.
- Tapping **Pause today** in the notification opens the app with the reason picker.

### Install

On a connected device:

```bash
adb install -r releases/Punch-Reminder-v1.2.0.apk
```

Or copy the APK to the device and open it. You may need to allow installation from
unknown sources for the file manager or browser you use.

> The app is not distributed through Google Play. It is a personal project, and the APK is
> signed with a local release key. Future updates must be signed with the same key.
