package com.still.app.accessibility

import android.os.Build
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.still.app.domain.SupportedApp
import java.util.ArrayDeque

data class DetectionResult(
    val isShortFormFeed: Boolean,
    val confidence: Int,
    val evidence: Set<String>,
    val isKnownNonFeed: Boolean = false,
)

private data class AccessibilitySnapshot(
    val labels: Set<String>,
    val selectedLabels: Set<String>,
    val hasReelsViewerLayout: Boolean,
)

private data class NodeVisit(
    val node: AccessibilityNodeInfo,
    val depth: Int,
    val selectedSingleChildParent: Boolean = false,
)

object ShortFormDetector {
    private const val MAX_NODES = 450

    fun detect(app: SupportedApp, root: AccessibilityNodeInfo?): DetectionResult {
        if (root == null || root.packageName?.toString() !in app.packageNames) {
            return DetectionResult(false, 0, emptySet())
        }
        val snapshot = collectSnapshot(root)
        return detect(app, snapshot.labels, snapshot.selectedLabels, snapshot.hasReelsViewerLayout)
    }

    internal fun detect(
        app: SupportedApp,
        labels: Set<String>,
        selectedLabels: Set<String> = emptySet(),
        hasReelsViewerLayout: Boolean = false,
    ): DetectionResult {
        val normalized = labels.mapTo(mutableSetOf()) { it.lowercase().trim() }
        val selected = selectedLabels.mapTo(mutableSetOf()) { it.lowercase().trim() }
        val evidence = linkedSetOf<String>()
        var isKnownNonFeed = false

        fun contains(fragment: String): Boolean {
            val match = normalized.firstOrNull { fragment in it }
            if (match != null) evidence += match
            return match != null
        }

        fun containsExact(label: String): Boolean {
            val match = normalized.firstOrNull {
                it == label || it.startsWith("$label,")
            }
            if (match != null) evidence += match
            return match != null
        }

        val score = when (app) {
            SupportedApp.INSTAGRAM -> {
                val ids = normalized.filter { it.startsWith("com.instagram.android:id/") }
                    .map { it.substringAfter(":id/") }
                fun hasId(prefix: String) = ids.any { it == prefix || it.startsWith("${prefix}_") }

                // A video player is reused outside Reels. Accept either a viewer
                // container, the selected Reels tab with controls, or the viewer's
                // header and vertical action rail. IDs are not stable across versions.
                val hasStoryContext = normalized.any {
                    "story_viewer" in it ||
                        "stories_viewer" in it ||
                        "story_progress" in it ||
                        it == "stories" ||
                        it.startsWith("story by ")
                } || hasId("reel_viewer") ||
                    (containsExact("send message") && containsExact("watch full reel"))
                val safeScreenVisible = listOf(
                    "direct_inbox", "direct_thread", "profile_header", "profile_grid",
                    "feed_timeline", "main_feed", "newsfeed", "notification_list",
                ).any(::hasId) || normalized.any {
                    it == "your story" || it == "edit profile" || it == "share profile" ||
                        it == "search or ask meta ai" || it == "notifications"
                }
                val nonReelsTabSelected = selected.any { label ->
                    listOf("home", "search", "explore", "profile", "direct", "inbox").any { tab ->
                        label == tab || label.startsWith("$tab,") ||
                            (label.startsWith("com.instagram.android:id/") && "tab" in label && tab in label)
                    }
                }
                val hasViewerId = hasId("clips_viewer") || hasId("reels_viewer")
                val reelsTabSelected = selected.any { label ->
                    label == "reels" || label.startsWith("reels,") ||
                        (label.startsWith("com.instagram.android:id/") && "tab" in label && "clips" in label)
                }
                if (hasViewerId) evidence += ids.filter { "viewer" in it }
                isKnownNonFeed = hasStoryContext || safeScreenVisible || nonReelsTabSelected
                val reelContext = (hasViewerId || reelsTabSelected || hasReelsViewerLayout) && !isKnownNonFeed
                if (reelsTabSelected) evidence += "selected_reels_tab"
                if (hasReelsViewerLayout) evidence += "reels_header_and_action_rail"
                val controls = listOf(
                    contains("like"), contains("comment"), contains("share") || containsExact("send"),
                ).count { it }
                var value = if (reelContext) {
                    if (hasViewerId || hasReelsViewerLayout) 4 else 3
                } else 0
                value += controls
                if (contains("use audio") || contains("original audio")) value += 2
                if (reelContext && (hasViewerId || hasReelsViewerLayout || controls >= 2)) value else 0
            }

        }

        return DetectionResult(
            isShortFormFeed = score >= 5,
            confidence = score,
            evidence = evidence,
            isKnownNonFeed = isKnownNonFeed,
        )
    }

    private fun collectSnapshot(root: AccessibilityNodeInfo): AccessibilitySnapshot {
        val labels = linkedSetOf<String>()
        val selectedLabels = linkedSetOf<String>()
        val queue = ArrayDeque<NodeVisit>()
        val viewport = Rect().also(root::getBoundsInScreen)
        val viewerLayout = InstagramViewerLayout(viewport)
        queue.add(NodeVisit(root, depth = 0))
        var visited = 0

        while (queue.isNotEmpty() && visited++ < MAX_NODES) {
            val visit = queue.removeFirst()
            val node = visit.node
            val stateSelected = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                stateSaysSelected(node.stateDescription?.toString())
            // A selected single-child wrapper may label one tab. Never propagate
            // selection from a multi-child navigation bar to all of its tabs.
            val nodeSaysSelected = visit.depth > 0 &&
                (node.isSelected || nodeIsChecked(node) || stateSelected || visit.selectedSingleChildParent ||
                    stateSaysSelected(node.contentDescription?.toString()))
            val nodeLabels = listOfNotNull(
                node.text?.toString()?.takeIf(String::isNotBlank),
                node.contentDescription?.toString()?.takeIf(String::isNotBlank),
                node.viewIdResourceName?.takeIf(String::isNotBlank),
            )
            // Instagram retains hidden tabs and viewers in its accessibility tree.
            // Traverse their children, but never use hidden nodes as screen evidence.
            if (node.isVisibleToUser) {
                labels += nodeLabels
                if (nodeSaysSelected) selectedLabels += nodeLabels
                viewerLayout.observe(
                    listOfNotNull(node.text, node.contentDescription).map { it.toString().trim().lowercase() },
                    Rect().also(node::getBoundsInScreen),
                )
            }
            for (index in 0 until node.childCount) {
                node.getChild(index)?.let { child ->
                    queue.addLast(
                        NodeVisit(
                            node = child,
                            depth = visit.depth + 1,
                            selectedSingleChildParent = node.childCount == 1 && nodeSaysSelected,
                        ),
                    )
                }
            }
        }
        return AccessibilitySnapshot(labels, selectedLabels, viewerLayout.isViewer)
    }

    internal fun stateSaysSelected(description: String?): Boolean =
        description?.split(',')?.any { it.trim().equals("selected", ignoreCase = true) } == true

    @Suppress("DEPRECATION")
    private fun nodeIsChecked(node: AccessibilityNodeInfo): Boolean =
        if (Build.VERSION.SDK_INT >= 36) {
            node.getChecked() == AccessibilityNodeInfo.CHECKED_STATE_TRUE
        } else {
            node.isChecked
        }
}
