package me.bmax.apatch.ui.page.terminal

import androidx.compose.runtime.Immutable

@Immutable
data class TerminalUiState(
    val logs: String = "",
    val isRunning: Boolean = false,
    val isFinished: Boolean = false,
    val isSuccess: Boolean = false
)
