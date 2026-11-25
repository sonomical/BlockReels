// SPDX-License-Identifier: MIT
package com.blockreels.app.ui.components

import android.content.Context
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.blockreels.app.R
import com.blockreels.app.data.AppIconStyle
import com.blockreels.app.data.SupportedTargets
import com.blockreels.app.ui.theme.LocalAppIconStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

private data class CachedThemedIcon(
    val themedMonochrome: ImageBitmap?
)

private object AppIconMemoryCache {
    private val cache = ConcurrentHashMap<String, CachedThemedIcon>()
    private val EMPTY = CachedThemedIcon(themedMonochrome = null)

    fun get(packageName: String): CachedThemedIcon? = cache[packageName]

    fun load(context: Context, packageName: String, pxSize: Int = 128): CachedThemedIcon {
        cache[packageName]?.let { return it }
        val loaded = try {
            val drawable: Drawable = context.packageManager.getApplicationIcon(packageName)
            val monoBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && drawable is AdaptiveIconDrawable) {
                drawable.monochrome?.toBitmap(width = pxSize, height = pxSize)?.asImageBitmap()
            } else {
                null
            }
            CachedThemedIcon(themedMonochrome = monoBitmap)
        } catch (_: Exception) {
            EMPTY
        }
        cache[packageName] = loaded
        return loaded
    }
}

/**
 * Renders strictly Material 3 theme-colored social media icons:
 * - Colors always derive from the active [MaterialTheme.colorScheme] (never original app brand colors).
 * - Uses Android 13+ [AdaptiveIconDrawable.getMonochrome] when available and tints it with theme tokens,
 *   or falls back to crisp monochrome platform vector glyphs tinted strictly with Material 3 roles.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppThemedIcon(
    platformKey: String,
    packageName: String? = null,
    modifier: Modifier = Modifier,
    sizeDp: Int = 44,
    active: Boolean = true
) {
    val context = LocalContext.current
    val iconStyle = LocalAppIconStyle.current
    val resolvedPackage = packageName ?: SupportedTargets.primaryPackageForPlatform(platformKey)

    val cachedIcon by produceState<CachedThemedIcon?>(
        initialValue = resolvedPackage?.let { AppIconMemoryCache.get(it) },
        key1 = resolvedPackage
    ) {
        if (resolvedPackage != null && AppIconMemoryCache.get(resolvedPackage) == null) {
            value = withContext(Dispatchers.IO) {
                AppIconMemoryCache.load(context.applicationContext, resolvedPackage)
            }
        }
    }

    val containerShape = when (iconStyle) {
        AppIconStyle.TONAL_COOKIE -> MaterialShapes.Cookie4Sided.toShape()
        AppIconStyle.TONAL_SQUIRCLE -> RoundedCornerShape((sizeDp * 0.30f).dp)
        AppIconStyle.NEUTRAL_TONAL -> CircleShape
    }

    val containerColor = if (!active) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        when (iconStyle) {
            AppIconStyle.TONAL_COOKIE -> MaterialTheme.colorScheme.secondaryContainer
            AppIconStyle.TONAL_SQUIRCLE -> MaterialTheme.colorScheme.primaryContainer
            AppIconStyle.NEUTRAL_TONAL -> MaterialTheme.colorScheme.surfaceContainerHighest
        }
    }

    val contentColor = if (!active) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        when (iconStyle) {
            AppIconStyle.TONAL_COOKIE -> MaterialTheme.colorScheme.onSecondaryContainer
            AppIconStyle.TONAL_SQUIRCLE -> MaterialTheme.colorScheme.onPrimaryContainer
            AppIconStyle.NEUTRAL_TONAL -> MaterialTheme.colorScheme.primary
        }
    }

    val monoBitmap = cachedIcon?.themedMonochrome

    Surface(
        modifier = modifier.size(sizeDp.dp),
        shape = containerShape,
        color = containerColor,
        contentColor = contentColor
    ) {
        if (monoBitmap != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding((sizeDp * 0.14f).dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = monoBitmap,
                    contentDescription = "$platformKey icon",
                    colorFilter = ColorFilter.tint(contentColor),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            val brandRes = when (platformKey.uppercase()) {
                "INSTAGRAM" -> R.drawable.ic_brand_instagram
                "YOUTUBE" -> R.drawable.ic_brand_youtube
                "TIKTOK" -> R.drawable.ic_brand_tiktok
                "FACEBOOK" -> R.drawable.ic_brand_facebook
                "SNAPCHAT" -> R.drawable.ic_brand_snapchat
                else -> null
            }
            Box(contentAlignment = Alignment.Center) {
                if (brandRes != null) {
                    Icon(
                        painter = painterResource(id = brandRes),
                        contentDescription = "$platformKey icon",
                        tint = contentColor,
                        modifier = Modifier.size((sizeDp * 0.52f).dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = "$platformKey icon",
                        tint = contentColor,
                        modifier = Modifier.size((sizeDp * 0.52f).dp)
                    )
                }
            }
        }
    }
}
