package me.bmax.apatch.ui.page.kpm

import androidx.compose.runtime.Immutable

@Immutable
data class KPModuleUiState(
    val modules: List<KPModel.KPMInfo> = emptyList(),
    /** Search-applied view of [modules]; the only list the UI reads. */
    val filteredModules: List<KPModel.KPMInfo> = emptyList(),
    val isRefreshing: Boolean = false,
    /** True once a load has completed successfully; an empty [modules] is only "no modules" then. */
    val hasLoaded: Boolean = false,
    val error: Throwable? = null,
    val search: String = "",
    val controlTarget: KPModel.KPMInfo? = null,
    val showControlDialog: Boolean = false,
    /** Repository releases newer than the persisted KPM file, by module id. */
    val updates: Map<String, KPModel.KpmUpdateInfo> = emptyMap()
)
