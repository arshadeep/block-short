# Block Short privacy note

Block Short processes Instagram accessibility events on the device to detect Reels. It reads visible labels to identify the screen, exits detected Reels, and displays a blocking screen when Block Reels is on. Foreground package identity is checked to dismiss that screen when you leave Instagram.

The app does not request internet access, transmit data, save message text or viewed content, record the screen, or use advertising/analytics SDKs.

The app saves the Block Reels on/off preference and streak history locally. Existing installations may retain unused settings and aggregate counters from earlier versions; these no longer affect protection or receive updates. Preferences are excluded from Android cloud backup and device transfer.

Turning Block Reels off requests that Android disable Block Short's accessibility service. Turning it back on requires enabling access in Android settings after the in-app disclosure.

This note describes the development version's behavior.
