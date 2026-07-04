package me.bmax.apatch.ui.page.kpm

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.util.listKernelModules
import me.bmax.apatch.util.loadKernelModule
import me.bmax.apatch.util.unloadKernelModule
import java.text.Collator
import java.util.Locale

class KPModuleViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(KPModuleUiState())
    val uiState = _uiState.asStateFlow()

    private var rawModules = emptyList<KPModel.KPMInfo>()

    init {
        fetchModuleList()
    }

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

    private suspend fun refreshModuleList() {
        rawModules = listKernelModules()
        sortAndEmit(rawModules)
    }

    suspend fun loadModule(uri: Uri): Int {
        _uiState.update { it.copy(isRefreshing = true) }
        return try {
            withContext(Dispatchers.IO) {
                val rc = loadKernelModule(uri, "")
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
                    val success = unloadKernelModule(moduleName)
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

    private fun sortAndEmit(list: List<KPModel.KPMInfo>) {
        val comparator = compareBy(Collator.getInstance(Locale.getDefault()), KPModel.KPMInfo::name)
        _uiState.update { it.copy(modules = list.sortedWith(comparator)) }
    }

    fun openControlDialog(module: KPModel.KPMInfo) {
        _uiState.update { it.copy(controlTarget = module, showControlDialog = true) }
    }

    fun closeControlDialog() {
        _uiState.update { it.copy(showControlDialog = false) }
    }
}