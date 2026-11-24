package com.blockreels.app.detection

import android.view.accessibility.AccessibilityEvent
import com.blockreels.app.disambiguation.DisambiguationEngine
import com.blockreels.app.engine.TraversalEngine
import com.blockreels.app.engine.TraversalStats
import com.blockreels.app.model.AccessibilityNode
import java.util.regex.Pattern
import kotlin.math.max
import kotlin.math.min

/**
 * Detects only the full-screen Shorts player. Home-feed Shorts shelves and the
 * Shorts tab button are deliberately ignored so regular long-form videos and
 * the rest of YouTube stay usable.
 */
class YouTubeDetector : AppDetector {

    override val targetPackages: Set<String> = setOf("com.google.android.youtube")
    override val platform: AppPlatform = AppPlatform.YOUTUBE

    companion object {
        private const val ID_PREFIX = "com.google.android.youtube:id/"
        private val PRIMARY_VIEW_IDS = setOf(
            "com.google.android.youtube:id/reel_watch_fragment_root",
            "com.google.android.youtube:id/reel_recycler",
            "com.google.android.youtube:id/reel_video",
            "com.google.android.youtube:id/reel_player_view",
            "com.google.android.youtube:id/shorts_container",
            "com.google.android.youtube:id/reel_player_page_view",
            "com.google.android.youtube:id/reel_swipe_layout"
        )

        private val FALLBACK_ID_PATTERN = Pattern.compile(
            "^com\\.google\\.android\\.youtube:id/(?:(reel_(?:recycler|video|player|swipe|watch_).*|" +
                "shorts_(?:container|player|viewer).*)|reel_(?:progress_bar|sound_title|like_button|comment_button).*)$"
        )

        private val FOREGROUND_CONTEXT_IDS = setOf(
            "${ID_PREFIX}search_edit_text",
            "${ID_PREFIX}comment_composer",
            "${ID_PREFIX}comment_edit_text",
            "${ID_PREFIX}create_comment_view",
            "${ID_PREFIX}channel_header",
            "${ID_PREFIX}channel_banner",
            "${ID_PREFIX}inline_time_bar"
        )

        private val VIEWER_DESC_KEYWORDS = listOf(
            "shorts player", "shorts viewer", "shorts feed",
            "reproductor de shorts", "lecteur shorts", "shorts-player",
            "player de shorts", "مشغل الفيديوهات القصيرة",
            "शॉर्ट्स प्लेयर", "ショートプレーヤー", "shorts 플레이어",
            "pemutar shorts"
        )

        private val SHORTS_LABELS = listOf("فيديوهات قصيرة", "शॉर्ट्स", "ショート", "쇼츠")
        private val EMPTY_STATS = TraversalStats(0, 0, 0L)
    }

    override fun evaluate(
        rootNode: AccessibilityNode?,
        event: AccessibilityEvent?,
        traversalEngine: TraversalEngine
    ): DetectionResult = evaluateWithTrace(rootNode, event, traversalEngine)

    internal fun evaluateWithTrace(
        rootNode: AccessibilityNode?,
        event: AccessibilityEvent?,
        traversalEngine: TraversalEngine,
        observeNode: ((AccessibilityNode, Int) -> Unit)? = null
    ): DetectionResult {
        if (rootNode == null || !rootNode.isVisibleToUser ||
            (rootNode.packageName != null && rootNode.packageName.toString() !in targetPackages)
        ) return DetectionResult.negative(platform)

        var positive: DetectionResult? = null
        var suppressionReason: SuppressionReason? = null
        var foregroundSuppression: SuppressionReason? = null
        val viewport = rootNode.getBoundsInScreenArray()
        val viewerBounds = arrayOfNulls<IntArray>(traversalEngine.maxBudget.coerceAtLeast(0))
        val hidden = BooleanArray(viewerBounds.size)

        val stats = traversalEngine.traverse(rootNode, diagnosticVisitor = visit@{ node, depth, index, parent ->
            observeNode?.invoke(node, depth)
            hidden[index] = !node.isVisibleToUser || (parent != null && hidden[parent])
            if (hidden[index]) return@visit
            val bounds = node.getBoundsInScreenArray()
            val enclosingViewer = parent?.let { viewerBounds[it] }
            viewerBounds[index] = enclosingViewer
            if (!intersectsViewport(bounds, viewport)) {
                hidden[index] = true
                return@visit
            }

            val viewId = node.viewIdResourceName
            val timeBar = viewId?.startsWith("${ID_PREFIX}time_bar_") == true
            val suppression = if (timeBar) SuppressionReason.STANDARD_FEED_SCRUBBER
                else DisambiguationEngine.checkSuppression(node, platform)
            if (suppression != null) {
                if (suppressionReason == null) suppressionReason = suppression
                if (foregroundSuppression == null &&
                    (timeBar || viewId in FOREGROUND_CONTEXT_IDS ||
                        (node.isFocused && suppression == SuppressionReason.CHAT_COMPOSER_ACTIVE))) {
                    foregroundSuppression = suppression
                }
            }

            val fullSize = fillsViewport(bounds, viewport) ||
                (viewId == ID_PREFIX + "reel_recycler" && bounds.fillsVideoViewport(viewport))
            val insideViewer = enclosingViewer != null && intersectsViewport(bounds, enclosingViewer)
            if (!fullSize && !insideViewer) return@visit

            val primary = viewId in PRIMARY_VIEW_IDS
            val fallback = viewId?.let { FALLBACK_ID_PATTERN.matcher(it) }?.takeIf { it.matches() }
            val containerId = primary || fallback?.group(1) != null
            val description = node.displayDescription?.toString()?.lowercase()
                ?.takeIf { value -> VIEWER_DESC_KEYWORDS.any { value.contains(it) } }
            val text = node.displayText?.toString()?.lowercase()
            val viewerText = text != null && VIEWER_DESC_KEYWORDS.any { text.contains(it) }
            if (fullSize && (containerId || description != null || viewerText)) {
                viewerBounds[index] = bounds
            }

            val candidate = when {
                primary -> match(DetectionConfidence.DEFINITIVE, DetectionLayer.PRIMARY_RESOURCE_ID, viewId!!)
                fallback != null && (containerId || insideViewer) ->
                    match(DetectionConfidence.HIGH, DetectionLayer.FALLBACK_REGEX_ID, viewId!!)
                description != null ->
                    match(DetectionConfidence.HIGH, DetectionLayer.CONTENT_DESCRIPTION, description)
                text != null && (viewerText || (insideViewer && SHORTS_LABELS.any { text.contains(it) })) ->
                    match(DetectionConfidence.MEDIUM, DetectionLayer.MULTILINGUAL_TEXT, text)
                else -> null
            }
            if (candidate != null && candidate.confidence.score > (positive?.confidence?.score ?: 0.0)) {
                positive = candidate
            }
        }, shouldVisitChildren = { !hidden[it] }) { _, _ -> false }

        foregroundSuppression?.let {
            return DetectionResult.suppressed(platform, it, stats, foregroundConfirmed = true)
        }
        if (positive?.confidence == DetectionConfidence.DEFINITIVE) return positive!!.copy(stats = stats)
        suppressionReason?.let { return DetectionResult.suppressed(platform, it, stats,
            foregroundConfirmed = positive == null && !stats.budgetExhausted && !stats.depthLimitReached) }
        return positive?.copy(stats = stats) ?: DetectionResult.negative(platform, stats)
    }

    private fun match(confidence: DetectionConfidence, layer: DetectionLayer, value: String): DetectionResult =
        DetectionResult.positive(platform, confidence, layer, value, EMPTY_STATS)

    private fun intersectsViewport(b: IntArray, r: IntArray): Boolean {
        return min(b[2], r[2]) > max(b[0], r[0]) && min(b[3], r[3]) > max(b[1], r[1])
    }

    private fun fillsViewport(b: IntArray, r: IntArray): Boolean {
        val width = r[2] - r[0]
        val height = r[3] - r[1]
        return width > 0 && height > 0 &&
            min(b[2], r[2]) - max(b[0], r[0]) >= width * 0.85f &&
            min(b[3], r[3]) - max(b[1], r[1]) >= height * 0.85f
    }
}
