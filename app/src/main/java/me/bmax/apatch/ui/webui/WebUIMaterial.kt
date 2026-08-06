package me.bmax.apatch.ui.webui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

@Composable
fun HandleWebUIEventMaterial(
    event: WebUIEvent?,
    onAlertResult: () -> Unit,
    onConfirmResult: (Boolean) -> Unit,
    onPromptResult: (String?) -> Unit
) {
    when (event) {
        is WebUIEvent.ShowAlert -> {
            val showDialog = remember(event) { mutableStateOf(true) }
            if (showDialog.value) {
                AlertDialog(
                    onDismissRequest = {
                        onAlertResult()
                        showDialog.value = false
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                onAlertResult()
                                showDialog.value = false
                            },
                        ) {
                            Text(stringResource(android.R.string.ok))
                        }
                    },
                    text = {
                        Text(event.message)
                    }
                )
            }
        }

        is WebUIEvent.ShowConfirm -> {
            val showDialog = remember(event) { mutableStateOf(true) }
            if (showDialog.value) {
                AlertDialog(
                    onDismissRequest = {
                        onConfirmResult(false)
                        showDialog.value = false
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                onConfirmResult(true)
                                showDialog.value = false
                            },
                        ) {
                            Text(stringResource(android.R.string.ok))
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                onConfirmResult(false)
                                showDialog.value = false
                            },
                        ) {
                            Text(stringResource(android.R.string.cancel))
                        }
                    },
                    text = {
                        Text(event.message)
                    }
                )
            }
        }

        is WebUIEvent.ShowPrompt -> {
            val showDialog = remember(event) { mutableStateOf(true) }
            var text by remember(event) { mutableStateOf(event.defaultValue) }
            if (showDialog.value) {
                AlertDialog(
                    onDismissRequest = {
                        onPromptResult(null)
                        showDialog.value = false
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                onPromptResult(text)
                                showDialog.value = false
                            },
                        ) {
                            Text(stringResource(android.R.string.ok))
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                onPromptResult(null)
                                showDialog.value = false
                            },
                        ) {
                            Text(stringResource(android.R.string.cancel))
                        }
                    },
                    text = {
                        Column {
                            OutlinedTextField(
                                label = { Text(event.message) },
                                value = text,
                                onValueChange = { text = it },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                )
            }
        }

        null -> {}
    }
}
