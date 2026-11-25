// SPDX-License-Identifier: MIT
package com.blockreels.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blockreels.app.BuildConfig
import com.blockreels.app.data.AppThemeMode
import com.blockreels.app.ui.components.AccessibilityDisclosureDialog
import com.blockreels.app.ui.components.ConnectedToggleButtonGroup
import com.blockreels.app.ui.components.ExpressiveSwitch
import com.blockreels.app.ui.components.SectionHeader
import com.blockreels.app.ui.components.segmentedListShape
import com.blockreels.app.viewmodel.BlockReelsUiState

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    uiState: BlockReelsUiState,
    bottomContentPadding: Dp = 24.dp,
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item(key = "appearance_section") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(title = "Appearance")
                    ConnectedToggleButtonGroup(
                        options = AppThemeMode.entries,
                        selected = uiState.settings.themeMode,
                        onSelect = onSelectTheme,
                        label = { it.label },
                        tagFor = { "settings_theme_${it.name.lowercase()}" }
                    )
                }
            }

            item(key = "lock_section") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(title = "Protection Lock")
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Surface(
                            shape = segmentedListShape(0, 2),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                leadingContent = {
                                    SettingsIconBadge(
                                        icon = Icons.Rounded.Lock,
                                        container = MaterialTheme.colorScheme.primaryContainer,
                                        content = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                },
                                headlineContent = { Text("PIN Lock") },
                                supportingContent = {
                                    Text(if (uiState.settings.pinProtected) "4-digit PIN active" else "Off")
                                },
                                trailingContent = {
                                    ExpressiveSwitch(
                                        checked = uiState.settings.pinProtected,
                                        onCheckedChange = { enable ->
                                            if (enable) onRequestPinSetup() else onRequestDisablePin()
                                        },
                                        modifier = Modifier.testTag("settings_pin_switch")
                                    )
                                }
                            )
                        }

                        Surface(
                            shape = segmentedListShape(1, 2),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                leadingContent = {
                                    SettingsIconBadge(
                                        icon = Icons.Rounded.Shield,
                                        container = MaterialTheme.colorScheme.secondaryContainer,
                                        content = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                },
                                headlineContent = { Text("Global Strict Lock") },
                                supportingContent = { Text("Disable hold-to-pass across all targets") },
                                trailingContent = {
                                    ExpressiveSwitch(
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
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(title = "Permissions")
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        PermissionStatusRow(
                            title = "Accessibility Service",
                            subtitle = if (uiState.systemHealth.accessibilityEnabled) {
                                "Monitoring active targets"
                            } else {
                                "Required for feed detection"
                            },
                            badgeLabel = if (uiState.systemHealth.accessibilityEnabled) "Active" else "Action needed",
                            granted = uiState.systemHealth.accessibilityEnabled,
                            icon = Icons.Rounded.AccessibilityNew,
                            shape = segmentedListShape(0, 3),
                            onClick = { showDisclosureDialog = true },
                            tag = "settings_accessibility_row"
                        )
                        PermissionStatusRow(
                            title = "Battery Optimization",
                            subtitle = if (uiState.systemHealth.batteryOptimizationIgnored) {
                                "Exempt from background sleep"
                            } else {
                                "May pause blocker when idle"
                            },
                            badgeLabel = if (uiState.systemHealth.batteryOptimizationIgnored) "Unrestricted" else "Restricted",
                            granted = uiState.systemHealth.batteryOptimizationIgnored,
                            icon = Icons.Rounded.BatteryChargingFull,
                            shape = segmentedListShape(1, 3),
                            onClick = onOpenBatterySettings,
                            tag = "settings_battery_row"
                        )
                        Surface(
                            onClick = onOpenNotificationSettings,
                            shape = segmentedListShape(2, 3),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_notifications_row")
                        ) {
                            val running = uiState.systemHealth.monitoringServiceRunning
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                leadingContent = {
                                    SettingsIconBadge(
                                        icon = Icons.Rounded.NotificationsActive,
                                        container = if (running) MaterialTheme.colorScheme.secondaryContainer
                                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                                        content = if (running) MaterialTheme.colorScheme.onSecondaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                headlineContent = { Text("Notifications") },
                                supportingContent = {
                                    Text(if (running) "Status notification active" else "Configure status alerts")
                                },
                                trailingContent = {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (running) MaterialTheme.colorScheme.secondaryContainer
                                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                                        contentColor = if (running) MaterialTheme.colorScheme.onSecondaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    ) {
                                        Text(
                                            text = if (running) "Running" else "Configure",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }

            item(key = "backup_section") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(title = "Backup & Reset")
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Surface(
                            onClick = {
                                val json = onExportBackup()
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("BlockReels Backup", json))
                                onShowMessage("Backup JSON copied")
                            },
                            shape = segmentedListShape(0, 4),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_export_backup_row")
                        ) {
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                leadingContent = {
                                    SettingsIconBadge(
                                        icon = Icons.Rounded.ContentCopy,
                                        container = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        content = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                headlineContent = { Text("Export JSON") },
                                supportingContent = { Text("Copy rules and schedules to clipboard") }
                            )
                        }

                        Surface(
                            onClick = { showImportDialog = true },
                            shape = segmentedListShape(1, 4),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_import_backup_row")
                        ) {
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                leadingContent = {
                                    SettingsIconBadge(
                                        icon = Icons.Rounded.Download,
                                        container = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        content = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                headlineContent = { Text("Import JSON") },
                                supportingContent = { Text("Restore configuration from backup text") }
                            )
                        }

                        Surface(
                            onClick = onReplayOnboarding,
                            shape = segmentedListShape(2, 4),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_replay_onboarding_row")
                        ) {
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                leadingContent = {
                                    SettingsIconBadge(
                                        icon = Icons.Rounded.RestartAlt,
                                        container = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        content = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                headlineContent = { Text("Setup Wizard") },
                                supportingContent = { Text("Review permissions and initial targets") }
                            )
                        }

                        Surface(
                            onClick = { showClearDialog = true },
                            shape = segmentedListShape(3, 4),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_clear_history_row")
                        ) {
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                leadingContent = {
                                    SettingsIconBadge(
                                        icon = Icons.Rounded.DeleteOutline,
                                        container = MaterialTheme.colorScheme.errorContainer,
                                        content = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                },
                                headlineContent = {
                                    Text("Clear History", color = MaterialTheme.colorScheme.error)
                                },
                                supportingContent = { Text("Reset local block counters and logs") }
                            )
                        }
                    }
                }
            }

            item(key = "privacy_and_about_section") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(title = "Privacy & About")
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = MaterialShapes.Cookie7Sided.toShape(),
                                color = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.VerifiedUser,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "100% On-Device Privacy",
                                    style = MaterialTheme.typography.titleMediumEmphasized,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Zero network permissions, zero telemetry, and no cloud sync. All rules and stats stay strictly on this phone.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = { showLicenseDialog = true },
                        shape = segmentedListShape(0, 1),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_license_row")
                    ) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            leadingContent = {
                                SettingsIconBadge(
                                    icon = Icons.Rounded.Code,
                                    container = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    content = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            headlineContent = { Text("BlockReels v${BuildConfig.VERSION_NAME}") },
                            supportingContent = { Text("MIT License · Open Source") }
                        )
                    }
                }
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
                Button(
                    onClick = {
                        if (onImportBackup(jsonInput)) {
                            showImportDialog = false
                        }
                    },
                    shapes = ButtonDefaults.shapes()
                ) { Text("Restore") }
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
                Button(
                    onClick = {
                        showClearDialog = false
                        onClearHistory()
                    },
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text("Clear") }
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
                Button(
                    onClick = { showLicenseDialog = false },
                    shapes = ButtonDefaults.shapes()
                ) { Text("Close") }
            }
        )
    }
}

@Composable
private fun SettingsIconBadge(
    icon: ImageVector,
    container: Color,
    content: Color
) {
    Surface(
        shape = CircleShape,
        color = container,
        contentColor = content,
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun PermissionStatusRow(
    title: String,
    subtitle: String,
    badgeLabel: String,
    granted: Boolean,
    icon: ImageVector,
    shape: Shape,
    onClick: () -> Unit,
    tag: String
) {
    Surface(
        onClick = onClick,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            leadingContent = {
                SettingsIconBadge(
                    icon = icon,
                    container = if (granted) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.tertiaryContainer,
                    content = if (granted) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onTertiaryContainer
                )
            },
            headlineContent = { Text(title) },
            supportingContent = { Text(subtitle) },
            trailingContent = {
                Surface(
                    shape = CircleShape,
                    color = if (granted) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = if (granted) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (granted) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                            contentDescription = badgeLabel,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = badgeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }
        )
    }
}
