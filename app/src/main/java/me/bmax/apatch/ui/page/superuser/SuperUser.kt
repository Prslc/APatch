package me.bmax.apatch.ui.page.superuser

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.compose.viewModel
import me.bmax.apatch.ui.LocalUiMode
import me.bmax.apatch.ui.UiMode
import me.bmax.apatch.ui.page.material.superuser.SuperUserScreenMaterial
import me.bmax.apatch.ui.page.miuix.superuser.SuperUserScreenMiuix

@Composable
fun SuperUserScreen(
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
    isCurrentPage: Boolean = true,
    viewModel: SuperUserViewModel = viewModel()
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> SuperUserScreenMiuix(bottomPadding, modifier, isCurrentPage, viewModel)
        UiMode.Material -> SuperUserScreenMaterial(bottomPadding, modifier, isCurrentPage, viewModel)
    }
}
