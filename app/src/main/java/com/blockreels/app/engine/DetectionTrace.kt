package com.blockreels.app.engine

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.blockreels.app.detection.DetectionResult
import com.blockreels.app.detection.AppPlatform
import com.blockreels.app.detection.SuppressionReason
import com.blockreels.app.disambiguation.DisambiguationEngine
import com.blockreels.app.model.AccessibilityNode
import java.util.concurrent.atomic.AtomicLong

/** Opt-in, debug-app-only metadata. Never accepts node text, descriptions or PINs. */
internal object DetectionTrace {
    const val TAG = "BlockReelsDetection"
    private var debugApp = false
    private var versions = ""
    private val sequence = AtomicLong()
    private val evaluations = LinkedHashMap<Long, Frame>()
    private val interventions = LinkedHashMap<Long, Frame>()
    val enabled: Boolean get() = debugApp && Log.isLoggable(TAG, Log.DEBUG)

    fun configure(context: Context) {
        debugApp = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (!debugApp) return
        fun version(pkg: String): String = try {
            @Suppress("DEPRECATION")
            val info = context.packageManager.getPackageInfo(pkg, 0)
            @Suppress("DEPRECATION")
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode
                else info.versionCode.toLong()
            "${info.versionName}/$code"
        } catch (_: Exception) { "unavailable" }
        versions = "android=${Build.VERSION.RELEASE}/${Build.VERSION.SDK_INT}" +
            " app=${version(context.packageName)} youtube=${version("com.google.android.youtube")}" +
            " instagram=${version("com.instagram.android")}" +
            " facebook=${version("com.facebook.katana")} facebookLite=${version("com.facebook.lite")}"
    }

    data class Frame(
        val packageName: String,
        val eventType: Int?,
        val eventTime: Long?,
        val eventWindow: Int?,
        val followUp: Boolean,
        val receivedUptimeMs: Long = SystemClock.uptimeMillis(),
        val id: Long = sequence.incrementAndGet(),
        var ready: Boolean = false,
        var masterEnabled: Boolean = false,
        var appEnabled: Boolean = false,
        var blockingActive: Boolean = false,
        var temporarilyAllowed: Boolean = false,
        var ruleMode: String? = null,
        var rootPackage: String? = null,
        var rootWindow: Int? = null,
        var rootVisible: Boolean? = null,
        var evidenceUptimeMs: Long? = null,
        var detectionCompletedUptimeMs: Long? = null
    )

    enum class Stage { RECEIVED, NOT_READY, INELIGIBLE, ALLOWED, THROTTLED, OVERLAY_ACTIVE,
        NO_ROOT, WRONG_ROOT, DETECTED, INTERVENTION, LITE_SIGNATURE_UNSUPPORTED }
    enum class Lifecycle { REQUESTED, SHOW_QUEUED, ATTACHED, FAILED, DISMISSED, FINISHED, AUTO_BACK,
        EXIT_REQUESTED, EXIT_ACCEPTED, EXIT_VERIFIED, EXIT_FAILED }

    fun hold(id: Long, event: String, actionMasked: Int?, pointerId: Int?, state: String,
             elapsedRealtimeMs: Long, heldMs: Long?, ownerActive: Boolean, parentLocked: Boolean,
             changedPointer: Int?, eventTimeUptimeMs: Long?) {
        if (!enabled) return
        Log.d(TAG, "hold=$event intervention=$id actionMasked=$actionMasked pointer=$pointerId" +
            " state=$state elapsedRealtimeMs=$elapsedRealtimeMs heldMs=$heldMs" +
            " ownerActive=$ownerActive parentInterceptionDisallowed=$parentLocked" +
            " changedPointer=$changedPointer eventUptimeMs=$eventTimeUptimeMs" +
            " processedUptimeMs=${SystemClock.uptimeMillis()}")
    }

    fun audio(id: Long, state: String, sdk: Int? = null, targetSdk: Int? = null,
              foregroundServiceRunning: Boolean? = null, result: Int? = null) {
        if (!enabled) return
        Log.d(TAG, "uptimeMs=${SystemClock.uptimeMillis()} audio=$state intervention=$id" +
            " sdk=$sdk targetSdk=$targetSdk monitoringFgs=$foregroundServiceRunning" +
            " focusResult=$result silenceVerified=false")
    }

    fun pipeline(frame: Frame, stage: Stage, result: DetectionResult? = null,
                 engine: TraversalEngine? = null, diagnosticStats: TraversalStats? = null) {
        if (!enabled) return
        if (stage == Stage.DETECTED && result?.isMatch == true) {
            evaluations[frame.id] = frame.copy()
            while (evaluations.size > 16) evaluations.remove(evaluations.keys.first())
        }
        Log.d(TAG, "$versions uptimeMs=${SystemClock.uptimeMillis()} stage=$stage frame=$frame match=${result?.isMatch}" +
            " layer=${result?.layerMatched} suppression=${result?.suppressionReason}" +
            " stats=${diagnosticStats ?: result?.stats} maxDepth=${engine?.maxDepth} maxBudget=${engine?.maxBudget}")
    }

    fun lifecycle(id: Long, state: Lifecycle, packageName: String? = null, mode: String? = null) {
        if (!enabled) return
        val now = SystemClock.uptimeMillis()
        val frame = interventions[id]
        val latency = if (state == Lifecycle.ATTACHED && frame != null) AttachmentLatency.from(frame, now) else null
        Log.d(TAG, "uptimeMs=$now overlay=$state intervention=$id evaluation=${frame?.id}" +
            " package=$packageName mode=$mode latency=$latency")
        if (state == Lifecycle.FINISHED || state == Lifecycle.FAILED || state == Lifecycle.DISMISSED) {
            interventions.remove(id)
        }
    }

    fun bindIntervention(id: Long, evaluationId: Long?) {
        if (!enabled) return
        evaluationId?.let { evaluations.remove(it) }?.let { interventions[id] = it }
        while (interventions.size > 16) interventions.remove(interventions.keys.first())
    }

    data class AttachmentLatency(val eventDeliveryMs: Long?, val arrivalToAttachMs: Long?,
                                 val evidenceToAttachMs: Long?, val detectionToAttachMs: Long?) {
        companion object {
            fun from(frame: Frame, attachedUptimeMs: Long): AttachmentLatency {
                fun difference(end: Long, start: Long?): Long? =
                    start?.let { (end - it).takeIf { elapsed -> elapsed >= 0L } }
                return AttachmentLatency(difference(frame.receivedUptimeMs, frame.eventTime),
                    if (frame.eventTime != null) difference(attachedUptimeMs, frame.receivedUptimeMs) else null,
                    difference(attachedUptimeMs, frame.evidenceUptimeMs),
                    difference(attachedUptimeMs, frame.detectionCompletedUptimeMs))
            }
        }
    }

    enum class UiLabelHint { REELS, LIKE, COMMENT, SHARE, BACK, SEARCH }

    data class NodeSummary(
        val depth: Int, val resourceId: String?, val className: String?,
        val bounds: List<Int>, val visible: Boolean, val editable: Boolean,
        val focused: Boolean, val children: Int, val suppression: SuppressionReason?,
        val index: Int?, val parentIndex: Int?, val clickable: Boolean, val uiLabelHint: UiLabelHint?
    ) {
        companion object {
            fun from(node: AccessibilityNode, depth: Int, platform: AppPlatform? = AppPlatform.YOUTUBE,
                     packageName: String? = platform?.packageName, index: Int? = null,
                     parentIndex: Int? = null): NodeSummary {
                val id = node.viewIdResourceName?.takeIf {
                    packageName != null && it.matches(Regex("${Regex.escape(packageName)}:id/[A-Za-z0-9_]+"))
                }
                val cls = node.className?.toString()?.takeIf {
                    it.matches(Regex("[A-Za-z0-9_.$]+"))
                }
                fun hint(label: CharSequence?): UiLabelHint? = when (label?.toString()?.trim()?.lowercase()) {
                    "reels" -> UiLabelHint.REELS
                    "like" -> UiLabelHint.LIKE
                    "comment", "comments" -> UiLabelHint.COMMENT
                    "share" -> UiLabelHint.SHARE
                    "back" -> UiLabelHint.BACK
                    "search" -> UiLabelHint.SEARCH
                    else -> null
                }
                return NodeSummary(depth, id, cls, node.getBoundsInScreenArray().toList(),
                    node.isVisibleToUser, node.isEditable, node.isFocused, node.childCount,
                    platform?.let { DisambiguationEngine.checkSuppression(node, it) },
                    index, parentIndex, node.isClickable, hint(node.displayDescription) ?: hint(node.displayText))
            }
        }
    }

    fun structure(frame: Frame, nodes: List<NodeSummary>) {
        if (!enabled) return
        val limit = if (frame.packageName == "com.facebook.lite") 150 else 48
        nodes.take(limit).forEach { Log.d(TAG, "evaluation=${frame.id} structure=$it") }
    }
}
