// SPDX-License-Identifier: MIT
package com.blockreels.app.detection

import android.view.accessibility.AccessibilityEvent
import com.blockreels.app.disambiguation.DisambiguationEngine
import com.blockreels.app.engine.TraversalEngine
import com.blockreels.app.engine.TraversalStats
import com.blockreels.app.model.AccessibilityNode
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/** Detects Snapchat Spotlight short-form video feeds while keeping Chat, Camera, and Stories open. */
class SnapchatDetector : AppDetector {

    override val targetPackages: Set<String> = setOf(AppPlatform.SNAPCHAT.packageName)
    override val platform: AppPlatform = AppPlatform.SNAPCHAT

    companion object {
        private const val ID_PREFIX = "com.snapchat.android:id/"
        private val PRIMARY_VIEW_IDS = setOf(
            ID_PREFIX + "spotlight_feed_container",
            ID_PREFIX + "spotlight_player",
            ID_PREFIX + "spotlight_view_pager",
            ID_PREFIX + "spotlight_page_container",
            ID_PREFIX + "spotlight_video_view",
            ID_PREFIX + "spotlight_swipe_container"
        )
        private val SPOTLIGHT_TAB_IDS = setOf(
            ID_PREFIX + "ngs_spotlight_icon_container",
            ID_PREFIX + "spotlight_tab"
        )
        private val SPOTLIGHT_DESC_KEYWORDS = listOf(
            "spotlight",
            "spotlight video",
            "spotlight feed",
            "swipe up for next snap"
        )
        private val SUPPRESSION_IDS = setOf(
            ID_PREFIX + "chat_input_text_field",
            ID_PREFIX + "chat_message_list",
            ID_PREFIX + "feed_list",
            ID_PREFIX + "camera_capture_button",
            ID_PREFIX + "ngs_camera_icon_container",
            ID_PREFIX + "story_player_container"
        )
        private val EMPTY_STATS = TraversalStats(0, 0, 0L)
    }

    override fun evaluate(
        rootNode: AccessibilityNode?,
        event: AccessibilityEvent?,
        traversalEngine: TraversalEngine
    ): DetectionResult = evaluateWithTrace(rootNode, traversalEngine)

    internal fun evaluateWithTrace(
        rootNode: AccessibilityNode?,
        traversalEngine: TraversalEngine,
        observeNode: ((AccessibilityNode, Int) -> Unit)? = null
    ): DetectionResult {
        if (rootNode == null || !rootNode.isVisibleToUser ||
            (rootNode.packageName != null && rootNode.packageName.toString() !in targetPackages)
        ) return DetectionResult.negative(platform)

        val viewport = rootNode.getBoundsInScreenArray()
        var best: DetectionResult? = null
        var suppression: SuppressionReason? = null
        var foregroundSuppression: SuppressionReason? = null
        var spotlightTabSelected = false
        var fullScreenOperaPlayer = false

        val stats = traversalEngine.traverse(rootNode, diagnosticVisitor = visit@{ node, depth, _, _ ->
            observeNode?.invoke(node, depth)
            if (!node.isVisibleToUser) return@visit

            val bounds = node.getBoundsInScreenArray()
            if (min(bounds[2], viewport[2]) <= max(bounds[0], viewport[0]) ||
                min(bounds[3], viewport[3]) <= max(bounds[1], viewport[1])
            ) return@visit

            val viewId = node.viewIdResourceName
            val disamb = DisambiguationEngine.checkSuppression(node, platform)
            if (disamb != null) {
                if (suppression == null) suppression = disamb
                if (node.isFocused && disamb == SuppressionReason.CHAT_COMPOSER_ACTIVE) {
                    foregroundSuppression = disamb
                }
            }

            if (viewId in SUPPRESSION_IDS) {
                val reason = when {
                    viewId!!.contains("camera") -> SuppressionReason.CREATOR_CAMERA_ACTIVE
                    viewId.contains("story") -> SuppressionReason.STORY_VIEWER_ACTIVE
                    viewId.contains("chat_input") -> SuppressionReason.CHAT_COMPOSER_ACTIVE
                    else -> SuppressionReason.CHAT_THREAD_CONTAINER
                }
                if (suppression == null) suppression = reason
                if (bounds.fillsVideoViewport(viewport) || node.isFocused) {
                    foregroundSuppression = reason
                }
            }

            if (node.isSelected && viewId in SPOTLIGHT_TAB_IDS) {
                spotlightTabSelected = true
            }

            val fullViewport = bounds.fillsVideoViewport(viewport)
            if (fullViewport && viewId != null && (viewId.contains("opera_") || viewId.contains("spotlight"))) {
                if (viewId in PRIMARY_VIEW_IDS) {
                    best = pickHigher(
                        best,
                        DetectionResult.positive(
                            platform,
                            DetectionConfidence.DEFINITIVE,
                            DetectionLayer.PRIMARY_RESOURCE_ID,
                            viewId,
                            EMPTY_STATS
                        )
                    )
                } else if (viewId.contains("spotlight")) {
                    best = pickHigher(
                        best,
                        DetectionResult.positive(
                            platform,
                            DetectionConfidence.HIGH,
                            DetectionLayer.FALLBACK_REGEX_ID,
                            viewId,
                            EMPTY_STATS
                        )
                    )
                } else if (viewId.contains("opera_")) {
                    fullScreenOperaPlayer = true
                }
            }

            val desc = node.displayDescription?.toString()?.lowercase(Locale.ROOT)
            val text = node.displayText?.toString()?.lowercase(Locale.ROOT)
            if (fullViewport && desc != null && SPOTLIGHT_DESC_KEYWORDS.any { desc.contains(it) }) {
                best = pickHigher(
                    best,
                    DetectionResult.positive(
                        platform,
                        DetectionConfidence.HIGH,
                        DetectionLayer.CONTENT_DESCRIPTION,
                        desc,
                        EMPTY_STATS
                    )
                )
            } else if (fullViewport && text != null && SPOTLIGHT_DESC_KEYWORDS.any { text.contains(it) }) {
                best = pickHigher(
                    best,
                    DetectionResult.positive(
                        platform,
                        DetectionConfidence.MEDIUM,
                        DetectionLayer.MULTILINGUAL_TEXT,
                        text,
                        EMPTY_STATS
                    )
                )
            }
        }) { _, _ -> false }

        foregroundSuppression?.let {
            return DetectionResult.suppressed(platform, it, stats, foregroundConfirmed = true)
        }
        if (best?.confidence == DetectionConfidence.DEFINITIVE) {
            return best.copy(stats = stats)
        }
        if (spotlightTabSelected && fullScreenOperaPlayer) {
            return DetectionResult.positive(
                platform,
                DetectionConfidence.HIGH,
                DetectionLayer.CLASS_HIERARCHY,
                "snapchat_spotlight_opera",
                stats
            )
        }
        suppression?.let {
            return DetectionResult.suppressed(
                platform,
                it,
                stats,
                foregroundConfirmed = best == null && !stats.budgetExhausted && !stats.depthLimitReached
            )
        }
        return best?.copy(stats = stats) ?: DetectionResult.negative(platform, stats)
    }

    private fun pickHigher(current: DetectionResult?, candidate: DetectionResult): DetectionResult {
        return if (current == null || candidate.confidence.score > current.confidence.score) candidate else current
    }
}
