package com.blockreels.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blockreels.app.data.TargetAppConfig
import com.blockreels.app.state.InterventionMode
import com.blockreels.app.ui.components.PlatformAvatar
import com.blockreels.app.viewmodel.BlockReelsUiState

private enum class TargetFilter(val label: String) {
    ALL("All Platforms"),
    ACTIVE("Active Only"),
    INSTALLED("Installed")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBlockerScreen(
    uiState: BlockReelsUiState,
    bottomContentPadding: Dp = 96.dp,
    onToggleTarget: (String, Boolean) -> Unit,
    onChangeTargetMode: (String, InterventionMode) -> Unit,
    onOpenPreviewOverlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    var filter by remember { mutableStateOf(TargetFilter.ALL) }

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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = bottomContentPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "header_banner") {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Selective Feed Detection",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = if (uiState.settings.strictMode) {
                                    "Strict Mode is ON: all enabled platforms use immediate Strict Block."
                                } else {
                                    "Choose an intervention mode per app. Messages, search, and long-form videos stay open."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                            )
                        }
                        FilledTonalButton(
                            onClick = onOpenPreviewOverlay,
                            modifier = Modifier.testTag("apps_preview_overlay_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayCircleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Preview")
                        }
                    }
                }
            }

            item(key = "filter_chips") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TargetFilter.entries.forEach { option ->
                        FilterChip(
                            selected = filter == option,
                            onClick = { filter = option },
                            label = { Text(option.label) },
                            modifier = Modifier.testTag("apps_filter_${option.name.lowercase()}")
                        )
                    }
                }
            }

            if (filteredTargets.isEmpty()) {
                item(key = "empty_filter") {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "No matching apps",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Try switching back to All Platforms to configure rules ahead of time.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredTargets, key = { it.packageName }) { target ->
                    AppTargetRuleCard(
                        target = target,
                        strictModeOverride = uiState.settings.strictMode,
                        holdDurationSeconds = uiState.settings.holdDurationSeconds,
                        onToggle = { enabled -> onToggleTarget(target.packageName, enabled) },
                        onModeChange = { mode -> onChangeTargetMode(target.packageName, mode) }
                    )
                }
            }

            item(key = "bottom_space") {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTargetRuleCard(
    target: TargetAppConfig,
    strictModeOverride: Boolean,
    holdDurationSeconds: Int,
    onToggle: (Boolean) -> Unit,
    onModeChange: (InterventionMode) -> Unit
) {
    val effectiveMode = if (strictModeOverride) InterventionMode.STRICT_BLOCK else target.mode

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("app_target_card_${target.packageName}"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (target.isEnabled) {
                MaterialTheme.colorScheme.surfaceContainerLow
            } else {
                MaterialTheme.colorScheme.surfaceContainerLowest
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PlatformAvatar(platformKey = target.platformKey, sizeDp = 46)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = target.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (target.isInstalled) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                            ) {
                                Text(
                                    text = "Installed",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "Blocks ${target.feedName} · ${target.packageName}",
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Intervention Mode",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (strictModeOverride) {
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
                                    text = "Locked by Strict Mode",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    val modes = listOf(
                        InterventionMode.AUTO_BACK to "Auto Back",
                        InterventionMode.MINDFUL_PAUSE to "Mindful (${holdDurationSeconds}s)",
                        InterventionMode.STRICT_BLOCK to "Strict"
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        modes.forEachIndexed { idx, (mode, label) ->
                            SegmentedButton(
                                selected = effectiveMode == mode,
                                enabled = !strictModeOverride,
                                onClick = { onModeChange(mode) },
                                shape = SegmentedButtonDefaults.itemShape(index = idx, count = modes.size),
                                modifier = Modifier.testTag("mode_${target.packageName}_${mode.name.lowercase()}")
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
