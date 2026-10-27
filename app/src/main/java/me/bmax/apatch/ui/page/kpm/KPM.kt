package me.bmax.apatch.ui.page.kpm

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.compose.viewModel
import me.bmax.apatch.ui.LocalUiMode
import me.bmax.apatch.ui.UiMode

@Composable
fun KPModuleScreen(
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
    isCurrentPage: Boolean = true,
    viewModel: KPModuleViewModel = viewModel()
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> KPModuleScreenMiuix(bottomPadding, modifier, isCurrentPage, viewModel)
        UiMode.Material -> KPModuleScreenMaterial(bottomPadding, modifier, isCurrentPage, viewModel)
    }
}
