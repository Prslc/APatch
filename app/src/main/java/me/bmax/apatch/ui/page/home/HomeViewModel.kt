package me.bmax.apatch.ui.page.home

import android.os.Build
import android.system.Os
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.bmax.apatch.APApplication
import me.bmax.apatch.Natives
import me.bmax.apatch.util.LatestVersionInfo
import me.bmax.apatch.util.checkNewVersion
import me.bmax.apatch.util.getKernelModuleCount
import me.bmax.apatch.util.getModuleCount
import me.bmax.apatch.util.getSELinuxStatus

class HomeViewModel : ViewModel() {
    val kpState = APApplication.Companion.kpStateLiveData.asFlow()
    val apState = APApplication.Companion.apStateLiveData.asFlow()

    private val _apmCount = MutableStateFlow(0)
    private val _kpmCount = MutableStateFlow(0)
    val apmCount = _apmCount.asStateFlow()
    val kpmCount = _kpmCount.asStateFlow()

    private val _newVersionInfo = MutableStateFlow<LatestVersionInfo?>(null)
    val newVersionInfo = _newVersionInfo.asStateFlow()

    private val _systemInfo = MutableStateFlow(
        SystemInfo(
            deviceInfo = getDeviceInfo(),
            kernelVersion = Os.uname().release,
            androidVersion = getSystemVersion(),
            fingerprint = Build.FINGERPRINT,
            selinux = "",
            suPath = Natives.suPath()
        )
    )
    val systemInfo = _systemInfo.asStateFlow()

    data class SystemInfo(
        val deviceInfo: String,
        val kernelVersion: String,
        val androidVersion: String,
        val fingerprint: String,
        val selinux: String,
        val suPath: String
    )

    init {
        refreshCounts()
        refreshSystemInfoAsync()
    }

    fun refreshCounts() = viewModelScope.launch(Dispatchers.IO) {
        _apmCount.value = getModuleCount().coerceAtLeast(0)
        _kpmCount.value = getKernelModuleCount().coerceAtLeast(0)
    }

    private fun refreshSystemInfoAsync() = viewModelScope.launch(Dispatchers.IO) {
        val seStatus = getSELinuxStatus()
        _systemInfo.value = _systemInfo.value.copy(selinux = seStatus)
    }

    fun checkUpdate() = viewModelScope.launch(Dispatchers.IO) {
        val info = checkNewVersion()
        _newVersionInfo.value = info
    }

    private fun getSystemVersion(): String {
        return "${Build.VERSION.RELEASE} ${if (Build.VERSION.PREVIEW_SDK_INT != 0) "Preview" else ""} (API ${Build.VERSION.SDK_INT})"
    }

    private fun getDeviceInfo(): String {
        var manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        if (!Build.BRAND.equals(Build.MANUFACTURER, ignoreCase = true)) {
            manufacturer += " " + Build.BRAND.replaceFirstChar { it.uppercase() }
        }
        return "$manufacturer ${Build.MODEL} "
    }
}