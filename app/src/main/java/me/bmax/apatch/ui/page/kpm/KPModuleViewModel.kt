package me.bmax.apatch.ui.page.kpm

import android.net.Uri
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
import me.bmax.apatch.Natives
import me.bmax.apatch.data.repository.KPModuleRepository
import me.bmax.apatch.data.repository.KPModuleRepositoryImpl
import me.bmax.apatch.ui.page.home.ModuleCountsRefresher
import me.bmax.apatch.util.HanziToPinyin
import me.bmax.apatch.util.ModuleRepo
import java.text.Collator
import java.util.Locale

private const val UPDATE_CACHE_TTL_MS = 5 * 60 * 1000L

class KPModuleViewModel(
    private val moduleRepo: KPModuleRepository = KPModuleRepositoryImpl
) : ViewModel() {
    private val _uiState = MutableStateFlow(KPModuleUiState())
    val uiState = _uiState.asStateFlow()

    private data class CachedUpdate(
        val checkedAt: Long,
        val storedVersion: String,
        val update: KPModel.KpmUpdateInfo?
    )

    private val updateCache = mutableMapOf<String, CachedUpdate>()

    private var fetchJob: Job? = null
    private var updateJob: Job? = null
    private var searchJob: Job? = null

    private val staticCollator = Collator.getInstance(Locale.getDefault())
    private val nameComparator = compareBy(staticCollator, KPModel.KPMInfo::name)

    /** Recomputes the single visible list held in [KPModuleUiState]. */
    private fun KPModuleUiState.withVisibleModules(): KPModuleUiState {
        val trimmed = search.trim()
        val visible = modules.filter {
            trimmed.isEmpty() ||
                    it.name.contains(trimmed, true) ||
                    it.author.contains(trimmed, true) ||
                    it.description.contains(trimmed, true) ||
                    it.pinyinName.contains(trimmed, true)
        }
        return copy(filteredModules = visible)
    }

    /**
     * Single entry point for the screens' visibility/resume triggers. Refreshes the
     * repository update info when the list is already loaded, otherwise reloads it.
     */
    fun loadIfNeeded() {
        val state = _uiState.value
        if (state.isRefreshing) return
        if (!state.hasLoaded || state.error != null) {
            fetchModuleList()
        } else {
            refreshOnlineUpdates()
        }
    }

    fun fetchModuleList() {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            try {
                withContext(Dispatchers.IO) { refreshModuleList() }
                _uiState.update { it.copy(isRefreshing = false, hasLoaded = true, error = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("KPM", "fetchModuleList failed", e)
                _uiState.update { it.copy(isRefreshing = false, hasLoaded = false, error = e) }
            }
        }
    }

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
        _uiState.update {
            it.copy(modules = modules, hasLoaded = true, error = null).withVisibleModules()
        }
        refreshOnlineUpdates()
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
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    suspend fun installModule(uri: Uri): Int {
        _uiState.update { it.copy(isRefreshing = true) }
        return try {
            withContext(Dispatchers.IO) {
                val rc = moduleRepo.installModule(uri)
                if (rc == 0) refreshModuleList()
                rc
            }
        } catch (e: Exception) {
            Log.e("KPM", "Install failed", e)
            -1
        } finally {
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    suspend fun uninstallModule(module: KPModel.KPMInfo): Boolean {
        if (module.loadSource == "embedded") return false
        _uiState.update { it.copy(isRefreshing = true) }
        return try {
            withContext(Dispatchers.IO) {
                if (module.loadSource.isNotBlank()) {
                    moduleRepo.unloadModule(module.name)
                }
                val removed = if (module.installed) {
                    moduleRepo.removeInstalledModule(module.moduleId.ifBlank { module.name })
                } else {
                    true
                }
                refreshModuleList()
                removed
            }
        } catch (e: Exception) {
            Log.e("KPM", "Uninstall failed", e)
            false
        } finally {
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    suspend fun setModuleDisabled(module: KPModel.KPMInfo, enabled: Boolean) {
        _uiState.update { it.copy(isRefreshing = true) }
        try {
            withContext(Dispatchers.IO) {
                val id = module.moduleId.ifBlank { module.name }
                if (moduleRepo.setModuleDisabled(id, !enabled)) {
                    refreshModuleList()
                }
            }
        } catch (e: Exception) {
            Log.e("KPM", "Toggle failed", e)
        } finally {
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun refreshOnlineUpdates() {
        // Serialize update checks: the cache below is a plain map and the previous
        // check would otherwise race with this one.
        updateJob?.cancel()
        updateJob = viewModelScope.launch {
            val installed = _uiState.value.modules.filter {
                it.installed && it.loadSource != "embedded" && it.moduleId.isNotBlank()
            }
            if (installed.isEmpty()) {
                _uiState.update { it.copy(updates = emptyMap()) }
                return@launch
            }
            _uiState.update { it.copy(updates = checkOnlineUpdates(installed)) }
        }
    }

    private suspend fun checkOnlineUpdates(
        installed: List<KPModel.KPMInfo>
    ): Map<String, KPModel.KpmUpdateInfo> = coroutineScope {
        val now = SystemClock.elapsedRealtime()
        installed
            .map { module ->
                async {
                    val id = module.moduleId
                    val stored = module.storedVersion.ifBlank { module.version }
                    val cached = updateCache[id]
                    if (cached != null && cached.storedVersion == stored &&
                        now - cached.checkedAt < UPDATE_CACHE_TTL_MS
                    ) {
                        return@async cached.update?.let { id to it }
                    }
                    val doc = runCatching { ModuleRepo.moduleDoc(id, kpm = true) }.getOrNull()
                    val update = doc?.takeIf {
                        it.version.isNotBlank() && it.downloadUrl.isNotBlank() && it.version != stored
                    }?.let { KPModel.KpmUpdateInfo(it.version, it.downloadUrl) }
                    updateCache[id] = CachedUpdate(now, stored, update)
                    update?.let { id to it }
                }
            }
            .awaitAll()
            .filterNotNull()
            .toMap()
    }

    fun openControlDialog(module: KPModel.KPMInfo) {
        _uiState.update { it.copy(controlTarget = module, showControlDialog = true) }
    }

    fun closeControlDialog() {
        _uiState.update { it.copy(showControlDialog = false) }
    }

    fun updateSearch(query: String) {
        _uiState.update { it.copy(search = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(200)
            _uiState.update { it.withVisibleModules() }
        }
    }

    suspend fun controlModule(name: String, param: String): Natives.KPMCtlRes = withContext(Dispatchers.IO) {
        moduleRepo.controlModule(name, param)
    }
}
