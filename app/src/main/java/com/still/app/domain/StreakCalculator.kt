package com.still.app.domain

import java.time.LocalDate

data class StreakState(
    val current: Int = 0,
    val best: Int = 0,
    val lastProtectedDate: LocalDate? = null,
)

object StreakCalculator {
    fun recordProtectedDay(state: StreakState, today: LocalDate): StreakState {
        if (state.lastProtectedDate == today) return state

        val current = if (state.lastProtectedDate == today.minusDays(1)) {
            state.current + 1
        } else {
            1
        }
        return StreakState(
            current = current,
            best = maxOf(state.best, current),
            lastProtectedDate = today,
        )
    }

    fun visibleCurrent(state: StreakState, today: LocalDate): Int = when (state.lastProtectedDate) {
        today, today.minusDays(1) -> state.current
        else -> 0
    }
}
