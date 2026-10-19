# Manual QA Checklist

## Pre-Test Setup

- [ ] Android device or emulator (API 24+, preferably API 31+ for exact alarm testing)
- [ ] Punch Reminder debug APK installed
- [ ] greytHR app installed (optional, for testing app launch)

## 1. First Launch & Permissions

- [ ] App launches without crash
- [ ] Notification permission is requested (Android 13+)
- [ ] Exact alarm permission banner appears (Android 12+)
- [ ] Tapping "Open exact-alarm settings" opens the system settings page
- [ ] Home screen shows "Not signed in yet today." and scheduled sign-in time

## 2. Sign-In Reminder (Morning)

- [ ] At 10:30 AM on a workday, notification appears: "Time to sign in"
- [ ] Notification has actions: I've signed in, Snooze <configured snooze duration>, Pause today
- [ ] Tapping the notification body opens Punch Reminder (not greytHR)
- [ ] "I've signed in" records the time, updates home screen, schedules sign-out reminder
- [ ] "Snooze" delays the next reminder by the configured snooze duration (label matches it)
- [ ] "Pause today" opens the app with the reason picker
- [ ] Repeated reminders fire at the configured interval when ignored
- [ ] No reminder on Saturday/Sunday (default configuration)
- [ ] No reminder when reminders are disabled in Settings

## 3. Sign-Out Timer

- [ ] After confirming sign-in at 11:00 AM, sign-out reminder is due at 8:00 PM
- [ ] After confirming sign-in at 10:30 AM, sign-out reminder is due at 7:30 PM
- [ ] After confirming sign-in at 2:45 PM, sign-out reminder is due at 11:45 PM
- [ ] Home screen shows the next sign-out reminder time
- [ ] Sign-out time survives app close and reopen
- [ ] No sign-out reminder is scheduled if sign-in was never confirmed
- [ ] Home screen shows "Signed in at <time>" and "Sign-out reminder at <time>"

## 4. Sign-Out Notification

- [ ] At due time, notification appears: "Time to sign out"
- [ ] Notification body shows the configured session duration (for example, "Your 9-hour work session is complete")
- [ ] Actions: I've signed out, Snooze <configured snooze duration>
- [ ] Tapping the notification body opens Punch Reminder
- [ ] "I've signed out" records time, cancels reminder, marks session complete
- [ ] Home screen shows "Today's session is complete."
- [ ] Repeated reminders fire at interval until sign-out confirmed
- [ ] Snoozing sign-out moves reminder without changing session deadline

## 5. State Machine

- [ ] Confirming sign-in twice does not create duplicate timers
- [ ] Confirming sign-out when not signed in shows no effect
- [ ] Pausing today cancels today's reminders without deleting history
- [ ] Paused day shows the chosen reason on Home and in History
- [ ] Pause resets automatically on the next day
- [ ] Resume reminders restores today's schedule
- [ ] Paused session can be resumed; other terminal states cannot be reopened

## 6. Persistence & Recovery

- [ ] Reminders survive app being closed/swiped from recents
- [ ] Device restart restores pending reminders
- [ ] App update (reinstall) preserves pending schedules
- [ ] Changing device time does not corrupt recorded instants
- [ ] Changing time zone does not corrupt stored epoch millis
- [ ] History is preserved across app restarts

## 7. Settings

- [ ] Dark/light theme toggle switches the whole app immediately and persists after restart
- [ ] Changing workdays toggles reminders on/off for those days
- [ ] Changing sign-in time affects future workdays, not active sessions
- [ ] Changing session duration updates future calculations
- [ ] Changing repetition interval affects new repetitions
- [ ] Changing snooze duration updates the notification Snooze label and behavior
- [ ] Turning reminders off disables the dependent settings and cancels pending alarms
- [ ] Turning reminders back on restores the previously saved values
- [ ] Clear local history deletes all records with confirmation

## 8. History Screen

- [ ] Shows previous workdays with dates
- [ ] Shows confirmed sign-in time (locally recorded)
- [ ] Shows calculated sign-out reminder time
- [ ] Shows confirmed sign-out time
- [ ] Shows paused days with the selected reason
- [ ] Shows skipped days
- [ ] Shows auto-closed days as "Not signed out"
- [ ] Swipe left reveals Delete; tapping it asks for confirmation and removes the record
- [ ] Disclaimer visible: "not official greytHR attendance records"

## 9. greytHR Integration

- [ ] Installed greytHR app launches when "Open greytHR" tapped
- [ ] Play Store listing opens when greytHR is not installed
- [ ] No greytHR integration when no app is installed and the Play Store cannot be resolved

## 10. Edge Cases

- [ ] Sign-in at 11:00 PM → session auto-closes at midnight and is recorded as "Not signed out"
- [ ] Next day starts fresh with a new sign-in reminder
- [ ] Repeated app launches don't create duplicate notifications
- [ ] Acknowledged notification doesn't reappear
- [ ] App behaves gracefully when exact alarm permission is denied (falls back to inexact window)

## Pass/Fail Criteria

All items must pass for release. Items marked with known device limitations (exact alarm on restricted ROMs) are acceptable failures with user-visible explanation.
