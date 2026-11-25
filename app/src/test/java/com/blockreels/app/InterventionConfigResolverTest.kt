// SPDX-License-Identifier: MIT
package com.blockreels.app

import com.blockreels.app.intervention.InterventionConfigResolver
import com.blockreels.app.state.BlockingScope
import com.blockreels.app.state.InterventionConfig
import com.blockreels.app.state.InterventionMode
import com.blockreels.app.state.RuleConfig
import com.blockreels.app.state.ScheduleRule
import org.junit.Assert.assertEquals
import org.junit.Test

class InterventionConfigResolverTest {

    @Test
    fun resolvesPerAppOverridesOverGlobalDefaults() {
        val base = InterventionConfig(
            mode = InterventionMode.MINDFUL_PAUSE,
            holdDurationSeconds = 5,
            allowanceMinutes = 5,
            dailyPassLimit = 0,
            strictMode = false
        )
        val rule = RuleConfig(
            packageName = "com.instagram.android",
            isEnabled = true,
            mode = InterventionMode.REDIRECT_HOME,
            holdDurationSeconds = 12,
            scope = BlockingScope.ENTIRE_APP,
            allowanceMinutes = 3,
            dailyPassLimit = 2
        )

        val resolved = InterventionConfigResolver.resolve(base, rule)
        assertEquals(InterventionMode.REDIRECT_HOME, resolved.mode)
        assertEquals(12, resolved.holdDurationSeconds)
        assertEquals(3, resolved.allowanceMinutes)
        assertEquals(2, resolved.dailyPassLimit)
        assertEquals(BlockingScope.ENTIRE_APP, InterventionConfigResolver.resolveScope(rule))
    }

    @Test
    fun activeScheduleOverridesRuleModeAndScope() {
        val base = InterventionConfig(
            mode = InterventionMode.MINDFUL_PAUSE,
            holdDurationSeconds = 5,
            allowanceMinutes = 5,
            dailyPassLimit = 3
        )
        val rule = RuleConfig(
            packageName = "com.google.android.youtube",
            isEnabled = true,
            mode = InterventionMode.AUTO_BACK,
            scope = BlockingScope.FEED_ONLY
        )
        val schedule = ScheduleRule(
            id = "workday",
            title = "Workday",
            isActive = true,
            daysOfWeek = setOf(1, 2, 3, 4, 5),
            startMinuteOfDay = 9 * 60,
            endMinuteOfDay = 17 * 60,
            enabledPackages = setOf("com.google.android.youtube"),
            modeOverride = InterventionMode.STRICT_BLOCK,
            scopeOverride = BlockingScope.ENTIRE_APP
        )

        val resolved = InterventionConfigResolver.resolve(base, rule, schedule)
        assertEquals(InterventionMode.STRICT_BLOCK, resolved.mode)
        assertEquals(BlockingScope.ENTIRE_APP, InterventionConfigResolver.resolveScope(rule, schedule))
    }

    @Test
    fun globalStrictModeForcesStrictBlockRegardlessOfRuleOrSchedule() {
        val base = InterventionConfig(
            mode = InterventionMode.MINDFUL_PAUSE,
            strictMode = true
        )
        val rule = RuleConfig(
            packageName = "com.snapchat.android",
            isEnabled = true,
            mode = InterventionMode.AUTO_BACK
        )
        val schedule = ScheduleRule(
            id = "evening",
            title = "Evening",
            isActive = true,
            daysOfWeek = setOf(1, 2, 3, 4, 5, 6, 7),
            startMinuteOfDay = 20 * 60,
            endMinuteOfDay = 23 * 60,
            enabledPackages = setOf("com.snapchat.android"),
            modeOverride = InterventionMode.MINDFUL_PAUSE
        )

        val resolved = InterventionConfigResolver.resolve(base, rule, schedule)
        assertEquals(InterventionMode.STRICT_BLOCK, resolved.mode)
    }

    @Test
    fun fallsBackToGlobalDefaultsWhenRuleOverridesAreNull() {
        val base = InterventionConfig(
            mode = InterventionMode.MINDFUL_PAUSE,
            holdDurationSeconds = 8,
            allowanceMinutes = 10,
            dailyPassLimit = 5
        )
        val rule = RuleConfig(
            packageName = "com.facebook.katana",
            isEnabled = true,
            mode = InterventionMode.MINDFUL_PAUSE,
            holdDurationSeconds = null,
            allowanceMinutes = null,
            dailyPassLimit = null
        )

        val resolved = InterventionConfigResolver.resolve(base, rule)
        assertEquals(8, resolved.holdDurationSeconds)
        assertEquals(10, resolved.allowanceMinutes)
        assertEquals(5, resolved.dailyPassLimit)
        assertEquals(BlockingScope.FEED_ONLY, InterventionConfigResolver.resolveScope(rule))
    }
}
