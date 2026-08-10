package me.bmax.apatch.ui.page.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import me.bmax.apatch.R
import me.bmax.apatch.ui.LocalUiMode
import me.bmax.apatch.ui.UiMode
import me.bmax.apatch.ui.component.dialog.ConfirmResult
import me.bmax.apatch.ui.component.dialog.rememberConfirmDialog
import me.bmax.apatch.ui.component.dialog.rememberLoadingDialog
import me.bmax.apatch.util.clearAppCache
import me.bmax.apatch.util.formatSize

@Composable
fun SettingsDialogOverlay(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    when (uiState.currentDialog) {
        SettingDialogType.RESET_SU_PATH -> when (LocalUiMode.current) {
            UiMode.Miuix -> ResetSUPathDialogMiuix(viewModel)
            UiMode.Material -> ResetSUPathDialogMaterial(viewModel)
        }

        SettingDialogType.CLEAR_CACHE ->
            ClearDialog(uiState.cacheSize, viewModel)

        SettingDialogType.SEND_LOG -> when (LocalUiMode.current) {
            UiMode.Miuix -> LogDialogMiuix(viewModel)
            UiMode.Material -> LogDialogMaterial(viewModel)
        }

        SettingDialogType.NONE -> { /* None */ }
    }
}

@Composable
private fun ClearDialog(cacheSize: Long, viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val loading = rememberLoadingDialog()
    var confirmed by remember { mutableStateOf(false) }

    val title = stringResource(R.string.clear_cache_title)
    val message = stringResource(R.string.clear_cache_message, formatSize(cacheSize))

    if (confirmed) {
        LaunchedEffect(Unit) {
            loading.withLoading { clearAppCache(context) }
            viewModel.refreshCacheSize()
            viewModel.dismissDialog()
        }
        return
    }

    val confirmDialog = rememberConfirmDialog()
    val okText = stringResource(android.R.string.ok)
    val cancelText = stringResource(android.R.string.cancel)
    LaunchedEffect(Unit) {
        val result = confirmDialog.awaitConfirm(
            title = title,
            content = message,
            confirm = okText,
            dismiss = cancelText
        )
        if (result == ConfirmResult.Confirmed) {
            confirmed = true
        } else {
            viewModel.dismissDialog()
        }
    }
}
