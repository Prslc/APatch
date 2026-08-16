package me.bmax.apatch.ui.page.apm

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
import kotlin.time.Duration.Companion.milliseconds

class APModuleViewModel(
    private val moduleRepo: ApModuleRepository = ApModuleRepositoryImpl,
    private val settingsRepo: SettingsRepository = SettingsRepositoryImpl
) : ViewModel() {
    companion object {
        private const val TAG = "ModuleViewModel"
    }

    private val _uiState = MutableStateFlow(APMUiState().loadSortMode())
    val uiState = _uiState.asStateFlow()

    private var refreshJob: Job? = null

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

    @OptIn(FlowPreview::class)
    val filteredModules = combine(
        uiState.map { it.search }.distinctUntilChanged().debounce(200.milliseconds),
        uiState.map { it.modules }.distinctUntilChanged(),
        uiState.map { it.sortEnabledFirst }.distinctUntilChanged(),
        uiState.map { it.sortActionFirst }.distinctUntilChanged(),
        uiState.map { it.sortWebFirst }.distinctUntilChanged()
    ) { query, modules, sortEnabledFirst, sortActionFirst, sortWebFirst ->
        val trimmedQuery = query.trim()
        modules.filter {
            trimmedQuery.isEmpty() ||
                    it.id.contains(trimmedQuery, true) ||
                    it.name.contains(trimmedQuery, true) ||
                    it.pinyinName.contains(trimmedQuery, true)
        }.sortedWith(moduleComparator(sortEnabledFirst, sortActionFirst, sortWebFirst))
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
    }

    fun markNeedRefresh() {
        _uiState.update { it.copy(isNeedRefresh = true) }
    }

    fun resetSort() {
        _uiState.update {
            it.copy(
                sortEnabledFirst = false,
                sortActionFirst = false,
                sortWebFirst = false
            )
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
            )
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
            )
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
            )
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
        refreshJob = viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            val result = withContext(Dispatchers.IO) {
                moduleRepo.listModules()
            }

            result.fold(
                onSuccess = { newList ->
                    ModuleCountsRefresher.requestRefresh()
                    val updateResults = try {
                        withContext(Dispatchers.IO) {
                            newList.map { module ->
                                async { module.id to moduleRepo.checkModuleUpdate(module) }
                            }.awaitAll().toMap()
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "update check failed", e)
                        _uiState.value.updateResults
                    }
                    _uiState.update {
                        it.copy(
                            modules = newList,
                            isNeedRefresh = false,
                            isRefreshing = false,
                            updateResults = updateResults
                        )
                    }
                    checkMetaModuleWarning(newList)
                },
                onFailure = { e ->
                    Log.e(TAG, "fetchModuleList failed", e)
                    _uiState.update { it.copy(isNeedRefresh = false, isRefreshing = false) }
                }
            )
        }
    }
}
