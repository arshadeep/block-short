package com.still.app.accessibility

import com.still.app.domain.SupportedApp

/** Confirm entry across transient missing controls, but never block on a stale snapshot. */
internal class HardModeEntryGate {
    private var candidate: SupportedApp? = null
    private var firstSeenAt = 0L
    private var lastSeenAt = 0L

    fun observe(app: SupportedApp, result: DetectionResult, now: Long): Boolean {
        if (app != candidate || result.isKnownNonFeed || now - lastSeenAt > MAX_GAP_MS) reset()
        if (!result.isShortFormFeed) return false
        if (candidate == null) {
            candidate = app
            firstSeenAt = now
        }
        lastSeenAt = now
        return now - firstSeenAt >= CONFIRM_MS
    }

    fun reset() {
        candidate = null
        firstSeenAt = 0L
        lastSeenAt = 0L
    }

    private companion object {
        const val CONFIRM_MS = 120L
        const val MAX_GAP_MS = 1_000L
    }
}
