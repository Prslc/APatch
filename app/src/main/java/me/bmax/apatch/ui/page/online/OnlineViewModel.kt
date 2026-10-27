package me.bmax.apatch.ui.page.online

import android.net.Uri
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
import me.bmax.apatch.data.repository.KPModuleRepository
import me.bmax.apatch.data.repository.KPModuleRepositoryImpl
import me.bmax.apatch.util.InstalledModule
import me.bmax.apatch.util.ModuleRepo
import me.bmax.apatch.util.ModuleStore
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "OnlineViewModel"
private const val SEARCH_DEBOUNCE_MS = 450L

enum class OnlineCategory(val kpm: Boolean) {
    SYSTEM(false), KPM(true);

    companion object {
        fun of(kpmOnly: Boolean): OnlineCategory = if (kpmOnly) KPM else SYSTEM
    }
}

data class OnlineModule(
    val repo: ModuleRepo.RepoModule,
    val installedModule: InstalledModule? = null,
) {
    val id: String get() = repo.id
    val kpm: Boolean get() = repo.kpm
    val stars: Int get() = repo.stars
    val installed: Boolean get() = installedModule != null
    val installedVersion: String get() = installedModule?.version.orEmpty()
    val author: String get() = repo.author
    val summary: String get() = repo.description
}

data class OnlineUiState(
    val category: OnlineCategory = OnlineCategory.SYSTEM,
    val search: String = "",
    val modules: List<OnlineModule> = emptyList(),
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val errorMessage: String = "",
    val hasLoadedOnce: Boolean = false,
)

enum class OnlineListState { LOADING, ERROR, EMPTY, CONTENT }

val OnlineUiState.listState: OnlineListState
    get() = when {
        modules.isNotEmpty() -> OnlineListState.CONTENT
        !hasLoadedOnce || isRefreshing -> OnlineListState.LOADING
        errorMessage.isNotBlank() -> OnlineListState.ERROR
        else -> OnlineListState.EMPTY
    }

class OnlineViewModel(
    private val kpmRepo: KPModuleRepository = KPModuleRepositoryImpl
) : ViewModel() {
    private val _uiState = MutableStateFlow(OnlineUiState())
    val uiState = _uiState.asStateFlow()

    private var entered = false
    private var nextPage = 1

    @Volatile
    private var generation = 0
    private var pageJob: Job? = null
    private var searchJob: Job? = null

    private val docCache = ConcurrentHashMap<String, ModuleRepo.RepoModuleDoc>()

    /** Installed-module snapshot for the current catalogue generation. */
    @Volatile
    private var installedCache: Pair<Map<String, InstalledModule>, Map<String, InstalledModule>>? = null

    fun onEnter(kpmOnly: Boolean) {
        if (!entered) {
            entered = true
            _uiState.update { it.copy(category = OnlineCategory.of(kpmOnly)) }
        }
        refresh()
    }

    fun onSearchChange(text: String) {
        if (text == _uiState.value.search) return
        _uiState.update { it.copy(search = text) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            refresh()
        }
    }

    fun refresh() {
        pageJob?.cancel()
        generation++
        installedCache = null
        val gen = generation
        _uiState.update { it.copy(isRefreshing = true, errorMessage = "") }
        pageJob = viewModelScope.launch(Dispatchers.IO) {
            val fetched = runCatching {
                ModuleRepo.listModules(
                    page = 1,
                    perPage = ModuleRepo.PAGE_SIZE,
                    kpm = _uiState.value.category.kpm,
                    search = _uiState.value.search
                )
            }
            if (gen != generation) return@launch
            val listed = fetched.getOrElse { error ->
                Log.w(TAG, "load online modules failed", error)
                _uiState.update {
                    it.copy(
                        modules = emptyList(),
                        hasMore = false,
                        errorMessage = error.message ?: error.javaClass.simpleName,
                        isRefreshing = false,
                        hasLoadedOnce = true
                    )
                }
                return@launch
            }
            nextPage = 2
            _uiState.update {
                it.copy(
                    modules = listed.map { module -> OnlineModule(repo = module) },
                    hasMore = listed.size >= ModuleRepo.PAGE_SIZE,
                    isRefreshing = false,
                    hasLoadedOnce = true
                )
            }
            applyInstalled(gen)
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isRefreshing || state.isLoadingMore || !state.hasMore || pageJob?.isActive == true) return
        val gen = generation
        val target = nextPage
        _uiState.update { it.copy(isLoadingMore = true) }
        pageJob = viewModelScope.launch(Dispatchers.IO) {
            val fetched = runCatching {
                ModuleRepo.listModules(
                    page = target,
                    perPage = ModuleRepo.PAGE_SIZE,
                    kpm = _uiState.value.category.kpm,
                    search = _uiState.value.search
                )
            }
            if (gen != generation) return@launch
            val listed = fetched.getOrElse { error ->
                Log.w(TAG, "load page $target failed", error)
                _uiState.update {
                    it.copy(
                        errorMessage = error.message ?: error.javaClass.simpleName,
                        isLoadingMore = false
                    )
                }
                return@launch
            }
            nextPage = target + 1
            val installed = installedCache
            _uiState.update {
                it.copy(
                    modules = it.modules + listed.map { module ->
                        val row = OnlineModule(repo = module)
                        if (installed != null) row.decoratedBy(installed) else row
                    },
                    hasMore = listed.size >= ModuleRepo.PAGE_SIZE,
                    isLoadingMore = false
                )
            }
            if (installed == null) applyInstalled(gen)
        }
    }

    /** Cached document, shown instantly when the detail dialog opens again. */
    fun cachedDoc(module: OnlineModule): ModuleRepo.RepoModuleDoc? =
        docCache[docKey(module.kpm, module.id)]

    /** Reads a module document for the detail view, falling back to the cache. */
    suspend fun moduleDocResult(module: OnlineModule): ModuleRepo.ModuleDocResult {
        val key = docKey(module.kpm, module.id)
        val result = runCatching { ModuleRepo.moduleDocResult(module.id, module.kpm) }
            .getOrDefault(ModuleRepo.ModuleDocResult(null))
        if (result.doc != null) {
            docCache[key] = result.doc
            return result
        }
        val cached = docCache[key]
        return if (cached != null) ModuleRepo.ModuleDocResult(cached) else result
    }

    suspend fun installKpm(uri: Uri): Int = kpmRepo.installModule(uri)

    private fun docKey(kpm: Boolean, id: String): String = "$kpm:$id"

    /**
     * Local installed detection is best effort: it can wait on root, so the
     * catalogue is shown first and this only decorates the rows it finds.
     */
    private fun applyInstalled(gen: Int) {
        val installed = installedModules()
        if (gen != generation) return
        installedCache = installed
        _uiState.update { state ->
            state.copy(modules = state.modules.map { row -> row.decoratedBy(installed) })
        }
    }

    private fun OnlineModule.decoratedBy(
        installed: Pair<Map<String, InstalledModule>, Map<String, InstalledModule>>
    ): OnlineModule = copy(
        installedModule = if (kpm) installed.second[id] else installed.first[id]
    )

    private fun installedModules(): Pair<Map<String, InstalledModule>, Map<String, InstalledModule>> =
        runCatching { ModuleStore.listSystemModules() to ModuleStore.listKpmModules() }
            .getOrDefault(emptyMap<String, InstalledModule>() to emptyMap())
}
