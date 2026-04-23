package me.bmax.apatch.ui.page.patchmode

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import me.bmax.apatch.util.isABDevice
import me.bmax.apatch.util.rootAvailable

class PatchModeViewModel : ViewModel() {
    var uiState by mutableStateOf(PatchModeUiState())
        private set

    init {
        uiState = uiState.copy(
            rootAvailable = rootAvailable(),
            isAbDevice = isABDevice()
        )
    }

    fun onOptionSelected(method: InstallMethod) {
        uiState = uiState.copy(selectedOption = method)
    }

    fun onFileSelected(uri: Uri) {
        uiState = uiState.copy(selectedUri = uri)
    }
}