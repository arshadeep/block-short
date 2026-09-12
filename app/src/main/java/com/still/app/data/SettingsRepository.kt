package com.still.app.data

import android.content.Context
import android.content.SharedPreferences
import com.still.app.domain.DailyStats
import com.still.app.domain.ProtectionConfig
import com.still.app.domain.StreakCalculator
import com.still.app.domain.StreakState
import java.time.LocalDate

class SettingsRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // Old modes, schedules, allowances and app selections cannot silently affect protection.
    fun loadConfig(): ProtectionConfig = ProtectionConfig(
        enabled = prefs.getBoolean(KEY_ENABLED, true),
    )

    fun saveConfig(config: ProtectionConfig) {
        prefs.edit().putBoolean(KEY_ENABLED, config.enabled).apply()
    }

    fun registerConfigChangeListener(
        onChanged: (ProtectionConfig) -> Unit,
    ): SharedPreferences.OnSharedPreferenceChangeListener {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_ENABLED) onChanged(loadConfig())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        return listener
    }

    fun unregisterConfigChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    @Synchronized
    fun loadTodayStats(): DailyStats {
        val today = LocalDate.now()
        val streak = loadStreakState()
        return DailyStats(
            currentStreak = StreakCalculator.visibleCurrent(streak, today),
            protectedToday = streak.lastProtectedDate == today,
        )
    }

    @Synchronized
    fun recordExit() {
        val updated = StreakCalculator.recordProtectedDay(loadStreakState(), LocalDate.now())
        prefs.edit()
            .putInt(KEY_STREAK_CURRENT, updated.current)
            .putInt(KEY_STREAK_BEST, updated.best)
            .putString(KEY_STREAK_LAST_DATE, updated.lastProtectedDate?.toString())
            .apply()
    }

    private fun loadStreakState(): StreakState = StreakState(
        current = prefs.getInt(KEY_STREAK_CURRENT, 0),
        best = prefs.getInt(KEY_STREAK_BEST, 0),
        lastProtectedDate = prefs.getString(KEY_STREAK_LAST_DATE, null)?.let { stored ->
            runCatching { LocalDate.parse(stored) }.getOrNull()
        },
    )

    private companion object {
        const val PREFS = "still_preferences"
        const val KEY_ENABLED = "enabled"
        const val KEY_STREAK_CURRENT = "streak_current"
        const val KEY_STREAK_BEST = "streak_best"
        const val KEY_STREAK_LAST_DATE = "streak_last_date"
    }
}
