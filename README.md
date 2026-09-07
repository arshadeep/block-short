# Block Short

Block Short is a native Android wellbeing app that interrupts short-form feeds without blocking the useful parts of social apps. Instagram messaging, regular YouTube, and TikTok inboxes remain available; Reels, Shorts, and TikTok's video feed receive the chosen intervention.

## V0 features

- **Hard Mode:** blocks a detected short-form feed immediately, with a research-gated five/ten/twenty-scroll override.
- **Soft Mode:** counts feed swipes, warns at three/two/one remaining, and pauses at the configured limit.
- **Optional windows:** independently toggle Morning (6am–12pm), Afternoon (12pm–6pm), and Night (10pm–7am); with every window off, protection stays active all day.
- **Adaptive-limit preview:** clearly marked future Fitbit, smartwatch, and calendar connections show how limits could eventually respond to sleep, activity, and commitments.
- **Message-persona preview:** V0 uses an original chaotic, fourth-wall-breaking voice; future controls preview Gentle, Coach, and Chaotic personas.
- **Wake-up notification demo:** a Protect-screen button requests notification permission when needed and immediately posts a sample morning check-in; automatic wake detection remains a future integration.
- **Intentional extension:** leaving is immediate; adding five more swipes requires an eight-second research note.
- **On-device insights:** daily swipes, pauses, exits, extensions, and a conservative time-protected estimate.
- **Protected-day streaks:** one intentional feed exit protects the day; streaks and personal bests stay on-device and can be shared through Android's share sheet.
- **Research library:** concise, careful summaries linking to the underlying PubMed records.
- **Local-only:** no account, analytics SDK, network permission, or backend.

## Run it

Requirements: Android Studio Quail or newer, JDK 17, and Android SDK 37.

1. Open this directory in Android Studio and let Gradle sync.
2. Run the `app` configuration on a physical device or emulator running Android 8.0+.
3. Read the in-app disclosure, tap **I understand — open settings**, and enable **Block Short protection**.
4. Open a supported short-form feed and test Hard or Soft Mode.

The project uses Android Gradle Plugin 9.3.0, Gradle 9.5.0, built-in Kotlin, and the Compose 2026.06 BOM.

## How feed detection works

The Accessibility Service is package-scoped to Instagram, YouTube, and both global TikTok package IDs. Block Short looks for combinations of visible accessibility labels (for example, a short-form section label plus feed controls). A lone **Reels** or **Shorts** navigation label is intentionally insufficient. This reduces false positives and keeps messaging usable.

The intervention overlay is owned by the detected social app and is checked against the foreground window while visible. It is removed when the user leaves that app, changes to a non-short-form screen, disables protection, or removes that app from protection.

Social apps change their accessibility trees frequently. Before release, validate the detector against current app versions and add localized label sets. The detector logic is isolated in `ShortFormDetector.kt` and has pure unit tests.

## Important release notes

- Accessibility access is sensitive. The onboarding copy must remain explicit about what is inspected and what is not stored.
- Some banking and UPI apps refuse to run while *any* accessibility service is enabled, regardless of package scoping. Block Short provides an in-app action that calls Android's `disableSelf()` API and opens Accessibility settings; users must explicitly re-enable protection after banking. An app-level pause cannot satisfy finance apps that inspect the system accessibility setting.
- Google Play requires a declaration and prominent disclosure for non-accessibility-tool uses of `AccessibilityService`. Review the current policy before publishing.
- Block Short is a wellbeing product, not a medical device. Research copy distinguishes association from causation and avoids clinical claims.
- V0's “time protected” number is an estimate, not measured sleep impact.

## Research used in the app

- [Digital media use and sleep in late adolescence and young adulthood](https://pubmed.ncbi.nlm.nih.gov/36638702/)
- [Doomscrolling Scale and its associations](https://pubmed.ncbi.nlm.nih.gov/36275044/)
- [Pre-sleep social media use: a laboratory study](https://pubmed.ncbi.nlm.nih.gov/34627122/)
- [Short-video intervention and sleep quality: randomized trial](https://pubmed.ncbi.nlm.nih.gov/41743515/)
- [Social media use and sleep quality: review of reviews](https://pubmed.ncbi.nlm.nih.gov/41597059/)
