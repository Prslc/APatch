package me.bmax.apatch.ui.navigation

import kotlinx.serialization.Serializable
import me.bmax.apatch.ui.page.install.MODULE_TYPE
import me.bmax.apatch.ui.page.patch.PatchesViewModel

@Serializable
object MainRoute

@Serializable
data class InstallPreviewRoute(val uriString: String)

@Serializable
object ModeSelectRoute

@Serializable
object AboutRoute

@Serializable
data class PatchesRoute(
    val mode: PatchesViewModel.PatchMode,
    val bootImageUri: String? = null
)

@Serializable
data class InstallRoute(
    val uriString: String,
    val type: MODULE_TYPE
)

@Serializable
data class ExecuteActionRoute(val moduleId: String)