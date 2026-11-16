package com.rift.data.widgets

import android.appwidget.AppWidgetProviderInfo
import android.graphics.Bitmap

/**
 * Represents a group of AppWidgets belonging to the same host application.
 */
data class AppWidgetGroup(
    val packageName: String,
    val appName: String,
    val appIcon: Bitmap?,
    val widgets: List<AppWidgetProviderInfo>,
)
