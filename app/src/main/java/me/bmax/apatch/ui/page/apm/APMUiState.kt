package me.bmax.apatch.ui.page.apm

enum class ShortcutType { Action, WebUI }

data class ModuleInfo(
    val id: String,
    val name: String,
    val pinyinName: String,
    val author: String,
    val version: String,
    val versionCode: Int,
    val description: String,
    val enabled: Boolean,
    val update: Boolean,
    val remove: Boolean,
    val updateJson: String,
    val hasWebUi: Boolean,
    val hasActionScript: Boolean,
    val metamodule: Boolean,
    val actionIconPath: String?,
    val webUiIconPath: String?,
)

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