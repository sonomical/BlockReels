// SPDX-License-Identifier: MIT
package com.blockreels.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.blockreels.app.data.AppColorSeed
import com.blockreels.app.data.AppContrastLevel
import com.blockreels.app.data.CustomPaletteConfig
import com.blockreels.app.ui.theme.LocalIsDarkTheme
import com.blockreels.app.ui.theme.MonoFontFamily
import com.blockreels.app.ui.theme.argbToHsl
import com.blockreels.app.ui.theme.buildSeedColorScheme
import com.blockreels.app.ui.theme.formatArgbHex
import com.blockreels.app.ui.theme.hslToArgb
import com.blockreels.app.ui.theme.parseHexToArgb

private enum class PaletteRole(val label: String) {
    PRIMARY("Primary"),
    SECONDARY("Secondary"),
    TERTIARY("Tertiary")
}

private data class CuratedCustomPreset(
    val name: String,
    val config: CustomPaletteConfig
)

private val CURATED_CUSTOM_PRESETS = listOf(
    CuratedCustomPreset(
        "Cyber Teal",
        CustomPaletteConfig(0xFF00796B, 0xFF2563EB, 0xFFF59E0B, 0.35f)
    ),
    CuratedCustomPreset(
        "Sunset Coral",
        CustomPaletteConfig(0xFFD9465F, 0xFF7C3AED, 0xFFEAB308, 0.40f)
    ),
    CuratedCustomPreset(
        "Midnight Indigo",
        CustomPaletteConfig(0xFF3730A3, 0xFF0284C7, 0xFF10B981, 0.45f)
    ),
    CuratedCustomPreset(
        "Matcha Gold",
        CustomPaletteConfig(0xFF4D7C0F, 0xFF0F766E, 0xFFD97706, 0.35f)
    ),
    CuratedCustomPreset(
        "Crimson Slate",
        CustomPaletteConfig(0xFFB91C1C, 0xFF475569, 0xFF0284C7, 0.25f)
    ),
    CuratedCustomPreset(
        "Rosewood",
        CustomPaletteConfig(0xFF9D174D, 0xFF6D28D9, 0xFF0D9488, 0.38f)
    )
)

private val QUICK_SWATCH_COLORS = listOf(
    0xFF0F766EL,
    0xFF0284C7L,
    0xFF2563EBL,
    0xFF4F46E5L,
    0xFF7C3AEDL,
    0xFF9D174DL,
    0xFFDC2626L,
    0xFFEA580CL,
    0xFFD97706L,
    0xFF65A30DL,
    0xFF15803DL,
    0xFF475569L
)

private val HUE_SPECTRUM_STOPS = listOf(
    Color(0xFFE53935),
    Color(0xFFFB8C00),
    Color(0xFFFDD835),
    Color(0xFF43A047),
    Color(0xFF00ACC1),
    Color(0xFF1E88E5),
    Color(0xFF8E24AA),
    Color(0xFFE53935)
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CustomPaletteEditorSheet(
    initial: CustomPaletteConfig,
    amoled: Boolean,
    contrastLevel: AppContrastLevel,
    onDismiss: () -> Unit,
    onApply: (CustomPaletteConfig) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDark = LocalIsDarkTheme.current

    var draft by remember { mutableStateOf(initial) }
    var activeRole by remember { mutableStateOf(PaletteRole.PRIMARY) }

    val activeArgb = when (activeRole) {
        PaletteRole.PRIMARY -> draft.primaryArgb
        PaletteRole.SECONDARY -> draft.secondaryArgb
        PaletteRole.TERTIARY -> draft.tertiaryArgb
    }

    var hue by remember { mutableFloatStateOf(argbToHsl(activeArgb)[0]) }
    var saturation by remember { mutableFloatStateOf(argbToHsl(activeArgb)[1].coerceIn(0.10f, 1f)) }
    var lightness by remember { mutableFloatStateOf(argbToHsl(activeArgb)[2].coerceIn(0.22f, 0.72f)) }
    var hexInput by remember { mutableStateOf(formatArgbHex(activeArgb)) }
    var hexError by remember { mutableStateOf(false) }

    fun syncControlsFromArgb(argb: Long) {
        val hsl = argbToHsl(argb)
        hue = hsl[0]
        saturation = hsl[1].coerceIn(0.10f, 1f)
        lightness = hsl[2].coerceIn(0.22f, 0.72f)
        hexInput = formatArgbHex(argb)
        hexError = false
    }

    fun applyArgbToActiveRole(argb: Long) {
        draft = when (activeRole) {
            PaletteRole.PRIMARY -> draft.copy(primaryArgb = argb)
            PaletteRole.SECONDARY -> draft.copy(secondaryArgb = argb)
            PaletteRole.TERTIARY -> draft.copy(tertiaryArgb = argb)
        }
    }

    LaunchedEffect(activeRole) {
        val current = when (activeRole) {
            PaletteRole.PRIMARY -> draft.primaryArgb
            PaletteRole.SECONDARY -> draft.secondaryArgb
            PaletteRole.TERTIARY -> draft.tertiaryArgb
        }
        syncControlsFromArgb(current)
    }

    val previewScheme = remember(draft, isDark, amoled, contrastLevel) {
        buildSeedColorScheme(
            seed = AppColorSeed.CUSTOM,
            isDark = isDark,
            amoled = amoled,
            contrast = contrastLevel,
            customPalette = draft
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Custom Color Palette",
                        style = MaterialTheme.typography.headlineSmallEmphasized
                    )
                    Text(
                        text = "Generates full opaque Material 3 light and dark tones",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = {
                        val defaultCfg = CustomPaletteConfig()
                        draft = defaultCfg
                        val resetArgb = when (activeRole) {
                            PaletteRole.PRIMARY -> defaultCfg.primaryArgb
                            PaletteRole.SECONDARY -> defaultCfg.secondaryArgb
                            PaletteRole.TERTIARY -> defaultCfg.tertiaryArgb
                        }
                        syncControlsFromArgb(resetArgb)
                    },
                    modifier = Modifier.testTag("custom_palette_reset_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset")
                }
            }

            // Live Material 3 Expressive Preview Card rendered with the draft scheme
            MaterialTheme(colorScheme = previewScheme) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = ExpressiveShapes.Cookie7Sided,
                                    color = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Shield,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Live Theme Preview",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${formatArgbHex(draft.primaryArgb)} · ${formatArgbHex(draft.secondaryArgb)} · ${formatArgbHex(draft.tertiaryArgb)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Primary", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Secondary", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Tertiary", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            // Quick curated harmonic presets
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quick Palettes",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilledTonalButton(
                        onClick = {
                            val pHsl = argbToHsl(draft.primaryArgb)
                            val sec = hslToArgb((pHsl[0] + 38f) % 360f, (pHsl[1] * 0.78f).coerceIn(0.22f, 0.75f), 0.42f)
                            val ter = hslToArgb((pHsl[0] + 150f) % 360f, (pHsl[1] * 0.88f).coerceIn(0.30f, 0.85f), 0.46f)
                            val harmonized = draft.copy(secondaryArgb = sec, tertiaryArgb = ter)
                            draft = harmonized
                            val current = when (activeRole) {
                                PaletteRole.PRIMARY -> harmonized.primaryArgb
                                PaletteRole.SECONDARY -> harmonized.secondaryArgb
                                PaletteRole.TERTIARY -> harmonized.tertiaryArgb
                            }
                            syncControlsFromArgb(current)
                        },
                        shapes = ButtonDefaults.shapes(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("custom_palette_harmony_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Auto-Match", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CURATED_CUSTOM_PRESETS.forEach { preset ->
                        val isMatch = draft.primaryArgb == preset.config.primaryArgb &&
                            draft.secondaryArgb == preset.config.secondaryArgb &&
                            draft.tertiaryArgb == preset.config.tertiaryArgb
                        Surface(
                            onClick = {
                                draft = preset.config
                                val selectedArgb = when (activeRole) {
                                    PaletteRole.PRIMARY -> preset.config.primaryArgb
                                    PaletteRole.SECONDARY -> preset.config.secondaryArgb
                                    PaletteRole.TERTIARY -> preset.config.tertiaryArgb
                                }
                                syncControlsFromArgb(selectedArgb)
                            },
                            shape = CircleShape,
                            color = if (isMatch) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = if (isMatch) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier.testTag("custom_preset_${preset.name.lowercase().replace(' ', '_')}")
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    ColorDot(Color(preset.config.primaryArgb), 14)
                                    ColorDot(Color(preset.config.secondaryArgb), 14)
                                    ColorDot(Color(preset.config.tertiaryArgb), 14)
                                }
                                Text(
                                    text = preset.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isMatch) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Role selector: Primary / Secondary / Tertiary
            ConnectedToggleButtonGroup(
                options = PaletteRole.entries,
                selected = activeRole,
                onSelect = { activeRole = it },
                label = { it.label },
                tagFor = { "custom_palette_role_${it.name.lowercase()}" }
            )

            // Quick single-color swatch bar for the active role
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QUICK_SWATCH_COLORS.forEach { swatchArgb ->
                    val isSelected = activeArgb == swatchArgb
                    Surface(
                        onClick = {
                            applyArgbToActiveRole(swatchArgb)
                            syncControlsFromArgb(swatchArgb)
                        },
                        shape = CircleShape,
                        color = Color(swatchArgb),
                        contentColor = Color.White,
                        border = if (isSelected) BorderStroke(2.5.dp, MaterialTheme.colorScheme.onSurface) else null,
                        modifier = Modifier.size(34.dp)
                    ) {
                        if (isSelected) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Selected color",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Interactive Hue Spectrum Bar
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${activeRole.label} Hue",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = "${hue.toInt()}°",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                HueSpectrumBar(
                    hue = hue,
                    onHueChange = { nextHue ->
                        hue = nextHue
                        val nextArgb = hslToArgb(hue, saturation, lightness)
                        applyArgbToActiveRole(nextArgb)
                        hexInput = formatArgbHex(nextArgb)
                        hexError = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("custom_palette_hue_bar")
                )
            }

            // Saturation & Richness sliders
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Chroma / Saturation", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = "${(saturation * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = saturation,
                        onValueChange = { s ->
                            saturation = s
                            val nextArgb = hslToArgb(hue, saturation, lightness)
                            applyArgbToActiveRole(nextArgb)
                            hexInput = formatArgbHex(nextArgb)
                            hexError = false
                        },
                        valueRange = 0.10f..1.0f,
                        modifier = Modifier.testTag("custom_palette_sat_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tone / Depth", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = "${(lightness * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = lightness,
                        onValueChange = { l ->
                            lightness = l
                            val nextArgb = hslToArgb(hue, saturation, lightness)
                            applyArgbToActiveRole(nextArgb)
                            hexInput = formatArgbHex(nextArgb)
                            hexError = false
                        },
                        valueRange = 0.22f..0.72f,
                        modifier = Modifier.testTag("custom_palette_lightness_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Surface Color Tint", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = "${(draft.surfaceTintStrength * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = draft.surfaceTintStrength,
                        onValueChange = { tint ->
                            draft = draft.copy(surfaceTintStrength = tint)
                        },
                        valueRange = 0f..1f,
                        modifier = Modifier.testTag("custom_palette_surface_tint_slider")
                    )
                }
            }

            // Direct Hex Code Input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = ExpressiveShapes.Cookie4Sided,
                    color = Color(activeArgb),
                    modifier = Modifier.size(46.dp)
                ) {}
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { raw ->
                        val cleaned = raw.uppercase().filter { it == '#' || it in '0'..'9' || it in 'A'..'F' }.take(7)
                        hexInput = cleaned
                        val parsed = parseHexToArgb(cleaned)
                        if (parsed != null) {
                            hexError = false
                            applyArgbToActiveRole(parsed)
                            val hsl = argbToHsl(parsed)
                            hue = hsl[0]
                            saturation = hsl[1].coerceIn(0.10f, 1f)
                            lightness = hsl[2].coerceIn(0.22f, 0.72f)
                        } else {
                            hexError = cleaned.isNotEmpty()
                        }
                    },
                    label = { Text("${activeRole.label} Hex (#RRGGBB)") },
                    isError = hexError,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFontFamily),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("custom_palette_hex_input")
                )
            }

            // Footer actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("custom_palette_cancel_btn")
                ) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { onApply(draft) },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.testTag("custom_palette_apply_btn")
                ) {
                    Text("Apply Palette")
                }
            }
        }
    }
}

@Composable
private fun ColorDot(color: Color, sizeDp: Int) {
    Surface(
        shape = CircleShape,
        color = color,
        modifier = Modifier.size(sizeDp.dp)
    ) {}
}

@Composable
private fun HueSpectrumBar(
    hue: Float,
    onHueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val thumbBorderColor = MaterialTheme.colorScheme.onSurface
    val thumbFillColor = remember(hue) { Color(hslToArgb(hue, 0.85f, 0.48f)) }
    val spectrumBrush = remember { Brush.horizontalGradient(HUE_SPECTRUM_STOPS) }
    val latestOnHueChange by rememberUpdatedState(onHueChange)

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                    latestOnHueChange(fraction * 360f)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    val fraction = (change.position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                    latestOnHueChange(fraction * 360f)
                }
            }
    ) {
        val barHeight = size.height * 0.40f
        val barTop = (size.height - barHeight) / 2f
        val radius = CornerRadius(barHeight / 2f, barHeight / 2f)

        drawRoundRect(
            brush = spectrumBrush,
            topLeft = Offset(0f, barTop),
            size = androidx.compose.ui.geometry.Size(size.width, barHeight),
            cornerRadius = radius
        )

        val thumbRadius = size.height * 0.34f
        val clampedX = ((hue / 360f).coerceIn(0f, 1f) * size.width)
            .coerceIn(thumbRadius, (size.width - thumbRadius).coerceAtLeast(thumbRadius))
        val center = Offset(clampedX, size.height / 2f)

        drawCircle(
            color = thumbFillColor,
            radius = thumbRadius,
            center = center
        )
        drawCircle(
            color = thumbBorderColor,
            radius = thumbRadius,
            center = center,
            style = Stroke(width = 3.dp.toPx())
        )
    }
}
