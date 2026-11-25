// SPDX-License-Identifier: MIT
package com.blockreels.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WorkOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blockreels.app.data.ScheduleItem
import com.blockreels.app.data.TargetAppConfig
import com.blockreels.app.state.BlockingScope
import com.blockreels.app.state.InterventionMode
import com.blockreels.app.ui.components.ExpressiveCard
import com.blockreels.app.ui.components.ExpressiveSwitch
import com.blockreels.app.ui.components.ModeBadge
import com.blockreels.app.ui.components.PlatformAvatar
import com.blockreels.app.ui.components.ScopeBadge
import com.blockreels.app.ui.components.SectionHeader
import com.blockreels.app.ui.theme.MonoFontFamily
import com.blockreels.app.viewmodel.BlockReelsUiState
import java.util.UUID

private val ISO_DAY_LABELS = listOf(
    1 to "M",
    2 to "T",
    3 to "W",
    4 to "T",
    5 to "F",
    6 to "S",
    7 to "S"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SchedulesScreen(
    uiState: BlockReelsUiState,
    bottomContentPadding: Dp = 24.dp,
    onUpsertSchedule: (ScheduleItem) -> Unit,
    onToggleSchedule: (String, Boolean) -> Unit,
    onDeleteSchedule: (String) -> Unit,
    onCancelQuickBoost: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editingSchedule by remember { mutableStateOf<ScheduleItem?>(null) }
    var showEditorSheet by remember { mutableStateOf(false) }

    val allPackages = remember(uiState.targets) { uiState.targets.map { it.packageName }.toSet() }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingSchedule = null
                    showEditorSheet = true
                },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("New Schedule") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = if (bottomContentPadding > 32.dp) 80.dp else 0.dp)
                    .testTag("schedules_add_fab")
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = bottomContentPadding + 84.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item(key = "coverage_summary") {
                    ScheduleCoverageHeader(
                        activeCount = uiState.activeRegularSchedulesCount,
                        totalCount = uiState.regularSchedules.size,
                        focusLockActive = uiState.activeQuickBoost != null,
                        onAddWorkday = {
                            onUpsertSchedule(
                                ScheduleItem(
                                    id = UUID.randomUUID().toString(),
                                    title = "Workday",
                                    isActive = true,
                                    daysOfWeek = setOf(1, 2, 3, 4, 5),
                                    startHour = 9,
                                    startMinute = 0,
                                    endHour = 17,
                                    endMinute = 0,
                                    enabledPackages = allPackages,
                                    modeOverride = InterventionMode.STRICT_BLOCK
                                )
                            )
                        },
                        onAddNight = {
                            onUpsertSchedule(
                                ScheduleItem(
                                    id = UUID.randomUUID().toString(),
                                    title = "Night",
                                    isActive = true,
                                    daysOfWeek = setOf(1, 2, 3, 4, 5, 6, 7),
                                    startHour = 22,
                                    startMinute = 30,
                                    endHour = 7,
                                    endMinute = 0,
                                    enabledPackages = allPackages,
                                    modeOverride = InterventionMode.REDIRECT_HOME
                                )
                            )
                        },
                        onAddMorning = {
                            onUpsertSchedule(
                                ScheduleItem(
                                    id = UUID.randomUUID().toString(),
                                    title = "Deep Morning",
                                    isActive = true,
                                    daysOfWeek = setOf(1, 2, 3, 4, 5, 6, 7),
                                    startHour = 6,
                                    startMinute = 0,
                                    endHour = 9,
                                    endMinute = 0,
                                    enabledPackages = allPackages,
                                    modeOverride = InterventionMode.MINDFUL_PAUSE
                                )
                            )
                        }
                    )
                }

                uiState.activeQuickBoost?.let { boost ->
                    item(key = "active_boost") {
                        val remainingMin = (((boost.expiresAtMillis ?: uiState.nowMillis) - uiState.nowMillis) / 60_000L)
                            .coerceAtLeast(1L)
                        Surface(
                            shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = MaterialShapes.Cookie4Sided.toShape(),
                                    color = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Bolt,
                                            contentDescription = null,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "${boost.title} · ${remainingMin}m left",
                                        style = MaterialTheme.typography.titleMediumEmphasized,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        boost.modeOverride?.let { ModeBadge(mode = it, compact = true) }
                                        boost.scopeOverride?.let { ScopeBadge(scope = it) }
                                    }
                                }
                                FilledTonalIconButton(
                                    onClick = onCancelQuickBoost,
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.testTag("schedules_cancel_boost_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Cancel Focus Lock"
                                    )
                                }
                            }
                        }
                    }
                }

                if (uiState.regularSchedules.isEmpty()) {
                    item(key = "empty_schedules") {
                        ExpressiveCard(shape = MaterialTheme.shapes.extraLarge) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = MaterialShapes.Cookie7Sided.toShape(),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.CalendarMonth,
                                            contentDescription = null,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "No custom schedules yet",
                                        style = MaterialTheme.typography.titleLargeEmphasized
                                    )
                                    Text(
                                        text = "Pick a quick template above or tap New Schedule to automate stricter rules by time of day.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else {
                    item(key = "schedules_header") {
                        SectionHeader(
                            title = "Rules (${uiState.activeRegularSchedulesCount}/${uiState.regularSchedules.size} active)"
                        )
                    }
                    items(uiState.regularSchedules, key = { it.id }) { schedule ->
                        ScheduleRuleCard(
                            schedule = schedule,
                            allTargets = uiState.targets,
                            onToggle = { active -> onToggleSchedule(schedule.id, active) },
                            onEdit = {
                                editingSchedule = schedule
                                showEditorSheet = true
                            },
                            onDelete = { onDeleteSchedule(schedule.id) }
                        )
                    }
                }
            }
        }
    }

    if (showEditorSheet) {
        ScheduleEditorBottomSheet(
            initial = editingSchedule,
            allTargets = uiState.targets,
            onDismiss = { showEditorSheet = false },
            onSave = { saved ->
                onUpsertSchedule(saved)
                showEditorSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ScheduleCoverageHeader(
    activeCount: Int,
    totalCount: Int,
    focusLockActive: Boolean,
    onAddWorkday: () -> Unit,
    onAddNight: () -> Unit,
    onAddMorning: () -> Unit
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = MaterialShapes.Cookie7Sided.toShape(),
                        color = if (activeCount > 0 || focusLockActive) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        contentColor = if (activeCount > 0 || focusLockActive) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = if (activeCount > 0) "$activeCount active windows" else "Always-on baseline",
                            style = MaterialTheme.typography.titleMediumEmphasized,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (totalCount > 0) {
                                "$totalCount saved schedule${if (totalCount == 1) "" else "s"} configured"
                            } else {
                                "Add time-based overrides below"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = if (focusLockActive) {
                        MaterialTheme.colorScheme.primary
                    } else if (activeCount > 0) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    contentColor = if (focusLockActive) {
                        MaterialTheme.colorScheme.onPrimary
                    } else if (activeCount > 0) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                ) {
                    Text(
                        text = if (focusLockActive) "Lock On" else if (activeCount > 0) "Automated" else "Manual",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Quick templates",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickTemplateButton(
                        label = "Workday 09–17",
                        icon = Icons.Rounded.WorkOutline,
                        onClick = onAddWorkday,
                        tag = "schedules_add_preset_btn"
                    )
                    QuickTemplateButton(
                        label = "Night 22:30–07",
                        icon = Icons.Rounded.Bedtime,
                        onClick = onAddNight,
                        tag = "schedules_add_night_preset_btn"
                    )
                    QuickTemplateButton(
                        label = "Morning 06–09",
                        icon = Icons.Rounded.WbSunny,
                        onClick = onAddMorning,
                        tag = "schedules_add_morning_preset_btn"
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun QuickTemplateButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    tag: String
) {
    FilledTonalButton(
        onClick = onClick,
        shapes = ButtonDefaults.shapes(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        modifier = Modifier.testTag(tag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, maxLines = 1)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ScheduleRuleCard(
    schedule: ScheduleItem,
    allTargets: List<TargetAppConfig>,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val coveredTargets = remember(schedule.enabledPackages, allTargets) {
        if (schedule.enabledPackages.isEmpty()) {
            allTargets
        } else {
            allTargets.filter { schedule.enabledPackages.contains(it.packageName) }
        }
    }

    ExpressiveCard(
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.testTag("schedule_card_${schedule.id}"),
        containerColor = if (schedule.isActive) {
            MaterialTheme.colorScheme.surfaceContainerLow
        } else {
            MaterialTheme.colorScheme.surfaceContainerLowest
        }
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = schedule.title,
                            style = MaterialTheme.typography.titleLargeEmphasized,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            shape = CircleShape,
                            color = if (schedule.isActive) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            contentColor = if (schedule.isActive) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        ) {
                            Text(
                                text = if (schedule.isActive) "Active" else "Paused",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = schedule.formattedTimeRange(),
                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFontFamily),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                ExpressiveSwitch(
                    checked = schedule.isActive,
                    onCheckedChange = onToggle,
                    modifier = Modifier.testTag("schedule_switch_${schedule.id}")
                )
            }

            if (schedule.modeOverride != null || schedule.scopeOverride != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    schedule.modeOverride?.let { ModeBadge(mode = it, compact = true) }
                    schedule.scopeOverride?.let { ScopeBadge(scope = it) }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ISO_DAY_LABELS.forEach { (dayNum, label) ->
                    val selected = schedule.daysOfWeek.contains(dayNum)
                    Surface(
                        modifier = Modifier.size(32.dp),
                        shape = CircleShape,
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    coveredTargets.take(4).forEach { target ->
                        PlatformAvatar(
                            platformKey = target.platformKey,
                            packageName = target.packageName,
                            sizeDp = 28,
                            active = schedule.isActive
                        )
                    }
                    if (coveredTargets.size > 4) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "+${coveredTargets.size - 4}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    } else if (coveredTargets.size >= allTargets.size && allTargets.isNotEmpty()) {
                        Text(
                            text = "All apps",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalIconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("schedule_edit_${schedule.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit schedule"
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("schedule_delete_${schedule.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete schedule",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ScheduleEditorBottomSheet(
    initial: ScheduleItem?,
    allTargets: List<TargetAppConfig>,
    onDismiss: () -> Unit,
    onSave: (ScheduleItem) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf(initial?.title ?: "Workday") }
    var startHour by remember { mutableIntStateOf(initial?.startHour ?: 9) }
    var startMinute by remember { mutableIntStateOf(initial?.startMinute ?: 0) }
    var endHour by remember { mutableIntStateOf(initial?.endHour ?: 17) }
    var endMinute by remember { mutableIntStateOf(initial?.endMinute ?: 0) }
    var selectedDays by remember { mutableStateOf(initial?.daysOfWeek ?: setOf(1, 2, 3, 4, 5)) }
    var selectedPackages by remember {
        mutableStateOf(initial?.enabledPackages ?: allTargets.map { it.packageName }.toSet())
    }
    var modeOverride by remember { mutableStateOf(initial?.modeOverride) }
    var scopeOverride by remember { mutableStateOf(initial?.scopeOverride) }
    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }

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
            Text(
                text = if (initial == null) "New Schedule" else "Edit Schedule",
                style = MaterialTheme.typography.headlineSmallEmphasized
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("schedule_title_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TimeSlotCard(
                    label = "Start",
                    timeText = String.format("%02d:%02d", startHour, startMinute),
                    onClick = { pickingStart = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("schedule_start_time_btn")
                )
                TimeSlotCard(
                    label = "End",
                    timeText = String.format("%02d:%02d", endHour, endMinute),
                    onClick = { pickingEnd = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("schedule_end_time_btn")
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Days",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DayPresetPill("Weekdays", selectedDays == setOf(1, 2, 3, 4, 5)) {
                            selectedDays = setOf(1, 2, 3, 4, 5)
                        }
                        DayPresetPill("Daily", selectedDays.size == 7) {
                            selectedDays = setOf(1, 2, 3, 4, 5, 6, 7)
                        }
                        DayPresetPill("Weekends", selectedDays == setOf(6, 7)) {
                            selectedDays = setOf(6, 7)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ISO_DAY_LABELS.forEach { (dayNum, label) ->
                        val isSelected = selectedDays.contains(dayNum)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedDays = if (isSelected && selectedDays.size > 1) {
                                    selectedDays - dayNum
                                } else {
                                    selectedDays + dayNum
                                }
                            },
                            label = { Text(label) },
                            modifier = Modifier.testTag("schedule_day_$dayNum")
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Action Override",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = modeOverride == null,
                        onClick = { modeOverride = null },
                        label = { Text("Default") },
                        modifier = Modifier.testTag("schedule_mode_default")
                    )
                    InterventionMode.entries.forEach { mode ->
                        FilterChip(
                            selected = modeOverride == mode,
                            onClick = { modeOverride = mode },
                            label = { Text(mode.shortLabel) },
                            modifier = Modifier.testTag("schedule_mode_${mode.name.lowercase()}")
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Scope Override",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = scopeOverride == null,
                        onClick = { scopeOverride = null },
                        label = { Text("Default") },
                        modifier = Modifier.testTag("schedule_scope_default")
                    )
                    BlockingScope.entries.forEach { scope ->
                        FilterChip(
                            selected = scopeOverride == scope,
                            onClick = { scopeOverride = scope },
                            label = { Text(scope.displayName) },
                            modifier = Modifier.testTag("schedule_scope_${scope.name.lowercase()}")
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Targets (${selectedPackages.size}/${allTargets.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    allTargets.forEach { target ->
                        val checked = selectedPackages.contains(target.packageName)
                        FilterChip(
                            selected = checked,
                            onClick = {
                                selectedPackages = if (checked) {
                                    selectedPackages - target.packageName
                                } else {
                                    selectedPackages + target.packageName
                                }
                            },
                            leadingIcon = {
                                PlatformAvatar(
                                    platformKey = target.platformKey,
                                    packageName = target.packageName,
                                    sizeDp = 20,
                                    active = checked
                                )
                            },
                            label = { Text(target.displayName) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        onSave(
                            ScheduleItem(
                                id = initial?.id ?: UUID.randomUUID().toString(),
                                title = title.trim().ifEmpty { "Schedule" },
                                isActive = initial?.isActive ?: true,
                                daysOfWeek = selectedDays,
                                startHour = startHour,
                                startMinute = startMinute,
                                endHour = endHour,
                                endMinute = endMinute,
                                enabledPackages = selectedPackages,
                                modeOverride = modeOverride,
                                scopeOverride = scopeOverride
                            )
                        )
                    },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.testTag("schedule_save_btn")
                ) {
                    Text("Save")
                }
            }
        }
    }

    if (pickingStart) {
        val state = rememberTimePickerState(initialHour = startHour, initialMinute = startMinute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickingStart = false },
            title = { Text("Start Time") },
            text = { TimePicker(state = state) },
            confirmButton = {
                Button(
                    onClick = {
                        startHour = state.hour
                        startMinute = state.minute
                        pickingStart = false
                    },
                    shapes = ButtonDefaults.shapes()
                ) { Text("Set") }
            },
            dismissButton = {
                TextButton(onClick = { pickingStart = false }) { Text("Cancel") }
            }
        )
    }

    if (pickingEnd) {
        val state = rememberTimePickerState(initialHour = endHour, initialMinute = endMinute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickingEnd = false },
            title = { Text("End Time") },
            text = { TimePicker(state = state) },
            confirmButton = {
                Button(
                    onClick = {
                        endHour = state.hour
                        endMinute = state.minute
                        pickingEnd = false
                    },
                    shapes = ButtonDefaults.shapes()
                ) { Text("Set") }
            },
            dismissButton = {
                TextButton(onClick = { pickingEnd = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TimeSlotCard(
    label: String,
    timeText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.titleMediumEmphasized.copy(fontFamily = MonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun DayPresetPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}
