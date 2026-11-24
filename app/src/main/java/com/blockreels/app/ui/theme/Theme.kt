package com.blockreels.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.blockreels.app.data.AppThemeMode

val BlockReelsShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun BlockReelsTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDarkIcons: Boolean
    val colorScheme = when (themeMode) {
        AppThemeMode.LIGHT -> {
            useDarkIcons = true
            BlockReelsLightColorScheme
        }
        AppThemeMode.DARK -> {
            useDarkIcons = false
            BlockReelsDarkColorScheme
        }
        AppThemeMode.AMOLED -> {
            useDarkIcons = false
            BlockReelsAmoledColorScheme
        }
        AppThemeMode.SYSTEM -> {
            useDarkIcons = !systemDark
            if (systemDark) BlockReelsDarkColorScheme else BlockReelsLightColorScheme
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = useDarkIcons
                    isAppearanceLightNavigationBars = useDarkIcons
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = BlockReelsTypography,
        shapes = BlockReelsShapes,
        content = content
    )
}
