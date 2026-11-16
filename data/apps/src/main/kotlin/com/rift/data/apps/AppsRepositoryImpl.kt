package com.rift.data.apps

import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppsRepository {

    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager

    override val allApps: Flow<List<AppInfo>> = callbackFlow {
        val callback = object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String, user: android.os.UserHandle) {
                trySend(loadAllApps())
            }
            override fun onPackageAdded(packageName: String, user: android.os.UserHandle) {
                trySend(loadAllApps())
            }
            override fun onPackageChanged(packageName: String, user: android.os.UserHandle) {
                trySend(loadAllApps())
            }
            override fun onPackagesAvailable(packageNames: Array<out String>, user: android.os.UserHandle, replacing: Boolean) {
                trySend(loadAllApps())
            }
            override fun onPackagesUnavailable(packageNames: Array<out String>, user: android.os.UserHandle, replacing: Boolean) {
                trySend(loadAllApps())
            }
        }
        launcherApps.registerCallback(callback)
        trySend(loadAllApps())
        awaitClose { launcherApps.unregisterCallback(callback) }
    }

    override suspend fun refresh() {
        // The callbackFlow will re-emit on the next callback; manual refresh is a no-op for now.
    }

    override fun launchApp(app: AppInfo, sourceBounds: android.graphics.Rect?) {
        launcherApps.startMainActivity(app.componentName, app.userHandle, sourceBounds, null)
    }

    private fun loadAllApps(): List<AppInfo> {
        val result = mutableListOf<AppInfo>()
        for (profile in launcherApps.profiles) {
            val serial = userManager.getSerialNumberForUser(profile)
            val isWork = profile != android.os.Process.myUserHandle()
            val activities = launcherApps.getActivityList(null, profile)
            for (info in activities) {
                result += AppInfo(
                    componentName = info.componentName,
                    label = info.label.toString(),
                    userHandle = profile,
                    userSerial = serial,
                    isWorkProfile = isWork,
                    category = info.applicationInfo.category,
                    lastUpdateTime = try {
                        context.packageManager.getPackageInfo(info.componentName.packageName, 0).lastUpdateTime
                    } catch (_: Exception) { 0L },
                )
            }
        }
        return result.sortedBy { it.label.lowercase() }
    }
}
