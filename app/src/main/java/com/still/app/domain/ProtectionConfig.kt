package com.still.app.domain

enum class SupportedApp(
    val displayName: String,
    val packageNames: Set<String>,
) {
    INSTAGRAM("Instagram", setOf("com.instagram.android"));

    companion object {
        fun fromPackage(packageName: String): SupportedApp? =
            entries.firstOrNull { packageName in it.packageNames }
    }
}

data class ProtectionConfig(val enabled: Boolean = true)

data class DailyStats(
    val currentStreak: Int = 0,
    val protectedToday: Boolean = false,
)
