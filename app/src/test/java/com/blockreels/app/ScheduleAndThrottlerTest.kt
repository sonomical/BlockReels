// SPDX-License-Identifier: MIT
package com.blockreels.app

import android.view.accessibility.AccessibilityEvent
import com.blockreels.app.engine.EventThrottler
import com.blockreels.app.state.BlockingScope
import com.blockreels.app.state.DetectionSensitivity
import com.blockreels.app.state.InterventionMode
import com.blockreels.app.state.ScheduleRule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleAndThrottlerTest {

    @Test
    fun daytimeScheduleMatchesOnlyInsideWindowAndSelectedDays() {
        val rule = ScheduleRule(
            id = "workday",
            title = "Workday",
            isActive = true,
            daysOfWeek = setOf(1, 2, 3, 4, 5),
            startMinuteOfDay = 9 * 60,
            endMinuteOfDay = 17 * 60,
            enabledPackages = setOf("com.instagram.android"),
            modeOverride = InterventionMode.STRICT_BLOCK,
            scopeOverride = BlockingScope.ENTIRE_APP
        )

        // Monday 10:00 -> active
        assertTrue(rule.isTimeActive(1, 7, 10 * 60, "com.instagram.android"))
        // Monday 18:00 -> outside window
        assertFalse(rule.isTimeActive(1, 7, 18 * 60, "com.instagram.android"))
        // Saturday 10:00 -> weekend excluded
        assertFalse(rule.isTimeActive(6, 5, 10 * 60, "com.instagram.android"))
        // Unlisted package -> excluded
        assertFalse(rule.isTimeActive(1, 7, 10 * 60, "com.snapchat.android"))
    }

    @Test
    fun overnightScheduleSpansMidnightUsingPreviousIsoDay() {
        val nightRule = ScheduleRule(
            id = "night",
            title = "Night",
            isActive = true,
            daysOfWeek = setOf(1), // Starts Monday night 22:30 -> Tuesday 07:00
            startMinuteOfDay = 22 * 60 + 30,
            endMinuteOfDay = 7 * 60,
            enabledPackages = emptySet(),
            modeOverride = InterventionMode.REDIRECT_HOME
        )

        // Monday 23:00 -> active
        assertTrue(nightRule.isTimeActive(1, 7, 23 * 60, "com.instagram.android"))
        // Tuesday 06:30 (previousIsoDay = 1) -> active
        assertTrue(nightRule.isTimeActive(2, 1, 6 * 60 + 30, "com.instagram.android"))
        // Tuesday 07:00 -> ended
        assertFalse(nightRule.isTimeActive(2, 1, 7 * 60, "com.instagram.android"))
        // Wednesday 06:30 (previousIsoDay = 2) -> inactive
        assertFalse(nightRule.isTimeActive(3, 2, 6 * 60 + 30, "com.instagram.android"))
    }

    @Test
    fun quickBoostRuleExpiresAfterConfiguredDuration() {
        val startedAt = 1_700_000_000_000L
        val boost = ScheduleRule(
            id = "${ScheduleRule.QUICK_BOOST_ID_PREFIX}$startedAt",
            title = "Focus Boost",
            isActive = true,
            daysOfWeek = setOf(1, 2, 3, 4, 5, 6, 7),
            startMinuteOfDay = 600,
            endMinuteOfDay = 630, // 30 minutes
            enabledPackages = emptySet(),
            modeOverride = InterventionMode.STRICT_BLOCK
        )

        assertTrue(boost.isTimeActive(1, 7, 610, "com.instagram.android", nowMillis = startedAt + 15 * 60_000L))
        assertFalse(boost.isTimeActive(1, 7, 631, "com.instagram.android", nowMillis = startedAt + 31 * 60_000L))
    }

    @Test
    fun eventThrottlerRespectsDynamicSensitivityWindowMs() {
        val throttler = EventThrottler(debounceWindowMs = 75L)
        val pkg = "com.instagram.android"

        // First content event passes
        assertTrue(
            throttler.shouldProcess(
                packageName = pkg,
                eventType = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                isOverlayActive = false,
                timestamp = 1_000L,
                windowMs = DetectionSensitivity.AGGRESSIVE.debounceWindowMs
            )
        )
        // 45ms later: aggressive (40ms) passes, conservative (150ms) would block
        assertFalse(
            throttler.shouldProcess(
                packageName = pkg,
                eventType = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                isOverlayActive = false,
                timestamp = 1_045L,
                windowMs = DetectionSensitivity.CONSERVATIVE.debounceWindowMs
            )
        )
        assertTrue(
            throttler.shouldProcess(
                packageName = pkg,
                eventType = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                isOverlayActive = false,
                timestamp = 1_045L,
                windowMs = DetectionSensitivity.AGGRESSIVE.debounceWindowMs
            )
        )
    }
}
