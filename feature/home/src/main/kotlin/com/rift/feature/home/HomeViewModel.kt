package com.rift.feature.home

import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Process
import android.provider.Settings
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rift.data.apps.AppInfo
import com.rift.data.apps.AppsRepository
import com.rift.data.settings.SettingsRepository
import com.rift.data.widgets.AppWidgetGroup
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

data class HomeUiState(
    val hasCompletedSetup: Boolean = false,
    val useSystemWallpaper: Boolean = true,
    val iconShape: String = "SYSTEM",
    val iconPackPackage: String? = null,
    val favoriteApps: List<AppInfo> = emptyList(),
    val desktopApps: List<AppInfo> = emptyList(),
    val homeWidgets: List<WorkspaceItemEntity> = emptyList(),
    val allInstalledApps: List<AppInfo> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appsRepository: AppsRepository,
    private val settingsRepository: SettingsRepository,
    val widgetHostManager: WidgetHostManager,
    private val workspaceRepository: WorkspaceRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private val _widgetGroups = MutableStateFlow<List<AppWidgetGroup>>(emptyList())
    val widgetGroups: StateFlow<List<AppWidgetGroup>> = _widgetGroups.asStateFlow()

    init {
        loadWidgetGroups()
    }

    fun loadWidgetGroups() {
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

            _widgetGroups.value = groups
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        appsRepository.allApps,
        settingsRepository.settings,
        workspaceRepository.getWidgetsByContainer(WorkspaceItemEntity.CONTAINER_DESKTOP),
    ) { apps, settings, widgets ->
        val appMap = apps.associateBy { it.packageName }

        // 1. Identify Candidate Suggested Apps if none set
        val candidates = mutableListOf<AppInfo>()
        val phoneApp = apps.find { it.packageName.contains("dialer") || it.packageName.contains("phone") }
        val msgApp = apps.find { it.packageName.contains("messaging") || it.packageName.contains("mms") }
        val whatsappApp = apps.find { it.packageName.contains("whatsapp") }
        val snapchatApp = apps.find { it.packageName.contains("snapchat") }
        val browserApp = apps.find { it.packageName.contains("chrome") || it.packageName.contains("browser") }
        val cameraApp = apps.find { it.packageName.contains("camera") }

        listOfNotNull(phoneApp, msgApp, whatsappApp, snapchatApp, browserApp, cameraApp).forEach {
            if (!candidates.contains(it)) candidates.add(it)
        }

        for (app in apps) {
            if (candidates.size >= 8) break
            if (!candidates.contains(app)) candidates.add(app)
        }

        // 2. Resolve Active Favorite Apps (Dock)
        val favoriteApps = if (settings.favoriteAppPackages.isNotEmpty()) {
            settings.favoriteAppPackages.mapNotNull { appMap[it] }
        } else {
            candidates.take(5)
        }

        // 3. Resolve Desktop Pinned Apps
        val desktopApps = settings.desktopAppPackages.mapNotNull { appMap[it] }

        HomeUiState(
            hasCompletedSetup = settings.hasCompletedSetup,
            useSystemWallpaper = settings.useSystemWallpaper,
            iconShape = settings.iconShape,
            iconPackPackage = settings.iconPackPackage,
            favoriteApps = favoriteApps,
            desktopApps = desktopApps,
            homeWidgets = widgets.sortedBy { it.rank },
            allInstalledApps = apps,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(),
    )

    fun launchApp(app: AppInfo) {
        appsRepository.launchApp(app)
    }

    /**
     * Direct Touch Reorder: Move favorite app from slot [fromIndex] to [toIndex].
     */
    fun reorderFavoriteApps(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val currentList = uiState.value.favoriteApps.map { it.packageName }.toMutableList()
            if (fromIndex in currentList.indices && toIndex in currentList.indices && fromIndex != toIndex) {
                val item = currentList.removeAt(fromIndex)
                currentList.add(toIndex, item)
                settingsRepository.update { it.copy(favoriteAppPackages = currentList) }
            }
        }
    }

    /**
     * Direct Touch Reorder: Move desktop app from slot [fromIndex] to [toIndex].
     */
    fun reorderDesktopApps(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val currentList = uiState.value.desktopApps.map { it.packageName }.toMutableList()
            if (fromIndex in currentList.indices && toIndex in currentList.indices && fromIndex != toIndex) {
                val item = currentList.removeAt(fromIndex)
                currentList.add(toIndex, item)
                settingsRepository.update { it.copy(desktopAppPackages = currentList) }
            }
        }
    }

    fun addAppToDesktop(app: AppInfo) {
        viewModelScope.launch {
            val currentList = uiState.value.desktopApps.map { it.packageName }.toMutableList()
            if (!currentList.contains(app.packageName)) {
                currentList.add(app.packageName)
                settingsRepository.update { it.copy(desktopAppPackages = currentList) }
            }
        }
    }

    fun removeAppFromDesktop(app: AppInfo) {
        viewModelScope.launch {
            val currentList = uiState.value.desktopApps.map { it.packageName }.toMutableList()
            currentList.remove(app.packageName)
            settingsRepository.update { it.copy(desktopAppPackages = currentList) }
        }
    }

    fun addAppToDock(app: AppInfo) {
        viewModelScope.launch {
            val currentList = uiState.value.favoriteApps.map { it.packageName }.toMutableList()
            if (!currentList.contains(app.packageName) && currentList.size < 5) {
                currentList.add(app.packageName)
                settingsRepository.update { it.copy(favoriteAppPackages = currentList) }
            }
        }
    }

    fun removeAppFromDock(app: AppInfo) {
        viewModelScope.launch {
            val currentList = uiState.value.favoriteApps.map { it.packageName }.toMutableList()
            currentList.remove(app.packageName)
            settingsRepository.update { it.copy(favoriteAppPackages = currentList) }
        }
    }

    fun openAppDetails(context: Context, app: AppInfo) {
        runCatching {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", app.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun requestUninstallApp(context: Context, app: AppInfo) {
        runCatching {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.fromParts("package", app.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun addHomeAppWidget(provider: AppWidgetProviderInfo, onRequireBindPermission: (Int, AppWidgetProviderInfo) -> Unit) {
        viewModelScope.launch {
            val appWidgetId = widgetHostManager.allocateId()
            val canBind = widgetHostManager.tryBind(appWidgetId, provider.provider, Process.myUserHandle())
            if (canBind) {
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
                    rank = uiState.value.homeWidgets.size,
                )
                workspaceRepository.addItem(entity)
            } else {
                onRequireBindPermission(appWidgetId, provider)
            }
        }
    }

    fun onHomeWidgetBindPermissionGranted(appWidgetId: Int, provider: AppWidgetProviderInfo) {
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
                rank = uiState.value.homeWidgets.size,
            )
            workspaceRepository.addItem(entity)
        }
    }

    fun removeHomeWidget(item: WorkspaceItemEntity) {
        viewModelScope.launch {
            item.appWidgetId?.let { widgetHostManager.release(it) }
            workspaceRepository.removeItem(item.id)
        }
    }

    fun moveHomeWidget(item: WorkspaceItemEntity, delta: Int) {
        viewModelScope.launch {
            val list = uiState.value.homeWidgets.toMutableList()
            val index = list.indexOfFirst { it.id == item.id }
            val targetIndex = index + delta
            if (index != -1 && targetIndex in list.indices) {
                val current = list.removeAt(index)
                list.add(targetIndex, current)
                list.forEachIndexed { i, w ->
                    workspaceRepository.updateItem(w.copy(rank = i))
                }
            }
        }
    }
}
