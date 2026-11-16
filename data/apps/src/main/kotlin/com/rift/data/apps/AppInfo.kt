package com.rift.data.apps

import android.content.ComponentName
import android.os.UserHandle

/**
 * Represents a launchable application on the device.
 */
data class AppInfo(
    val componentName: ComponentName,
    val label: String,
    val userHandle: UserHandle,
    val userSerial: Long,
    val isWorkProfile: Boolean = false,
    val category: Int = 0,
    val packageName: String = componentName.packageName,
    val lastUpdateTime: Long = 0,
)
