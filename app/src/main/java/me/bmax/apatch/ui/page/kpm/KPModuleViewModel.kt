package me.bmax.apatch.ui.page.kpm

import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.bmax.apatch.util.listKernelModules
import java.text.Collator
import java.util.Locale

class KPModuleViewModel : ViewModel() {
    companion object {
        private const val TAG = "KPModuleViewModel"
        private var modules by mutableStateOf<List<KPModel.KPMInfo>>(emptyList())
    }

    var isRefreshing by mutableStateOf(false)
        private set

    val moduleList by derivedStateOf {
        val comparator = compareBy(Collator.getInstance(Locale.getDefault()), KPModel.KPMInfo::name)
        modules.sortedWith(comparator)
    }

    var isNeedRefresh by mutableStateOf(false)
        private set

    fun markNeedRefresh() {
        isNeedRefresh = true
    }

    fun fetchModuleList() {
        viewModelScope.launch(Dispatchers.IO) {
            isRefreshing = true
            delay(50)
            val start = SystemClock.elapsedRealtime()
            try {
                val result = listKernelModules()
                modules = result
                isNeedRefresh = false

            } catch (e: Exception) {
                Log.e(TAG, "fetch failed", e)
            } finally {
                isRefreshing = false
                Log.i(TAG, "load cost: ${SystemClock.elapsedRealtime() - start}")
            }
        }
    }
}