package com.rift.data.apps

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppsRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : AppsRepository {

    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
    private val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager
    private val mainHandler = Handler(Looper.getMainLooper())

    override val allApps: Flow<List<AppInfo>> = callbackFlow {
        val callback = object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String, user: UserHandle) {
                trySend(loadAllApps())
            }
            override fun onPackageAdded(packageName: String, user: UserHandle) {
                trySend(loadAllApps())
            }
            override fun onPackageChanged(packageName: String, user: UserHandle) {
                trySend(loadAllApps())
            }
            override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
                trySend(loadAllApps())
            }
            override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
                trySend(loadAllApps())
            }
        }

        try {
            launcherApps?.registerCallback(callback, mainHandler)
        } catch (_: Exception) {
            // Ignore if callback cannot be registered (e.g. permission or profile lock)
        }

        trySend(loadAllApps())

        awaitClose {
            try {
                launcherApps?.unregisterCallback(callback)
            } catch (_: Exception) {}
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun refresh() {
        // Triggers manual refresh if needed
    }

    override fun launchApp(app: AppInfo, sourceBounds: Rect?) {
        try {
            launcherApps?.startMainActivity(app.componentName, app.userHandle, sourceBounds, null)
        } catch (_: Exception) {
            try {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)?.apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    sourceBounds?.let { bounds -> this.sourceBounds = bounds }
                }
                if (launchIntent != null) {
                    context.startActivity(launchIntent)
                }
            } catch (_: Exception) {}
        }
    }

    private fun loadAllApps(): List<AppInfo> {
        val result = mutableListOf<AppInfo>()

        // 1. Try modern LauncherApps API across all available profiles (handles Work & Personal)
        var loadedViaLauncherApps = false
        try {
            val profiles = launcherApps?.profiles ?: listOf(Process.myUserHandle())
            for (profile in profiles) {
                try {
                    val serial = try {
                        userManager?.getSerialNumberForUser(profile) ?: 0L
                    } catch (_: Exception) { 0L }

                    val isWork = profile != Process.myUserHandle()
                    val activities = launcherApps?.getActivityList(null, profile) ?: emptyList()

                    for (info in activities) {
                        result += AppInfo(
                            componentName = info.componentName,
                            label = info.label?.toString() ?: info.componentName.packageName,
                            userHandle = profile,
                            userSerial = serial,
                            isWorkProfile = isWork,
                            category = try { info.applicationInfo.category } catch (_: Exception) { -1 },
                            lastUpdateTime = try {
                                context.packageManager.getPackageInfo(info.componentName.packageName, 0).lastUpdateTime
                            } catch (_: Exception) { 0L },
                        )
                    }
                    if (activities.isNotEmpty()) {
                        loadedViaLauncherApps = true
                    }
                } catch (_: Exception) {
                    // Profile might be locked or restricted (e.g. Samsung Knox / Secure Folder)
                }
            }
        } catch (_: Exception) {}

        // 2. Reliable Fallback via standard PackageManager if LauncherApps didn't yield apps
        if (!loadedViaLauncherApps || result.isEmpty()) {
            try {
                val myUser = Process.myUserHandle()
                val mainIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfos = context.packageManager.queryIntentActivities(mainIntent, 0)
                for (resolve in resolveInfos) {
                    val activityInfo = resolve.activityInfo ?: continue
                    val pkgName = activityInfo.packageName
                    val name = activityInfo.name
                    val label = resolve.loadLabel(context.packageManager).toString()
                    val category = try { activityInfo.applicationInfo.category } catch (_: Exception) { -1 }

                    result += AppInfo(
                        componentName = ComponentName(pkgName, name),
                        label = label,
                        userHandle = myUser,
                        userSerial = 0L,
                        isWorkProfile = false,
                        category = category,
                        lastUpdateTime = try {
                            context.packageManager.getPackageInfo(pkgName, 0).lastUpdateTime
                        } catch (_: Exception) { 0L },
                    )
                }
            } catch (_: Exception) {}
        }

        return result.distinctBy { it.componentName }.sortedBy { it.label.lowercase() }
    }
}
