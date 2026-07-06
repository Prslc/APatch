package me.bmax.apatch.ui.page.superuser

import androidx.compose.runtime.Immutable

enum class SortBy { NAME, PACKAGE_NAME, INSTALL_TIME }

@Immutable
data class SuperUserUiState(
    val isRefreshing: Boolean = false,
    val search: String = "",
    val showSystemApps: Boolean = false,
    val sortBy: SortBy = SortBy.NAME
)