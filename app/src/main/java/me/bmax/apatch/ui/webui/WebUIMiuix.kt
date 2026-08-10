package me.bmax.apatch.ui.webui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
fun HandleWebUIEventMiuix(
    event: WebUIEvent?,
    onAlertResult: () -> Unit,
    onConfirmResult: (Boolean) -> Unit,
    onPromptResult: (String?) -> Unit
) {
    when (event) {
        is WebUIEvent.ShowAlert -> {
            val showDialog = remember(event) { mutableStateOf(true) }
            WindowDialog(
                show = showDialog.value,
                onDismissRequest = {
                    onAlertResult()
                    showDialog.value = false
                },
                content = {
                    Column {
                        Text(event.message)
                        Spacer(Modifier.height(12.dp))
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                onAlertResult()
                                showDialog.value = false
                            },
                            text = stringResource(android.R.string.ok),
                            colors = ButtonDefaults.textButtonColorsPrimary()
                        )
                    }
                }
            )
        }

        is WebUIEvent.ShowConfirm -> {
            val showDialog = remember(event) { mutableStateOf(true) }
            WindowDialog(
                show = showDialog.value,
                onDismissRequest = {
                    onConfirmResult(false)
                    showDialog.value = false
                },
                content = {
                    Column {
                        Text(event.message)
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(
                                onClick = {
                                    onConfirmResult(false)
                                    showDialog.value = false
                                },
                                text = stringResource(android.R.string.cancel),
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(modifier = Modifier.width(20.dp))
                            TextButton(
                                onClick = {
                                    onConfirmResult(true)
                                    showDialog.value = false
                                },
                                text = stringResource(android.R.string.ok),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.textButtonColorsPrimary()
                            )
                        }
                    }
                }
            )
        }

        is WebUIEvent.ShowPrompt -> {
            val showDialog = remember(event) { mutableStateOf(true) }
            var text by remember(event) { mutableStateOf(event.defaultValue) }
            WindowDialog(
                show = showDialog.value,
                onDismissRequest = {
                    onPromptResult(null)
                    showDialog.value = false
                },
                content = {
                    Column {
                        Text(event.message)
                        Spacer(Modifier.height(12.dp))
                        TextField(
                            modifier = Modifier.padding(bottom = 16.dp),
                            value = text,
                            onValueChange = { text = it }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(
                                onClick = {
                                    onPromptResult(null)
                                    showDialog.value = false
                                },
                                text = stringResource(android.R.string.cancel),
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(modifier = Modifier.width(20.dp))
                            TextButton(
                                onClick = {
                                    onPromptResult(text)
                                    showDialog.value = false
                                },
                                text = stringResource(android.R.string.ok),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.textButtonColorsPrimary()
                            )
                        }
                    }
                }
            )
        }

        null -> {}
    }
}
