package me.bmax.apatch.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import me.bmax.apatch.ui.LocalUiMode
import me.bmax.apatch.ui.UiMode
import top.yukonga.miuix.kmp.basic.SnackbarDuration
import top.yukonga.miuix.kmp.basic.SnackbarHostState as MiuixSnackbarHostState
import top.yukonga.miuix.kmp.basic.SnackbarResult as MiuixSnackbarResult
import androidx.compose.material3.SnackbarHostState as Material3SnackbarHostState
import androidx.compose.material3.SnackbarResult as Material3SnackbarResult

/**
 * Dual-UI snackbar host state: holds one host per UI mode and dispatches
 * [showSnackbar] to the active mode's host.
 *
 * Returns true when the snackbar action was performed (e.g. reboot clicked).
 */
@Stable
class AppSnackbarHostState {
    val miuixState = MiuixSnackbarHostState()
    val materialState = Material3SnackbarHostState()

    // Kept in sync during composition (see rememberAppSnackbarHostState)
    var mode: UiMode = UiMode.Miuix

    suspend fun showSnackbar(
        message: String,
        actionLabel: String? = null,
        duration: SnackbarDuration = SnackbarDuration.Short
    ): Boolean {
        return when (mode) {
            UiMode.Miuix ->
                miuixState.showSnackbar(
                    message = message,
                    actionLabel = actionLabel,
                    duration = duration
                ) == MiuixSnackbarResult.ActionPerformed

            UiMode.Material ->
                materialState.showSnackbar(
                    message = message,
                    actionLabel = actionLabel,
                    duration = when (duration) {
                        SnackbarDuration.Short -> androidx.compose.material3.SnackbarDuration.Short
                        SnackbarDuration.Long -> androidx.compose.material3.SnackbarDuration.Long
                        SnackbarDuration.Indefinite -> androidx.compose.material3.SnackbarDuration.Indefinite
                        // M3 has no Custom; fall back to Long
                        is SnackbarDuration.Custom -> androidx.compose.material3.SnackbarDuration.Long
                    }
                ) == Material3SnackbarResult.ActionPerformed
        }
    }
}

@Composable
fun rememberAppSnackbarHostState(): AppSnackbarHostState {
    val state = remember { AppSnackbarHostState() }
    state.mode = LocalUiMode.current
    return state
}
