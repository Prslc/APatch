package me.bmax.apatch.ui.page.settings

data class SettingsUiState(
    val isKpatchReady: Boolean = false,
    val isApatchReady: Boolean = false,
    val isGlobalNamespaceEnabled: Boolean = false,
    val enableWebDebugging: Boolean = false,
    val checkUpdate: Boolean = true,
    val themeMode: Int = 0,
    val keyColor: Int = 0,
    val cacheSize: Long = 0L,
    val currentLanguageIndex: Int = 0,
    val showResetSuPathDialog: Boolean = false,
    val showLogDialog: Boolean = false,
    val showClearDialog: Boolean = false
)