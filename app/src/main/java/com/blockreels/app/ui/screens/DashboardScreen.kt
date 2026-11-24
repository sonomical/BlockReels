package com.blockreels.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blockreels.app.data.HistoryRecord
import com.blockreels.app.data.ProtectionStatus
import com.blockreels.app.data.ScheduleItem
import com.blockreels.app.data.TargetAppConfig
import com.blockreels.app.ui.components.AccessibilityDisclosureDialog
import com.blockreels.app.ui.components.ModeBadge
import com.blockreels.app.ui.components.PlatformAvatar
import com.blockreels.app.ui.components.SectionHeader
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
    onStartQuickBoost: (Int) -> Unit,
    onCancelQuickBoost: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenPreviewOverlay: () -> Unit,
    onNavigateToApps: () -> Unit,
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = bottomContentPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "hero_status") {
                ProtectionHeroCard(
                    status = uiState.protectionStatus,
                    masterEnabled = uiState.settings.masterEnabled,
                    enabledAppsCount = uiState.enabledTargetsCount,
                    strictMode = uiState.settings.strictMode,
                    onToggleMaster = onToggleMaster,
                    onEnableAccessibilityClick = { showDisclosure = true }
                )
            }

            item(key = "kpi_row") {
                KpiMetricsRow(
                    blocksToday = uiState.analytics.blocksToday,
                    minutesSavedToday = uiState.analytics.minutesSavedToday,
                    streakDays = uiState.analytics.streakDays
                )
            }

            item(key = "quick_boost") {
                QuickBoostSection(
                    activeBoost = uiState.activeQuickBoost,
                    nowMillis = uiState.nowMillis,
                    onStartBoost = onStartQuickBoost,
                    onCancelBoost = onCancelQuickBoost
                )
            }

            item(key = "quick_toggles") {
                QuickAppTogglesSection(
                    targets = uiState.targets.take(4),
                    onToggleTarget = onToggleTargetApp,
                    onSeeAllClick = onNavigateToApps
                )
            }

            item(key = "mindful_preview_card") {
                MindfulPreviewBanner(
                    holdSeconds = uiState.settings.holdDurationSeconds,
                    onPreviewClick = onOpenPreviewOverlay
                )
            }

            item(key = "recent_header") {
                SectionHeader(
                    title = "Recent Interceptions",
                    subtitle = "100% on-device detection log",
                    trailing = {
                        if (uiState.history.isNotEmpty()) {
                            TextButton(
                                onClick = onNavigateToStats,
                                modifier = Modifier.testTag("dashboard_view_all_stats_btn")
                            ) {
                                Text("Insights")
                            }
                        }
                    }
                )
            }

            if (uiState.history.isEmpty()) {
                item(key = "empty_recent") {
                    EmptyInterceptionsCard(
                        accessibilityEnabled = uiState.systemHealth.accessibilityEnabled
                    )
                }
            } else {
                item(key = "recent_list_card") {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        shape = MaterialTheme.shapes.large
                    ) {
                        val recent = uiState.history.take(5)
                        Column {
                            recent.forEachIndexed { index, record ->
                                RecentInterceptionRow(
                                    record = record,
                                    nowMillis = uiState.nowMillis
                                )
                                if (index < recent.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun ProtectionHeroCard(
    status: ProtectionStatus,
    masterEnabled: Boolean,
    enabledAppsCount: Int,
    strictMode: Boolean,
    onToggleMaster: (Boolean) -> Unit,
    onEnableAccessibilityClick: () -> Unit
) {
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
            targetValue = 1.07f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        ).value
    } else 1f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_protection_hero_card"),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = containerColor)
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
                Box(
                    modifier = Modifier
                        .size(56.dp)
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
                        contentDescription = "Protection status icon",
                        tint = contentColor,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    val badgeText = when (status) {
                        ProtectionStatus.PROTECTED -> if (strictMode) "STRICT LOCK ACTIVE" else "SHIELD ACTIVE"
                        ProtectionStatus.PAUSED -> "SHIELD PAUSED"
                        ProtectionStatus.PERMISSION_NEEDED -> "SETUP REQUIRED"
                    }
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when (status) {
                            ProtectionStatus.PROTECTED -> "Focus Protected"
                            ProtectionStatus.PAUSED -> "Protection Paused"
                            ProtectionStatus.PERMISSION_NEEDED -> "Enable Accessibility"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        color = contentColor
                    )
                    Text(
                        text = when (status) {
                            ProtectionStatus.PROTECTED -> "Guarding $enabledAppsCount short-form feeds locally"
                            ProtectionStatus.PAUSED -> "Short-video feeds are currently unrestricted"
                            ProtectionStatus.PERMISSION_NEEDED -> "Grant Accessibility permission to detect Reels & Shorts"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.85f)
                    )
                }

                Switch(
                    checked = masterEnabled,
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
                    Text("Activate Accessibility Shield")
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
            label = "Blocked Today",
            value = blocksToday.toString(),
            icon = Icons.Rounded.Shield,
            iconTint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        KpiTile(
            label = "Time Saved",
            value = formatDurationMinutes(minutesSavedToday),
            icon = Icons.Rounded.Schedule,
            iconTint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f)
        )
        KpiTile(
            label = "Focus Streak",
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun QuickBoostSection(
    activeBoost: ScheduleItem?,
    nowMillis: Long,
    onStartBoost: (Int) -> Unit,
    onCancelBoost: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(
            title = "Quick Focus Boost",
            subtitle = "Lock down all short-video feeds immediately"
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
                            text = "$remainingMin min remaining · All feeds guarded",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                        )
                    }
                    IconButton(
                        onClick = onCancelBoost,
                        modifier = Modifier.testTag("dashboard_cancel_boost_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Cancel Focus Boost",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        } else {
            val presets = listOf(30 to "30 min", 60 to "1 hour", 120 to "2 hours")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                presets.forEach { (minutes, label) ->
                    FilterChip(
                        selected = false,
                        onClick = { onStartBoost(minutes) },
                        label = { Text(label) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Bolt,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
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
private fun QuickAppTogglesSection(
    targets: List<TargetAppConfig>,
    onToggleTarget: (String, Boolean) -> Unit,
    onSeeAllClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(
            title = "Guarded Feeds",
            subtitle = "Selective blocking keeps messages & regular video open",
            trailing = {
                TextButton(
                    onClick = onSeeAllClick,
                    modifier = Modifier.testTag("dashboard_manage_apps_btn")
                ) {
                    Text("Configure")
                }
            }
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(targets, key = { it.packageName }) { target ->
                val container = if (target.isEnabled) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                }
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = container,
                    modifier = Modifier
                        .width(136.dp)
                        .clip(MaterialTheme.shapes.large)
                        .clickable { onToggleTarget(target.packageName, !target.isEnabled) }
                        .testTag("quick_toggle_${target.platformKey.lowercase()}")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PlatformAvatar(platformKey = target.platformKey, sizeDp = 36)
                            Surface(
                                shape = CircleShape,
                                color = if (target.isEnabled) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                }
                            ) {
                                Text(
                                    text = if (target.isEnabled) "ON" else "OFF",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (target.isEnabled) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = target.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = target.feedName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MindfulPreviewBanner(
    holdSeconds: Int,
    onPreviewClick: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.SelfImprovement,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Mindful Pause Friction",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Requires a ${holdSeconds}s intentional hold before allowing a 5m pass.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(
                onClick = onPreviewClick,
                modifier = Modifier.testTag("dashboard_preview_overlay_btn")
            ) {
                Text("Test")
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
            PlatformAvatar(platformKey = record.platformKey, sizeDp = 40)
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
            ModeBadge(mode = record.interventionMode)
        }
    )
}

@Composable
private fun EmptyInterceptionsCard(accessibilityEnabled: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.VerifiedUser,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = if (accessibilityEnabled) "Calm waters so far" else "Ready to protect your focus",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (accessibilityEnabled) {
                    "When you open Reels, Shorts, or TikTok, BlockReels will intercept them and log your saved time here."
                } else {
                    "Activate the Accessibility Shield above or tap Test on Mindful Pause to preview how interventions feel."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
