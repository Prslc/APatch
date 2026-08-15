package me.bmax.apatch.ui.page.superuser

import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
import kotlin.time.Duration.Companion.milliseconds

class SuperUserViewModel(
    private val suRepo: SuRepository = SuRepositoryImpl,
    private val settingsRepo: SettingsRepository = SettingsRepositoryImpl
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SuperUserUiState(sortBy = loadSortBy())
    )
    val uiState = _uiState.asStateFlow()

    private fun loadSortBy(): SortBy {
        val name = settingsRepo.getString("su_sort_by", SortBy.NAME.name)
        return SortBy.entries.find { it.name == name } ?: SortBy.NAME
    }

    private fun persistSort(sortBy: SortBy) {
        settingsRepo.setString("su_sort_by", sortBy.name)
    }

    private val staticCollator = Collator.getInstance(Locale.getDefault())

    private fun buildComparator(sortBy: SortBy): Comparator<AppInfo> {
        val priorityComparator = compareBy<AppInfo> {
            when {
                it.config.allow != 0 -> 0
                it.config.exclude == 1 -> 1
                else -> 2
            }
        }
        val sortComparator = when (sortBy) {
            SortBy.NAME -> compareBy(staticCollator) { it.label }
            SortBy.PACKAGE_NAME -> compareBy(staticCollator) { it.packageName }
            SortBy.INSTALL_TIME -> compareByDescending<AppInfo> {
                it.packageInfo.firstInstallTime
            }
        }
        return priorityComparator.thenComparing(sortComparator)
    }

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    val filteredApps = combine(
        AppRepository.apps,
        uiState.map { it.search }.distinctUntilChanged().debounce(200.milliseconds),
        uiState.map { it.showSystemApps }.distinctUntilChanged(),
        uiState.map { it.sortBy }.distinctUntilChanged()
    ) { apps, query, showSystem, sortBy ->
        val trimmedQuery = query.trim()

        apps.asSequence()
            .filter { app ->
                if (app.packageName == apApp.packageName) return@filter false
                val isSystem =
                    (app.packageInfo.applicationInfo!!.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val matchSystem = showSystem || !isSystem || app.uid == 2000
                if (!matchSystem) return@filter false

                trimmedQuery.isEmpty() ||
                        app.packageName.contains(trimmedQuery, ignoreCase = true) ||
                        app.lowercaseLabel.contains(trimmedQuery) ||
                        app.pinyinLabel.contains(trimmedQuery)
            }
            .sortedWith(buildComparator(sortBy))
            .toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun fetchAppList() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isRefreshing = true) }
                AppRepository.fetchAppList()
            } catch (e: Exception) {
                Log.e("SuperUserVM", "fetchAppList failed", e)
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
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
    }

    fun toggleSystemApps() {
        _uiState.update { it.copy(showSystemApps = !it.showSystemApps) }
    }

    fun updateSort(sortBy: SortBy) {
        _uiState.update { it.copy(sortBy = sortBy) }
        persistSort(sortBy)
    }
}
