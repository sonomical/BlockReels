// SPDX-License-Identifier: MIT
package com.blockreels.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.TimerOff
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blockreels.app.data.ActiveAllowanceItem
import com.blockreels.app.data.HistoryRecord
import com.blockreels.app.data.ProtectionStatus
import com.blockreels.app.data.ScheduleItem
import com.blockreels.app.data.TargetAppConfig
import com.blockreels.app.state.BlockingScope
import com.blockreels.app.state.InterventionMode
import com.blockreels.app.ui.components.AccessibilityDisclosureDialog
import com.blockreels.app.ui.components.ModeBadge
import com.blockreels.app.ui.components.PlatformAvatar
import com.blockreels.app.ui.components.SectionHeader
import com.blockreels.app.ui.components.TonalCard
import com.blockreels.app.ui.components.formatDurationMinutes
import com.blockreels.app.ui.components.formatRelativeTime
import com.blockreels.app.ui.theme.MonoFontFamily
import com.blockreels.app.viewmodel.BlockReelsUiState

@Composable
fun DashboardScreen(
    uiState: BlockReelsUiState,
    bottomContentPadding: Dp = 96.dp,
    onToggleMaster: (Boolean) -> Unit,
    onToggleTargetApp: (String, Boolean) -> Unit,
    onStartQuickBoost: (Int, InterventionMode) -> Unit,
    onCancelQuickBoost: () -> Unit,
    onRevokeAllowance: (String) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onNavigateToEngine: () -> Unit,
    onNavigateToStats: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDisclosure by remember { mutableStateOf(false) }

    if (showDisclosure) {
        AccessibilityDisclosureDialog(
            onDismiss = { showDisclosure = false },
            onContinueToSettings = {
                showDisclosure = false
                onOpenAccessibilitySettings()
            }
        )
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomContentPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "hero_status") {
                ProtectionHeroCard(
                    uiState = uiState,
                    onToggleMaster = onToggleMaster,
                    onEnableAccessibilityClick = { showDisclosure = true },
                    onTuneEngineClick = onNavigateToEngine
                )
            }

            if (uiState.activeAllowances.isNotEmpty()) {
                item(key = "active_passes") {
                    ActivePassesStrip(
                        allowances = uiState.activeAllowances,
                        onRevoke = onRevokeAllowance
                    )
                }
            }

            item(key = "kpi_row") {
                KpiMetricsRow(
                    blocksToday = uiState.analytics.blocksToday,
                    minutesSavedToday = uiState.analytics.minutesSavedToday,
                    streakDays = uiState.analytics.streakDays
                )
            }

            item(key = "quick_boost") {
                QuickLockSection(
                    activeBoost = uiState.activeQuickBoost,
                    nowMillis = uiState.nowMillis,
                    onStartBoost = onStartQuickBoost,
                    onCancelBoost = onCancelQuickBoost
                )
            }

            item(key = "quick_toggles") {
                GuardedTargetsGrid(
                    targets = uiState.targets.take(6),
                    onToggleTarget = onToggleTargetApp,
                    onConfigureClick = onNavigateToEngine
                )
            }

            item(key = "recent_header") {
                SectionHeader(
                    title = "Recent Activity",
                    trailing = {
                        if (uiState.history.isNotEmpty()) {
                            TextButton(
                                onClick = onNavigateToStats,
                                modifier = Modifier.testTag("dashboard_view_all_stats_btn")
                            ) {
                                Text("All")
                            }
                        }
                    }
                )
            }

            if (uiState.history.isEmpty()) {
                item(key = "empty_recent") {
                    TonalCard {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.VerifiedUser,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "No interceptions recorded yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                item(key = "recent_list_card") {
                    TonalCard {
                        val recent = uiState.history.take(5)
                        recent.forEachIndexed { index, record ->
                            RecentInterceptionRow(
                                record = record,
                                nowMillis = uiState.nowMillis
                            )
                            if (index < recent.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                                )
                            }
                        }
                    }
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ProtectionHeroCard(
    uiState: BlockReelsUiState,
    onToggleMaster: (Boolean) -> Unit,
    onEnableAccessibilityClick: () -> Unit,
    onTuneEngineClick: () -> Unit
) {
    val status = uiState.protectionStatus
    val settings = uiState.settings

    val containerColor by animateColorAsState(
        targetValue = when (status) {
            ProtectionStatus.PROTECTED -> MaterialTheme.colorScheme.primaryContainer
            ProtectionStatus.PAUSED -> MaterialTheme.colorScheme.surfaceContainerHigh
            ProtectionStatus.PERMISSION_NEEDED -> MaterialTheme.colorScheme.tertiaryContainer
        },
        label = "hero_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = when (status) {
            ProtectionStatus.PROTECTED -> MaterialTheme.colorScheme.onPrimaryContainer
            ProtectionStatus.PAUSED -> MaterialTheme.colorScheme.onSurface
            ProtectionStatus.PERMISSION_NEEDED -> MaterialTheme.colorScheme.onTertiaryContainer
        },
        label = "hero_fg"
    )

    val pulseScale = if (status == ProtectionStatus.PROTECTED) {
        val infiniteTransition = rememberInfiniteTransition(label = "shield_pulse")
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.06f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        ).value
    } else 1f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_protection_hero_card"),
        shape = MaterialTheme.shapes.extraLarge,
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                        }
                        .clip(CircleShape)
                        .background(contentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (status) {
                            ProtectionStatus.PROTECTED -> Icons.Rounded.VerifiedUser
                            ProtectionStatus.PAUSED -> Icons.Rounded.PauseCircle
                            ProtectionStatus.PERMISSION_NEEDED -> Icons.Rounded.WarningAmber
                        },
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (status) {
                            ProtectionStatus.PROTECTED -> if (settings.strictMode) "Strict Lock Active" else "Shield Active"
                            ProtectionStatus.PAUSED -> "Shield Paused"
                            ProtectionStatus.PERMISSION_NEEDED -> "Accessibility Required"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        color = contentColor
                    )
                    Text(
                        text = when (status) {
                            ProtectionStatus.PROTECTED -> "${uiState.enabledTargetsCount} targets guarded"
                            ProtectionStatus.PAUSED -> "Blocking disabled"
                            ProtectionStatus.PERMISSION_NEEDED -> "Enable service to start"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.8f)
                    )
                }

                Switch(
                    checked = settings.masterEnabled,
                    onCheckedChange = onToggleMaster,
                    modifier = Modifier.testTag("dashboard_master_switch")
                )
            }

            if (status == ProtectionStatus.PERMISSION_NEEDED) {
                Button(
                    onClick = onEnableAccessibilityClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_enable_accessibility_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enable Accessibility")
                }
            } else {
                val activeMode = if (settings.strictMode) InterventionMode.STRICT_BLOCK else settings.globalMode
                val quotaLabel = if (settings.dailyPassLimit > 0) "${settings.dailyPassLimit}/day cap" else "No pass cap"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(contentColor.copy(alpha = 0.08f))
                        .clickable(onClick = onTuneEngineClick)
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${activeMode.displayName} · ${settings.sensitivity.displayName} · $quotaLabel",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                        color = contentColor.copy(alpha = 0.92f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tune",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivePassesStrip(
    allowances: List<ActiveAllowanceItem>,
    onRevoke: (String) -> Unit
) {
    TonalCard(
        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
        borderColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Active Passes",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            allowances.forEach { pass ->
                val remMin = (pass.remainingMillis / 60_000L).coerceAtLeast(1L)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PlatformAvatar(platformKey = pass.platformKey, sizeDp = 32)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = pass.appLabel,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${remMin}m left",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { onRevoke(pass.packageName) }) {
                        Icon(
                            imageVector = Icons.Rounded.TimerOff,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("End")
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiMetricsRow(
    blocksToday: Int,
    minutesSavedToday: Int,
    streakDays: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        KpiTile(
            label = "Blocked",
            value = blocksToday.toString(),
            icon = Icons.Rounded.Shield,
            iconTint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        KpiTile(
            label = "Saved",
            value = formatDurationMinutes(minutesSavedToday),
            icon = Icons.Rounded.Schedule,
            iconTint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f)
        )
        KpiTile(
            label = "Streak",
            value = "${streakDays}d",
            icon = Icons.Rounded.LocalFireDepartment,
            iconTint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun KpiTile(
    label: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = MonoFontFamily),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun QuickLockSection(
    activeBoost: ScheduleItem?,
    nowMillis: Long,
    onStartBoost: (Int, InterventionMode) -> Unit,
    onCancelBoost: () -> Unit
) {
    var selectedBoostMode by remember { mutableStateOf(InterventionMode.STRICT_BLOCK) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(
            title = "Focus Lock",
            trailing = if (activeBoost == null) {
                {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            InterventionMode.STRICT_BLOCK to "Strict",
                            InterventionMode.REDIRECT_HOME to "Home",
                            InterventionMode.MINDFUL_PAUSE to "Mindful"
                        ).forEach { (mode, label) ->
                            val active = selectedBoostMode == mode
                            Surface(
                                shape = CircleShape,
                                color = if (active) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { selectedBoostMode = mode }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                    color = if (active) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            } else null
        )

        if (activeBoost != null) {
            val remainingMin = (((activeBoost.expiresAtMillis ?: nowMillis) - nowMillis) / 60_000L)
                .coerceAtLeast(1L)
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Bolt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeBoost.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "${remainingMin}m remaining · ${(activeBoost.modeOverride ?: InterventionMode.STRICT_BLOCK).displayName}",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFontFamily),
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                        )
                    }
                    IconButton(
                        onClick = onCancelBoost,
                        modifier = Modifier.testTag("dashboard_cancel_boost_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "End Focus Lock",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        } else {
            val presets = listOf(15 to "15m", 30 to "30m", 60 to "1h", 120 to "2h")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { (minutes, label) ->
                    FilterChip(
                        selected = false,
                        onClick = { onStartBoost(minutes, selectedBoostMode) },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoFontFamily),
                                maxLines = 1
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Bolt,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_boost_${minutes}_chip")
                    )
                }
            }
        }
    }
}

@Composable
private fun GuardedTargetsGrid(
    targets: List<TargetAppConfig>,
    onToggleTarget: (String, Boolean) -> Unit,
    onConfigureClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(
            title = "Targets",
            trailing = {
                TextButton(
                    onClick = onConfigureClick,
                    modifier = Modifier.testTag("dashboard_manage_apps_btn")
                ) {
                    Text("Engine")
                }
            }
        )

        targets.chunked(2).forEach { rowTargets ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowTargets.forEach { target ->
                    val enabled = target.isEnabled
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = if (enabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        else MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.large)
                            .clickable { onToggleTarget(target.packageName, !enabled) }
                            .testTag("quick_toggle_${target.packageName}")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PlatformAvatar(platformKey = target.platformKey, sizeDp = 36)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = target.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (target.scope == BlockingScope.ENTIRE_APP) "Full App" else target.feedName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (enabled) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant
                                    )
                            )
                        }
                    }
                }
                if (rowTargets.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun RecentInterceptionRow(
    record: HistoryRecord,
    nowMillis: Long
) {
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = {
            PlatformAvatar(platformKey = record.platformKey, sizeDp = 38)
        },
        headlineContent = {
            Text(
                text = record.appLabel,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = formatRelativeTime(record.timestampMillis, nowMillis),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            ModeBadge(mode = record.interventionMode, compact = true)
        }
    )
}
