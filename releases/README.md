# Releases

## Punch Reminder 1.1.0

| | |
|---|---|
| File | `Punch-Reminder-v1.1.0.apk` |
| Version | 1.1.0 (versionCode 3) |
| Min SDK | 24 |
| Target SDK | 34 |
| Size | ~3.0 MB |
| SHA-256 | `f75e44dca726ec63f2ed5563512bfe5de5d3b40e9417479b4955faa107bb0ac8` |
| Signing certificate SHA-256 | `4836a72d857e4feac3d8873c434e2248bd8528c1d5ac7873c5907ed8ea75409e` |

### What's new in 1.1.0

- **Home quick actions:** confirm sign-in and sign-out directly from the Home screen.
- The **I've signed in** action appears once your configured sign-in time has passed.
- Before that time, Home shows when the action becomes available.
- While a session is open, **I've signed out** appears on Home.
- Using a Home action dismisses the matching reminder notification.

### Install

On a connected device:

```bash
adb install -r releases/Punch-Reminder-v1.1.0.apk
```

Or copy the APK to the device and open it. You may need to allow installation from
unknown sources for the file manager or browser you use.

> The app is not distributed through Google Play. It is a personal project, and the APK is
> signed with a local release key. Future updates must be signed with the same key.
