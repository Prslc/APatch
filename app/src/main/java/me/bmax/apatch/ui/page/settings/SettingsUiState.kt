package me.bmax.apatch.ui.page.settings

import androidx.compose.runtime.Immutable

enum class SettingDialogType {
    NONE,
    RESET_SU_PATH,
    SEND_LOG,
    CLEAR_CACHE,
    SELINUX_HIDE_WARNING,
}

@Immutable
data class SettingsUiState(
    val isKpatchReady: Boolean = false,
    val isApatchReady: Boolean = false,
    val isGlobalNamespaceEnabled: Boolean = false,
    val sucompatEnabled: Boolean = false,
    val selinuxHideEnabled: Boolean = false,
    val enableWebDebugging: Boolean = false,
    val checkUpdate: Boolean = true,
    val cacheSize: Long = 0L,
    val currentLanguageIndex: Int = 0,
    val suPath: String = "",
    val kernelVersionCode: Int? = null,
    val isGki: Boolean = true,
    val currentDialog: SettingDialogType = SettingDialogType.NONE
) {
    /** SELinux hide is only offered on kernel 4.19 and above. */
    val kernelSupported: Boolean get() = (kernelVersionCode ?: 0) >= 419

    /** Only 5.10+ GKI kernels are tested; anything else gets a warning first. */
    val selinuxHideNeedsWarning: Boolean
        get() = (kernelVersionCode ?: 0) < 510 || !isGki
}
