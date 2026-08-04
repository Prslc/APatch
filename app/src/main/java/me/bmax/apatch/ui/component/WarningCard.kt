package me.bmax.apatch.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import me.bmax.apatch.ui.LocalUiMode
import me.bmax.apatch.ui.UiMode
import me.bmax.apatch.ui.component.material.WarningCardMaterial
import me.bmax.apatch.ui.component.miuix.WarningCard as WarningCardMiuix
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun WarningCard(
    message: String,
    containerColor: Color? = null,
    contentColor: Color? = null,
    onClick: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> WarningCardMiuix(
            message = message,
            containerColor = containerColor ?: MiuixTheme.colorScheme.error,
            contentColor = contentColor ?: MiuixTheme.colorScheme.onError,
            onClick = onClick,
            onClose = onClose,
            icon = icon
        )
        UiMode.Material -> WarningCardMaterial(
            message = message,
            containerColor = containerColor ?: MaterialTheme.colorScheme.errorContainer,
            contentColor = contentColor ?: MaterialTheme.colorScheme.onErrorContainer,
            onClick = onClick,
            onClose = onClose,
            icon = icon
        )
    }
}
