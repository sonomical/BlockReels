// SPDX-License-Identifier: MIT
package com.blockreels.app

import com.blockreels.app.intervention.AudioDuckingManager
import com.blockreels.app.intervention.AudioFocusClient
import com.blockreels.app.intervention.BackLoopGuard
import com.blockreels.app.intervention.FocusChange
import com.blockreels.app.intervention.InterventionController
import com.blockreels.app.intervention.InterventionOverlay
import com.blockreels.app.state.AudioAction
import com.blockreels.app.state.InterventionConfig
import com.blockreels.app.state.InterventionMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InterventionControllerEngineTest {

    private class FakeAudioFocusClient : AudioFocusClient {
        var requests = 0
        var abandons = 0
        override fun requestFocus(interventionId: Long, onChange: (FocusChange) -> Unit): Boolean {
            requests++
            return true
        }
        override fun abandonFocus() {
            abandons++
        }
    }

    private class FakeOverlay : InterventionOverlay {
        override var isOverlayAttached: Boolean = false
        var lastStrict: Boolean? = null
        var lastTitle: String? = null
        var lastAllowanceMinutes: Int? = null
        var lastDailyPassLimit: Int? = null
        var lastPassesUsed: Int? = null
        var lastUnlockCallback: (() -> Unit)? = null
        var lastAttachCallback: (() -> Unit)? = null

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
            isOverlayAttached = true
            lastTitle = title
            lastStrict = isStrictMode
            lastAllowanceMinutes = allowanceMinutes
            lastDailyPassLimit = dailyPassLimit
            lastPassesUsed = passesUsedToday
            lastUnlockCallback = onUnlocked
            lastAttachCallback = onAttached
            onAttached()
        }

        override fun dismiss(interventionId: Long) {
            isOverlayAttached = false
        }
    }

    @Test
    fun redirectHomeModeTriggersSystemHomeAndRestoresAudio() {
        var now = 1_000L
        var systemHomeCalls = 0
        val audioClient = FakeAudioFocusClient()
        val controller = InterventionController(
            backLoopGuard = BackLoopGuard(clockMs = { now }),
            audioDuckingManager = AudioDuckingManager(audioClient),
            onGlobalBackAction = { false },
            onHomeAction = {},
            overlayManager = FakeOverlay(),
            allowanceNowMs = { now },
            onSystemHomeAction = {
                systemHomeCalls++
                true
            }
        )

        val config = InterventionConfig(
            mode = InterventionMode.REDIRECT_HOME,
            audioAction = AudioAction.DUCK
        )
        val triggered = controller.executeIntervention("com.instagram.android", config)

        assertTrue(triggered)
        assertEquals(1, systemHomeCalls)
        assertEquals(1, audioClient.requests)
        assertEquals(1, audioClient.abandons)
    }

    @Test
    fun audioActionNoneSkipsAudioFocusRequests() {
        val audioClient = FakeAudioFocusClient()
        val overlay = FakeOverlay()
        val controller = InterventionController(
            backLoopGuard = BackLoopGuard(),
            audioDuckingManager = AudioDuckingManager(audioClient),
            onGlobalBackAction = { true },
            onHomeAction = {},
            overlayManager = overlay,
            getPassesUsedToday = { 0 },
            recordPassUsed = {}
        )

        val config = InterventionConfig(
            mode = InterventionMode.MINDFUL_PAUSE,
            audioAction = AudioAction.NONE,
            soundEnabled = false
        )
        assertTrue(controller.executeIntervention("com.instagram.android", config))
        assertEquals(0, audioClient.requests)
    }

    @Test
    fun mindfulUnlockGrantsConfiguredAllowanceAndRevocationReEnablesBlocking() {
        var now = 10_000L
        val audioClient = FakeAudioFocusClient()
        val overlay = FakeOverlay()
        var recordedPasses = 0
        val controller = InterventionController(
            backLoopGuard = BackLoopGuard(clockMs = { now }),
            audioDuckingManager = AudioDuckingManager(audioClient),
            onGlobalBackAction = { true },
            onHomeAction = {},
            overlayManager = overlay,
            allowanceNowMs = { now },
            getPassesUsedToday = { recordedPasses },
            recordPassUsed = { recordedPasses++ }
        )

        val config = InterventionConfig(
            mode = InterventionMode.MINDFUL_PAUSE,
            holdDurationSeconds = 5,
            allowanceMinutes = 10,
            dailyPassLimit = 3
        )
        assertTrue(controller.executeIntervention("com.google.android.youtube", config))
        assertEquals(10, overlay.lastAllowanceMinutes)
        assertEquals(false, overlay.lastStrict)

        overlay.lastUnlockCallback?.invoke()

        assertEquals(1, recordedPasses)
        assertTrue(controller.isAppTemporarilyAllowed("com.google.android.youtube"))
        assertEquals(10 * 60_000L, controller.temporaryAllowanceRemainingMs("com.google.android.youtube"))
        assertFalse(controller.executeIntervention("com.google.android.youtube", config))

        // Revoke active pass -> intervention executes immediately again
        assertTrue(controller.revokeTemporaryAllowance("com.google.android.youtube"))
        assertFalse(controller.isAppTemporarilyAllowed("com.google.android.youtube"))
        assertTrue(controller.executeIntervention("com.google.android.youtube", config))
    }

    @Test
    fun dailyPassQuotaExhaustedForcesStrictOverlayLock() {
        val overlay = FakeOverlay()
        val controller = InterventionController(
            backLoopGuard = BackLoopGuard(),
            audioDuckingManager = AudioDuckingManager(FakeAudioFocusClient()),
            onGlobalBackAction = { true },
            onHomeAction = {},
            overlayManager = overlay,
            getPassesUsedToday = { 3 },
            recordPassUsed = {}
        )

        val config = InterventionConfig(
            mode = InterventionMode.MINDFUL_PAUSE,
            dailyPassLimit = 3
        )
        assertTrue(controller.executeIntervention("com.instagram.android", config))
        assertEquals(true, overlay.lastStrict)
        assertEquals("Daily passes used", overlay.lastTitle)
        assertEquals(3, overlay.lastDailyPassLimit)
        assertEquals(3, overlay.lastPassesUsed)
    }
}
