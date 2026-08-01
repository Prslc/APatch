package me.bmax.apatch.ui.page.patch

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import me.bmax.apatch.ui.LocalUiMode
import me.bmax.apatch.ui.UiMode

@Composable
fun PatchesScreen(
    mode: PatchMode,
    bootImageUri: Uri? = null,
    viewModel: PatchesViewModel = viewModel()
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> PatchesScreenMiuix(mode, bootImageUri, viewModel)
        UiMode.Material -> PatchesScreenMaterial(mode, bootImageUri, viewModel)
    }
}
