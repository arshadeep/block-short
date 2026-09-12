package com.still.app.data

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SettingsRepositoryTest {
    private val base = InstrumentationRegistry.getInstrumentation().targetContext
    private val prefsName = "minimal_version_test_preferences"
    private val context = object : ContextWrapper(base) {
        override fun getApplicationContext(): Context = this
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
            base.getSharedPreferences(prefsName, mode)
    }
    private val prefs = context.getSharedPreferences("ignored", Context.MODE_PRIVATE)

    @After fun cleanUp() { base.deleteSharedPreferences(prefsName) }

    @Test
    fun upgradePreservesStreakWhileIgnoringOldProtectionOptions() {
        prefs.edit().clear()
            .putBoolean("enabled", true)
            .putString("mode", "SOFT")
            .putBoolean("app_INSTAGRAM", false)
            .putBoolean("window_NIGHT", true)
            .putInt("session_limit", 50)
            .putInt("streak_current", 4)
            .putInt("streak_best", 6)
            .putString("streak_last_date", LocalDate.now().minusDays(1).toString())
            .commit()
        val repository = SettingsRepository(context)
        assertTrue(repository.loadConfig().enabled)
        assertEquals(4, repository.loadTodayStats().currentStreak)
        repository.recordExit()
        repository.recordExit()
        assertEquals(5, SettingsRepository(context).loadTodayStats().currentStreak)
        assertTrue(repository.loadTodayStats().protectedToday)
        repository.saveConfig(repository.loadConfig().copy(enabled = false))
        assertFalse(SettingsRepository(context).loadConfig().enabled)
        assertEquals(5, repository.loadTodayStats().currentStreak)
    }
}
