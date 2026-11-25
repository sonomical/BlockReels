// SPDX-License-Identifier: MIT
package com.blockreels.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.blockreels.app.data.AppColorSeed
import com.blockreels.app.data.AppContrastLevel
import com.blockreels.app.data.AppCornerStyle
import com.blockreels.app.data.AppFontScale
import com.blockreels.app.data.AppIconStyle
import com.blockreels.app.data.AppThemeMode

val LocalIsDarkTheme = compositionLocalOf { false }
val LocalAppIconStyle = compositionLocalOf { AppIconStyle.TONAL_COOKIE }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun shapesForCornerStyle(style: AppCornerStyle): Shapes = when (style) {
    AppCornerStyle.EXPRESSIVE -> Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(10.dp),
        medium = RoundedCornerShape(14.dp),
        large = RoundedCornerShape(18.dp),
        largeIncreased = RoundedCornerShape(22.dp),
        extraLarge = RoundedCornerShape(28.dp),
        extraLargeIncreased = RoundedCornerShape(34.dp),
        extraExtraLarge = RoundedCornerShape(48.dp)
    )
    AppCornerStyle.BALANCED -> Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(10.dp),
        large = RoundedCornerShape(14.dp),
        largeIncreased = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(20.dp),
        extraLargeIncreased = RoundedCornerShape(24.dp),
        extraExtraLarge = RoundedCornerShape(32.dp)
    )
    AppCornerStyle.COMPACT -> Shapes(
        extraSmall = RoundedCornerShape(2.dp),
        small = RoundedCornerShape(4.dp),
        medium = RoundedCornerShape(6.dp),
        large = RoundedCornerShape(8.dp),
        largeIncreased = RoundedCornerShape(10.dp),
        extraLarge = RoundedCornerShape(12.dp),
        extraLargeIncreased = RoundedCornerShape(14.dp),
        extraExtraLarge = RoundedCornerShape(20.dp)
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BlockReelsTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    dynamicColorEnabled: Boolean = false,
    colorSeed: AppColorSeed = AppColorSeed.EMERALD,
    contrastLevel: AppContrastLevel = AppContrastLevel.STANDARD,
    cornerStyle: AppCornerStyle = AppCornerStyle.EXPRESSIVE,
    fontScale: AppFontScale = AppFontScale.DEFAULT,
    iconStyle: AppIconStyle = AppIconStyle.TONAL_COOKIE,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK, AppThemeMode.AMOLED -> true
        AppThemeMode.SYSTEM -> systemDark
    }
    val amoled = themeMode == AppThemeMode.AMOLED

    val colorScheme = if (dynamicColorEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val dynamicBase = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        val withAmoled = if (isDark && amoled) applyAmoledSurfaces(dynamicBase) else dynamicBase
        applyContrastBoost(withAmoled, contrastLevel, isDark)
    } else {
        buildSeedColorScheme(
            seed = colorSeed,
            isDark = isDark,
            amoled = amoled,
            contrast = contrastLevel
        )
    }

    val shapes = remember(cornerStyle) { shapesForCornerStyle(cornerStyle) }
    val typography = remember(fontScale) { buildBlockReelsTypography(fontScale) }

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

    CompositionLocalProvider(
        LocalIsDarkTheme provides isDark,
        LocalAppIconStyle provides iconStyle
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            shapes = shapes,
            typography = typography,
            content = content
        )
    }
}
