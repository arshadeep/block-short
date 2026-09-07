# Block Short V0 privacy note

Block Short processes accessibility events on the device to identify supported short-form feeds and count scroll events. It does not transmit data, request internet access, store message text, store viewed content, record the screen, or use advertising/analytics SDKs.

The service is package-scoped to supported social apps. A detected feed is exited before the intervention appears so its media stops, and the intervention is removed when the social app is no longer foreground. Some banking and UPI apps may still reject any enabled accessibility service; Block Short offers a control to disable its system accessibility access before using those apps.

Saved data is limited to protection settings and aggregate daily counters. The preference file is excluded from Android cloud backup and device transfer.

This note is a product-development summary, not a substitute for the final public privacy policy or Google Play disclosure.
