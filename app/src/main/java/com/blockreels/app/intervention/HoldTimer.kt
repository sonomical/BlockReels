package com.blockreels.app.intervention

/** Shared elapsed-time requirement for touch and accessibility activation. */
internal class HoldTimer(durationSeconds: Int) {
    private val durationMs = durationSeconds.coerceAtLeast(1) * 1000L
    private var startedAt: Long? = null

    fun start(now: Long) {
        startedAt = now
    }

    fun cancel() {
        startedAt = null
    }

    private fun elapsed(now: Long): Long =
        startedAt?.let { (now - it).coerceAtLeast(0L) } ?: 0L

    fun progress(now: Long): Int =
        ((elapsed(now) * 100L) / durationMs).coerceIn(0L, 100L).toInt()

    fun remainingSeconds(now: Long): Long =
        ((durationMs - elapsed(now) + 999L) / 1000L).coerceAtLeast(0L)

    fun complete(now: Long): Boolean {
        if (startedAt == null || elapsed(now) < durationMs) return false
        cancel()
        return true
    }
}
