package me.bmax.apatch.ui.page.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.bmax.apatch.ui.LocalUiMode
import me.bmax.apatch.ui.LocalModuleCounts
import me.bmax.apatch.ui.UiMode

@Composable
fun HomeScreen(
    modifier: Modifier,
    bottomPadding: Dp,
    isCurrentPage: Boolean = true,
    viewModel: HomeViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val counts = LocalModuleCounts.current
    LaunchedEffect(uiState.apmCount, uiState.kpmCount) {
        counts.apmCount = uiState.apmCount
        counts.kpmCount = uiState.kpmCount
    }

    when (LocalUiMode.current) {
        UiMode.Miuix -> HomeScreenMiuix(modifier, bottomPadding, isCurrentPage, viewModel)
        UiMode.Material -> HomeScreenMaterial(modifier, bottomPadding, isCurrentPage, viewModel)
    }
}
