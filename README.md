# Block Short

A minimal Android app for blocking Instagram Reels.

## First version

- One screen: your daily streak and one **Block Reels** on/off toggle.
- Block Reels on blocks detected Instagram Reels all day. There are no scroll allowances or extensions.
- Block Reels off stops blocking and disables Block Short's accessibility service. Turning it back on requires enabling the service in Android settings again; the toggle opens a short disclosure first.
- Choose **Instagram home** or **Phone home** from a blocked Reel to count the day toward your streak. Multiple exits on the same day count once. Missing a day resets the current streak.
- Instagram only. Home, Stories, messages, Notifications, and Profile are intended to stay available.
- No accounts, network permission, analytics, notifications, schedules, research screens, or extra settings.

Existing streaks and the saved protection on/off preference are retained. Legacy mode, schedule, app-selection, and session-limit preferences are ignored.

## Run

Use Android Studio, JDK 17, and Android SDK 37. Open this directory, sync Gradle, and run the `app` configuration on Android 8.0 or newer. Turn on Block Reels, read the disclosure, and enable **Block Short protection** in Android accessibility settings.

## Detection and intervention

The service receives events for `com.instagram.android` only. It checks foreground window identity to prevent stale events or overlays from affecting other apps. Only Instagram content is scanned for Reels evidence.

The detector uses visible viewer containers, selected Reels tabs with playback controls, or a Reels header above a vertical action rail. Visible Home, Stories, inbox, Notifications, and Profile markers veto detection. Hidden nodes and shared video-player components are insufficient.

Fresh observations confirm entry at least 120 ms apart, with an additional 250 ms foreground Instagram scan. Briefly missing controls do not cancel confirmation; known permitted screens, app switches, or more than one second without positive evidence reset it. Idle scans run once per second.

Before showing the two-exit blocking screen, the service sends Back to leave Reels and stop playback. The overlay belongs to Instagram and is dismissed when Instagram leaves the foreground or protection is disabled. Delayed home navigation verifies that Instagram is still foreground before acting. Actions remain scrollable on compact displays and at large font sizes.

## Validation

Run `./gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleRelease assembleDebugAndroidTest`.

With an emulator or test device connected, run `./gradlew.bat connectedDebugAndroidTest` for accessibility-node filtering, cross-app root rejection, compact/large-font overlay layouts, exit taps, the minimal screen, and preference compatibility.

On a physical phone, verify Reels entry from its tab and shared posts, playback stopping, both exit buttons, and overlay dismissal when switching apps. Home scrolling, multiple Stories including shared Reels, inbox, Notifications, and Profile should remain uninterrupted. Check Block Reels off, re-enabling through Android settings, streak persistence, and that YouTube/TikTok receive no intervention. Instrumentation tests do not reproduce Instagram's private accessibility tree or verify its audio behavior.

Some banking and UPI apps reject enabled accessibility services. Turning Block Reels off requests Android to disable Block Short's service; other enabled services may still affect those apps.
