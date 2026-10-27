package me.bmax.apatch.ui.page.kpm

import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
import me.bmax.apatch.util.ModuleRepo
import java.text.Collator
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

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
            _uiState.update { it.copy(isRefreshing = false, isNeedRefresh = false) }
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
            _uiState.update { it.copy(isRefreshing = false, isNeedRefresh = false) }
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
        viewModelScope.launch {
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
    }

    suspend fun controlModule(name: String, param: String): Natives.KPMCtlRes = withContext(Dispatchers.IO) {
        moduleRepo.controlModule(name, param)
    }
}
