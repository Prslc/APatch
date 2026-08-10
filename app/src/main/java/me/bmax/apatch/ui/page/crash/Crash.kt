package me.bmax.apatch.ui.page.crash

import androidx.compose.runtime.Composable
import me.bmax.apatch.ui.LocalUiMode
import me.bmax.apatch.ui.UiMode

@Composable
fun CrashScreen(message: String) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> CrashScreenMiuix(message)
        UiMode.Material -> CrashScreenMaterial(message)
    }
}
