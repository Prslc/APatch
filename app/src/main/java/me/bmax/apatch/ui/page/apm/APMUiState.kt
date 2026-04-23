package me.bmax.apatch.ui.page.apm

enum class ShortcutType { Action, WebUI }

data class APMUiState(
    val modules: List<ModuleInfo> = emptyList(),
    val isRefreshing: Boolean = false,
    val search: String = "",
    val isNeedRefresh: Boolean = false,

    val shortcutState: ShortcutState? = null,
    val metaModuleWarning: String? = null
)

data class ShortcutState(
    val moduleId: String,
    val name: String,
    val iconUri: String?,
    val defaultIconUri: String?,
    val type: ShortcutType,
    val hasAction: Boolean,
    val hasWebUi: Boolean,
    val isExisting: Boolean
)