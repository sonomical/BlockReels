// SPDX-License-Identifier: MIT
package com.blockreels.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.blockreels.app.data.TargetAppConfig
import com.blockreels.app.ui.components.PlatformAvatar
import com.blockreels.app.ui.components.TonalCard
import com.blockreels.app.viewmodel.BlockReelsUiState
import kotlinx.coroutines.launch

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

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(pageCount) { idx ->
                            val selected = pagerState.currentPage == idx
                            Box(
                                modifier = Modifier
                                    .height(8.dp)
                                    .size(width = if (selected) 24.dp else 8.dp, height = 8.dp)
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
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.testTag("onboarding_next_btn")
                        ) {
                            Text(if (pagerState.currentPage < pageCount - 1) "Next" else "Start")
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
                    verticalArrangement = Arrangement.spacedBy(18.dp)
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

@Composable
private fun AccessibilityStep(
    enabled: Boolean,
    onOpenSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(
                if (enabled) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.tertiaryContainer
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (enabled) Icons.Rounded.CheckCircle else Icons.Rounded.AccessibilityNew,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.size(36.dp)
        )
    }

    Text(
        text = "Accessibility Service",
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center
    )

    TonalCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "BlockReels uses AccessibilityService on-device to detect short-video feeds and trigger your chosen block action. No network permission or data collection.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
    }

    if (enabled) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                text = "Enabled",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
            )
        }
    } else {
        FilledTonalButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_accessibility_btn")
        ) {
            Text("Open Accessibility Settings")
        }
    }
}

@Composable
private fun BatteryStep(
    ignored: Boolean,
    onOpenSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (ignored) Icons.Rounded.CheckCircle else Icons.Rounded.BatteryChargingFull,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(36.dp)
        )
    }

    Text(
        text = "Background Execution",
        style = MaterialTheme.typography.headlineMedium,
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
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                text = "Unrestricted",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
            )
        }
    } else {
        FilledTonalButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_battery_btn")
        ) {
            Text("Allow Unrestricted Battery")
        }
    }
}

@Composable
private fun TargetAppsStep(
    targets: List<TargetAppConfig>,
    onToggleTarget: (String, Boolean) -> Unit
) {
    Icon(
        imageVector = Icons.Rounded.Tune,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(36.dp)
    )
    Text(
        text = "Select Targets",
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        targets.forEach { target ->
            TonalCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PlatformAvatar(platformKey = target.platformKey, sizeDp = 36)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(target.displayName, style = MaterialTheme.typography.titleSmall)
                        Text(
                            target.feedName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = target.isEnabled,
                        onCheckedChange = { onToggleTarget(target.packageName, it) }
                    )
                }
            }
        }
    }
}
