package me.bmax.apatch.ui.page.patchmode

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.bmax.apatch.util.isABDevice
import me.bmax.apatch.util.isJailbreakMode
import me.bmax.apatch.util.rootAvailable

class PatchModeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PatchModeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val root = rootAvailable()
            val ab = isABDevice()
            val jailbreak = isJailbreakMode()
            _uiState.update {
                it.copy(
                    rootAvailable = root,
                    isAbDevice = ab,
                    jailbreakBlocked = jailbreak,
                    isProbing = false
                )
            }
        }
    }

    fun onOptionSelected(method: InstallMethod) {
        _uiState.update { it.copy(selectedOption = method) }
    }

    fun onFileSelected(uri: Uri) {
        _uiState.update { it.copy(selectedUri = uri) }
    }
}
