package me.bmax.apatch.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.core.net.toUri
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.toRoute
import me.bmax.apatch.ui.MainScreen
import me.bmax.apatch.ui.component.ModuleInstallDialog
import me.bmax.apatch.ui.page.about.AboutScreen
import me.bmax.apatch.ui.page.apm.ExecuteAPMActionScreen
import me.bmax.apatch.ui.page.install.InstallScreen
import me.bmax.apatch.ui.page.install.MODULE_TYPE
import me.bmax.apatch.ui.page.patch.mode.PatchMode
import me.bmax.apatch.ui.page.patch.PatchesScreen

@Composable
fun NavGraph() {
    val navigator = LocalNavigator.current
    navigator.HandleGlobalIntents()

    NavHost(
        navController = navigator.navController,
        startDestination = MainRoute,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(500, easing = FastOutSlowInEasing)
            )
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it / 5 },
                animationSpec = tween(500, easing = FastOutSlowInEasing)
            )
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 5 },
                animationSpec = tween(500, easing = FastOutSlowInEasing)
            )
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(500, easing = FastOutSlowInEasing)
            )
        }
    ) {
        dialog<InstallPreviewRoute> { backStackEntry ->
            val args = backStackEntry.toRoute<InstallPreviewRoute>()
            ModuleInstallDialog(
                uri = args.uriString.toUri(),
                onDismiss = { navigator.navController.popBackStack() },
                onConfirm = { uri ->
                    navigator.navController.popBackStack()
                    navigator.navController.navigate(
                        InstallRoute(uriString = uri.toString(), type = MODULE_TYPE.APM)
                    )
                }
            )
        }

        composable<MainRoute> {
            MainScreen()
        }

        composable<ModeSelectRoute> {
            PatchMode()
        }

        composable<AboutRoute> {
            AboutScreen()
        }

        composable<PatchesRoute> { backStackEntry ->
            val args = backStackEntry.toRoute<PatchesRoute>()
            PatchesScreen(mode = args.mode)
        }

        composable<ExecuteActionRoute> { backStackEntry ->
            val args = backStackEntry.toRoute<ExecuteActionRoute>()
            ExecuteAPMActionScreen(moduleId = args.moduleId)
        }

        composable<InstallRoute> { backStackEntry ->
            val args = backStackEntry.toRoute<InstallRoute>()
            val uri = args.uriString.toUri()
            InstallScreen(
                uri = uri,
                type = args.type
            )
        }
    }
}