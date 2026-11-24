package com.blockreels.app.intervention

sealed class BackActionDecision {
    data class ExecuteBack(val attempt: Int) : BackActionDecision()
    data object FallbackToHome : BackActionDecision()
    data object Debounced : BackActionDecision()
}

class BackLoopGuard(
    val maxRetries: Int = 2,
    val timeWindowMs: Long = 1200L,
    val debounceCooldownMs: Long = 350L,
    private val clockMs: () -> Long = System::currentTimeMillis
) {
    private var backAttemptCount = 0
    private var lastBackTimestamp = 0L

    fun recordBackAttempt(now: Long = clockMs()): BackActionDecision {
        if (now - lastBackTimestamp < debounceCooldownMs) {
            return BackActionDecision.Debounced
        }

        if (now - lastBackTimestamp > timeWindowMs) {
            backAttemptCount = 0
        }

        backAttemptCount++
        lastBackTimestamp = now

        return if (backAttemptCount > maxRetries) {
            BackActionDecision.FallbackToHome
        } else {
            BackActionDecision.ExecuteBack(attempt = backAttemptCount)
        }
    }

    fun reset() {
        backAttemptCount = 0
        lastBackTimestamp = 0L
    }
}
