package me.bmax.apatch.ui.page.home

import androidx.compose.runtime.Immutable
import me.bmax.apatch.APApplication
import me.bmax.apatch.util.LatestVersionInfo

@Immutable
data class HomeUiState(
    val deviceInfo: String = "",
    val kernelVersion: String = "",
    val androidVersion: String = "",
    val fingerprint: String = "",
    val selinux: String = "",
    val suPath: String = "",

    val apmCount: Int = 0,
    val kpmCount: Int = 0,

    val isCheckingUpdate: Boolean = false,
    val newVersionInfo: LatestVersionInfo? = null,

    val kpState: APApplication.State = APApplication.State.UNKNOWN_STATE,
    val apState: APApplication.State = APApplication.State.ANDROIDPATCH_INSTALLED,
)