// SPDX-License-Identifier: MIT
package com.blockreels.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.blockreels.app.ui.theme.MonoFontFamily
import kotlin.math.roundToInt

/**
 * Material 3 Expressive numerical value picker combining:
 * 1) One-tap quick presets for speed.
 * 2) Fine-grained -/+ stepper & slider across the full logical range.
 * 3) Direct numeric entry dialog validated strictly against [range].
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveNumberSelector(
    title: String,
    value: Int,
    range: IntRange,
    quickPresets: List<Int>,
    unitPluralLabel: String,
    valueFormatter: (Int) -> String,
    presetLabel: (Int) -> String,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    step: Int = 1,
    leadingIcon: ImageVector? = null,
    accentSecondary: Boolean = false,
    tagPrefix: String = "number_selector",
    showSlider: Boolean = true
) {
    var showCustomDialog by remember { mutableStateOf(false) }
    val clampedValue = value.coerceIn(range.first, range.last)
    val isCustomValue = clampedValue !in quickPresets

    val pillContainer = if (accentSecondary) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val pillContent = if (accentSecondary) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = if (accentSecondary) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Limit: ${range.first}–${range.last} $unitPluralLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                onClick = { showCustomDialog = true },
                shape = CircleShape,
                color = pillContainer,
                contentColor = pillContent,
                modifier = Modifier.testTag("${tagPrefix}_value_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = valueFormatter(clampedValue),
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoFontFamily),
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Enter custom $title",
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        // Quick preset chips + Custom entry chip
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            quickPresets.forEach { preset ->
                FilterChip(
                    selected = clampedValue == preset,
                    onClick = { onValueChange(preset.coerceIn(range.first, range.last)) },
                    label = {
                        Text(
                            text = presetLabel(preset),
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily)
                        )
                    },
                    modifier = Modifier.testTag("${tagPrefix}_preset_$preset")
                )
            }
            FilterChip(
                selected = isCustomValue,
                onClick = { showCustomDialog = true },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                },
                label = {
                    Text(
                        text = if (isCustomValue) "Custom (${presetLabel(clampedValue)})" else "Custom",
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                modifier = Modifier.testTag("${tagPrefix}_custom_chip")
            )
        }

        // Precision stepper + slider bar
        if (showSlider) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilledTonalIconButton(
                        onClick = {
                            onValueChange((clampedValue - step).coerceIn(range.first, range.last))
                        },
                        enabled = clampedValue > range.first,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.testTag("${tagPrefix}_minus_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Remove,
                            contentDescription = "Decrease $title"
                        )
                    }

                    Slider(
                        value = clampedValue.toFloat(),
                        onValueChange = { raw ->
                            val snapped = if (step > 1) {
                                ((raw - range.first) / step).roundToInt() * step + range.first
                            } else {
                                raw.roundToInt()
                            }
                            onValueChange(snapped.coerceIn(range.first, range.last))
                        },
                        valueRange = range.first.toFloat()..range.last.toFloat(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("${tagPrefix}_slider")
                    )

                    FilledTonalIconButton(
                        onClick = {
                            onValueChange((clampedValue + step).coerceIn(range.first, range.last))
                        },
                        enabled = clampedValue < range.last,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.testTag("${tagPrefix}_plus_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Increase $title"
                        )
                    }
                }
            }
        }
    }

    if (showCustomDialog) {
        NumberInputDialog(
            title = title,
            initialValue = clampedValue,
            range = range,
            unitPluralLabel = unitPluralLabel,
            quickPresets = quickPresets,
            presetLabel = presetLabel,
            onDismiss = { showCustomDialog = false },
            onConfirm = { picked ->
                onValueChange(picked)
                showCustomDialog = false
            }
        )
    }
}

/**
 * Nullable override variant used in per-app rule sheets:
 * offers a "Default" chip alongside quick presets and a custom numeric entry within [range].
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OptionalExpressiveNumberSelector(
    title: String,
    value: Int?,
    defaultValue: Int,
    range: IntRange,
    quickPresets: List<Int>,
    unitPluralLabel: String,
    valueFormatter: (Int) -> String,
    presetLabel: (Int) -> String,
    onValueChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    step: Int = 1,
    tagPrefix: String = "optional_number"
) {
    var showCustomDialog by remember { mutableStateOf(false) }
    val effectiveValue = (value ?: defaultValue).coerceIn(range.first, range.last)
    val isCustomOverride = value != null && value !in quickPresets

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (value == null) {
                        "Using default (${valueFormatter(defaultValue)}) · Range ${range.first}–${range.last}"
                    } else {
                        "Custom override · Range ${range.first}–${range.last} $unitPluralLabel"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                onClick = { showCustomDialog = true },
                shape = CircleShape,
                color = if (value == null) MaterialTheme.colorScheme.surfaceContainerHigh
                else MaterialTheme.colorScheme.primaryContainer,
                contentColor = if (value == null) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (value == null) "Default" else valueFormatter(effectiveValue),
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Set custom $title",
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilterChip(
                selected = value == null,
                onClick = { onValueChange(null) },
                label = { Text("Default (${presetLabel(defaultValue)})") },
                modifier = Modifier.testTag("${tagPrefix}_default")
            )
            quickPresets.forEach { preset ->
                FilterChip(
                    selected = value == preset,
                    onClick = { onValueChange(preset.coerceIn(range.first, range.last)) },
                    label = {
                        Text(
                            text = presetLabel(preset),
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily)
                        )
                    },
                    modifier = Modifier.testTag("${tagPrefix}_preset_$preset")
                )
            }
            FilterChip(
                selected = isCustomOverride,
                onClick = { showCustomDialog = true },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                },
                label = {
                    Text(if (isCustomOverride) "Custom (${presetLabel(effectiveValue)})" else "Custom")
                },
                modifier = Modifier.testTag("${tagPrefix}_custom")
            )
        }

        if (value != null) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilledTonalIconButton(
                        onClick = { onValueChange((effectiveValue - step).coerceIn(range.first, range.last)) },
                        enabled = effectiveValue > range.first,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(Icons.Rounded.Remove, contentDescription = "Decrease $title")
                    }
                    Slider(
                        value = effectiveValue.toFloat(),
                        onValueChange = { raw -> onValueChange(raw.roundToInt().coerceIn(range.first, range.last)) },
                        valueRange = range.first.toFloat()..range.last.toFloat(),
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalIconButton(
                        onClick = { onValueChange((effectiveValue + step).coerceIn(range.first, range.last)) },
                        enabled = effectiveValue < range.last,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Increase $title")
                    }
                }
            }
        }
    }

    if (showCustomDialog) {
        NumberInputDialog(
            title = title,
            initialValue = effectiveValue,
            range = range,
            unitPluralLabel = unitPluralLabel,
            quickPresets = quickPresets,
            presetLabel = presetLabel,
            onDismiss = { showCustomDialog = false },
            onConfirm = { picked ->
                onValueChange(picked)
                showCustomDialog = false
            }
        )
    }
}

/**
 * Reusable numeric input dialog with strict logical range validation and quick presets.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NumberInputDialog(
    title: String,
    initialValue: Int,
    range: IntRange,
    unitPluralLabel: String,
    quickPresets: List<Int>,
    presetLabel: (Int) -> String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
    confirmLabel: String = "Apply"
) {
    var draft by remember(initialValue) { mutableStateOf(initialValue.toString()) }
    val parsed = draft.trim().toIntOrNull()
    val isValid = parsed != null && parsed in range
    val errorText = when {
        draft.isBlank() -> "Enter a number between ${range.first} and ${range.last}"
        parsed == null -> "Numbers only"
        parsed < range.first -> "Minimum allowed is ${range.first} $unitPluralLabel"
        parsed > range.last -> "Maximum allowed is ${range.last} $unitPluralLabel"
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Choose any value between ${range.first} and ${range.last} $unitPluralLabel, or pick a quick preset.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = draft,
                    onValueChange = { input ->
                        val digitsOnly = input.filter { it.isDigit() }.take(4)
                        draft = digitsOnly
                    },
                    singleLine = true,
                    label = { Text("Value ($unitPluralLabel)") },
                    isError = errorText != null,
                    supportingText = {
                        Text(
                            text = errorText ?: "Valid range: ${range.first}–${range.last} $unitPluralLabel"
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("number_dialog_input")
                )

                if (quickPresets.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        quickPresets.forEach { preset ->
                            FilterChip(
                                selected = parsed == preset,
                                onClick = { draft = preset.toString() },
                                label = {
                                    Text(
                                        text = presetLabel(preset),
                                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { parsed?.let(onConfirm) },
                enabled = isValid,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.testTag("number_dialog_confirm_btn")
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
