package com.orbit.data.widgets

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

    private val appWidgetManager = AppWidgetManager.getInstance(context)

    private val host = object : AppWidgetHost(context, HOST_ID) {
        override fun onCreateView(
            ctx: Context,
            appWidgetId: Int,
            appWidget: AppWidgetProviderInfo?,
        ): OrbitWidgetHostView {
            return OrbitWidgetHostView(ctx).also {
                it.setAppWidget(appWidgetId, appWidget)
            }
        }

        override fun onProvidersChanged() {
            // TODO: Refresh widget picker and placed widgets
        }
    }

    fun onStart() {
        host.startListening()
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
    ): Boolean = appWidgetManager.bindAppWidgetIdIfAllowed(id, user, provider, options)

    fun release(id: Int) {
        host.deleteAppWidgetId(id)
    }

    fun getInstalledProviders(): List<AppWidgetProviderInfo> =
        appWidgetManager.installedProviders

    fun obtainView(context: Context, appWidgetId: Int): OrbitWidgetHostView {
        return host.createView(context, appWidgetId, appWidgetManager.getAppWidgetInfo(appWidgetId)) as OrbitWidgetHostView
    }
}
