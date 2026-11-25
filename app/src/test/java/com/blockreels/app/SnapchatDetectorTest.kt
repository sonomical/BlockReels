// SPDX-License-Identifier: MIT
package com.blockreels.app

import com.blockreels.app.detection.AppPlatform
import com.blockreels.app.detection.DetectionConfidence
import com.blockreels.app.detection.SnapchatDetector
import com.blockreels.app.detection.SuppressionReason
import com.blockreels.app.engine.TraversalEngine
import com.blockreels.app.model.SyntheticNode
import com.blockreels.app.model.syntheticTree
import com.blockreels.app.state.DetectionSensitivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SnapchatDetectorTest {

    private val detector = SnapchatDetector()
    private val traversal = TraversalEngine()

    @Test
    fun detectsPrimarySpotlightFeedContainerWithDefinitiveConfidence() {
        val root = syntheticTree(packageName = AppPlatform.SNAPCHAT.packageName) {
            node(
                viewId = "com.snapchat.android:id/spotlight_feed_container",
                bounds = intArrayOf(0, 0, 1080, 2300)
            )
        }

        val result = detector.evaluate(root, null, traversal)
        assertTrue(result.isMatch)
        assertEquals(AppPlatform.SNAPCHAT, result.platform)
        assertEquals(DetectionConfidence.DEFINITIVE, result.confidence)
        assertTrue(result.confidence.score >= DetectionSensitivity.CONSERVATIVE.minConfidenceScore)
    }

    @Test
    fun suppressesChatComposerInsideSnapchat() {
        val root = syntheticTree(packageName = AppPlatform.SNAPCHAT.packageName) {
            node(
                viewId = "com.snapchat.android:id/chat_input_text_field",
                className = "android.widget.EditText",
                isEditable = true,
                isFocused = true,
                bounds = intArrayOf(40, 1900, 960, 2050)
            )
        }

        val result = detector.evaluate(root, null, traversal)
        assertFalse(result.isMatch)
        assertEquals(SuppressionReason.CHAT_COMPOSER_ACTIVE, result.suppressionReason)
        assertTrue(result.foregroundSuppressionConfirmed)
    }

    @Test
    fun suppressesCameraCaptureScreen() {
        val root = syntheticTree(packageName = AppPlatform.SNAPCHAT.packageName) {
            node(
                viewId = "com.snapchat.android:id/camera_capture_button",
                bounds = intArrayOf(420, 1900, 660, 2140)
            )
        }

        val result = detector.evaluate(root, null, traversal)
        assertFalse(result.isMatch)
        assertEquals(SuppressionReason.CREATOR_CAMERA_ACTIVE, result.suppressionReason)
    }

    @Test
    fun detectsSpotlightOperaPlayerWhenSpotlightTabIsSelected() {
        val root = syntheticTree(packageName = AppPlatform.SNAPCHAT.packageName) {
            node(
                viewId = "com.snapchat.android:id/opera_player_container",
                bounds = intArrayOf(0, 0, 1080, 2250)
            )
        }
        val tab = SyntheticNode(
            viewIdResourceName = "com.snapchat.android:id/spotlight_tab",
            isSelected = true,
            bounds = intArrayOf(860, 2200, 1060, 2380)
        )
        root.addChild(tab)

        val result = detector.evaluate(root, null, traversal)
        assertTrue(result.isMatch)
        assertEquals(DetectionConfidence.HIGH, result.confidence)
        assertTrue(result.confidence.score >= DetectionSensitivity.BALANCED.minConfidenceScore)
        assertFalse(result.confidence.score >= DetectionSensitivity.CONSERVATIVE.minConfidenceScore)
    }
}
