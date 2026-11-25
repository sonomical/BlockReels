// SPDX-License-Identifier: MIT
package com.blockreels.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blockreels.app.BuildConfig
import com.blockreels.app.data.AppColorSeed
import com.blockreels.app.data.AppContrastLevel
import com.blockreels.app.data.AppCornerStyle
import com.blockreels.app.data.AppFontScale
import com.blockreels.app.data.AppIconStyle
import com.blockreels.app.data.AppThemeMode
import com.blockreels.app.data.CustomPaletteConfig
import com.blockreels.app.ui.components.AccessibilityDisclosureDialog
import com.blockreels.app.ui.components.ConnectedToggleButtonGroup
import com.blockreels.app.ui.components.CustomPaletteEditorSheet
import com.blockreels.app.ui.components.ExpressiveShapes
import com.blockreels.app.ui.components.ExpressiveSwitch
import com.blockreels.app.ui.components.PlatformAvatar
import com.blockreels.app.ui.components.SectionHeader
import com.blockreels.app.ui.components.segmentedListShape
import com.blockreels.app.viewmodel.BlockReelsUiState

private val THEME_PREVIEW_PLATFORMS = listOf(
    "INSTAGRAM" to "com.instagram.android",
    "YOUTUBE" to "com.google.android.youtube",
    "TIKTOK" to "com.zhiliaoapp.musically",
    "FACEBOOK" to "com.facebook.katana",
    "SNAPCHAT" to "com.snapchat.android"
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    uiState: BlockReelsUiState,
    bottomContentPadding: Dp = 24.dp,
    onToggleStrictMode: (Boolean) -> Unit,
    onSelectTheme: (AppThemeMode) -> Unit,
    onToggleDynamicColor: (Boolean) -> Unit,
    onSelectColorSeed: (AppColorSeed) -> Unit,
    onSaveCustomPalette: (CustomPaletteConfig) -> Unit,
    onSelectContrast: (AppContrastLevel) -> Unit,
    onSelectCornerStyle: (AppCornerStyle) -> Unit,
    onSelectFontScale: (AppFontScale) -> Unit,
    onSelectIconStyle: (AppIconStyle) -> Unit,
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
    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    var showImportDialog by remember { mutableStateOf(false) }
    var showDisclosureDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showLicenseDialog by remember { mutableStateOf(false) }
    var showCustomPaletteSheet by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomContentPadding),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Complete Material 3 Expressive Appearance & Theme Studio
            item(key = "appearance_section") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(title = "Appearance & Theme Studio")

                    // Live M3 Theme Preview Banner
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = ExpressiveShapes.Cookie7Sided,
                                        color = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Rounded.Palette,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = if (uiState.settings.dynamicColorEnabled && supportsDynamicColor) {
                                                "Material You · ${uiState.settings.themeMode.label}"
                                            } else {
                                                "${uiState.settings.colorSeed.label} · ${uiState.settings.themeMode.label}"
                                            },
                                            style = MaterialTheme.typography.titleMediumEmphasized
                                        )
                                        Text(
                                            text = "${uiState.settings.cornerStyle.label} corners · ${uiState.settings.contrastLevel.label} contrast · ${uiState.settings.fontScale.label} text",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                THEME_PREVIEW_PLATFORMS.forEach { (platformKey, pkg) ->
                                    PlatformAvatar(
                                        platformKey = platformKey,
                                        packageName = pkg,
                                        sizeDp = 34,
                                        active = true
                                    )
                                }
                            }
                        }
                    }

                    // Theme Controls Segmented Stack
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        // 1. Light / Dark / AMOLED / System
                        Surface(
                            shape = segmentedListShape(0, 7),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("Theme Mode", style = MaterialTheme.typography.titleSmall)
                                ConnectedToggleButtonGroup(
                                    options = AppThemeMode.entries,
                                    selected = uiState.settings.themeMode,
                                    onSelect = onSelectTheme,
                                    label = { it.label },
                                    tagFor = { "settings_theme_${it.name.lowercase()}" }
                                )
                            }
                        }

                        // 2. Dynamic Color (Material You)
                        Surface(
                            shape = segmentedListShape(1, 7),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                leadingContent = {
                                    SettingsIconBadge(
                                        icon = Icons.Rounded.Wallpaper,
                                        container = MaterialTheme.colorScheme.secondaryContainer,
                                        content = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                },
                                headlineContent = { Text("Dynamic Color (Wallpaper)") },
                                supportingContent = {
                                    Text(
                                        if (supportsDynamicColor) "Match your Android wallpaper colors"
                                        else "Requires Android 12 or newer"
                                    )
                                },
                                trailingContent = {
                                    ExpressiveSwitch(
                                        checked = uiState.settings.dynamicColorEnabled && supportsDynamicColor,
                                        enabled = supportsDynamicColor,
                                        onCheckedChange = onToggleDynamicColor,
                                        modifier = Modifier.testTag("settings_dynamic_color_switch")
                                    )
                                }
                            )
                        }

                        // 3. Color Palette Seed Swatches
                        Surface(
                            shape = segmentedListShape(2, 7),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Accent Color Palette", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = if (uiState.settings.dynamicColorEnabled && supportsDynamicColor) {
                                            "Wallpaper"
                                        } else {
                                            uiState.settings.colorSeed.label
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    AppColorSeed.entries.forEach { seed ->
                                        val selected = !uiState.settings.dynamicColorEnabled &&
                                            uiState.settings.colorSeed == seed
                                        if (seed == AppColorSeed.CUSTOM) {
                                            CustomPaletteSwatch(
                                                palette = uiState.settings.customPalette,
                                                selected = selected,
                                                onClick = {
                                                    if (selected) {
                                                        showCustomPaletteSheet = true
                                                    } else {
                                                        onSelectColorSeed(AppColorSeed.CUSTOM)
                                                    }
                                                }
                                            )
                                        } else {
                                            ColorSeedSwatch(
                                                seed = seed,
                                                selected = selected,
                                                onClick = { onSelectColorSeed(seed) }
                                            )
                                        }
                                    }
                                }

                                FilledTonalButton(
                                    onClick = { showCustomPaletteSheet = true },
                                    shapes = ButtonDefaults.shapes(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("settings_customize_palette_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Tune,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(8.dp))
                                    Text(
                                        if (uiState.settings.colorSeed == AppColorSeed.CUSTOM && !uiState.settings.dynamicColorEnabled) {
                                            "Edit Custom Color Palette"
                                        } else {
                                            "Create Custom Color Palette"
                                        }
                                    )
                                }
                            }
                        }

                        // 4. Contrast Level
                        Surface(
                            shape = segmentedListShape(3, 7),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("Contrast Level", style = MaterialTheme.typography.titleSmall)
                                ConnectedToggleButtonGroup(
                                    options = AppContrastLevel.entries,
                                    selected = uiState.settings.contrastLevel,
                                    onSelect = onSelectContrast,
                                    label = { it.label },
                                    tagFor = { "settings_contrast_${it.name.lowercase()}" }
                                )
                            }
                        }

                        // 5. Corner / Shape Style
                        Surface(
                            shape = segmentedListShape(4, 7),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("Shape & Corners", style = MaterialTheme.typography.titleSmall)
                                ConnectedToggleButtonGroup(
                                    options = AppCornerStyle.entries,
                                    selected = uiState.settings.cornerStyle,
                                    onSelect = onSelectCornerStyle,
                                    label = { it.label },
                                    tagFor = { "settings_corners_${it.name.lowercase()}" }
                                )
                            }
                        }

                        // 6. Typography Scale
                        Surface(
                            shape = segmentedListShape(5, 7),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("Typography Size", style = MaterialTheme.typography.titleSmall)
                                ConnectedToggleButtonGroup(
                                    options = AppFontScale.entries,
                                    selected = uiState.settings.fontScale,
                                    onSelect = onSelectFontScale,
                                    label = { it.label },
                                    tagFor = { "settings_font_${it.name.lowercase()}" }
                                )
                            }
                        }

                        // 7. Themed App Icon Shape & Tone
                        Surface(
                            shape = segmentedListShape(6, 7),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("Themed App Icons", style = MaterialTheme.typography.titleSmall)
                                ConnectedToggleButtonGroup(
                                    options = AppIconStyle.entries,
                                    selected = uiState.settings.iconStyle,
                                    onSelect = onSelectIconStyle,
                                    label = { it.label },
                                    tagFor = { "settings_icon_style_${it.name.lowercase()}" }
                                )
                            }
                        }
                    }
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
                                supportingContent = { Text("Copy rules, goals, theme, and schedules") }
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
                                shape = ExpressiveShapes.Cookie7Sided,
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
    if (showCustomPaletteSheet) {
        CustomPaletteEditorSheet(
            initial = uiState.settings.customPalette,
            amoled = uiState.settings.themeMode == AppThemeMode.AMOLED,
            contrastLevel = uiState.settings.contrastLevel,
            onDismiss = { showCustomPaletteSheet = false },
            onApply = { updated ->
                onSaveCustomPalette(updated)
                showCustomPaletteSheet = false
            }
        )
    }
}

@Composable
private fun CustomPaletteSwatch(
    palette: CustomPaletteConfig,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.testTag("settings_seed_custom")
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(palette.primaryArgb),
                    modifier = Modifier.size(16.dp)
                ) {}
                Surface(
                    shape = CircleShape,
                    color = Color(palette.secondaryArgb),
                    modifier = Modifier.size(16.dp)
                ) {}
                Surface(
                    shape = CircleShape,
                    color = Color(palette.tertiaryArgb),
                    modifier = Modifier.size(16.dp)
                ) {}
            }
            Text(
                text = "Custom",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ColorSeedSwatch(
    seed: AppColorSeed,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.testTag("settings_seed_${seed.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier.padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color(seed.swatchHex),
                contentColor = Color.White,
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (selected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Selected ${seed.label}",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Text(
                text = seed.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface
            )
        }
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
