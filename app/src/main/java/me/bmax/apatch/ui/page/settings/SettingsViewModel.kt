package me.bmax.apatch.ui.page.settings

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.LocaleManagerCompat
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.Natives
import me.bmax.apatch.R
import me.bmax.apatch.apApp
import me.bmax.apatch.util.calculateCacheSize
import me.bmax.apatch.util.isGlobalNamespaceEnabled
import me.bmax.apatch.util.rootShellForResult
import me.bmax.apatch.util.setGlobalNamespaceEnabled

class SettingsViewModel : ViewModel() {
    private val prefs = APApplication.sharedPreferences

    var uiState by mutableStateOf(SettingsUiState())
        private set

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

        uiState = uiState.copy(
            enableWebDebugging = prefs.getBoolean("enable_web_debugging", false),
            checkUpdate = prefs.getBoolean("check_update", true),
            themeMode = prefs.getInt("color_mode", 0),
            keyColor = prefs.getInt("key_color", 0),
            currentLanguageIndex = languagesValues.indexOf(tag).coerceAtLeast(0)
        )
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
                    globalMount = withContext(Dispatchers.IO) { isGlobalNamespaceEnabled() }
                }

                uiState = uiState.copy(
                    isKpatchReady = kReady,
                    isApatchReady = aReady,
                    isGlobalNamespaceEnabled = globalMount
                )
            }
        }
    }

    fun refreshCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            val size = calculateCacheSize(apApp)
            uiState = uiState.copy(cacheSize = size)
        }
    }

    fun toggleGlobalNamespace(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            setGlobalNamespaceEnabled(if (enabled) "1" else "0")
            uiState = uiState.copy(isGlobalNamespaceEnabled = enabled)
        }
    }

    fun setWebDebugging(enabled: Boolean) {
        prefs.edit { putBoolean("enable_web_debugging", enabled) }
        uiState = uiState.copy(enableWebDebugging = enabled)
    }

    fun setCheckUpdate(enabled: Boolean) {
        prefs.edit { putBoolean("check_update", enabled) }
        uiState = uiState.copy(checkUpdate = enabled)
    }

    fun setThemeMode(index: Int) {
        prefs.edit { putInt("color_mode", index) }
        uiState = uiState.copy(themeMode = index)
    }

    fun setKeyColor(index: Int) {
        val color = colorValues[index]
        prefs.edit { putInt("key_color", color) }
        uiState = uiState.copy(keyColor = color)
    }

    fun showDialog(dialogType: SettingDialogType) {
        uiState = uiState.copy(currentDialog = dialogType)
    }

    fun dismissDialog() {
        uiState = uiState.copy(currentDialog = SettingDialogType.NONE)
    }

    fun resetSuPath(newPath: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = Natives.resetSuPath(newPath)
            if (success) {
                rootShellForResult("echo $newPath > ${APApplication.SU_PATH_FILE}")
            }
            withContext(Dispatchers.Main) {
                onResult(success)
            }
        }
    }

    fun updateLanguage(context: Context, localeTag: String, index: Int) {
        uiState = uiState.copy(currentLanguageIndex = index)

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