package me.bmax.apatch.ui.page.superuser

import android.content.pm.ApplicationInfo
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
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.Natives
import me.bmax.apatch.apApp
import me.bmax.apatch.data.AppInfo
import me.bmax.apatch.data.AppRepository
import me.bmax.apatch.util.PkgConfig
import java.text.Collator
import java.util.Locale

class SuperUserViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SuperUserUiState())
    val uiState = _uiState.asStateFlow()

    private val staticCollator = Collator.getInstance(Locale.getDefault())

    private val appComparator = compareBy<AppInfo> {
        when {
            it.config.allow != 0 -> 0
            it.config.exclude == 1 -> 1
            else -> 2
        }
    }.thenBy(staticCollator) { it.label }

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    val filteredApps = combine(
        AppRepository.apps,
        uiState.map { it.search }.distinctUntilChanged().debounce(200),
        uiState.map { it.showSystemApps }.distinctUntilChanged()
    ) { apps, query, showSystem ->
        withContext(Dispatchers.Default) {
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
                .sortedWith(appComparator)
                .toList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun fetchAppList() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isRefreshing = true) }
                AppRepository.fetchAppList()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                kotlinx.coroutines.delay(100)
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun toggleSu(app: AppInfo, granted: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val newConfig = app.config.copy().apply {
                if (granted) {
                    allow = 1
                    exclude = 0
                    Natives.grantSu(app.uid, 0, profile.scontext)
                    Natives.setUidExclude(app.uid, 0)
                } else {
                    allow = 0
                    Natives.revokeSu(app.uid)
                }
            }

            PkgConfig.changeConfig(newConfig)
            AppRepository.updateLocalConfig(app.uid, newConfig)
        }
    }

    fun toggleExclude(app: AppInfo, excluded: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val newConfig = app.config.copy().apply {
                if (excluded) {
                    allow = 0
                    exclude = 1
                    profile = profile.copy(
                        uid = app.uid,
                        scontext = APApplication.MAGISK_SCONTEXT
                    )
                    Natives.revokeSu(app.uid)
                    Natives.setUidExclude(app.uid, 1)
                } else {
                    exclude = 0
                    Natives.setUidExclude(app.uid, 0)
                }
                profile = profile.copy(uid = app.uid)
            }

            PkgConfig.changeConfig(newConfig)
            AppRepository.updateLocalConfig(app.uid, newConfig)
        }
    }

    override fun onCleared() {
        super.onCleared()
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
}