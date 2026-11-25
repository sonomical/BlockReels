// SPDX-License-Identifier: MIT
package com.blockreels.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.ViewStream
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.blockreels.app.R
import com.blockreels.app.data.TargetAppConfig
import com.blockreels.app.state.ContentBlockPolicy
import com.blockreels.app.ui.components.ConnectedToggleButtonGroup
import com.blockreels.app.ui.components.ExpressiveCard
import com.blockreels.app.ui.components.ExpressiveNumberSelector
import com.blockreels.app.ui.components.ExpressiveSwitch
import com.blockreels.app.ui.components.PlatformAvatar
import com.blockreels.app.ui.components.segmentedListShape
import com.blockreels.app.ui.theme.MonoFontFamily
import com.blockreels.app.viewmodel.BlockReelsUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnboardingScreen(
    uiState: BlockReelsUiState,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onToggleTarget: (String, Boolean) -> Unit,
    onSelectPolicy: (ContentBlockPolicy) -> Unit,
    onToggleRequestHide: (Boolean) -> Unit,
    onChangeReelsGoal: (Int) -> Unit,
    onChangeFeedGoal: (Int) -> Unit,
    onApplyPolicyAndGoals: () -> Unit,
    onFinishOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pageCount = 4
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val scope = rememberCoroutineScope()
    val motionScheme = MaterialTheme.motionScheme

    BackHandler(enabled = pagerState.currentPage > 0) {
        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ) {
                            Text(
                                text = "STEP ${pagerState.currentPage + 1} OF $pageCount",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = MonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                        if (pagerState.currentPage < pageCount - 1) {
                            TextButton(
                                onClick = {
                                    onApplyPolicyAndGoals()
                                    onFinishOnboarding()
                                },
                                modifier = Modifier.testTag("onboarding_skip_btn")
                            ) {
                                Text("Skip setup")
                            }
                        } else {
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }

                    // Expressive 4-segment progress track
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        repeat(pageCount) { idx ->
                            val reached = idx <= pagerState.currentPage
                            val barColor by animateColorAsState(
                                targetValue = if (reached) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                                animationSpec = motionScheme.defaultEffectsSpec(),
                                label = "onboarding_step_bar_$idx"
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(5.dp)
                                    .clip(CircleShape)
                                    .background(barColor)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 560.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (pagerState.currentPage > 0) {
                            FilledTonalIconButton(
                                onClick = {
                                    scope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                },
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier
                                    .size(56.dp)
                                    .testTag("onboarding_back_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Previous step"
                                )
                            }
                        }

                        val isLastPage = pagerState.currentPage == pageCount - 1
                        Button(
                            onClick = {
                                if (!isLastPage) {
                                    if (pagerState.currentPage == 2) {
                                        onApplyPolicyAndGoals()
                                    }
                                    scope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                } else {
                                    onApplyPolicyAndGoals()
                                    onFinishOnboarding()
                                }
                            },
                            shapes = ButtonDefaults.shapes(),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .testTag("onboarding_next_btn")
                        ) {
                            Text(
                                text = when (pagerState.currentPage) {
                                    0 -> "Begin Setup"
                                    1 -> "Set Guardrails"
                                    2 -> "Continue to Shield"
                                    else -> if (uiState.systemHealth.accessibilityEnabled) {
                                        "Enter Dashboard"
                                    } else {
                                        "Finish Setup"
                                    }
                                },
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp)
                        .padding(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    when (page) {
                        0 -> WelcomeStep()
                        1 -> TargetAppsStep(
                            targets = uiState.targets,
                            onToggleTarget = onToggleTarget
                        )
                        2 -> GuardrailsAndGoalsStep(
                            policy = uiState.settings.defaultContentPolicy,
                            requestHide = uiState.settings.defaultRequestHideSurfaces,
                            reelsGoalMinutes = uiState.settings.defaultReelsGoalMinutes,
                            feedGoalMinutes = uiState.settings.defaultFeedGoalMinutes,
                            onSelectPolicy = onSelectPolicy,
                            onToggleRequestHide = onToggleRequestHide,
                            onChangeReelsGoal = onChangeReelsGoal,
                            onChangeFeedGoal = onChangeFeedGoal
                        )
                        3 -> LiveShieldPermissionsStep(
                            accessibilityEnabled = uiState.systemHealth.accessibilityEnabled,
                            batteryIgnored = uiState.systemHealth.batteryOptimizationIgnored,
                            onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                            onOpenBatterySettings = onOpenBatterySettings
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WelcomeStep() {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_onboarding_welcome),
            contentDescription = "Calm editorial illustration representing reclaimed time and focus",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.VerifiedUser,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "ON-DEVICE · ZERO TRACKING",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = MonoFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Text(
            text = "Reclaim your attention.",
            style = MaterialTheme.typography.displaySmallEmphasized,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Block endless Reels, Shorts, and algorithmic feeds while keeping messaging, search, and useful tools completely open.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    val highlights = listOf(
        Triple(
            Icons.Rounded.Shield,
            "Surgical feed blocking",
            "Block Reels, main feeds, or both across every supported app."
        ),
        Triple(
            Icons.Rounded.Flag,
            "Daily consumption goals",
            "Set exact minute limits or quick presets with mindful pause friction."
        ),
        Triple(
            Icons.Rounded.Lock,
            "Private by design",
            "Zero network permissions, zero cloud telemetry, 100% local."
        )
    )

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        highlights.forEachIndexed { idx, (icon, title, subtitle) ->
            Surface(
                shape = segmentedListShape(idx, highlights.size),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = MaterialShapes.Cookie7Sided.toShape(),
                        color = if (idx == 0) MaterialTheme.colorScheme.primaryContainer
                        else if (idx == 1) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = if (idx == 0) MaterialTheme.colorScheme.onPrimaryContainer
                        else if (idx == 1) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onTertiaryContainer,
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
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
private fun TargetAppsStep(
    targets: List<TargetAppConfig>,
    onToggleTarget: (String, Boolean) -> Unit
) {
    val selectedCount = targets.count { it.isEnabled }

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_onboarding_apps_goals),
            contentDescription = "Editorial illustration of balanced apps and focus habits",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Pick what pulls you in.",
            style = MaterialTheme.typography.headlineLargeEmphasized,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "$selectedCount of ${targets.size} supported apps selected. All icons follow your active Material 3 theme.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FilterChip(
            selected = selectedCount == targets.size,
            onClick = {
                targets.forEach { if (!it.isEnabled) onToggleTarget(it.packageName, true) }
            },
            label = { Text("Guard All (${targets.size})") },
            modifier = Modifier.testTag("onboarding_select_all_apps")
        )
        FilterChip(
            selected = false,
            onClick = {
                val essentials = setOf(
                    "com.instagram.android",
                    "com.google.android.youtube",
                    "com.zhiliaoapp.musically"
                )
                targets.forEach { target ->
                    val shouldEnable = target.packageName in essentials
                    if (target.isEnabled != shouldEnable) {
                        onToggleTarget(target.packageName, shouldEnable)
                    }
                }
            },
            label = { Text("Top 3 Essentials") },
            modifier = Modifier.testTag("onboarding_select_essentials")
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        targets.forEachIndexed { idx, target ->
            Surface(
                shape = segmentedListShape(idx, targets.size),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PlatformAvatar(
                        platformKey = target.platformKey,
                        packageName = target.packageName,
                        sizeDp = 42,
                        active = target.isEnabled
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(target.displayName, style = MaterialTheme.typography.titleSmall)
                            if (target.isInstalled) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ) {
                                    Text(
                                        text = "Installed",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            target.feedName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    ExpressiveSwitch(
                        checked = target.isEnabled,
                        onCheckedChange = { onToggleTarget(target.packageName, it) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GuardrailsAndGoalsStep(
    policy: ContentBlockPolicy,
    requestHide: Boolean,
    reelsGoalMinutes: Int,
    feedGoalMinutes: Int,
    onSelectPolicy: (ContentBlockPolicy) -> Unit,
    onToggleRequestHide: (Boolean) -> Unit,
    onChangeReelsGoal: (Int) -> Unit,
    onChangeFeedGoal: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            shape = MaterialShapes.Cookie7Sided.toShape(),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Text(
            text = "Design your guardrails.",
            style = MaterialTheme.typography.headlineLargeEmphasized,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Pick what to block and set realistic daily goals. Use quick presets or tap any number pill to enter an exact limit.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    ExpressiveCard(shape = MaterialTheme.shapes.extraLarge) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Blocking Scope",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                ConnectedToggleButtonGroup(
                    options = ContentBlockPolicy.entries,
                    selected = policy,
                    onSelect = onSelectPolicy,
                    label = { it.shortLabel },
                    tagFor = { "onboarding_policy_${it.name.lowercase()}" }
                )
                Text(
                    text = policy.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Rounded.VisibilityOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    headlineContent = {
                        Text("Request not to see them", style = MaterialTheme.typography.titleSmall)
                    },
                    supportingContent = {
                        Text(
                            text = "Dismiss detected feeds automatically",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        ExpressiveSwitch(
                            checked = requestHide || policy == ContentBlockPolicy.HIDE_SURFACES,
                            onCheckedChange = onToggleRequestHide,
                            modifier = Modifier.testTag("onboarding_hide_switch")
                        )
                    }
                )
            }

            ExpressiveNumberSelector(
                title = "Reels / Shorts daily goal",
                value = reelsGoalMinutes,
                range = 0..180,
                step = 5,
                quickPresets = listOf(0, 10, 15, 30, 45, 60),
                unitPluralLabel = "min",
                valueFormatter = { if (it == 0) "0m (Block)" else "${it}m / day" },
                presetLabel = { if (it == 0) "0m" else "${it}m" },
                leadingIcon = Icons.Rounded.PlayCircle,
                tagPrefix = "onboarding_reels_goal",
                onValueChange = onChangeReelsGoal
            )

            ExpressiveNumberSelector(
                title = "Main Feed daily goal",
                value = feedGoalMinutes,
                range = 0..180,
                step = 5,
                quickPresets = listOf(0, 15, 20, 30, 45, 60),
                unitPluralLabel = "min",
                valueFormatter = { if (it == 0) "0m (Block)" else "${it}m / day" },
                presetLabel = { if (it == 0) "0m" else "${it}m" },
                leadingIcon = Icons.Rounded.ViewStream,
                accentSecondary = true,
                tagPrefix = "onboarding_feed_goal",
                onValueChange = onChangeFeedGoal
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LiveShieldPermissionsStep(
    accessibilityEnabled: Boolean,
    batteryIgnored: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenBatterySettings: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 10f)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_onboarding_permissions),
            contentDescription = "Editorial illustration of an on-device privacy shield",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Turn on live protection.",
            style = MaterialTheme.typography.headlineLargeEmphasized,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Grant these two Android permissions so BlockReels can detect short-video feeds instantaneously and stay active in the background.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        PermissionSetupCard(
            index = 0,
            total = 2,
            icon = Icons.Rounded.AccessibilityNew,
            title = "Accessibility Service",
            subtitle = "Detects Reels, Shorts, and feeds locally on-device to trigger your block action.",
            statusLabel = if (accessibilityEnabled) "Active" else "Required",
            granted = accessibilityEnabled,
            actionLabel = "Open Accessibility Settings",
            buttonTag = "onboarding_accessibility_btn",
            onAction = onOpenAccessibilitySettings
        )
        PermissionSetupCard(
            index = 1,
            total = 2,
            icon = Icons.Rounded.BatteryChargingFull,
            title = "Unrestricted Battery",
            subtitle = "Prevents Android from putting the shield to sleep while you use other apps.",
            statusLabel = if (batteryIgnored) "Unrestricted" else "Recommended",
            granted = batteryIgnored,
            actionLabel = "Allow Unrestricted Battery",
            buttonTag = "onboarding_battery_btn",
            onAction = onOpenBatterySettings
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PermissionSetupCard(
    index: Int,
    total: Int,
    icon: ImageVector,
    title: String,
    subtitle: String,
    statusLabel: String,
    granted: Boolean,
    actionLabel: String,
    buttonTag: String,
    onAction: () -> Unit
) {
    Surface(
        shape = segmentedListShape(index, total),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = MaterialShapes.Cookie7Sided.toShape(),
                    color = if (granted) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = if (granted) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (granted) Icons.Rounded.CheckCircle else icon,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = CircleShape,
                            color = if (granted) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = if (granted) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!granted) {
                FilledTonalButton(
                    onClick = onAction,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(buttonTag)
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}
