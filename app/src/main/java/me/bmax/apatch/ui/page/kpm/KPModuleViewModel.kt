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
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isRefreshing = true) }
            try {
                rawModules = listKernelModules()
                sortAndEmit(rawModules)
            } catch (e: Exception) {
                Log.e("KPM", "Fetch failed", e)
            } finally {
                _uiState.update { it.copy(isRefreshing = false, isNeedRefresh = false) }
            }
        }
    }

    fun loadModule(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isRefreshing = true) }
            val rc = loadKernelModule(uri, "")
            _uiState.update { it.copy(isRefreshing = false) }
            fetchModuleList()
        }
    }

    fun uninstallModule(moduleName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isRefreshing = true) }
            try {
                val success = unloadKernelModule(moduleName)
                if (success) {
                    fetchModuleList()
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

    fun markNeedRefresh() {
        _uiState.update { it.copy(isNeedRefresh = true) }
    }
}