package com.rift.feature.settings

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rift.core.icons.IconPackInfo
import com.rift.core.icons.IconPackManager
import com.rift.data.apps.AppInfo
import com.rift.data.apps.AppsRepository
import com.rift.data.settings.DarkMode
import com.rift.data.settings.DrawerSortMode
import com.rift.data.settings.RiftSettings
import com.rift.data.settings.SettingsRepository
import com.rift.data.settings.updater.UpdateCheckResult
import com.rift.data.settings.updater.UpdateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data class Available(val update: UpdateCheckResult.UpdateAvailable) : UpdateUiState
    data class Downloading(val progress: Int) : UpdateUiState
    data object ReadyToInstall : UpdateUiState
    data object UpToDate : UpdateUiState
    data class Error(val message: String) : UpdateUiState
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val appsRepository: AppsRepository,
    private val updateManager: UpdateManager,
    private val iconPackManager: IconPackManager,
) : ViewModel() {

    val settings: StateFlow<RiftSettings> = settingsRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RiftSettings()
        )

    private val _installedIconPacks = MutableStateFlow<List<IconPackInfo>>(emptyList())
    val installedIconPacks: StateFlow<List<IconPackInfo>> = _installedIconPacks.asStateFlow()

    init {
        loadInstalledIconPacks()
    }

    fun loadInstalledIconPacks() {
        viewModelScope.launch {
            _installedIconPacks.value = iconPackManager.getInstalledIconPacks()
        }
    }

    val hiddenApps: StateFlow<List<AppInfo>> = combine(
        appsRepository.allApps,
        settingsRepository.settings,
    ) { apps, s ->
        val hiddenSet = s.hiddenAppPackages.toSet()
        apps.filter { hiddenSet.contains(it.packageName) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val allApps: StateFlow<List<AppInfo>> = appsRepository.allApps
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _updateUiState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val updateUiState: StateFlow<UpdateUiState> = _updateUiState.asStateFlow()

    val currentVersionName: String = "1.0.11"

    fun checkForUpdates() {
        viewModelScope.launch {
            _updateUiState.value = UpdateUiState.Checking
            val currentRepo = settings.value.updateRepo
            when (val result = updateManager.checkForUpdates(currentVersionName, currentRepo)) {
                is UpdateCheckResult.UpdateAvailable -> {
                    _updateUiState.value = UpdateUiState.Available(result)
                }
                is UpdateCheckResult.UpToDate -> {
                    _updateUiState.value = UpdateUiState.UpToDate
                }
                is UpdateCheckResult.Error -> {
                    _updateUiState.value = UpdateUiState.Error(result.message)
                }
            }
        }
    }

    fun downloadAndInstallUpdate(downloadUrl: String) {
        viewModelScope.launch {
            _updateUiState.value = UpdateUiState.Downloading(0)
            try {
                updateManager.downloadApk(downloadUrl).collect { progress ->
                    _updateUiState.value = UpdateUiState.Downloading(progress)
                }
                _updateUiState.value = UpdateUiState.ReadyToInstall
                updateManager.triggerInstall()
            } catch (e: Exception) {
                _updateUiState.value = UpdateUiState.Error(e.localizedMessage ?: "فشل تحميل التحديث")
            }
        }
    }

    fun unhideApp(packageName: String) {
        viewModelScope.launch {
            val current = settingsRepository.settings.first().hiddenAppPackages.toMutableList()
            current.remove(packageName)
            settingsRepository.update { it.copy(hiddenAppPackages = current) }
        }
    }

    fun hideApp(packageName: String) {
        viewModelScope.launch {
            val current = settingsRepository.settings.first().hiddenAppPackages.toMutableList()
            if (!current.contains(packageName)) {
                current.add(packageName)
                settingsRepository.update { it.copy(hiddenAppPackages = current) }
            }
        }
    }

    fun setGridSize(columns: Int, rows: Int) {
        viewModelScope.launch {
            settingsRepository.update {
                it.copy(gridColumns = columns, gridRows = rows)
            }
        }
    }

    fun setDarkMode(mode: DarkMode) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(darkMode = mode) }
        }
    }

    fun setAutoCategorize(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(autoCategorizeEnabled = enabled) }
        }
    }

    fun setUseSystemWallpaper(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(useSystemWallpaper = enabled) }
        }
    }

    fun setAutoCheckUpdates(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(autoCheckUpdates = enabled) }
        }
    }

    fun resetSetup() {
        viewModelScope.launch {
            settingsRepository.update { it.copy(hasCompletedSetup = false) }
        }
    }

    fun resetUpdateState() {
        _updateUiState.value = UpdateUiState.Idle
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(dynamicColor = enabled) }
        }
    }

    fun openInObtainium(context: Context) {
        val repo = settings.value.updateRepo
        val obtainiumIntent = Intent(Intent.ACTION_VIEW, updateManager.getObtainiumUri(repo)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val fallbackIntent = Intent(Intent.ACTION_VIEW, updateManager.getObtainiumDownloadUrl()).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        runCatching {
            context.startActivity(obtainiumIntent)
        }.recoverCatching {
            context.startActivity(fallbackIntent)
        }
    }

    fun openGitHubReleases(context: Context) {
        val repo = settings.value.updateRepo
        val intent = Intent(Intent.ACTION_VIEW, updateManager.getGitHubReleasesUrl(repo)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        runCatching {
            context.startActivity(intent)
        }
    }

    fun setDrawerSortMode(mode: DrawerSortMode) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(drawerSortMode = mode) }
        }
    }

    fun setIconShape(shape: String) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(iconShape = shape) }
        }
    }

    fun setIconPack(packageName: String?) {
        viewModelScope.launch {
            iconPackManager.clearCache()
            settingsRepository.update { it.copy(iconPackPackage = packageName) }
        }
    }

    fun openPlayStoreForIconPacks(context: Context) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("market://search?q=icon+pack&c=apps")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }.recoverCatching {
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/search?q=icon+pack&c=apps")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
}
