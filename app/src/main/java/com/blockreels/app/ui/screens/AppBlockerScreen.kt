// SPDX-License-Identifier: MIT
package com.blockreels.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blockreels.app.data.TargetAppConfig
import com.blockreels.app.state.AudioAction
import com.blockreels.app.state.BlockingScope
import com.blockreels.app.state.DetectionSensitivity
import com.blockreels.app.state.InterventionMode
import com.blockreels.app.ui.components.ModeBadge
import com.blockreels.app.ui.components.PlatformAvatar
import com.blockreels.app.ui.components.ScopeBadge
import com.blockreels.app.ui.components.SectionHeader
import com.blockreels.app.ui.components.TonalCard
import com.blockreels.app.ui.theme.MonoFontFamily
import com.blockreels.app.viewmodel.BlockReelsUiState
import kotlin.math.roundToInt

private enum class EngineSection(val label: String) {
    TARGETS("Targets"),
    PARAMETERS("Parameters")
}

private enum class TargetFilter(val label: String) {
    ALL("All"),
    ACTIVE("Active"),
    INSTALLED("Installed")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppBlockerScreen(
    uiState: BlockReelsUiState,
    bottomContentPadding: Dp = 96.dp,
    onToggleTarget: (String, Boolean) -> Unit,
    onUpdateTargetRule: (TargetAppConfig) -> Unit,
    onUpdateGlobalMode: (InterventionMode) -> Unit,
    onUpdateHoldDuration: (Int) -> Unit,
    onUpdateAllowanceMinutes: (Int) -> Unit,
    onUpdateDailyPassLimit: (Int) -> Unit,
    onUpdateEntryDelaySeconds: (Int) -> Unit,
    onUpdateSensitivity: (DetectionSensitivity) -> Unit,
    onUpdateAudioAction: (AudioAction) -> Unit,
    onUpdateCustomMessage: (String) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onOpenPreviewOverlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    var section by remember { mutableStateOf(EngineSection.TARGETS) }
    var filter by remember { mutableStateOf(TargetFilter.ALL) }
    var editingTarget by remember { mutableStateOf<TargetAppConfig?>(null) }
    var showPromptDialog by remember { mutableStateOf(false) }

    val filteredTargets = remember(uiState.targets, filter) {
        when (filter) {
            TargetFilter.ALL -> uiState.targets
            TargetFilter.ACTIVE -> uiState.targets.filter { it.isEnabled }
            TargetFilter.INSTALLED -> uiState.targets.filter { it.isInstalled }
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomContentPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "engine_section_tabs") {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    EngineSection.entries.forEachIndexed { idx, tab ->
                        SegmentedButton(
                            selected = section == tab,
                            onClick = { section = tab },
                            shape = SegmentedButtonDefaults.itemShape(idx, EngineSection.entries.size),
                            modifier = Modifier.testTag("engine_tab_${tab.name.lowercase()}")
                        ) {
                            Text(tab.label)
                        }
                    }
                }
            }

            if (section == EngineSection.TARGETS) {
                item(key = "filter_bar") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TargetFilter.entries.forEach { option ->
                                FilterChip(
                                    selected = filter == option,
                                    onClick = { filter = option },
                                    label = { Text(option.label) },
                                    modifier = Modifier.testTag("apps_filter_${option.name.lowercase()}")
                                )
                            }
                        }
                        FilledTonalButton(
                            onClick = onOpenPreviewOverlay,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("apps_preview_overlay_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayCircleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test")
                        }
                    }
                }

                if (filteredTargets.isEmpty()) {
                    item(key = "empty_filter") {
                        TonalCard {
                            Text(
                                text = "No matching targets.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    }
                } else {
                    items(filteredTargets, key = { it.packageName }) { target ->
                        TargetRuleCard(
                            target = target,
                            strictModeOverride = uiState.settings.strictMode,
                            globalHoldSeconds = uiState.settings.holdDurationSeconds,
                            globalAllowanceMinutes = uiState.settings.allowanceMinutes,
                            globalDailyLimit = uiState.settings.dailyPassLimit,
                            passesUsedToday = uiState.passesUsedToday[target.packageName] ?: 0,
                            onToggle = { enabled -> onToggleTarget(target.packageName, enabled) },
                            onConfigure = { editingTarget = target }
                        )
                    }
                }
            } else {
                item(key = "default_mode_card") {
                    EngineGlobalModeSection(
                        selected = uiState.settings.globalMode,
                        strictLocked = uiState.settings.strictMode,
                        onSelect = onUpdateGlobalMode
                    )
                }

                item(key = "detection_section") {
                    EngineDetectionSection(
                        sensitivity = uiState.settings.sensitivity,
                        entryDelaySeconds = uiState.settings.entryDelaySeconds,
                        onSelectSensitivity = onUpdateSensitivity,
                        onSelectEntryDelay = onUpdateEntryDelaySeconds
                    )
                }

                item(key = "pause_friction_section") {
                    EngineFrictionSection(
                        holdDurationSeconds = uiState.settings.holdDurationSeconds,
                        allowanceMinutes = uiState.settings.allowanceMinutes,
                        dailyPassLimit = uiState.settings.dailyPassLimit,
                        customMessage = uiState.settings.customMessage,
                        onChangeHold = onUpdateHoldDuration,
                        onChangeAllowance = onUpdateAllowanceMinutes,
                        onChangeDailyLimit = onUpdateDailyPassLimit,
                        onEditPrompt = { showPromptDialog = true }
                    )
                }

                item(key = "audio_haptics_section") {
                    EngineFeedbackSection(
                        audioAction = uiState.settings.audioAction,
                        hapticsEnabled = uiState.settings.hapticsEnabled,
                        onSelectAudio = onUpdateAudioAction,
                        onToggleHaptics = onToggleHaptics,
                        onOpenPreview = onOpenPreviewOverlay
                    )
                }
            }

            item(key = "bottom_space") {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    editingTarget?.let { target ->
        TargetRuleEditorSheet(
            target = target,
            strictLocked = uiState.settings.strictMode,
            globalHoldSeconds = uiState.settings.holdDurationSeconds,
            globalAllowanceMinutes = uiState.settings.allowanceMinutes,
            globalDailyLimit = uiState.settings.dailyPassLimit,
            onDismiss = { editingTarget = null },
            onSave = { updated ->
                onUpdateTargetRule(updated)
                editingTarget = null
            }
        )
    }

    if (showPromptDialog) {
        var draft by remember { mutableStateOf(uiState.settings.customMessage) }
        AlertDialog(
            onDismissRequest = { showPromptDialog = false },
            title = { Text("Pause Prompt") },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateCustomMessage(draft)
                    showPromptDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showPromptDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun TargetRuleCard(
    target: TargetAppConfig,
    strictModeOverride: Boolean,
    globalHoldSeconds: Int,
    globalAllowanceMinutes: Int,
    globalDailyLimit: Int,
    passesUsedToday: Int,
    onToggle: (Boolean) -> Unit,
    onConfigure: () -> Unit
) {
    val effectiveMode = if (strictModeOverride) InterventionMode.STRICT_BLOCK else target.mode
    val effectiveHold = target.holdDurationOverride ?: globalHoldSeconds
    val effectiveAllowance = target.allowanceMinutesOverride ?: globalAllowanceMinutes
    val effectiveLimit = target.dailyPassLimitOverride ?: globalDailyLimit

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onConfigure)
            .testTag("app_target_card_${target.packageName}"),
        shape = MaterialTheme.shapes.large,
        color = if (target.isEnabled) MaterialTheme.colorScheme.surfaceContainerLow
        else MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(
            width = 1.dp,
            color = if (target.isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PlatformAvatar(platformKey = target.platformKey, sizeDp = 42)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = target.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (target.isInstalled) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                    Text(
                        text = target.feedName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = target.isEnabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.testTag("app_toggle_${target.packageName}")
                )
            }

            if (target.isEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ModeBadge(mode = effectiveMode, compact = true)
                        ScopeBadge(scope = target.scope)
                        if (effectiveMode == InterventionMode.MINDFUL_PAUSE) {
                            val spec = buildString {
                                append("${effectiveHold}s / ${effectiveAllowance}m")
                                if (effectiveLimit > 0) append(" · $passesUsedToday/$effectiveLimit")
                            }
                            Text(
                                text = spec,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = "Configure ${target.displayName}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EngineGlobalModeSection(
    selected: InterventionMode,
    strictLocked: Boolean,
    onSelect: (InterventionMode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(
            title = "Default Intervention",
            trailing = if (strictLocked) {
                {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Strict Lock",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else null
        )

        val effective = if (strictLocked) InterventionMode.STRICT_BLOCK else selected
        val modes = listOf(
            InterventionMode.AUTO_BACK to "Exit via Back",
            InterventionMode.REDIRECT_HOME to "Jump to Home",
            InterventionMode.MINDFUL_PAUSE to "Hold-to-Unlock",
            InterventionMode.STRICT_BLOCK to "Hard Lock"
        )

        modes.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pair.forEach { (mode, spec) ->
                    val isSelected = effective == mode
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.large)
                            .clickable(enabled = !strictLocked) { onSelect(mode) }
                            .testTag("engine_global_mode_${mode.name.lowercase()}")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = mode.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = spec,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EngineDetectionSection(
    sensitivity: DetectionSensitivity,
    entryDelaySeconds: Int,
    onSelectSensitivity: (DetectionSensitivity) -> Unit,
    onSelectEntryDelay: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(title = "Detection Engine")
        TonalCard {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Sensitivity", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = sensitivity.specLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        DetectionSensitivity.entries.forEachIndexed { idx, level ->
                            SegmentedButton(
                                selected = sensitivity == level,
                                onClick = { onSelectSensitivity(level) },
                                shape = SegmentedButtonDefaults.itemShape(idx, DetectionSensitivity.entries.size),
                                modifier = Modifier.testTag("engine_sensitivity_${level.name.lowercase()}")
                            ) {
                                Text(level.displayName, maxLines = 1)
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Entry Grace Delay", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = if (entryDelaySeconds == 0) "Instant" else "${entryDelaySeconds}s",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0 to "Instant", 2 to "2s", 5 to "5s", 10 to "10s").forEach { (sec, label) ->
                            FilterChip(
                                selected = entryDelaySeconds == sec,
                                onClick = { onSelectEntryDelay(sec) },
                                label = { Text(label, maxLines = 1) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("engine_delay_${sec}")
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EngineFrictionSection(
    holdDurationSeconds: Int,
    allowanceMinutes: Int,
    dailyPassLimit: Int,
    customMessage: String,
    onChangeHold: (Int) -> Unit,
    onChangeAllowance: (Int) -> Unit,
    onChangeDailyLimit: (Int) -> Unit,
    onEditPrompt: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(title = "Mindful Pause & Quotas")
        TonalCard {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Hold Duration", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "${holdDurationSeconds}s",
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoFontFamily),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = holdDurationSeconds.toFloat(),
                        onValueChange = { onChangeHold(it.roundToInt()) },
                        valueRange = 3f..30f,
                        steps = 26,
                        modifier = Modifier.testTag("engine_hold_slider")
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Pass Duration", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "${allowanceMinutes}m",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1, 2, 5, 10, 15, 30).forEach { mins ->
                            FilterChip(
                                selected = allowanceMinutes == mins,
                                onClick = { onChangeAllowance(mins) },
                                label = { Text("${mins}m") },
                                modifier = Modifier.testTag("engine_pass_${mins}m")
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Daily Pass Cap", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = if (dailyPassLimit == 0) "Unlimited" else "$dailyPassLimit / day",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0 to "Off", 1 to "1", 2 to "2", 3 to "3", 5 to "5", 10 to "10").forEach { (limit, label) ->
                            FilterChip(
                                selected = dailyPassLimit == limit,
                                onClick = { onChangeDailyLimit(limit) },
                                label = { Text(label) },
                                modifier = Modifier.testTag("engine_quota_$limit")
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .clickable(onClick = onEditPrompt)
                        .testTag("engine_custom_message_row"),
                    leadingContent = { Icon(Icons.Rounded.EditNote, contentDescription = null) },
                    headlineContent = { Text("Pause Prompt") },
                    supportingContent = { Text(customMessage, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EngineFeedbackSection(
    audioAction: AudioAction,
    hapticsEnabled: Boolean,
    onSelectAudio: (AudioAction) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onOpenPreview: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(title = "Audio & Feedback")
        TonalCard {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Audio Action", style = MaterialTheme.typography.titleSmall)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        AudioAction.entries.forEachIndexed { idx, action ->
                            SegmentedButton(
                                selected = audioAction == action,
                                onClick = { onSelectAudio(action) },
                                shape = SegmentedButtonDefaults.itemShape(idx, AudioAction.entries.size),
                                modifier = Modifier.testTag("engine_audio_${action.name.lowercase()}")
                            ) {
                                Text(action.displayName, maxLines = 1)
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    leadingContent = { Icon(Icons.Rounded.Vibration, contentDescription = null) },
                    headlineContent = { Text("Haptic Feedback") },
                    trailingContent = {
                        Switch(
                            checked = hapticsEnabled,
                            onCheckedChange = onToggleHaptics,
                            modifier = Modifier.testTag("engine_haptics_switch")
                        )
                    }
                )

                FilledTonalButton(
                    onClick = onOpenPreview,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("engine_preview_overlay_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayCircleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test Pause Overlay")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TargetRuleEditorSheet(
    target: TargetAppConfig,
    strictLocked: Boolean,
    globalHoldSeconds: Int,
    globalAllowanceMinutes: Int,
    globalDailyLimit: Int,
    onDismiss: () -> Unit,
    onSave: (TargetAppConfig) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var enabled by remember { mutableStateOf(target.isEnabled) }
    var scope by remember { mutableStateOf(target.scope) }
    var mode by remember { mutableStateOf(target.mode) }
    var holdOverride by remember { mutableStateOf(target.holdDurationOverride) }
    var allowanceOverride by remember { mutableStateOf(target.allowanceMinutesOverride) }
    var limitOverride by remember { mutableStateOf(target.dailyPassLimitOverride) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PlatformAvatar(platformKey = target.platformKey, sizeDp = 44)
                Column(modifier = Modifier.weight(1f)) {
                    Text(target.displayName, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        target.feedName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = enabled, onCheckedChange = { enabled = it })
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Blocking Scope", style = MaterialTheme.typography.titleSmall)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    BlockingScope.entries.forEachIndexed { idx, s ->
                        SegmentedButton(
                            selected = scope == s,
                            onClick = { scope = s },
                            shape = SegmentedButtonDefaults.itemShape(idx, BlockingScope.entries.size)
                        ) {
                            Text(s.displayName)
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Intervention Mode", style = MaterialTheme.typography.titleSmall)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    InterventionMode.entries.forEachIndexed { idx, m ->
                        SegmentedButton(
                            selected = (if (strictLocked) InterventionMode.STRICT_BLOCK else mode) == m,
                            enabled = !strictLocked,
                            onClick = { mode = m },
                            shape = SegmentedButtonDefaults.itemShape(idx, InterventionMode.entries.size)
                        ) {
                            Text(m.shortLabel, maxLines = 1)
                        }
                    }
                }
            }

            if (mode == InterventionMode.MINDFUL_PAUSE && !strictLocked) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Hold Override", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = holdOverride?.let { "${it}s" } ?: "Global (${globalHoldSeconds}s)",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = holdOverride == null,
                            onClick = { holdOverride = null },
                            label = { Text("Global") }
                        )
                        listOf(3, 5, 10, 15, 30).forEach { sec ->
                            FilterChip(
                                selected = holdOverride == sec,
                                onClick = { holdOverride = sec },
                                label = { Text("${sec}s") }
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Pass Length Override", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = allowanceOverride?.let { "${it}m" } ?: "Global (${globalAllowanceMinutes}m)",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = allowanceOverride == null,
                            onClick = { allowanceOverride = null },
                            label = { Text("Global") }
                        )
                        listOf(1, 2, 5, 10, 15).forEach { min ->
                            FilterChip(
                                selected = allowanceOverride == min,
                                onClick = { allowanceOverride = min },
                                label = { Text("${min}m") }
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Daily Pass Cap Override", style = MaterialTheme.typography.titleSmall)
                        val globalText = if (globalDailyLimit == 0) "Off" else "$globalDailyLimit/d"
                        Text(
                            text = limitOverride?.let { if (it == 0) "Off" else "$it/d" } ?: "Global ($globalText)",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = limitOverride == null,
                            onClick = { limitOverride = null },
                            label = { Text("Global") }
                        )
                        listOf(0 to "Off", 1 to "1", 2 to "2", 3 to "3", 5 to "5").forEach { (cap, label) ->
                            FilterChip(
                                selected = limitOverride == cap,
                                onClick = { limitOverride = cap },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        onSave(
                            target.copy(
                                isEnabled = enabled,
                                scope = scope,
                                mode = mode,
                                holdDurationOverride = holdOverride,
                                allowanceMinutesOverride = allowanceOverride,
                                dailyPassLimitOverride = limitOverride
                            )
                        )
                    }
                ) {
                    Text("Save")
                }
            }
        }
    }
}
