package me.bmax.apatch.ui.page.apm

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
import kotlinx.coroutines.withContext
import me.bmax.apatch.data.repository.ApModuleRepository
import me.bmax.apatch.data.repository.ApModuleRepositoryImpl
import java.text.Collator
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

class APModuleViewModel(
    private val moduleRepo: ApModuleRepository = ApModuleRepositoryImpl
) : ViewModel() {
    companion object {
        private const val TAG = "ModuleViewModel"
    }

    private val _uiState = MutableStateFlow(APMUiState())
    val uiState = _uiState.asStateFlow()

    @OptIn(FlowPreview::class)
    val filteredModules = uiState
        .map { it.search }.distinctUntilChanged().debounce(200.milliseconds)
        .combine(uiState.map { it.modules }.distinctUntilChanged()) { query, modules ->
            val collator = Collator.getInstance(Locale.getDefault())
            val comparator = compareByDescending<ModuleInfo> { it.metamodule && it.enabled }
                .thenBy(collator) { it.id }

            modules.filter {
                it.id.contains(query, true) ||
                        it.name.contains(query, true) ||
                        it.pinyinName.contains(query, true)
            }.sortedWith(comparator)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchChange(newSearch: String) {
        _uiState.update { it.copy(search = newSearch) }
    }

    fun markNeedRefresh() {
        _uiState.update { it.copy(isNeedRefresh = true) }
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
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            val result = withContext(Dispatchers.IO) {
                moduleRepo.listModules()
            }

            result.fold(
                onSuccess = { newList ->
                    _uiState.update {
                        it.copy(
                            modules = newList, isNeedRefresh = false, isRefreshing = false
                        )
                    }
                    checkMetaModuleWarning(newList)
                    launch {
                        val updateResults = withContext(Dispatchers.IO) {
                            newList.map { module ->
                                async { module.id to moduleRepo.checkModuleUpdate(module) }
                            }.awaitAll().toMap()
                        }
                        _uiState.update { it.copy(updateResults = updateResults) }
                    }
                },
                onFailure = { e ->
                    Log.e(TAG, "fetchModuleList failed", e)
                    _uiState.update { it.copy(isNeedRefresh = false, isRefreshing = false) }
                }
            )
        }
    }
}
