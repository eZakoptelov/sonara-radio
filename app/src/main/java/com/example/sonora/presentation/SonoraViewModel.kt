package com.example.sonora.presentation

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sonora.data.settings.AppTheme
import com.example.sonora.data.settings.BufferMode
import com.example.sonora.data.settings.EqualizerSettings
import com.example.sonora.data.settings.SettingsRepository
import com.example.sonora.domain.model.RadioLanguage
import com.example.sonora.domain.model.RadioStation
import com.example.sonora.domain.repository.RadioRepository
import com.example.sonora.presentation.localization.uiText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class SonoraUiState(
    val popularStations: List<RadioStation> = emptyList(),
    val searchResults: List<RadioStation> = emptyList(),
    val favorites: List<RadioStation> = emptyList(),
    val history: List<RadioStation> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMoreStations: Boolean = false,
    val loadMoreError: String? = null,
    val isSearching: Boolean = false,
    val isLoadingMoreSearch: Boolean = false,
    val hasMoreSearchResults: Boolean = false,
    val searchLoadMoreError: String? = null,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedGenre: String? = null,
    val currentStation: RadioStation? = null,
    val radioLanguage: RadioLanguage? = null,
    val isSettingsLoaded: Boolean = false,
    val bufferMode: BufferMode = BufferMode.AUTO,
    val appTheme: AppTheme = AppTheme.NEON,
    val equalizerSettings: EqualizerSettings = EqualizerSettings()
)

class SonoraViewModel(
    private val repository: RadioRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private companion object {
        const val PAGE_SIZE = 10
        const val FILTER_ALL = "all"
    }

    private val _uiState = MutableStateFlow(SonoraUiState())
    val uiState = _uiState.asStateFlow()
    private var searchJob: Job? = null
    private var catalogJob: Job? = null
    private var loadMoreJob: Job? = null
    private var nextOffset = 0
    private var nextSearchOffset = 0
    private var allFavorites: List<RadioStation> = emptyList()
    private var allHistory: List<RadioStation> = emptyList()

    private fun stationsForSelectedLanguage(stations: List<RadioStation>, language: RadioLanguage?) =
        if (language == null) stations
        else stations.filter { language.matchesStationLanguage(it.language) }

    init {
        observeLibrary()
        observeSettings()
        _uiState.update { it.copy(equalizerSettings = settingsRepository.getEqualizerSettings()) }
        // The catalogue is intentionally not loaded here. On a fresh install the user
        // must choose the preferred station language first.
    }

    private fun observeLibrary() {
        viewModelScope.launch {
            repository.observeFavorites().collect { favorites ->
                allFavorites = favorites
                _uiState.update {
                    it.copy(favorites = stationsForSelectedLanguage(allFavorites, it.radioLanguage))
                }
            }
        }
        viewModelScope.launch {
            repository.observeHistory().collect { history ->
                allHistory = history
                _uiState.update {
                    it.copy(history = stationsForSelectedLanguage(allHistory, it.radioLanguage))
                }
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.radioLanguage.collect { language ->
                val previousLanguage = _uiState.value.radioLanguage
                _uiState.update {
                    it.copy(
                        radioLanguage = language,
                        isSettingsLoaded = true,
                        favorites = stationsForSelectedLanguage(allFavorites, language),
                        history = stationsForSelectedLanguage(allHistory, language),
                        selectedGenre = null,
                        searchQuery = if (previousLanguage != language) "" else it.searchQuery,
                        searchResults = if (previousLanguage != language) emptyList() else it.searchResults,
                        isSearching = false,
                        isLoadingMoreSearch = false,
                        hasMoreSearchResults = false,
                        searchLoadMoreError = null,
                        loadMoreError = null,
                        error = null
                    )
                }
                searchJob?.cancel()

                if (language != null && language != previousLanguage) {
                    loadCatalog(filter = null, language = language)
                }
            }
        }
        viewModelScope.launch {
            settingsRepository.bufferMode.collect { mode ->
                _uiState.update { it.copy(bufferMode = mode) }
            }
        }
        viewModelScope.launch {
            settingsRepository.appTheme.collect { theme ->
                _uiState.update { it.copy(appTheme = theme) }
            }
        }
    }

    fun setRadioLanguage(language: RadioLanguage) {
        // Apply the choice to the UI immediately. The first-launch screen is
        // controlled by radioLanguage == null, so waiting only for DataStore
        // can leave the user on the setup screen until its Flow emits again.
        searchJob?.cancel()
        _uiState.update { state ->
            state.copy(
                radioLanguage = language,
                isSettingsLoaded = true,
                favorites = stationsForSelectedLanguage(allFavorites, language),
                history = stationsForSelectedLanguage(allHistory, language),
                selectedGenre = null,
                searchQuery = "",
                searchResults = emptyList(),
                isSearching = false,
                isLoadingMoreSearch = false,
                hasMoreSearchResults = false,
                searchLoadMoreError = null,
                loadMoreError = null,
                error = null
            )
        }

        // Load the first page right away; DataStore persistence runs in parallel.
        loadCatalog(filter = null, language = language)
        viewModelScope.launch {
            settingsRepository.setRadioLanguage(language)
        }
    }

    fun loadPopular() {
        loadCatalog(filter = null)
    }

    /** Filter keys come from HomeScreen: country:RU, tag:pop, tag:rock, etc. */
    fun loadGenre(filter: String) {
        if (filter == FILTER_ALL) {
            loadPopular()
        } else {
            loadCatalog(filter)
        }
    }

    private fun loadCatalog(
        filter: String?,
        language: RadioLanguage? = _uiState.value.radioLanguage
    ) {
        if (language == null) return

        catalogJob?.cancel()
        loadMoreJob?.cancel()
        catalogJob = viewModelScope.launch {
            nextOffset = 0
            _uiState.update {
                it.copy(
                    isLoading = true,
                    isLoadingMore = false,
                    hasMoreStations = false,
                    loadMoreError = null,
                    error = null,
                    selectedGenre = filter,
                    popularStations = emptyList()
                )
            }

            runCatching { fetchCatalogPage(filter, offset = 0, language = language) }
                .onSuccess { stations ->
                    nextOffset = PAGE_SIZE
                    _uiState.update {
                        it.copy(
                            popularStations = stations,
                            isLoading = false,
                            hasMoreStations = stations.size == PAGE_SIZE,
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            hasMoreStations = false,
                            error = error.message ?: uiText(language, "error.load_stations")
                        )
                    }
                }
        }
    }

    fun loadMoreStations() {
        val current = _uiState.value
        val language = current.radioLanguage ?: return
        if (current.isLoading || current.isLoadingMore || !current.hasMoreStations) return

        val filter = current.selectedGenre
        val offset = nextOffset
        loadMoreJob?.cancel()
        loadMoreJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true, loadMoreError = null) }
            runCatching { fetchCatalogPage(filter, offset, language) }
                .onSuccess { stations ->
                    nextOffset = offset + PAGE_SIZE
                    _uiState.update { state ->
                        state.copy(
                            popularStations = (state.popularStations + stations).distinctBy { it.id },
                            isLoadingMore = false,
                            hasMoreStations = stations.size == PAGE_SIZE,
                            loadMoreError = null,
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            loadMoreError = error.message ?: uiText(language, "error.load_more_stations")
                        )
                    }
                }
        }
    }

    private suspend fun fetchCatalogPage(
        filter: String?,
        offset: Int,
        language: RadioLanguage
    ): List<RadioStation> =
        when (filter) {
            null, FILTER_ALL -> repository.getPopularStations(language, PAGE_SIZE, offset)
            else -> repository.getStationsByTag(
                tag = filter.removePrefix("tag:"),
                language = language,
                limit = PAGE_SIZE,
                offset = offset
            )
        }

    fun onSearchQueryChange(query: String) {
        searchJob?.cancel()
        val language = _uiState.value.radioLanguage
        if (query.isBlank() || language == null) {
            _uiState.update {
                it.copy(
                    searchQuery = query,
                    searchResults = emptyList(),
                    isSearching = false,
                    isLoadingMoreSearch = false,
                    hasMoreSearchResults = false,
                    searchLoadMoreError = null,
                    error = null
                )
            }
            return
        }
        _uiState.update {
            it.copy(
                searchQuery = query,
                searchResults = emptyList(),
                isSearching = true,
                isLoadingMoreSearch = false,
                hasMoreSearchResults = false,
                searchLoadMoreError = null,
                error = null
            )
        }
        searchJob = viewModelScope.launch {
            delay(350)
            runCatching { repository.searchStations(query.trim(), language, limit = PAGE_SIZE) }
                .onSuccess { stations ->
                    nextSearchOffset = PAGE_SIZE
                    _uiState.update {
                        it.copy(
                            searchResults = stations,
                            isSearching = false,
                            hasMoreSearchResults = stations.size == PAGE_SIZE,
                            searchLoadMoreError = null
                        )
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    _uiState.update {
                        it.copy(isSearching = false, error = error.message ?: uiText(language, "error.search"))
                    }
                }
        }
    }

    fun loadMoreSearchResults() {
        val current = _uiState.value
        val language = current.radioLanguage ?: return
        val query = current.searchQuery.trim()
        if (query.isBlank() || current.isSearching || current.isLoadingMoreSearch ||
            !current.hasMoreSearchResults
        ) return

        val offset = nextSearchOffset
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoreSearch = true, searchLoadMoreError = null) }
            runCatching {
                repository.searchStations(query, language, limit = PAGE_SIZE, offset = offset)
            }
                .onSuccess { stations ->
                    nextSearchOffset = offset + PAGE_SIZE
                    _uiState.update { state ->
                        state.copy(
                            searchResults = (state.searchResults + stations).distinctBy { it.id },
                            isLoadingMoreSearch = false,
                            hasMoreSearchResults = stations.size == PAGE_SIZE,
                            searchLoadMoreError = null
                        )
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    _uiState.update {
                        it.copy(
                            isLoadingMoreSearch = false,
                            searchLoadMoreError = error.message ?: uiText(language, "error.load_search_results")
                        )
                    }
                }
        }
    }

    fun toggleFavorite(station: RadioStation) {
        viewModelScope.launch { repository.toggleFavorite(station) }
    }

    fun onStationSelected(station: RadioStation) {
        _uiState.update { it.copy(currentStation = station) }
        viewModelScope.launch { repository.recordPlay(station) }
    }

    fun setBufferMode(mode: BufferMode) {
        viewModelScope.launch { settingsRepository.setBufferMode(mode) }
    }

    fun setAppTheme(theme: AppTheme) {
        viewModelScope.launch { settingsRepository.setAppTheme(theme) }
    }

    fun setEqualizerSettings(settings: EqualizerSettings) {
        val normalized = settings.normalized()
        _uiState.update { it.copy(equalizerSettings = normalized) }
        settingsRepository.setEqualizerSettings(normalized)
    }

    fun clearHistory() {
        viewModelScope.launch { repository.clearHistory() }
    }
}
