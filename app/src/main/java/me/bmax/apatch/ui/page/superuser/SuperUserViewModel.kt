package me.bmax.apatch.ui.page.superuser

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.bmax.apatch.APApplication
import me.bmax.apatch.apApp
import me.bmax.apatch.data.AppInfo
import me.bmax.apatch.data.AppRepository
import me.bmax.apatch.data.repository.SettingsRepository
import me.bmax.apatch.data.repository.SettingsRepositoryImpl
import me.bmax.apatch.data.repository.SuRepository
import me.bmax.apatch.data.repository.SuRepositoryImpl
import me.bmax.apatch.ui.page.home.ModuleCountsRefresher
import java.text.Collator
import java.util.Locale

class SuperUserViewModel(
    private val suRepo: SuRepository = SuRepositoryImpl,
    private val settingsRepo: SettingsRepository = SettingsRepositoryImpl
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SuperUserUiState(sortBy = loadSortBy())
    )
    val uiState = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        // AppRepository stays the single source used by the WebUI bridge; mirror it into
        // this screen's state so the UI never reads a second flow.
        viewModelScope.launch {
            AppRepository.apps.collect { apps ->
                _uiState.update { it.copy(apps = apps).withVisibleApps() }
            }
        }
    }

    private fun loadSortBy(): SortBy {
        val name = settingsRepo.getString("su_sort_by", SortBy.NAME.name)
        return SortBy.entries.find { it.name == name } ?: SortBy.NAME
    }

    private fun persistSort(sortBy: SortBy) {
        settingsRepo.setString("su_sort_by", sortBy.name)
    }

    private val staticCollator = Collator.getInstance(Locale.getDefault())

    private val priorityComparator = compareBy<AppInfo> {
        when {
            it.config.allow != 0 -> 0
            it.config.exclude == 1 -> 1
            else -> 2
        }
    }

    private val sortComparators = mapOf(
        SortBy.NAME to compareBy(staticCollator) { it.label },
        SortBy.PACKAGE_NAME to compareBy(staticCollator) { it.packageName },
        SortBy.INSTALL_TIME to compareByDescending<AppInfo> { it.firstInstallTime }
    )

    private fun buildComparator(sortBy: SortBy): Comparator<AppInfo> =
        priorityComparator.thenComparing(sortComparators.getValue(sortBy))

    /** Recomputes the single visible list held in [SuperUserUiState]. */
    private fun SuperUserUiState.withVisibleApps(): SuperUserUiState {
        val trimmedQuery = search.trim()

        val visible = apps.asSequence()
            .filter { app ->
                if (app.packageName == apApp.packageName) return@filter false
                val matchSystem = showSystemApps || !app.isSystem || app.uid == 2000
                if (!matchSystem) return@filter false

                trimmedQuery.isEmpty() ||
                        app.packageName.contains(trimmedQuery, ignoreCase = true) ||
                        app.lowercaseLabel.contains(trimmedQuery) ||
                        app.pinyinLabel.contains(trimmedQuery)
            }
            .sortedWith(buildComparator(sortBy))
            .toList()
        return copy(filteredApps = visible)
    }

    fun fetchAppList() {
        if (_uiState.value.isRefreshing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            val result = AppRepository.fetchAppList()
            result.onFailure { Log.e("SuperUserVM", "fetchAppList failed", it) }
            _uiState.update {
                it.copy(
                    apps = AppRepository.apps.value,
                    isRefreshing = false,
                    hasLoaded = result.isSuccess,
                    error = result.exceptionOrNull()
                ).withVisibleApps()
            }
        }
    }

    /**
     * Single entry point for the screens' visibility/resume triggers. Retries after a
     * failed load instead of leaving the page permanently blank.
     */
    fun loadIfNeeded() {
        val state = _uiState.value
        if (state.isRefreshing) return
        if (!state.hasLoaded || state.error != null) fetchAppList()
    }

    fun toggleSu(app: AppInfo, granted: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val nativeOk = if (granted) {
                suRepo.grantSu(app.uid, 0, APApplication.MAGISK_SCONTEXT) == 0L &&
                        suRepo.setUidExclude(app.uid, 0) == 0
            } else {
                suRepo.revokeSu(app.uid) == 0L
            }
            if (!nativeOk) {
                Log.e("SuperUserVM", "toggleSu (granted=$granted) failed for uid ${app.uid}")
                return@launch
            }

            val newConfig = app.config.copy().apply {
                if (granted) {
                    allow = 1
                    exclude = 0
                    profile = profile.copy(
                        uid = app.uid,
                        scontext = APApplication.MAGISK_SCONTEXT
                    )
                } else {
                    allow = 0
                }
            }

            suRepo.changeConfig(newConfig)
            AppRepository.updateLocalConfig(app.uid, newConfig)
            ModuleCountsRefresher.requestRefresh()
        }
    }

    fun toggleExclude(app: AppInfo, excluded: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val nativeOk = if (excluded) {
                suRepo.revokeSu(app.uid) == 0L && suRepo.setUidExclude(app.uid, 1) == 0
            } else {
                suRepo.setUidExclude(app.uid, 0) == 0
            }
            if (!nativeOk) {
                Log.e("SuperUserVM", "toggleExclude (excluded=$excluded) failed for uid ${app.uid}")
                return@launch
            }

            val newConfig = app.config.copy().apply {
                if (excluded) {
                    allow = 0
                    exclude = 1
                } else {
                    exclude = 0
                }
                profile = profile.copy(
                    uid = app.uid,
                    scontext = if (excluded) APApplication.DEFAULT_SCONTEXT else APApplication.MAGISK_SCONTEXT
                )
            }

            suRepo.changeConfig(newConfig)
            AppRepository.updateLocalConfig(app.uid, newConfig)
        }
    }

    override fun onCleared() {
        viewModelScope.launch(Dispatchers.Main) {
            AppRepository.stopRootService()
        }
    }

    fun updateSearch(query: String) {
        _uiState.update { it.copy(search = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(200)
            _uiState.update { it.withVisibleApps() }
        }
    }

    fun toggleSystemApps() {
        _uiState.update { it.copy(showSystemApps = !it.showSystemApps).withVisibleApps() }
    }

    fun updateSort(sortBy: SortBy) {
        _uiState.update { it.copy(sortBy = sortBy).withVisibleApps() }
        persistSort(sortBy)
    }
}
