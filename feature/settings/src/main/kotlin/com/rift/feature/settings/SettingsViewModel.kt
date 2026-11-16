package com.rift.feature.settings

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rift.data.settings.DarkMode
import com.rift.data.settings.RiftSettings
import com.rift.data.settings.SettingsRepository
import com.rift.data.settings.updater.UpdateCheckResult
import com.rift.data.settings.updater.UpdateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val updateManager: UpdateManager,
) : ViewModel() {

    val settings: StateFlow<RiftSettings> = settingsRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RiftSettings()
        )

    private val _updateUiState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val updateUiState: StateFlow<UpdateUiState> = _updateUiState.asStateFlow()

    val currentVersionName: String = "1.0.1"

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

    fun openInObtainium(context: Context) {
        val obtainiumUri = updateManager.getObtainiumUri(settings.value.updateRepo)
        val intent = Intent(Intent.ACTION_VIEW, obtainiumUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val browserIntent = Intent(Intent.ACTION_VIEW, updateManager.getObtainiumDownloadUrl()).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(browserIntent)
        }
    }

    fun openGitHubReleases(context: Context) {
        val uri = updateManager.getGitHubReleasesUrl(settings.value.updateRepo)
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun resetUpdateState() {
        _updateUiState.value = UpdateUiState.Idle
    }

    fun setDarkMode(mode: DarkMode) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(darkMode = mode) }
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(dynamicColor = enabled) }
        }
    }

    fun setAutoCategorize(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(autoCategorizeEnabled = enabled) }
        }
    }

    fun setAutoCheckUpdates(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(autoCheckUpdates = enabled) }
        }
    }
}
