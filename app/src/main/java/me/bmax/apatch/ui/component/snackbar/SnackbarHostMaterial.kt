// This file includes code derived from https://github.com/ReSukiSU/ReSukiSU
// Swipeable snackbar host adapted from ui/component/SwipeableSnackbarHost.kt
package me.bmax.apatch.ui.component.snackbar

import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier

@Composable
fun SwipeableSnackbarHostMaterial(
    modifier: Modifier = Modifier,
    hostState: SnackbarHostState,
    snackbar: @Composable (SnackbarData) -> Unit = { Snackbar(it) },
) {
    val state = rememberSwipeToDismissBoxState()

    SwipeToDismissBox(
        state = state,
        backgroundContent = {},
        onDismiss = {
            hostState.currentSnackbarData?.dismiss()
        }
    ) {
        SnackbarHost(
            modifier = modifier,
            hostState = hostState,
            snackbar = snackbar,
        )
    }

    LaunchedEffect(hostState.currentSnackbarData) {
        if (hostState.currentSnackbarData == null) return@LaunchedEffect

        state.snapTo(SwipeToDismissBoxValue.Settled)
    }
}
