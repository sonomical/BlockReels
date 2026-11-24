package com.blockreels.app.detection

import android.view.accessibility.AccessibilityEvent
import com.blockreels.app.disambiguation.DisambiguationEngine
import com.blockreels.app.engine.TraversalEngine
import com.blockreels.app.engine.TraversalStats
import com.blockreels.app.model.AccessibilityNode
import java.util.regex.Pattern
import kotlin.math.max
import kotlin.math.min

/** Detects the Reels viewer while preserving the home feed, Stories, and other screens. */
class InstagramDetector : AppDetector {

    override val targetPackages: Set<String> = setOf("com.instagram.android")
    override val platform: AppPlatform = AppPlatform.INSTAGRAM

    companion object {
        private const val ID_PREFIX = "com.instagram.android:id/"
        private const val FEED = ID_PREFIX + "feed_recycler_view"
        private const val PROFILE = ID_PREFIX + "unified_profile_layout"
        private const val STORY_TITLE = ID_PREFIX + "reel_viewer_title"
        private val STORY_VIEWERS = setOf(ID_PREFIX + "reel_viewer_tall_story_container",
            ID_PREFIX + "reel_viewer_root", ID_PREFIX + "reel_viewer_tall_shadow")
        private val EARLY_CONTROL_IDS = setOf(
            ID_PREFIX + "clips_author_container",
            ID_PREFIX + "clips_action_button"
        )
        private val PRIMARY_VIEW_IDS = setOf(
            ID_PREFIX + "clips_video_container",
            ID_PREFIX + "clips_viewer_view_pager"
        ) + EARLY_CONTROL_IDS
        private val DM_AND_SEARCH_IDS = setOf(
            ID_PREFIX + "row_thread_composer",
            ID_PREFIX + "row_thread_composer_edittext",
            ID_PREFIX + "direct_text_message_composer",
            ID_PREFIX + "action_bar_search_edit_text",
            ID_PREFIX + "explore_grid_recycler"
        )
        private val FALLBACK_ID_PATTERN = Pattern.compile(
            "^com\\.instagram\\.android:id/(clips_(viewer|video|ufi|action|author)_.*|reels_viewer_.*)$"
        )
        private val VIEWER_PHRASES = listOf(
            "reel by", "reel de", "reel par", "reel von", "reel do",
            "reels viewer", "reels player", "remix this reel", "مقطع ريلز"
        )
        private val VIEWER_DETAILS = VIEWER_PHRASES + listOf(
            "audio original", "original audio", "sonido original", "remixt",
            "ريلز", "リール", "릴스", "रील्स"
        )
        private val EMPTY_STATS = TraversalStats(0, 0, 0L)
    }

    private class ViewportEvidence {
        var clipsId: String? = null
        var detailSeen = false
    }

    private data class Branch(
        val viewport: ViewportEvidence?,
        val insideViewer: Boolean,
        val insideFeed: Boolean,
        val insideStory: Boolean
    )

    override fun evaluate(
        rootNode: AccessibilityNode?,
        event: AccessibilityEvent?,
        traversalEngine: TraversalEngine
    ): DetectionResult = evaluateWithTrace(rootNode, traversalEngine)

    internal fun evaluateWithTrace(
        rootNode: AccessibilityNode?,
        traversalEngine: TraversalEngine,
        observeNode: ((AccessibilityNode, Int) -> Unit)? = null,
        onViewerEvidence: (() -> Unit)? = null
    ): DetectionResult {
        if (rootNode == null || !rootNode.isVisibleToUser ||
            (rootNode.packageName != null && rootNode.packageName.toString() !in targetPackages)
        ) return DetectionResult.negative(platform)

        var positive: DetectionResult? = null
        var standaloneLabel: DetectionResult? = null
        var suppressed: SuppressionReason? = null
        var confirmedDmOrSearch: SuppressionReason? = null
        var fullProfile = false
        var focusedEditor = false
        var storyUi = false
        var foregroundStory = false
        var fullFeed = false
        val viewport = rootNode.getBoundsInScreenArray()
        val rootWidth = viewport[2] - viewport[0]
        val rootHeight = viewport[3] - viewport[1]
        val branches = arrayOfNulls<Branch>(traversalEngine.maxBudget.coerceAtLeast(0))

        fun accept(candidate: DetectionResult) {
            if (candidate.confidence.score > (positive?.confidence?.score ?: 0.0)) {
                if (positive == null && standaloneLabel == null) onViewerEvidence?.invoke()
                positive = candidate
            }
        }

        val stats = traversalEngine.traverse(rootNode, diagnosticVisitor = visit@{ node, depth, index, parent ->
            observeNode?.invoke(node, depth)
            val enclosing = parent?.let { branches[it] }
            if (!node.isVisibleToUser || (parent != null && enclosing == null)) return@visit
            branches[index] = enclosing ?: Branch(null, false, false, false)

            val bounds = node.getBoundsInScreenArray()
            val width = min(bounds[2], viewport[2]) - max(bounds[0], viewport[0])
            val height = min(bounds[3], viewport[3]) - max(bounds[1], viewport[1])
            if (width <= 0 || height <= 0) {
                branches[index] = null
                return@visit
            }
            val fullSize = width >= rootWidth * 0.85f && height >= rootHeight * 0.85f
            val id = node.viewIdResourceName
            val activePager = id == ID_PREFIX + "clips_viewer_view_pager" &&
                bounds.fillsVideoViewport(viewport)
            val suppression = DisambiguationEngine.checkSuppression(node, platform, rootWidth, rootHeight)
            if (suppression != null) {
                if (suppressed == null) suppressed = suppression
                if ((fullSize || node.isFocused) &&
                    (id in DM_AND_SEARCH_IDS || suppression == SuppressionReason.CHAT_THREAD_CONTAINER)) {
                    confirmedDmOrSearch = suppression
                }
                if (node.isFocused && suppression == SuppressionReason.CHAT_COMPOSER_ACTIVE) {
                    focusedEditor = true
                }
                if (id == PROFILE && fullSize) fullProfile = true
            }
            if (id == FEED && suppressed == null) {
                suppressed = SuppressionReason.STANDARD_FEED_SCRUBBER
            }
            if (id == FEED && fullSize) fullFeed = true

            val feed = (enclosing?.insideFeed == true || id == FEED) && !activePager
            val insideViewer = enclosing?.insideViewer == true
            val storyId = id?.startsWith(ID_PREFIX + "reel_viewer_") == true
            val story = !insideViewer && (enclosing?.insideStory == true || storyId)
            if (!feed && story) storyUi = true
            if (!feed && !insideViewer && fullSize && id in STORY_VIEWERS) foregroundStory = true

            val frame = enclosing?.viewport
            val viewerId = id?.startsWith(ID_PREFIX + "clips_viewer_") == true ||
                id?.startsWith(ID_PREFIX + "reels_viewer_") == true
            branches[index] = Branch(
                viewport = if (fullSize || activePager) ViewportEvidence() else frame,
                insideViewer = insideViewer || (!feed && !story && viewerId && (fullSize || activePager || frame != null)),
                insideFeed = feed,
                insideStory = story
            )
            if (feed) return@visit

            val primary = id != null && id in PRIMARY_VIEW_IDS
            if (!story && id != null && (fullSize || activePager || insideViewer)) {
                when {
                    primary -> accept(match(DetectionConfidence.DEFINITIVE, DetectionLayer.PRIMARY_RESOURCE_ID, id))
                    insideViewer && id == STORY_TITLE ->
                        accept(match(DetectionConfidence.DEFINITIVE, DetectionLayer.PRIMARY_RESOURCE_ID, id))
                    FALLBACK_ID_PATTERN.matcher(id).matches() ->
                        accept(match(DetectionConfidence.HIGH, DetectionLayer.FALLBACK_REGEX_ID, id))
                }
            }

            val description = node.displayDescription?.toString()?.lowercase()
            val text = node.displayText?.toString()?.lowercase()
            val detail = labelEvidence(description, text, VIEWER_DETAILS)
            if (insideViewer && detail != null) accept(detail)
            if (fullSize && !story) {
                val label = labelEvidence(description, text, VIEWER_PHRASES)
                val previous = standaloneLabel
                if (label != null && (previous == null || label.confidence.score > previous.confidence.score)) {
                    if (positive == null && previous == null) onViewerEvidence?.invoke()
                    standaloneLabel = label
                }
            }

            if (frame != null && !insideViewer) {
                if (id != null && id in EARLY_CONTROL_IDS && !story) {
                    val previous = frame.clipsId
                    if ((previous != null && previous != id) || frame.detailSeen) {
                        accept(match(DetectionConfidence.DEFINITIVE, DetectionLayer.PRIMARY_RESOURCE_ID, id))
                    }
                    frame.clipsId = id
                } else if ((detail != null && !story) || (id == STORY_TITLE && enclosing?.insideStory != true)) {
                    frame.clipsId?.let {
                        accept(match(DetectionConfidence.DEFINITIVE, DetectionLayer.PRIMARY_RESOURCE_ID, it))
                    }
                    frame.detailSeen = true
                }
            }
        }, shouldVisitChildren = { branches[it] != null }) { _, _ -> false }

        if (foregroundStory) return DetectionResult.suppressed(platform, SuppressionReason.STORY_VIEWER_ACTIVE,
            stats, foregroundConfirmed = true)
        confirmedDmOrSearch?.let { return DetectionResult.suppressed(platform, it, stats, foregroundConfirmed = true) }
        if (focusedEditor) return DetectionResult.suppressed(platform, SuppressionReason.CHAT_COMPOSER_ACTIVE, stats)
        positive?.takeIf { it.confidence == DetectionConfidence.DEFINITIVE }?.let { return it.copy(stats = stats) }
        if (fullProfile) {
            val complete = !stats.budgetExhausted && !stats.depthLimitReached
            return DetectionResult.suppressed(platform, SuppressionReason.PROFILE_HEADER_DETECTED,
                stats, foregroundConfirmed = complete)
        }
        suppressed?.let { return DetectionResult.suppressed(platform, it, stats,
            foregroundConfirmed = fullFeed && positive == null && standaloneLabel == null && !storyUi &&
                !stats.budgetExhausted && !stats.depthLimitReached) }
        val result = positive ?: standaloneLabel.takeUnless { storyUi }
        return result?.copy(stats = stats) ?: DetectionResult.negative(platform, stats)
    }

    private fun labelEvidence(description: String?, text: String?, phrases: List<String>): DetectionResult? {
        if (description != null && phrases.any { description.contains(it) }) {
            return match(DetectionConfidence.HIGH, DetectionLayer.CONTENT_DESCRIPTION, description)
        }
        return if (text != null && phrases.any { text.contains(it) }) {
            match(DetectionConfidence.MEDIUM, DetectionLayer.MULTILINGUAL_TEXT, text)
        } else null
    }

    private fun match(confidence: DetectionConfidence, layer: DetectionLayer, value: String): DetectionResult =
        DetectionResult.positive(platform, confidence, layer, value, EMPTY_STATS)
}
