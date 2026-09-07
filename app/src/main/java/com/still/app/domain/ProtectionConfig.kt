package com.still.app.domain

import java.time.LocalTime

enum class ProtectionMode { HARD, SOFT }

enum class ProtectionWindow {
    MORNING,
    AFTERNOON,
    NIGHT;

    fun contains(time: LocalTime): Boolean {
        val minutes = time.hour * 60 + time.minute
        return when (this) {
            MORNING -> minutes in 6 * 60 until 12 * 60
            AFTERNOON -> minutes in 12 * 60 until 18 * 60
            NIGHT -> minutes >= 22 * 60 || minutes < 7 * 60
        }
    }
}

enum class SupportedApp(
    val displayName: String,
    val packageNames: Set<String>,
) {
    INSTAGRAM("Instagram", setOf("com.instagram.android")),
    YOUTUBE("YouTube", setOf("com.google.android.youtube")),
    TIKTOK("TikTok", setOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill"));

    companion object {
        fun fromPackage(packageName: String): SupportedApp? =
            entries.firstOrNull { packageName in it.packageNames }
    }
}

data class ProtectionConfig(
    val enabled: Boolean = true,
    val mode: ProtectionMode = ProtectionMode.HARD,
    val enabledWindows: Set<ProtectionWindow> = emptySet(),
    val sessionLimit: Int = 10,
    val enabledApps: Set<SupportedApp> = SupportedApp.entries.toSet(),
) {
    fun isActiveAt(time: LocalTime): Boolean = enabled &&
        (enabledWindows.isEmpty() || enabledWindows.any { it.contains(time) })
}

data class DailyStats(
    val date: String,
    val scrollsSeen: Int = 0,
    val blocksShown: Int = 0,
    val exitsChosen: Int = 0,
    val extensionsChosen: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val protectedToday: Boolean = false,
) {
    val estimatedMinutesSaved: Int
        get() = exitsChosen * 6 + blocksShown.coerceAtMost(exitsChosen) * 2
}
