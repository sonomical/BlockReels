// SPDX-License-Identifier: MIT
package com.blockreels.app.ui.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.blockreels.app.R
import com.blockreels.app.data.AppFontScale

val DisplayFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans, FontWeight.ExtraBold)
)

val BodyFontFamily = FontFamily(
    Font(R.font.inter, FontWeight.Normal),
    Font(R.font.inter, FontWeight.Medium),
    Font(R.font.inter, FontWeight.SemiBold),
    Font(R.font.inter, FontWeight.Bold)
)

val MonoFontFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Medium),
    Font(R.font.jetbrains_mono, FontWeight.Bold)
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun buildBlockReelsTypography(fontScale: AppFontScale = AppFontScale.DEFAULT): Typography {
    val m = fontScale.multiplier
    fun s(v: Float) = (v * m).sp

    return Typography(
        displayLarge = TextStyle(
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = s(57f),
            lineHeight = s(64f),
            letterSpacing = (-0.25).sp
        ),
        displayMedium = TextStyle(
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = s(45f),
            lineHeight = s(52f),
            letterSpacing = (-0.2).sp
        ),
        displaySmall = TextStyle(
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = s(36f),
            lineHeight = s(44f),
            letterSpacing = (-0.25).sp
        ),
        headlineLarge = TextStyle(
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = s(32f),
            lineHeight = s(40f),
            letterSpacing = (-0.2).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = s(28f),
            lineHeight = s(36f),
            letterSpacing = (-0.15).sp
        ),
        headlineSmall = TextStyle(
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = s(24f),
            lineHeight = s(32f)
        ),
        titleLarge = TextStyle(
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = s(22f),
            lineHeight = s(28f)
        ),
        titleMedium = TextStyle(
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = s(16f),
            lineHeight = s(24f),
            letterSpacing = 0.15.sp
        ),
        titleSmall = TextStyle(
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = s(14f),
            lineHeight = s(20f),
            letterSpacing = 0.1.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = s(16f),
            lineHeight = s(24f),
            letterSpacing = 0.25.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = s(14f),
            lineHeight = s(20f),
            letterSpacing = 0.25.sp
        ),
        bodySmall = TextStyle(
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = s(12f),
            lineHeight = s(16f),
            letterSpacing = 0.4.sp
        ),
        labelLarge = TextStyle(
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = s(14f),
            lineHeight = s(20f),
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = s(12f),
            lineHeight = s(16f),
            letterSpacing = 0.5.sp
        ),
        labelSmall = TextStyle(
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = s(11f),
            lineHeight = s(16f),
            letterSpacing = 0.5.sp
        ),
        displaySmallEmphasized = TextStyle(
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = s(36f),
            lineHeight = s(44f),
            letterSpacing = (-0.4).sp
        ),
        headlineLargeEmphasized = TextStyle(
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = s(32f),
            lineHeight = s(38f),
            letterSpacing = (-0.35).sp
        ),
        headlineMediumEmphasized = TextStyle(
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = s(28f),
            lineHeight = s(36f),
            letterSpacing = (-0.25).sp
        ),
        headlineSmallEmphasized = TextStyle(
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = s(24f),
            lineHeight = s(32f),
            letterSpacing = (-0.15).sp
        ),
        titleLargeEmphasized = TextStyle(
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = s(22f),
            lineHeight = s(28f)
        ),
        titleMediumEmphasized = TextStyle(
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = s(16f),
            lineHeight = s(24f),
            letterSpacing = 0.1.sp
        ),
        labelLargeEmphasized = TextStyle(
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = s(14f),
            lineHeight = s(20f),
            letterSpacing = 0.1.sp
        )
    )
}

val BlockReelsTypography = buildBlockReelsTypography(AppFontScale.DEFAULT)
