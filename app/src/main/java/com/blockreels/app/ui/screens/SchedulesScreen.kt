// SPDX-License-Identifier: MIT
package com.blockreels.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blockreels.app.data.ScheduleItem
import com.blockreels.app.data.TargetAppConfig
import com.blockreels.app.state.BlockingScope
import com.blockreels.app.state.InterventionMode
import com.blockreels.app.ui.components.ModeBadge
import com.blockreels.app.ui.components.ScopeBadge
import com.blockreels.app.ui.components.TonalCard
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulesScreen(
    uiState: BlockReelsUiState,
    bottomContentPadding: Dp = 96.dp,
    onUpsertSchedule: (ScheduleItem) -> Unit,
    onToggleSchedule: (String, Boolean) -> Unit,
    onDeleteSchedule: (String) -> Unit,
    onCancelQuickBoost: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editingSchedule by remember { mutableStateOf<ScheduleItem?>(null) }
    var showEditorSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingSchedule = null
                    showEditorSheet = true
                },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("New") },
                modifier = Modifier
                    .padding(bottom = (bottomContentPadding - 16.dp).coerceAtLeast(0.dp))
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
                    bottom = bottomContentPadding + 72.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                uiState.activeQuickBoost?.let { boost ->
                    item(key = "active_boost") {
                        val remainingMin = (((boost.expiresAtMillis ?: uiState.nowMillis) - uiState.nowMillis) / 60_000L)
                            .coerceAtLeast(1L)
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Bolt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "${boost.title} · ${remainingMin}m left",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        boost.modeOverride?.let { ModeBadge(mode = it, compact = true) }
                                        boost.scopeOverride?.let { ScopeBadge(scope = it) }
                                    }
                                }
                                IconButton(
                                    onClick = onCancelQuickBoost,
                                    modifier = Modifier.testTag("schedules_cancel_boost_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Cancel Focus Lock",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                if (uiState.regularSchedules.isEmpty()) {
                    item(key = "empty_schedules") {
                        TonalCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "No schedules",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilledTonalButton(
                                        onClick = {
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
                                                    enabledPackages = uiState.targets.map { it.packageName }.toSet(),
                                                    modeOverride = InterventionMode.STRICT_BLOCK
                                                )
                                            )
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("schedules_add_preset_btn")
                                    ) {
                                        Text("Workday 09–17", maxLines = 1)
                                    }
                                    FilledTonalButton(
                                        onClick = {
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
                                                    enabledPackages = uiState.targets.map { it.packageName }.toSet(),
                                                    modeOverride = InterventionMode.REDIRECT_HOME
                                                )
                                            )
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("schedules_add_night_preset_btn")
                                    ) {
                                        Text("Night 22:30–07", maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                } else {
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

@Composable
private fun ScheduleRuleCard(
    schedule: ScheduleItem,
    allTargets: List<TargetAppConfig>,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    TonalCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("schedule_card_${schedule.id}"),
        containerColor = if (schedule.isActive) {
            MaterialTheme.colorScheme.surfaceContainerLow
        } else {
            MaterialTheme.colorScheme.surfaceContainerLowest
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = schedule.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = schedule.formattedTimeRange(),
                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFontFamily),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Switch(
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
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val appSummary = if (schedule.enabledPackages.isEmpty() ||
                    schedule.enabledPackages.size >= allTargets.size
                ) {
                    "All apps"
                } else {
                    "${schedule.enabledPackages.size} apps"
                }
                Text(
                    text = appSummary,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("schedule_edit_${schedule.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit schedule",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (initial == null) "New Schedule" else "Edit Schedule",
                style = MaterialTheme.typography.headlineSmall
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
                OutlinedButton(
                    onClick = { pickingStart = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("schedule_start_time_btn")
                ) {
                    Text(
                        text = "Start ${String.format("%02d:%02d", startHour, startMinute)}",
                        fontFamily = MonoFontFamily
                    )
                }
                OutlinedButton(
                    onClick = { pickingEnd = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("schedule_end_time_btn")
                ) {
                    Text(
                        text = "End ${String.format("%02d:%02d", endHour, endMinute)}",
                        fontFamily = MonoFontFamily
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Days",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                    style = MaterialTheme.typography.labelMedium,
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
                    style = MaterialTheme.typography.labelMedium,
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
                    text = "Targets",
                    style = MaterialTheme.typography.labelMedium,
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
                TextButton(onClick = {
                    startHour = state.hour
                    startMinute = state.minute
                    pickingStart = false
                }) { Text("Set") }
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
                TextButton(onClick = {
                    endHour = state.hour
                    endMinute = state.minute
                    pickingEnd = false
                }) { Text("Set") }
            },
            dismissButton = {
                TextButton(onClick = { pickingEnd = false }) { Text("Cancel") }
            }
        )
    }
}
