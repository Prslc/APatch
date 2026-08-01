package me.bmax.apatch.ui.page.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import me.bmax.apatch.R
import me.bmax.apatch.ui.LocalUiMode
import me.bmax.apatch.ui.UiMode
import me.bmax.apatch.ui.component.rememberLoadingDialog
import me.bmax.apatch.ui.page.material.settings.PageScaleDialogMaterial
import me.bmax.apatch.ui.page.miuix.settings.LogDialogMiuix
import me.bmax.apatch.ui.page.miuix.settings.PageScaleDialogMiuix
import me.bmax.apatch.ui.page.miuix.settings.ResetSUPathDialogMiuix
import me.bmax.apatch.util.clearAppCache
import me.bmax.apatch.util.formatSize

@Composable
fun SettingsDialogOverlay(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    when (uiState.currentDialog) {
        SettingDialogType.RESET_SU_PATH ->
            ResetSUPathDialogMiuix(viewModel)

        SettingDialogType.CLEAR_CACHE ->
            ClearDialog(uiState.cacheSize, viewModel)

        SettingDialogType.SEND_LOG ->
            LogDialogMiuix(viewModel)

        SettingDialogType.PAGE_SCALE -> when (LocalUiMode.current) {
            UiMode.Miuix -> PageScaleDialogMiuix(viewModel)
            UiMode.Material -> PageScaleDialogMaterial(viewModel)
        }

        SettingDialogType.NONE -> { /* None */ }
    }
}

@Composable
private fun ClearDialog(cacheSize: Long, viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val loading = rememberLoadingDialog()
    var confirmed by remember { mutableStateOf(false) }

    val title = context.getString(R.string.clear_cache_title)
    val message = context.getString(R.string.clear_cache_message, formatSize(cacheSize))

    if (confirmed) {
        LaunchedEffect(Unit) {
            loading.withLoading { clearAppCache(context) }
            viewModel.refreshCacheSize()
            viewModel.dismissDialog()
        }
        return
    }

    when (LocalUiMode.current) {
        UiMode.Miuix -> me.bmax.apatch.ui.page.miuix.settings.ClearDialogMiuix(cacheSize, viewModel)
        UiMode.Material -> androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.dismissDialog() },
            title = { androidx.compose.material3.Text(title) },
            text = { androidx.compose.material3.Text(message) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { confirmed = true }) {
                    androidx.compose.material3.Text(context.getString(android.R.string.ok))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { viewModel.dismissDialog() }) {
                    androidx.compose.material3.Text(context.getString(android.R.string.cancel))
                }
            }
        )
    }
}
