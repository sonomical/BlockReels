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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
                text = { Text("New Schedule") },
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
                    top = 12.dp,
                    bottom = bottomContentPadding + 72.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Bolt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${boost.title} Active",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "$remainingMin min remaining across all apps",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                    )
                                }
                                IconButton(
                                    onClick = onCancelQuickBoost,
                                    modifier = Modifier.testTag("schedules_cancel_boost_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Cancel Quick Boost",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                if (uiState.regularSchedules.isEmpty()) {
                    item(key = "empty_schedules") {
                        Surface(
                            shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = "No Focus Schedules Yet",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Create recurring quiet hours (like Deep Work 09:00–17:00 or Sleep Wind-Down 22:30–07:00) to enforce blocking automatically.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                FilledTonalButton(
                                    onClick = {
                                        onUpsertSchedule(
                                            ScheduleItem(
                                                id = UUID.randomUUID().toString(),
                                                title = "Deep Work",
                                                isActive = true,
                                                daysOfWeek = setOf(1, 2, 3, 4, 5),
                                                startHour = 9,
                                                startMinute = 0,
                                                endHour = 17,
                                                endMinute = 0,
                                                enabledPackages = uiState.targets.map { it.packageName }.toSet()
                                            )
                                        )
                                    },
                                    modifier = Modifier.testTag("schedules_add_preset_btn")
                                ) {
                                    Text("Add Weekday Deep Work (09:00–17:00)")
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("schedule_card_${schedule.id}"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (schedule.isActive) {
                MaterialTheme.colorScheme.surfaceContainerLow
            } else {
                MaterialTheme.colorScheme.surfaceContainerLowest
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            modifier = Modifier.size(16.dp)
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

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ISO_DAY_LABELS.forEach { (dayNum, label) ->
                    val selected = schedule.daysOfWeek.contains(dayNum)
                    Box(
                        modifier = Modifier
                            .size(30.dp)
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
                    "All supported feeds"
                } else {
                    "${schedule.enabledPackages.size} selected feeds"
                }
                Text(
                    text = appSummary,
                    style = MaterialTheme.typography.bodySmall,
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
    var title by remember { mutableStateOf(initial?.title ?: "Deep Focus") }
    var startHour by remember { mutableIntStateOf(initial?.startHour ?: 9) }
    var startMinute by remember { mutableIntStateOf(initial?.startMinute ?: 0) }
    var endHour by remember { mutableIntStateOf(initial?.endHour ?: 17) }
    var endMinute by remember { mutableIntStateOf(initial?.endMinute ?: 0) }
    var selectedDays by remember { mutableStateOf(initial?.daysOfWeek ?: setOf(1, 2, 3, 4, 5)) }
    var selectedPackages by remember {
        mutableStateOf(initial?.enabledPackages ?: allTargets.map { it.packageName }.toSet())
    }
    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (initial == null) "Create Focus Schedule" else "Edit Schedule",
                style = MaterialTheme.typography.headlineSmall
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Schedule Name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("schedule_title_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { pickingStart = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("schedule_start_time_btn")
                ) {
                    Text(
                        text = "Start: ${String.format("%02d:%02d", startHour, startMinute)}",
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
                        text = "End: ${String.format("%02d:%02d", endHour, endMinute)}",
                        fontFamily = MonoFontFamily
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Active Days",
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
                    text = "Protected Feeds",
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
                                title = title.trim().ifEmpty { "Focus Schedule" },
                                isActive = initial?.isActive ?: true,
                                daysOfWeek = selectedDays,
                                startHour = startHour,
                                startMinute = startMinute,
                                endHour = endHour,
                                endMinute = endMinute,
                                enabledPackages = selectedPackages
                            )
                        )
                    },
                    modifier = Modifier.testTag("schedule_save_btn")
                ) {
                    Text("Save Schedule")
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
