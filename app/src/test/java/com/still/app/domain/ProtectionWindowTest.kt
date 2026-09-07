package com.still.app.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class ProtectionWindowTest {
    @Test
    fun morningAndAfternoonUseHalfOpenWindows() {
        assertTrue(ProtectionWindow.MORNING.contains(LocalTime.of(6, 0)))
        assertFalse(ProtectionWindow.MORNING.contains(LocalTime.of(12, 0)))
        assertTrue(ProtectionWindow.AFTERNOON.contains(LocalTime.of(12, 0)))
        assertFalse(ProtectionWindow.AFTERNOON.contains(LocalTime.of(18, 0)))
    }

    @Test
    fun nightWrapsAcrossMidnight() {
        assertTrue(ProtectionWindow.NIGHT.contains(LocalTime.of(22, 0)))
        assertTrue(ProtectionWindow.NIGHT.contains(LocalTime.of(6, 59)))
        assertFalse(ProtectionWindow.NIGHT.contains(LocalTime.of(12, 0)))
    }

    @Test
    fun noSelectedWindowsMeansAllDayProtection() {
        val config = ProtectionConfig(enabledWindows = emptySet())

        assertTrue(config.isActiveAt(LocalTime.of(20, 0)))
    }

    @Test
    fun multipleSelectedWindowsAreCombined() {
        val config = ProtectionConfig(
            enabledWindows = setOf(ProtectionWindow.MORNING, ProtectionWindow.NIGHT),
        )

        assertTrue(config.isActiveAt(LocalTime.of(8, 0)))
        assertTrue(config.isActiveAt(LocalTime.of(23, 0)))
        assertFalse(config.isActiveAt(LocalTime.of(15, 0)))
    }
}
