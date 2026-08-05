package me.bmax.apatch.ui.page.settings

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import android.util.Log
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
import me.bmax.apatch.Natives
import me.bmax.apatch.R
import me.bmax.apatch.apApp
import me.bmax.apatch.data.repository.SettingsRepository
import me.bmax.apatch.data.repository.SettingsRepositoryImpl
import me.bmax.apatch.util.rootShellForResult

class SettingsViewModel(
    private val settingsRepo: SettingsRepository = SettingsRepositoryImpl
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadPersistentSettings()
        observeApatchState()
        refreshCacheSize()
    }

    private fun loadPersistentSettings() {
        val languagesValues = apApp.resources.getStringArray(R.array.languages_values)
        val currentLocales = LocaleManagerCompat.getApplicationLocales(apApp)
        val tag = if (currentLocales.isEmpty) null else currentLocales.get(0)?.toLanguageTag()

        _uiState.update {
            it.copy(
                sucompatEnabled = settingsRepo.getBoolean("sucompat_enabled", false),
                enableWebDebugging = settingsRepo.getBoolean("enable_web_debugging", false),
                checkUpdate = settingsRepo.getBoolean("check_update", true),
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

    fun toggleSucompat(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = if (enabled) {
                // Enable: create marker file and register hooks via supercall
                rootShellForResult("touch ${APApplication.SUCOMPAT_FILE}")
                Natives.controlFeature("sucompat_extra", true)
            } else {
                // Disable: remove marker file and unregister hooks via supercall
                rootShellForResult("rm -f ${APApplication.SUCOMPAT_FILE}")
                Natives.controlFeature("sucompat_extra", false)
            }
            Log.d("SucompatToggle", "sucompat_extra ${if (enabled) "enable" else "disable"} result: $result")
            if (result == 0L) {
                settingsRepo.setBoolean("sucompat_enabled", enabled)
                _uiState.update { it.copy(sucompatEnabled = enabled) }
            }
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
