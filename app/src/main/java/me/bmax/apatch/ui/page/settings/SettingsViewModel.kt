package me.bmax.apatch.ui.page.settings

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.LocaleManagerCompat
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.apApp
import me.bmax.apatch.data.repository.SettingsRepository
import me.bmax.apatch.data.repository.SettingsRepositoryImpl
import me.bmax.apatch.ui.theme.blurEnabled

var pageScale: Float by mutableFloatStateOf(1.0f)

class SettingsViewModel(
    private val settingsRepo: SettingsRepository = SettingsRepositoryImpl
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
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
        loadPersistentSettings()
        observeApatchState()
        refreshCacheSize()
    }

    private fun loadPersistentSettings() {
        val languagesValues = apApp.resources.getStringArray(R.array.languages_values)
        val currentLocales = LocaleManagerCompat.getApplicationLocales(apApp)
        val tag = if (currentLocales.isEmpty) null else currentLocales.get(0)?.toLanguageTag()

        pageScale = settingsRepo.getPageScale()
        blurEnabled = settingsRepo.getBoolean("blur_enabled", true)

        _uiState.update {
            it.copy(
                enableWebDebugging = settingsRepo.getBoolean("enable_web_debugging", false),
                checkUpdate = settingsRepo.getBoolean("check_update", true),
                blurEnabled = blurEnabled,
                themeMode = settingsRepo.getInt("color_mode", 0),
                keyColor = settingsRepo.getInt("key_color", 0),
                paletteStyle = settingsRepo.getString("palette_style", "TonalSpot"),
                uiMode = settingsRepo.getString("ui_mode", "miuix"),
                currentLanguageIndex = languagesValues.indexOf(tag).coerceAtLeast(0)
            )
        }
    }

    private fun observeApatchState() {
        viewModelScope.launch {
            APApplication.apStateLiveData.asFlow().collect { state ->
                val kReady = state != APApplication.State.UNKNOWN_STATE
                val aReady = state in listOf(
                    APApplication.State.ANDROIDPATCH_INSTALLING,
                    APApplication.State.ANDROIDPATCH_INSTALLED,
                    APApplication.State.ANDROIDPATCH_NEED_UPDATE
                )

                var globalMount = false
                if (kReady && aReady) {
                    globalMount = withContext(Dispatchers.IO) {
                        settingsRepo.isGlobalNamespaceEnabled()
                    }
                }

                _uiState.update {
                    it.copy(
                        isKpatchReady = kReady,
                        isApatchReady = aReady,
                        isGlobalNamespaceEnabled = globalMount
                    )
                }
            }
        }
    }

    fun refreshCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            val size = settingsRepo.calculateCacheSize()
            _uiState.update { it.copy(cacheSize = size) }
        }
    }

    fun toggleGlobalNamespace(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.setGlobalNamespaceEnabled(enabled)
            _uiState.update { it.copy(isGlobalNamespaceEnabled = enabled) }
        }
    }

    fun setWebDebugging(enabled: Boolean) {
        settingsRepo.setBoolean("enable_web_debugging", enabled)
        _uiState.update { it.copy(enableWebDebugging = enabled) }
    }

    fun setCheckUpdate(enabled: Boolean) {
        settingsRepo.setBoolean("check_update", enabled)
        _uiState.update { it.copy(checkUpdate = enabled) }
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

    fun showDialog(dialogType: SettingDialogType) {
        _uiState.update { it.copy(currentDialog = dialogType) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(currentDialog = SettingDialogType.NONE) }
    }

    fun resetSuPath(newPath: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = settingsRepo.resetSuPath(newPath)
            withContext(Dispatchers.Main) {
                onResult(success)
            }
        }
    }

    fun updateLanguage(context: Context, localeTag: String, index: Int) {
        _uiState.update { it.copy(currentLanguageIndex = index) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            val locales = if (localeTag.isEmpty()) {
                LocaleList.getEmptyLocaleList()
            } else {
                LocaleList.forLanguageTags(localeTag)
            }
            // Native API 33+ handles persistence and lifecycle automatically.
            localeManager.applicationLocales = locales
        } else {
            // For API 26-32, we manually persist the preference.
            APApplication.sharedPreferences.edit {
                putString("app_lang", localeTag)
            }

            // Recreate the activity to apply language changes on legacy API levels.
            (context as? Activity)?.recreate()
        }
    }
}
