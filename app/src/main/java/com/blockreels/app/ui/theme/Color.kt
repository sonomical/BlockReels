// SPDX-License-Identifier: MIT
package com.blockreels.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// =============================================================================
// Emerald Focus Sanctuary — Strict Material 3 Expressive Tonal Color System
// Zero translucent/glass surfaces; every container and content role is opaque.
// =============================================================================

// Light Tonal Palette
val EmeraldPrimaryLight = Color(0xFF006B5E)
val OnEmeraldPrimaryLight = Color(0xFFFFFFFF)
val EmeraldContainerLight = Color(0xFF9AF2E1)
val OnEmeraldContainerLight = Color(0xFF00201B)
val EmeraldInversePrimaryLight = Color(0xFF5EDBC6)

val SageSecondaryLight = Color(0xFF4A635E)
val OnSageSecondaryLight = Color(0xFFFFFFFF)
val SageContainerLight = Color(0xFFCDE8E1)
val OnSageContainerLight = Color(0xFF06201B)

val AmberTertiaryLight = Color(0xFF7B5800)
val OnAmberTertiaryLight = Color(0xFFFFFFFF)
val AmberContainerLight = Color(0xFFFFDEA4)
val OnAmberContainerLight = Color(0xFF261900)

val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)

val SurfaceLight = Color(0xFFF4FBF8)
val SurfaceBrightLight = Color(0xFFF4FBF8)
val SurfaceDimLight = Color(0xFFD5DCD9)
val OnSurfaceLight = Color(0xFF161D1B)
val SurfaceVariantLight = Color(0xFFDAE5E1)
val OnSurfaceVariantLight = Color(0xFF3F4946)
val OutlineLight = Color(0xFF6F7976)
val OutlineVariantLight = Color(0xFFBEC9C5)
val InverseSurfaceLight = Color(0xFF2B3230)
val InverseOnSurfaceLight = Color(0xFFECF2EF)
val ScrimColor = Color(0xFF000000)

val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowLight = Color(0xFFEEF5F2)
val SurfaceContainerLight = Color(0xFFE8EFEC)
val SurfaceContainerHighLight = Color(0xFFE2E9E6)
val SurfaceContainerHighestLight = Color(0xFFDCE4E0)

// Fixed Accent Roles (invariant across Light & Dark per M3 spec)
val PrimaryFixed = Color(0xFF9AF2E1)
val PrimaryFixedDim = Color(0xFF5EDBC6)
val OnPrimaryFixed = Color(0xFF00201B)
val OnPrimaryFixedVariant = Color(0xFF005046)

val SecondaryFixed = Color(0xFFCDE8E1)
val SecondaryFixedDim = Color(0xFFB1CCC5)
val OnSecondaryFixed = Color(0xFF06201B)
val OnSecondaryFixedVariant = Color(0xFF334B46)

val TertiaryFixed = Color(0xFFFFDEA4)
val TertiaryFixedDim = Color(0xFFF6BD52)
val OnTertiaryFixed = Color(0xFF261900)
val OnTertiaryFixedVariant = Color(0xFF5D4200)

// Dark Tonal Palette
val EmeraldPrimaryDark = Color(0xFF5EDBC6)
val OnEmeraldPrimaryDark = Color(0xFF003730)
val EmeraldContainerDark = Color(0xFF005046)
val OnEmeraldContainerDark = Color(0xFF9AF2E1)
val EmeraldInversePrimaryDark = Color(0xFF006B5E)

val SageSecondaryDark = Color(0xFFB1CCC5)
val OnSageSecondaryDark = Color(0xFF1C3530)
val SageContainerDark = Color(0xFF334B46)
val OnSageContainerDark = Color(0xFFCDE8E1)

val AmberTertiaryDark = Color(0xFFF6BD52)
val OnAmberTertiaryDark = Color(0xFF412D00)
val AmberContainerDark = Color(0xFF5D4200)
val OnAmberContainerDark = Color(0xFFFFDEA4)

val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

val SurfaceDark = Color(0xFF0E1513)
val SurfaceBrightDark = Color(0xFF343B39)
val SurfaceDimDark = Color(0xFF0E1513)
val OnSurfaceDark = Color(0xFFDDE4E1)
val SurfaceVariantDark = Color(0xFF3F4946)
val OnSurfaceVariantDark = Color(0xFFBEC9C5)
val OutlineDark = Color(0xFF899390)
val OutlineVariantDark = Color(0xFF3F4946)
val InverseSurfaceDark = Color(0xFFDDE4E1)
val InverseOnSurfaceDark = Color(0xFF2B3230)

val SurfaceContainerLowestDark = Color(0xFF09100E)
val SurfaceContainerLowDark = Color(0xFF161D1B)
val SurfaceContainerDark = Color(0xFF1A2220)
val SurfaceContainerHighDark = Color(0xFF242C2A)
val SurfaceContainerHighestDark = Color(0xFF2F3735)

// Opaque Harmonized Platform Tonal Pairs (Light & Dark — zero alpha blending)
data class PlatformTonalColors(
    val container: Color,
    val content: Color
)

private val InstagramLight = PlatformTonalColors(Color(0xFFFFD8E4), Color(0xFF630B32))
private val InstagramDark = PlatformTonalColors(Color(0xFF5C1132), Color(0xFFFFD8E4))

private val YouTubeLight = PlatformTonalColors(Color(0xFFFFDAD6), Color(0xFF690005))
private val YouTubeDark = PlatformTonalColors(Color(0xFF69120D), Color(0xFFFFDAD6))

private val TikTokLight = PlatformTonalColors(Color(0xFFC2F0EB), Color(0xFF003733))
private val TikTokDark = PlatformTonalColors(Color(0xFF004F49), Color(0xFFC2F0EB))

private val FacebookLight = PlatformTonalColors(Color(0xFFD6E3FF), Color(0xFF002F6D))
private val FacebookDark = PlatformTonalColors(Color(0xFF0A3A7E), Color(0xFFD6E3FF))

private val SnapchatLight = PlatformTonalColors(Color(0xFFFFE5A0), Color(0xFF422E00))
private val SnapchatDark = PlatformTonalColors(Color(0xFF563D00), Color(0xFFFFE5A0))

@Composable
fun platformTonalColors(platformKey: String, isDark: Boolean): PlatformTonalColors {
    return when (platformKey.uppercase()) {
        "INSTAGRAM" -> if (isDark) InstagramDark else InstagramLight
        "YOUTUBE" -> if (isDark) YouTubeDark else YouTubeLight
        "TIKTOK" -> if (isDark) TikTokDark else TikTokLight
        "FACEBOOK" -> if (isDark) FacebookDark else FacebookLight
        "SNAPCHAT" -> if (isDark) SnapchatDark else SnapchatLight
        else -> if (isDark) {
            PlatformTonalColors(EmeraldContainerDark, OnEmeraldContainerDark)
        } else {
            PlatformTonalColors(EmeraldContainerLight, OnEmeraldContainerLight)
        }
    }
}

val BlockReelsLightColorScheme = lightColorScheme(
    primary = EmeraldPrimaryLight,
    onPrimary = OnEmeraldPrimaryLight,
    primaryContainer = EmeraldContainerLight,
    onPrimaryContainer = OnEmeraldContainerLight,
    inversePrimary = EmeraldInversePrimaryLight,
    secondary = SageSecondaryLight,
    onSecondary = OnSageSecondaryLight,
    secondaryContainer = SageContainerLight,
    onSecondaryContainer = OnSageContainerLight,
    tertiary = AmberTertiaryLight,
    onTertiary = OnAmberTertiaryLight,
    tertiaryContainer = AmberContainerLight,
    onTertiaryContainer = OnAmberContainerLight,
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceTint = EmeraldPrimaryLight,
    inverseSurface = InverseSurfaceLight,
    inverseOnSurface = InverseOnSurfaceLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    scrim = ScrimColor,
    surfaceBright = SurfaceBrightLight,
    surfaceDim = SurfaceDimLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
    primaryFixed = PrimaryFixed,
    primaryFixedDim = PrimaryFixedDim,
    onPrimaryFixed = OnPrimaryFixed,
    onPrimaryFixedVariant = OnPrimaryFixedVariant,
    secondaryFixed = SecondaryFixed,
    secondaryFixedDim = SecondaryFixedDim,
    onSecondaryFixed = OnSecondaryFixed,
    onSecondaryFixedVariant = OnSecondaryFixedVariant,
    tertiaryFixed = TertiaryFixed,
    tertiaryFixedDim = TertiaryFixedDim,
    onTertiaryFixed = OnTertiaryFixed,
    onTertiaryFixedVariant = OnTertiaryFixedVariant
)

val BlockReelsDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = OnEmeraldPrimaryDark,
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = OnEmeraldContainerDark,
    inversePrimary = EmeraldInversePrimaryDark,
    secondary = SageSecondaryDark,
    onSecondary = OnSageSecondaryDark,
    secondaryContainer = SageContainerDark,
    onSecondaryContainer = OnSageContainerDark,
    tertiary = AmberTertiaryDark,
    onTertiary = OnAmberTertiaryDark,
    tertiaryContainer = AmberContainerDark,
    onTertiaryContainer = OnAmberContainerDark,
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceTint = EmeraldPrimaryDark,
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    scrim = ScrimColor,
    surfaceBright = SurfaceBrightDark,
    surfaceDim = SurfaceDimDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    primaryFixed = PrimaryFixed,
    primaryFixedDim = PrimaryFixedDim,
    onPrimaryFixed = OnPrimaryFixed,
    onPrimaryFixedVariant = OnPrimaryFixedVariant,
    secondaryFixed = SecondaryFixed,
    secondaryFixedDim = SecondaryFixedDim,
    onSecondaryFixed = OnSecondaryFixed,
    onSecondaryFixedVariant = OnSecondaryFixedVariant,
    tertiaryFixed = TertiaryFixed,
    tertiaryFixedDim = TertiaryFixedDim,
    onTertiaryFixed = OnTertiaryFixed,
    onTertiaryFixedVariant = OnTertiaryFixedVariant
)

val BlockReelsAmoledColorScheme = BlockReelsDarkColorScheme.copy(
    background = Color(0xFF000000),
    surface = Color(0xFF000000),
    surfaceDim = Color(0xFF000000),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF0E1412),
    surfaceContainer = Color(0xFF141C19),
    surfaceContainerHigh = Color(0xFF1D2623),
    surfaceContainerHighest = Color(0xFF27312E)
)
