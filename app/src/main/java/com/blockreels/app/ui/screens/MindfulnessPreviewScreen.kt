// SPDX-License-Identifier: MIT
package com.blockreels.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blockreels.app.R
import com.blockreels.app.state.AudioAction
import com.blockreels.app.state.ContentBlockPolicy
import com.blockreels.app.ui.components.ExpressiveShapes
import com.blockreels.app.ui.theme.MonoFontFamily
import kotlinx.coroutines.delay
import kotlin.math.ceil

private val STRONG_MESSAGE_PRESETS = listOf(
    "Pause before you scroll.",
    "Protect your focus right now.",
    "Close the feed. Stay present.",
    "Is this worth your time?"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MindfulnessPreviewScreen(
    holdSeconds: Int,
    allowanceMinutes: Int,
    dailyPassLimit: Int,
    customHeadline: String,
    customMessage: String,
    contentPolicy: ContentBlockPolicy,
    requestHideSurfaces: Boolean,
    reelsGoalMinutes: Int,
    feedGoalMinutes: Int,
    audioAction: AudioAction,
    strictMode: Boolean,
    hapticsEnabled: Boolean,
    onUpdateMessageCopy: (headline: String, body: String) -> Unit,
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
    var showEditSheet by remember { mutableStateOf(false) }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "hold_progress"
    )

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

    val singleStrongMessage = when {
        unlocked -> "${allowanceMinutes}m pass unlocked."
        strictMode -> "Feed locked for focus."
        else -> customHeadline.trim().ifBlank {
            customMessage.trim().ifBlank { "Pause before you scroll." }
        }
    }

    val statusEyebrow = when {
        unlocked -> "PASS UNLOCKED"
        strictMode -> "STRICT LOCK"
        dailyPassLimit > 0 -> "MINDFUL PAUSE · $dailyPassLimit/DAY"
        else -> "MINDFUL PAUSE"
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Block Message",
                        style = MaterialTheme.typography.titleLargeEmphasized,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("preview_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { showEditSheet = true },
                        shapes = ButtonDefaults.shapes(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("preview_edit_copy_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Edit",
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            val isSideBySide = maxHeight < 520.dp && maxWidth >= 520.dp

            val illustrationHalf: @Composable (Modifier) -> Unit = { stageModifier ->
                Box(
                    modifier = stageModifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_mindful_pause_hero),
                            contentDescription = "Mindful focus illustration",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(MaterialTheme.shapes.extraLarge)
                        )
                    }

                    Surface(
                        shape = ExpressiveShapes.Cookie7Sided,
                        color = when {
                            unlocked -> MaterialTheme.colorScheme.primary
                            strictMode -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.primaryContainer
                        },
                        contentColor = when {
                            unlocked -> MaterialTheme.colorScheme.onPrimary
                            strictMode -> MaterialTheme.colorScheme.onError
                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(14.dp)
                            .size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (unlocked) Icons.Rounded.CheckCircle else Icons.Rounded.Shield,
                                contentDescription = null,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }

            val messageAndActionsHalf: @Composable (Modifier) -> Unit = { contentModifier ->
                Column(
                    modifier = contentModifier
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Single strong, unmistakable message block
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = statusEyebrow,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = MonoFontFamily,
                                letterSpacing = 1.4.sp
                            ),
                            fontWeight = FontWeight.Bold,
                            color = if (strictMode) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = singleStrongMessage,
                            style = MaterialTheme.typography.displaySmallEmphasized.copy(
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = 42.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("preview_strong_message")
                        )
                    }

                    // Clean bottom action controls
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onBack,
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("preview_go_back_btn")
                        ) {
                            Text(
                                text = if (unlocked) "Done" else "Return to Focus",
                                style = MaterialTheme.typography.titleMediumEmphasized,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        if (!strictMode && !unlocked) {
                            val remainingSec = ceil((1f - progress) * holdSeconds).toInt().coerceAtLeast(1)
                            val holdLabel = when {
                                isHolding -> "Keep holding · ${remainingSec}s"
                                canceledNotice -> "Released early · Hold ${holdSeconds}s"
                                else -> "Hold ${holdSeconds}s for ${allowanceMinutes}m pass"
                            }

                            Surface(
                                shape = MaterialTheme.shapes.largeIncreased,
                                color = if (isHolding) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = if (isHolding) MaterialTheme.colorScheme.onSecondaryContainer
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
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
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = holdLabel,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontFamily = if (isHolding) MonoFontFamily
                                            else MaterialTheme.typography.labelLarge.fontFamily
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    LinearWavyProgressIndicator(
                                        progress = { animatedProgress },
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isSideBySide) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 840.dp)
                ) {
                    illustrationHalf(
                        Modifier
                            .weight(0.50f)
                            .fillMaxHeight()
                    )
                    messageAndActionsHalf(
                        Modifier
                            .weight(0.50f)
                            .fillMaxHeight()
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 580.dp)
                ) {
                    // Exact 50/100 vertical illustration stage
                    illustrationHalf(
                        Modifier
                            .fillMaxWidth()
                            .weight(0.50f)
                    )
                    // Exact 50/100 single strong message + action stage
                    messageAndActionsHalf(
                        Modifier
                            .fillMaxWidth()
                            .weight(0.50f)
                    )
                }
            }
        }
    }

    if (showEditSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var draftMessage by remember {
            mutableStateOf(customHeadline.trim().ifBlank { customMessage.trim() })
        }

        ModalBottomSheet(
            onDismissRequest = { showEditSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Edit Block Message",
                    style = MaterialTheme.typography.headlineSmallEmphasized
                )
                Text(
                    text = "Keep it short and clear so it reads at a glance.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    STRONG_MESSAGE_PRESETS.forEach { preset ->
                        FilterChip(
                            selected = draftMessage.trim() == preset,
                            onClick = { draftMessage = preset },
                            label = {
                                Text(
                                    text = preset,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = draftMessage,
                    onValueChange = { draftMessage = it.take(80) },
                    label = { Text("Strong block message") },
                    supportingText = { Text("${draftMessage.length}/80") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("block_msg_headline_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showEditSheet = false }) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val cleaned = draftMessage.trim().ifBlank { "Pause before you scroll." }
                            onUpdateMessageCopy(cleaned, cleaned)
                            showEditSheet = false
                        },
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.testTag("block_msg_save_btn")
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
