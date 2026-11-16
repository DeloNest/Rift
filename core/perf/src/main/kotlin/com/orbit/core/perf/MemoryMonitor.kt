package com.orbit.core.perf

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build

/**
 * Monitors memory usage and tracks exit reasons for diagnostics.
 */
object MemoryMonitor {

    fun isLowRamDevice(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return am.isLowRamDevice
    }

    fun getMemoryClass(context: Context): Int {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return am.memoryClass
    }

    /**
     * Retrieves recent exit info for diagnostics.
     * Only available on API 30+.
     */
    fun getRecentExitReasons(context: Context, maxResults: Int = 5): List<ApplicationExitInfo> {
        if (Build.VERSION.SDK_INT < 30) return emptyList()
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return am.getHistoricalProcessExitReasons(context.packageName, 0, maxResults)
    }
}
