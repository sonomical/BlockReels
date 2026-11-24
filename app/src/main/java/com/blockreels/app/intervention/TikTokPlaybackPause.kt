package com.blockreels.app.intervention

import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import com.blockreels.app.detection.AppPlatform
import com.blockreels.app.detection.TikTokDetector
import com.blockreels.app.engine.TraversalEngine
import com.blockreels.app.model.AccessibilityNode
import com.blockreels.app.model.RealAccessibilityNode
import kotlin.math.max
import kotlin.math.min

/** Best effort. A successful accessibility action acknowledges the click, not silence. */
internal object TikTokPlaybackPause {
    enum class Outcome { NO_CONTROL, CONTROL_CHANGED, CLICK_ACCEPTED, CLICK_REJECTED }

    data class Control(val enabled: Boolean, val clickLabel: String?, val playbackState: String?)

    private val pauseLabels = setOf("pause", "pause video", "pause playback")
    private val playLabels = setOf("play", "play video", "resume", "resume video", "resume playback")
    private val toggleLabels = setOf("play/pause", "play or pause", "play/pause video")
    private val playingStates = setOf("playing", "video playing", "playback playing")
    private val stoppedStates = setOf("paused", "stopped", "video paused", "playback paused", "not playing")

    fun attempt(
        root: RealAccessibilityNode,
        engine: TraversalEngine,
        ownerIsActive: () -> Boolean = { true }
    ): Outcome = attempt(
        root, engine,
        controlFor = { runCatching { readControl((it as RealAccessibilityNode).realNode) }.getOrNull() },
        refresh = { runCatching { (it as RealAccessibilityNode).realNode.refresh() }.getOrDefault(false) },
        click = {
            ownerIsActive() && runCatching {
                (it as RealAccessibilityNode).realNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }.getOrDefault(false)
        }
    )

    internal fun attempt(
        root: AccessibilityNode,
        engine: TraversalEngine,
        controlFor: (AccessibilityNode) -> Control?,
        refresh: (AccessibilityNode) -> Boolean = { true },
        click: (AccessibilityNode) -> Boolean
    ): Outcome {
        val pkg = root.packageName?.toString()
        if (AppPlatform.fromPackageName(pkg) != AppPlatform.TIKTOK || !root.isVisibleToUser) {
            return Outcome.NO_CONTROL
        }
        val viewport = root.getBoundsInScreenArray()
        val minWidth = (viewport[2] - viewport[0]) * 0.85f
        val minHeight = (viewport[3] - viewport[1]) * 0.85f
        if (minWidth <= 0 || minHeight <= 0) return Outcome.NO_CONTROL

        val visible = BooleanArray(engine.maxBudget.coerceAtLeast(0))
        val viewer = BooleanArray(visible.size)
        var outcome = Outcome.NO_CONTROL
        fun visibleInViewport(bounds: IntArray): Boolean =
            min(bounds[2], viewport[2]) > max(bounds[0], viewport[0]) &&
                min(bounds[3], viewport[3]) > max(bounds[1], viewport[1])

        engine.traverse(root, diagnosticVisitor = visit@{ node, _, index, parent ->
            if (!node.isVisibleToUser || (parent != null && !visible[parent])) return@visit
            visible[index] = true
            val inViewer = parent != null && viewer[parent]
            viewer[index] = inViewer
            val id = node.viewIdResourceName
            val viewerId = id?.startsWith("$pkg:id/") == true && TikTokDetector.isFeedViewerId(id)
            if (!inViewer && !viewerId) return@visit
            val bounds = node.getBoundsInScreenArray()
            val width = min(bounds[2], viewport[2]) - max(bounds[0], viewport[0])
            val height = min(bounds[3], viewport[3]) - max(bounds[1], viewport[1])
            if (width <= 0 || height <= 0) return@visit
            if (viewerId && width >= minWidth && height >= minHeight) viewer[index] = true
            if (!inViewer || node.packageName?.toString() != pkg) return@visit
            if (node.isInputField) return@visit
            val control = controlFor(node) ?: return@visit
            if (canPause(node, control)) {
                val stillPausable = refresh(node) && node.isVisibleToUser &&
                    node.packageName?.toString() == pkg && visibleInViewport(node.getBoundsInScreenArray()) &&
                    controlFor(node)?.let { canPause(node, it) } == true
                outcome = when {
                    !stillPausable -> Outcome.CONTROL_CHANGED
                    click(node) -> Outcome.CLICK_ACCEPTED
                    else -> Outcome.CLICK_REJECTED
                }
            }
        }) { _, _ -> outcome != Outcome.NO_CONTROL }
        return outcome
    }

    private fun readControl(info: AccessibilityNodeInfo): Control? {
        val action = info.actionList.firstOrNull { it.id == AccessibilityNodeInfo.ACTION_CLICK } ?: return null
        val state = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) info.stateDescription?.toString() else null
        return Control(info.isEnabled, action.label?.toString(), state)
    }

    private fun canPause(node: AccessibilityNode, control: Control): Boolean {
        if (!control.enabled) return false
        fun normalized(value: CharSequence?): String? =
            value?.toString()?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        val action = normalized(control.clickLabel)
        val state = normalized(control.playbackState)
        val description = normalized(node.displayDescription)
        val playing = state in playingStates
        if (state in stoppedStates || description in playLabels) return false
        if (action != null) return action in pauseLabels || (playing && action in toggleLabels)
        if (!node.isClickable) return false
        if (playing && (description in pauseLabels || description in toggleLabels)) return true
        val button = node.className?.toString() == "android.widget.Button" ||
            node.className?.toString() == "android.widget.ImageButton"
        val buttonLabel = description ?: normalized(node.displayText)
        return button && (buttonLabel in pauseLabels || (playing && buttonLabel in toggleLabels))
    }
}
