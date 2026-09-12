package com.still.app.accessibility

import com.still.app.domain.SupportedApp
import org.junit.Assert.assertFalse
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
    fun selectedInstagramReelsTabWithControlsTriggersWithoutViewerId() {
        val result = ShortFormDetector.detect(
            SupportedApp.INSTAGRAM,
            labels = setOf("Home", "Reels", "Like", "Comment", "Share"),
            selectedLabels = setOf("Reels"),
        )

        assertTrue(result.isShortFormFeed)
    }

    @Test
    fun selectedReelsTabWithoutPlaybackControlsDoesNotTrigger() {
        assertFalse(ShortFormDetector.detect(SupportedApp.INSTAGRAM,
            setOf("Home", "Reels", "Original audio"), setOf("Reels")).isShortFormFeed)
    }

    @Test
    fun reelsViewerLayoutWorksWithoutIdsOrSelection() {
        assertTrue(ShortFormDetector.detect(SupportedApp.INSTAGRAM,
            setOf("Reels", "Like", "Comments", "Send"),
            hasReelsViewerLayout = true).isShortFormFeed)
    }

    @Test
    fun storyAndInboxStillVetoBothFallbacks() {
        for (safeScreen in listOf("Story by Alex", "Search or ask Meta AI", "Edit profile", "Your story", "Notifications")) {
            val result = ShortFormDetector.detect(SupportedApp.INSTAGRAM,
                setOf("Reels", "Like", "Comment", "Share", safeScreen), setOf("Reels"),
                hasReelsViewerLayout = true)
            assertFalse(safeScreen, result.isShortFormFeed)
            assertTrue(safeScreen, result.isKnownNonFeed)
        }
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
    fun instagramViewerTriggersBeforeSelectedTabStateArrives() {
        val result = ShortFormDetector.detect(
            SupportedApp.INSTAGRAM,
            labels = setOf("com.instagram.android:id/clips_viewer", "Like"),
        )

        assertTrue(result.isShortFormFeed)
    }

    @Test
    fun instagramStoryViewerNeverTriggersEvenWithStaleReelsSelection() {
        val result = ShortFormDetector.detect(
            SupportedApp.INSTAGRAM,
            labels = setOf(
                "com.instagram.android:id/reels_viewer",
                "com.instagram.android:id/story_progress",
                "Story by Alex",
                "Like",
                "Comment",
            ),
            selectedLabels = setOf("Reels"),
        )

        assertFalse(result.isShortFormFeed)
    }

    @Test
    fun ambiguousSingularReelViewerUsedByStoriesDoesNotTrigger() {
        val result = ShortFormDetector.detect(
            SupportedApp.INSTAGRAM,
            labels = setOf("com.instagram.android:id/reel_viewer", "Like", "Comment"),
            selectedLabels = setOf("Reels"),
        )

        assertFalse(result.isShortFormFeed)
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
    fun sharedVideoComponentDoesNotIdentifyReels() {
        for (id in listOf("clips_video", "clips_video_container", "clips_video_view")) {
            assertFalse(id, ShortFormDetector.detect(
                SupportedApp.INSTAGRAM,
                setOf("com.instagram.android:id/$id", "Like", "Comment", "Original audio"),
            ).isShortFormFeed)
        }
    }

    @Test
    fun permittedInstagramScreensVetoRetainedReelsViewer() {
        // Modeled after the visible screens in the recording, not device tree dumps.
        val screenMarkers = listOf(
            "Your story", "Notifications", "Edit profile", "Share profile",
            "Search or ask Meta AI", "com.instagram.android:id/direct_inbox",
            "com.instagram.android:id/direct_thread_recycler_view",
            "com.instagram.android:id/profile_header", "com.instagram.android:id/profile_grid",
            "com.instagram.android:id/feed_timeline", "com.instagram.android:id/story_progress",
            "com.instagram.android:id/reel_viewer",
        )
        for (screen in screenMarkers) {
            assertFalse(screen, ShortFormDetector.detect(
                SupportedApp.INSTAGRAM,
                setOf("com.instagram.android:id/clips_viewer", "Like", "Comment", screen),
            ).isShortFormFeed)
        }
    }

    @Test
    fun storyContainingSharedReelDoesNotTrigger() {
        assertFalse(ShortFormDetector.detect(
            SupportedApp.INSTAGRAM,
            setOf("com.instagram.android:id/clips_viewer", "Like", "Send message", "Watch full reel"),
        ).isShortFormFeed)
    }

    @Test
    fun selectedPermittedTabVetoesRetainedViewer() {
        for (tab in listOf("Profile", "Search", "Explore", "Inbox", "Direct", "Home, tab 1 of 5")) {
            assertFalse(tab, ShortFormDetector.detect(
                SupportedApp.INSTAGRAM,
                setOf("com.instagram.android:id/clips_viewer", "Like", "Comment"),
                selectedLabels = setOf(tab),
            ).isShortFormFeed)
        }
    }

    @Test
    fun fullScreenReelOpenedOutsideReelsTabStillTriggers() {
        assertTrue(ShortFormDetector.detect(
            SupportedApp.INSTAGRAM,
            setOf("com.instagram.android:id/clips_viewer_container", "Like", "Comment"),
        ).isShortFormFeed)
    }

    @Test
    fun notSelectedAndUnselectedAreNotSelectionEvidence() {
        assertFalse(ShortFormDetector.stateSaysSelected("Not selected"))
        assertFalse(ShortFormDetector.stateSaysSelected("Unselected"))
        assertFalse(ShortFormDetector.stateSaysSelected(null))
        assertTrue(ShortFormDetector.stateSaysSelected("Selected"))
        assertTrue(ShortFormDetector.stateSaysSelected("Selected, tab 2 of 5"))
    }

    @Test
    fun onlyInstagramIsSupported() {
        assertTrue(SupportedApp.fromPackage("com.instagram.android") == SupportedApp.INSTAGRAM)
        for (other in listOf("com.google.android.youtube", "com.zhiliaoapp.musically", "com.ss.android.ugc.trill", "com.still.app")) {
            assertTrue(SupportedApp.fromPackage(other) == null)
        }
    }
}
