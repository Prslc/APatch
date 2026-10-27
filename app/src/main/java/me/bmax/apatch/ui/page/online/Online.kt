package me.bmax.apatch.ui.page.online

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import me.bmax.apatch.ui.LocalUiMode
import me.bmax.apatch.ui.UiMode

@Composable
fun OnlineScreen(
    kpmOnly: Boolean,
    onBack: () -> Unit,
    viewModel: OnlineViewModel = viewModel()
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> OnlineScreenMiuix(kpmOnly, onBack, viewModel)
        UiMode.Material -> OnlineScreenMaterial(kpmOnly, onBack, viewModel)
    }
}
