// SPDX-License-Identifier: MIT
package com.blockreels.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.blockreels.app.data.AppThemeMode

val LocalIsDarkTheme = compositionLocalOf { false }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val BlockReelsExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    largeIncreased = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
    extraLargeIncreased = RoundedCornerShape(32.dp),
    extraExtraLarge = RoundedCornerShape(48.dp)
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BlockReelsTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark: Boolean
    val colorScheme = when (themeMode) {
        AppThemeMode.LIGHT -> {
            isDark = false
            BlockReelsLightColorScheme
        }
        AppThemeMode.DARK -> {
            isDark = true
            BlockReelsDarkColorScheme
        }
        AppThemeMode.AMOLED -> {
            isDark = true
            BlockReelsAmoledColorScheme
        }
        AppThemeMode.SYSTEM -> {
            isDark = systemDark
            if (systemDark) BlockReelsDarkColorScheme else BlockReelsLightColorScheme
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !isDark
                    isAppearanceLightNavigationBars = !isDark
                }
            }
        }
    }

    CompositionLocalProvider(LocalIsDarkTheme provides isDark) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            shapes = BlockReelsExpressiveShapes,
            typography = BlockReelsTypography,
            content = content
        )
    }
}
