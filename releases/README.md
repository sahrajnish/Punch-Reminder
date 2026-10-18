# Releases

## Punch Reminder 1.1.1

| | |
|---|---|
| File | `Punch-Reminder-v1.1.1.apk` |
| Version | 1.1.1 (versionCode 4) |
| Min SDK | 24 |
| Target SDK | 34 |
| Size | ~3.0 MB |
| SHA-256 | `7bb57410ef9830623fc2ce9592f55c044ac4faed6a1c7c14a722d4403127eb86` |
| Signing certificate SHA-256 | `4836a72d857e4feac3d8873c434e2248bd8528c1d5ac7873c5907ed8ea75409e` |

### What's new in 1.1.1

- **I've signed in** is now green, matching the sign-in icon.
- **I've signed out** is now red, matching the sign-out icon.
- Button content colors adapt for contrast in dark and light themes.

Home quick actions from 1.1.0 remain: the sign-in action appears after your configured
sign-in time, and the sign-out action appears while a session is open.

### Install

On a connected device:

```bash
adb install -r releases/Punch-Reminder-v1.1.1.apk
```

Or copy the APK to the device and open it. You may need to allow installation from
unknown sources for the file manager or browser you use.

> The app is not distributed through Google Play. It is a personal project, and the APK is
> signed with a local release key. Future updates must be signed with the same key.
