// SPDX-License-Identifier: MIT
package com.blockreels.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blockreels.app.data.DayCountPoint
import com.blockreels.app.data.PlatformBreakdownItem
import com.blockreels.app.data.StatsRange
import com.blockreels.app.data.SupportedTargets
import com.blockreels.app.ui.components.ConnectedToggleButtonGroup
import com.blockreels.app.ui.components.ExpressiveCard
import com.blockreels.app.ui.components.ExpressiveShapes
import com.blockreels.app.ui.components.ModeBadge
import com.blockreels.app.ui.components.PlatformAvatar
import com.blockreels.app.ui.components.SectionHeader
import com.blockreels.app.ui.components.formatDurationMinutes
import com.blockreels.app.ui.components.formatRelativeTime
import com.blockreels.app.ui.components.segmentedListShape
import com.blockreels.app.ui.theme.MonoFontFamily
import com.blockreels.app.viewmodel.BlockReelsUiState

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StatisticsScreen(
    uiState: BlockReelsUiState,
    bottomContentPadding: Dp = 24.dp,
    onSelectRange: (StatsRange) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var confirmClear by remember { mutableStateOf(false) }
    var selectedLogPlatform by remember { mutableStateOf<String?>(null) }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            icon = { Icon(Icons.Rounded.DeleteSweep, contentDescription = null) },
            title = { Text("Clear history?") },
            text = { Text("Resets local block counters and logs. Rules and schedules stay intact.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmClear = false
                        onClearHistory()
                    },
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("stats_confirm_clear_btn")
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val analytics = uiState.analytics
    val distinctPlatforms = remember(uiState.history) {
        uiState.history.distinctBy { it.platformKey }.map { it.platformKey to it.appLabel }
    }
    val effectiveFilter = if (selectedLogPlatform != null && distinctPlatforms.any { it.first == selectedLogPlatform }) {
        selectedLogPlatform
    } else null
    val filteredHistory = remember(uiState.history, effectiveFilter) {
        if (effectiveFilter == null) uiState.history else uiState.history.filter { it.platformKey == effectiveFilter }
    }
    val visibleHistory = remember(filteredHistory) { filteredHistory.take(30) }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomContentPadding),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item(key = "range_selector", contentType = "range_selector") {
                Column(
                    modifier = Modifier.padding(bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ConnectedToggleButtonGroup(
                        options = StatsRange.entries,
                        selected = uiState.selectedStatsRange,
                        onSelect = onSelectRange,
                        label = { it.label },
                        tagFor = { "stats_range_${it.name.lowercase()}" }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Text(
                                text = "Daily avg ${String.format("%.1f", analytics.averageDailyBlocks)} / day",
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                        analytics.platformBreakdown.firstOrNull()?.let { top ->
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 6.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    PlatformAvatar(
                                        platformKey = top.platformKey,
                                        packageName = SupportedTargets.primaryPackageForPlatform(top.platformKey),
                                        sizeDp = 22
                                    )
                                    Text(
                                        text = "Top: ${top.label} ${(top.fraction * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item(key = "summary_grid", contentType = "summary_grid") {
                Column(
                    modifier = Modifier.padding(bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatHeroTile(
                            label = "Blocked",
                            value = analytics.totalBlocks.toString(),
                            icon = Icons.Rounded.Shield,
                            container = MaterialTheme.colorScheme.primaryContainer,
                            content = MaterialTheme.colorScheme.onPrimaryContainer,
                            badgeContainer = MaterialTheme.colorScheme.primary,
                            badgeContent = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        StatHeroTile(
                            label = "Reclaimed",
                            value = formatDurationMinutes(analytics.minutesSaved),
                            icon = Icons.Rounded.AccessTime,
                            container = MaterialTheme.colorScheme.secondaryContainer,
                            content = MaterialTheme.colorScheme.onSecondaryContainer,
                            badgeContainer = MaterialTheme.colorScheme.secondary,
                            badgeContent = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatHeroTile(
                            label = "Peak Hour",
                            value = analytics.peakHourLabel,
                            icon = Icons.Rounded.Insights,
                            container = MaterialTheme.colorScheme.surfaceContainerHigh,
                            content = MaterialTheme.colorScheme.onSurface,
                            badgeContainer = MaterialTheme.colorScheme.surfaceContainerLowest,
                            badgeContent = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        StatHeroTile(
                            label = "Streak",
                            value = "${analytics.streakDays}d",
                            icon = Icons.Rounded.LocalFireDepartment,
                            container = MaterialTheme.colorScheme.tertiaryContainer,
                            content = MaterialTheme.colorScheme.onTertiaryContainer,
                            badgeContainer = MaterialTheme.colorScheme.tertiary,
                            badgeContent = MaterialTheme.colorScheme.onTertiary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item(key = "weekly_chart", contentType = "weekly_chart") {
                Box(modifier = Modifier.padding(bottom = 14.dp)) {
                    WeeklyActivityChartCard(series = analytics.dailySeries)
                }
            }

            if (analytics.platformBreakdown.isNotEmpty()) {
                item(key = "platform_breakdown", contentType = "platform_breakdown") {
                    Box(modifier = Modifier.padding(bottom = 14.dp)) {
                        PlatformBreakdownCard(items = analytics.platformBreakdown)
                    }
                }
            }

            item(key = "log_header", contentType = "log_header") {
                Column(
                    modifier = Modifier.padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SectionHeader(
                        title = "Log (${filteredHistory.size})",
                        trailing = {
                            if (uiState.history.isNotEmpty()) {
                                TextButton(
                                    onClick = { confirmClear = true },
                                    modifier = Modifier.testTag("stats_clear_history_btn")
                                ) {
                                    Text("Clear", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    )

                    if (distinctPlatforms.size > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = effectiveFilter == null,
                                onClick = { selectedLogPlatform = null },
                                label = { Text("All (${uiState.history.size})") },
                                modifier = Modifier.testTag("stats_log_filter_all")
                            )
                            distinctPlatforms.forEach { (platformKey, label) ->
                                val isSelected = effectiveFilter == platformKey
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedLogPlatform = if (isSelected) null else platformKey
                                    },
                                    leadingIcon = {
                                        PlatformAvatar(
                                            platformKey = platformKey,
                                            packageName = SupportedTargets.primaryPackageForPlatform(platformKey),
                                            sizeDp = 18,
                                            active = isSelected
                                        )
                                    },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag("stats_log_filter_$platformKey")
                                )
                            }
                        }
                    }
                }
            }

            if (visibleHistory.isEmpty()) {
                item(key = "log_empty", contentType = "log_empty") {
                    ExpressiveCard(shape = MaterialTheme.shapes.extraLarge) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = ExpressiveShapes.Cookie7Sided,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.History,
                                        contentDescription = null,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Text(
                                text = "No events recorded",
                                style = MaterialTheme.typography.titleMediumEmphasized
                            )
                            Text(
                                text = "Interceptions appear here locally as soon as a guarded feed is blocked.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(
                    items = visibleHistory,
                    key = { _, record -> record.id },
                    contentType = { _, _ -> "history_row" }
                ) { idx, record ->
                    Surface(
                        shape = segmentedListShape(idx, visibleHistory.size),
                        color = MaterialTheme.colorScheme.surfaceContainerLow
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
                                    text = formatRelativeTime(record.timestampMillis, uiState.nowMillis),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingContent = {
                                ModeBadge(mode = record.interventionMode, compact = true)
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
private fun StatHeroTile(
    label: String,
    value: String,
    icon: ImageVector,
    container: Color,
    content: Color,
    badgeContainer: Color,
    badgeContent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = container,
        contentColor = content
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = ExpressiveShapes.Cookie4Sided,
                color = badgeContainer,
                contentColor = badgeContent,
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmallEmphasized.copy(fontFamily = MonoFontFamily),
                    color = content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = content
                )
            }
        }
    }
}

@Composable
private fun WeeklyActivityChartCard(series: List<DayCountPoint>) {
    val defaultIndex = remember(series) {
        series.indexOfFirst { it.isToday }.takeIf { it >= 0 } ?: (series.size - 1).coerceAtLeast(0)
    }
    var selectedIndex by remember(series.size) { mutableIntStateOf(defaultIndex) }
    val safeIndex = selectedIndex.coerceIn(0, (series.size - 1).coerceAtLeast(0))
    val selectedPoint = series.getOrNull(safeIndex)

    ExpressiveCard(shape = MaterialTheme.shapes.extraLarge) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionHeader(
                title = "7-Day Trend",
                trailing = {
                    selectedPoint?.let { point ->
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(
                                text = "${if (point.isToday) "Today" else point.dayLabel} · ${point.count} ${if (point.count == 1) "block" else "blocks"}",
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            )

            val maxCount = remember(series) { (series.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1) }
            val selectedBarColor = MaterialTheme.colorScheme.primary
            val todayBarColor = MaterialTheme.colorScheme.secondary
            val normalBarColor = MaterialTheme.colorScheme.secondaryContainer
            val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(144.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val count = series.size.coerceAtLeast(1)
                    val slotWidth = size.width / count
                    val barWidth = (slotWidth * 0.48f).coerceAtMost(30.dp.toPx())
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
                            (size.height * fraction).coerceAtLeast(16.dp.toPx())
                        } else 0f
                        if (barHeight > 0f) {
                            val barColor = when {
                                index == safeIndex -> selectedBarColor
                                point.isToday -> todayBarColor
                                else -> normalBarColor
                            }
                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(left, size.height - barHeight),
                                size = Size(barWidth, barHeight),
                                cornerRadius = radius
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxSize()) {
                    series.forEachIndexed { index, point ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(role = Role.Button) { selectedIndex = index }
                                .testTag("stats_chart_bar_${point.dayLabel.lowercase()}_$index")
                        )
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                series.forEachIndexed { index, point ->
                    val isSelected = index == safeIndex
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(role = Role.Button) { selectedIndex = index },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = point.count.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Text(
                                text = point.dayLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected || point.isToday) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlatformBreakdownCard(items: List<PlatformBreakdownItem>) {
    ExpressiveCard(shape = MaterialTheme.shapes.extraLarge) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionHeader(title = "By Platform")
            items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PlatformAvatar(
                        platformKey = item.platformKey,
                        packageName = SupportedTargets.primaryPackageForPlatform(item.platformKey),
                        sizeDp = 38
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${item.count} (${(item.fraction * 100).toInt()}%)",
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        LinearWavyProgressIndicator(
                            progress = { item.fraction.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    }
                }
            }
        }
    }
}
