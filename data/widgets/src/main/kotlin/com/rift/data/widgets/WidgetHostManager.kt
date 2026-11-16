package com.rift.data.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.UserHandle
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the AppWidgetHost lifecycle for hosting third-party widgets.
 * See plan section 7.4.
 */
@Singleton
class WidgetHostManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val HOST_ID = 1024
    }

    val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)

    private val host = object : AppWidgetHost(context, HOST_ID) {
        override fun onCreateView(
            ctx: Context,
            appWidgetId: Int,
            appWidget: AppWidgetProviderInfo?,
        ): RiftWidgetHostView {
            return RiftWidgetHostView(ctx).also {
                it.setAppWidget(appWidgetId, appWidget)
            }
        }

        override fun onProvidersChanged() {
            // Providers updated
        }
    }

    fun onStart() {
        runCatching { host.startListening() }
    }

    fun onStop() {
        runCatching { host.stopListening() }
    }

    fun allocateId(): Int = host.allocateAppWidgetId()

    fun tryBind(
        id: Int,
        provider: ComponentName,
        user: UserHandle,
        options: Bundle = Bundle.EMPTY,
    ): Boolean = runCatching {
        appWidgetManager.bindAppWidgetIdIfAllowed(id, user, provider, options)
    }.getOrDefault(false)

    fun release(id: Int) {
        runCatching { host.deleteAppWidgetId(id) }
    }

    fun getInstalledProviders(): List<AppWidgetProviderInfo> =
        runCatching { appWidgetManager.installedProviders }.getOrDefault(emptyList())

    fun getAppWidgetInfo(appWidgetId: Int): AppWidgetProviderInfo? =
        runCatching { appWidgetManager.getAppWidgetInfo(appWidgetId) }.getOrNull()

    fun obtainView(context: Context, appWidgetId: Int): RiftWidgetHostView? {
        val info = getAppWidgetInfo(appWidgetId) ?: return null
        return runCatching {
            host.createView(context, appWidgetId, info) as? RiftWidgetHostView
        }.getOrNull()
    }
}
