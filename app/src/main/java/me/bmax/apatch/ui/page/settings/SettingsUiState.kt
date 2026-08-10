package me.bmax.apatch.ui.page.settings

import androidx.compose.runtime.Immutable

enum class SettingDialogType {
    NONE,
    RESET_SU_PATH,
    SEND_LOG,
    CLEAR_CACHE,
}

@Immutable
data class SettingsUiState(
    val isKpatchReady: Boolean = false,
    val isApatchReady: Boolean = false,
    val isJailbreak: Boolean = false,
    val isGlobalNamespaceEnabled: Boolean = false,
    val sucompatEnabled: Boolean = false,
    val enableWebDebugging: Boolean = false,
    val checkUpdate: Boolean = true,
    val cacheSize: Long = 0L,
    val currentLanguageIndex: Int = 0,
    val currentDialog: SettingDialogType = SettingDialogType.NONE
)
