package com.orbit.core.icons

/**
 * Pipeline for loading, rendering, and caching app icons.
 *
 * Flow: LauncherActivityInfo → getIcon(density) → Adaptive/Themed/IconPack mapper
 *       → render at target size → LRU memory (Hardware Bitmap) ↔ disk cache (WebP lossless)
 *
 * TODO: Implement full pipeline in Phase 1.
 */
interface IconPipeline {
    suspend fun loadIcon(key: IconKey): android.graphics.Bitmap?
    suspend fun invalidate(packageName: String, userSerial: Long)
    suspend fun invalidateAll()
}
