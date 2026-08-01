package me.bmax.apatch.ui.page.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.compose.viewModel
import me.bmax.apatch.ui.LocalUiMode
import me.bmax.apatch.ui.UiMode
import me.bmax.apatch.ui.page.material.home.HomeScreenMaterial
import me.bmax.apatch.ui.page.miuix.home.HomeScreenMiuix

@Composable
fun HomeScreen(
    modifier: Modifier,
    bottomPadding: Dp,
    isCurrentPage: Boolean = true,
    viewModel: HomeViewModel = viewModel(),
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> HomeScreenMiuix(modifier, bottomPadding, isCurrentPage, viewModel)
        UiMode.Material -> HomeScreenMaterial(modifier, bottomPadding, isCurrentPage, viewModel)
    }
}
