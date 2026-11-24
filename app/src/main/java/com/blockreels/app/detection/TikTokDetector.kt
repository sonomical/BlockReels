package com.blockreels.app.detection

import android.view.accessibility.AccessibilityEvent
import com.blockreels.app.disambiguation.DisambiguationEngine
import com.blockreels.app.engine.TraversalEngine
import com.blockreels.app.model.AccessibilityNode
import kotlin.math.max
import kotlin.math.min

/** Feed-only protection. A package, generic pager or For You tab is never enough. */
class TikTokDetector : AppDetector {
    override val targetPackages = setOf(
        "com.zhiliaoapp.musically", "com.ss.android.ugc.trill",
        "com.ss.android.ugc.aweme", "com.ss.android.ugc.aweme.lite"
    )
    override val platform = AppPlatform.TIKTOK

    companion object {
        private val FEED_IDS = setOf("main_feed_container", "vertical_viewpager")
        internal fun isFeedViewerId(viewId: String?): Boolean =
            viewId?.substringAfter(":id/", "") in FEED_IDS || viewId?.endsWith(":id/viewpager") == true ||
                viewId?.endsWith(":id/view_pager_layout_wrapper") == true
        private val PLAYBACK_PHRASES = listOf(
            "swipe up for next video", "desliza hacia arriba para ver el siguiente video",
            "faire glisser vers le haut", "nach oben wischen", "deslize para cima",
            "اسحب للأعلى للفيديو التالي", "अगले वीडियो के लिए ऊपर स्वाइप करें",
            "上にスワイプして次の動画へ", "위로 스와이프하여 다음 동영상 보기",
            "geser ke atas untuk video berikutnya"
        )
    }

    override fun evaluate(rootNode: AccessibilityNode?, event: AccessibilityEvent?,
                          traversalEngine: TraversalEngine): DetectionResult = evaluateWithTrace(rootNode, traversalEngine)

    internal fun evaluateWithTrace(rootNode: AccessibilityNode?, traversalEngine: TraversalEngine,
                                   observeNode: ((AccessibilityNode, Int) -> Unit)? = null): DetectionResult {
        val pkg = rootNode?.packageName?.toString()
        if (rootNode == null || !rootNode.isVisibleToUser || pkg !in targetPackages) {
            return DetectionResult.negative(platform)
        }
        val viewport = rootNode.getBoundsInScreenArray()
        val prefix = "$pkg:id/"
        val viewerBranches = BooleanArray(traversalEngine.maxBudget.coerceAtLeast(0))
        val hiddenBranches = BooleanArray(viewerBranches.size)
        var viewer: String? = null
        var videoInViewer = false
        var explicitPlayback = false
        var legacyFeed: String? = null
        var suppression: SuppressionReason? = null
        var confirmedSurface = false
        var weakSuppression: SuppressionReason? = null

        val stats = traversalEngine.traverse(rootNode, diagnosticVisitor = visit@{ node, depth, index, parent ->
            observeNode?.invoke(node, depth)
            val ancestorHidden = parent?.let { hiddenBranches[it] } == true
            val insideViewer = parent?.let { viewerBranches[it] } == true
            val id = node.viewIdResourceName
            val localId = id?.takeIf { it.startsWith(prefix) }?.removePrefix(prefix)
            if (!ancestorHidden && insideViewer && localId == "long_press_layout") videoInViewer = true
            hiddenBranches[index] = ancestorHidden || !node.isVisibleToUser
            viewerBranches[index] = insideViewer
            if (hiddenBranches[index]) return@visit
            val bounds = node.getBoundsInScreenArray()
            if (min(bounds[2], viewport[2]) <= max(bounds[0], viewport[0]) ||
                min(bounds[3], viewport[3]) <= max(bounds[1], viewport[1])) {
                hiddenBranches[index] = true
                return@visit
            }

            val full = bounds.fillsVideoViewport(viewport, minWidth = 0.9f)
            val reason = DisambiguationEngine.checkSuppression(node, platform)
            if (reason != null) {
                weakSuppression = weakSuppression ?: reason
                val foreground = when (reason) {
                    SuppressionReason.CHAT_THREAD_CONTAINER -> full
                    SuppressionReason.PROFILE_HEADER_DETECTED -> !insideViewer && localId == "profile_header"
                    SuppressionReason.SEARCH_EXPLORE_GRID_ACTIVE -> true
                    SuppressionReason.CREATOR_CAMERA_ACTIVE -> full
                    else -> false
                }
                if (foreground || (node.isFocused && reason == SuppressionReason.CHAT_COMPOSER_ACTIVE)) {
                    suppression = reason
                    confirmedSurface = foreground
                }
            }
            if (full && localId in FEED_IDS) legacyFeed = id
            if (full && (localId == "view_pager_layout_wrapper" || localId == "viewpager")) {
                viewer = id
                viewerBranches[index] = true
            }
            if (insideViewer || full) {
                val description = node.displayDescription?.toString()?.lowercase().orEmpty()
                if (PLAYBACK_PHRASES.any { description.contains(it) }) explicitPlayback = true
            }
        }, shouldVisitChildren = { !hiddenBranches[it] }) { _, _ -> false }

        suppression?.let {
            return DetectionResult.suppressed(platform, it, stats, foregroundConfirmed = confirmedSurface)
        }
        val matched = legacyFeed ?: viewer?.takeIf { videoInViewer || explicitPlayback }
        if (matched != null) return DetectionResult.positive(platform, DetectionConfidence.DEFINITIVE,
            DetectionLayer.PRIMARY_RESOURCE_ID, matched, stats)
        weakSuppression?.let { return DetectionResult.suppressed(platform, it, stats) }
        return DetectionResult.negative(platform, stats)
    }
}
