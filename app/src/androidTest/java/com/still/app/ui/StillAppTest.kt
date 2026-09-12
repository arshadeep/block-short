package com.still.app.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.still.app.domain.DailyStats
import com.still.app.domain.ProtectionConfig
import com.still.app.ui.theme.StillTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StillAppTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun singleToggleTurnsProtectionOffWithoutChangingStreak() {
        val config = mutableStateOf(ProtectionConfig(enabled = true))
        compose.setContent {
            StillTheme {
                StillApp(config.value, DailyStats(currentStreak = 4), serviceEnabled = true) { config.value = it }
            }
        }
        compose.onAllNodes(isToggleable()).assertCountEquals(1)
        compose.onNode(isToggleable()).assertIsOn().performClick().assertIsOff()
        compose.runOnIdle { assertFalse(config.value.enabled) }
        compose.onNodeWithText("4").assertExists()
        compose.onNode(hasText("Soft Mode")).assertDoesNotExist()
        compose.onNode(hasText("Research")).assertDoesNotExist()
        compose.onNode(hasText("Insights")).assertDoesNotExist()
    }

    @Test
    fun missingSystemAccessShowsOffAndRequiresDisclosureBeforeEnabling() {
        var configChanged = false
        compose.setContent {
            StillTheme {
                StillApp(ProtectionConfig(enabled = true), DailyStats(), serviceEnabled = false) { configChanged = true }
            }
        }
        compose.onNode(isToggleable()).assertIsOff().performClick()
        compose.onNodeWithText("Enable Instagram protection").assertExists()
        compose.runOnIdle { assertFalse(configChanged) }
        compose.onNodeWithText("Cancel").performClick()
        compose.onNode(isToggleable()).assertIsOff()
        compose.runOnIdle { assertTrue(!configChanged) }
    }
}
