// SPDX-License-Identifier: MIT
package com.blockreels.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blockreels.app.ui.components.FloatingBottomNavBar
import com.blockreels.app.ui.components.FloatingNavItem
import com.blockreels.app.ui.screens.AppBlockerScreen
import com.blockreels.app.ui.screens.DashboardScreen
import com.blockreels.app.ui.screens.MindfulnessPreviewScreen
import com.blockreels.app.ui.screens.OnboardingScreen
import com.blockreels.app.ui.screens.PinSetupScreen
import com.blockreels.app.ui.screens.PinVerifyDialog
import com.blockreels.app.ui.screens.SchedulesScreen
import com.blockreels.app.ui.screens.SettingsScreen
import com.blockreels.app.ui.screens.StatisticsScreen
import com.blockreels.app.ui.theme.BlockReelsTheme
import com.blockreels.app.viewmodel.BlockReelsViewModel
import kotlinx.coroutines.launch

private enum class MainTab(
    val title: String,
    val navLabel: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    SHIELD("BlockReels", "Shield", Icons.Rounded.Shield, Icons.Outlined.Shield, "nav_dashboard"),
    ENGINE("Blocking Engine", "Engine", Icons.Rounded.Tune, Icons.Outlined.Tune, "nav_apps"),
    SCHEDULES("Schedules", "Schedules", Icons.Rounded.CalendarMonth, Icons.Outlined.CalendarMonth, "nav_schedules"),
    ACTIVITY("Activity", "Activity", Icons.Rounded.BarChart, Icons.Outlined.BarChart, "nav_stats"),
    SETTINGS("Settings", "Settings", Icons.Rounded.Settings, Icons.Outlined.Settings, "nav_settings")
}

private enum class ModalScreen {
    NONE,
    MINDFUL_PREVIEW,
    PIN_SETUP
}

private data class PendingPinAction(
    val title: String,
    val subtitle: String,
    val onAuthorized: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockReelsApp(viewModel: BlockReelsViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(MainTab.SHIELD) }
    var modalScreen by remember { mutableStateOf(ModalScreen.NONE) }
    var pendingPinAction by remember { mutableStateOf<PendingPinAction?>(null) }

    val navItems = remember {
        MainTab.entries.map { tab ->
            FloatingNavItem(
                id = tab.name,
                label = tab.navLabel,
                selectedIcon = tab.selectedIcon,
                unselectedIcon = tab.unselectedIcon,
                testTag = tab.tag
            )
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshSystemHealth()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(viewModel) {
        viewModel.snackbarMessages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    BackHandler(enabled = modalScreen == ModalScreen.NONE && selectedTab != MainTab.SHIELD) {
        selectedTab = MainTab.SHIELD
    }

    fun guardWithPinIfNeeded(title: String, subtitle: String, action: () -> Unit) {
        if (uiState.settings.pinProtected) {
            pendingPinAction = PendingPinAction(title, subtitle, action)
        } else {
            action()
        }
    }

    BlockReelsTheme(themeMode = uiState.settings.themeMode) {
        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            !uiState.settings.onboardingCompleted -> {
                OnboardingScreen(
                    uiState = uiState,
                    onOpenAccessibilitySettings = viewModel::openAccessibilitySettings,
                    onOpenBatterySettings = viewModel::openBatterySettings,
                    onToggleTarget = viewModel::toggleTargetApp,
                    onFinishOnboarding = viewModel::completeOnboarding
                )
            }
            modalScreen == ModalScreen.MINDFUL_PREVIEW -> {
                MindfulnessPreviewScreen(
                    holdSeconds = uiState.settings.holdDurationSeconds,
                    allowanceMinutes = uiState.settings.allowanceMinutes,
                    dailyPassLimit = uiState.settings.dailyPassLimit,
                    customMessage = uiState.settings.customMessage,
                    strictMode = uiState.settings.strictMode,
                    hapticsEnabled = uiState.settings.hapticsEnabled,
                    onCompletedPreview = viewModel::recordPreviewInterception,
                    onBack = { modalScreen = ModalScreen.NONE }
                )
            }
            modalScreen == ModalScreen.PIN_SETUP -> {
                PinSetupScreen(
                    onPinConfirmed = { pin ->
                        viewModel.enablePinLock(pin)
                        modalScreen = ModalScreen.NONE
                    },
                    onCancel = { modalScreen = ModalScreen.NONE }
                )
            }
            else -> {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val useNavRail = maxWidth >= 600.dp
                    val floatingBarClearance = if (useNavRail) 16.dp else 96.dp

                    Row(modifier = Modifier.fillMaxSize()) {
                        if (useNavRail) {
                            NavigationRail(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ) {
                                MainTab.entries.forEach { tab ->
                                    val selected = selectedTab == tab
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = { selectedTab = tab },
                                        alwaysShowLabel = false,
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.navLabel
                                            )
                                        },
                                        label = { Text(tab.navLabel) },
                                        modifier = Modifier.testTag(tab.tag)
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                            Scaffold(
                                modifier = Modifier.fillMaxSize(),
                                containerColor = MaterialTheme.colorScheme.background,
                                snackbarHost = {
                                    SnackbarHost(
                                        hostState = snackbarHostState,
                                        modifier = Modifier.padding(
                                            bottom = if (useNavRail) 0.dp else 80.dp
                                        )
                                    )
                                },
                                topBar = {
                                    CenterAlignedTopAppBar(
                                        title = {
                                            AnimatedContent(
                                                targetState = selectedTab.title,
                                                transitionSpec = {
                                                    fadeIn(tween(180)) togetherWith fadeOut(tween(120))
                                                },
                                                label = "topbar_title"
                                            ) { title ->
                                                Text(
                                                    text = title,
                                                    style = MaterialTheme.typography.titleLarge
                                                )
                                            }
                                        },
                                        colors = TopAppBarDefaults.topAppBarColors(
                                            containerColor = MaterialTheme.colorScheme.background
                                        )
                                    )
                                }
                            ) { innerPadding ->
                                val contentPadding = PaddingValues(
                                    top = innerPadding.calculateTopPadding(),
                                    bottom = innerPadding.calculateBottomPadding()
                                )

                                AnimatedContent(
                                    targetState = selectedTab,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(contentPadding),
                                    transitionSpec = {
                                        val forward = targetState.ordinal >= initialState.ordinal
                                        val slideIn = slideInHorizontally(tween(220)) { width ->
                                            if (forward) width / 14 else -width / 14
                                        }
                                        val slideOut = slideOutHorizontally(tween(180)) { width ->
                                            if (forward) -width / 14 else width / 14
                                        }
                                        (fadeIn(tween(200)) + slideIn) togetherWith
                                            (fadeOut(tween(140)) + slideOut)
                                    },
                                    label = "main_tab_transition"
                                ) { tab ->
                                    when (tab) {
                                        MainTab.SHIELD -> DashboardScreen(
                                            uiState = uiState,
                                            bottomContentPadding = floatingBarClearance,
                                            onToggleMaster = { enable ->
                                                if (!enable) {
                                                    guardWithPinIfNeeded(
                                                        title = "Pause Shield?",
                                                        subtitle = "Enter PIN to pause protection."
                                                    ) { viewModel.toggleMasterProtection(false) }
                                                } else {
                                                    viewModel.toggleMasterProtection(true)
                                                }
                                            },
                                            onToggleTargetApp = { pkg, enable ->
                                                if (!enable) {
                                                    guardWithPinIfNeeded(
                                                        title = "Disable Rule?",
                                                        subtitle = "Enter PIN to relax blocking for this app."
                                                    ) { viewModel.toggleTargetApp(pkg, false) }
                                                } else {
                                                    viewModel.toggleTargetApp(pkg, true)
                                                }
                                            },
                                            onStartQuickBoost = viewModel::startQuickBoost,
                                            onCancelQuickBoost = {
                                                guardWithPinIfNeeded(
                                                    title = "Cancel Focus Lock?",
                                                    subtitle = "Enter PIN to end Focus Lock early."
                                                ) { viewModel.cancelQuickBoost() }
                                            },
                                            onRevokeAllowance = viewModel::revokeTemporaryAllowance,
                                            onOpenAccessibilitySettings = viewModel::openAccessibilitySettings,
                                            onNavigateToEngine = { selectedTab = MainTab.ENGINE },
                                            onNavigateToStats = { selectedTab = MainTab.ACTIVITY }
                                        )
                                        MainTab.ENGINE -> AppBlockerScreen(
                                            uiState = uiState,
                                            bottomContentPadding = floatingBarClearance,
                                            onToggleTarget = { pkg, enable ->
                                                if (!enable) {
                                                    guardWithPinIfNeeded(
                                                        title = "Disable Rule?",
                                                        subtitle = "Enter PIN to disable blocking for this target."
                                                    ) { viewModel.toggleTargetApp(pkg, false) }
                                                } else {
                                                    viewModel.toggleTargetApp(pkg, true)
                                                }
                                            },
                                            onUpdateTargetRule = viewModel::updateTargetRule,
                                            onUpdateGlobalMode = viewModel::updateGlobalMode,
                                            onUpdateHoldDuration = viewModel::updateHoldDuration,
                                            onUpdateAllowanceMinutes = viewModel::updateAllowanceMinutes,
                                            onUpdateDailyPassLimit = viewModel::updateDailyPassLimit,
                                            onUpdateEntryDelaySeconds = viewModel::updateEntryDelaySeconds,
                                            onUpdateSensitivity = viewModel::updateSensitivity,
                                            onUpdateAudioAction = viewModel::updateAudioAction,
                                            onUpdateCustomMessage = viewModel::updateCustomMessage,
                                            onToggleHaptics = viewModel::setHapticsEnabled,
                                            onOpenPreviewOverlay = { modalScreen = ModalScreen.MINDFUL_PREVIEW }
                                        )
                                        MainTab.SCHEDULES -> SchedulesScreen(
                                            uiState = uiState,
                                            bottomContentPadding = floatingBarClearance,
                                            onUpsertSchedule = viewModel::upsertSchedule,
                                            onToggleSchedule = { id, active ->
                                                if (!active) {
                                                    guardWithPinIfNeeded(
                                                        title = "Pause Schedule?",
                                                        subtitle = "Enter PIN to pause this schedule."
                                                    ) { viewModel.toggleSchedule(id, false) }
                                                } else {
                                                    viewModel.toggleSchedule(id, true)
                                                }
                                            },
                                            onDeleteSchedule = { id ->
                                                guardWithPinIfNeeded(
                                                    title = "Delete Schedule?",
                                                    subtitle = "Enter PIN to delete this schedule."
                                                ) { viewModel.deleteSchedule(id) }
                                            },
                                            onCancelQuickBoost = {
                                                guardWithPinIfNeeded(
                                                    title = "Cancel Focus Lock?",
                                                    subtitle = "Enter PIN to end Focus Lock early."
                                                ) { viewModel.cancelQuickBoost() }
                                            }
                                        )
                                        MainTab.ACTIVITY -> StatisticsScreen(
                                            uiState = uiState,
                                            bottomContentPadding = floatingBarClearance,
                                            onSelectRange = viewModel::selectStatsRange,
                                            onClearHistory = viewModel::clearHistory
                                        )
                                        MainTab.SETTINGS -> SettingsScreen(
                                            uiState = uiState,
                                            bottomContentPadding = floatingBarClearance,
                                            onToggleStrictMode = { enable ->
                                                if (!enable) {
                                                    guardWithPinIfNeeded(
                                                        title = "Disable Strict Lock?",
                                                        subtitle = "Enter PIN to re-enable hold-to-pass."
                                                    ) { viewModel.setStrictMode(false) }
                                                } else {
                                                    viewModel.setStrictMode(true)
                                                }
                                            },
                                            onSelectTheme = viewModel::setThemeMode,
                                            onRequestPinSetup = { modalScreen = ModalScreen.PIN_SETUP },
                                            onRequestDisablePin = {
                                                guardWithPinIfNeeded(
                                                    title = "Remove PIN Lock?",
                                                    subtitle = "Enter your current 4-digit PIN."
                                                ) { viewModel.disablePinLock() }
                                            },
                                            onOpenAccessibilitySettings = viewModel::openAccessibilitySettings,
                                            onOpenBatterySettings = viewModel::openBatterySettings,
                                            onOpenNotificationSettings = viewModel::openNotificationSettings,
                                            onExportBackup = viewModel::exportBackupJson,
                                            onImportBackup = viewModel::importBackupJson,
                                            onClearHistory = viewModel::clearHistory,
                                            onReplayOnboarding = viewModel::resetOnboarding,
                                            onShowMessage = { msg ->
                                                coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                                            }
                                        )
                                    }
                                }
                            }

                            if (!useNavRail) {
                                FloatingBottomNavBar(
                                    items = navItems,
                                    selectedId = selectedTab.name,
                                    onSelect = { id ->
                                        MainTab.entries.firstOrNull { it.name == id }?.let { selectedTab = it }
                                    },
                                    modifier = Modifier.align(Alignment.BottomCenter)
                                )
                            }
                        }
                    }
                }
            }
        }

        pendingPinAction?.let { pending ->
            PinVerifyDialog(
                title = pending.title,
                subtitle = pending.subtitle,
                onVerify = viewModel::verifyPin,
                onSuccess = {
                    pendingPinAction = null
                    pending.onAuthorized()
                },
                onDismiss = { pendingPinAction = null }
            )
        }
    }
}
