// SPDX-License-Identifier: MIT
package com.blockreels.app.intervention

import com.blockreels.app.state.BlockingScope
import com.blockreels.app.state.InterventionConfig
import com.blockreels.app.state.InterventionMode
import com.blockreels.app.state.RuleConfig
import com.blockreels.app.state.ScheduleRule

/**
 * Resolves the effective intervention configuration and blocking scope for a package.
 *
 * Precedence (lowest to highest):
 * 1. Global [InterventionConfig] defaults
 * 2. Per-app [RuleConfig] overrides (mode, scope, hold duration, pass length, daily pass cap)
 * 3. Active [ScheduleRule] overrides (modeOverride, scopeOverride)
 * 4. Global strict mode (forces [InterventionMode.STRICT_BLOCK])
 */
object InterventionConfigResolver {
    fun resolve(
        base: InterventionConfig,
        rule: RuleConfig?,
        activeSchedule: ScheduleRule? = null
    ): InterventionConfig {
        val withRule = if (rule == null) {
            base
        } else {
            base.copy(
                mode = rule.mode,
                holdDurationSeconds = rule.holdDurationSeconds ?: base.holdDurationSeconds,
                allowanceMinutes = rule.allowanceMinutes ?: base.allowanceMinutes,
                dailyPassLimit = rule.dailyPassLimit ?: base.dailyPassLimit
            )
        }
        val withSchedule = activeSchedule?.modeOverride?.let { override ->
            withRule.copy(mode = override)
        } ?: withRule

        return if (base.strictMode) {
            withSchedule.copy(mode = InterventionMode.STRICT_BLOCK)
        } else {
            withSchedule
        }
    }

    fun resolveScope(
        rule: RuleConfig?,
        activeSchedule: ScheduleRule? = null
    ): BlockingScope {
        return activeSchedule?.scopeOverride ?: rule?.scope ?: BlockingScope.FEED_ONLY
    }
}
