package me.bmax.apatch.ui.page.home

import android.os.Build
import android.system.Os
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.Natives
import me.bmax.apatch.data.repository.ApModuleRepository
import me.bmax.apatch.data.repository.ApModuleRepositoryImpl
import me.bmax.apatch.data.repository.KPModuleRepository
import me.bmax.apatch.data.repository.KPModuleRepositoryImpl
import me.bmax.apatch.util.checkNewVersion
import me.bmax.apatch.util.getDeviceInfo
import me.bmax.apatch.util.getSELinuxStatus
import me.bmax.apatch.util.getSystemVersion
import me.bmax.apatch.util.isJailbreakMode

class HomeViewModel(
    private val apRepo: ApModuleRepository = ApModuleRepositoryImpl,
    private val kpRepo: KPModuleRepository = KPModuleRepositoryImpl
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(
                deviceInfo = getDeviceInfo(),
                kernelVersion = Os.uname().release,
                androidVersion = getSystemVersion(),
                fingerprint = Build.FINGERPRINT
            )
        }

        viewModelScope.launch {
            combine(
                APApplication.kpStateLiveData.asFlow(),
                APApplication.apStateLiveData.asFlow()
            ) { kp, ap ->
                Pair(kp, ap)
            }.collect { (kp, ap) ->
                _uiState.update { it.copy(kpState = kp, apState = ap) }
            }
        }

        viewModelScope.launch { refreshCounts() }
        refreshSystemInfoAsync()

        viewModelScope.launch {
            ModuleCountsRefresher.events.collect {
                try {
                    refreshCounts()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "refresh counts failed", e)
                }
            }
        }
    }

    private suspend fun refreshCounts() = withContext(Dispatchers.IO) {
        val apm = apRepo.getModuleCount().coerceAtLeast(0)
        val kpm = kpRepo.getModuleCount().coerceAtLeast(0)
        val su = Natives.suUids().size
        _uiState.update { it.copy(apmCount = apm, kpmCount = kpm, suCount = su) }
    }

    private fun refreshSystemInfoAsync() = viewModelScope.launch(Dispatchers.IO) {
        val seStatus = getSELinuxStatus()
        val jailbreak = isJailbreakMode()
        val suPath = Natives.suPath()
        _uiState.update { it.copy(selinux = seStatus, isJailbreak = jailbreak, suPath = suPath) }
    }

    fun checkUpdate() = viewModelScope.launch(Dispatchers.IO) {
        _uiState.update { it.copy(isCheckingUpdate = true) }
        val info = checkNewVersion()
        _uiState.update { it.copy(newVersionInfo = info, isCheckingUpdate = false) }
    }
}
