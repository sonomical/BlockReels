package com.blockreels.app.engine

import android.view.accessibility.AccessibilityWindowInfo
import com.blockreels.app.detection.AppPlatform

/** Select the foreground application; system windows, overlays and IMEs cannot own it. */
internal object ForegroundWindowSelection {
    data class Window(val id: Int, val type: Int, val focused: Boolean, val active: Boolean)

    fun select(windows: List<Window>): Int? =
        windows.firstOrNull {
            it.focused && it.type == AccessibilityWindowInfo.TYPE_APPLICATION
        }?.id ?: windows.firstOrNull {
            it.active && it.type == AccessibilityWindowInfo.TYPE_APPLICATION
        }?.id

    fun acceptsActiveRoot(selectedId: Int?, rootId: Int, windowType: Int?,
                          rootPackage: String?, expectedPackage: String?): Boolean {
        if (selectedId != null) return rootId == selectedId
        if (windowType != null) return windowType == AccessibilityWindowInfo.TYPE_APPLICATION
        return rootId >= 0 && rootPackage == expectedPackage &&
            AppPlatform.fromPackageName(rootPackage) != null
    }
}
