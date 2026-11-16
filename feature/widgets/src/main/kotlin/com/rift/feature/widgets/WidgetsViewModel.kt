package com.rift.feature.widgets

import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.graphics.Bitmap
import android.os.Process
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rift.data.settings.SettingsRepository
import com.rift.data.widgets.WidgetHostManager
import com.rift.data.workspace.WorkspaceItemEntity
import com.rift.data.workspace.WorkspaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppWidgetGroup(
    val packageName: String,
    val appName: String,
    val appIcon: Bitmap?,
    val widgets: List<AppWidgetProviderInfo>,
)

data class WidgetsUiState(
    val placedWidgets: List<WorkspaceItemEntity> = emptyList(),
    val availableProviders: List<AppWidgetProviderInfo> = emptyList(),
    val widgetGroups: List<AppWidgetGroup> = emptyList(),
    val useSystemWallpaper: Boolean = true,
)

@HiltViewModel
class WidgetsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    val widgetHostManager: WidgetHostManager,
    private val workspaceRepository: WorkspaceRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _availableProviders = MutableStateFlow<List<AppWidgetProviderInfo>>(emptyList())
    val availableProviders: StateFlow<List<AppWidgetProviderInfo>> = _availableProviders.asStateFlow()

    private val _widgetGroups = MutableStateFlow<List<AppWidgetGroup>>(emptyList())
    val widgetGroups: StateFlow<List<AppWidgetGroup>> = _widgetGroups.asStateFlow()

    val uiState: StateFlow<WidgetsUiState> = combine(
        workspaceRepository.getAllWidgets(),
        settingsRepository.settings,
        _availableProviders,
        _widgetGroups,
    ) { widgets, settings, providers, groups ->
        WidgetsUiState(
            placedWidgets = widgets.sortedBy { it.rank },
            availableProviders = providers,
            widgetGroups = groups,
            useSystemWallpaper = settings.useSystemWallpaper,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WidgetsUiState(),
    )

    init {
        loadAvailableProviders()
    }

    fun loadAvailableProviders() {
        viewModelScope.launch(Dispatchers.IO) {
            val providers = widgetHostManager.getInstalledProviders()
            val pm = context.packageManager
            val groups = providers.groupBy { it.provider.packageName }.map { (pkg, widgetList) ->
                val appInfo = runCatching { pm.getApplicationInfo(pkg, 0) }.getOrNull()
                val appName = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: pkg
                val appIcon = runCatching { appInfo?.loadIcon(pm)?.toBitmap(width = 96, height = 96) }.getOrNull()
                AppWidgetGroup(
                    packageName = pkg,
                    appName = appName,
                    appIcon = appIcon,
                    widgets = widgetList.sortedBy { it.loadLabel(pm) },
                )
            }.sortedBy { it.appName.lowercase() }

            _availableProviders.value = providers
            _widgetGroups.value = groups
        }
    }

    /**
     * Allocates an AppWidget ID, binds the provider, and saves the entity in Room.
     */
    fun addAppWidget(provider: AppWidgetProviderInfo, onRequireBindPermission: (Int, AppWidgetProviderInfo) -> Unit) {
        viewModelScope.launch {
            val appWidgetId = widgetHostManager.allocateId()
            val canBind = widgetHostManager.tryBind(appWidgetId, provider.provider, Process.myUserHandle())

            if (canBind) {
                // Successfully bound, persist to workspace
                val entity = WorkspaceItemEntity(
                    type = WorkspaceItemEntity.TYPE_WIDGET,
                    container = WorkspaceItemEntity.CONTAINER_DESKTOP,
                    page = 0,
                    cellX = 0,
                    cellY = 0,
                    spanX = 4,
                    spanY = 2,
                    appWidgetId = appWidgetId,
                    widgetProvider = provider.provider.flattenToString(),
                    title = provider.loadLabel(context.packageManager),
                    rank = uiState.value.placedWidgets.size,
                )
                workspaceRepository.addItem(entity)
            } else {
                // System permission dialog required
                onRequireBindPermission(appWidgetId, provider)
            }
        }
    }

    fun onWidgetBindPermissionGranted(appWidgetId: Int, provider: AppWidgetProviderInfo) {
        viewModelScope.launch {
            val entity = WorkspaceItemEntity(
                type = WorkspaceItemEntity.TYPE_WIDGET,
                container = WorkspaceItemEntity.CONTAINER_DESKTOP,
                page = 0,
                cellX = 0,
                cellY = 0,
                spanX = 4,
                spanY = 2,
                appWidgetId = appWidgetId,
                widgetProvider = provider.provider.flattenToString(),
                title = provider.loadLabel(context.packageManager),
                rank = uiState.value.placedWidgets.size,
            )
            workspaceRepository.addItem(entity)
        }
    }

    fun removeWidget(item: WorkspaceItemEntity) {
        viewModelScope.launch {
            item.appWidgetId?.let { widgetHostManager.release(it) }
            workspaceRepository.removeItem(item.id)
        }
    }

    fun moveWidget(item: WorkspaceItemEntity, direction: Int) {
        viewModelScope.launch {
            val list = uiState.value.placedWidgets.toMutableList()
            val index = list.indexOfFirst { it.id == item.id }
            val newIndex = (index + direction).coerceIn(0, list.size - 1)
            if (index != -1 && index != newIndex) {
                list.removeAt(index)
                list.add(newIndex, item)
                list.forEachIndexed { i, w ->
                    workspaceRepository.updateItem(w.copy(rank = i))
                }
            }
        }
    }
}
