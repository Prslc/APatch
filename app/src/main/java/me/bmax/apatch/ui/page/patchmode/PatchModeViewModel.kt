package me.bmax.apatch.ui.page.patchmode

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import me.bmax.apatch.util.isABDevice
import me.bmax.apatch.util.isJailbreakMode
import me.bmax.apatch.util.rootAvailable

class PatchModeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PatchModeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(
                rootAvailable = rootAvailable(),
                isAbDevice = isABDevice(),
                jailbreakBlocked = isJailbreakMode()
            )
        }
    }

    fun onOptionSelected(method: InstallMethod) {
        _uiState.update { it.copy(selectedOption = method) }
    }

    fun onFileSelected(uri: Uri) {
        _uiState.update { it.copy(selectedUri = uri) }
    }
}