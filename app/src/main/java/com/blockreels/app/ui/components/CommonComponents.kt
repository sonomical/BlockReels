// SPDX-License-Identifier: MIT
package com.blockreels.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.blockreels.app.state.BlockingScope
import com.blockreels.app.state.InterventionMode
import com.blockreels.app.ui.theme.BrandFacebook
import com.blockreels.app.ui.theme.BrandInstagram
import com.blockreels.app.ui.theme.BrandSnapchat
import com.blockreels.app.ui.theme.BrandTikTok
import com.blockreels.app.ui.theme.BrandYouTube
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TonalCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(content = content)
    }
}

@Composable
fun PlatformAvatar(
    platformKey: String,
    modifier: Modifier = Modifier,
    sizeDp: Int = 44
) {
    val (bg, icon) = when (platformKey.uppercase()) {
        "INSTAGRAM" -> BrandInstagram to Icons.Rounded.CameraAlt
        "YOUTUBE" -> BrandYouTube to Icons.Rounded.PlayArrow
        "TIKTOK" -> BrandTikTok to Icons.Rounded.MusicNote
        "FACEBOOK" -> BrandFacebook to Icons.Rounded.Public
        "SNAPCHAT" -> BrandSnapchat to Icons.Rounded.Bolt
        else -> MaterialTheme.colorScheme.primary to Icons.Rounded.Shield
    }

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "$platformKey icon",
            tint = bg,
            modifier = Modifier.size((sizeDp * 0.52f).dp)
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
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
            Icons.Rounded.Undo,
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
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = content,
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
        color = if (isFull) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f)
        else MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Text(
            text = scope.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = if (isFull) MaterialTheme.colorScheme.onTertiaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

fun formatRelativeTime(timestampMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val diffSec = ((nowMillis - timestampMillis) / 1000L).coerceAtLeast(0L)
    return when {
        diffSec < 60L -> "Just now"
        diffSec < 3600L -> "${diffSec / 60L}m ago"
        diffSec < 86_400L -> "${diffSec / 3600L}h ago"
        else -> SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(timestampMillis))
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
            TextButton(onClick = onContinueToSettings) {
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
