package com.still.app.accessibility

import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.still.app.domain.SupportedApp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@Suppress("DEPRECATION")
@RunWith(AndroidJUnit4::class)
class VisibleNodeDetectionTest {
    @Test
    fun hiddenViewerCannotTriggerButSameVisibleViewerCan() {
        val node = AccessibilityNodeInfo.obtain().apply {
            packageName = "com.instagram.android"
            viewIdResourceName = "com.instagram.android:id/clips_viewer"
            contentDescription = "Like"
            isVisibleToUser = false
        }
        assertFalse(ShortFormDetector.detect(SupportedApp.INSTAGRAM, node).isShortFormFeed)
        node.isVisibleToUser = true
        assertTrue(ShortFormDetector.detect(SupportedApp.INSTAGRAM, node).isShortFormFeed)
    }

    @Test
    fun queuedInstagramEventCannotInspectAnotherAppsRoot() {
        val node = AccessibilityNodeInfo.obtain().apply {
            packageName = "com.still.app"
            viewIdResourceName = "com.instagram.android:id/clips_viewer"
            contentDescription = "Like"
            isVisibleToUser = true
        }
        assertFalse(ShortFormDetector.detect(SupportedApp.INSTAGRAM, node).isShortFormFeed)
    }
}
