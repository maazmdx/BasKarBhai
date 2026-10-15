package com.baskarbhai.roast.util

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Strict, robust filter for detecting Instagram Reels and YouTube Shorts surfaces.
 * Filters out DM chats, explore feeds, home timelines, standard videos, and comments.
 * Engineered by maazmdx.
 */
object FilterUtils {

    private const val TAG = "FilterUtils"
    private const val PACKAGE_INSTAGRAM = "com.instagram.android"
    private const val PACKAGE_YOUTUBE = "com.google.android.youtube"

    // Instagram DM markers — reject immediately
    private val INSTAGRAM_DM_MARKERS = setOf(
        "direct_thread", "direct_inbox", "direct_chat",
        "message_composer", "direct_message", "row_inbox",
        "direct_private", "direct_tab", "thread_title",
        "inbox_fragment", "direct_reply", "message_thread",
        "row_thread", "direct_voice_record"
    )

    // Instagram non-reels surfaces to reject
    private val INSTAGRAM_REJECT_MARKERS = setOf(
        "explore_grid", "feed_timeline", "profile_tab",
        "action_bar_container", "search_edit_text",
        "story_viewer", "media_group", "comment_composer"
    )

    // Instagram Reels-specific markers
    private val INSTAGRAM_REELS_IDS = setOf(
        "clips_viewer", "clips_video_container",
        "clips_media_item", "clips_root", "reel_viewer",
        "reel_swipe", "reel_animator", "reel_main_container",
        "clips_tab", "clips_surface", "clips_touch_container"
    )

    // YouTube Shorts-specific markers
    private val YOUTUBE_SHORTS_IDS = setOf(
        "shorts_player", "reel_recycler_view",
        "shorts_container", "reel_player_fragment",
        "reel_watch_fragment", "shorts_video_player",
        "shorts_shelf_item"
    )

    private val PAGER_CLASS_PATTERNS = setOf(
        "ViewPager", "ViewPager2", "RecyclerView"
    )

    /**
     * Determines if the active window is genuinely a full-screen vertical Reels/Shorts player.
     * Rejects DMs and non-reels surfaces, and confirms both pager and reels markers.
     */
    fun isReelsOrShortsSurface(rootNode: AccessibilityNodeInfo?, packageName: String?): Boolean {
        if (rootNode == null || packageName == null) return false
        if (packageName != PACKAGE_INSTAGRAM && packageName != PACKAGE_YOUTUBE) return false

        // Fast path 1: Reject DMs by native text / ID search
        if (packageName == PACKAGE_INSTAGRAM) {
            val dmSendNodes = rootNode.findAccessibilityNodeInfosByText("Message...")
            if (dmSendNodes.isNotEmpty()) {
                dmSendNodes.forEach { try { it.recycle() } catch (_: Exception) {} }
                return false
            }
        }

        // Fast path 2: Direct view ID match for known primary viewpagers
        val primaryIds = if (packageName == PACKAGE_INSTAGRAM) {
            listOf(
                "com.instagram.android:id/clips_viewer_view_pager",
                "com.instagram.android:id/clips_video_container",
                "com.instagram.android:id/clips_root"
            )
        } else {
            listOf(
                "com.google.android.youtube:id/shorts_player",
                "com.google.android.youtube:id/reel_recycler_view"
            )
        }

        for (id in primaryIds) {
            val matched = rootNode.findAccessibilityNodeInfosByViewId(id)
            if (matched.isNotEmpty()) {
                matched.forEach { try { it.recycle() } catch (_: Exception) {} }
                Log.d(TAG, "Fast confirmed $packageName Reels surface via $id")
                return true
            }
        }

        // Deep BFS search with high node limit for modern complex hierarchies
        var hasPagerNode = false
        var hasReelsShortsMarker = false

        val queue = ArrayDeque<Pair<AccessibilityNodeInfo, Int>>()
        queue.add(Pair(rootNode, 0))
        var visitCount = 0
        val maxVisit = 280

        while (queue.isNotEmpty() && visitCount < maxVisit) {
            val (node, depth) = queue.removeFirst()
            visitCount++

            val className = node.className?.toString().orEmpty()
            val viewId = node.viewIdResourceName?.lowercase().orEmpty()
            val contentDesc = node.contentDescription?.toString().orEmpty().lowercase()

            // Strict DM & non-reels rejection
            if (packageName == PACKAGE_INSTAGRAM) {
                if (INSTAGRAM_DM_MARKERS.any { viewId.contains(it) } ||
                    contentDesc.contains("send message") ||
                    contentDesc.contains("message...")
                ) {
                    return false
                }
                if (INSTAGRAM_REJECT_MARKERS.any { viewId.contains(it) }) {
                    return false
                }
            }

            // Check for vertical pager
            if (PAGER_CLASS_PATTERNS.any { className.contains(it) } && node.isScrollable) {
                hasPagerNode = true
            }

            // Check for Reels/Shorts markers
            if (packageName == PACKAGE_INSTAGRAM) {
                if (INSTAGRAM_REELS_IDS.any { viewId.contains(it) } ||
                    contentDesc == "reels" ||
                    contentDesc.startsWith("reel by")
                ) {
                    hasReelsShortsMarker = true
                }
            } else if (packageName == PACKAGE_YOUTUBE) {
                if (YOUTUBE_SHORTS_IDS.any { viewId.contains(it) } ||
                    viewId.contains("reel") ||
                    contentDesc == "shorts"
                ) {
                    hasReelsShortsMarker = true
                }
            }

            if (hasPagerNode && hasReelsShortsMarker) {
                Log.d(TAG, "Confirmed Reels/Shorts surface for $packageName")
                return true
            }

            if (depth < 12) {
                for (i in 0 until node.childCount) {
                    try {
                        val child = node.getChild(i)
                        if (child != null) {
                            queue.add(Pair(child, depth + 1))
                        }
                    } catch (_: Exception) {
                        // Node recycled
                    }
                }
            }
        }

        return hasPagerNode && hasReelsShortsMarker
    }
}
