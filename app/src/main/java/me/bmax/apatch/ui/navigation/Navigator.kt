package me.bmax.apatch.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.MutableSharedFlow
import me.bmax.apatch.ui.page.patch.PatchMode

val LocalNavigator = staticCompositionLocalOf<Navigator> {
    error("NavController not provided")
}

class Navigator(val navController: NavHostController) {
    private val intentEvents = MutableSharedFlow<android.content.Intent>(
        replay = 1,
        extraBufferCapacity = 1
    )

    fun onNewIntent(intent: android.content.Intent) {
        intentEvents.tryEmit(intent)
    }

    @Composable
    fun HandleGlobalIntents() {
        LaunchedEffect(Unit) {
            intentEvents.collect { intent ->
                performNavigation(intent)
            }
        }
    }

    private fun performNavigation(intent: android.content.Intent) {
        val type = intent.getStringExtra("shortcut_type")

        val uri: Uri? = intent.data
            ?: if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayListExtra("uris", Uri::class.java)?.firstOrNull()
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra<Uri>("uris")?.firstOrNull()
            }

        when {
            type == "module_action" -> {
                val moduleId = intent.getStringExtra("module_id") ?: return
                navigateToExecuteAction(moduleId)
            }

            uri != null -> {
                navController.navigate(InstallPreviewRoute(uri.toString()))
            }

            else -> {}
        }

        // clean
        intent.data = null
        intent.removeExtra("shortcut_type")
        intent.removeExtra("uris")
    }

    private var _switchToTabHandler: ((Int) -> Unit)? = null

    fun bindPager(handler: (Int) -> Unit) {
        this._switchToTabHandler = handler
    }

    fun switchToTab(index: Int) {
        _switchToTabHandler?.invoke(index)
    }

    fun navigateToModeSelect() = navController.navigate(ModeSelectRoute)
    fun navigateToAbout() = navController.navigate(AboutRoute)

    fun navigateToPatches(mode: PatchMode, uri: Uri? = null) {
        navController.navigate(PatchesRoute(mode, uri?.toString()))
    }

    fun navigateToExecuteAction(moduleId: String) {
        navController.navigate(
            TerminalRoute(
                taskType = TERMINAL_TASK_TYPE.ACTION,
                targetId = moduleId,
                moduleType = MODULE_TYPE.APM
            )
        ) {
            launchSingleTop = true
        }
    }

    fun navigateToInstall(uri: Uri, type: MODULE_TYPE = MODULE_TYPE.APM) {
        navController.navigate(
            TerminalRoute(
                taskType = TERMINAL_TASK_TYPE.INSTALL,
                targetId = uri.toString(),
                moduleType = type
            )
        )
    }

    fun popBackStack() = navController.popBackStack()
}
