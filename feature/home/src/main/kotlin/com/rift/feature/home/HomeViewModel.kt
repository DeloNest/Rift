package com.rift.feature.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rift.data.apps.AppInfo
import com.rift.data.apps.AppsRepository
import com.rift.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    val allInstalledApps: List<AppInfo> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appsRepository: AppsRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        appsRepository.allApps,
        settingsRepository.settings,
    ) { apps, settings ->
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
}
