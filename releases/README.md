# Releases

## Punch Reminder 1.0.1

| | |
|---|---|
| File | `Punch-Reminder-v1.0.1.apk` |
| Version | 1.0.1 (versionCode 2) |
| Min SDK | 24 |
| Target SDK | 34 |
| Size | ~3.0 MB |
| SHA-256 | `94816e16c1f60eedae42c73d62ce118b58d9169a2640b40714c27d64b61465a8` |
| Signing certificate SHA-256 | `4836a72d857e4feac3d8873c434e2248bd8528c1d5ac7873c5907ed8ea75409e` |

### What changed in 1.0.1

Removed the optional accessibility service and notification listener. Those services
requested sensitive system access and caused Google Play Protect to block sideloaded
installs with a "can request sensitive data" warning. The core reminder, history, theme,
and settings features are unchanged.

### Install

On a connected device:

```bash
adb install -r releases/Punch-Reminder-v1.0.1.apk
```

Or copy the APK to the device and open it. You may need to allow installation from
unknown sources for the file manager or browser you use.

> The app is not distributed through Google Play. It is a personal project, and the APK is
> signed with a local release key. Future updates must be signed with the same key.
