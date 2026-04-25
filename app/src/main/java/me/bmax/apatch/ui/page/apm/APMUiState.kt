package me.bmax.apatch.ui.page.apm

import androidx.compose.runtime.Immutable

enum class ShortcutType { Action, WebUI }

@Immutable
data class APMUiState(
    val modules: List<ModuleInfo> = emptyList(),
    val isRefreshing: Boolean = false,
    val search: String = "",
    val isNeedRefresh: Boolean = false,

    val shortcutState: ShortcutState? = null,
    val metaModuleWarning: String? = null,
    val updateResults: Map<String, Triple<String, String, String>> = emptyMap()
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