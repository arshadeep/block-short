package com.still.app.accessibility

import android.graphics.Rect

/** Alternate evidence for versions that omit viewer IDs and tab selection. */
internal class InstagramViewerLayout(private val viewport: Rect) {
    private var hasHeader = false
    private val railControls = mutableMapOf<String, Int>()

    fun observe(labels: List<String>, bounds: Rect) {
        if (viewport.isEmpty || bounds.isEmpty || !Rect.intersects(viewport, bounds)) return
        if ("reels" in labels && bounds.bottom <= viewport.top + viewport.height() / 4) hasHeader = true
        if (bounds.centerX() < viewport.left + viewport.width() * 0.65f) return
        for ((control, aliases) in CONTROL_LABELS) {
            if (labels.any { label -> aliases.any { label == it || label.startsWith("$it,") || label.startsWith("$it ") } }) {
                railControls[control] = bounds.centerY()
            }
        }
    }

    val isViewer: Boolean
        get() {
            val positions = railControls.values.sorted()
            return hasHeader && positions.size == 3 && positions.zipWithNext().all { (a, b) ->
                b - a >= viewport.height() * 0.025f
            }
        }

    private companion object {
        val CONTROL_LABELS = mapOf(
            "like" to listOf("like", "unlike"),
            "comment" to listOf("comment", "comments"),
            "share" to listOf("share", "send"),
        )
    }
}
