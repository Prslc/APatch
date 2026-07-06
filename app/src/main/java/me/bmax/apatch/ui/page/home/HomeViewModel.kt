package me.bmax.apatch.ui.page.home

import android.os.Build
import android.system.Os
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
                fingerprint = Build.FINGERPRINT,
                suPath = Natives.suPath()
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

        refreshCounts()
        refreshSystemInfoAsync()
    }

    fun refreshCounts() = viewModelScope.launch(Dispatchers.IO) {
        val apm = apRepo.getModuleCount().coerceAtLeast(0)
        val kpm = kpRepo.getModuleCount().coerceAtLeast(0)
        _uiState.update { it.copy(apmCount = apm, kpmCount = kpm) }
    }

    private fun refreshSystemInfoAsync() = viewModelScope.launch(Dispatchers.IO) {
        val seStatus = getSELinuxStatus()
        _uiState.update { it.copy(selinux = seStatus) }
    }

    fun checkUpdate() = viewModelScope.launch(Dispatchers.IO) {
        _uiState.update { it.copy(isCheckingUpdate = true) }
        val info = checkNewVersion()
        _uiState.update { it.copy(newVersionInfo = info, isCheckingUpdate = false) }
    }
}
