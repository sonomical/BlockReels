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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
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

object AppIconPrewarmer {
    fun prewarm(context: Context, packageNames: Iterable<String>) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val appContext = context.applicationContext
        for (pkg in packageNames) {
            if (AppIconMemoryCache.get(pkg) == null) {
                AppIconMemoryCache.load(appContext, pkg)
            }
        }
    }
}

private object AppIconMemoryCache {
    private val cache = ConcurrentHashMap<String, CachedThemedIcon>(16)
    private val EMPTY = CachedThemedIcon(themedMonochrome = null)

    fun get(packageName: String): CachedThemedIcon? = cache[packageName]

    fun load(context: Context, packageName: String, pxSize: Int = 96): CachedThemedIcon {
        cache[packageName]?.let { return it }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            cache[packageName] = EMPTY
            return EMPTY
        }
        val loaded = try {
            val drawable: Drawable = context.packageManager.getApplicationIcon(packageName)
            val monoBitmap = if (drawable is AdaptiveIconDrawable) {
                drawable.monochrome?.toBitmap(width = pxSize, height = pxSize)?.asImageBitmap()
            } else {
                null
            }
            if (monoBitmap != null) CachedThemedIcon(themedMonochrome = monoBitmap) else EMPTY
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

    val squircleShape = remember(sizeDp) { RoundedCornerShape((sizeDp * 0.30f).dp) }
    val containerShape = when (iconStyle) {
        AppIconStyle.TONAL_COOKIE -> ExpressiveShapes.Cookie4Sided
        AppIconStyle.TONAL_SQUIRCLE -> squircleShape
        AppIconStyle.NEUTRAL_TONAL -> CircleShape
    }

    val scheme = MaterialTheme.colorScheme
    val containerColor = if (!active) {
        scheme.surfaceContainerHighest
    } else {
        when (iconStyle) {
            AppIconStyle.TONAL_COOKIE -> scheme.secondaryContainer
            AppIconStyle.TONAL_SQUIRCLE -> scheme.primaryContainer
            AppIconStyle.NEUTRAL_TONAL -> scheme.surfaceContainerHighest
        }
    }

    val contentColor = if (!active) {
        scheme.onSurfaceVariant
    } else {
        when (iconStyle) {
            AppIconStyle.TONAL_COOKIE -> scheme.onSecondaryContainer
            AppIconStyle.TONAL_SQUIRCLE -> scheme.onPrimaryContainer
            AppIconStyle.NEUTRAL_TONAL -> scheme.primary
        }
    }

    val monoBitmap = cachedIcon?.themedMonochrome
    val iconSize = remember(sizeDp) { (sizeDp * 0.52f).dp }

    Surface(
        modifier = modifier.size(sizeDp.dp),
        shape = containerShape,
        color = containerColor,
        contentColor = contentColor
    ) {
        if (monoBitmap != null) {
            val tintFilter = remember(contentColor) { ColorFilter.tint(contentColor) }
            val pad = remember(sizeDp) { (sizeDp * 0.14f).dp }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(pad),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = monoBitmap,
                    contentDescription = "$platformKey icon",
                    colorFilter = tintFilter,
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
                        modifier = Modifier.size(iconSize)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = "$platformKey icon",
                        tint = contentColor,
                        modifier = Modifier.size(iconSize)
                    )
                }
            }
        }
    }
}
