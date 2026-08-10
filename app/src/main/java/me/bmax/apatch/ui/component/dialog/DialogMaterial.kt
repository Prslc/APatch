package me.bmax.apatch.ui.component.dialog

import android.graphics.text.LineBreaker
import android.os.Build
import android.text.Layout
import android.text.method.LinkMovementMethod
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.noties.markwon.Markwon
import io.noties.markwon.utils.NoCopySpannableFactory
import me.bmax.apatch.ui.component.dialog.ConfirmDialogVisuals
import me.bmax.apatch.ui.component.loadingindicator.LoadingIndicator

@Composable
internal fun LoadingDialogMaterial(showDialog: MutableState<Boolean>) {
    if (showDialog.value) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnClickOutside = false,
                dismissOnBackPress = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Surface(
                modifier = Modifier.size(100.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                ) {
                    LoadingIndicator()
                }
            }
        }
    }
}

@Composable
internal fun ConfirmDialogMaterial(
    visuals: ConfirmDialogVisuals,
    confirm: () -> Unit,
    dismiss: () -> Unit,
    showDialog: MutableState<Boolean>
) {
    if (showDialog.value) {
        AlertDialog(
            onDismissRequest = {
                dismiss()
                showDialog.value = false
            },
            title = { Text(visuals.title) },
            text = {
                if (visuals.isMarkdown) {
                    MarkdownContentMaterial(content = visuals.content)
                } else {
                    Text(text = visuals.content)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirm()
                        showDialog.value = false
                    }
                ) {
                    Text(visuals.confirm ?: stringResource(id = android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        dismiss()
                        showDialog.value = false
                    }
                ) {
                    Text(visuals.dismiss ?: stringResource(id = android.R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun MarkdownContentMaterial(content: String) {
    val contentColor = MaterialTheme.colorScheme.onBackground.toArgb()

    AndroidView(
        factory = { context ->
            TextView(context).apply {
                movementMethod = LinkMovementMethod.getInstance()
                setSpannableFactory(NoCopySpannableFactory.getInstance())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    breakStrategy = LineBreaker.BREAK_STRATEGY_SIMPLE
                    hyphenationFrequency = Layout.HYPHENATION_FREQUENCY_NONE
                }

                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        update = {
            Markwon.create(it.context).setMarkdown(it, content)
            it.setTextColor(contentColor)
        }
    )
}

@Composable
internal fun EditableDialogMaterial(
    title: String,
    content: @Composable () -> Unit,
    confirmText: String?,
    dismissText: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = content,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText ?: stringResource(id = android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText ?: stringResource(id = android.R.string.cancel))
            }
        }
    )
}
