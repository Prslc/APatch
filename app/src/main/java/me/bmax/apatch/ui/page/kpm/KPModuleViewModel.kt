package me.bmax.apatch.ui.page.kpm

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
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
import me.bmax.apatch.Natives
import me.bmax.apatch.data.repository.KPModuleRepository
import me.bmax.apatch.data.repository.KPModuleRepositoryImpl
import me.bmax.apatch.ui.page.home.ModuleCountsRefresher
import me.bmax.apatch.util.HanziToPinyin
import java.text.Collator
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

class KPModuleViewModel(
    private val moduleRepo: KPModuleRepository = KPModuleRepositoryImpl
) : ViewModel() {
    private val _uiState = MutableStateFlow(KPModuleUiState())
    val uiState = _uiState.asStateFlow()

    @OptIn(FlowPreview::class)
    val filteredModules = combine(
        uiState.map { it.search }.distinctUntilChanged().debounce(200.milliseconds),
        uiState.map { it.modules }.distinctUntilChanged()
    ) { query, modules ->
        val trimmed = query.trim()
        modules.filter {
            trimmed.isEmpty() ||
                    it.name.contains(trimmed, true) ||
                    it.author.contains(trimmed, true) ||
                    it.description.contains(trimmed, true) ||
                    it.pinyinName.contains(trimmed, true)
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun fetchModuleList() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            try {
                withContext(Dispatchers.IO) { refreshModuleList() }
            } finally {
                _uiState.update { it.copy(isRefreshing = false, isNeedRefresh = false) }
            }
        }
    }

    private val staticCollator = Collator.getInstance(Locale.getDefault())
    private val nameComparator = compareBy(staticCollator, KPModel.KPMInfo::name)

    private suspend fun refreshModuleList() {
        val h2p = HanziToPinyin.getInstance()
        val modules = moduleRepo.listModules()
            .map { module ->
                if (module.pinyinName.isEmpty()) {
                    module.copy(pinyinName = h2p.toPinyinString(module.name) ?: "")
                } else {
                    module
                }
            }
            .sortedWith(nameComparator)
        ModuleCountsRefresher.requestRefresh()
        _uiState.update { it.copy(modules = modules) }
    }

    suspend fun loadModule(uri: Uri): Int {
        _uiState.update { it.copy(isRefreshing = true) }
        return try {
            withContext(Dispatchers.IO) {
                val rc = moduleRepo.loadModule(uri, "")
                refreshModuleList()
                rc
            }
        } catch (e: Exception) {
            Log.e("KPM", "Load failed", e)
            -1
        } finally {
            _uiState.update { it.copy(isRefreshing = false, isNeedRefresh = false) }
        }
    }

    fun uninstallModule(moduleName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            try {
                withContext(Dispatchers.IO) {
                    val success = moduleRepo.unloadModule(moduleName)
                    if (success) {
                        refreshModuleList()
                    }
                }
            } catch (e: Exception) {
                Log.e("KPM", "Unload failed", e)
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun openControlDialog(module: KPModel.KPMInfo) {
        _uiState.update { it.copy(controlTarget = module, showControlDialog = true) }
    }

    fun closeControlDialog() {
        _uiState.update { it.copy(showControlDialog = false) }
    }

    fun updateSearch(query: String) {
        _uiState.update { it.copy(search = query) }
    }

    suspend fun controlModule(name: String, param: String): Natives.KPMCtlRes = withContext(Dispatchers.IO) {
        moduleRepo.controlModule(name, param)
    }
}
