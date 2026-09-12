package com.still.app.accessibility

import com.still.app.domain.SupportedApp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HardModeEntryGateTest {
    private val gate = HardModeEntryGate()
    private val instagram = SupportedApp.INSTAGRAM
    private val reels = ShortFormDetector.detect(instagram,
        setOf("Reels", "Like", "Comment", "Share"), setOf("Reels"))
    private val missingControls = ShortFormDetector.detect(instagram, emptySet())
    private val home = ShortFormDetector.detect(instagram, setOf("Your story", "Home"), setOf("Home"))

    @Test
    fun periodicSnapshotConfirmsEntryWithoutAnotherScrollEvent() {
        assertFalse(gate.observe(instagram, reels, 0))
        // Same screen sampled by the periodic check, with no new scroll event.
        assertTrue(gate.observe(instagram, reels, 250))
    }

    @Test
    fun transientMissingControlsDoNotKeepCancellingTheBlock() {
        assertFalse(gate.observe(instagram, reels, 0))
        for (now in listOf(40L, 80L, 120L, 160L, 200L)) {
            assertFalse(gate.observe(instagram, missingControls, now))
        }
        assertTrue(gate.observe(instagram, reels, 250))
    }

    @Test
    fun frequentEventsCannotDelayConfirmation() {
        assertFalse(gate.observe(instagram, reels, 0))
        assertFalse(gate.observe(instagram, reels, 40))
        assertFalse(gate.observe(instagram, reels, 80))
        assertTrue(gate.observe(instagram, reels, 120))
    }

    @Test
    fun leavingForHomeImmediatelyClearsPendingEntry() {
        assertFalse(gate.observe(instagram, reels, 0))
        assertFalse(gate.observe(instagram, home, 100))
        assertFalse(gate.observe(instagram, missingControls, 250))
        assertFalse(gate.observe(instagram, reels, 300))
        assertTrue(gate.observe(instagram, reels, 550))
    }

    @Test
    fun missingSnapshotsAloneNeverTriggerABlock() {
        assertFalse(gate.observe(instagram, reels, 0))
        for (now in 250L..5_000L step 250L) {
            assertFalse(gate.observe(instagram, missingControls, now))
        }
        assertFalse(gate.observe(instagram, reels, 5_250))
    }

    @Test
    fun appSwitchCannotReuseAnotherAppsPendingEntry() {
        assertFalse(gate.observe(instagram, reels, 0))
        gate.reset() // A known non-Instagram foreground resets entry in the service.
        assertFalse(gate.observe(instagram, reels, 500))
    }

    @Test
    fun exitingOrDisablingClearsEntryHistory() {
        assertFalse(gate.observe(instagram, reels, 0))
        gate.reset()
        assertFalse(gate.observe(instagram, reels, 250))
    }
}
