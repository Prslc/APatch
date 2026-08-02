package me.bmax.apatch.ui.page.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import com.materialkolor.PaletteStyle
import me.bmax.apatch.ui.LocalUiMode
import me.bmax.apatch.ui.UiMode
import me.bmax.apatch.ui.navigation.LocalNavigator
import me.bmax.apatch.ui.page.settings.PageScaleDialogMaterial
import me.bmax.apatch.ui.page.settings.PageScaleDialogMiuix

@Composable
fun ThemeScreen(viewModel: ThemeViewModel = viewModel()) {
    val navigator = LocalNavigator.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val paletteIndex = PaletteStyle.entries.indexOfFirst { it.name == uiState.paletteStyle }.coerceAtLeast(0)
    val keyColorIndex = viewModel.colorValues.indexOf(uiState.keyColor).coerceAtLeast(0)

    val state = ThemeUiState(
        uiMode = uiState.uiMode,
        themeMode = uiState.themeMode,
        keyColorIndex = keyColorIndex,
        paletteStyleIndex = paletteIndex,
    )

    val actions = ThemeScreenActions(
        onBack = dropUnlessResumed { navigator.popBackStack() },
        onSetUiMode = viewModel::setUiMode,
        onSetThemeMode = viewModel::setThemeMode,
        onSetKeyColor = viewModel::setKeyColor,
        onSetPaletteStyle = viewModel::setPaletteStyle,
        onSetPageScale = viewModel::setPageScale,
        onSetBlur = viewModel::setBlurEnabled,
    )

    // Page Scale dialog
    if (uiState.showPageScaleDialog) {
        when (LocalUiMode.current) {
            UiMode.Miuix -> PageScaleDialogMiuix(viewModel)
            UiMode.Material -> PageScaleDialogMaterial(viewModel)
        }
    }

    when (LocalUiMode.current) {
        UiMode.Miuix -> ThemeScreenMiuix(state, actions, viewModel)
        UiMode.Material -> ThemeScreenMaterial(state, actions, viewModel)
    }
}

class ThemeUiState(
    val uiMode: String,
    val themeMode: Int,
    val keyColorIndex: Int,
    val paletteStyleIndex: Int,
)

class ThemeScreenActions(
    val onBack: () -> Unit,
    val onSetUiMode: (String) -> Unit,
    val onSetThemeMode: (Int) -> Unit,
    val onSetKeyColor: (Int) -> Unit,
    val onSetPaletteStyle: (Int) -> Unit,
    val onSetPageScale: (Float) -> Unit,
    val onSetBlur: (Boolean) -> Unit,
)
