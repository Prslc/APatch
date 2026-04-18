package me.bmax.apatch.ui.navigation

import kotlinx.serialization.Serializable
import me.bmax.apatch.ui.screen.MODULE_TYPE
import me.bmax.apatch.ui.viewmodel.PatchesViewModel

@Serializable
object Main

@Serializable
data class InstallPreview(val uriString: String)

@Serializable
object ModeSelect

@Serializable
object About

@Serializable
data class Patches(
    val mode: PatchesViewModel.PatchMode,
    val bootImageUri: String? = null
)

@Serializable
data class Install(
    val uriString: String,
    val type: MODULE_TYPE
)

@Serializable
data class ExecuteAction(val moduleId: String)