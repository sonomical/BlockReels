// SPDX-License-Identifier: MIT
package com.blockreels.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blockreels.app.BuildConfig
import com.blockreels.app.data.AppThemeMode
import com.blockreels.app.ui.components.AccessibilityDisclosureDialog
import com.blockreels.app.ui.components.SectionHeader
import com.blockreels.app.ui.components.TonalCard
import com.blockreels.app.viewmodel.BlockReelsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: BlockReelsUiState,
    bottomContentPadding: Dp = 96.dp,
    onToggleStrictMode: (Boolean) -> Unit,
    onSelectTheme: (AppThemeMode) -> Unit,
    onRequestPinSetup: () -> Unit,
    onRequestDisablePin: () -> Unit,
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
    var showImportDialog by remember { mutableStateOf(false) }
    var showDisclosureDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showLicenseDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomContentPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "appearance_section") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader(title = "Appearance")
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

            item(key = "lock_section") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader(title = "Protection Lock")
                    TonalCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                leadingContent = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                                headlineContent = { Text("PIN Lock") },
                                supportingContent = {
                                    Text(if (uiState.settings.pinProtected) "4-digit PIN active" else "Off")
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
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                leadingContent = { Icon(Icons.Rounded.Shield, contentDescription = null) },
                                headlineContent = { Text("Global Strict Lock") },
                                supportingContent = { Text("Disable hold-to-pass across all targets") },
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

            item(key = "permissions_section") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader(title = "Permissions")
                    TonalCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            PermissionStatusRow(
                                title = "Accessibility Service",
                                status = if (uiState.systemHealth.accessibilityEnabled) "Active" else "Off",
                                granted = uiState.systemHealth.accessibilityEnabled,
                                icon = Icons.Rounded.AccessibilityNew,
                                onClick = { showDisclosureDialog = true },
                                tag = "settings_accessibility_row"
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                            PermissionStatusRow(
                                title = "Battery Optimization",
                                status = if (uiState.systemHealth.batteryOptimizationIgnored) "Unrestricted" else "Restricted",
                                granted = uiState.systemHealth.batteryOptimizationIgnored,
                                icon = Icons.Rounded.BatteryChargingFull,
                                onClick = onOpenBatterySettings,
                                tag = "settings_battery_row"
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier
                                    .clickable(onClick = onOpenNotificationSettings)
                                    .testTag("settings_notifications_row"),
                                leadingContent = { Icon(Icons.Rounded.NotificationsActive, contentDescription = null) },
                                headlineContent = { Text("Notifications") },
                                supportingContent = {
                                    Text(if (uiState.systemHealth.monitoringServiceRunning) "Running" else "Configure")
                                }
                            )
                        }
                    }
                }
            }

            item(key = "backup_section") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader(title = "Backup & Reset")
                    TonalCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier
                                    .clickable {
                                        val json = onExportBackup()
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("BlockReels Backup", json))
                                        onShowMessage("Backup JSON copied")
                                    }
                                    .testTag("settings_export_backup_row"),
                                leadingContent = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) },
                                headlineContent = { Text("Export JSON") }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier
                                    .clickable { showImportDialog = true }
                                    .testTag("settings_import_backup_row"),
                                leadingContent = { Icon(Icons.Rounded.Download, contentDescription = null) },
                                headlineContent = { Text("Import JSON") }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier
                                    .clickable(onClick = onReplayOnboarding)
                                    .testTag("settings_replay_onboarding_row"),
                                leadingContent = { Icon(Icons.Rounded.RestartAlt, contentDescription = null) },
                                headlineContent = { Text("Setup Wizard") }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
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
                                    Text("Clear History", color = MaterialTheme.colorScheme.error)
                                }
                            )
                        }
                    }
                }
            }

            item(key = "license_section") {
                TonalCard(modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .clickable { showLicenseDialog = true }
                            .testTag("settings_license_row"),
                        leadingContent = { Icon(Icons.Rounded.Code, contentDescription = null) },
                        headlineContent = { Text("BlockReels v${BuildConfig.VERSION_NAME}") },
                        supportingContent = { Text("MIT License · Open Source") }
                    )
                }
            }

            item(key = "bottom_pad") {
                Spacer(modifier = Modifier.height(12.dp))
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

    if (showImportDialog) {
        var jsonInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Backup JSON") },
            text = {
                OutlinedTextField(
                    value = jsonInput,
                    onValueChange = { jsonInput = it },
                    label = { Text("JSON") },
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
            title = { Text("Clear history?") },
            text = { Text("All local counts and logs will be reset.") },
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

    if (showLicenseDialog) {
        AlertDialog(
            onDismissRequest = { showLicenseDialog = false },
            title = { Text("MIT License") },
            text = {
                Text(
                    "Copyright (c) 2026 BlockReels Contributors\n\n" +
                        "Permission is hereby granted, free of charge, to any person obtaining a copy " +
                        "of this software and associated documentation files, to deal in the Software " +
                        "without restriction, including without limitation the rights to use, copy, " +
                        "modify, merge, publish, distribute, sublicense, and/or sell copies of the Software.\n\n" +
                        "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND."
                )
            },
            confirmButton = {
                TextButton(onClick = { showLicenseDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun PermissionStatusRow(
    title: String,
    status: String,
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
        supportingContent = { Text(status) },
        trailingContent = {
            Icon(
                imageVector = if (granted) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                contentDescription = status,
                tint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
            )
        }
    )
}
