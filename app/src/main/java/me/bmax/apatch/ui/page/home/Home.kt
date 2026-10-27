package me.bmax.apatch.ui.page.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import me.bmax.apatch.ui.LocalUiMode
import me.bmax.apatch.ui.LocalModuleCounts
import me.bmax.apatch.ui.UiMode

@Composable
fun HomeScreen(
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
    isCurrentPage: Boolean = true,
    viewModel: HomeViewModel = viewModel(),
) {
    val counts = LocalModuleCounts.current

    LaunchedEffect(viewModel) {
        viewModel.uiState
            .map { Triple(it.apmCount, it.kpmCount, it.suCount) }
            .distinctUntilChanged()
            .collect { (apm, kpm, su) ->
                counts.apmCount = apm
                counts.kpmCount = kpm
                counts.suCount = su
            }
    }

    when (LocalUiMode.current) {
        UiMode.Miuix -> HomeScreenMiuix(bottomPadding, modifier, isCurrentPage, viewModel)
        UiMode.Material -> HomeScreenMaterial(bottomPadding, modifier, isCurrentPage, viewModel)
    }
}
