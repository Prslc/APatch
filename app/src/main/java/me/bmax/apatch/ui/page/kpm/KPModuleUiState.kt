package me.bmax.apatch.ui.page.kpm

import androidx.compose.runtime.Immutable

@Immutable
data class KPModuleUiState(
    val modules: List<KPModel.KPMInfo> = emptyList(),
    val isRefreshing: Boolean = false,
    val isNeedRefresh: Boolean = false,
    val search: String = "",
    val controlTarget: KPModel.KPMInfo? = null,
    val showControlDialog: Boolean = false,
    /** Repository releases newer than the persisted KPM file, by module id. */
    val updates: Map<String, KPModel.KpmUpdateInfo> = emptyMap()
)
