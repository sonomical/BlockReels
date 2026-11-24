package com.blockreels.app.detection

import android.view.accessibility.AccessibilityEvent
import com.blockreels.app.disambiguation.DisambiguationEngine
import com.blockreels.app.engine.TraversalEngine
import com.blockreels.app.engine.TraversalStats
import com.blockreels.app.model.AccessibilityNode
import java.util.Locale
import java.util.regex.Pattern
import kotlin.math.max
import kotlin.math.min

/** Detects the standard Facebook Reels viewer using visible, viewport-scoped evidence. */
class FacebookDetector : AppDetector {

    override val targetPackages: Set<String> = setOf(AppPlatform.FACEBOOK.packageName, AppPlatform.FACEBOOK_LITE_PACKAGE)
    override val platform: AppPlatform = AppPlatform.FACEBOOK

    companion object {
        private const val RECYCLER_VIEW = "androidx.recyclerview.widget.RecyclerView"
        private const val LITE_PLAYER_ID = "com.facebook.lite:id/video_view"
        private const val ID_PREFIX = "com.facebook.katana:id/"
        private val PRIMARY_VIEW_IDS = setOf(
            ID_PREFIX + "fb_shorts_viewer_view_pager",
            ID_PREFIX + "fb_shorts_unified_viewer",
            ID_PREFIX + "reels_viewer_root",
            ID_PREFIX + "fb_shorts_viewer_container",
            ID_PREFIX + "fb_shorts_video_player",
            ID_PREFIX + "reel_viewer_surface_view",
            ID_PREFIX + "fb_shorts_bottom_sheet_container",
            ID_PREFIX + "fb_shorts_ufi_container",
            ID_PREFIX + "reels_feed_view_pager",
            ID_PREFIX + "fb_shorts_player_fragment",
            ID_PREFIX + "watch_and_go_reels_container",
            ID_PREFIX + "shorts_player"
        )
        private val REELS_SCREEN_IDS = setOf("reels_screen", "reels_player", "video_player_reels")
        private val EXTRA_VIEWER_IDS = setOf("reels_feed_view_pager", "fb_shorts_player_fragment",
            "watch_and_go_reels_container", "shorts_player")
        private val REELS_TAB_IDS = setOf("reels_tab", "tab_reels", "fb_shorts_tab", "reels_tab_button")
        private val HOME_TAB_IDS = setOf("home_tab", "tab_home")
        private val REELS_TAB_LABELS = setOf("reels, tab", "reels tab", "tab reels",
            "ريلز، علامة تبويب", "علامة تبويب ريلز", "onglet reels", "reels, onglet")
        private val NUMBERED_REELS_TAB = Regex("""reels, tab \d+ of \d+""")
        private val REEL_ATTACHMENT_DESCRIPTIONS = setOf(
            "fbshortscomposerattachmentcomponentspec_sticker",
            "fbshortscomposerattachmentcomponentspec_gif"
        )
        private val REELS_LABELS = setOf("reels", "ريلز", "الريلز", "مقاطع ريلز")
        private val REELS_DETAILS = setOf("reels tab details", "تفاصيل علامة تبويب ريلز",
            "تفاصيل علامة التبويب ريلز", "détails de l’onglet reels", "détails de l'onglet reels")
        private val STORY_VIEWERS = setOf("story_viewer", "story_viewer_root", "story_viewer_container",
            "stories_viewer", "story_viewer_surface", "reel_viewer_tall_story_container")

        // Group 1 is viewer architecture. Interaction controls need that context.
        private val FALLBACK_ID_PATTERN = Pattern.compile(
            "^com\\.facebook\\.katana:id/(?:(reels_viewer_.*|reel_viewer_.*|" +
                "fb_shorts_(?:viewer|video).*|fb_reels_viewer.*)|fb_shorts_(?:ufi|bottom_sheet).*)$"
        )

        private val VIEWER_DESC_KEYWORDS = listOf(
            "facebook reels", "ver reels", "regarder des reels",
            "reels ansehen", "assistir a reels", "reels viewer",
            "reel by", "watch reels", "مشاهدة مقاطع reels",
            "रील्स देखें", "facebookリール", "facebook 릴스", "tonton reels"
        )
        private val EMPTY_STATS = TraversalStats(0, 0, 0L)
    }

    private data class Branch(
        val fullViewport: Boolean = false,
        val viewerBounds: IntArray? = null,
        val inFeed: Boolean = false,
        val inChat: Boolean = false,
        val inLargeContentContainer: Boolean = false,
        val inScrollableViewer: Boolean = false,
        val inFullVideoItem: Boolean = false
    )

    override fun evaluate(
        rootNode: AccessibilityNode?,
        event: AccessibilityEvent?,
        traversalEngine: TraversalEngine
    ): DetectionResult = evaluateWithTrace(rootNode, traversalEngine)

    internal fun evaluateWithTrace(rootNode: AccessibilityNode?, traversalEngine: TraversalEngine,
                                   observeNode: ((AccessibilityNode, Int) -> Unit)? = null,
                                   isCurrent: () -> Boolean = { true }): DetectionResult {
        if (rootNode == null || !rootNode.isVisibleToUser ||
            rootNode.packageName?.toString() !in targetPackages
        ) return DetectionResult.negative(platform)

        val lite = rootNode.packageName?.toString() == AppPlatform.FACEBOOK_LITE_PACKAGE
        val viewport = rootNode.getBoundsInScreenArray()
        val minWidth = (viewport[2] - viewport[0]) * 0.85f
        val minHeight = (viewport[3] - viewport[1]) * 0.85f
        val branches = arrayOfNulls<Branch>(traversalEngine.maxBudget.coerceAtLeast(0))
        var best: DetectionResult? = null
        var suppression: SuppressionReason? = null
        var focusedSuppression: SuppressionReason? = null
        var storyViewer = false
        var confirmedViewer: DetectionResult? = null
        var sawLitePlayer = false
        var selectedReelsTab: DetectionResult? = null
        var selectedHomeTab = false
        val prefix = "${rootNode.packageName}:id/"

        val stats = traversalEngine.traverse(rootNode, diagnosticVisitor = visit@{ node, depth, index, parent ->
            observeNode?.invoke(node, depth)
            val enclosing = if (parent == null) Branch() else branches[parent] ?: return@visit
            if (!node.isVisibleToUser) return@visit
            val bounds = node.getBoundsInScreenArray()
            val width = overlapWidth(bounds, viewport)
            val height = overlapHeight(bounds, viewport)
            if (width <= 0 || height <= 0) return@visit
            branches[index] = enclosing

            val reason = DisambiguationEngine.checkSuppression(node, platform)
            val inFeed = enclosing.inFeed || reason == SuppressionReason.STANDARD_FEED_SCRUBBER
            val inChat = enclosing.inChat || reason == SuppressionReason.CHAT_THREAD_CONTAINER
            if (reason != null && (suppression == null || suppression == SuppressionReason.STANDARD_FEED_SCRUBBER)) {
                suppression = reason
            }
            if (node.isFocused) {
                if (reason != null && reason != SuppressionReason.STANDARD_FEED_SCRUBBER) {
                    focusedSuppression = reason
                } else if (inChat) {
                    focusedSuppression = SuppressionReason.CHAT_THREAD_CONTAINER
                }
            }

            val rawId = node.viewIdResourceName
            val localId = rawId?.takeIf { it.startsWith(prefix) }?.removePrefix(prefix)
            val id = localId?.let { ID_PREFIX + it }
            val explicitViewer = localId in REELS_SCREEN_IDS
            val primary = id in PRIMARY_VIEW_IDS || explicitViewer
            val fallback = id?.let { FALLBACK_ID_PATTERN.matcher(it) }?.takeIf { it.matches() }
            val viewerId = fallback?.group(1) != null || explicitViewer ||
                localId in EXTRA_VIEWER_IDS
            val description = node.displayDescription?.toString()?.trim()?.lowercase(Locale.ROOT)
            if (lite && rawId == LITE_PLAYER_ID) {
                sawLitePlayer = true
                if (confirmedViewer == null && isLiteReelsPlayer(node, viewport, isCurrent)) {
                    confirmedViewer = layoutMatch("Facebook Lite Reels player")
                }
            } else if (!lite && confirmedViewer == null) {
                if (description in REEL_ATTACHMENT_DESCRIPTIONS) {
                    confirmedViewer = match(DetectionConfidence.HIGH, DetectionLayer.CONTENT_DESCRIPTION,
                        "Facebook Reels composer attachment")
                } else if (enclosing.inFullVideoItem && node.className?.toString() == "android.view.SurfaceView" &&
                    bounds.fillsVideoViewport(viewport, minWidth = 0.9f)) {
                    confirmedViewer = layoutMatch("Facebook Reels video layout")
                }
            }
            if (node.isSelected) {
                if (localId in HOME_TAB_IDS) selectedHomeTab = true
                val label = description ?: node.displayText?.toString()?.trim()?.lowercase(Locale.ROOT)
                if (localId in REELS_TAB_IDS || label in REELS_TAB_LABELS ||
                    (label != null && NUMBERED_REELS_TAB.matches(label)) ||
                    (!lite && description?.startsWith("reels,") == true &&
                        bounds[3] <= viewport[1] + (viewport[3] - viewport[1]) * 0.25f) ||
                    (node.isClickable && label in REELS_LABELS)) {
                    selectedReelsTab = if (localId in REELS_TAB_IDS) {
                        match(DetectionConfidence.HIGH, DetectionLayer.PRIMARY_RESOURCE_ID, checkNotNull(rawId))
                    } else match(DetectionConfidence.HIGH, DetectionLayer.CONTENT_DESCRIPTION, "Selected Reels tab")
                }
            }
            val fullSize = width >= minWidth && height >= minHeight
            val activeViewer = viewerId && bounds.fillsVideoViewport(viewport)
            if (localId in STORY_VIEWERS && bounds.fillsVideoViewport(viewport)) storyViewer = true
            val insideViewer = enclosing.viewerBounds?.let {
                overlapWidth(bounds, it) > 0 && overlapHeight(bounds, it) > 0
            } == true
            val opensViewer = viewerId && (activeViewer || (!inFeed && enclosing.fullViewport))
            val startsList = !lite && node.className?.toString() == RECYCLER_VIEW
            val scrollableViewer = if (startsList) {
                node.isScrollable && bounds.fillsVideoViewport(viewport, minWidth = 0.9f)
            } else enclosing.inScrollableViewer
            branches[index] = Branch(
                fullViewport = enclosing.fullViewport || fullSize,
                viewerBounds = if (opensViewer && (!insideViewer || fullSize)) bounds else enclosing.viewerBounds,
                inFeed = inFeed,
                inChat = inChat,
                inLargeContentContainer = enclosing.inLargeContentContainer ||
                    (depth > 0 && bounds.fillsVideoViewport(viewport) && isContentContainer(node)),
                inScrollableViewer = scrollableViewer,
                inFullVideoItem = scrollableViewer && ((!startsList && enclosing.inFullVideoItem) ||
                    (node.className?.toString() == "android.widget.Button" && node.isLongClickable &&
                        bounds.fillsVideoViewport(viewport, minWidth = 0.9f)))
            )

            val candidate = when {
                rawId != null && (insideViewer || activeViewer) && primary ->
                    match(DetectionConfidence.DEFINITIVE, DetectionLayer.PRIMARY_RESOURCE_ID, rawId)
                rawId != null && (insideViewer || activeViewer) && fallback != null ->
                    match(DetectionConfidence.HIGH, DetectionLayer.FALLBACK_REGEX_ID, rawId)
                description in REELS_DETAILS && (bounds.fillsVideoViewport(viewport) ||
                    (!node.isClickable && !inFeed && enclosing.inLargeContentContainer)) -> {
                    match(DetectionConfidence.HIGH, DetectionLayer.CONTENT_DESCRIPTION, "Reels tab details")
                        .also { if (confirmedViewer == null) confirmedViewer = it }
                }
                insideViewer -> labelEvidence(node)
                else -> null
            }
            if (candidate != null && candidate.confidence.score > (best?.confidence?.score ?: 0.0)) {
                best = candidate
            }
        }, shouldVisitChildren = { branches[it] != null }) { _, _ -> false }

        if (storyViewer) return DetectionResult.suppressed(platform, SuppressionReason.STORY_VIEWER_ACTIVE,
            stats, foregroundConfirmed = true)
        focusedSuppression?.let { return DetectionResult.suppressed(platform, it, stats, foregroundConfirmed = true) }
        if (best?.confidence == DetectionConfidence.DEFINITIVE) return best!!.copy(stats = stats)
        confirmedViewer?.let { return it.copy(stats = stats) }
        if (lite && (!sawLitePlayer || stats.budgetExhausted || stats.depthLimitReached) && isCurrent()) {
            val players = rootNode.findByViewId(LITE_PLAYER_ID)
            try {
                if (players.take(8).any { isLiteReelsPlayer(it, viewport, isCurrent) }) {
                    return layoutMatch("Facebook Lite Reels player").copy(stats = stats)
                }
            } finally {
                players.forEach(AccessibilityNode::recycle)
            }
        }
        if (!selectedHomeTab && best == null) selectedReelsTab?.let { return it.copy(stats = stats) }
        val match = best
        if (match != null && (suppression == null || suppression == SuppressionReason.STANDARD_FEED_SCRUBBER)) {
            return match.copy(stats = stats)
        }
        suppression?.let { return DetectionResult.suppressed(platform, it, stats,
            foregroundConfirmed = !stats.budgetExhausted && !stats.depthLimitReached) }
        return DetectionResult.negative(platform, stats)
    }

    private fun isLiteReelsPlayer(
        player: AccessibilityNode, viewport: IntArray, isCurrent: () -> Boolean
    ): Boolean {
        if (!isCurrent() || !player.isVisibleToUser || player.viewIdResourceName != LITE_PLAYER_ID) return false
        val video = player.getBoundsInScreenArray()
        if (!video.fillsVideoViewport(viewport, minHeight = 0.6f)) return false

        var hasPlayerContainer = false
        var ancestor = player.parent
        try {
            repeat(24) {
                val node = ancestor ?: return false
                if (!isCurrent() || !node.isVisibleToUser ||
                    node.viewIdResourceName?.substringAfter(":id/") in STORY_VIEWERS) return false
                val type = node.className?.toString()
                if (type == RECYCLER_VIEW) {
                    return hasPlayerContainer && node.isScrollable &&
                        node.getBoundsInScreenArray().fillsVideoViewport(viewport, minWidth = 0.9f, minHeight = 0.6f)
                }
                if (type == "android.view.ViewGroup" && !hasPlayerContainer) {
                    val container = node.getBoundsInScreenArray()
                    if (node.isClickable || !video.fillsVideoViewport(container, minWidth = 0.98f, minHeight = 0.98f) ||
                        !container.fillsVideoViewport(video, minWidth = 0.98f, minHeight = 0.98f)) return false
                    hasPlayerContainer = true
                }
                ancestor = node.parent
                node.recycle()
            }
            return false
        } finally {
            ancestor?.recycle()
        }
    }

    private fun layoutMatch(identifier: String): DetectionResult =
        match(DetectionConfidence.HIGH, DetectionLayer.CLASS_HIERARCHY, identifier)

    private fun isContentContainer(node: AccessibilityNode): Boolean {
        val name = node.className?.toString().orEmpty()
        return name == "com.facebook.litho.LithoView" ||
            name.endsWith("RecyclerView") || name.endsWith("ViewPager")
    }

    private fun labelEvidence(node: AccessibilityNode): DetectionResult? {
        val description = node.displayDescription?.toString()?.lowercase()
        if (description != null && VIEWER_DESC_KEYWORDS.any { description.contains(it) }) {
            return match(DetectionConfidence.HIGH, DetectionLayer.CONTENT_DESCRIPTION, description)
        }
        val text = node.displayText?.toString()?.lowercase()
        return if (text != null && VIEWER_DESC_KEYWORDS.any { text.contains(it) }) {
            match(DetectionConfidence.MEDIUM, DetectionLayer.MULTILINGUAL_TEXT, text)
        } else null
    }

    private fun match(confidence: DetectionConfidence, layer: DetectionLayer, id: String): DetectionResult =
        DetectionResult.positive(platform, confidence, layer, id, EMPTY_STATS)

    private fun overlapWidth(a: IntArray, b: IntArray): Int = min(a[2], b[2]) - max(a[0], b[0])
    private fun overlapHeight(a: IntArray, b: IntArray): Int = min(a[3], b[3]) - max(a[1], b[1])
}
