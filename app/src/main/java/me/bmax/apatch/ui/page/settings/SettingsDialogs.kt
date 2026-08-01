package me.bmax.apatch.ui.page.settings

import androidx.compose.runtime.Composable
import me.bmax.apatch.ui.LocalUiMode
import me.bmax.apatch.ui.UiMode
import me.bmax.apatch.ui.page.material.settings.PageScaleDialogMaterial
import me.bmax.apatch.ui.page.miuix.settings.ClearDialogMiuix
import me.bmax.apatch.ui.page.miuix.settings.LogDialogMiuix
import me.bmax.apatch.ui.page.miuix.settings.PageScaleDialogMiuix
import me.bmax.apatch.ui.page.miuix.settings.ResetSUPathDialogMiuix

@Composable
fun SettingsDialogOverlay(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    when (uiState.currentDialog) {
        SettingDialogType.RESET_SU_PATH ->
            ResetSUPathDialogMiuix(viewModel)

        SettingDialogType.CLEAR_CACHE ->
            ClearDialogMiuix(uiState.cacheSize, viewModel)

        SettingDialogType.SEND_LOG ->
            LogDialogMiuix(viewModel)

        SettingDialogType.PAGE_SCALE -> when (LocalUiMode.current) {
            UiMode.Miuix -> PageScaleDialogMiuix(viewModel)
            UiMode.Material -> PageScaleDialogMaterial(viewModel)
        }

        SettingDialogType.NONE -> { /* None */ }
    }
}
