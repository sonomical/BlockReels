// SPDX-License-Identifier: MIT
package com.blockreels.app.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TonalToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blockreels.app.data.TargetAppConfig
import com.blockreels.app.state.AudioAction
import com.blockreels.app.state.BlockingScope
import com.blockreels.app.state.DetectionSensitivity
import com.blockreels.app.state.InterventionMode
import com.blockreels.app.ui.components.ConnectedToggleButtonGroup
import com.blockreels.app.ui.components.ExpressiveCard
import com.blockreels.app.ui.components.ExpressiveSwitch
import com.blockreels.app.ui.components.ModeBadge
import com.blockreels.app.ui.components.PlatformAvatar
import com.blockreels.app.ui.components.ScopeBadge
import com.blockreels.app.ui.components.SectionHeader
import com.blockreels.app.ui.components.segmentedListShape
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppBlockerScreen(
    uiState: BlockReelsUiState,
    bottomContentPadding: Dp = 24.dp,
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Engine Status Summary Strip
            item(key = "engine_summary") {
                val activeMode = if (uiState.settings.strictMode) {
                    InterventionMode.STRICT_BLOCK
                } else {
                    uiState.settings.globalMode
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EngineStatPill(
                        label = "Guarded",
                        value = "${uiState.enabledTargetsCount}/${uiState.targets.size}",
                        container = MaterialTheme.colorScheme.primaryContainer,
                        content = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    EngineStatPill(
                        label = "Installed",
                        value = uiState.installedTargetsCount.toString(),
                        container = MaterialTheme.colorScheme.surfaceContainerLow,
                        content = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    EngineStatPill(
                        label = "Default",
                        value = activeMode.shortLabel,
                        container = MaterialTheme.colorScheme.secondaryContainer,
                        content = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item(key = "engine_section_tabs") {
                ConnectedToggleButtonGroup(
                    options = EngineSection.entries,
                    selected = section,
                    onSelect = { section = it },
                    label = { it.label },
                    tagFor = { "engine_tab_${it.name.lowercase()}" }
                )
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
                                val count = when (option) {
                                    TargetFilter.ALL -> uiState.targets.size
                                    TargetFilter.ACTIVE -> uiState.enabledTargetsCount
                                    TargetFilter.INSTALLED -> uiState.installedTargetsCount
                                }
                                FilterChip(
                                    selected = filter == option,
                                    onClick = { filter = option },
                                    label = { Text("${option.label} ($count)", maxLines = 1) },
                                    modifier = Modifier.testTag("apps_filter_${option.name.lowercase()}")
                                )
                            }
                        }
                        FilledTonalButton(
                            onClick = onOpenPreviewOverlay,
                            shapes = ButtonDefaults.shapes(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("apps_preview_overlay_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayCircleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Test")
                        }
                    }
                }

                if (filteredTargets.isEmpty()) {
                    item(key = "empty_filter") {
                        ExpressiveCard {
                            Text(
                                text = "No targets match this filter.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    }
                } else {
                    item(key = "targets_segmented_list") {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            filteredTargets.forEachIndexed { idx, target ->
                                TargetRuleCard(
                                    target = target,
                                    shape = segmentedListShape(idx, filteredTargets.size),
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
                Button(
                    onClick = {
                        onUpdateCustomMessage(draft)
                        showPromptDialog = false
                    },
                    shapes = ButtonDefaults.shapes()
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showPromptDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EngineStatPill(
    label: String,
    value: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.largeIncreased,
        color = container,
        contentColor = content
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLargeEmphasized.copy(fontFamily = MonoFontFamily),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TargetRuleCard(
    target: TargetAppConfig,
    shape: Shape,
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
        onClick = onConfigure,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("app_target_card_${target.packageName}"),
        shape = shape,
        color = if (target.isEnabled) MaterialTheme.colorScheme.surfaceContainerLow
        else MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PlatformAvatar(
                    platformKey = target.platformKey,
                    packageName = target.packageName,
                    sizeDp = 46,
                    active = target.isEnabled
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = target.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (target.isInstalled) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                Text(
                                    text = "Installed",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = target.feedName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ExpressiveSwitch(
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
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = "Configure ${target.displayName}",
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (target.hasCustomOverrides) "Custom" else "Rule",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class ModeCardOption(
    val mode: InterventionMode,
    val subtitle: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EngineGlobalModeSection(
    selected: InterventionMode,
    strictLocked: Boolean,
    onSelect: (InterventionMode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(
            title = "Default Intervention",
            trailing = if (strictLocked) {
                {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Strict Lock",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            } else null
        )

        val effective = if (strictLocked) InterventionMode.STRICT_BLOCK else selected
        val modes = listOf(
            ModeCardOption(InterventionMode.AUTO_BACK, "Exit via Back", Icons.Rounded.Undo),
            ModeCardOption(InterventionMode.REDIRECT_HOME, "Jump to Home", Icons.Rounded.Home),
            ModeCardOption(InterventionMode.MINDFUL_PAUSE, "Hold-to-Unlock", Icons.Rounded.SelfImprovement),
            ModeCardOption(InterventionMode.STRICT_BLOCK, "Hard Lock", Icons.Rounded.Block)
        )

        modes.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pair.forEach { option ->
                    val isSelected = effective == option.mode
                    TonalToggleButton(
                        checked = isSelected,
                        enabled = !strictLocked,
                        onCheckedChange = { if (it) onSelect(option.mode) },
                        shapes = ToggleButtonDefaults.shapes(
                            shape = MaterialTheme.shapes.largeIncreased,
                            pressedShape = MaterialTheme.shapes.medium,
                            checkedShape = MaterialTheme.shapes.extraLarge
                        ),
                        colors = ToggleButtonDefaults.tonalToggleButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("engine_global_mode_${option.mode.name.lowercase()}")
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = option.mode.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = option.subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EngineDetectionSection(
    sensitivity: DetectionSensitivity,
    entryDelaySeconds: Int,
    onSelectSensitivity: (DetectionSensitivity) -> Unit,
    onSelectEntryDelay: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title = "Detection Engine")
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Surface(
                shape = segmentedListShape(0, 2),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Sensitivity", style = MaterialTheme.typography.titleSmall)
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(
                                text = sensitivity.specLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    ConnectedToggleButtonGroup(
                        options = DetectionSensitivity.entries,
                        selected = sensitivity,
                        onSelect = onSelectSensitivity,
                        label = { it.displayName },
                        tagFor = { "engine_sensitivity_${it.name.lowercase()}" }
                    )
                }
            }

            Surface(
                shape = segmentedListShape(1, 2),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Entry Grace Delay", style = MaterialTheme.typography.titleSmall)
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(
                                text = if (entryDelaySeconds == 0) "Instant" else "${entryDelaySeconds}s",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    ConnectedToggleButtonGroup(
                        options = listOf(0, 2, 5, 10),
                        selected = entryDelaySeconds,
                        onSelect = onSelectEntryDelay,
                        label = { if (it == 0) "Instant" else "${it}s" },
                        tagFor = { "engine_delay_$it" }
                    )
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
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title = "Mindful Pause & Quotas")
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Surface(
                shape = segmentedListShape(0, 4),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Hold Duration", style = MaterialTheme.typography.titleSmall)
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(
                                text = "${holdDurationSeconds}s",
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Slider(
                        value = holdDurationSeconds.toFloat(),
                        onValueChange = { onChangeHold(it.roundToInt()) },
                        valueRange = 3f..30f,
                        steps = 26,
                        modifier = Modifier.testTag("engine_hold_slider")
                    )
                }
            }

            Surface(
                shape = segmentedListShape(1, 4),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
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
            }

            Surface(
                shape = segmentedListShape(2, 4),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
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
            }

            Surface(
                onClick = onEditPrompt,
                shape = segmentedListShape(3, 4),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("engine_custom_message_row")
            ) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    leadingContent = { Icon(Icons.Rounded.EditNote, contentDescription = null) },
                    headlineContent = { Text("Pause Prompt") },
                    supportingContent = { Text(customMessage, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EngineFeedbackSection(
    audioAction: AudioAction,
    hapticsEnabled: Boolean,
    onSelectAudio: (AudioAction) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onOpenPreview: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title = "Audio & Feedback")
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Surface(
                shape = segmentedListShape(0, 3),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Audio Action", style = MaterialTheme.typography.titleSmall)
                    ConnectedToggleButtonGroup(
                        options = AudioAction.entries,
                        selected = audioAction,
                        onSelect = onSelectAudio,
                        label = { it.displayName },
                        tagFor = { "engine_audio_${it.name.lowercase()}" }
                    )
                }
            }

            Surface(
                shape = segmentedListShape(1, 3),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    leadingContent = { Icon(Icons.Rounded.Vibration, contentDescription = null) },
                    headlineContent = { Text("Haptic Feedback") },
                    trailingContent = {
                        ExpressiveSwitch(
                            checked = hapticsEnabled,
                            onCheckedChange = onToggleHaptics,
                            modifier = Modifier.testTag("engine_haptics_switch")
                        )
                    }
                )
            }

            Surface(
                shape = segmentedListShape(2, 3),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    FilledTonalButton(
                        onClick = onOpenPreview,
                        shapes = ButtonDefaults.shapes(),
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
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
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

    val hasAnyOverride = scope != BlockingScope.FEED_ONLY ||
        holdOverride != null ||
        allowanceOverride != null ||
        limitOverride != null

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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PlatformAvatar(
                    platformKey = target.platformKey,
                    packageName = target.packageName,
                    sizeDp = 48,
                    active = enabled
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(target.displayName, style = MaterialTheme.typography.headlineSmallEmphasized)
                    Text(
                        target.feedName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ExpressiveSwitch(checked = enabled, onCheckedChange = { enabled = it })
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Blocking Scope", style = MaterialTheme.typography.titleSmall)
                ConnectedToggleButtonGroup(
                    options = BlockingScope.entries,
                    selected = scope,
                    onSelect = { scope = it },
                    label = { it.displayName }
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Intervention Mode", style = MaterialTheme.typography.titleSmall)
                ConnectedToggleButtonGroup(
                    options = InterventionMode.entries,
                    selected = if (strictLocked) InterventionMode.STRICT_BLOCK else mode,
                    onSelect = { mode = it },
                    enabled = !strictLocked,
                    label = { it.shortLabel }
                )
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasAnyOverride) {
                    TextButton(
                        onClick = {
                            scope = BlockingScope.FEED_ONLY
                            holdOverride = null
                            allowanceOverride = null
                            limitOverride = null
                        }
                    ) {
                        Text("Reset overrides")
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
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
                        },
                        shapes = ButtonDefaults.shapes()
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
