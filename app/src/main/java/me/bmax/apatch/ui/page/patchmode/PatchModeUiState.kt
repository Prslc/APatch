package me.bmax.apatch.ui.page.patchmode

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
data class PatchModeUiState(
    val isProbing: Boolean = true,
    val rootAvailable: Boolean = false,
    val isAbDevice: Boolean = false,
    val jailbreakBlocked: Boolean = false,
    val selectedOption: InstallMethod? = null,
    val selectedUri: Uri? = null
)
