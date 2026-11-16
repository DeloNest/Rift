package com.rift.core.icons

import android.graphics.Bitmap
import android.util.LruCache

/**
 * LRU memory cache for rendered app icons (Hardware Bitmaps).
 * Sized by total bytes, not count.
 *
 * Target budget: 6–10 MB (plan section 7.3).
 */
class IconCache(maxSizeBytes: Int = DEFAULT_CACHE_SIZE_BYTES) {

    companion object {
        /** ~8 MB default */
        const val DEFAULT_CACHE_SIZE_BYTES = 8 * 1024 * 1024
    }

    private val cache = object : LruCache<IconKey, Bitmap>(maxSizeBytes) {
        override fun sizeOf(key: IconKey, value: Bitmap): Int = value.allocationByteCount
    }

    fun get(key: IconKey): Bitmap? = cache.get(key)

    fun put(key: IconKey, bitmap: Bitmap) {
        cache.put(key, bitmap)
    }

    fun evict(key: IconKey) {
        cache.remove(key)
    }

    fun trimToSize(maxSize: Int) {
        cache.trimToSize(maxSize)
    }

    fun clear() {
        cache.evictAll()
    }

    val size: Int get() = cache.size()
    val maxSize: Int get() = cache.maxSize()
}

/**
 * Unique key for an icon in the cache.
 */
data class IconKey(
    val componentName: String,
    val userSerial: Long,
    val shapeName: String = "default",
    val themed: Boolean = false,
    val iconPackName: String? = null,
)
