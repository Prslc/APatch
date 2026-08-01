package me.bmax.apatch.ui.component.loadingindicator

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import me.bmax.apatch.R
import me.bmax.apatch.ui.LocalUiMode
import me.bmax.apatch.ui.UiMode

@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    text: String? = stringResource(R.string.loading)
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> LoadingIndicatorMiuix(modifier, text)
        UiMode.Material -> LoadingIndicatorMaterial(modifier, text)
    }
}
