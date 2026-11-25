// SPDX-License-Identifier: MIT
package com.blockreels.app.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.blockreels.app.data.TargetAppConfig
import com.blockreels.app.ui.components.ExpressiveCard
import com.blockreels.app.ui.components.ExpressiveSwitch
import com.blockreels.app.ui.components.PlatformAvatar
import com.blockreels.app.ui.components.segmentedListShape
import com.blockreels.app.viewmodel.BlockReelsUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnboardingScreen(
    uiState: BlockReelsUiState,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onToggleTarget: (String, Boolean) -> Unit,
    onFinishOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pageCount = 3
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val scope = rememberCoroutineScope()
    val motionScheme = MaterialTheme.motionScheme

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(pageCount) { idx ->
                            val selected = pagerState.currentPage == idx
                            val indicatorWidth by animateDpAsState(
                                targetValue = if (selected) 28.dp else 8.dp,
                                animationSpec = motionScheme.fastSpatialSpec(),
                                label = "onboarding_dot_width"
                            )
                            Box(
                                modifier = Modifier
                                    .width(indicatorWidth)
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceContainerHighest
                                    )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 520.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (pagerState.currentPage == 0) {
                            TextButton(
                                onClick = onFinishOnboarding,
                                modifier = Modifier.testTag("onboarding_skip_btn")
                            ) {
                                Text("Skip")
                            }
                        } else {
                            TextButton(
                                onClick = {
                                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                                },
                                modifier = Modifier.testTag("onboarding_back_btn")
                            ) {
                                Text("Back")
                            }
                        }

                        Button(
                            onClick = {
                                if (pagerState.currentPage < pageCount - 1) {
                                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                } else {
                                    onFinishOnboarding()
                                }
                            },
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier.testTag("onboarding_next_btn")
                        ) {
                            Text(if (pagerState.currentPage < pageCount - 1) "Next" else "Get Started")
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
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 520.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    when (page) {
                        0 -> AccessibilityStep(
                            enabled = uiState.systemHealth.accessibilityEnabled,
                            onOpenSettings = onOpenAccessibilitySettings
                        )
                        1 -> BatteryStep(
                            ignored = uiState.systemHealth.batteryOptimizationIgnored,
                            onOpenSettings = onOpenBatterySettings
                        )
                        2 -> TargetAppsStep(
                            targets = uiState.targets,
                            onToggleTarget = onToggleTarget
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AccessibilityStep(
    enabled: Boolean,
    onOpenSettings: () -> Unit
) {
    Surface(
        modifier = Modifier.size(88.dp),
        shape = MaterialShapes.Cookie9Sided.toShape(),
        color = if (enabled) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onTertiaryContainer
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (enabled) Icons.Rounded.CheckCircle else Icons.Rounded.AccessibilityNew,
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
        }
    }

    Text(
        text = "Accessibility Service",
        style = MaterialTheme.typography.headlineMediumEmphasized,
        textAlign = TextAlign.Center
    )

    ExpressiveCard(shape = MaterialTheme.shapes.extraLarge) {
        Text(
            text = "BlockReels uses AccessibilityService on-device to detect short-video feeds and trigger your chosen block action. No network permission or data collection.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(20.dp)
        )
    }

    if (enabled) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Text(
                text = "Enabled",
                style = MaterialTheme.typography.labelLargeEmphasized,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
            )
        }
    } else {
        FilledTonalButton(
            onClick = onOpenSettings,
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_accessibility_btn")
        ) {
            Text("Open Accessibility Settings")
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BatteryStep(
    ignored: Boolean,
    onOpenSettings: () -> Unit
) {
    Surface(
        modifier = Modifier.size(88.dp),
        shape = MaterialShapes.Sunny.toShape(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (ignored) Icons.Rounded.CheckCircle else Icons.Rounded.BatteryChargingFull,
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
        }
    }

    Text(
        text = "Background Execution",
        style = MaterialTheme.typography.headlineMediumEmphasized,
        textAlign = TextAlign.Center
    )
    Text(
        text = "Exempt BlockReels from battery optimization so Android keeps the blocker running.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )

    if (ignored) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Text(
                text = "Unrestricted",
                style = MaterialTheme.typography.labelLargeEmphasized,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
            )
        }
    } else {
        FilledTonalButton(
            onClick = onOpenSettings,
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_battery_btn")
        ) {
            Text("Allow Unrestricted Battery")
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TargetAppsStep(
    targets: List<TargetAppConfig>,
    onToggleTarget: (String, Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.size(76.dp),
        shape = MaterialShapes.Cookie7Sided.toShape(),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = null,
                modifier = Modifier.size(36.dp)
            )
        }
    }

    Text(
        text = "Select Targets",
        style = MaterialTheme.typography.headlineMediumEmphasized,
        textAlign = TextAlign.Center
    )

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
                        sizeDp = 40,
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
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
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
