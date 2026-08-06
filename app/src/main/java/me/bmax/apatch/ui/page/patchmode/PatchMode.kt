package me.bmax.apatch.ui.page.patchmode

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import me.bmax.apatch.R
import me.bmax.apatch.ui.LocalUiMode
import me.bmax.apatch.ui.UiMode

sealed class InstallMethod {
    data class SelectFile(@param:StringRes override val label: Int = R.string.mode_select_page_select_file) :
        InstallMethod()

    data object DirectInstall : InstallMethod() {
        override val label = R.string.mode_select_page_patch_and_install
    }

    data object DirectInstallToInactiveSlot : InstallMethod() {
        override val label = R.string.mode_select_page_install_inactive_slot
    }

    abstract val label: Int
}

@Composable
fun PatchMode(viewModel: PatchModeViewModel = viewModel()) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> PatchModeMiuix(viewModel)
        UiMode.Material -> PatchModeMaterial(viewModel)
    }
}
