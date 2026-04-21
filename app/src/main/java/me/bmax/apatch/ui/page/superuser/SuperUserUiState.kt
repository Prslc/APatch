package me.bmax.apatch.ui.page.superuser

data class SuperUserUiState(
    val isRefreshing: Boolean = false,
    val search: String = "",
    val showSystemApps: Boolean = false
)