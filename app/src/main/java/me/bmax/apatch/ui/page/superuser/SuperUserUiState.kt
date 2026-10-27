package me.bmax.apatch.ui.page.superuser

import androidx.compose.runtime.Immutable
import me.bmax.apatch.data.AppInfo

enum class SortBy { NAME, PACKAGE_NAME, INSTALL_TIME }

@Immutable
data class SuperUserUiState(
    val apps: List<AppInfo> = emptyList(),
    /** Search- and filter-applied view of [apps]; the only list the UI reads. */
    val filteredApps: List<AppInfo> = emptyList(),
    val isRefreshing: Boolean = false,
    /** True once a load has completed successfully; an empty [apps] is only "no apps" then. */
    val hasLoaded: Boolean = false,
    val error: Throwable? = null,
    val search: String = "",
    val showSystemApps: Boolean = false,
    val sortBy: SortBy = SortBy.NAME
)
