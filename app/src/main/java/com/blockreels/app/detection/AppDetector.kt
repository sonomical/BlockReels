package com.blockreels.app.detection

import android.view.accessibility.AccessibilityEvent
import com.blockreels.app.engine.TraversalEngine
import com.blockreels.app.model.AccessibilityNode

interface AppDetector {
    /** Target supported package names */
    val targetPackages: Set<String>

    /** Supported platform */
    val platform: AppPlatform

    /**
     * Primary evaluation entry point.
     * Traverses the node tree, evaluates disambiguation rules, and checks multi-layer heuristics.
     */
    fun evaluate(
        rootNode: AccessibilityNode?,
        event: AccessibilityEvent? = null,
        traversalEngine: TraversalEngine = TraversalEngine()
    ): DetectionResult
}
