package com.blockreels.app.intervention

import android.view.MotionEvent
import com.blockreels.app.engine.DetectionTrace

/** One overlay's gesture, elapsed timer, parent-interception lock and callback. */
internal class HoldGestureController(
    private val interventionId: Long,
    durationSeconds: Int,
    private val touchSlop: Float,
    private val nowMs: () -> Long,
    private val postDelayed: (Runnable, Long) -> Unit,
    private val removeCallback: (Runnable) -> Unit,
    private val ownerIsActive: () -> Boolean,
    private val disallowParentInterception: (Boolean) -> Unit,
    private val render: (State, Int, Long) -> Unit,
    private val onCompleted: () -> Unit
) {
    enum class State { IDLE, PHYSICAL, ACCESSIBLE, CANCELED, COMPLETED, CLOSED }

    data class Bounds(val left: Float, val top: Float, val right: Float, val bottom: Float) {
        companion object {
            fun fromDown(
                rawX: Float, rawY: Float, localX: Float, localY: Float,
                width: Int, height: Int
            ): Bounds {
                val left = rawX - localX
                val top = rawY - localY
                return Bounds(left, top, left + width, top + height)
            }
        }

        fun contains(x: Float, y: Float, slop: Float): Boolean = right > left && bottom > top &&
            x >= left - slop && x <= right + slop && y >= top - slop && y <= bottom + slop
    }

    private val timer = HoldTimer(durationSeconds)
    private var pending: Runnable? = null
    private var startedAt: Long? = null
    private var enabled = true
    private var physicalSequence = false
    private var parentLocked = false
    private var pressBounds: Bounds? = null
    var activePointerId: Int? = null
        private set
    var state = State.IDLE
        private set

    fun onTouch(
        actionMasked: Int, changedPointerId: Int, activeX: Float?, activeY: Float?,
        downBounds: Bounds? = null, eventTimeUptimeMs: Long? = null
    ): Boolean {
        record("TOUCH", actionMasked, changedPointerId, eventTimeUptimeMs)
        if (state == State.CLOSED || state == State.COMPLETED) return true
        if (!ownerIsActive()) {
            close()
            return true
        }
        when (actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                physicalSequence = true
                stopSession(notify = false)
                if (enabled && activeX != null && activeY != null &&
                    downBounds?.contains(activeX, activeY, touchSlop) == true
                ) {
                    activePointerId = changedPointerId
                    pressBounds = downBounds
                    setParentLocked(true)
                    start(State.PHYSICAL)
                }
            }
            MotionEvent.ACTION_MOVE -> if (state == State.PHYSICAL) {
                if (activeX == null || activeY == null ||
                    pressBounds?.contains(activeX, activeY, touchSlop) != true
                ) {
                    stopSession(keepParentLock = true)
                }
            }
            MotionEvent.ACTION_POINTER_UP -> if (changedPointerId == activePointerId) {
                stopSession(keepParentLock = true)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_OUTSIDE -> {
                if (state == State.PHYSICAL) stopSession()
                physicalSequence = false
                setParentLocked(false)
            }
            MotionEvent.ACTION_POINTER_DOWN -> Unit
        }
        return true
    }

    fun activateAccessibility(): Boolean {
        if (physicalSequence || !enabled || state == State.CLOSED || state == State.COMPLETED) return false
        if (!ownerIsActive()) {
            close()
            return false
        }
        if (state == State.ACCESSIBLE) stopSession() else {
            stopSession(notify = false)
            start(State.ACCESSIBLE)
        }
        return true
    }

    fun setEnabled(value: Boolean) {
        enabled = value
        if (!value) stopSession()
    }

    fun close() {
        if (state == State.CLOSED) return
        record("CLOSE")
        stopSession(notify = false)
        physicalSequence = false
        state = State.CLOSED
    }

    private fun start(mode: State) {
        val now = nowMs()
        startedAt = now
        timer.start(now)
        state = mode
        record("START")
        render(mode, 0, timer.remainingSeconds(now))
        scheduleTick()
    }

    private fun scheduleTick() {
        lateinit var task: Runnable
        task = Runnable {
            if (pending !== task) return@Runnable
            pending = null
            if (!ownerIsActive()) {
                close()
                return@Runnable
            }
            if (!enabled || (state != State.PHYSICAL && state != State.ACCESSIBLE)) return@Runnable
            val now = nowMs()
            val progress = timer.progress(now)
            val remaining = timer.remainingSeconds(now)
            if (timer.complete(now)) {
                state = State.COMPLETED
                activePointerId = null
                pressBounds = null
                setParentLocked(false)
                record("COMPLETE")
                render(state, 100, 0L)
                onCompleted()
            } else {
                render(state, progress, remaining)
                if (state == State.PHYSICAL || state == State.ACCESSIBLE) scheduleTick()
            }
        }
        pending = task
        postDelayed(task, 50L)
    }

    private fun stopSession(notify: Boolean = true, keepParentLock: Boolean = false) {
        val wasRunning = state == State.PHYSICAL || state == State.ACCESSIBLE
        if (wasRunning) record("CANCEL")
        pending?.let(removeCallback)
        pending = null
        timer.cancel()
        startedAt = null
        activePointerId = null
        pressBounds = null
        if (!keepParentLock) setParentLocked(false)
        if (state != State.CLOSED && state != State.COMPLETED) {
            state = if (notify && wasRunning) State.CANCELED else State.IDLE
            if (notify && wasRunning) render(state, 0, timer.remainingSeconds(nowMs()))
        }
    }

    private fun setParentLocked(value: Boolean) {
        if (parentLocked == value) return
        parentLocked = value
        disallowParentInterception(value)
    }

    private fun record(
        event: String, action: Int? = null, changedPointer: Int? = null,
        eventTimeUptimeMs: Long? = null
    ) {
        if (!DetectionTrace.enabled) return
        val now = nowMs()
        DetectionTrace.hold(
            interventionId, event, action, activePointerId, state.name,
            now, startedAt?.let { now - it }, ownerIsActive(), parentLocked, changedPointer, eventTimeUptimeMs
        )
    }
}
