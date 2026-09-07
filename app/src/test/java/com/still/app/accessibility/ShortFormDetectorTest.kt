package com.still.app.accessibility

import com.still.app.domain.SupportedApp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortFormDetectorTest {
    @Test
    fun instagramNavLabelAloneDoesNotTrigger() {
        val result = ShortFormDetector.detect(
            SupportedApp.INSTAGRAM,
            setOf("Home", "Search", "Reels", "Profile"),
        )

        assertFalse(result.isShortFormFeed)
    }

    @Test
    fun instagramHomePostControlsDoNotTrigger() {
        val result = ShortFormDetector.detect(
            SupportedApp.INSTAGRAM,
            setOf("Reels", "Like", "Comment", "Share", "Original audio"),
        )

        assertFalse(result.isShortFormFeed)
    }

    @Test
    fun selectedInstagramHomeDoesNotMakeReelsTrigger() {
        val result = ShortFormDetector.detect(
            SupportedApp.INSTAGRAM,
            labels = setOf("Home", "Search", "Reels", "Like", "Comment", "Share", "Original audio"),
            selectedLabels = setOf("Home"),
        )

        assertFalse(result.isShortFormFeed)
    }

    @Test
    fun selectedInstagramReelsTabWithControlsDoesNotTriggerWithoutViewer() {
        val result = ShortFormDetector.detect(
            SupportedApp.INSTAGRAM,
            labels = setOf("Home", "Reels", "Like", "Comment", "Share"),
            selectedLabels = setOf("Reels"),
        )

        assertFalse(result.isShortFormFeed)
    }

    @Test
    fun instagramReelsViewerIdWithControlsTriggers() {
        val result = ShortFormDetector.detect(
            SupportedApp.INSTAGRAM,
            labels = setOf("com.instagram.android:id/clips_viewer", "Reels", "Like", "Comment"),
            selectedLabels = setOf("Reels"),
        )

        assertTrue(result.isShortFormFeed)
    }

    @Test
    fun recycledViewerIdOnInstagramHomeDoesNotTrigger() {
        val result = ShortFormDetector.detect(
            SupportedApp.INSTAGRAM,
            labels = setOf("com.instagram.android:id/clips_viewer", "Home", "Reels", "Like", "Comment"),
            selectedLabels = setOf("Home"),
        )

        assertFalse(result.isShortFormFeed)
    }

    @Test
    fun differentReelContentProducesDifferentFingerprint() {
        val first = ShortFormDetector.detect(
            SupportedApp.INSTAGRAM,
            labels = setOf("Reels", "Like", "Comment", "@alex", "First caption"),
            selectedLabels = setOf("Reels"),
        )
        val second = ShortFormDetector.detect(
            SupportedApp.INSTAGRAM,
            labels = setOf("Reels", "Like", "Comment", "@sam", "Second caption"),
            selectedLabels = setOf("Reels"),
        )

        assertNotEquals(first.contentFingerprint, second.contentFingerprint)
    }

    @Test
    fun youtubeLongFormControlsDoNotTrigger() {
        val result = ShortFormDetector.detect(
            SupportedApp.YOUTUBE,
            setOf("Like this video", "Comments", "Subscribe"),
        )

        assertFalse(result.isShortFormFeed)
    }

    @Test
    fun youtubeBottomNavigationAndLongFormControlsDoNotTrigger() {
        val result = ShortFormDetector.detect(
            SupportedApp.YOUTUBE,
            labels = setOf("Home", "Shorts", "Like this video", "Comments", "Subscribe"),
            selectedLabels = setOf("Home"),
        )

        assertFalse(result.isShortFormFeed)
    }

    @Test
    fun selectedYoutubeShortsWithControlsTriggers() {
        val result = ShortFormDetector.detect(
            SupportedApp.YOUTUBE,
            labels = setOf("Home", "Shorts", "Like this video", "Comments"),
            selectedLabels = setOf("Shorts"),
        )

        assertTrue(result.isShortFormFeed)
    }

    @Test
    fun youtubeShortsViewerWithControlsTriggersWithoutSelectedTab() {
        val result = ShortFormDetector.detect(
            SupportedApp.YOUTUBE,
            labels = setOf(
                "com.google.android.youtube:id/reel_watch_player",
                "Like this video",
            ),
        )

        assertTrue(result.isShortFormFeed)
    }

    @Test
    fun tiktokInboxDoesNotTrigger() {
        val result = ShortFormDetector.detect(
            SupportedApp.TIKTOK,
            setOf("Inbox", "Messages", "Following"),
        )

        assertFalse(result.isShortFormFeed)
    }

    @Test
    fun tiktokProfileFollowingCountDoesNotTrigger() {
        val result = ShortFormDetector.detect(
            SupportedApp.TIKTOK,
            setOf("Profile", "123 Following", "Like", "Share"),
        )

        assertFalse(result.isShortFormFeed)
    }

    @Test
    fun tiktokForYouFeedWithControlsTriggers() {
        val result = ShortFormDetector.detect(
            SupportedApp.TIKTOK,
            setOf("For You", "Like", "Comment"),
        )

        assertTrue(result.isShortFormFeed)
    }
}
