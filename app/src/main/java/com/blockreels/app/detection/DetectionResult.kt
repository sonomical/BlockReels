package com.blockreels.app.detection

import com.blockreels.app.engine.TraversalStats
import com.blockreels.app.model.AccessibilityNode
import kotlin.math.max
import kotlin.math.min

/** Clip against the app window, allowing room for navigation and status bars. */
internal fun IntArray.fillsVideoViewport(viewport: IntArray, minWidth: Float = 0.85f,
                                        minHeight: Float = 0.75f): Boolean {
    val width = viewport[2] - viewport[0]
    val height = viewport[3] - viewport[1]
    return width > 0 && height > 0 &&
        min(this[2], viewport[2]) - max(this[0], viewport[0]) >= width * minWidth &&
        min(this[3], viewport[3]) - max(this[1], viewport[1]) >= height * minHeight
}

/**
 * True when [node] occupies nearly the whole screen while [root] is attached.
 * Used to let content-description fallbacks corroborate a full-screen short-form
 * player without matching small tab buttons or feed shelves.
 */
internal fun AccessibilityNode.coversScreen(root: AccessibilityNode, minRatio: Float = 0.85f): Boolean {
    val nodeBounds = getBoundsInScreenArray()
    val rootBounds = root.getBoundsInScreenArray()
    val rootWidth = rootBounds[2] - rootBounds[0]
    val rootHeight = rootBounds[3] - rootBounds[1]
    if (rootWidth <= 0 || rootHeight <= 0) return false
    val nodeWidth = nodeBounds[2] - nodeBounds[0]
    val nodeHeight = nodeBounds[3] - nodeBounds[1]
    return nodeWidth >= rootWidth * minRatio && nodeHeight >= rootHeight * minRatio
}

data class DetectionResult(
    val isMatch: Boolean,
    val platform: AppPlatform,
    val confidence: DetectionConfidence = DetectionConfidence.LOW,
    val layerMatched: DetectionLayer? = null,
    val matchedIdentifier: String? = null,
    val disambiguationSuppressed: Boolean = false,
    val suppressionReason: SuppressionReason? = null,
    val stats: TraversalStats = TraversalStats(0, 0, 0L),
    val foregroundSuppressionConfirmed: Boolean = false
) {
    companion object {
        fun negative(platform: AppPlatform, stats: TraversalStats = TraversalStats(0, 0, 0L)) =
            DetectionResult(isMatch = false, platform = platform, stats = stats)

        fun suppressed(
            platform: AppPlatform,
            reason: SuppressionReason,
            stats: TraversalStats = TraversalStats(0, 0, 0L),
            foregroundConfirmed: Boolean = false
        ) =
            DetectionResult(
                isMatch = false,
                platform = platform,
                disambiguationSuppressed = true,
                suppressionReason = reason,
                stats = stats,
                foregroundSuppressionConfirmed = foregroundConfirmed
            )

        fun positive(
            platform: AppPlatform,
            confidence: DetectionConfidence,
            layer: DetectionLayer,
            identifier: String,
            stats: TraversalStats = TraversalStats(0, 0, 0L)
        ) = DetectionResult(
            isMatch = true,
            platform = platform,
            confidence = confidence,
            layerMatched = layer,
            matchedIdentifier = identifier,
            stats = stats
        )
    }
}
