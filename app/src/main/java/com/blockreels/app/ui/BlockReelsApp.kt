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
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Settings
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
    DASHBOARD("BlockReels", "Home", Icons.Rounded.Dashboard, Icons.Outlined.Dashboard, "nav_dashboard"),
    APPS("Guarded Apps", "Apps", Icons.Rounded.Tune, Icons.Outlined.Tune, "nav_apps"),
    SCHEDULES("Focus Schedules", "Schedules", Icons.Rounded.CalendarMonth, Icons.Outlined.CalendarMonth, "nav_schedules"),
    STATS("Focus Insights", "Stats", Icons.Rounded.BarChart, Icons.Outlined.BarChart, "nav_stats"),
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

    var selectedTab by remember { mutableStateOf(MainTab.DASHBOARD) }
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

    BackHandler(enabled = modalScreen == ModalScreen.NONE && selectedTab != MainTab.DASHBOARD) {
        selectedTab = MainTab.DASHBOARD
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
                                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
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
                                        MainTab.DASHBOARD -> DashboardScreen(
                                            uiState = uiState,
                                            bottomContentPadding = floatingBarClearance,
                                            onToggleMaster = { enable ->
                                                if (!enable) {
                                                    guardWithPinIfNeeded(
                                                        title = "Pause Protection?",
                                                        subtitle = "Enter your 4-digit PIN to pause the focus shield."
                                                    ) { viewModel.toggleMasterProtection(false) }
                                                } else {
                                                    viewModel.toggleMasterProtection(true)
                                                }
                                            },
                                            onToggleTargetApp = { pkg, enable ->
                                                if (!enable) {
                                                    guardWithPinIfNeeded(
                                                        title = "Disable Feed Blocking?",
                                                        subtitle = "Enter your PIN to relax blocking for this app."
                                                    ) { viewModel.toggleTargetApp(pkg, false) }
                                                } else {
                                                    viewModel.toggleTargetApp(pkg, true)
                                                }
                                            },
                                            onStartQuickBoost = viewModel::startQuickBoost,
                                            onCancelQuickBoost = {
                                                guardWithPinIfNeeded(
                                                    title = "Cancel Focus Boost?",
                                                    subtitle = "Enter your PIN to end the active Focus Boost early."
                                                ) { viewModel.cancelQuickBoost() }
                                            },
                                            onOpenAccessibilitySettings = viewModel::openAccessibilitySettings,
                                            onOpenPreviewOverlay = { modalScreen = ModalScreen.MINDFUL_PREVIEW },
                                            onNavigateToApps = { selectedTab = MainTab.APPS },
                                            onNavigateToStats = { selectedTab = MainTab.STATS }
                                        )
                                        MainTab.APPS -> AppBlockerScreen(
                                            uiState = uiState,
                                            bottomContentPadding = floatingBarClearance,
                                            onToggleTarget = { pkg, enable ->
                                                if (!enable) {
                                                    guardWithPinIfNeeded(
                                                        title = "Disable Feed Rule?",
                                                        subtitle = "Enter your PIN to turn off blocking for this platform."
                                                    ) { viewModel.toggleTargetApp(pkg, false) }
                                                } else {
                                                    viewModel.toggleTargetApp(pkg, true)
                                                }
                                            },
                                            onChangeTargetMode = viewModel::setTargetAppMode,
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
                                                        subtitle = "Enter your PIN to disable this focus schedule."
                                                    ) { viewModel.toggleSchedule(id, false) }
                                                } else {
                                                    viewModel.toggleSchedule(id, true)
                                                }
                                            },
                                            onDeleteSchedule = { id ->
                                                guardWithPinIfNeeded(
                                                    title = "Delete Schedule?",
                                                    subtitle = "Enter your PIN to remove this focus schedule."
                                                ) { viewModel.deleteSchedule(id) }
                                            },
                                            onCancelQuickBoost = {
                                                guardWithPinIfNeeded(
                                                    title = "Cancel Focus Boost?",
                                                    subtitle = "Enter your PIN to end the active Focus Boost."
                                                ) { viewModel.cancelQuickBoost() }
                                            }
                                        )
                                        MainTab.STATS -> StatisticsScreen(
                                            uiState = uiState,
                                            bottomContentPadding = floatingBarClearance,
                                            onSelectRange = viewModel::selectStatsRange,
                                            onClearHistory = viewModel::clearHistory
                                        )
                                        MainTab.SETTINGS -> SettingsScreen(
                                            uiState = uiState,
                                            bottomContentPadding = floatingBarClearance,
                                            onUpdateGlobalMode = viewModel::updateGlobalMode,
                                            onUpdateHoldDuration = viewModel::updateHoldDuration,
                                            onUpdateCustomMessage = viewModel::updateCustomMessage,
                                            onToggleSound = viewModel::setSoundEnabled,
                                            onToggleHaptics = viewModel::setHapticsEnabled,
                                            onToggleStrictMode = { enable ->
                                                if (!enable) {
                                                    guardWithPinIfNeeded(
                                                        title = "Disable Strict Mode?",
                                                        subtitle = "Enter your PIN to re-enable temporary hold bypasses."
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
                                                    subtitle = "Enter your current 4-digit PIN to turn off PIN protection."
                                                ) { viewModel.disablePinLock() }
                                            },
                                            onOpenPreviewOverlay = { modalScreen = ModalScreen.MINDFUL_PREVIEW },
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
