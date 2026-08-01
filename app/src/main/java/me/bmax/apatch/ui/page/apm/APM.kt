package me.bmax.apatch.ui.page.apm

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.compose.viewModel
import me.bmax.apatch.ui.LocalUiMode
import me.bmax.apatch.ui.UiMode
import me.bmax.apatch.ui.page.material.apm.APModuleScreenMaterial
import me.bmax.apatch.ui.page.miuix.apm.APModuleScreenMiuix

@Composable
fun APModuleScreen(
    modifier: Modifier,
    bottomPadding: Dp,
    isCurrentPage: Boolean = true,
    viewModel: APModuleViewModel = viewModel()
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> APModuleScreenMiuix(modifier, bottomPadding, isCurrentPage, viewModel)
        UiMode.Material -> APModuleScreenMaterial(modifier, bottomPadding, isCurrentPage, viewModel)
    }
}
