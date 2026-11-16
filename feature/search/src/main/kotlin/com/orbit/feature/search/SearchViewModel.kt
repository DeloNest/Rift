package com.orbit.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.data.apps.AppInfo
import com.orbit.data.apps.AppsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false,
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val appsRepository: AppsRepository,
    private val searchEngine: SearchEngine,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<List<SearchResult>>(emptyList())
    val results: StateFlow<List<SearchResult>> = _results.asStateFlow()

    init {
        // Keep search index synchronized with installed apps
        viewModelScope.launch {
            appsRepository.allApps.collect { apps ->
                searchEngine.updateApps(apps)
                if (_query.value.isNotBlank()) {
                    _results.value = searchEngine.search(_query.value)
                }
            }
        }
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        if (newQuery.isBlank()) {
            _results.value = emptyList()
        } else {
            _results.value = searchEngine.search(newQuery)
        }
    }

    fun onAppClick(app: AppInfo) {
        appsRepository.launchApp(app)
    }

    fun clearQuery() {
        onQueryChange("")
    }
}
