package me.bmax.apatch.ui.page.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import com.materialkolor.dynamiccolor.ColorSpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import me.bmax.apatch.data.repository.SettingsRepository
import me.bmax.apatch.data.repository.SettingsRepositoryImpl
import me.bmax.apatch.ui.theme.blurEnabled

var pageScale: Float by mutableFloatStateOf(1.0f)

data class ThemeState(
    val blurEnabled: Boolean = true,
    val themeMode: Int = 0,
    val keyColor: Int = 0,
    val paletteStyle: String = "TonalSpot",
    val colorSpec: String = "SPEC_2025",
    val uiMode: String = "miuix",
    val showPageScaleDialog: Boolean = false,
)

class ThemeViewModel(
    private val settingsRepo: SettingsRepository = SettingsRepositoryImpl
) : ViewModel() {
    private val _uiState = MutableStateFlow(ThemeState())
    val uiState = _uiState.asStateFlow()

    val colorValues = listOf(
        0,
        Color(0xFFEA4335).toArgb(),
        Color(0xFF34A853).toArgb(),
        Color(0xFF1A73E8).toArgb(),
        Color(0xFF9333EA).toArgb(),
        Color(0xFFFB8C00).toArgb(),
        Color(0xFF009688).toArgb(),
        Color(0xFFE91E63).toArgb(),
        Color(0xFF795548).toArgb(),
    )

    init {
        loadSettings()
    }

    private fun loadSettings() {
        pageScale = settingsRepo.getPageScale()
        blurEnabled = settingsRepo.getBoolean("blur_enabled", true)
        _uiState.update {
            it.copy(
                blurEnabled = blurEnabled,
                themeMode = settingsRepo.getInt("color_mode", 0),
                keyColor = settingsRepo.getInt("key_color", 0),
                paletteStyle = settingsRepo.getString("palette_style", "TonalSpot"),
                colorSpec = settingsRepo.getString("color_spec", "SPEC_2025"),
                uiMode = settingsRepo.getString("ui_mode", "miuix"),
            )
        }
    }

    fun setBlurEnabled(enabled: Boolean) {
        settingsRepo.setBoolean("blur_enabled", enabled)
        blurEnabled = enabled
        _uiState.update { it.copy(blurEnabled = enabled) }
    }

    fun setPageScale(scale: Float) {
        settingsRepo.setPageScale(scale)
        pageScale = scale.coerceIn(0.8f, 1.1f)
    }

    fun setThemeMode(index: Int) {
        settingsRepo.setInt("color_mode", index)
        _uiState.update { it.copy(themeMode = index) }
    }

    fun setUiMode(mode: String) {
        settingsRepo.setString("ui_mode", mode)
        _uiState.update { it.copy(uiMode = mode) }
    }

    fun setKeyColor(index: Int) {
        val color = colorValues[index]
        settingsRepo.setInt("key_color", color)
        _uiState.update { it.copy(keyColor = color) }
    }

    fun setPaletteStyle(index: Int) {
        val style = com.materialkolor.PaletteStyle.entries[index].name
        settingsRepo.setString("palette_style", style)
        _uiState.update { it.copy(paletteStyle = style) }
    }

    fun setColorSpec(index: Int) {
        val spec = ColorSpec.SpecVersion.entries[index].name
        settingsRepo.setString("color_spec", spec)
        _uiState.update { it.copy(colorSpec = spec) }
    }

    fun showPageScaleDialog() {
        _uiState.update { it.copy(showPageScaleDialog = true) }
    }

    fun dismissPageScaleDialog() {
        _uiState.update { it.copy(showPageScaleDialog = false) }
    }
}
