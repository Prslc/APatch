package me.bmax.apatch.ui.page.superuser

import androidx.compose.runtime.Immutable

@Immutable
data class SuperUserUiState(
    val isRefreshing: Boolean = false,
    val search: String = "",
    val showSystemApps: Boolean = false
)