package me.bmax.apatch.ui.page.settings

import androidx.compose.runtime.Immutable

enum class SettingDialogType {
    NONE,
    RESET_SU_PATH,
    SEND_LOG,
    CLEAR_CACHE,
    PAGE_SCALE
}

@Immutable
data class SettingsUiState(
    val isKpatchReady: Boolean = false,
    val isApatchReady: Boolean = false,
    val isGlobalNamespaceEnabled: Boolean = false,
    val enableWebDebugging: Boolean = false,
    val checkUpdate: Boolean = true,
    val blurEnabled: Boolean = true,
    val themeMode: Int = 0,
    val keyColor: Int = 0,
    val uiMode: String = "miuix",
    val cacheSize: Long = 0L,
    val currentLanguageIndex: Int = 0,
    val currentDialog: SettingDialogType = SettingDialogType.NONE
)
