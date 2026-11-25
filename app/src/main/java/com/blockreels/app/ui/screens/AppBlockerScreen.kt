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
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VisibilityOff
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TonalToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blockreels.app.data.TargetAppConfig
import com.blockreels.app.state.AudioAction
import com.blockreels.app.state.BlockingScope
import com.blockreels.app.state.ContentBlockPolicy
import com.blockreels.app.state.DetectionSensitivity
import com.blockreels.app.state.InterventionMode
import com.blockreels.app.ui.components.ConnectedToggleButtonGroup
import com.blockreels.app.ui.components.ExpressiveCard
import com.blockreels.app.ui.components.ExpressiveNumberSelector
import com.blockreels.app.ui.components.ExpressiveSwitch
import com.blockreels.app.ui.components.ModeBadge
import com.blockreels.app.ui.components.OptionalExpressiveNumberSelector
import com.blockreels.app.ui.components.PlatformAvatar
import com.blockreels.app.ui.components.ScopeBadge
import com.blockreels.app.ui.components.SectionHeader
import com.blockreels.app.ui.components.segmentedListShape
import com.blockreels.app.ui.theme.MonoFontFamily
import com.blockreels.app.viewmodel.BlockReelsUiState

private enum class EngineSection(val label: String) {
    TARGETS("Apps & Goals"),
    PARAMETERS("Engine Rules")
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
    onUpdateDefaultContentPolicy: (ContentBlockPolicy) -> Unit,
    onUpdateDefaultRequestHide: (Boolean) -> Unit,
    onUpdateDefaultReelsGoal: (Int) -> Unit,
    onUpdateDefaultFeedGoal: (Int) -> Unit,
    onApplyPolicyAndGoalsToAll: (
        policy: ContentBlockPolicy,
        requestHide: Boolean,
        reelsGoalMinutes: Int,
        feedGoalMinutes: Int
    ) -> Unit,
    onUpdateGlobalMode: (InterventionMode) -> Unit,
    onUpdateHoldDuration: (Int) -> Unit,
    onUpdateAllowanceMinutes: (Int) -> Unit,
    onUpdateDailyPassLimit: (Int) -> Unit,
    onUpdateEntryDelaySeconds: (Int) -> Unit,
    onUpdateSensitivity: (DetectionSensitivity) -> Unit,
    onUpdateAudioAction: (AudioAction) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onOpenBlockMessageScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    var section by remember { mutableStateOf(EngineSection.TARGETS) }
    var filter by remember { mutableStateOf(TargetFilter.ALL) }
    var editingTarget by remember { mutableStateOf<TargetAppConfig?>(null) }

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
                        label = "Policy",
                        value = uiState.settings.defaultContentPolicy.shortLabel,
                        container = MaterialTheme.colorScheme.secondaryContainer,
                        content = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    EngineStatPill(
                        label = "Action",
                        value = activeMode.shortLabel,
                        container = MaterialTheme.colorScheme.surfaceContainerLow,
                        content = MaterialTheme.colorScheme.onSurface,
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
                // Universal Blocking Policy & Daily Consumption Goals Card
                item(key = "universal_policy_and_goals") {
                    UniversalBlockingAndGoalsCard(
                        policy = uiState.settings.defaultContentPolicy,
                        requestHide = uiState.settings.defaultRequestHideSurfaces,
                        reelsGoalMinutes = uiState.settings.defaultReelsGoalMinutes,
                        feedGoalMinutes = uiState.settings.defaultFeedGoalMinutes,
                        totalAppsCount = uiState.targets.size,
                        onSelectPolicy = onUpdateDefaultContentPolicy,
                        onToggleRequestHide = onUpdateDefaultRequestHide,
                        onChangeReelsGoal = onUpdateDefaultReelsGoal,
                        onChangeFeedGoal = onUpdateDefaultFeedGoal,
                        onApplyToAll = {
                            onApplyPolicyAndGoalsToAll(
                                uiState.settings.defaultContentPolicy,
                                uiState.settings.defaultRequestHideSurfaces,
                                uiState.settings.defaultReelsGoalMinutes,
                                uiState.settings.defaultFeedGoalMinutes
                            )
                        }
                    )
                }

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
                            onClick = onOpenBlockMessageScreen,
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
                            Text("Message")
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
                        customHeadline = uiState.settings.customMessageHeadline,
                        customMessage = uiState.settings.customMessage,
                        onChangeHold = onUpdateHoldDuration,
                        onChangeAllowance = onUpdateAllowanceMinutes,
                        onChangeDailyLimit = onUpdateDailyPassLimit,
                        onOpenBlockMessageScreen = onOpenBlockMessageScreen
                    )
                }

                item(key = "audio_haptics_section") {
                    EngineFeedbackSection(
                        audioAction = uiState.settings.audioAction,
                        hapticsEnabled = uiState.settings.hapticsEnabled,
                        onSelectAudio = onUpdateAudioAction,
                        onToggleHaptics = onToggleHaptics,
                        onOpenPreview = onOpenBlockMessageScreen
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
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
private fun UniversalBlockingAndGoalsCard(
    policy: ContentBlockPolicy,
    requestHide: Boolean,
    reelsGoalMinutes: Int,
    feedGoalMinutes: Int,
    totalAppsCount: Int,
    onSelectPolicy: (ContentBlockPolicy) -> Unit,
    onToggleRequestHide: (Boolean) -> Unit,
    onChangeReelsGoal: (Int) -> Unit,
    onChangeFeedGoal: (Int) -> Unit,
    onApplyToAll: () -> Unit
) {
    ExpressiveCard(
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "What to Block & Daily Goals",
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = policy.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text(
                        text = policy.shortLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // Block Reels / Block Feed / Block Both / Don't Show
            ConnectedToggleButtonGroup(
                options = ContentBlockPolicy.entries,
                selected = policy,
                onSelect = onSelectPolicy,
                label = { it.shortLabel },
                tagFor = { "policy_${it.name.lowercase()}" }
            )

            // Request not to see them toggle
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Rounded.VisibilityOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    headlineContent = {
                        Text(
                            text = "Request not to see them",
                            style = MaterialTheme.typography.titleSmall
                        )
                    },
                    supportingContent = {
                        Text(
                            text = "Auto-dismiss feed and reels surfaces immediately",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        ExpressiveSwitch(
                            checked = requestHide || policy == ContentBlockPolicy.HIDE_SURFACES,
                            onCheckedChange = onToggleRequestHide,
                            modifier = Modifier.testTag("policy_request_hide_switch")
                        )
                    }
                )
            }

            // Daily Consumption Goal: Reels / Shorts (Quick presets + Custom 0..180m)
            ExpressiveNumberSelector(
                title = "Reels / Shorts daily goal",
                value = reelsGoalMinutes,
                range = 0..180,
                step = 5,
                quickPresets = listOf(0, 5, 10, 15, 30, 45, 60),
                unitPluralLabel = "min",
                valueFormatter = { if (it == 0) "0m (Block)" else "${it}m / day" },
                presetLabel = { if (it == 0) "0m" else "${it}m" },
                leadingIcon = Icons.Rounded.Flag,
                accentSecondary = false,
                tagPrefix = "goal_reels",
                onValueChange = onChangeReelsGoal
            )

            // Daily Consumption Goal: Main Feed (Quick presets + Custom 0..180m)
            ExpressiveNumberSelector(
                title = "Main Feed daily goal",
                value = feedGoalMinutes,
                range = 0..180,
                step = 5,
                quickPresets = listOf(0, 10, 20, 30, 45, 60, 90),
                unitPluralLabel = "min",
                valueFormatter = { if (it == 0) "0m (Block)" else "${it}m / day" },
                presetLabel = { if (it == 0) "0m" else "${it}m" },
                leadingIcon = Icons.Rounded.Flag,
                accentSecondary = true,
                tagPrefix = "goal_feed",
                onValueChange = onChangeFeedGoal
            )

            Button(
                onClick = onApplyToAll,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("apply_policy_all_apps_btn")
            ) {
                Icon(
                    imageVector = Icons.Rounded.DoneAll,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Apply to All $totalAppsCount Supported Apps")
            }
        }
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

@OptIn(ExperimentalLayoutApi::class)
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
                        text = "${target.feedName} · Goals: Reels ${target.reelsDailyGoalMinutes}m · Feed ${target.feedDailyGoalMinutes}m",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                    FlowRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ) {
                            Text(
                                text = target.contentPolicy.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                        ModeBadge(mode = effectiveMode, compact = true)
                        if (target.scope == BlockingScope.ENTIRE_APP) {
                            ScopeBadge(scope = target.scope)
                        }
                        if (effectiveMode == InterventionMode.MINDFUL_PAUSE) {
                            val spec = buildString {
                                append("${effectiveHold}s / ${effectiveAllowance}m")
                                if (effectiveLimit > 0) append(" · $passesUsedToday/$effectiveLimit")
                            }
                            Text(
                                text = spec,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
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
                Box(modifier = Modifier.padding(16.dp)) {
                    ExpressiveNumberSelector(
                        title = "Entry Grace Delay",
                        value = entryDelaySeconds,
                        range = 0..10,
                        step = 1,
                        quickPresets = listOf(0, 2, 5, 10),
                        unitPluralLabel = "sec",
                        valueFormatter = { if (it == 0) "Instant" else "${it}s" },
                        presetLabel = { if (it == 0) "Instant" else "${it}s" },
                        tagPrefix = "engine_delay",
                        onValueChange = onSelectEntryDelay
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
    customHeadline: String,
    customMessage: String,
    onChangeHold: (Int) -> Unit,
    onChangeAllowance: (Int) -> Unit,
    onChangeDailyLimit: (Int) -> Unit,
    onOpenBlockMessageScreen: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title = "Mindful Pause & Quotas")
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Surface(
                shape = segmentedListShape(0, 4),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    ExpressiveNumberSelector(
                        title = "Hold Duration",
                        value = holdDurationSeconds,
                        range = 3..30,
                        step = 1,
                        quickPresets = listOf(3, 5, 10, 15, 20, 30),
                        unitPluralLabel = "sec",
                        valueFormatter = { "${it}s" },
                        presetLabel = { "${it}s" },
                        tagPrefix = "engine_hold",
                        onValueChange = onChangeHold
                    )
                }
            }

            Surface(
                shape = segmentedListShape(1, 4),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    ExpressiveNumberSelector(
                        title = "Pass Duration",
                        value = allowanceMinutes,
                        range = 1..30,
                        step = 1,
                        quickPresets = listOf(1, 2, 5, 10, 15, 30),
                        unitPluralLabel = "min",
                        valueFormatter = { "${it}m" },
                        presetLabel = { "${it}m" },
                        accentSecondary = true,
                        tagPrefix = "engine_pass",
                        onValueChange = onChangeAllowance
                    )
                }
            }

            Surface(
                shape = segmentedListShape(2, 4),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    ExpressiveNumberSelector(
                        title = "Daily Pass Cap",
                        value = dailyPassLimit,
                        range = 0..10,
                        step = 1,
                        quickPresets = listOf(0, 1, 2, 3, 5, 10),
                        unitPluralLabel = "passes",
                        valueFormatter = { if (it == 0) "Unlimited" else "$it / day" },
                        presetLabel = { if (it == 0) "Off" else "$it" },
                        tagPrefix = "engine_quota",
                        onValueChange = onChangeDailyLimit
                    )
                }
            }

            Surface(
                onClick = onOpenBlockMessageScreen,
                shape = segmentedListShape(3, 4),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("engine_custom_message_row")
            ) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    leadingContent = { Icon(Icons.Rounded.EditNote, contentDescription = null) },
                    headlineContent = { Text("Block Message Screen") },
                    supportingContent = {
                        Text(
                            text = "$customHeadline — $customMessage",
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
                        Text("Open Block Message Screen")
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
    var contentPolicy by remember { mutableStateOf(target.contentPolicy) }
    var requestHide by remember { mutableStateOf(target.requestHideSurfaces) }
    var reelsGoal by remember { mutableIntStateOf(target.reelsDailyGoalMinutes) }
    var feedGoal by remember { mutableIntStateOf(target.feedDailyGoalMinutes) }
    var scope by remember { mutableStateOf(target.scope) }
    var mode by remember { mutableStateOf(target.mode) }
    var holdOverride by remember { mutableStateOf(target.holdDurationOverride) }
    var allowanceOverride by remember { mutableStateOf(target.allowanceMinutesOverride) }
    var limitOverride by remember { mutableStateOf(target.dailyPassLimitOverride) }

    val hasAnyOverride = scope != BlockingScope.FEED_ONLY ||
        contentPolicy != ContentBlockPolicy.REELS_ONLY ||
        requestHide ||
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
                Text("What to Block", style = MaterialTheme.typography.titleSmall)
                ConnectedToggleButtonGroup(
                    options = ContentBlockPolicy.entries,
                    selected = contentPolicy,
                    onSelect = {
                        contentPolicy = it
                        if (it == ContentBlockPolicy.HIDE_SURFACES) requestHide = true
                    },
                    label = { it.shortLabel },
                    tagFor = { "sheet_policy_${it.name.lowercase()}" }
                )
            }

            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text("Request not to see them", style = MaterialTheme.typography.titleSmall) },
                    supportingContent = { Text("Immediately hide and dismiss detected feeds") },
                    trailingContent = {
                        ExpressiveSwitch(
                            checked = requestHide,
                            onCheckedChange = { requestHide = it }
                        )
                    }
                )
            }

            ExpressiveNumberSelector(
                title = "Reels Daily Goal",
                value = reelsGoal,
                range = 0..180,
                step = 5,
                quickPresets = listOf(0, 10, 15, 30, 45, 60),
                unitPluralLabel = "min",
                valueFormatter = { if (it == 0) "0m (Block)" else "${it}m / day" },
                presetLabel = { if (it == 0) "0m" else "${it}m" },
                tagPrefix = "sheet_reels_goal",
                onValueChange = { reelsGoal = it }
            )

            ExpressiveNumberSelector(
                title = "Main Feed Daily Goal",
                value = feedGoal,
                range = 0..180,
                step = 5,
                quickPresets = listOf(0, 15, 20, 30, 45, 60),
                unitPluralLabel = "min",
                valueFormatter = { if (it == 0) "0m (Block)" else "${it}m / day" },
                presetLabel = { if (it == 0) "0m" else "${it}m" },
                accentSecondary = true,
                tagPrefix = "sheet_feed_goal",
                onValueChange = { feedGoal = it }
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("App Scope", style = MaterialTheme.typography.titleSmall)
                ConnectedToggleButtonGroup(
                    options = BlockingScope.entries,
                    selected = scope,
                    onSelect = { scope = it },
                    label = { it.displayName },
                    tagFor = { "sheet_scope_${it.name.lowercase()}" }
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Intervention Mode", style = MaterialTheme.typography.titleSmall)
                ConnectedToggleButtonGroup(
                    options = InterventionMode.entries,
                    selected = if (strictLocked) InterventionMode.STRICT_BLOCK else mode,
                    onSelect = { mode = it },
                    enabled = !strictLocked,
                    label = { it.shortLabel },
                    tagFor = { "sheet_mode_${it.name.lowercase()}" }
                )
            }

            if (!strictLocked && mode == InterventionMode.MINDFUL_PAUSE) {
                OptionalExpressiveNumberSelector(
                    title = "Hold Override",
                    value = holdOverride,
                    defaultValue = globalHoldSeconds,
                    range = 3..30,
                    step = 1,
                    quickPresets = listOf(3, 5, 10, 15, 20),
                    unitPluralLabel = "sec",
                    valueFormatter = { "${it}s" },
                    presetLabel = { "${it}s" },
                    tagPrefix = "sheet_hold_override",
                    onValueChange = { holdOverride = it }
                )

                OptionalExpressiveNumberSelector(
                    title = "Pass Length Override",
                    value = allowanceOverride,
                    defaultValue = globalAllowanceMinutes,
                    range = 1..30,
                    step = 1,
                    quickPresets = listOf(1, 3, 5, 10, 15),
                    unitPluralLabel = "min",
                    valueFormatter = { "${it}m" },
                    presetLabel = { "${it}m" },
                    tagPrefix = "sheet_pass_override",
                    onValueChange = { allowanceOverride = it }
                )

                OptionalExpressiveNumberSelector(
                    title = "Daily Pass Cap Override",
                    value = limitOverride,
                    defaultValue = globalDailyLimit,
                    range = 0..10,
                    step = 1,
                    quickPresets = listOf(0, 1, 2, 3, 5),
                    unitPluralLabel = "passes",
                    valueFormatter = { if (it == 0) "Off" else "$it / day" },
                    presetLabel = { if (it == 0) "Off" else "$it" },
                    tagPrefix = "sheet_limit_override",
                    onValueChange = { limitOverride = it }
                )
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
                            contentPolicy = ContentBlockPolicy.REELS_ONLY
                            requestHide = false
                            holdOverride = null
                            allowanceOverride = null
                            limitOverride = null
                        },
                        modifier = Modifier.testTag("sheet_reset_overrides_btn")
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
                                    contentPolicy = contentPolicy,
                                    requestHideSurfaces = requestHide,
                                    reelsDailyGoalMinutes = reelsGoal,
                                    feedDailyGoalMinutes = feedGoal,
                                    scope = scope,
                                    mode = mode,
                                    holdDurationOverride = holdOverride,
                                    allowanceMinutesOverride = allowanceOverride,
                                    dailyPassLimitOverride = limitOverride
                                )
                            )
                        },
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.testTag("sheet_save_rule_btn")
                    ) {
                        Text("Save Rule")
                    }
                }
            }
        }
    }
}
