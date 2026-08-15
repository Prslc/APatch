package me.bmax.apatch.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.toRoute
import me.bmax.apatch.ui.LocalUiMode
import me.bmax.apatch.ui.MainScreen
import me.bmax.apatch.ui.UiMode
import me.bmax.apatch.ui.component.moduleinstalldialog.ModuleInstallDialog
import me.bmax.apatch.ui.page.about.AboutScreen
import me.bmax.apatch.ui.page.patch.PatchesScreen
import me.bmax.apatch.ui.page.theme.ThemeScreen
import me.bmax.apatch.ui.page.patchmode.PatchMode
import me.bmax.apatch.ui.page.terminal.TerminalScreen
import androidx.compose.material3.MaterialTheme
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun NavGraph() {
    val navigator = LocalNavigator.current
    navigator.HandleGlobalIntents()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = when (LocalUiMode.current) {
            UiMode.Miuix -> MiuixTheme.colorScheme.surface
            UiMode.Material -> MaterialTheme.colorScheme.surfaceContainer
        }
    ) {
        NavHost(
            navController = navigator.navController,
            startDestination = MainRoute,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                )
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { -it / 5 }, animationSpec = tween(300)) +
                        fadeOut(targetAlpha = 0f, animationSpec = tween(300))
            },
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { -it / 5 }, animationSpec = tween(300)) +
                        fadeIn(initialAlpha = 0f, animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                )
            }
        ) {
            dialog<InstallPreviewRoute> { backStackEntry ->
                val args = backStackEntry.toRoute<InstallPreviewRoute>()
                val previewUri = args.uriString.toUri()

                ModuleInstallDialog(
                    uri = previewUri,
                    onDismiss = { navigator.navController.popBackStack() },
                    onConfirm = { confirmedUri ->
                        navigator.navController.popBackStack()
                        navigator.navigateToInstall(confirmedUri)
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

            composable<ThemeRoute> {
                ThemeScreen()
            }

            composable<PatchesRoute> { backStackEntry ->
                val args = backStackEntry.toRoute<PatchesRoute>()
                val uri = remember(args.bootImageUri) {
                    args.bootImageUri?.toUri()
                }
                PatchesScreen(
                    mode = args.mode,
                    bootImageUri = uri
                )
            }

            composable<TerminalRoute> { backStackEntry ->
                val args = backStackEntry.toRoute<TerminalRoute>()
                TerminalScreen(
                    taskType = args.taskType,
                    targetId = args.targetId,
                    moduleType = args.moduleType,
                    onBack = { navigator.popBackStack() }
                )
            }
        }
    }
}
