package com.orbit.data.apps

import kotlinx.coroutines.flow.Flow

/**
 * Repository wrapping LauncherApps for querying installed applications.
 */
interface AppsRepository {
    /** Flow of all launchable apps, updated on install/uninstall/update. */
    val allApps: Flow<List<AppInfo>>

    /** Force a refresh of the app list. */
    suspend fun refresh()

    /** Launch an app. */
    fun launchApp(app: AppInfo, sourceBounds: android.graphics.Rect? = null)
}
