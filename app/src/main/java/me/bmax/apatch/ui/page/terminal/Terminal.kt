package me.bmax.apatch.ui.page.terminal

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import me.bmax.apatch.ui.LocalUiMode
import me.bmax.apatch.ui.UiMode
import me.bmax.apatch.ui.navigation.MODULE_TYPE
import me.bmax.apatch.ui.navigation.TERMINAL_TASK_TYPE

@Composable
fun TerminalScreen(
    taskType: TERMINAL_TASK_TYPE,
    targetId: String,
    moduleType: MODULE_TYPE,
    onBack: () -> Unit,
    viewModel: TerminalViewModel = viewModel()
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> TerminalScreenMiuix(taskType, targetId, moduleType, onBack, viewModel)
        UiMode.Material -> TerminalScreenMaterial(taskType, targetId, moduleType, onBack, viewModel)
    }
}
