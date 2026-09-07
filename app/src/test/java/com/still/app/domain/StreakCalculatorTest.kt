package com.still.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StreakCalculatorTest {
    private val today = LocalDate.of(2026, 8, 9)

    @Test
    fun consecutiveProtectedDaysGrowTheStreak() {
        val yesterday = StreakState(3, 5, today.minusDays(1))

        val updated = StreakCalculator.recordProtectedDay(yesterday, today)

        assertEquals(StreakState(4, 5, today), updated)
    }

    @Test
    fun multipleExitsOnOneDayOnlyCountOnce() {
        val alreadyProtected = StreakState(4, 4, today)

        assertEquals(alreadyProtected, StreakCalculator.recordProtectedDay(alreadyProtected, today))
    }

    @Test
    fun aGapStartsANewStreakWithoutLosingTheBest() {
        val old = StreakState(8, 8, today.minusDays(3))

        assertEquals(StreakState(1, 8, today), StreakCalculator.recordProtectedDay(old, today))
        assertEquals(0, StreakCalculator.visibleCurrent(old, today))
    }
}
