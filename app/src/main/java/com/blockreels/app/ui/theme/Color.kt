package com.blockreels.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Emerald Focus Sanctuary — Material 3 Tonal Palette
val EmeraldPrimaryLight = Color(0xFF006B5E)
val OnEmeraldPrimaryLight = Color(0xFFFFFFFF)
val EmeraldContainerLight = Color(0xFF9DF2E2)
val OnEmeraldContainerLight = Color(0xFF00201B)

val SageSecondaryLight = Color(0xFF4A635E)
val OnSageSecondaryLight = Color(0xFFFFFFFF)
val SageContainerLight = Color(0xFFCDE8E1)
val OnSageContainerLight = Color(0xFF06201B)

val AmberTertiaryLight = Color(0xFF7D5700)
val OnAmberTertiaryLight = Color(0xFFFFFFFF)
val AmberContainerLight = Color(0xFFFFDEA6)
val OnAmberContainerLight = Color(0xFF271900)

val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)

val SurfaceLight = Color(0xFFF4FAF7)
val OnSurfaceLight = Color(0xFF161D1B)
val SurfaceVariantLight = Color(0xFFDAE5E1)
val OnSurfaceVariantLight = Color(0xFF3F4946)
val OutlineLight = Color(0xFF6F7976)
val OutlineVariantLight = Color(0xFFBEC9C5)
val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowLight = Color(0xFFEEF5F2)
val SurfaceContainerLight = Color(0xFFE8F0EC)
val SurfaceContainerHighLight = Color(0xFFE2EAE6)
val SurfaceContainerHighestLight = Color(0xFFDCE4E0)

// Dark Emerald Tonal Palette
val EmeraldPrimaryDark = Color(0xFF5CDAC5)
val OnEmeraldPrimaryDark = Color(0xFF003730)
val EmeraldContainerDark = Color(0xFF005046)
val OnEmeraldContainerDark = Color(0xFF9DF2E2)

val SageSecondaryDark = Color(0xFFB1CCC5)
val OnSageSecondaryDark = Color(0xFF1C3530)
val SageContainerDark = Color(0xFF334B46)
val OnSageContainerDark = Color(0xFFCDE8E1)

val AmberTertiaryDark = Color(0xFFF6BD52)
val OnAmberTertiaryDark = Color(0xFF422C00)
val AmberContainerDark = Color(0xFF5F4100)
val OnAmberContainerDark = Color(0xFFFFDEA6)

val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

val SurfaceDark = Color(0xFF0E1513)
val OnSurfaceDark = Color(0xFFDDE4E1)
val SurfaceVariantDark = Color(0xFF3F4946)
val OnSurfaceVariantDark = Color(0xFFBEC9C5)
val OutlineDark = Color(0xFF899390)
val OutlineVariantDark = Color(0xFF3F4946)
val SurfaceContainerLowestDark = Color(0xFF09100E)
val SurfaceContainerLowDark = Color(0xFF161D1B)
val SurfaceContainerDark = Color(0xFF1A2220)
val SurfaceContainerHighDark = Color(0xFF242C2A)
val SurfaceContainerHighestDark = Color(0xFF2F3735)

// Platform Brand Accent Tokens (subtle, harmonious indicators)
val BrandInstagram = Color(0xFFD93175)
val BrandYouTube = Color(0xFFD93025)
val BrandTikTok = Color(0xFF00A699)
val BrandFacebook = Color(0xFF1877F2)

val BlockReelsLightColorScheme = lightColorScheme(
    primary = EmeraldPrimaryLight,
    onPrimary = OnEmeraldPrimaryLight,
    primaryContainer = EmeraldContainerLight,
    onPrimaryContainer = OnEmeraldContainerLight,
    secondary = SageSecondaryLight,
    onSecondary = OnSageSecondaryLight,
    secondaryContainer = SageContainerLight,
    onSecondaryContainer = OnSageContainerLight,
    tertiary = AmberTertiaryLight,
    onTertiary = OnAmberTertiaryLight,
    tertiaryContainer = AmberContainerLight,
    onTertiaryContainer = OnAmberContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight
)

val BlockReelsDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = OnEmeraldPrimaryDark,
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = OnEmeraldContainerDark,
    secondary = SageSecondaryDark,
    onSecondary = OnSageSecondaryDark,
    secondaryContainer = SageContainerDark,
    onSecondaryContainer = OnSageContainerDark,
    tertiary = AmberTertiaryDark,
    onTertiary = OnAmberTertiaryDark,
    tertiaryContainer = AmberContainerDark,
    onTertiaryContainer = OnAmberContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark
)

val BlockReelsAmoledColorScheme = BlockReelsDarkColorScheme.copy(
    background = Color(0xFF000000),
    surface = Color(0xFF000000),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF0A0F0E),
    surfaceContainer = Color(0xFF101715),
    surfaceContainerHigh = Color(0xFF17201D),
    surfaceContainerHighest = Color(0xFF1F2A27)
)
