package com.blockreels.app.intervention

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.blockreels.app.detection.AppPlatform
import com.blockreels.app.detection.DetectionResult
import com.blockreels.app.engine.DetectionTrace
import com.blockreels.app.state.InterventionConfig
import com.blockreels.app.state.InterventionMode
import java.util.concurrent.ConcurrentHashMap

class InterventionController internal constructor(
    val backLoopGuard: BackLoopGuard,
    val audioDuckingManager: AudioDuckingManager,
    private val onGlobalBackAction: () -> Boolean,
    private val onHomeAction: () -> Unit,
    var overlayManager: InterventionOverlay?,
    private val allowanceNowMs: () -> Long = System::currentTimeMillis,
    private val onSystemHomeAction: () -> Boolean = { false },
    private val onExitStarted: (Long) -> Unit = {},
    private val onExitFinished: (Long) -> Unit = {},
    private val onAllowanceGranted: (String) -> Unit = {},
    private val onTikTokOverlayAttached: (String, Long) -> Unit = { _, _ -> },
    private val onFeedLeft: (String) -> Unit = {},
    private val dispatchAudio: (() -> Unit) -> Unit = { it() }
) {
    constructor(
        context: Context,
        backLoopGuard: BackLoopGuard = BackLoopGuard(),
        audioDuckingManager: AudioDuckingManager? = null,
        onGlobalBackAction: () -> Boolean = { false },
        onSystemHomeAction: () -> Boolean = { false },
        onExitStarted: (Long) -> Unit = {},
        onExitFinished: (Long) -> Unit = {},
        onAllowanceGranted: (String) -> Unit = {},
        onTikTokOverlayAttached: (String, Long) -> Unit = { _, _ -> },
        onFeedLeft: (String) -> Unit = {},
        dispatchAudio: (() -> Unit) -> Unit = { it() }
    ) : this(
        backLoopGuard,
        audioDuckingManager ?: AudioDuckingManager(context, dispatchAudio),
        onGlobalBackAction,
        { launchHome(context) },
        OverlayManager(context),
        allowanceNowMs = SystemClock::uptimeMillis,
        onSystemHomeAction = onSystemHomeAction,
        onExitStarted = onExitStarted,
        onExitFinished = onExitFinished,
        onAllowanceGranted = onAllowanceGranted,
        onTikTokOverlayAttached = onTikTokOverlayAttached,
        onFeedLeft = onFeedLeft,
        dispatchAudio = dispatchAudio
    )

    companion object {
        internal const val BACK_TRANSITION_MS = 400L
        const val TEMPORARY_ALLOWANCE_MS = 5 * 60 * 1000L

        private fun launchHome(context: Context) {
            try {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                }
                context.startActivity(homeIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private enum class Phase { ACTIVE, EXITING, EXIT_FAILED }
    private data class Intervention(
        val id: Long,
        val packageName: String,
        val windowId: Int?,
        val mode: InterventionMode,
        val phase: Phase = Phase.ACTIVE
    )

    private val sequence = java.util.concurrent.atomic.AtomicLong()
    private val currentIntervention = java.util.concurrent.atomic.AtomicReference<Intervention?>(null)
    private var leavingPackage: String? = null
    private var leavingUntil = 0L
    private val temporaryAllowances = ConcurrentHashMap<String, Long>()

    fun isAppTemporarilyAllowed(packageName: String): Boolean {
        return temporaryAllowanceRemainingMs(packageName) > 0L
    }

    fun temporaryAllowanceRemainingMs(packageName: String): Long {
        val expiry = temporaryAllowances[packageName] ?: return 0L
        val remaining = expiry - allowanceNowMs()
        if (remaining > 0L) return remaining
        temporaryAllowances.remove(packageName, expiry)
        return 0L
    }

    fun grantTemporaryAllowance(packageName: String, durationMs: Long = TEMPORARY_ALLOWANCE_MS) {
        temporaryAllowances[packageName] = allowanceNowMs() + durationMs
        if (hasInterventionFor(packageName)) dismissOverlay()
        onAllowanceGranted(packageName)
    }

    fun executeIntervention(
        packageName: String,
        config: InterventionConfig,
        windowId: Int? = null,
        evaluationId: Long? = null
    ): Boolean {
        if (isAppTemporarilyAllowed(packageName) ||
            (packageName == leavingPackage && allowanceNowMs() < leavingUntil)
        ) {
            return false
        }

        val existing = currentIntervention.get()
        val sameTikTokExit = existing != null && existing.phase != Phase.ACTIVE &&
            isTikTok(existing.packageName) && isTikTok(packageName)
        if (sameTikTokExit && (existing!!.phase == Phase.EXITING ||
                overlayManager?.isInterventionActive(existing.id) == true)
        ) return false
        if (existing != null && existing.packageName == packageName &&
            existing.mode == config.mode && overlayManager?.isInterventionActive(existing.id) == true
        ) {
            return false
        }
        val intervention = Intervention(
            sequence.incrementAndGet(), packageName, windowId, config.mode,
            if (sameTikTokExit) Phase.EXIT_FAILED else Phase.ACTIVE
        )
        DetectionTrace.bindIntervention(intervention.id, evaluationId)
        DetectionTrace.lifecycle(intervention.id, DetectionTrace.Lifecycle.REQUESTED, packageName, config.mode.name)
        val previous = currentIntervention.getAndSet(intervention)
        if (previous != null) {
            if (previous.phase == Phase.EXITING) onExitFinished(previous.id)
            try {
                overlayManager?.dismiss(previous.id)
            } finally {
                dispatchAudio { audioDuckingManager.restoreAudio(previous.id) }
            }
        }

        when (config.mode) {
            InterventionMode.AUTO_BACK -> {
                if (config.soundEnabled) dispatchAudio {
                    if (isActive(intervention.id)) audioDuckingManager.requestTransientFocus(intervention.id)
                }
                DetectionTrace.lifecycle(intervention.id, DetectionTrace.Lifecycle.AUTO_BACK, packageName)
                try {
                    executeBackOrHome()
                } finally {
                    finishIntervention(intervention.id)
                }
            }
            InterventionMode.MINDFUL_PAUSE, InterventionMode.STRICT_BLOCK -> {
                showOverlay(intervention, config)
                if (config.mode == InterventionMode.STRICT_BLOCK) executeBackOrHome()
            }
        }
        return true
    }

    fun onForegroundChanged(packageName: String, windowId: Int, isApplicationWindow: Boolean = true) {
        if (isApplicationWindow && packageName != leavingPackage) leavingPackage = null
        val active = currentIntervention.get() ?: return
        val instagram = isInstagram(active.packageName)
        if (!isApplicationWindow) return
        if (active.phase != Phase.ACTIVE) {
            if (isApplicationWindow && !isTikTok(packageName)) {
                DetectionTrace.lifecycle(active.id, DetectionTrace.Lifecycle.EXIT_VERIFIED, packageName)
                dismissOverlay()
                backLoopGuard.reset()
            }
            return
        }
        if (active.packageName != packageName ||
            (!instagram && active.windowId != null && active.windowId != windowId &&
                overlayManager?.isInterventionActive(active.id) != true)
        ) {
            dismissOverlay()
            backLoopGuard.reset()
        }
    }

    internal fun onScreenEvaluated(packageName: String, result: DetectionResult) {
        if (packageName == leavingPackage && result.foregroundSuppressionConfirmed) leavingPackage = null
        val active = currentIntervention.get() ?: return
        if (active.phase == Phase.ACTIVE && active.packageName == packageName &&
            result.platform == AppPlatform.fromPackageName(packageName) &&
            !result.isMatch && result.foregroundSuppressionConfirmed
        ) {
            dismissOverlay()
            backLoopGuard.reset()
        }
    }

    fun hasInterventionFor(packageName: String): Boolean =
        currentIntervention.get()?.packageName == packageName

    internal fun isActive(id: Long): Boolean = currentIntervention.get()?.let {
        it.id == id && it.phase == Phase.ACTIVE
    } == true

    fun isExitPending(interventionId: Long): Boolean = currentIntervention.get()?.let {
        it.id == interventionId && it.phase == Phase.EXITING
    } == true

    private fun isTikTok(packageName: String): Boolean =
        AppPlatform.fromPackageName(packageName) == AppPlatform.TIKTOK

    private fun isInstagram(packageName: String): Boolean =
        AppPlatform.fromPackageName(packageName) == AppPlatform.INSTAGRAM

    private fun requestTikTokExit(interventionId: Long) {
        val active = currentIntervention.get() ?: return
        if (active.id != interventionId || active.phase != Phase.ACTIVE) return
        if (!currentIntervention.compareAndSet(active, active.copy(phase = Phase.EXITING))) return
        DetectionTrace.lifecycle(interventionId, DetectionTrace.Lifecycle.EXIT_REQUESTED, active.packageName)
        try {
            onExitStarted(interventionId)
            overlayManager?.setExitState(interventionId, OverlayExitState.EXITING)
            if (!isExitPending(interventionId)) return
            if (onSystemHomeAction()) {
                DetectionTrace.lifecycle(interventionId, DetectionTrace.Lifecycle.EXIT_ACCEPTED, active.packageName)
            } else onExitVerificationTimeout(interventionId)
        } catch (_: Exception) {
            onExitVerificationTimeout(interventionId)
        }
    }

    fun onExitVerificationTimeout(interventionId: Long) {
        val active = currentIntervention.get() ?: return
        if (active.id != interventionId || active.phase != Phase.EXITING) return
        if (!currentIntervention.compareAndSet(active, active.copy(phase = Phase.EXIT_FAILED))) return
        onExitFinished(interventionId)
        DetectionTrace.lifecycle(interventionId, DetectionTrace.Lifecycle.EXIT_FAILED, active.packageName)
        try {
            overlayManager?.setExitState(interventionId, OverlayExitState.FAILED)
        } catch (_: Exception) {
            dismissOverlay()
            return
        }
        if (overlayManager?.isInterventionActive(interventionId) != true) {
            dispatchAudio { audioDuckingManager.restoreAudio(interventionId) }
        }
    }

    private fun showOverlay(intervention: Intervention, config: InterventionConfig) {
        val strict = config.mode == InterventionMode.STRICT_BLOCK
        val failed: () -> Unit = {
            if (currentIntervention.get()?.id == intervention.id) {
                try {
                    if (!strict && currentIntervention.get()?.phase == Phase.ACTIVE) executeBackOrHome()
                } finally {
                    finishIntervention(intervention.id)
                }
            }
        }
        val overlay = overlayManager
        if (overlay == null) {
            failed()
            return
        }
        try {
            overlay.show(
                interventionId = intervention.id,
                title = if (strict) "Feed blocked" else "Take a Mindful Pause",
                message = if (strict) "This short-video feed is blocked." else config.customMessage,
                holdSeconds = config.holdDurationSeconds,
                isStrictMode = strict,
                allowanceMinutes = (TEMPORARY_ALLOWANCE_MS / 60_000L).toInt(),
                hapticsEnabled = config.hapticsEnabled,
                onExitRequested = {
                    if (currentIntervention.get()?.id == intervention.id) {
                        if (isTikTok(intervention.packageName)) requestTikTokExit(intervention.id)
                        else {
                            leavingPackage = intervention.packageName
                            leavingUntil = allowanceNowMs() + BACK_TRANSITION_MS
                            dismissOverlay()
                            onFeedLeft(intervention.packageName)
                            if (!onGlobalBackAction()) redirectToHomeScreen()
                        }
                    }
                },
                onUnlocked = { finishIntervention(intervention.id, unlocked = true) },
                onDismissed = {
                    if (!isExitPending(intervention.id)) finishIntervention(intervention.id)
                },
                onFailed = failed,
                onAttached = {
                    if (config.soundEnabled) dispatchAudio {
                        if (isActive(intervention.id)) silenceAttachedOverlay(intervention)
                    }
                }
            )
            if (intervention.phase == Phase.EXIT_FAILED) {
                overlay.setExitState(intervention.id, OverlayExitState.FAILED)
            }
        } catch (_: Exception) {
            failed()
        }
    }

    private fun silenceAttachedOverlay(intervention: Intervention) {
        val tiktok = isTikTok(intervention.packageName)
        if (tiktok && audioDuckingManager.silenceOverlayMedia(intervention.id)) return

        audioDuckingManager.requestTransientFocus(intervention.id)
        if (tiktok && isActive(intervention.id)) {
            try {
                onTikTokOverlayAttached(intervention.packageName, intervention.id)
            } catch (_: Exception) {
                DetectionTrace.audio(intervention.id, "TIKTOK_PAUSE_ERROR")
            }
        }
    }

    private fun finishIntervention(id: Long, unlocked: Boolean = false) {
        val intervention = currentIntervention.get() ?: return
        if (unlocked && intervention.phase == Phase.EXITING) return
        if (intervention.id != id || !currentIntervention.compareAndSet(intervention, null)) return
        if (intervention.phase == Phase.EXITING) onExitFinished(id)
        if (unlocked) grantTemporaryAllowance(intervention.packageName)
        DetectionTrace.lifecycle(id, DetectionTrace.Lifecycle.FINISHED, intervention.packageName)
        dispatchAudio { audioDuckingManager.restoreAudio(id) }
    }

    fun executeBackOrHome(): BackActionDecision {
        val decision = backLoopGuard.recordBackAttempt()
        when (decision) {
            is BackActionDecision.ExecuteBack -> {
                val backSuccess = onGlobalBackAction()
                if (!backSuccess) {
                    redirectToHomeScreen()
                }
            }
            is BackActionDecision.FallbackToHome -> {
                redirectToHomeScreen()
                backLoopGuard.reset()
            }
            is BackActionDecision.Debounced -> Unit
        }
        return decision
    }

    fun redirectToHomeScreen() = onHomeAction()

    fun dismissOverlay() {
        val intervention = currentIntervention.getAndSet(null) ?: return
        if (intervention.phase == Phase.EXITING) onExitFinished(intervention.id)
        try {
            overlayManager?.dismiss(intervention.id)
        } finally {
            dispatchAudio { audioDuckingManager.restoreAudio(intervention.id) }
        }
    }

    fun isOverlayActive(): Boolean {
        return overlayManager?.isOverlayAttached == true
    }
}
