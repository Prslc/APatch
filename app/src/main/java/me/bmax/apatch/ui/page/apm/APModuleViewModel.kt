package me.bmax.apatch.ui.page.apm

import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.data.repository.ApModuleRepository
import me.bmax.apatch.data.repository.ApModuleRepositoryImpl
import me.bmax.apatch.data.repository.SettingsRepository
import me.bmax.apatch.data.repository.SettingsRepositoryImpl
import me.bmax.apatch.ui.page.home.ModuleCountsRefresher
import me.bmax.apatch.util.hasMagisk
import java.text.Collator
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

private const val UPDATE_CACHE_TTL_MS = 5 * 60 * 1000L

class APModuleViewModel(
    private val moduleRepo: ApModuleRepository = ApModuleRepositoryImpl,
    private val settingsRepo: SettingsRepository = SettingsRepositoryImpl
) : ViewModel() {
    companion object {
        private const val TAG = "ModuleViewModel"
    }

    private val _uiState = MutableStateFlow(APMUiState().loadSortMode())
    val uiState = _uiState.asStateFlow()

    private data class CachedUpdate(
        val checkedAt: Long,
        val versionCode: Int,
        val result: Triple<String, String, String>
    )

    private val updateCache = ConcurrentHashMap<String, CachedUpdate>()

    private var refreshJob: Job? = null
    private var updateJob: Job? = null
    private var searchJob: Job? = null

    /** Incremented on every list load so a stale update check cannot publish. */
    private var fetchGeneration = 0

    init {
        refreshMagiskState()
    }

    private fun refreshMagiskState() {
        viewModelScope.launch {
            val hasMagisk = withContext(Dispatchers.IO) { hasMagisk() }
            _uiState.update { it.copy(hasMagisk = hasMagisk) }
        }
    }

    private fun APMUiState.loadSortMode(): APMUiState =
        when (settingsRepo.getString("apm_sort_mode", "")) {
            "ENABLED_FIRST" -> copy(sortEnabledFirst = true)
            "ACTION_FIRST" -> copy(sortActionFirst = true)
            "WEB_FIRST" -> copy(sortWebFirst = true)
            else -> this
        }

    private val staticCollator = Collator.getInstance(Locale.getDefault())

    /** Recomputes the single visible list held in [APMUiState]. */
    private fun APMUiState.withVisibleModules(): APMUiState {
        val trimmedQuery = search.trim()
        val visible = modules.filter {
            trimmedQuery.isEmpty() ||
                    it.id.contains(trimmedQuery, true) ||
                    it.name.contains(trimmedQuery, true) ||
                    it.pinyinName.contains(trimmedQuery, true)
        }.sortedWith(moduleComparator(sortEnabledFirst, sortActionFirst, sortWebFirst))
        return copy(filteredModules = visible)
    }

    private fun moduleComparator(
        sortEnabledFirst: Boolean,
        sortActionFirst: Boolean,
        sortWebFirst: Boolean
    ): Comparator<ModuleInfo> {
        return compareBy<ModuleInfo>(
            {
                val executable = it.hasWebUi || it.hasActionScript
                when {
                    it.metamodule && it.enabled -> 0
                    sortWebFirst && it.hasWebUi -> 1
                    sortEnabledFirst && it.enabled -> 2
                    sortActionFirst && executable -> 3
                    else -> 4
                }
            },
            { if (sortWebFirst) !it.hasWebUi else 0 },
            { if (sortEnabledFirst) !it.enabled else 0 },
            { if (sortActionFirst) !(it.hasWebUi || it.hasActionScript) else 0 }
        ).thenBy(staticCollator) { it.id }
    }

    fun onSearchChange(newSearch: String) {
        _uiState.update { it.copy(search = newSearch) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(200)
            _uiState.update { it.withVisibleModules() }
        }
    }

    fun markNeedRefresh() {
        _uiState.update { it.copy(isNeedRefresh = true) }
    }

    /**
     * Single entry point for the screens' visibility/resume triggers. Keeps the retry
     * decision in the ViewModel and never re-enters while a load is in flight.
     */
    fun loadIfNeeded() {
        val state = _uiState.value
        if (state.isRefreshing) return
        if (!state.hasLoaded || state.isNeedRefresh) fetchModuleList()
    }

    fun resetSort() {
        _uiState.update {
            it.copy(
                sortEnabledFirst = false,
                sortActionFirst = false,
                sortWebFirst = false
            ).withVisibleModules()
        }
        persistSort()
    }

    fun selectSortEnabledFirst() {
        if (_uiState.value.sortEnabledFirst) return
        _uiState.update {
            it.copy(
                sortEnabledFirst = true,
                sortActionFirst = false,
                sortWebFirst = false
            ).withVisibleModules()
        }
        persistSort()
    }

    fun selectSortActionFirst() {
        if (_uiState.value.sortActionFirst) return
        _uiState.update {
            it.copy(
                sortActionFirst = true,
                sortEnabledFirst = false,
                sortWebFirst = false
            ).withVisibleModules()
        }
        persistSort()
    }

    fun selectSortWebFirst() {
        if (_uiState.value.sortWebFirst) return
        _uiState.update {
            it.copy(
                sortWebFirst = true,
                sortEnabledFirst = false,
                sortActionFirst = false
            ).withVisibleModules()
        }
        persistSort()
    }

    private fun persistSort() {
        val s = _uiState.value
        val mode = when {
            s.sortEnabledFirst -> "ENABLED_FIRST"
            s.sortActionFirst -> "ACTION_FIRST"
            s.sortWebFirst -> "WEB_FIRST"
            else -> ""
        }
        settingsRepo.setString("apm_sort_mode", mode)
    }

    suspend fun toggleModule(id: String, enable: Boolean): Boolean {
        return moduleRepo.toggleModule(id, enable)
    }

    suspend fun uninstallModule(id: String): Boolean {
        return moduleRepo.uninstallModule(id)
    }

    suspend fun undoUninstallModule(id: String): Boolean {
        return moduleRepo.undoUninstallModule(id)
    }

    private suspend fun checkMetaModuleWarning(modules: List<ModuleInfo>) {
        val warning = withContext(Dispatchers.IO) {
            moduleRepo.checkMetaModuleWarning(modules)
        }
        _uiState.update { it.copy(metaModuleWarning = warning) }
    }

    fun fetchModuleList() {
        refreshJob?.cancel()
        updateJob?.cancel()
        val generation = ++fetchGeneration
        refreshJob = viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            val result = withContext(Dispatchers.IO) {
                moduleRepo.listModules()
            }

            result.fold(
                onSuccess = { newList ->
                    ModuleCountsRefresher.requestRefresh()
                    _uiState.update {
                        it.copy(
                            modules = newList,
                            isNeedRefresh = false,
                            isRefreshing = false,
                            hasLoaded = true,
                            error = null
                        ).withVisibleModules()
                    }
                    updateJob = viewModelScope.launch {
                        refreshUpdateResults(newList, generation)
                    }
                    checkMetaModuleWarning(newList)
                },
                onFailure = { e ->
                    // Keep hasLoaded false so the next page visit/resume retries, and keep
                    // isNeedRefresh pending so a requested refresh is not lost.
                    Log.e(TAG, "fetchModuleList failed", e)
                    _uiState.update { it.copy(isRefreshing = false, hasLoaded = false, error = e) }
                }
            )
        }
    }

    private suspend fun refreshUpdateResults(modules: List<ModuleInfo>, generation: Int) {
        val results = try {
            withContext(Dispatchers.IO) {
                coroutineScope {
                    val now = SystemClock.elapsedRealtime()
                    modules.map { module ->
                        async {
                            val cached = updateCache[module.id]
                            if (cached != null &&
                                cached.versionCode == module.versionCode &&
                                now - cached.checkedAt < UPDATE_CACHE_TTL_MS
                            ) {
                                return@async module.id to cached.result
                            }
                            val result = moduleRepo.checkModuleUpdate(module)
                            updateCache[module.id] = CachedUpdate(
                                checkedAt = now,
                                versionCode = module.versionCode,
                                result = result
                            )
                            module.id to result
                        }
                    }.awaitAll().toMap()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "update check failed", e)
            return
        }
        if (generation != fetchGeneration) return
        _uiState.update { it.copy(updateResults = results) }
    }
}
