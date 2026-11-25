// SPDX-License-Identifier: MIT
package com.blockreels.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.blockreels.app.data.AppColorSeed
import com.blockreels.app.data.AppContrastLevel

// =============================================================================
// Strict Material 3 Expressive Opaque Tonal Palettes
// Zero glassmorphism or translucent fills; every container role is solid opaque.
// =============================================================================

private val ScrimColor = Color(0xFF000000)

private val ErrorLight = Color(0xFFBA1A1A)
private val OnErrorLight = Color(0xFFFFFFFF)
private val ErrorContainerLight = Color(0xFFFFDAD6)
private val OnErrorContainerLight = Color(0xFF410002)

private val ErrorDark = Color(0xFFFFB4AB)
private val OnErrorDark = Color(0xFF690005)
private val ErrorContainerDark = Color(0xFF93000A)
private val OnErrorContainerDark = Color(0xFFFFDAD6)

private data class PaletteSpec(
    // Light
    val primaryL: Color,
    val onPrimaryL: Color,
    val primaryContainerL: Color,
    val onPrimaryContainerL: Color,
    val secondaryL: Color,
    val onSecondaryL: Color,
    val secondaryContainerL: Color,
    val onSecondaryContainerL: Color,
    val tertiaryL: Color,
    val onTertiaryL: Color,
    val tertiaryContainerL: Color,
    val onTertiaryContainerL: Color,
    val surfaceL: Color,
    val surfaceDimL: Color,
    val surfaceLowestL: Color,
    val surfaceLowL: Color,
    val surfaceContainerL: Color,
    val surfaceHighL: Color,
    val surfaceHighestL: Color,
    val onSurfaceL: Color,
    val onSurfaceVariantL: Color,
    val outlineL: Color,
    val outlineVariantL: Color,
    // Dark
    val primaryD: Color,
    val onPrimaryD: Color,
    val primaryContainerD: Color,
    val onPrimaryContainerD: Color,
    val secondaryD: Color,
    val onSecondaryD: Color,
    val secondaryContainerD: Color,
    val onSecondaryContainerD: Color,
    val tertiaryD: Color,
    val onTertiaryD: Color,
    val tertiaryContainerD: Color,
    val onTertiaryContainerD: Color,
    val surfaceD: Color,
    val surfaceBrightD: Color,
    val surfaceLowestD: Color,
    val surfaceLowD: Color,
    val surfaceContainerD: Color,
    val surfaceHighD: Color,
    val surfaceHighestD: Color,
    val onSurfaceD: Color,
    val onSurfaceVariantD: Color,
    val outlineD: Color,
    val outlineVariantD: Color
)

private val EmeraldSpec = PaletteSpec(
    primaryL = Color(0xFF006B5E), onPrimaryL = Color(0xFFFFFFFF),
    primaryContainerL = Color(0xFF9AF2E1), onPrimaryContainerL = Color(0xFF00201B),
    secondaryL = Color(0xFF4A635E), onSecondaryL = Color(0xFFFFFFFF),
    secondaryContainerL = Color(0xFFCDE8E1), onSecondaryContainerL = Color(0xFF06201B),
    tertiaryL = Color(0xFF7B5800), onTertiaryL = Color(0xFFFFFFFF),
    tertiaryContainerL = Color(0xFFFFDEA4), onTertiaryContainerL = Color(0xFF261900),
    surfaceL = Color(0xFFF4FBF8), surfaceDimL = Color(0xFFD5DCD9),
    surfaceLowestL = Color(0xFFFFFFFF), surfaceLowL = Color(0xFFEEF5F2),
    surfaceContainerL = Color(0xFFE8EFEC), surfaceHighL = Color(0xFFE2E9E6), surfaceHighestL = Color(0xFFDCE4E0),
    onSurfaceL = Color(0xFF161D1B), onSurfaceVariantL = Color(0xFF3F4946),
    outlineL = Color(0xFF6F7976), outlineVariantL = Color(0xFFBEC9C5),
    primaryD = Color(0xFF5EDBC6), onPrimaryD = Color(0xFF003730),
    primaryContainerD = Color(0xFF005046), onPrimaryContainerD = Color(0xFF9AF2E1),
    secondaryD = Color(0xFFB1CCC5), onSecondaryD = Color(0xFF1C3530),
    secondaryContainerD = Color(0xFF334B46), onSecondaryContainerD = Color(0xFFCDE8E1),
    tertiaryD = Color(0xFFF6BD52), onTertiaryD = Color(0xFF412D00),
    tertiaryContainerD = Color(0xFF5D4200), onTertiaryContainerD = Color(0xFFFFDEA4),
    surfaceD = Color(0xFF0E1513), surfaceBrightD = Color(0xFF343B39),
    surfaceLowestD = Color(0xFF09100E), surfaceLowD = Color(0xFF161D1B),
    surfaceContainerD = Color(0xFF1A2220), surfaceHighD = Color(0xFF242C2A), surfaceHighestD = Color(0xFF2F3735),
    onSurfaceD = Color(0xFFDDE4E1), onSurfaceVariantD = Color(0xFFBEC9C5),
    outlineD = Color(0xFF899390), outlineVariantD = Color(0xFF3F4946)
)

private val OceanSpec = PaletteSpec(
    primaryL = Color(0xFF005AC1), onPrimaryL = Color(0xFFFFFFFF),
    primaryContainerL = Color(0xFFD8E2FF), onPrimaryContainerL = Color(0xFF001A41),
    secondaryL = Color(0xFF535E78), onSecondaryL = Color(0xFFFFFFFF),
    secondaryContainerL = Color(0xFFD8E2FF), onSecondaryContainerL = Color(0xFF101B32),
    tertiaryL = Color(0xFF006874), onTertiaryL = Color(0xFFFFFFFF),
    tertiaryContainerL = Color(0xFF97F0FF), onTertiaryContainerL = Color(0xFF001F24),
    surfaceL = Color(0xFFF9F9FF), surfaceDimL = Color(0xFFD8D9E2),
    surfaceLowestL = Color(0xFFFFFFFF), surfaceLowL = Color(0xFFF2F3FC),
    surfaceContainerL = Color(0xFFECEDF6), surfaceHighL = Color(0xFFE6E7F0), surfaceHighestL = Color(0xFFE0E2EB),
    onSurfaceL = Color(0xFF191B23), onSurfaceVariantL = Color(0xFF444752),
    outlineL = Color(0xFF747784), outlineVariantL = Color(0xFFC4C6D4),
    primaryD = Color(0xFFADC6FF), onPrimaryD = Color(0xFF002E69),
    primaryContainerD = Color(0xFF004494), onPrimaryContainerD = Color(0xFFD8E2FF),
    secondaryD = Color(0xFFBBC6E4), onSecondaryD = Color(0xFF253048),
    secondaryContainerD = Color(0xFF3C475F), onSecondaryContainerD = Color(0xFFD8E2FF),
    tertiaryD = Color(0xFF4FD8EB), onTertiaryD = Color(0xFF00363D),
    tertiaryContainerD = Color(0xFF004F58), onTertiaryContainerD = Color(0xFF97F0FF),
    surfaceD = Color(0xFF11131B), surfaceBrightD = Color(0xFF373942),
    surfaceLowestD = Color(0xFF0C0E15), surfaceLowD = Color(0xFF191B23),
    surfaceContainerD = Color(0xFF1D2027), surfaceHighD = Color(0xFF282A32), surfaceHighestD = Color(0xFF32353D),
    onSurfaceD = Color(0xFFE1E2EC), onSurfaceVariantD = Color(0xFFC4C6D4),
    outlineD = Color(0xFF8E909E), outlineVariantD = Color(0xFF444752)
)

private val TerracottaSpec = PaletteSpec(
    primaryL = Color(0xFF9C4327), onPrimaryL = Color(0xFFFFFFFF),
    primaryContainerL = Color(0xFFFFDBD0), onPrimaryContainerL = Color(0xFF390C00),
    secondaryL = Color(0xFF77574D), onSecondaryL = Color(0xFFFFFFFF),
    secondaryContainerL = Color(0xFFFFDBD0), onSecondaryContainerL = Color(0xFF2C150E),
    tertiaryL = Color(0xFF6B5E2F), onTertiaryL = Color(0xFFFFFFFF),
    tertiaryContainerL = Color(0xFFF5E2A7), onTertiaryContainerL = Color(0xFF231B00),
    surfaceL = Color(0xFFFFF8F6), surfaceDimL = Color(0xFFE7D7D2),
    surfaceLowestL = Color(0xFFFFFFFF), surfaceLowL = Color(0xFFFFF1EC),
    surfaceContainerL = Color(0xFFFCEAE4), surfaceHighL = Color(0xFFF6E4DF), surfaceHighestL = Color(0xFFF0DFD9),
    onSurfaceL = Color(0xFF231917), onSurfaceVariantL = Color(0xFF53433E),
    outlineL = Color(0xFF85736E), outlineVariantL = Color(0xFFD8C2BC),
    primaryD = Color(0xFFFFB59D), onPrimaryD = Color(0xFF5D1802),
    primaryContainerD = Color(0xFF7E2C12), onPrimaryContainerD = Color(0xFFFFDBD0),
    secondaryD = Color(0xFFE7BDB0), onSecondaryD = Color(0xFF442A21),
    secondaryContainerD = Color(0xFF5D4037), onSecondaryContainerD = Color(0xFFFFDBD0),
    tertiaryD = Color(0xFFD8C68D), onTertiaryD = Color(0xFF3A3005),
    tertiaryContainerD = Color(0xFF52461A), onTertiaryContainerD = Color(0xFFF5E2A7),
    surfaceD = Color(0xFF1A110F), surfaceBrightD = Color(0xFF423734),
    surfaceLowestD = Color(0xFF140C0A), surfaceLowD = Color(0xFF231917),
    surfaceContainerD = Color(0xFF271D1B), surfaceHighD = Color(0xFF322825), surfaceHighestD = Color(0xFF3D322F),
    onSurfaceD = Color(0xFFF0DFD9), onSurfaceVariantD = Color(0xFFD8C2BC),
    outlineD = Color(0xFFA08D87), outlineVariantD = Color(0xFF53433E)
)

private val AmethystSpec = PaletteSpec(
    primaryL = Color(0xFF6B4FA2), onPrimaryL = Color(0xFFFFFFFF),
    primaryContainerL = Color(0xFFEBDDFF), onPrimaryContainerL = Color(0xFF250059),
    secondaryL = Color(0xFF635B70), onSecondaryL = Color(0xFFFFFFFF),
    secondaryContainerL = Color(0xFFE9DEF8), onSecondaryContainerL = Color(0xFF1F182B),
    tertiaryL = Color(0xFF7E5260), onTertiaryL = Color(0xFFFFFFFF),
    tertiaryContainerL = Color(0xFFFFD9E3), onTertiaryContainerL = Color(0xFF31101D),
    surfaceL = Color(0xFFFEF7FF), surfaceDimL = Color(0xFFDFD8E3),
    surfaceLowestL = Color(0xFFFFFFFF), surfaceLowL = Color(0xFFF8F1FA),
    surfaceContainerL = Color(0xFFF2EBF4), surfaceHighL = Color(0xFFECE6EF), surfaceHighestL = Color(0xFFE7E0E9),
    onSurfaceL = Color(0xFF1D1B20), onSurfaceVariantL = Color(0xFF49454E),
    outlineL = Color(0xFF7A757F), outlineVariantL = Color(0xFFCBC4CF),
    primaryD = Color(0xFFD3BCFD), onPrimaryD = Color(0xFF3B1F70),
    primaryContainerD = Color(0xFF523788), onPrimaryContainerD = Color(0xFFEBDDFF),
    secondaryD = Color(0xFFCDC2DB), onSecondaryD = Color(0xFF342D40),
    secondaryContainerD = Color(0xFF4B4358), onSecondaryContainerD = Color(0xFFE9DEF8),
    tertiaryD = Color(0xFFF0B7C8), onTertiaryD = Color(0xFF4A2532),
    tertiaryContainerD = Color(0xFF643B48), onTertiaryContainerD = Color(0xFFFFD9E3),
    surfaceD = Color(0xFF151218), surfaceBrightD = Color(0xFF3B383E),
    surfaceLowestD = Color(0xFF0F0D13), surfaceLowD = Color(0xFF1D1B20),
    surfaceContainerD = Color(0xFF211F24), surfaceHighD = Color(0xFF2C292F), surfaceHighestD = Color(0xFF37343A),
    onSurfaceD = Color(0xFFE7E0E9), onSurfaceVariantD = Color(0xFFCBC4CF),
    outlineD = Color(0xFF948F99), outlineVariantD = Color(0xFF49454E)
)

private val ForestSpec = PaletteSpec(
    primaryL = Color(0xFF386A20), onPrimaryL = Color(0xFFFFFFFF),
    primaryContainerL = Color(0xFFB8F397), onPrimaryContainerL = Color(0xFF072100),
    secondaryL = Color(0xFF55624C), onSecondaryL = Color(0xFFFFFFFF),
    secondaryContainerL = Color(0xFFD9E7CB), onSecondaryContainerL = Color(0xFF131F0D),
    tertiaryL = Color(0xFF386667), onTertiaryL = Color(0xFFFFFFFF),
    tertiaryContainerL = Color(0xFFBBEBEC), onTertiaryContainerL = Color(0xFF002021),
    surfaceL = Color(0xFFF8FAF0), surfaceDimL = Color(0xFFD8DBD1),
    surfaceLowestL = Color(0xFFFFFFFF), surfaceLowL = Color(0xFFF2F5EA),
    surfaceContainerL = Color(0xFFECEFE4), surfaceHighL = Color(0xFFE6E9DF), surfaceHighestL = Color(0xFFE1E4D9),
    onSurfaceL = Color(0xFF191D16), onSurfaceVariantL = Color(0xFF43483E),
    outlineL = Color(0xFF74796D), outlineVariantL = Color(0xFFC3C8BB),
    primaryD = Color(0xFF9CD67D), onPrimaryD = Color(0xFF123800),
    primaryContainerD = Color(0xFF205107), onPrimaryContainerD = Color(0xFFB8F397),
    secondaryD = Color(0xFFBDCBB0), onSecondaryD = Color(0xFF283420),
    secondaryContainerD = Color(0xFF3E4A36), onSecondaryContainerD = Color(0xFFD9E7CB),
    tertiaryD = Color(0xFFA0CFD0), onTertiaryD = Color(0xFF003738),
    tertiaryContainerD = Color(0xFF1E4E4F), onTertiaryContainerD = Color(0xFFBBEBEC),
    surfaceD = Color(0xFF11140E), surfaceBrightD = Color(0xFF373A33),
    surfaceLowestD = Color(0xFF0C0F09), surfaceLowD = Color(0xFF191D16),
    surfaceContainerD = Color(0xFF1D211A), surfaceHighD = Color(0xFF282B24), surfaceHighestD = Color(0xFF32362F),
    onSurfaceD = Color(0xFFE1E4D9), onSurfaceVariantD = Color(0xFFC3C8BB),
    outlineD = Color(0xFF8D9286), outlineVariantD = Color(0xFF43483E)
)

private val GraphiteSpec = PaletteSpec(
    primaryL = Color(0xFF3E5066), onPrimaryL = Color(0xFFFFFFFF),
    primaryContainerL = Color(0xFFD4E3F8), onPrimaryContainerL = Color(0xFF0A1B2E),
    secondaryL = Color(0xFF565E69), onSecondaryL = Color(0xFFFFFFFF),
    secondaryContainerL = Color(0xFFDAE2EE), onSecondaryContainerL = Color(0xFF141C25),
    tertiaryL = Color(0xFF66586C), onTertiaryL = Color(0xFFFFFFFF),
    tertiaryContainerL = Color(0xFFEDDDF3), onTertiaryContainerL = Color(0xFF211627),
    surfaceL = Color(0xFFF9F9FB), surfaceDimL = Color(0xFFD9DADF),
    surfaceLowestL = Color(0xFFFFFFFF), surfaceLowL = Color(0xFFF3F4F7),
    surfaceContainerL = Color(0xFFEDEFF2), surfaceHighL = Color(0xFFE7E9EC), surfaceHighestL = Color(0xFFE2E4E7),
    onSurfaceL = Color(0xFF191C1F), onSurfaceVariantL = Color(0xFF44474C),
    outlineL = Color(0xFF74787D), outlineVariantL = Color(0xFFC4C7CD),
    primaryD = Color(0xFFB3C7E0), onPrimaryD = Color(0xFF1C2D40),
    primaryContainerD = Color(0xFF2C3E53), onPrimaryContainerD = Color(0xFFD4E3F8),
    secondaryD = Color(0xFFBEC6D2), onSecondaryD = Color(0xFF28303A),
    secondaryContainerD = Color(0xFF3E4751), onSecondaryContainerD = Color(0xFFDAE2EE),
    tertiaryD = Color(0xFFD1C1D7), onTertiaryD = Color(0xFF372B3D),
    tertiaryContainerD = Color(0xFF4E4154), onTertiaryContainerD = Color(0xFFEDDDF3),
    surfaceD = Color(0xFF111316), surfaceBrightD = Color(0xFF373A3E),
    surfaceLowestD = Color(0xFF0C0E10), surfaceLowD = Color(0xFF191C1F),
    surfaceContainerD = Color(0xFF1E2024), surfaceHighD = Color(0xFF282B2E), surfaceHighestD = Color(0xFF33363A),
    onSurfaceD = Color(0xFFE2E4E7), onSurfaceVariantD = Color(0xFFC4C7CD),
    outlineD = Color(0xFF8E9197), outlineVariantD = Color(0xFF44474C)
)

private fun specFor(seed: AppColorSeed): PaletteSpec = when (seed) {
    AppColorSeed.EMERALD -> EmeraldSpec
    AppColorSeed.OCEAN -> OceanSpec
    AppColorSeed.TERRACOTTA -> TerracottaSpec
    AppColorSeed.AMETHYST -> AmethystSpec
    AppColorSeed.FOREST -> ForestSpec
    AppColorSeed.GRAPHITE -> GraphiteSpec
}

fun buildSeedColorScheme(
    seed: AppColorSeed,
    isDark: Boolean,
    amoled: Boolean,
    contrast: AppContrastLevel
): ColorScheme {
    val s = specFor(seed)
    val base = if (!isDark) {
        lightColorScheme(
            primary = s.primaryL,
            onPrimary = s.onPrimaryL,
            primaryContainer = s.primaryContainerL,
            onPrimaryContainer = s.onPrimaryContainerL,
            inversePrimary = s.primaryD,
            secondary = s.secondaryL,
            onSecondary = s.onSecondaryL,
            secondaryContainer = s.secondaryContainerL,
            onSecondaryContainer = s.onSecondaryContainerL,
            tertiary = s.tertiaryL,
            onTertiary = s.onTertiaryL,
            tertiaryContainer = s.tertiaryContainerL,
            onTertiaryContainer = s.onTertiaryContainerL,
            background = s.surfaceL,
            onBackground = s.onSurfaceL,
            surface = s.surfaceL,
            onSurface = s.onSurfaceL,
            surfaceVariant = s.surfaceHighestL,
            onSurfaceVariant = s.onSurfaceVariantL,
            surfaceTint = s.primaryL,
            inverseSurface = s.surfaceHighD,
            inverseOnSurface = s.onSurfaceD,
            error = ErrorLight,
            onError = OnErrorLight,
            errorContainer = ErrorContainerLight,
            onErrorContainer = OnErrorContainerLight,
            outline = s.outlineL,
            outlineVariant = s.outlineVariantL,
            scrim = ScrimColor,
            surfaceBright = s.surfaceL,
            surfaceDim = s.surfaceDimL,
            surfaceContainerLowest = s.surfaceLowestL,
            surfaceContainerLow = s.surfaceLowL,
            surfaceContainer = s.surfaceContainerL,
            surfaceContainerHigh = s.surfaceHighL,
            surfaceContainerHighest = s.surfaceHighestL
        )
    } else {
        darkColorScheme(
            primary = s.primaryD,
            onPrimary = s.onPrimaryD,
            primaryContainer = s.primaryContainerD,
            onPrimaryContainer = s.onPrimaryContainerD,
            inversePrimary = s.primaryL,
            secondary = s.secondaryD,
            onSecondary = s.onSecondaryD,
            secondaryContainer = s.secondaryContainerD,
            onSecondaryContainer = s.onSecondaryContainerD,
            tertiary = s.tertiaryD,
            onTertiary = s.onTertiaryD,
            tertiaryContainer = s.tertiaryContainerD,
            onTertiaryContainer = s.onTertiaryContainerD,
            background = s.surfaceD,
            onBackground = s.onSurfaceD,
            surface = s.surfaceD,
            onSurface = s.onSurfaceD,
            surfaceVariant = s.surfaceHighestD,
            onSurfaceVariant = s.onSurfaceVariantD,
            surfaceTint = s.primaryD,
            inverseSurface = s.onSurfaceD,
            inverseOnSurface = s.surfaceHighD,
            error = ErrorDark,
            onError = OnErrorDark,
            errorContainer = ErrorContainerDark,
            onErrorContainer = OnErrorContainerDark,
            outline = s.outlineD,
            outlineVariant = s.outlineVariantD,
            scrim = ScrimColor,
            surfaceBright = s.surfaceBrightD,
            surfaceDim = s.surfaceD,
            surfaceContainerLowest = s.surfaceLowestD,
            surfaceContainerLow = s.surfaceLowD,
            surfaceContainer = s.surfaceContainerD,
            surfaceContainerHigh = s.surfaceHighD,
            surfaceContainerHighest = s.surfaceHighestD
        )
    }

    val withAmoled = if (isDark && amoled) applyAmoledSurfaces(base) else base
    return applyContrastBoost(withAmoled, contrast, isDark)
}

fun applyAmoledSurfaces(scheme: ColorScheme): ColorScheme = scheme.copy(
    background = Color(0xFF000000),
    surface = Color(0xFF000000),
    surfaceDim = Color(0xFF000000),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF0D1010),
    surfaceContainer = Color(0xFF141818),
    surfaceContainerHigh = Color(0xFF1D2222),
    surfaceContainerHighest = Color(0xFF272D2D)
)

fun applyContrastBoost(
    scheme: ColorScheme,
    contrast: AppContrastLevel,
    isDark: Boolean
): ColorScheme = when (contrast) {
    AppContrastLevel.STANDARD -> scheme
    AppContrastLevel.MEDIUM -> if (isDark) {
        scheme.copy(
            onSurface = Color(0xFFF3F6F5),
            onSurfaceVariant = Color(0xFFD6DDD9),
            outline = Color(0xFFA7B0AD)
        )
    } else {
        scheme.copy(
            onSurface = Color(0xFF0B1110),
            onSurfaceVariant = Color(0xFF2A3230),
            outline = Color(0xFF535D5A)
        )
    }
    AppContrastLevel.HIGH -> if (isDark) {
        scheme.copy(
            onSurface = Color(0xFFFFFFFF),
            onSurfaceVariant = Color(0xFFEAEEEC),
            outline = Color(0xFFC8D0CD),
            surfaceContainerLow = Color(0xFF1B201F),
            surfaceContainerHigh = Color(0xFF2C3331)
        )
    } else {
        scheme.copy(
            surface = Color(0xFFFFFFFF),
            background = Color(0xFFFFFFFF),
            onSurface = Color(0xFF000000),
            onSurfaceVariant = Color(0xFF1B2120),
            outline = Color(0xFF394240),
            surfaceContainerLow = Color(0xFFEDF2F0),
            surfaceContainerHigh = Color(0xFFDDE5E2)
        )
    }
}

// Legacy aliases kept for backwards compatibility
val BlockReelsLightColorScheme: ColorScheme = buildSeedColorScheme(
    seed = AppColorSeed.EMERALD,
    isDark = false,
    amoled = false,
    contrast = AppContrastLevel.STANDARD
)

val BlockReelsDarkColorScheme: ColorScheme = buildSeedColorScheme(
    seed = AppColorSeed.EMERALD,
    isDark = true,
    amoled = false,
    contrast = AppContrastLevel.STANDARD
)

val BlockReelsAmoledColorScheme: ColorScheme = buildSeedColorScheme(
    seed = AppColorSeed.EMERALD,
    isDark = true,
    amoled = true,
    contrast = AppContrastLevel.STANDARD
)
