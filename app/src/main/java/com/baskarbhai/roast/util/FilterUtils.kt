package com.baskarbhai.roast.util

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Intelligent filter for detecting Instagram Reels and YouTube Shorts surfaces
 * while ignoring DM threads, inbox scrolls, feeds, and normal videos.
 * Engineered by mdx.
 */
object FilterUtils {

    private const val PACKAGE_INSTAGRAM = "com.instagram.android"
    private const val PACKAGE_YOUTUBE = "com.google.android.youtube"

    // Instagram DM markers that strictly invalidate counting
    private val INSTAGRAM_DM_IDENTIFIERS = listOf(
        "direct_thread",
        "direct_inbox",
        "direct_chat",
        "message_composer",
        "direct_message",
        "row_inbox",
        "direct_private",
        "direct_tab",
        "thread_title"
    )

    // Markers indicating Reels surface
    private val REELS_IDENTIFIERS = listOf(
        "reel_viewer",
        "clips_video_container",
        "reel_main_container",
        "clips_viewer",
        "clips_media_item",
        "clips_root",
        "reel_swipe",
        "reel_animator"
    )

    // Markers indicating YouTube Shorts surface
    private val SHORTS_IDENTIFIERS = listOf(
        "shorts_player",
        "reel_recycler_view",
        "shorts_container",
        "reel_player_fragment",
        "reel_watch_fragment",
        "shorts_shelf_item",
        "shorts_video_player"
    )

    /**
     * Inspects the view hierarchy to determine if the active window is genuinely a Reels/Shorts screen.
     */
    fun isReelsOrShortsSurface(rootNode: AccessibilityNodeInfo?, packageName: String?): Boolean {
        if (rootNode == null || packageName == null) return false

        // 1. Guard against Instagram DMs
        if (packageName == PACKAGE_INSTAGRAM && isInstagramDmSurface(rootNode)) {
            return false
        }

        // 2. Perform deep inspection for full-screen vertical pagers & Reels/Shorts nodes
        var hasVerticalPagerNode = false
        var hasReelsOrShortsMarker = false

        val nodesToVisit = ArrayDeque<Pair<AccessibilityNodeInfo, Int>>()
        nodesToVisit.add(Pair(rootNode, 0))
        var visitedCount = 0
        val maxNodes = 120

        while (nodesToVisit.isNotEmpty() && visitedCount < maxNodes) {
            val (current, depth) = nodesToVisit.removeFirst()
            visitedCount++

            val className = current.className?.toString().orEmpty()
            val viewId = current.viewIdResourceName?.lowercase().orEmpty()
            val contentDesc = current.contentDescription?.toString().orEmpty().lowercase()
            val text = current.text?.toString().orEmpty().lowercase()

            // Check for vertical pager components
            if (className.contains("ViewPager") ||
                className.contains("RecyclerView") ||
                current.isScrollable
            ) {
                hasVerticalPagerNode = true
            }

            // Check for Instagram Reels markers
            if (packageName == PACKAGE_INSTAGRAM) {
                if (REELS_IDENTIFIERS.any { viewId.contains(it) } ||
                    contentDesc.contains("reels") ||
                    text.contains("reels") ||
                    contentDesc.contains("audio used") ||
                    contentDesc.contains("original audio")
                ) {
                    hasReelsOrShortsMarker = true
                }
            }

            // Check for YouTube Shorts markers
            if (packageName == PACKAGE_YOUTUBE) {
                if (SHORTS_IDENTIFIERS.any { viewId.contains(it) } ||
                    contentDesc.contains("shorts") ||
                    text.contains("shorts") ||
                    contentDesc.contains("remix this video") ||
                    viewId.contains("reel")
                ) {
                    hasReelsOrShortsMarker = true
                }
            }

            if (hasVerticalPagerNode && hasReelsOrShortsMarker) {
                return true
            }

            // Traverse children up to max depth of 12
            if (depth < 12) {
                for (i in 0 until current.childCount) {
                    val child = current.getChild(i)
                    if (child != null) {
                        nodesToVisit.add(Pair(child, depth + 1))
                    }
                }
            }
        }

        // Fallback: If strong marker is found, consider it active
        return hasReelsOrShortsMarker
    }

    private fun isInstagramDmSurface(rootNode: AccessibilityNodeInfo): Boolean {
        val queue = ArrayDeque<Pair<AccessibilityNodeInfo, Int>>()
        queue.add(Pair(rootNode, 0))
        var count = 0

        while (queue.isNotEmpty() && count < 60) {
            val (node, depth) = queue.removeFirst()
            count++

            val viewId = node.viewIdResourceName?.lowercase().orEmpty()
            val contentDesc = node.contentDescription?.toString().orEmpty().lowercase()
            val text = node.text?.toString().orEmpty().lowercase()

            if (INSTAGRAM_DM_IDENTIFIERS.any { viewId.contains(it) } ||
                text.contains("direct") ||
                contentDesc.contains("send message") ||
                contentDesc.contains("message...")
            ) {
                return true
            }

            if (depth < 8) {
                for (i in 0 until node.childCount) {
                    val child = node.getChild(i)
                    if (child != null) {
                        queue.add(Pair(child, depth + 1))
                    }
                }
            }
        }
        return false
    }
}
