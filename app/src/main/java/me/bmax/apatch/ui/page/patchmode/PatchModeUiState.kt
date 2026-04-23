package me.bmax.apatch.ui.page.patchmode

import android.net.Uri

data class PatchModeUiState(
    val rootAvailable: Boolean = false,
    val isAbDevice: Boolean = false,
    val selectedOption: InstallMethod? = null,
    val selectedUri: Uri? = null
)