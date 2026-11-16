package com.rift.core.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rift.core.icons.IconPackManager
import com.rift.data.apps.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val LocalIconShape = compositionLocalOf { "SYSTEM" }
val LocalIconPackPackage = compositionLocalOf<String?> { null }
val LocalIconPackManager = compositionLocalOf<IconPackManager?> { null }

// 48 MB High-Definition Bitmap Cache for native 120Hz display
private val globalBitmapCache = object : android.util.LruCache<String, Bitmap>(48 * 1024 * 1024) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
}

/**
 * Universal ultra-high-definition AppIcon component.
 * - Decodes at full 256x256 native resolution for razor-sharp clarity on QHD+ / AMOLED screens.
 * - Supports installed third-party Icon Packs seamlessly via IconPackManager.
 * - Supports dynamic icon shapes (Circle, Squircle, Rounded Square, Teardrop, System Default).
 * - Backed by an in-memory hardware bitmap cache and asynchronous IO offloading.
 */
@Composable
fun AppIcon(
    app: AppInfo,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    val context = LocalContext.current
    val iconShape = LocalIconShape.current
    val iconPackPackage = LocalIconPackPackage.current
    val iconPackManager = LocalIconPackManager.current

    val cacheKey = "${iconPackPackage ?: "system"}/${app.packageName}/${app.userSerial}"
    var bitmap by remember(cacheKey) {
        mutableStateOf(globalBitmapCache.get(cacheKey))
    }

    if (bitmap == null) {
        LaunchedEffect(cacheKey) {
            val cached = globalBitmapCache.get(cacheKey)
            if (cached != null) {
                bitmap = cached
            } else {
                val loaded = withContext(Dispatchers.IO) {
                    val targetPx = 256
                    // 1. Try loading from active icon pack if selected
                    if (!iconPackPackage.isNullOrBlank() && iconPackManager != null) {
                        val packIcon = iconPackManager.loadIcon(iconPackPackage, app.componentName, targetPx)
                        if (packIcon != null) {
                            globalBitmapCache.put(cacheKey, packIcon)
                            return@withContext packIcon
                        }
                    }

                    // 2. Fall back to high-resolution system icon
                    runCatching {
                        var drawable = context.packageManager.getActivityIcon(app.componentName)
                        if (app.isWorkProfile) {
                            drawable = context.packageManager.getUserBadgedIcon(drawable, app.userHandle)
                        }
                        val bmp = Bitmap.createBitmap(targetPx, targetPx, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bmp)
                        drawable.setBounds(0, 0, targetPx, targetPx)
                        drawable.draw(canvas)
                        globalBitmapCache.put(cacheKey, bmp)
                        bmp
                    }.getOrNull()
                }
                if (loaded != null) {
                    bitmap = loaded
                }
            }
        }
    }

    val shapeModifier = when (iconShape.uppercase()) {
        "CIRCLE" -> Modifier.clip(CircleShape)
        "SQUIRCLE" -> Modifier.clip(RoundedCornerShape(24.dp))
        "ROUNDED_SQUARE" -> Modifier.clip(RoundedCornerShape(14.dp))
        "TEARDROP" -> Modifier.clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = 4.dp))
        else -> Modifier // SYSTEM: preserves native adaptive squircle
    }

    val currentBmp = bitmap
    if (currentBmp != null) {
        Image(
            bitmap = currentBmp.asImageBitmap(),
            contentDescription = app.label,
            modifier = modifier
                .size(size)
                .then(shapeModifier),
        )
    } else {
        // Fast fallback container
        Surface(
            modifier = modifier
                .size(size)
                .then(shapeModifier),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 2.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = app.label.take(1),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}
