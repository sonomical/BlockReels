package com.blockreels.app.intervention

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.ProgressBar
import android.widget.TextView
import com.blockreels.app.R
import com.blockreels.app.engine.DetectionTrace

enum class OverlayExitState { EXITING, FAILED }

interface InterventionOverlay {
    val isOverlayAttached: Boolean
    fun isInterventionActive(interventionId: Long): Boolean = isOverlayAttached
    fun setExitState(interventionId: Long, state: OverlayExitState) = Unit
    fun show(
        interventionId: Long,
        title: String,
        message: String,
        holdSeconds: Int,
        isStrictMode: Boolean,
        allowanceMinutes: Int,
        hapticsEnabled: Boolean,
        dailyPassLimit: Int = 0,
        passesUsedToday: Int = 0,
        onExitRequested: () -> Unit,
        onUnlocked: () -> Unit,
        onDismissed: () -> Unit,
        onFailed: () -> Unit,
        onAttached: () -> Unit = {}
    )
    fun dismiss(interventionId: Long)
}

class OverlayManager(private val context: Context) : InterventionOverlay {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
    private val mainHandler = Handler(Looper.getMainLooper())

    private var overlayView: View? = null
    override val isOverlayAttached: Boolean
        get() = attachedIntervention != null && overlayView?.isAttachedToWindow == true

    override fun isInterventionActive(interventionId: Long): Boolean =
        pendingIntervention == interventionId ||
            (attachedIntervention == interventionId && isOverlayAttached)

    private var holdGesture: HoldGestureController? = null
    private var holdOwnerId: Long? = null
    private var requestedIntervention: Long? = null
    private var pendingIntervention: Long? = null
    private var attachedIntervention: Long? = null
    private var onAttachedDismissed: (() -> Unit)? = null
    private var exitStateId: Long? = null
    private var exitState: OverlayExitState? = null
    private var currentHoldSeconds = 0

    override fun setExitState(interventionId: Long, state: OverlayExitState) {
        if (requestedIntervention != interventionId) return
        exitStateId = interventionId
        exitState = state
        if (holdOwnerId == interventionId) holdGesture?.setEnabled(state != OverlayExitState.EXITING)
        mainHandler.post {
            if (requestedIntervention != interventionId || exitStateId != interventionId) return@post
            val view = overlayView ?: return@post
            view.findViewById<TextView>(R.id.btnGoBack)?.apply {
                isEnabled = false
                text = context.getString(
                    if (state == OverlayExitState.EXITING) R.string.exit_in_progress
                    else R.string.exit_not_confirmed
                )
            }
            view.findViewById<TextView>(R.id.tvHoldButtonText)?.isEnabled = state != OverlayExitState.EXITING
            view.findViewById<ProgressBar>(R.id.pbHoldProgress)?.progress = 0
            view.findViewById<TextView>(R.id.tvHoldButtonText)?.text = "Hold for ${currentHoldSeconds}s"
            if (state == OverlayExitState.FAILED) {
                view.findViewById<TextView>(R.id.tvOverlayMessage)?.apply {
                    visibility = View.VISIBLE
                    text = context.getString(R.string.exit_not_confirmed_help)
                }
            }
        }
    }

    override fun show(
        interventionId: Long,
        title: String,
        message: String,
        holdSeconds: Int,
        isStrictMode: Boolean,
        allowanceMinutes: Int,
        hapticsEnabled: Boolean,
        dailyPassLimit: Int,
        passesUsedToday: Int,
        onExitRequested: () -> Unit,
        onUnlocked: () -> Unit,
        onDismissed: () -> Unit,
        onFailed: () -> Unit,
        onAttached: () -> Unit
    ) {
        if (requestedIntervention == interventionId && isInterventionActive(interventionId)) return
        endHoldGesture()
        requestedIntervention = interventionId
        pendingIntervention = interventionId
        exitStateId = null
        exitState = null
        currentHoldSeconds = holdSeconds
        DetectionTrace.lifecycle(interventionId, DetectionTrace.Lifecycle.SHOW_QUEUED)
        removeAttachedOverlay()
        if (windowManager == null) {
            DetectionTrace.lifecycle(interventionId, DetectionTrace.Lifecycle.FAILED)
            requestedIntervention = null
            pendingIntervention = null
            onFailed()
            return
        }
        try {
            val inflater = LayoutInflater.from(context)
            val view = inflater.inflate(R.layout.block_overlay, null)
            overlayView = view
            view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(attached: View) {
                    if (pendingIntervention == interventionId &&
                        requestedIntervention == interventionId && overlayView === attached
                    ) {
                        pendingIntervention = null
                        DetectionTrace.lifecycle(interventionId, DetectionTrace.Lifecycle.ATTACHED)
                        mainHandler.post {
                            if (requestedIntervention == interventionId &&
                                attachedIntervention == interventionId && isOverlayAttached
                            ) onAttached()
                        }
                    }
                }

                override fun onViewDetachedFromWindow(detached: View) {
                    if (attachedIntervention == interventionId && overlayView === detached) {
                        if (requestedIntervention == interventionId) requestedIntervention = null
                        removeAttachedOverlay(removeWindow = false)
                    }
                }
            })

            val tvEyebrow = view.findViewById<TextView>(R.id.tvOverlayEyebrow)
            val tvTitle = view.findViewById<TextView>(R.id.tvOverlayTitle)
            val tvMessage = view.findViewById<TextView>(R.id.tvOverlayMessage)
            val tvHoldText = view.findViewById<TextView>(R.id.tvHoldButtonText)
            val tvHoldHelp = view.findViewById<TextView>(R.id.tvHoldHelp)
            val pbProgress = view.findViewById<ProgressBar>(R.id.pbHoldProgress)
            val btnGoBack = view.findViewById<View>(R.id.btnGoBack)
            val holdSection = view.findViewById<View>(R.id.holdSection)

            val strongSingleMessage = message.trim().ifBlank { title }
            tvTitle?.text = strongSingleMessage
            tvMessage?.visibility = View.GONE

            btnGoBack?.setOnClickListener {
                if (requestedIntervention != interventionId || exitStateId == interventionId) return@setOnClickListener
                try {
                    onExitRequested()
                } finally {
                    if (exitStateId != interventionId) dismiss(interventionId)
                }
            }
            btnGoBack?.accessibilityDelegate = object : View.AccessibilityDelegate() {
                override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    info.className = "android.widget.Button"
                }
            }

            if (isStrictMode) {
                holdSection?.visibility = View.GONE
                val quotaLocked = dailyPassLimit > 0 && passesUsedToday >= dailyPassLimit
                tvEyebrow?.text = if (quotaLocked) "DAILY LIMIT" else "FOCUS LOCK"
                tvTitle?.text = if (quotaLocked) {
                    context.getString(R.string.quota_exhausted_message)
                } else {
                    context.getString(R.string.strict_mode_active)
                }
            } else {
                holdSection?.visibility = View.VISIBLE
                tvEyebrow?.text = "MINDFUL PAUSE"
                tvHoldText?.text = "Hold for ${holdSeconds}s"
                val quotaSuffix = if (dailyPassLimit > 0) {
                    val remaining = (dailyPassLimit - passesUsedToday).coerceAtLeast(0)
                    " · $remaining of $dailyPassLimit passes left today"
                } else ""
                tvHoldHelp?.text =
                    "Hold ${holdSeconds}s for a ${allowanceMinutes}m pass$quotaSuffix"
                if (pbProgress != null && tvHoldText != null) {
                    setupHoldTouchListener(
                        tvHoldText, pbProgress, holdSeconds, allowanceMinutes,
                        hapticsEnabled, interventionId, onUnlocked
                    )
                }
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }

            attachedIntervention = interventionId
            onAttachedDismissed = onDismissed
            windowManager.addView(view, params)
        } catch (_: Exception) {
            DetectionTrace.lifecycle(interventionId, DetectionTrace.Lifecycle.FAILED)
            if (requestedIntervention == interventionId) requestedIntervention = null
            if (pendingIntervention == interventionId) pendingIntervention = null
            try {
                onFailed()
            } finally {
                removeAttachedOverlay()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupHoldTouchListener(
        button: TextView,
        progressBar: ProgressBar,
        durationSec: Int,
        allowanceMinutes: Int,
        hapticsEnabled: Boolean,
        interventionId: Long,
        onUnlocked: () -> Unit
    ) {
        val instruction =
            "Hold for $durationSec seconds to allow this app for $allowanceMinutes minutes. Activate to start timed hold; activate again to cancel."
        button.accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE

        fun updateLabel(text: String, description: String) {
            if (button.text.toString() != text) button.text = text
            if (button.contentDescription != description) button.contentDescription = description
        }

        var previousState = HoldGestureController.State.IDLE
        var resetLabel: Runnable? = null
        val gesture = HoldGestureController(
            interventionId = interventionId,
            durationSeconds = durationSec,
            touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat(),
            nowMs = SystemClock::elapsedRealtime,
            postDelayed = { task, delay -> mainHandler.postDelayed(task, delay); Unit },
            removeCallback = { mainHandler.removeCallbacks(it) },
            ownerIsActive = {
                requestedIntervention == interventionId && attachedIntervention == interventionId &&
                    button.isAttachedToWindow
            },
            disallowParentInterception = { button.parent?.requestDisallowInterceptTouchEvent(it) },
            render = { state, progress, remaining ->
                resetLabel?.let { mainHandler.removeCallbacks(it) }
                resetLabel = null
                progressBar.progress = progress
                when (state) {
                    HoldGestureController.State.PHYSICAL, HoldGestureController.State.ACCESSIBLE -> {
                        if (state != previousState && hapticsEnabled) triggerHapticTick()
                        val accessible = state == HoldGestureController.State.ACCESSIBLE
                        updateLabel(
                            "${if (accessible) "Timed hold" else "Keep holding"} · ${remaining}s",
                            "$remaining seconds remaining. ${if (accessible) "Activate" else "Release"} to cancel."
                        )
                    }
                    HoldGestureController.State.CANCELED -> {
                        updateLabel("Canceled · hold for ${durationSec}s", "Canceled. $instruction")
                        if (hapticsEnabled) triggerCancelHaptic()
                        val reset = Runnable {
                            resetLabel = null
                            if (requestedIntervention == interventionId && holdOwnerId == interventionId &&
                                button.isAttachedToWindow
                            ) {
                                updateLabel("Hold for ${durationSec}s", instruction)
                            }
                        }
                        resetLabel = reset
                        mainHandler.postDelayed(reset, 650L)
                    }
                    HoldGestureController.State.COMPLETED -> {
                        if (hapticsEnabled) triggerSuccessHaptic()
                        val success = "Allowed for $allowanceMinutes minutes"
                        updateLabel(success, success)
                        @Suppress("DEPRECATION")
                        button.announceForAccessibility(success)
                    }
                    else -> Unit
                }
                previousState = state
            },
            onCompleted = { try { onUnlocked() } finally { dismiss(interventionId) } }
        )
        holdGesture = gesture
        holdOwnerId = interventionId
        button.contentDescription = instruction
        button.accessibilityDelegate = object : View.AccessibilityDelegate() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.className = "android.widget.Button"
                info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK)
            }

            override fun performAccessibilityAction(host: View, action: Int, args: Bundle?): Boolean {
                if (action == AccessibilityNodeInfo.ACTION_CLICK) return gesture.activateAccessibility()
                return super.performAccessibilityAction(host, action, args)
            }
        }
        button.setOnKeyListener { _, key, event ->
            if (key == KeyEvent.KEYCODE_ENTER || key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_SPACE) {
                if (event.action == KeyEvent.ACTION_UP) gesture.activateAccessibility()
                true
            } else false
        }
        button.setOnTouchListener { _, event ->
            val action = event.actionMasked
            val changed = if (event.actionIndex in 0 until event.pointerCount)
                event.getPointerId(event.actionIndex) else -1
            val pointer = if (action == MotionEvent.ACTION_DOWN) changed else gesture.activePointerId ?: changed
            val index = event.findPointerIndex(pointer)
            val x = if (index < 0) null else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                event.getRawX(index)
            } else event.rawX + event.getX(index) - event.getX(0)
            val y = if (index < 0) null else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                event.getRawY(index)
            } else event.rawY + event.getY(index) - event.getY(0)
            val bounds = if (action == MotionEvent.ACTION_DOWN) {
                HoldGestureController.Bounds.fromDown(
                    event.rawX, event.rawY, event.x, event.y,
                    button.width, button.height
                )
            } else null
            gesture.onTouch(action, changed, x, y, bounds, eventTimeUptimeMs = event.eventTime)
        }
        button.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) = Unit
            override fun onViewDetachedFromWindow(view: View) {
                resetLabel?.let { mainHandler.removeCallbacks(it) }
                resetLabel = null
                gesture.close()
            }
        })
    }

    private fun endHoldGesture(interventionId: Long? = holdOwnerId) {
        if (interventionId != holdOwnerId) return
        holdGesture?.close()
        holdGesture = null
        holdOwnerId = null
    }

    override fun dismiss(interventionId: Long) {
        endHoldGesture(interventionId)
        if (requestedIntervention == interventionId) requestedIntervention = null
        if (pendingIntervention == interventionId) pendingIntervention = null
        if (attachedIntervention == interventionId) removeAttachedOverlay()
    }

    private fun removeAttachedOverlay(removeWindow: Boolean = true) {
        endHoldGesture(attachedIntervention)
        val view = overlayView
        val dismissed = onAttachedDismissed
        if (exitStateId == attachedIntervention) {
            exitStateId = null
            exitState = null
        }
        if (pendingIntervention == attachedIntervention) pendingIntervention = null
        attachedIntervention?.let { DetectionTrace.lifecycle(it, DetectionTrace.Lifecycle.DISMISSED) }
        overlayView = null
        attachedIntervention = null
        onAttachedDismissed = null
        try {
            if (removeWindow && view != null) windowManager?.removeViewImmediate(view)
        } catch (_: Exception) {
            // The window may already have been removed by Android.
        } finally {
            dismissed?.invoke()
        }
    }

    private fun triggerHapticTick() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(30)
            }
        } catch (_: Exception) {}
    }

    private fun triggerCancelHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(50)
            }
        } catch (_: Exception) {}
    }

    private fun triggerSuccessHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 80, 50, 120), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(200)
            }
        } catch (_: Exception) {}
    }
}
