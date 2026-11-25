// SPDX-License-Identifier: MIT
package com.blockreels.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
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
import androidx.compose.material3.toShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.blockreels.app.R
import com.blockreels.app.state.AudioAction
import com.blockreels.app.state.ContentBlockPolicy
import com.blockreels.app.ui.components.segmentedListShape
import com.blockreels.app.ui.theme.MonoFontFamily
import kotlinx.coroutines.delay
import kotlin.math.ceil

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

    val headlineText = when {
        unlocked -> "${allowanceMinutes}m pass unlocked."
        strictMode -> "Feed locked for focus."
        else -> customHeadline
    }

    val bodyText = when {
        unlocked -> "Temporary access granted for $allowanceMinutes minutes. Protection resumes automatically when time expires."
        strictMode -> "Strict Lock is enabled. Temporary passes are turned off so distractions stay closed."
        else -> customMessage
    }

    val actionSteps = remember(contentPolicy, requestHideSurfaces, audioAction, strictMode, holdSeconds, allowanceMinutes, reelsGoalMinutes, feedGoalMinutes) {
        listOf(
            PlannedActionStep(
                icon = if (requestHideSurfaces || contentPolicy == ContentBlockPolicy.HIDE_SURFACES) {
                    Icons.Rounded.VisibilityOff
                } else {
                    Icons.Rounded.Block
                },
                title = when (contentPolicy) {
                    ContentBlockPolicy.REELS_ONLY -> "Intercept short-form reels"
                    ContentBlockPolicy.MAIN_FEED_ONLY -> "Intercept home scrolling feed"
                    ContentBlockPolicy.FEED_AND_REELS -> "Intercept both feed and reels"
                    ContentBlockPolicy.HIDE_SURFACES -> "Hide feed & reels from view"
                },
                detail = if (requestHideSurfaces || contentPolicy == ContentBlockPolicy.HIDE_SURFACES) {
                    "Requests not to show distracting surfaces and exits immediately."
                } else {
                    "Stops scrolling before autoplay captures your attention."
                },
                accentRole = ActionAccent.PRIMARY
            ),
            PlannedActionStep(
                icon = Icons.Rounded.VolumeOff,
                title = when (audioAction) {
                    AudioAction.SILENCE -> "Silence video audio"
                    AudioAction.DUCK -> "Lower background audio"
                    AudioAction.NONE -> "Keep media audio unchanged"
                },
                detail = when (audioAction) {
                    AudioAction.SILENCE -> "Mutes autoplay sound while this block screen is active."
                    AudioAction.DUCK -> "Reduces media volume so you can reset calmly."
                    AudioAction.NONE -> "Audio focus stays untouched during interception."
                },
                accentRole = ActionAccent.SECONDARY
            ),
            PlannedActionStep(
                icon = Icons.Rounded.Flag,
                title = if (strictMode) {
                    "Enforce strict exit"
                } else {
                    "Enforce goals & ${holdSeconds}s mindful hold"
                },
                detail = buildString {
                    append("Reels goal: ${if (reelsGoalMinutes == 0) "0m (block)" else "${reelsGoalMinutes}m/day"}")
                    append(" · Feed goal: ${if (feedGoalMinutes == 0) "0m (block)" else "${feedGoalMinutes}m/day"}")
                    if (!strictMode) {
                        append(" · Hold ${holdSeconds}s for ${allowanceMinutes}m pass")
                        if (dailyPassLimit > 0) append(" (max $dailyPassLimit/day)")
                    }
                },
                accentRole = ActionAccent.TERTIARY
            )
        )
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Block Message",
                        style = MaterialTheme.typography.titleLargeEmphasized
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
                        Text("Edit Text")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 560.dp),
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
                                style = MaterialTheme.typography.titleMediumEmphasized
                            )
                        }

                        if (!strictMode && !unlocked) {
                            val remainingSec = ceil((1f - progress) * holdSeconds).toInt().coerceAtLeast(1)
                            val holdLabel = when {
                                isHolding -> "Keep holding · ${remainingSec}s"
                                canceledNotice -> "Released early · Hold ${holdSeconds}s to unlock"
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
                                            fontFamily = if (isHolding) MonoFontFamily else MaterialTheme.typography.labelLarge.fontFamily
                                        )
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
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 580.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Big Material 3 Onboarding-style Hero Illustration Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(236.dp)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_mindful_pause_hero),
                            contentDescription = "Mindful focus sanctuary illustration",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(MaterialTheme.shapes.extraLarge)
                        )
                    }

                    // Expressive status emblem anchored cleanly at bottom-end of illustration
                    Surface(
                        shape = MaterialShapes.Cookie7Sided.toShape(),
                        color = if (unlocked) MaterialTheme.colorScheme.primary
                        else if (strictMode) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (unlocked) MaterialTheme.colorScheme.onPrimary
                        else if (strictMode) MaterialTheme.colorScheme.onError
                        else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(14.dp)
                            .size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (unlocked) Icons.Rounded.CheckCircle else Icons.Rounded.Shield,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                // Editorial Badge Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val quotaTag = if (dailyPassLimit > 0) " · $dailyPassLimit/day" else ""
                    Surface(
                        shape = CircleShape,
                        color = if (strictMode) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (strictMode) MaterialTheme.colorScheme.onErrorContainer
                        else MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Text(
                            text = if (strictMode) "STRICT LOCK" else "MINDFUL PAUSE$quotaTag",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Text(
                            text = contentPolicy.label,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                // Strong Editorial Typography Block
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = headlineText,
                        style = MaterialTheme.typography.displaySmallEmphasized,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = bodyText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Clear Breakdown of Actions That Will Be Taken
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Actions taken when triggered",
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        actionSteps.forEachIndexed { idx, step ->
                            val (iconBg, iconFg) = when (step.accentRole) {
                                ActionAccent.PRIMARY -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                                ActionAccent.SECONDARY -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
                                ActionAccent.TERTIARY -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
                            }
                            Surface(
                                shape = segmentedListShape(idx, actionSteps.size),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                ListItem(
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    leadingContent = {
                                        Surface(
                                            shape = CircleShape,
                                            color = iconBg,
                                            contentColor = iconFg,
                                            modifier = Modifier.size(42.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = step.icon,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    },
                                    headlineContent = {
                                        Text(
                                            text = step.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    supportingContent = {
                                        Text(
                                            text = step.detail,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (showEditSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var draftHeadline by remember { mutableStateOf(customHeadline) }
        var draftMessage by remember { mutableStateOf(customMessage) }

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
                    text = "Customize Block Message",
                    style = MaterialTheme.typography.headlineSmallEmphasized
                )
                OutlinedTextField(
                    value = draftHeadline,
                    onValueChange = { draftHeadline = it },
                    label = { Text("Headline") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("block_msg_headline_input")
                )
                OutlinedTextField(
                    value = draftMessage,
                    onValueChange = { draftMessage = it },
                    label = { Text("Message body") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("block_msg_body_input")
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
                            onUpdateMessageCopy(draftHeadline, draftMessage)
                            showEditSheet = false
                        },
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.testTag("block_msg_save_btn")
                    ) {
                        Text("Save Message")
                    }
                }
            }
        }
    }
}

private enum class ActionAccent { PRIMARY, SECONDARY, TERTIARY }

private data class PlannedActionStep(
    val icon: ImageVector,
    val title: String,
    val detail: String,
    val accentRole: ActionAccent
)
