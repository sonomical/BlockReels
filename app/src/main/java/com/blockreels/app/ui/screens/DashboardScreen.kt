// SPDX-License-Identifier: MIT
package com.blockreels.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.TimerOff
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.ViewStream
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TonalToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blockreels.app.data.ActiveAllowanceItem
import com.blockreels.app.data.DayCountPoint
import com.blockreels.app.data.HistoryRecord
import com.blockreels.app.data.PlatformBreakdownItem
import com.blockreels.app.data.ProtectionStatus
import com.blockreels.app.data.ScheduleItem
import com.blockreels.app.data.SupportedTargets
import com.blockreels.app.data.SystemHealthState
import com.blockreels.app.data.TargetAppConfig
import com.blockreels.app.state.BlockingScope
import com.blockreels.app.state.ContentBlockPolicy
import com.blockreels.app.state.InterventionMode
import com.blockreels.app.ui.components.AccessibilityDisclosureDialog
import com.blockreels.app.ui.components.ConnectedToggleButtonGroup
import com.blockreels.app.ui.components.ExpressiveCard
import com.blockreels.app.ui.components.ExpressiveSwitch
import com.blockreels.app.ui.components.ModeBadge
import com.blockreels.app.ui.components.NumberInputDialog
import com.blockreels.app.ui.components.PlatformAvatar
import com.blockreels.app.ui.components.SectionHeader
import com.blockreels.app.ui.components.formatDurationMinutes
import com.blockreels.app.ui.components.formatRelativeTime
import com.blockreels.app.ui.components.segmentedListShape
import com.blockreels.app.ui.theme.MonoFontFamily
import com.blockreels.app.viewmodel.BlockReelsUiState
import java.util.Locale

@Composable
fun DashboardScreen(
    uiState: BlockReelsUiState,
    bottomContentPadding: Dp = 24.dp,
    onToggleMaster: (Boolean) -> Unit,
    onToggleTargetApp: (String, Boolean) -> Unit,
    onStartQuickBoost: (Int, InterventionMode) -> Unit,
    onCancelQuickBoost: () -> Unit,
    onRevokeAllowance: (String) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onNavigateToEngine: () -> Unit,
    onNavigateToSchedules: () -> Unit = {},
    onNavigateToStats: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Live Shield Status Hero
            item(key = "hero_status") {
                ProtectionHeroCard(
                    uiState = uiState,
                    onToggleMaster = onToggleMaster,
                    onEnableAccessibilityClick = { showDisclosure = true },
                    onTuneEngineClick = onNavigateToEngine
                )
            }

            // 2. Active Temporary Passes (contextual)
            if (uiState.activeAllowances.isNotEmpty()) {
                item(key = "active_passes") {
                    ActivePassesStrip(
                        allowances = uiState.activeAllowances,
                        onRevoke = onRevokeAllowance
                    )
                }
            }

            // 3. Comprehensive Numbers & Summaries Bento
            item(key = "summary_bento") {
                DashboardSummaryBento(
                    uiState = uiState,
                    onOpenStats = onNavigateToStats,
                    onOpenEngine = onNavigateToEngine,
                    onOpenSchedules = onNavigateToSchedules
                )
            }

            // 3b. Active Content Policy & Daily Consumption Goals Strip
            item(key = "policy_and_goals") {
                PolicyAndGoalsSummaryCard(
                    policy = uiState.settings.defaultContentPolicy,
                    requestHideSurfaces = uiState.settings.defaultRequestHideSurfaces,
                    reelsGoalMinutes = uiState.settings.defaultReelsGoalMinutes,
                    feedGoalMinutes = uiState.settings.defaultFeedGoalMinutes,
                    protectedAppsCount = uiState.enabledTargetsCount,
                    totalAppsCount = uiState.targets.size,
                    onOpenEngine = onNavigateToEngine
                )
            }

            // 4. 7-Day Activity Pulse & Peak Insights Card
            item(key = "weekly_pulse") {
                WeeklyPulseSummaryCard(
                    series = uiState.analytics.dailySeries,
                    blocksLast7Days = uiState.blocksLast7Days,
                    peakHourLabel = uiState.analytics.peakHourLabel,
                    topPlatform = uiState.topBlockedPlatform,
                    onOpenStats = onNavigateToStats
                )
            }

            // 5. Instant Focus Lock Controls
            item(key = "quick_boost") {
                QuickLockSection(
                    activeBoost = uiState.activeQuickBoost,
                    nowMillis = uiState.nowMillis,
                    onStartBoost = onStartQuickBoost,
                    onCancelBoost = onCancelQuickBoost
                )
            }

            // 6. Protected Apps Deck with Real Themed Icons
            item(key = "quick_toggles") {
                GuardedTargetsGrid(
                    targets = uiState.targets.take(6),
                    enabledCount = uiState.enabledTargetsCount,
                    totalCount = uiState.targets.size,
                    onToggleTarget = onToggleTargetApp,
                    onConfigureClick = onNavigateToEngine
                )
            }

            // 7. System Readiness Summary Strip
            item(key = "system_readiness") {
                SystemReadinessCard(
                    health = uiState.systemHealth,
                    pinProtected = uiState.settings.pinProtected,
                    onOpenAccessibility = { showDisclosure = true },
                    onOpenSettings = onNavigateToSettings
                )
            }

            // 8. Recent Interceptions Segmented Feed
            item(key = "recent_activity_section") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(
                        title = "Recent Activity",
                        trailing = {
                            if (uiState.history.isNotEmpty()) {
                                TextButton(
                                    onClick = onNavigateToStats,
                                    modifier = Modifier.testTag("dashboard_view_all_stats_btn")
                                ) {
                                    Text("View all")
                                }
                            }
                        }
                    )

                    if (uiState.history.isEmpty()) {
                        ExpressiveCard {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.VerifiedUser,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "All clear",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Interceptions appear here as soon as a short-video feed is detected.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        val recent = uiState.history.take(4)
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            recent.forEachIndexed { index, record ->
                                Surface(
                                    shape = segmentedListShape(index, recent.size),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow
                                ) {
                                    RecentInterceptionRow(
                                        record = record,
                                        nowMillis = uiState.nowMillis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ProtectionHeroCard(
    uiState: BlockReelsUiState,
    onToggleMaster: (Boolean) -> Unit,
    onEnableAccessibilityClick: () -> Unit,
    onTuneEngineClick: () -> Unit
) {
    val status = uiState.protectionStatus
    val settings = uiState.settings
    val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Color>()

    val containerColor by animateColorAsState(
        targetValue = when (status) {
            ProtectionStatus.PROTECTED -> MaterialTheme.colorScheme.primaryContainer
            ProtectionStatus.PAUSED -> MaterialTheme.colorScheme.surfaceContainerHigh
            ProtectionStatus.PERMISSION_NEEDED -> MaterialTheme.colorScheme.tertiaryContainer
        },
        animationSpec = effectsSpec,
        label = "hero_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = when (status) {
            ProtectionStatus.PROTECTED -> MaterialTheme.colorScheme.onPrimaryContainer
            ProtectionStatus.PAUSED -> MaterialTheme.colorScheme.onSurface
            ProtectionStatus.PERMISSION_NEEDED -> MaterialTheme.colorScheme.onTertiaryContainer
        },
        animationSpec = effectsSpec,
        label = "hero_fg"
    )
    val emblemContainer by animateColorAsState(
        targetValue = when (status) {
            ProtectionStatus.PROTECTED -> MaterialTheme.colorScheme.primary
            ProtectionStatus.PAUSED -> MaterialTheme.colorScheme.surfaceContainerHighest
            ProtectionStatus.PERMISSION_NEEDED -> MaterialTheme.colorScheme.tertiary
        },
        animationSpec = effectsSpec,
        label = "emblem_bg"
    )
    val emblemContent by animateColorAsState(
        targetValue = when (status) {
            ProtectionStatus.PROTECTED -> MaterialTheme.colorScheme.onPrimary
            ProtectionStatus.PAUSED -> MaterialTheme.colorScheme.onSurfaceVariant
            ProtectionStatus.PERMISSION_NEEDED -> MaterialTheme.colorScheme.onTertiary
        },
        animationSpec = effectsSpec,
        label = "emblem_fg"
    )

    val emblemShape = when (status) {
        ProtectionStatus.PROTECTED -> MaterialShapes.Cookie9Sided.toShape()
        ProtectionStatus.PAUSED -> MaterialShapes.Pill.toShape()
        ProtectionStatus.PERMISSION_NEEDED -> MaterialShapes.Slanted.toShape()
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_protection_hero_card"),
        shape = MaterialTheme.shapes.extraLarge,
        color = containerColor,
        contentColor = contentColor
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = emblemShape,
                    color = emblemContainer,
                    contentColor = emblemContent
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when (status) {
                                ProtectionStatus.PROTECTED -> Icons.Rounded.VerifiedUser
                                ProtectionStatus.PAUSED -> Icons.Rounded.PauseCircle
                                ProtectionStatus.PERMISSION_NEEDED -> Icons.Rounded.WarningAmber
                            },
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = when (status) {
                            ProtectionStatus.PROTECTED -> if (settings.strictMode) "Strict Lock Active" else "Shield Active"
                            ProtectionStatus.PAUSED -> "Shield Paused"
                            ProtectionStatus.PERMISSION_NEEDED -> "Accessibility Required"
                        },
                        style = MaterialTheme.typography.headlineSmallEmphasized,
                        color = contentColor
                    )
                    Text(
                        text = when (status) {
                            ProtectionStatus.PROTECTED -> {
                                val monitorText = if (uiState.systemHealth.monitoringServiceRunning) "Monitor running" else "Ready"
                                "${uiState.enabledTargetsCount} apps guarded · $monitorText"
                            }
                            ProtectionStatus.PAUSED -> "Protection temporarily paused"
                            ProtectionStatus.PERMISSION_NEEDED -> "Enable Accessibility Service to activate blocking"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor
                    )
                }

                ExpressiveSwitch(
                    checked = settings.masterEnabled,
                    onCheckedChange = onToggleMaster,
                    modifier = Modifier.testTag("dashboard_master_switch")
                )
            }

            if (status == ProtectionStatus.PERMISSION_NEEDED) {
                Button(
                    onClick = onEnableAccessibilityClick,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_enable_accessibility_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enable Accessibility Service")
                }
            } else {
                val activeMode = if (settings.strictMode) InterventionMode.STRICT_BLOCK else settings.globalMode
                val passSpec = if (settings.dailyPassLimit > 0) {
                    "${uiState.totalPassesUsedToday}/${settings.dailyPassLimit} passes"
                } else {
                    "${settings.holdDurationSeconds}s hold"
                }
                FilledTonalButton(
                    onClick = onTuneEngineClick,
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${activeMode.displayName} · ${settings.sensitivity.displayName} · $passSpec",
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoFontFamily),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DashboardSummaryBento(
    uiState: BlockReelsUiState,
    onOpenStats: () -> Unit,
    onOpenEngine: () -> Unit,
    onOpenSchedules: () -> Unit
) {
    val analytics = uiState.analytics
    val dailyAvgFormatted = String.format(Locale.US, "%.1f/day", analytics.averageDailyBlocks)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(
            title = "Today's Summary",
            trailing = {
                TextButton(onClick = onOpenStats) {
                    Text("Insights")
                }
            }
        )

        // Primary 2-Column Big Number Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PrimarySummaryCard(
                title = "Blocked Today",
                bigValue = analytics.blocksToday.toString(),
                footnote = "${analytics.totalBlocks} total · avg $dailyAvgFormatted",
                icon = Icons.Rounded.Shield,
                container = MaterialTheme.colorScheme.surfaceContainerLow,
                content = MaterialTheme.colorScheme.onSurface,
                badgeContainer = MaterialTheme.colorScheme.primaryContainer,
                badgeContent = MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = onOpenStats,
                modifier = Modifier.weight(1f)
            )
            PrimarySummaryCard(
                title = "Time Reclaimed",
                bigValue = formatDurationMinutes(analytics.minutesSavedToday),
                footnote = "${formatDurationMinutes(analytics.minutesSaved)} saved in range",
                icon = Icons.Rounded.Schedule,
                container = MaterialTheme.colorScheme.surfaceContainerLow,
                content = MaterialTheme.colorScheme.onSurface,
                badgeContainer = MaterialTheme.colorScheme.secondaryContainer,
                badgeContent = MaterialTheme.colorScheme.onSecondaryContainer,
                onClick = onOpenStats,
                modifier = Modifier.weight(1f)
            )
        }

        // Secondary 3-Column Operational Numbers Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CompactNumberTile(
                label = "Streak",
                value = "${analytics.streakDays}d",
                icon = Icons.Rounded.LocalFireDepartment,
                badgeContainer = MaterialTheme.colorScheme.tertiaryContainer,
                badgeContent = MaterialTheme.colorScheme.onTertiaryContainer,
                onClick = onOpenStats,
                modifier = Modifier.weight(1f)
            )
            CompactNumberTile(
                label = "Active Apps",
                value = "${uiState.enabledTargetsCount}/${uiState.targets.size}",
                icon = Icons.Rounded.Tune,
                badgeContainer = MaterialTheme.colorScheme.primaryContainer,
                badgeContent = MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = onOpenEngine,
                modifier = Modifier.weight(1f)
            )
            CompactNumberTile(
                label = "Schedules",
                value = "${uiState.activeRegularSchedulesCount} on",
                icon = Icons.Rounded.CalendarMonth,
                badgeContainer = MaterialTheme.colorScheme.secondaryContainer,
                badgeContent = MaterialTheme.colorScheme.onSecondaryContainer,
                onClick = onOpenSchedules,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PrimarySummaryCard(
    title: String,
    bigValue: String,
    footnote: String,
    icon: ImageVector,
    container: Color,
    content: Color,
    badgeContainer: Color,
    badgeContent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = container,
        contentColor = content
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
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = MaterialShapes.Cookie4Sided.toShape(),
                    color = badgeContainer,
                    contentColor = badgeContent,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Text(
                text = bigValue,
                style = MaterialTheme.typography.displaySmallEmphasized.copy(fontFamily = MonoFontFamily),
                color = content,
                maxLines = 1
            )

            Text(
                text = footnote,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CompactNumberTile(
    label: String,
    value: String,
    icon: ImageVector,
    badgeContainer: Color,
    badgeContent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.largeIncreased,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = badgeContainer,
                contentColor = badgeContent,
                modifier = Modifier.size(30.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLargeEmphasized.copy(fontFamily = MonoFontFamily),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WeeklyPulseSummaryCard(
    series: List<DayCountPoint>,
    blocksLast7Days: Int,
    peakHourLabel: String,
    topPlatform: PlatformBreakdownItem?,
    onOpenStats: () -> Unit
) {
    Surface(
        onClick = onOpenStats,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth()
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
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "7-Day Pulse",
                        style = MaterialTheme.typography.titleMediumEmphasized
                    )
                    Text(
                        text = "$blocksLast7Days interceptions in the past 7 days",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Insights,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = peakHourLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily)
                        )
                    }
                }
            }

            if (series.isNotEmpty()) {
                val maxCount = (series.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)
                val activeBarColor = MaterialTheme.colorScheme.primary
                val normalBarColor = MaterialTheme.colorScheme.secondary
                val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(74.dp)
                ) {
                    val count = series.size.coerceAtLeast(1)
                    val slotWidth = size.width / count
                    val barWidth = (slotWidth * 0.44f).coerceAtMost(24.dp.toPx())
                    val radius = CornerRadius(barWidth / 2f, barWidth / 2f)

                    series.forEachIndexed { index, point ->
                        val left = index * slotWidth + (slotWidth - barWidth) / 2f
                        drawRoundRect(
                            color = trackColor,
                            topLeft = Offset(left, 0f),
                            size = Size(barWidth, size.height),
                            cornerRadius = radius
                        )
                        val fraction = (point.count.toFloat() / maxCount.toFloat()).coerceIn(0f, 1f)
                        val barHeight = if (point.count > 0) {
                            (size.height * fraction).coerceAtLeast(14.dp.toPx())
                        } else 0f
                        if (barHeight > 0f) {
                            drawRoundRect(
                                color = if (point.isToday) activeBarColor else normalBarColor,
                                topLeft = Offset(left, size.height - barHeight),
                                size = Size(barWidth, barHeight),
                                cornerRadius = radius
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    series.forEach { point ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = point.dayLabel.take(2),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (point.isToday) FontWeight.Bold else FontWeight.Normal,
                                color = if (point.isToday) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Bottom Top-Source Insight Strip
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (topPlatform != null) {
                        PlatformAvatar(
                            platformKey = topPlatform.platformKey,
                            packageName = SupportedTargets.primaryPackageForPlatform(topPlatform.platformKey),
                            sizeDp = 32
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Top blocked source",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = topPlatform.label,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(
                                text = "${topPlatform.count} (${(topPlatform.fraction * 100).toInt()}%)",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Insights,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Peak window and top source populate automatically as blocks occur.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ActivePassesStrip(
    allowances: List<ActiveAllowanceItem>,
    onRevoke: (String) -> Unit
) {
    ExpressiveCard(
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Active Passes",
                style = MaterialTheme.typography.labelLargeEmphasized,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            allowances.forEach { pass ->
                val remMin = (pass.remainingMillis / 60_000L).coerceAtLeast(1L)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PlatformAvatar(
                        platformKey = pass.platformKey,
                        packageName = pass.packageName,
                        sizeDp = 36
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = pass.appLabel,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "${remMin}m remaining",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    FilledTonalButton(
                        onClick = { onRevoke(pass.packageName) },
                        shapes = ButtonDefaults.shapes(),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.TimerOff,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Revoke")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun QuickLockSection(
    activeBoost: ScheduleItem?,
    nowMillis: Long,
    onStartBoost: (Int, InterventionMode) -> Unit,
    onCancelBoost: () -> Unit
) {
    var selectedBoostMode by remember { mutableStateOf(InterventionMode.STRICT_BLOCK) }
    val boostModes = remember {
        listOf(
            InterventionMode.STRICT_BLOCK,
            InterventionMode.REDIRECT_HOME,
            InterventionMode.MINDFUL_PAUSE
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title = "Quick Focus Lock")

        if (activeBoost != null) {
            val remainingMin = (((activeBoost.expiresAtMillis ?: nowMillis) - nowMillis) / 60_000L)
                .coerceAtLeast(1L)
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Bolt,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeBoost.title,
                            style = MaterialTheme.typography.titleMediumEmphasized,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "${remainingMin}m remaining · ${(activeBoost.modeOverride ?: InterventionMode.STRICT_BLOCK).displayName}",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFontFamily),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    FilledTonalIconButton(
                        onClick = onCancelBoost,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.testTag("dashboard_cancel_boost_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "End Focus Lock"
                        )
                    }
                }
            }
        } else {
            ExpressiveCard(shape = MaterialTheme.shapes.extraLarge) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ConnectedToggleButtonGroup(
                        options = boostModes,
                        selected = selectedBoostMode,
                        onSelect = { selectedBoostMode = it },
                        label = { mode ->
                            when (mode) {
                                InterventionMode.STRICT_BLOCK -> "Strict"
                                InterventionMode.REDIRECT_HOME -> "Home"
                                else -> "Mindful"
                            }
                        }
                    )

                    var showCustomDurationDialog by remember { mutableStateOf(false) }

                    val presets = listOf(15 to "15m", 30 to "30m", 60 to "1h", 120 to "2h")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                    ) {
                        presets.forEach { (minutes, label) ->
                            FilledTonalButton(
                                onClick = { onStartBoost(minutes, selectedBoostMode) },
                                shapes = ButtonDefaults.shapes(),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quick_boost_${minutes}_chip")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Bolt,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoFontFamily),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = { showCustomDurationDialog = true },
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quick_boost_custom_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Custom Lock Duration",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                            Text(
                                text = "5–480 min",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (showCustomDurationDialog) {
                        NumberInputDialog(
                            title = "Focus Lock Duration",
                            initialValue = 45,
                            range = 5..480,
                            unitPluralLabel = "minutes",
                            quickPresets = listOf(10, 20, 45, 90, 180, 240),
                            presetLabel = { mins ->
                                if (mins >= 60 && mins % 60 == 0) "${mins / 60}h" else "${mins}m"
                            },
                            confirmLabel = "Start Lock",
                            onDismiss = { showCustomDurationDialog = false },
                            onConfirm = { picked ->
                                showCustomDurationDialog = false
                                onStartBoost(picked, selectedBoostMode)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GuardedTargetsGrid(
    targets: List<TargetAppConfig>,
    enabledCount: Int,
    totalCount: Int,
    onToggleTarget: (String, Boolean) -> Unit,
    onConfigureClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(
            title = "Protected Apps ($enabledCount/$totalCount)",
            trailing = {
                TextButton(
                    onClick = onConfigureClick,
                    modifier = Modifier.testTag("dashboard_manage_apps_btn")
                ) {
                    Text("Manage")
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
                    TonalToggleButton(
                        checked = enabled,
                        onCheckedChange = { onToggleTarget(target.packageName, it) },
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
                        contentPadding = PaddingValues(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_toggle_${target.packageName}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PlatformAvatar(
                                platformKey = target.platformKey,
                                packageName = target.packageName,
                                sizeDp = 38,
                                active = enabled
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = target.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (target.scope == BlockingScope.ENTIRE_APP) "Full App" else target.feedName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
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
private fun SystemReadinessCard(
    health: SystemHealthState,
    pinProtected: Boolean,
    onOpenAccessibility: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(
            title = "System Health",
            trailing = {
                TextButton(onClick = onOpenSettings) {
                    Text("Settings")
                }
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ReadinessStatusPill(
                label = "Service",
                value = if (health.accessibilityEnabled) "Active" else "Off",
                ok = health.accessibilityEnabled,
                icon = Icons.Rounded.AccessibilityNew,
                onClick = if (health.accessibilityEnabled) onOpenSettings else onOpenAccessibility,
                modifier = Modifier.weight(1f)
            )
            ReadinessStatusPill(
                label = "Battery",
                value = if (health.batteryOptimizationIgnored) "Open" else "Limited",
                ok = health.batteryOptimizationIgnored,
                icon = Icons.Rounded.BatteryChargingFull,
                onClick = onOpenSettings,
                modifier = Modifier.weight(1f)
            )
            ReadinessStatusPill(
                label = "PIN Lock",
                value = if (pinProtected) "Locked" else "Off",
                ok = pinProtected,
                icon = Icons.Rounded.Shield,
                onClick = onOpenSettings,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ReadinessStatusPill(
    label: String,
    value: String,
    ok: Boolean,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.largeIncreased,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Icon(
                    imageVector = if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                    contentDescription = value,
                    tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
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
private fun RecentInterceptionRow(
    record: HistoryRecord,
    nowMillis: Long
) {
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = {
            PlatformAvatar(
                platformKey = record.platformKey,
                packageName = record.packageName,
                sizeDp = 40
            )
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PolicyAndGoalsSummaryCard(
    policy: ContentBlockPolicy,
    requestHideSurfaces: Boolean,
    reelsGoalMinutes: Int,
    feedGoalMinutes: Int,
    protectedAppsCount: Int,
    totalAppsCount: Int,
    onOpenEngine: () -> Unit
) {
    Surface(
        onClick = onOpenEngine,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_policy_goals_card")
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
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Active Policy & Daily Goals",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$protectedAppsCount of $totalAppsCount supported apps guarded",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text(
                        text = policy.label,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GoalMiniTile(
                    icon = Icons.Rounded.PlayCircle,
                    label = "Reels Goal",
                    value = if (reelsGoalMinutes == 0) "0m" else "${reelsGoalMinutes}m",
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.weight(1f)
                )
                GoalMiniTile(
                    icon = Icons.Rounded.ViewStream,
                    label = "Feed Goal",
                    value = if (feedGoalMinutes == 0) "0m" else "${feedGoalMinutes}m",
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.weight(1f)
                )
                GoalMiniTile(
                    icon = Icons.Rounded.VisibilityOff,
                    label = "Don't Show",
                    value = if (requestHideSurfaces || policy == ContentBlockPolicy.HIDE_SURFACES) "On" else "Off",
                    containerColor = if (requestHideSurfaces || policy == ContentBlockPolicy.HIDE_SURFACES) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    contentColor = if (requestHideSurfaces || policy == ContentBlockPolicy.HIDE_SURFACES) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GoalMiniTile(
    icon: ImageVector,
    label: String,
    value: String,
    containerColor: Color,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        contentColor = contentColor
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = contentColor,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (contentColor == MaterialTheme.colorScheme.onSurface) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    contentColor
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

