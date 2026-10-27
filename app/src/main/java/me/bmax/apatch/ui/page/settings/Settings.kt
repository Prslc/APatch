package me.bmax.apatch.ui.page.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.compose.viewModel
import me.bmax.apatch.ui.LocalUiMode
import me.bmax.apatch.ui.UiMode

@Composable
fun SettingScreen(
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
    isCurrentPage: Boolean = true,
    viewModel: SettingsViewModel = viewModel()
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> SettingScreenMiuix(bottomPadding, modifier, isCurrentPage, viewModel)
        UiMode.Material -> SettingScreenMaterial(bottomPadding, modifier, isCurrentPage, viewModel)
    }
}
