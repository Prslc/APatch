package me.bmax.apatch.ui.page.apm

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.compose.viewModel
import me.bmax.apatch.ui.LocalUiMode
import me.bmax.apatch.ui.UiMode

@Composable
fun APModuleScreen(
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
    isCurrentPage: Boolean = true,
    viewModel: APModuleViewModel = viewModel()
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> APModuleScreenMiuix(bottomPadding, modifier, isCurrentPage, viewModel)
        UiMode.Material -> APModuleScreenMaterial(bottomPadding, modifier, isCurrentPage, viewModel)
    }
}
