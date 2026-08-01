package me.bmax.apatch.ui.page.about

import androidx.compose.runtime.Composable
import me.bmax.apatch.ui.LocalUiMode
import me.bmax.apatch.ui.UiMode

@Composable
fun AboutScreen() {
    when (LocalUiMode.current) {
        UiMode.Miuix -> AboutScreenMiuix()
        UiMode.Material -> AboutScreenMaterial()
    }
}
