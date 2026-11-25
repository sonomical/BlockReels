// SPDX-License-Identifier: MIT
package com.blockreels.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.blockreels.app.state.BlockingScope
import com.blockreels.app.state.InterventionMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Shared Material 3 Expressive polygon shapes accessor.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object ExpressiveShapes {
    val Cookie4Sided: Shape
        @Composable get() = MaterialShapes.Cookie4Sided.toShape()
    val Cookie7Sided: Shape
        @Composable get() = MaterialShapes.Cookie7Sided.toShape()
    val Cookie9Sided: Shape
        @Composable get() = MaterialShapes.Cookie9Sided.toShape()
    val Pill: Shape
        @Composable get() = MaterialShapes.Pill.toShape()
    val Slanted: Shape
        @Composable get() = MaterialShapes.Slanted.toShape()
}

private val SEGMENT_SINGLE_SHAPE = RoundedCornerShape(20.dp)
private val SEGMENT_FIRST_SHAPE = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
private val SEGMENT_LAST_SHAPE = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
private val SEGMENT_MIDDLE_SHAPE = RoundedCornerShape(4.dp)

/**
 * Pure opaque Material 3 Expressive container card.
 * Strictly zero borders and zero alpha translucency.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.largeIncreased,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        content = content
    )
}

/**
 * Material 3 Expressive segmented card list corner geometry:
 * outer corners = 20.dp, inner adjacent corners = 4.dp, separated by 2.dp vertical gap.
 */
fun segmentedListShape(index: Int, count: Int): Shape {
    if (count <= 1) return SEGMENT_SINGLE_SHAPE
    return when (index) {
        0 -> SEGMENT_FIRST_SHAPE
        count - 1 -> SEGMENT_LAST_SHAPE
        else -> SEGMENT_MIDDLE_SHAPE
    }
}

/**
 * Native Material 3 Expressive connected toggle button group.
 * Replaces legacy SegmentedButton rows per M3 Expressive specification.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> ConnectedToggleButtonGroup(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tagFor: ((T) -> String)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        options.forEachIndexed { index, option ->
            val isChecked = selected == option
            val shapes = when {
                options.size == 1 -> ToggleButtonDefaults.shapes()
                index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                index == options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
            }
            ToggleButton(
                checked = isChecked,
                onCheckedChange = { if (it) onSelect(option) },
                enabled = enabled,
                shapes = shapes,
                colors = ToggleButtonDefaults.tonalToggleButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier
                    .weight(1f)
                    .semantics { role = Role.RadioButton }
                    .let { mod -> if (tagFor != null) mod.testTag(tagFor(option)) else mod }
            ) {
                Text(
                    text = label(option),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Native Material 3 Switch with check/close thumb icons per M3 specification.
 */
@Composable
fun ExpressiveSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        modifier = modifier,
        thumbContent = {
            Icon(
                imageVector = if (checked) Icons.Rounded.Check else Icons.Rounded.Close,
                contentDescription = null,
                modifier = Modifier.size(SwitchDefaults.IconSize)
            )
        }
    )
}

/**
 * Real Material 3 themed app icon wrapper.
 * Loads installed app's Android themed monochrome/launcher icon when present,
 * falling back to authentic M3-tinted monochrome platform vector glyphs.
 */
@Composable
fun PlatformAvatar(
    platformKey: String,
    modifier: Modifier = Modifier,
    sizeDp: Int = 44,
    packageName: String? = null,
    active: Boolean = true
) {
    AppThemedIcon(
        platformKey = platformKey,
        packageName = packageName,
        modifier = modifier,
        sizeDp = sizeDp,
        active = active
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMediumEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
fun ModeBadge(
    mode: InterventionMode,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val (label, icon, container, content) = when (mode) {
        InterventionMode.AUTO_BACK -> Quad(
            if (compact) "Back" else "Auto Back",
            Icons.AutoMirrored.Rounded.Undo,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        InterventionMode.REDIRECT_HOME -> Quad(
            if (compact) "Home" else "Home Exit",
            Icons.Rounded.Home,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        InterventionMode.MINDFUL_PAUSE -> Quad(
            if (compact) "Mindful" else "Mindful Pause",
            Icons.Rounded.SelfImprovement,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        InterventionMode.STRICT_BLOCK -> Quad(
            if (compact) "Strict" else "Strict Block",
            Icons.Rounded.Block,
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
    }
    Surface(
        modifier = modifier,
        color = container,
        contentColor = content,
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ScopeBadge(
    scope: BlockingScope,
    modifier: Modifier = Modifier
) {
    val isFull = scope == BlockingScope.ENTIRE_APP
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = if (isFull) MaterialTheme.colorScheme.tertiaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = if (isFull) MaterialTheme.colorScheme.onTertiaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            text = scope.displayName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

private val relativeDateFormatter = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
}

fun formatRelativeTime(timestampMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val diffSec = ((nowMillis - timestampMillis) / 1000L).coerceAtLeast(0L)
    return when {
        diffSec < 60L -> "Just now"
        diffSec < 3600L -> "${diffSec / 60L}m ago"
        diffSec < 86_400L -> "${diffSec / 3600L}h ago"
        else -> (relativeDateFormatter.get() ?: SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()))
            .format(Date(timestampMillis))
    }
}

fun formatDurationMinutes(minutes: Int): String {
    return if (minutes >= 60) {
        val h = minutes / 60
        val m = minutes % 60
        if (m == 0) "${h}h" else "${h}h ${m}m"
    } else {
        "${minutes}m"
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AccessibilityDisclosureDialog(
    onDismiss: () -> Unit,
    onContinueToSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Accessibility Access",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = "BlockReels uses Android AccessibilityService locally to detect selected short-video feeds and trigger your configured block action. No data is stored or sent over the network.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onContinueToSettings,
                shapes = ButtonDefaults.shapes()
            ) {
                Text("Open Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
