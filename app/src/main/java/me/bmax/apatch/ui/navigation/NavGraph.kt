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
import me.bmax.apatch.ui.screen.AboutScreen
import me.bmax.apatch.ui.screen.ExecuteAPMActionScreen
import me.bmax.apatch.ui.screen.InstallScreen
import me.bmax.apatch.ui.screen.MODULE_TYPE
import me.bmax.apatch.ui.screen.ModeSelectScreen

@Composable
fun NavGraph() {
    val navigator = LocalNavigator.current
    navigator.HandleGlobalIntents()

    NavHost(
        navController = navigator.navController,
        startDestination = Main,
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
        composable<Main> {
            MainScreen()
        }

        dialog<InstallPreview> { backStackEntry ->
            val args = backStackEntry.toRoute<InstallPreview>()
            ModuleInstallDialog(
                uri = args.uriString.toUri(),
                onDismiss = { navigator.navController.popBackStack() },
                onConfirm = { uri ->
                    navigator.navController.popBackStack()
                    navigator.navController.navigate(
                        Install(uriString = uri.toString(), type = MODULE_TYPE.APM)
                    )
                }
            )
        }

        composable<ModeSelect> {
            ModeSelectScreen()
        }

        composable<About> {
            AboutScreen()
        }

        composable<Patches> { backStackEntry ->
            val args = backStackEntry.toRoute<Patches>()
            Patches(mode = args.mode)
        }

        composable<ExecuteAction> { backStackEntry ->
            val args = backStackEntry.toRoute<ExecuteAction>()
            ExecuteAPMActionScreen(moduleId = args.moduleId)
        }

        composable<Install> { backStackEntry ->
            val args = backStackEntry.toRoute<Install>()
            val uri = args.uriString.toUri()
            InstallScreen(
                uri = uri,
                type = args.type
            )
        }
    }
}