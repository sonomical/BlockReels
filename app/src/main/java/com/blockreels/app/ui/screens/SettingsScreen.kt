package com.blockreels.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blockreels.app.data.AppThemeMode
import com.blockreels.app.state.InterventionMode
import com.blockreels.app.ui.components.AccessibilityDisclosureDialog
import com.blockreels.app.ui.components.SectionHeader
import com.blockreels.app.ui.theme.MonoFontFamily
import com.blockreels.app.viewmodel.BlockReelsUiState
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: BlockReelsUiState,
    bottomContentPadding: Dp = 96.dp,
    onUpdateGlobalMode: (InterventionMode) -> Unit,
    onUpdateHoldDuration: (Int) -> Unit,
    onUpdateCustomMessage: (String) -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onToggleStrictMode: (Boolean) -> Unit,
    onSelectTheme: (AppThemeMode) -> Unit,
    onRequestPinSetup: () -> Unit,
    onRequestDisablePin: () -> Unit,
    onOpenPreviewOverlay: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onExportBackup: () -> String,
    onImportBackup: (String) -> Boolean,
    onClearHistory: () -> Unit,
    onReplayOnboarding: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showMessageDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showDisclosureDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = bottomContentPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "privacy_card") {
                ZeroNetworkPrivacyCard()
            }

            item(key = "appearance_section") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(title = "Appearance", subtitle = "Material 3 dynamic color surfaces")
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        AppThemeMode.entries.forEachIndexed { idx, mode ->
                            SegmentedButton(
                                selected = uiState.settings.themeMode == mode,
                                onClick = { onSelectTheme(mode) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = idx,
                                    count = AppThemeMode.entries.size
                                ),
                                modifier = Modifier.testTag("settings_theme_${mode.name.lowercase()}")
                            ) {
                                Text(mode.label, maxLines = 1)
                            }
                        }
                    }
                }
            }

            item(key = "friction_section") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(
                        title = "Mindful Intervention",
                        subtitle = "Configure hold friction and feedback"
                    )
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Default Intervention Mode",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                val modes = listOf(
                                    InterventionMode.AUTO_BACK to "Auto Back",
                                    InterventionMode.MINDFUL_PAUSE to "Mindful",
                                    InterventionMode.STRICT_BLOCK to "Strict"
                                )
                                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                    modes.forEachIndexed { idx, (mode, label) ->
                                        SegmentedButton(
                                            selected = uiState.settings.globalMode == mode,
                                            onClick = { onUpdateGlobalMode(mode) },
                                            shape = SegmentedButtonDefaults.itemShape(idx, modes.size),
                                            modifier = Modifier.testTag("settings_global_mode_${mode.name.lowercase()}")
                                        ) {
                                            Text(label, maxLines = 1)
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Hold-to-Unlock Duration",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "${uiState.settings.holdDurationSeconds}s",
                                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                Slider(
                                    value = uiState.settings.holdDurationSeconds.toFloat(),
                                    onValueChange = { onUpdateHoldDuration(it.roundToInt()) },
                                    valueRange = 3f..15f,
                                    steps = 11,
                                    modifier = Modifier.testTag("settings_hold_duration_slider")
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier
                                    .clickable { showMessageDialog = true }
                                    .testTag("settings_custom_message_row"),
                                leadingContent = { Icon(Icons.Rounded.EditNote, contentDescription = null) },
                                headlineContent = { Text("Mindful Pause Prompt") },
                                supportingContent = { Text(uiState.settings.customMessage, maxLines = 2) }
                            )

                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                leadingContent = { Icon(Icons.Rounded.VolumeOff, contentDescription = null) },
                                headlineContent = { Text("Audio Ducking & Mute") },
                                supportingContent = { Text("Silence auto-playing reels while overlay is visible") },
                                trailingContent = {
                                    Switch(
                                        checked = uiState.settings.soundEnabled,
                                        onCheckedChange = onToggleSound,
                                        modifier = Modifier.testTag("settings_sound_switch")
                                    )
                                }
                            )

                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                leadingContent = { Icon(Icons.Rounded.Vibration, contentDescription = null) },
                                headlineContent = { Text("Haptic Feedback") },
                                supportingContent = { Text("Tactile pulse on hold start, cancel, and completion") },
                                trailingContent = {
                                    Switch(
                                        checked = uiState.settings.hapticsEnabled,
                                        onCheckedChange = onToggleHaptics,
                                        modifier = Modifier.testTag("settings_haptics_switch")
                                    )
                                }
                            )

                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier
                                    .clickable(onClick = onOpenPreviewOverlay)
                                    .testTag("settings_preview_overlay_row"),
                                leadingContent = { Icon(Icons.Rounded.PlayCircleOutline, contentDescription = null) },
                                headlineContent = { Text("Preview Mindful Overlay") },
                                supportingContent = { Text("Test the hold gesture and pause prompt right now") }
                            )
                        }
                    }
                }
            }

            item(key = "security_section") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(
                        title = "Security & Anti-Tamper",
                        subtitle = "Guard settings against impulsive overrides"
                    )
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column {
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                leadingContent = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                                headlineContent = { Text("PIN Lock Protection") },
                                supportingContent = {
                                    Text(
                                        if (uiState.settings.pinProtected) "4-digit salted PIN required to disable shield"
                                        else "Require a PIN before pausing or relaxing rules"
                                    )
                                },
                                trailingContent = {
                                    Switch(
                                        checked = uiState.settings.pinProtected,
                                        onCheckedChange = { enable ->
                                            if (enable) onRequestPinSetup() else onRequestDisablePin()
                                        },
                                        modifier = Modifier.testTag("settings_pin_switch")
                                    )
                                }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                leadingContent = { Icon(Icons.Rounded.Shield, contentDescription = null) },
                                headlineContent = { Text("Strict Mode") },
                                supportingContent = { Text("Remove temporary 5-minute bypasses on all blocked apps") },
                                trailingContent = {
                                    Switch(
                                        checked = uiState.settings.strictMode,
                                        onCheckedChange = onToggleStrictMode,
                                        modifier = Modifier.testTag("settings_strict_switch")
                                    )
                                }
                            )
                        }
                    }
                }
            }

            item(key = "system_health_section") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(
                        title = "System Permissions & Engine",
                        subtitle = "Keep background monitoring resilient"
                    )
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column {
                            PermissionStatusRow(
                                title = "Accessibility Shield",
                                subtitle = if (uiState.systemHealth.accessibilityEnabled) "Connected & monitoring"
                                else "Required for reel & shorts detection",
                                granted = uiState.systemHealth.accessibilityEnabled,
                                icon = Icons.Rounded.AccessibilityNew,
                                onClick = { showDisclosureDialog = true },
                                tag = "settings_accessibility_row"
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            PermissionStatusRow(
                                title = "Battery Optimization",
                                subtitle = if (uiState.systemHealth.batteryOptimizationIgnored) "Unrestricted background execution"
                                else "Recommended so Android won't kill the shield",
                                granted = uiState.systemHealth.batteryOptimizationIgnored,
                                icon = Icons.Rounded.BatteryChargingFull,
                                onClick = onOpenBatterySettings,
                                tag = "settings_battery_row"
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier
                                    .clickable(onClick = onOpenNotificationSettings)
                                    .testTag("settings_notifications_row"),
                                leadingContent = { Icon(Icons.Rounded.NotificationsActive, contentDescription = null) },
                                headlineContent = { Text("Monitoring Notification") },
                                supportingContent = {
                                    Text(
                                        if (uiState.systemHealth.monitoringServiceRunning) "Foreground service active"
                                        else "Configure persistent status notification"
                                    )
                                }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier
                                    .clickable { showDisclosureDialog = true }
                                    .testTag("settings_disclosure_row"),
                                leadingContent = { Icon(Icons.Rounded.Policy, contentDescription = null) },
                                headlineContent = { Text("Accessibility Disclosure") },
                                supportingContent = { Text("Review how on-device screen inspection works") }
                            )
                        }
                    }
                }
            }

            item(key = "data_section") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(title = "Local Backup & Data", subtitle = "Export or restore rules offline")
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column {
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier
                                    .clickable {
                                        val json = onExportBackup()
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("BlockReels Backup", json))
                                        onShowMessage("Backup JSON copied to clipboard")
                                    }
                                    .testTag("settings_export_backup_row"),
                                leadingContent = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) },
                                headlineContent = { Text("Export Backup to Clipboard") },
                                supportingContent = { Text("Copies rules, schedules, and settings as JSON") }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier
                                    .clickable { showImportDialog = true }
                                    .testTag("settings_import_backup_row"),
                                leadingContent = { Icon(Icons.Rounded.Download, contentDescription = null) },
                                headlineContent = { Text("Import Validated Backup") },
                                supportingContent = { Text("Paste exported JSON to restore your setup") }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier
                                    .clickable(onClick = onReplayOnboarding)
                                    .testTag("settings_replay_onboarding_row"),
                                leadingContent = { Icon(Icons.Rounded.RestartAlt, contentDescription = null) },
                                headlineContent = { Text("Replay Setup Wizard") },
                                supportingContent = { Text("Walk through permissions and app selection again") }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier
                                    .clickable { showClearDialog = true }
                                    .testTag("settings_clear_history_row"),
                                leadingContent = {
                                    Icon(
                                        Icons.Rounded.DeleteOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                headlineContent = {
                                    Text("Clear Interception History", color = MaterialTheme.colorScheme.error)
                                },
                                supportingContent = { Text("Reset daily counts and recorded logs") }
                            )
                        }
                    }
                }
            }

            item(key = "bottom_pad") {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showDisclosureDialog) {
        AccessibilityDisclosureDialog(
            onDismiss = { showDisclosureDialog = false },
            onContinueToSettings = {
                showDisclosureDialog = false
                onOpenAccessibilitySettings()
            }
        )
    }

    if (showMessageDialog) {
        var draft by remember { mutableStateOf(uiState.settings.customMessage) }
        AlertDialog(
            onDismissRequest = { showMessageDialog = false },
            title = { Text("Mindful Pause Prompt") },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    label = { Text("Overlay Message") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateCustomMessage(draft)
                    showMessageDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showMessageDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showImportDialog) {
        var jsonInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Backup JSON") },
            text = {
                OutlinedTextField(
                    value = jsonInput,
                    onValueChange = { jsonInput = it },
                    label = { Text("Paste JSON Backup") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (onImportBackup(jsonInput)) {
                        showImportDialog = false
                    }
                }) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Interception History?") },
            text = { Text("All local interception counts and logs will be reset to zero.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    onClearHistory()
                }) { Text("Clear", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ZeroNetworkPrivacyCard() {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.CloudOff,
                contentDescription = "Zero network privacy",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(30.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "Zero-Network Privacy Guarantee",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "INTERNET permission is absent from this APK. All detection, schedules, and analytics execute 100% locally on your device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.88f)
                )
            }
        }
    }
}

@Composable
private fun PermissionStatusRow(
    title: String,
    subtitle: String,
    granted: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    tag: String
) {
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(tag),
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = {
            Icon(
                imageVector = if (granted) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                contentDescription = if (granted) "Granted" else "Action needed",
                tint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
            )
        }
    )
}
