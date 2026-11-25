// SPDX-License-Identifier: MIT
package com.blockreels.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.blockreels.app.ui.theme.MonoFontFamily
import kotlinx.coroutines.delay
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindfulnessPreviewScreen(
    holdSeconds: Int,
    allowanceMinutes: Int,
    dailyPassLimit: Int,
    customMessage: String,
    strictMode: Boolean,
    hapticsEnabled: Boolean,
    onCompletedPreview: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val haptic = LocalHapticFeedback.current
    var isHolding by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var unlocked by remember { mutableStateOf(false) }
    var canceledNotice by remember { mutableStateOf(false) }

    val animatedProgress by animateFloatAsState(targetValue = progress, label = "hold_progress")

    LaunchedEffect(isHolding, holdSeconds) {
        if (!isHolding || unlocked) return@LaunchedEffect
        canceledNotice = false
        val durationMs = holdSeconds.coerceAtLeast(1) * 1000L
        val startTime = System.currentTimeMillis()
        if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        while (isHolding) {
            val elapsed = System.currentTimeMillis() - startTime
            progress = (elapsed.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            if (elapsed >= durationMs) {
                unlocked = true
                isHolding = false
                if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onCompletedPreview()
                break
            }
            delay(32L)
        }
        if (!unlocked && progress > 0f) {
            progress = 0f
            canceledNotice = true
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Overlay Preview") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("preview_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (unlocked) Icons.Rounded.CheckCircle else Icons.Rounded.SelfImprovement,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    val quotaTag = if (dailyPassLimit > 0) " · $dailyPassLimit/day" else ""
                    Text(
                        text = if (strictMode) "STRICT BLOCK" else "MINDFUL PAUSE$quotaTag",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = when {
                            unlocked -> "${allowanceMinutes}m Pass Granted"
                            strictMode -> "Blocked"
                            else -> "Take a Pause"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = when {
                            unlocked -> "Temporary pass active for $allowanceMinutes min."
                            strictMode -> "Passes disabled in Strict mode."
                            else -> customMessage
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = onBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("preview_go_back_btn"),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(if (unlocked) "Done" else "Go Back")
                    }

                    if (!strictMode && !unlocked) {
                        val remainingSec = ceil((1f - progress) * holdSeconds).toInt().coerceAtLeast(1)
                        val holdLabel = when {
                            isHolding -> "Hold · ${remainingSec}s"
                            canceledNotice -> "Released early · ${holdSeconds}s"
                            else -> "Hold ${holdSeconds}s for ${allowanceMinutes}m pass"
                        }

                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .pointerInput(holdSeconds) {
                                    awaitEachGesture {
                                        awaitFirstDown(requireUnconsumed = false)
                                        isHolding = true
                                        waitForUpOrCancellation()
                                        isHolding = false
                                    }
                                }
                                .testTag("preview_hold_surface")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = holdLabel,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = if (isHolding) MonoFontFamily else MaterialTheme.typography.labelLarge.fontFamily
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                LinearProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
