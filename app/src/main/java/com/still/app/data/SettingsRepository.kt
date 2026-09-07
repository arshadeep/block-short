package com.still.app.data

import android.content.Context
import android.content.SharedPreferences
import com.still.app.domain.DailyStats
import com.still.app.domain.ProtectionConfig
import com.still.app.domain.ProtectionMode
import com.still.app.domain.ProtectionWindow
import com.still.app.domain.StreakCalculator
import com.still.app.domain.StreakState
import com.still.app.domain.SupportedApp
import java.time.LocalDate

class SettingsRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun loadConfig(): ProtectionConfig {
        val apps = SupportedApp.entries.filterTo(mutableSetOf()) {
            prefs.getBoolean("app_${it.name}", true)
        }
        val hasWindowToggles = ProtectionWindow.entries.any { window ->
            prefs.contains("window_${window.name}")
        }
        val windows = if (hasWindowToggles) {
            ProtectionWindow.entries.filterTo(mutableSetOf()) { window ->
                prefs.getBoolean("window_${window.name}", false)
            }
        } else {
            // Preserve the single preset used by the previous build. A fresh
            // install has no selected windows, which means protection is all day.
            prefs.getString(KEY_LEGACY_WINDOW, null)?.let { stored ->
                runCatching { setOf(ProtectionWindow.valueOf(stored)) }.getOrDefault(emptySet())
            }.orEmpty()
        }
        return ProtectionConfig(
            enabled = prefs.getBoolean(KEY_ENABLED, true),
            mode = runCatching {
                ProtectionMode.valueOf(prefs.getString(KEY_MODE, ProtectionMode.HARD.name).orEmpty())
            }.getOrDefault(ProtectionMode.HARD),
            enabledWindows = windows,
            sessionLimit = prefs.getInt(KEY_LIMIT, 10).coerceIn(5, 50),
            enabledApps = apps,
        )
    }

    fun saveConfig(config: ProtectionConfig) {
        prefs.edit()
            .putBoolean(KEY_ENABLED, config.enabled)
            .putString(KEY_MODE, config.mode.name)
            .putInt(KEY_LIMIT, config.sessionLimit)
            .also { editor ->
                SupportedApp.entries.forEach { app ->
                    editor.putBoolean("app_${app.name}", app in config.enabledApps)
                }
                ProtectionWindow.entries.forEach { window ->
                    editor.putBoolean("window_${window.name}", window in config.enabledWindows)
                }
            }
            .apply()
    }

    fun registerConfigChangeListener(
        onChanged: (ProtectionConfig) -> Unit,
    ): SharedPreferences.OnSharedPreferenceChangeListener {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key != null && isConfigKey(key)) onChanged(loadConfig())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        return listener
    }

    fun unregisterConfigChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    @Synchronized
    fun loadTodayStats(): DailyStats {
        resetStatsIfNeeded()
        val today = LocalDate.now()
        val streak = loadStreakState()
        return DailyStats(
            date = prefs.getString(KEY_STATS_DATE, today.toString()).orEmpty(),
            scrollsSeen = prefs.getInt(KEY_SCROLLS, 0),
            blocksShown = prefs.getInt(KEY_BLOCKS, 0),
            exitsChosen = prefs.getInt(KEY_EXITS, 0),
            extensionsChosen = prefs.getInt(KEY_EXTENSIONS, 0),
            currentStreak = StreakCalculator.visibleCurrent(streak, today),
            bestStreak = streak.best,
            protectedToday = streak.lastProtectedDate == today,
        )
    }

    @Synchronized
    fun recordScroll() = increment(KEY_SCROLLS)

    @Synchronized
    fun recordBlock() = increment(KEY_BLOCKS)

    @Synchronized
    fun recordExit() {
        increment(KEY_EXITS)
        val updated = StreakCalculator.recordProtectedDay(loadStreakState(), LocalDate.now())
        prefs.edit()
            .putInt(KEY_STREAK_CURRENT, updated.current)
            .putInt(KEY_STREAK_BEST, updated.best)
            .putString(KEY_STREAK_LAST_DATE, updated.lastProtectedDate?.toString())
            .apply()
    }

    @Synchronized
    fun recordExtension() = increment(KEY_EXTENSIONS)

    private fun increment(key: String) {
        resetStatsIfNeeded()
        prefs.edit().putInt(key, prefs.getInt(key, 0) + 1).apply()
    }

    private fun loadStreakState(): StreakState = StreakState(
        current = prefs.getInt(KEY_STREAK_CURRENT, 0),
        best = prefs.getInt(KEY_STREAK_BEST, 0),
        lastProtectedDate = prefs.getString(KEY_STREAK_LAST_DATE, null)?.let { stored ->
            runCatching { LocalDate.parse(stored) }.getOrNull()
        },
    )

    private fun resetStatsIfNeeded() {
        val today = LocalDate.now().toString()
        if (prefs.getString(KEY_STATS_DATE, null) != today) {
            prefs.edit()
                .putString(KEY_STATS_DATE, today)
                .putInt(KEY_SCROLLS, 0)
                .putInt(KEY_BLOCKS, 0)
                .putInt(KEY_EXITS, 0)
                .putInt(KEY_EXTENSIONS, 0)
                .commit()
        }
    }

    private fun isConfigKey(key: String): Boolean =
        key == KEY_ENABLED ||
            key == KEY_MODE ||
            key == KEY_LEGACY_WINDOW ||
            key == KEY_LIMIT ||
            key.startsWith("app_") ||
            key.startsWith("window_")

    private companion object {
        const val PREFS = "still_preferences"
        const val KEY_ENABLED = "enabled"
        const val KEY_MODE = "mode"
        const val KEY_LEGACY_WINDOW = "protection_window"
        const val KEY_LIMIT = "session_limit"
        const val KEY_STATS_DATE = "stats_date"
        const val KEY_SCROLLS = "stats_scrolls"
        const val KEY_BLOCKS = "stats_blocks"
        const val KEY_EXITS = "stats_exits"
        const val KEY_EXTENSIONS = "stats_extensions"
        const val KEY_STREAK_CURRENT = "streak_current"
        const val KEY_STREAK_BEST = "streak_best"
        const val KEY_STREAK_LAST_DATE = "streak_last_date"
    }
}
