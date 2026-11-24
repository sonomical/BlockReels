package com.blockreels.app.engine

/** Two fresh-root checks per event burst; callbacks never retain an event or node. */
internal class ViewerScreenRecheck(
    private val postDelayed: (Runnable, Long) -> Unit,
    private val removeCallbacks: (Runnable) -> Unit,
    private val firstDelayMs: Long = 150L,
    private val secondDelayMs: Long = 350L,
    private val evaluateFreshScreen: () -> Boolean
) {
    private var pending: Runnable? = null
    private var attempt = 0
    val isPending: Boolean get() = pending != null
    val isFinalAttempt: Boolean get() = attempt == 1

    fun request() {
        if (pending != null) return
        attempt = 0
        scheduleNext()
    }

    fun cancel() {
        pending?.let(removeCallbacks)
        pending = null
        attempt = 0
    }

    private fun scheduleNext() {
        val delayMs = if (attempt == 0) firstDelayMs else secondDelayMs
        val callback = object : Runnable {
            override fun run() {
                if (pending !== this) return
                val retry = evaluateFreshScreen()
                if (pending !== this) return
                pending = null
                attempt++
                if (retry && attempt < 2) scheduleNext()
            }
        }
        pending = callback
        postDelayed(callback, delayMs)
    }
}
