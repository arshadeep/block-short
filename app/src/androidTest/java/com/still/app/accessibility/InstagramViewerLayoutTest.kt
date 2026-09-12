package com.still.app.accessibility

import android.graphics.Rect
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InstagramViewerLayoutTest {
    @Test
    fun titleAboveVerticalActionRailIdentifiesViewer() {
        val layout = InstagramViewerLayout(Rect(0, 0, 1080, 2400))
        layout.observe(listOf("reels"), Rect(20, 80, 240, 160))
        addRail(layout)
        assertTrue(layout.isViewer)
    }

    @Test
    fun bottomNavigationReelsLabelIsNotAViewerTitle() {
        val layout = InstagramViewerLayout(Rect(0, 0, 1080, 2400))
        layout.observe(listOf("reels"), Rect(200, 2250, 350, 2350))
        addRail(layout)
        assertFalse(layout.isViewer)
    }

    @Test
    fun horizontalPostActionsDoNotIdentifyReels() {
        val layout = InstagramViewerLayout(Rect(0, 0, 1080, 2400))
        layout.observe(listOf("reels"), Rect(20, 80, 240, 160))
        layout.observe(listOf("like"), Rect(20, 1600, 100, 1700))
        layout.observe(listOf("comment"), Rect(120, 1600, 200, 1700))
        layout.observe(listOf("share"), Rect(220, 1600, 300, 1700))
        assertFalse(layout.isViewer)
    }

    @Test
    fun missingHeaderAndOffscreenControlsCannotTrigger() {
        val layout = InstagramViewerLayout(Rect(0, 0, 1080, 2400))
        addRail(layout)
        assertFalse(layout.isViewer)
        layout.observe(listOf("reels"), Rect(20, -200, 240, -100))
        assertFalse(layout.isViewer)
    }

    private fun addRail(layout: InstagramViewerLayout) {
        layout.observe(listOf("unlike"), Rect(950, 1200, 1050, 1300))
        layout.observe(listOf("comments, 12"), Rect(950, 1400, 1050, 1500))
        layout.observe(listOf("send"), Rect(950, 1600, 1050, 1700))
    }
}
