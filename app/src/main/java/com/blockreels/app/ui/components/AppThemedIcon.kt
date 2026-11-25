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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.blockreels.app.R
import com.blockreels.app.data.SupportedTargets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

private sealed interface LoadedAppIcon {
    data object NotInstalled : LoadedAppIcon
    data class ThemedMonochrome(val bitmap: ImageBitmap) : LoadedAppIcon
    data class FullLauncher(val bitmap: ImageBitmap) : LoadedAppIcon
}

private object AppIconMemoryCache {
    private val cache = ConcurrentHashMap<String, LoadedAppIcon>()

    fun get(packageName: String): LoadedAppIcon? = cache[packageName]

    fun load(context: Context, packageName: String, pxSize: Int = 128): LoadedAppIcon {
        cache[packageName]?.let { return it }
        val loaded = try {
            val drawable: Drawable = context.packageManager.getApplicationIcon(packageName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && drawable is AdaptiveIconDrawable) {
                val monochrome = drawable.monochrome
                if (monochrome != null) {
                    LoadedAppIcon.ThemedMonochrome(
                        monochrome.toBitmap(width = pxSize, height = pxSize).asImageBitmap()
                    )
                } else {
                    LoadedAppIcon.FullLauncher(
                        drawable.toBitmap(width = pxSize, height = pxSize).asImageBitmap()
                    )
                }
            } else {
                LoadedAppIcon.FullLauncher(
                    drawable.toBitmap(width = pxSize, height = pxSize).asImageBitmap()
                )
            }
        } catch (_: Exception) {
            LoadedAppIcon.NotInstalled
        }
        cache[packageName] = loaded
        return loaded
    }
}

/**
 * Displays a real Material 3 themed app icon:
 * 1. If the target app is installed and exposes an Android 13+ AdaptiveIconDrawable monochrome layer,
 *    renders that real monochrome themed glyph tinted with the active M3 tonal role.
 * 2. If installed without a monochrome layer, renders the real installed launcher icon cleanly clipped.
 * 3. If not installed on this device, renders an authentic monochrome brand vector drawable tinted
 *    strictly with Material 3 Expressive container/content roles.
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
    val resolvedPackage = packageName ?: SupportedTargets.primaryPackageForPlatform(platformKey)

    val loadedIcon by produceState<LoadedAppIcon>(
        initialValue = resolvedPackage?.let { AppIconMemoryCache.get(it) } ?: LoadedAppIcon.NotInstalled,
        key1 = resolvedPackage
    ) {
        if (resolvedPackage != null && AppIconMemoryCache.get(resolvedPackage) == null) {
            value = withContext(Dispatchers.IO) {
                AppIconMemoryCache.load(context.applicationContext, resolvedPackage)
            }
        }
    }

    val containerColor = if (active) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val contentColor = if (active) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val expressiveShape = MaterialShapes.Cookie4Sided.toShape()

    when (val iconState = loadedIcon) {
        is LoadedAppIcon.ThemedMonochrome -> {
            Surface(
                modifier = modifier.size(sizeDp.dp),
                shape = expressiveShape,
                color = containerColor,
                contentColor = contentColor
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding((sizeDp * 0.14f).dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = iconState.bitmap,
                        contentDescription = "$platformKey icon",
                        colorFilter = ColorFilter.tint(contentColor),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        is LoadedAppIcon.FullLauncher -> {
            val clipShape = RoundedCornerShape((sizeDp * 0.28f).dp)
            Box(
                modifier = modifier
                    .size(sizeDp.dp)
                    .clip(clipShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = iconState.bitmap,
                    contentDescription = "$platformKey icon",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        LoadedAppIcon.NotInstalled -> {
            val brandRes = when (platformKey.uppercase()) {
                "INSTAGRAM" -> R.drawable.ic_brand_instagram
                "YOUTUBE" -> R.drawable.ic_brand_youtube
                "TIKTOK" -> R.drawable.ic_brand_tiktok
                "FACEBOOK" -> R.drawable.ic_brand_facebook
                "SNAPCHAT" -> R.drawable.ic_brand_snapchat
                else -> null
            }

            Surface(
                modifier = modifier.size(sizeDp.dp),
                shape = expressiveShape,
                color = containerColor,
                contentColor = contentColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (brandRes != null) {
                        Icon(
                            painter = painterResource(id = brandRes),
                            contentDescription = "$platformKey icon",
                            tint = contentColor,
                            modifier = Modifier.size((sizeDp * 0.50f).dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = "$platformKey icon",
                            tint = contentColor,
                            modifier = Modifier.size((sizeDp * 0.50f).dp)
                        )
                    }
                }
            }
        }
    }
}
