package me.bmax.apatch.ui.navigation

import kotlinx.serialization.Serializable
import me.bmax.apatch.ui.page.patch.PatchesViewModel

@Serializable
enum class MODULE_TYPE {
    KPM, APM
}

@Serializable
enum class TERMINAL_TASK_TYPE {
    INSTALL, ACTION
}

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
data class TerminalRoute(
    val taskType: TERMINAL_TASK_TYPE,    // INSTALL or ACTION
    val targetId: String,                // uri or module id
    val moduleType: MODULE_TYPE          // APM or KPM
)