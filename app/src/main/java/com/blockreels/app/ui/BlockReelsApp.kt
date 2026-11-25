// SPDX-License-Identifier: MIT
package com.blockreels.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blockreels.app.ui.components.FloatingBottomNavBar
import com.blockreels.app.ui.components.FloatingNavDestination
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BlockReelsApp(viewModel: BlockReelsViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    var selectedTab by remember { mutableStateOf(MainTab.SHIELD) }
    var modalScreen by remember { mutableStateOf(ModalScreen.NONE) }
    var pendingPinAction by remember { mutableStateOf<PendingPinAction?>(null) }

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

    val guardWithPinIfNeeded: (String, String, () -> Unit) -> Unit = remember(viewModel) {
        { title, subtitle, action ->
            if (viewModel.uiState.value.settings.pinProtected) {
                pendingPinAction = PendingPinAction(title, subtitle, action)
            } else {
                action()
            }
        }
    }

    val onToggleMaster: (Boolean) -> Unit = remember(viewModel) {
        { enable ->
            if (!enable) {
                guardWithPinIfNeeded(
                    "Pause Shield?",
                    "Enter PIN to pause protection."
                ) { viewModel.toggleMasterProtection(false) }
            } else {
                viewModel.toggleMasterProtection(true)
            }
        }
    }

    val onToggleTargetGuarded: (String, Boolean) -> Unit = remember(viewModel) {
        { pkg, enable ->
            if (!enable) {
                guardWithPinIfNeeded(
                    "Disable Rule?",
                    "Enter PIN to relax blocking for this app."
                ) { viewModel.toggleTargetApp(pkg, false) }
            } else {
                viewModel.toggleTargetApp(pkg, true)
            }
        }
    }

    val onCancelQuickBoostGuarded: () -> Unit = remember(viewModel) {
        {
            guardWithPinIfNeeded(
                "Cancel Focus Lock?",
                "Enter PIN to end Focus Lock early."
            ) { viewModel.cancelQuickBoost() }
        }
    }

    val onApplyPolicyAndGoalsToAll = remember(viewModel) {
        { policy: com.blockreels.app.state.ContentBlockPolicy, hide: Boolean, reelsGoal: Int, feedGoal: Int ->
            viewModel.applyBlockingPolicyAndGoalsToAllApps(
                policy = policy,
                requestHide = hide,
                reelsGoalMinutes = reelsGoal,
                feedGoalMinutes = feedGoal
            )
        }
    }

    val onApplyOnboardingPolicy: () -> Unit = remember(viewModel) {
        {
            val s = viewModel.uiState.value.settings
            viewModel.applyBlockingPolicyAndGoalsToAllApps(
                policy = s.defaultContentPolicy,
                requestHide = s.defaultRequestHideSurfaces,
                reelsGoalMinutes = s.defaultReelsGoalMinutes,
                feedGoalMinutes = s.defaultFeedGoalMinutes,
                enableAllApps = false
            )
        }
    }

    val onToggleScheduleGuarded: (String, Boolean) -> Unit = remember(viewModel) {
        { id, active ->
            if (!active) {
                guardWithPinIfNeeded(
                    "Pause Schedule?",
                    "Enter PIN to pause this schedule."
                ) { viewModel.toggleSchedule(id, false) }
            } else {
                viewModel.toggleSchedule(id, true)
            }
        }
    }

    val onDeleteScheduleGuarded: (String) -> Unit = remember(viewModel) {
        { id ->
            guardWithPinIfNeeded(
                "Delete Schedule?",
                "Enter PIN to delete this schedule."
            ) { viewModel.deleteSchedule(id) }
        }
    }

    val onToggleStrictModeGuarded: (Boolean) -> Unit = remember(viewModel) {
        { enable ->
            if (!enable) {
                guardWithPinIfNeeded(
                    "Disable Strict Lock?",
                    "Enter PIN to re-enable hold-to-pass."
                ) { viewModel.setStrictMode(false) }
            } else {
                viewModel.setStrictMode(true)
            }
        }
    }

    val onRequestDisablePinGuarded: () -> Unit = remember(viewModel) {
        {
            guardWithPinIfNeeded(
                "Remove PIN Lock?",
                "Enter your current 4-digit PIN."
            ) { viewModel.disablePinLock() }
        }
    }

    val onNavigateToEngine: () -> Unit = remember { { selectedTab = MainTab.ENGINE } }
    val onNavigateToSchedules: () -> Unit = remember { { selectedTab = MainTab.SCHEDULES } }
    val onNavigateToStats: () -> Unit = remember { { selectedTab = MainTab.ACTIVITY } }
    val onNavigateToSettings: () -> Unit = remember { { selectedTab = MainTab.SETTINGS } }
    val onOpenBlockMessageScreen: () -> Unit = remember { { modalScreen = ModalScreen.MINDFUL_PREVIEW } }
    val onRequestPinSetup: () -> Unit = remember { { modalScreen = ModalScreen.PIN_SETUP } }
    val onCloseModal: () -> Unit = remember { { modalScreen = ModalScreen.NONE } }
    val onShowMessage: (String) -> Unit = remember(coroutineScope, snackbarHostState) {
        { msg -> coroutineScope.launch { snackbarHostState.showSnackbar(msg) } }
    }
    val onSelectBottomDestination: (FloatingNavDestination) -> Unit = remember(haptics) {
        { dest ->
            val next = MainTab.valueOf(dest.id)
            if (selectedTab != next) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                selectedTab = next
            }
        }
    }

    val navDestinations = remember {
        MainTab.entries.map { tab ->
            FloatingNavDestination(
                id = tab.name,
                label = tab.navLabel,
                selectedIcon = tab.selectedIcon,
                unselectedIcon = tab.unselectedIcon,
                testTag = tab.tag
            )
        }
    }

    BlockReelsTheme(
        themeMode = uiState.settings.themeMode,
        dynamicColorEnabled = uiState.settings.dynamicColorEnabled,
        colorSeed = uiState.settings.colorSeed,
        customPalette = uiState.settings.customPalette,
        contrastLevel = uiState.settings.contrastLevel,
        cornerStyle = uiState.settings.cornerStyle,
        fontScale = uiState.settings.fontScale,
        iconStyle = uiState.settings.iconStyle
    ) {
        when {
            uiState.isLoading -> {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ContainedLoadingIndicator()
                    }
                }
            }
            !uiState.settings.onboardingCompleted -> {
                OnboardingScreen(
                    uiState = uiState,
                    onOpenAccessibilitySettings = viewModel::openAccessibilitySettings,
                    onOpenBatterySettings = viewModel::openBatterySettings,
                    onToggleTarget = viewModel::toggleTargetApp,
                    onSelectPolicy = viewModel::updateDefaultContentPolicy,
                    onToggleRequestHide = viewModel::updateDefaultRequestHideSurfaces,
                    onChangeReelsGoal = viewModel::updateDefaultReelsGoalMinutes,
                    onChangeFeedGoal = viewModel::updateDefaultFeedGoalMinutes,
                    onApplyPolicyAndGoals = onApplyOnboardingPolicy,
                    onFinishOnboarding = viewModel::completeOnboarding
                )
            }
            modalScreen == ModalScreen.MINDFUL_PREVIEW -> {
                MindfulnessPreviewScreen(
                    holdSeconds = uiState.settings.holdDurationSeconds,
                    allowanceMinutes = uiState.settings.allowanceMinutes,
                    dailyPassLimit = uiState.settings.dailyPassLimit,
                    customHeadline = uiState.settings.customMessageHeadline,
                    customMessage = uiState.settings.customMessage,
                    contentPolicy = uiState.settings.defaultContentPolicy,
                    requestHideSurfaces = uiState.settings.defaultRequestHideSurfaces,
                    reelsGoalMinutes = uiState.settings.defaultReelsGoalMinutes,
                    feedGoalMinutes = uiState.settings.defaultFeedGoalMinutes,
                    audioAction = uiState.settings.audioAction,
                    strictMode = uiState.settings.strictMode,
                    hapticsEnabled = uiState.settings.hapticsEnabled,
                    onUpdateMessageCopy = viewModel::updateBlockMessageCopy,
                    onCompletedPreview = viewModel::recordPreviewInterception,
                    onBack = onCloseModal
                )
            }
            modalScreen == ModalScreen.PIN_SETUP -> {
                PinSetupScreen(
                    onPinConfirmed = { pin ->
                        viewModel.enablePinLock(pin)
                        modalScreen = ModalScreen.NONE
                    },
                    onCancel = onCloseModal
                )
            }
            else -> {
                val motionScheme = MaterialTheme.motionScheme
                val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val useNavRail = maxWidth >= 600.dp
                    val screenBottomPadding = if (useNavRail) 24.dp else 112.dp

                    Row(modifier = Modifier.fillMaxSize()) {
                        if (useNavRail) {
                            NavigationRail(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            ) {
                                MainTab.entries.forEach { tab ->
                                    val selected = selectedTab == tab
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = {
                                            if (!selected) {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                selectedTab = tab
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.navLabel
                                            )
                                        },
                                        label = { Text(tab.navLabel, maxLines = 1) },
                                        modifier = Modifier.testTag(tab.tag)
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                        ) {
                            Scaffold(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                                snackbarHost = {
                                    SnackbarHost(
                                        hostState = snackbarHostState,
                                        modifier = Modifier.padding(
                                            bottom = if (useNavRail) 8.dp else 92.dp
                                        )
                                    )
                                },
                                topBar = {
                                    TopAppBar(
                                        title = {
                                            AnimatedContent(
                                                targetState = selectedTab.title,
                                                transitionSpec = {
                                                    fadeIn(motionScheme.fastEffectsSpec()) togetherWith
                                                        fadeOut(motionScheme.fastEffectsSpec())
                                                },
                                                label = "topbar_title"
                                            ) { title ->
                                                Text(
                                                    text = title,
                                                    style = MaterialTheme.typography.headlineSmallEmphasized
                                                )
                                            }
                                        },
                                        colors = TopAppBarDefaults.topAppBarColors(
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                            titleContentColor = MaterialTheme.colorScheme.onSurface
                                        ),
                                        scrollBehavior = scrollBehavior
                                    )
                                }
                            ) { innerPadding ->
                                AnimatedContent(
                                    targetState = selectedTab,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding),
                                    transitionSpec = {
                                        val forward = targetState.ordinal >= initialState.ordinal
                                        val slideIn = slideInHorizontally(motionScheme.fastSpatialSpec()) { width ->
                                            if (forward) width / 16 else -width / 16
                                        }
                                        val slideOut = slideOutHorizontally(motionScheme.fastSpatialSpec()) { width ->
                                            if (forward) -width / 16 else width / 16
                                        }
                                        (fadeIn(motionScheme.fastEffectsSpec()) + slideIn) togetherWith
                                            (fadeOut(motionScheme.fastEffectsSpec()) + slideOut)
                                    },
                                    label = "main_tab_transition"
                                ) { tab ->
                                    when (tab) {
                                        MainTab.SHIELD -> DashboardScreen(
                                            uiState = uiState,
                                            bottomContentPadding = screenBottomPadding,
                                            onToggleMaster = onToggleMaster,
                                            onToggleTargetApp = onToggleTargetGuarded,
                                            onStartQuickBoost = viewModel::startQuickBoost,
                                            onCancelQuickBoost = onCancelQuickBoostGuarded,
                                            onRevokeAllowance = viewModel::revokeTemporaryAllowance,
                                            onOpenAccessibilitySettings = viewModel::openAccessibilitySettings,
                                            onNavigateToEngine = onNavigateToEngine,
                                            onNavigateToSchedules = onNavigateToSchedules,
                                            onNavigateToStats = onNavigateToStats,
                                            onNavigateToSettings = onNavigateToSettings
                                        )
                                        MainTab.ENGINE -> AppBlockerScreen(
                                            uiState = uiState,
                                            bottomContentPadding = screenBottomPadding,
                                            onToggleTarget = onToggleTargetGuarded,
                                            onUpdateTargetRule = viewModel::updateTargetRule,
                                            onUpdateDefaultContentPolicy = viewModel::updateDefaultContentPolicy,
                                            onUpdateDefaultRequestHide = viewModel::updateDefaultRequestHideSurfaces,
                                            onUpdateDefaultReelsGoal = viewModel::updateDefaultReelsGoalMinutes,
                                            onUpdateDefaultFeedGoal = viewModel::updateDefaultFeedGoalMinutes,
                                            onApplyPolicyAndGoalsToAll = onApplyPolicyAndGoalsToAll,
                                            onUpdateGlobalMode = viewModel::updateGlobalMode,
                                            onUpdateHoldDuration = viewModel::updateHoldDuration,
                                            onUpdateAllowanceMinutes = viewModel::updateAllowanceMinutes,
                                            onUpdateDailyPassLimit = viewModel::updateDailyPassLimit,
                                            onUpdateEntryDelaySeconds = viewModel::updateEntryDelaySeconds,
                                            onUpdateSensitivity = viewModel::updateSensitivity,
                                            onUpdateAudioAction = viewModel::updateAudioAction,
                                            onToggleHaptics = viewModel::setHapticsEnabled,
                                            onOpenBlockMessageScreen = onOpenBlockMessageScreen
                                        )
                                        MainTab.SCHEDULES -> SchedulesScreen(
                                            uiState = uiState,
                                            bottomContentPadding = screenBottomPadding,
                                            onUpsertSchedule = viewModel::upsertSchedule,
                                            onToggleSchedule = onToggleScheduleGuarded,
                                            onDeleteSchedule = onDeleteScheduleGuarded,
                                            onCancelQuickBoost = onCancelQuickBoostGuarded
                                        )
                                        MainTab.ACTIVITY -> StatisticsScreen(
                                            uiState = uiState,
                                            bottomContentPadding = screenBottomPadding,
                                            onSelectRange = viewModel::selectStatsRange,
                                            onClearHistory = viewModel::clearHistory
                                        )
                                        MainTab.SETTINGS -> SettingsScreen(
                                            uiState = uiState,
                                            bottomContentPadding = screenBottomPadding,
                                            onToggleStrictMode = onToggleStrictModeGuarded,
                                            onSelectTheme = viewModel::setThemeMode,
                                            onToggleDynamicColor = viewModel::setDynamicColorEnabled,
                                            onSelectColorSeed = viewModel::setColorSeed,
                                            onSaveCustomPalette = viewModel::setCustomPalette,
                                            onSelectContrast = viewModel::setContrastLevel,
                                            onSelectCornerStyle = viewModel::setCornerStyle,
                                            onSelectFontScale = viewModel::setFontScale,
                                            onSelectIconStyle = viewModel::setIconStyle,
                                            onRequestPinSetup = onRequestPinSetup,
                                            onRequestDisablePin = onRequestDisablePinGuarded,
                                            onOpenAccessibilitySettings = viewModel::openAccessibilitySettings,
                                            onOpenBatterySettings = viewModel::openBatterySettings,
                                            onOpenNotificationSettings = viewModel::openNotificationSettings,
                                            onExportBackup = viewModel::exportBackupJson,
                                            onImportBackup = viewModel::importBackupJson,
                                            onClearHistory = viewModel::clearHistory,
                                            onReplayOnboarding = viewModel::resetOnboarding,
                                            onShowMessage = onShowMessage
                                        )
                                    }
                                }
                            }

                            if (!useNavRail) {
                                FloatingBottomNavBar(
                                    destinations = navDestinations,
                                    selectedId = selectedTab.name,
                                    onSelect = onSelectBottomDestination,
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
