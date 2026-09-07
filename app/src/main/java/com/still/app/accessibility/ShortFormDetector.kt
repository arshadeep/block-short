package com.still.app.accessibility

import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import com.still.app.domain.SupportedApp
import java.util.ArrayDeque

data class DetectionResult(
    val isShortFormFeed: Boolean,
    val confidence: Int,
    val evidence: Set<String>,
    val contentFingerprint: Int,
)

private data class AccessibilitySnapshot(
    val labels: Set<String>,
    val selectedLabels: Set<String>,
)

private data class NodeVisit(
    val node: AccessibilityNodeInfo,
    val depth: Int,
)

object ShortFormDetector {
    private const val MAX_NODES = 450

    fun detect(app: SupportedApp, root: AccessibilityNodeInfo?): DetectionResult {
        if (root == null) return DetectionResult(false, 0, emptySet(), 0)
        val snapshot = collectSnapshot(root)
        return detect(app, snapshot.labels, snapshot.selectedLabels)
    }

    internal fun detect(
        app: SupportedApp,
        labels: Set<String>,
        selectedLabels: Set<String> = emptySet(),
    ): DetectionResult {
        val normalized = labels.mapTo(mutableSetOf()) { it.lowercase().trim() }
        val selected = selectedLabels.mapTo(mutableSetOf()) { it.lowercase().trim() }
        val evidence = linkedSetOf<String>()

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
                // Instagram Home contains a Reels navigation label and the same post
                // controls as Reels. Stories have historically used singular
                // "reel_viewer" ids internally, so those ambiguous ids are never
                // accepted as evidence for the Reels product surface.
                val hasStoryContext = normalized.any {
                    "story_viewer" in it ||
                        "stories_viewer" in it ||
                        "story_progress" in it ||
                        it == "stories" ||
                        it.startsWith("story by ")
                }
                val hasViewerId = contains("clips_viewer") ||
                    contains("clips_video") ||
                    contains("reels_viewer")
                val homeTabDirectlySelected = selected.any {
                    it == "home" || it.startsWith("home,") || ("tab" in it && "home" in it)
                }
                // Viewer ids appear before Instagram reliably marks the Reels tab
                // selected. Acting on the strong viewer id avoids a late block.
                val reelContext = hasViewerId &&
                    !hasStoryContext &&
                    !homeTabDirectlySelected
                var value = if (reelContext) 4 else 0
                if (contains("like")) value += 1
                if (contains("comment")) value += 1
                if (contains("share")) value += 1
                if (contains("use audio") || contains("original audio")) value += 2
                if (reelContext) value else 0
            }

            SupportedApp.YOUTUBE -> {
                // The word "Shorts" is always present in YouTube's bottom
                // navigation and must not identify the active screen by itself.
                val hasShortsViewerId = contains("reel_watch") ||
                    contains("shorts_player") ||
                    contains("shorts_container") ||
                    contains("reel_recycler")
                val shortsTabDirectlySelected = selected.any {
                    it == "shorts" || it.startsWith("shorts,") || ("shorts" in it && "tab" in it)
                }
                val nonShortsTabSelected = selected.any {
                    it == "home" || it.startsWith("home,") ||
                        it == "subscriptions" || it.startsWith("subscriptions,") ||
                        it == "you" || it.startsWith("you,")
                }
                val shortsContext = (hasShortsViewerId || shortsTabDirectlySelected) &&
                    !nonShortsTabSelected
                var value = if (shortsContext) 3 else 0
                if (contains("like this video") || contains("dislike this video")) value += 2
                if (contains("comments")) value += 1
                if (contains("subscribe")) value += 1
                if (contains("sound")) value += 1
                if (shortsContext) value else 0
            }

            SupportedApp.TIKTOK -> {
                var value = 0
                // Profile statistics such as "123 Following" used to satisfy
                // the broad substring check. Only actual feed-tab labels count.
                if (containsExact("for you") || containsExact("following")) value += 3
                if (contains("like")) value += 1
                if (contains("comment")) value += 1
                if (contains("share")) value += 1
                if (contains("sound") || contains("original sound")) value += 2
                value
            }
        }

        return DetectionResult(
            isShortFormFeed = score >= 5,
            confidence = score,
            evidence = evidence,
            contentFingerprint = contentFingerprint(normalized),
        )
    }

    private fun contentFingerprint(labels: Set<String>): Int {
        val genericFragments = listOf(
            "com.instagram.android:id/",
            "com.google.android.youtube:id/",
            "reels",
            "shorts",
            "for you",
            "following",
            "home",
            "search",
            "profile",
            "like",
            "comment",
            "share",
            "subscribe",
            "audio",
            "sound",
            "more",
        )
        return labels.asSequence()
            .filter { label -> label.length >= 3 }
            .filterNot { label -> label.all(Char::isDigit) }
            .filterNot { label -> genericFragments.any { fragment -> fragment in label } }
            .sorted()
            .joinToString(separator = "|")
            .hashCode()
    }

    private fun collectSnapshot(root: AccessibilityNodeInfo): AccessibilitySnapshot {
        val labels = linkedSetOf<String>()
        val selectedLabels = linkedSetOf<String>()
        val queue = ArrayDeque<NodeVisit>()
        queue.add(NodeVisit(root, depth = 0))
        var visited = 0

        while (queue.isNotEmpty() && visited++ < MAX_NODES) {
            val visit = queue.removeFirst()
            val node = visit.node
            val stateSaysSelected = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                node.stateDescription?.toString()?.contains("selected", ignoreCase = true) == true
            // Only the node itself may contribute selected labels. Propagating a
            // parent's selected state makes every bottom-navigation tab look selected.
            val nodeSaysSelected = visit.depth > 0 &&
                (node.isSelected || nodeIsChecked(node) || stateSaysSelected)
            val nodeLabels = listOfNotNull(
                node.text?.toString()?.takeIf(String::isNotBlank),
                node.contentDescription?.toString()?.takeIf(String::isNotBlank),
                node.viewIdResourceName?.takeIf(String::isNotBlank),
            )
            labels += nodeLabels
            if (nodeSaysSelected) selectedLabels += nodeLabels
            for (index in 0 until node.childCount) {
                node.getChild(index)?.let { child ->
                    queue.addLast(
                        NodeVisit(
                            node = child,
                            depth = visit.depth + 1,
                        ),
                    )
                }
            }
        }
        return AccessibilitySnapshot(labels, selectedLabels)
    }

    @Suppress("DEPRECATION")
    private fun nodeIsChecked(node: AccessibilityNodeInfo): Boolean =
        if (Build.VERSION.SDK_INT >= 36) {
            node.getChecked() == AccessibilityNodeInfo.CHECKED_STATE_TRUE
        } else {
            node.isChecked
        }
}
