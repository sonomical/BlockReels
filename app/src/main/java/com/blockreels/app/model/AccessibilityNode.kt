package com.blockreels.app.model

import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Common abstraction over accessibility node trees.
 * Decouples detection and disambiguation algorithms from Android's platform-bound AccessibilityNodeInfo,
 * allowing 100% deterministic, high-speed JVM unit testing without Android emulator overhead.
 */
interface AccessibilityNode {
    val viewIdResourceName: String?
    val className: CharSequence?
    val text: CharSequence?
    val contentDescription: CharSequence?
    val packageName: CharSequence?
    val childCount: Int
    val isClickable: Boolean
    val isEditable: Boolean
    val isPassword: Boolean get() = false
    val isInputField: Boolean get() = isEditable || isPassword ||
        className?.contains("EditText", ignoreCase = true) == true
    val displayText: CharSequence? get() = if (isInputField) null else text
    val displayDescription: CharSequence? get() = if (isInputField) null else contentDescription
    val isVisibleToUser: Boolean get() = true
    val isFocused: Boolean get() = false
    val isSelected: Boolean get() = false
    val isScrollable: Boolean get() = false
    val isLongClickable: Boolean get() = false
    val parent: AccessibilityNode?

    fun findByViewId(viewId: String): List<AccessibilityNode> = emptyList()
    fun getChild(index: Int): AccessibilityNode?
    fun getBoundsInScreen(outBounds: Rect)

    /** Pure-Kotlin bounds as [left, top, right, bottom], safe in JVM unit tests. */
    fun getBoundsInScreenArray(): IntArray

    fun recycle()
}

/**
 * Real production implementation wrapping Android OS [AccessibilityNodeInfo].
 */
class RealAccessibilityNode(
    val realNode: AccessibilityNodeInfo
) : AccessibilityNode {

    override val isVisibleToUser: Boolean
        get() = try { realNode.isVisibleToUser } catch (_: Exception) { false }

    override val isFocused: Boolean
        get() = try { realNode.isFocused } catch (_: Exception) { false }

    override val isSelected: Boolean
        get() = realNode.isSelected

    override val isScrollable: Boolean
        get() = realNode.isScrollable

    override val isLongClickable: Boolean
        get() = realNode.isLongClickable

    override fun findByViewId(viewId: String): List<AccessibilityNode> =
        runCatching { realNode.findAccessibilityNodeInfosByViewId(viewId).map(::RealAccessibilityNode) }
            .getOrDefault(emptyList())

    override val viewIdResourceName: String?
        get() = try {
            realNode.viewIdResourceName
        } catch (_: Exception) {
            null
        }

    override val className: CharSequence?
        get() = try {
            realNode.className
        } catch (_: Exception) {
            null
        }

    override val text: CharSequence?
        get() = try {
            realNode.text
        } catch (_: Exception) {
            null
        }

    override val contentDescription: CharSequence?
        get() = try {
            realNode.contentDescription
        } catch (_: Exception) {
            null
        }

    override val packageName: CharSequence?
        get() = try {
            realNode.packageName
        } catch (_: Exception) {
            null
        }

    override val childCount: Int
        get() = try {
            realNode.childCount
        } catch (_: Exception) {
            0
        }

    override val isClickable: Boolean
        get() = try {
            realNode.isClickable
        } catch (_: Exception) {
            false
        }

    override val isPassword: Boolean get() = runCatching { realNode.isPassword }.getOrDefault(true)

    override val isEditable: Boolean
        get() = try {
            realNode.isEditable
        } catch (_: Exception) {
            true
        }

    override val parent: AccessibilityNode?
        get() = try {
            realNode.parent?.let { RealAccessibilityNode(it) }
        } catch (_: Exception) {
            null
        }

    override fun getChild(index: Int): AccessibilityNode? {
        return try {
            val child = realNode.getChild(index) ?: return null
            RealAccessibilityNode(child)
        } catch (_: Exception) {
            null
        }
    }

    override fun getBoundsInScreen(outBounds: Rect) {
        try {
            realNode.getBoundsInScreen(outBounds)
        } catch (_: Exception) {
            outBounds.set(0, 0, 0, 0)
        }
    }

    override fun getBoundsInScreenArray(): IntArray {
        val rect = Rect()
        return try {
            realNode.getBoundsInScreen(rect)
            intArrayOf(rect.left, rect.top, rect.right, rect.bottom)
        } catch (_: Exception) {
            intArrayOf(0, 0, 0, 0)
        }
    }

    override fun recycle() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            try {
                @Suppress("DEPRECATION")
                realNode.recycle()
            } catch (_: Exception) {
                // Safe ignore
            }
        }
    }
}

/**
 * Pure Kotlin in-memory node fixture for deterministic testing and synthetic tree construction.
 */
class SyntheticNode(
    override var viewIdResourceName: String? = null,
    override var className: CharSequence? = null,
    override var text: CharSequence? = null,
    override var contentDescription: CharSequence? = null,
    override var packageName: CharSequence? = null,
    override var isClickable: Boolean = false,
    override var isEditable: Boolean = false,
    override var isVisibleToUser: Boolean = true,
    override var isFocused: Boolean = false,
    var bounds: IntArray = intArrayOf(0, 0, 1080, 2400),
    override var isSelected: Boolean = false,
    override var isScrollable: Boolean = false,
    override var isLongClickable: Boolean = false
) : AccessibilityNode {

    override var parent: AccessibilityNode? = null
    val children = mutableListOf<SyntheticNode>()

    override val childCount: Int
        get() = children.size

    override fun getChild(index: Int): AccessibilityNode? {
        return if (index in children.indices) children[index] else null
    }

    override fun getBoundsInScreen(outBounds: Rect) {
        outBounds.set(bounds[0], bounds[1], bounds[2], bounds[3])
    }

    override fun getBoundsInScreenArray(): IntArray = bounds.copyOf()

    override fun recycle() {
        // No-op for synthetic JVM nodes
    }

    fun addChild(child: SyntheticNode): SyntheticNode {
        child.parent = this
        children.add(child)
        return this
    }
}

/**
 * Fluent builder DSL for synthetic accessibility hierarchies in tests.
 */
class SyntheticNodeBuilder(private val node: SyntheticNode) {

    fun viewId(id: String) { node.viewIdResourceName = id }
    fun className(cls: String) { node.className = cls }
    fun text(txt: String) { node.text = txt }
    fun contentDescription(desc: String) { node.contentDescription = desc }
    fun packageName(pkg: String) { node.packageName = pkg }
    fun clickable(clickable: Boolean) { node.isClickable = clickable }
    fun editable(editable: Boolean) { node.isEditable = editable }
    fun bounds(left: Int, top: Int, right: Int, bottom: Int) {
        node.bounds = intArrayOf(left, top, right, bottom)
    }

    fun node(
        viewId: String? = null,
        className: String? = null,
        text: String? = null,
        contentDescription: String? = null,
        packageName: String? = null,
        isClickable: Boolean = false,
        isEditable: Boolean = false,
        isVisibleToUser: Boolean = true,
        isFocused: Boolean = false,
        bounds: IntArray = intArrayOf(0, 0, 1080, 2400),
        init: (SyntheticNodeBuilder.() -> Unit)? = null
    ): SyntheticNode {
        val child = SyntheticNode(
            viewIdResourceName = viewId,
            className = className,
            text = text,
            contentDescription = contentDescription,
            packageName = packageName,
            isClickable = isClickable,
            isEditable = isEditable,
            isVisibleToUser = isVisibleToUser,
            isFocused = isFocused,
            bounds = bounds
        )
        if (init != null) {
            SyntheticNodeBuilder(child).init()
        }
        node.addChild(child)
        return child
    }
}

fun syntheticTree(
    packageName: String = "com.instagram.android",
    viewId: String? = null,
    className: String? = "android.widget.FrameLayout",
    bounds: IntArray = intArrayOf(0, 0, 1080, 2400),
    init: (SyntheticNodeBuilder.() -> Unit)? = null
): SyntheticNode {
    val root = SyntheticNode(
        packageName = packageName,
        viewIdResourceName = viewId,
        className = className,
        bounds = bounds
    )
    if (init != null) {
        SyntheticNodeBuilder(root).init()
    }
    return root
}
