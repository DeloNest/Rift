package com.rift.core.icons

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-performance asynchronous icon loader and LRU memory cache.
 * Offloads all PackageManager icon decoding to Dispatchers.IO to maintain 120 FPS.
 */
@Singleton
class AppIconLoader @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    // 24 MB in-memory LRU cache of decoded Bitmaps
    private val memoryCache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    /**
     * Retrieves cached icon instantly if present (0ms UI thread safe).
     */
    fun getCached(packageName: String): Bitmap? = memoryCache.get(packageName)

    /**
     * Loads icon asynchronously on Dispatchers.IO, caches it, and returns the Bitmap.
     */
    suspend fun loadIcon(packageName: String, componentName: ComponentName, sizePx: Int = 120): Bitmap? =
        withContext(Dispatchers.IO) {
            memoryCache.get(packageName)?.let { return@withContext it }

            runCatching {
                val drawable = context.packageManager.getActivityIcon(componentName)
                val bmp = drawable.toBitmap(width = sizePx, height = sizePx, config = Bitmap.Config.ARGB_8888)
                memoryCache.put(packageName, bmp)
                bmp
            }.getOrNull()
        }

    /**
     * Pre-warms the cache in background thread so app drawer is instantly primed.
     */
    suspend fun prewarm(components: List<Pair<String, ComponentName>>) = withContext(Dispatchers.IO) {
        for ((pkg, cmp) in components) {
            if (memoryCache.get(pkg) == null) {
                runCatching {
                    val drawable = context.packageManager.getActivityIcon(cmp)
                    val bmp = drawable.toBitmap(width = 120, height = 120, config = Bitmap.Config.ARGB_8888)
                    memoryCache.put(pkg, bmp)
                }
            }
        }
    }
}
