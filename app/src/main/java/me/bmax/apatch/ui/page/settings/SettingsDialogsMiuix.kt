package me.bmax.apatch.ui.page.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.BuildConfig
import me.bmax.apatch.Natives
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.dialog.rememberLoadingDialog
import me.bmax.apatch.ui.page.theme.ThemeViewModel
import me.bmax.apatch.ui.page.theme.pageScale
import me.bmax.apatch.util.getBugreportFile
import me.bmax.apatch.util.outputStream
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.window.WindowDialog
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun LogDialogMiuix(viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val loadingDialog = rememberLoadingDialog()
    val logSavedMessage = stringResource(R.string.log_saved)

    val exportBugreportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/gzip")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                loadingDialog.show()
                runCatching {
                    uri.outputStream().use { output ->
                        getBugreportFile(context).inputStream().use { it.copyTo(output) }
                    }
                }.onSuccess {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, logSavedMessage, Toast.LENGTH_LONG).show()
                    }
                }
                loadingDialog.hide()
                withContext(Dispatchers.Main) {
                    viewModel.dismissDialog()
                }
            }
        }
    }

    WindowDialog(
        show = true,
        title = stringResource(R.string.send_log),
        onDismissRequest = { viewModel.dismissDialog() }
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Card {
                ArrowPreference(
                    title = stringResource(R.string.save_log),
                    summary = stringResource(R.string.send_log_summary),
                    onClick = {
                        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH_mm")
                        val current = LocalDateTime.now().format(formatter)
                        exportBugreportLauncher.launch("APatch_bugreport_${current}.tar.gz")
                    },
                    startAction = {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 12.dp),
                            tint = colorScheme.primary
                        )
                    }
                )

                ArrowPreference(
                    title = stringResource(R.string.send_log),
                    summary = stringResource(R.string.send_log_summary),
                    onClick = {
                        scope.launch {
                            val bugreport = loadingDialog.withLoading {
                                withContext(Dispatchers.IO) { getBugreportFile(context) }
                            }
                            val uri: Uri = FileProvider.getUriForFile(
                                context,
                                "${BuildConfig.APPLICATION_ID}.fileprovider",
                                bugreport
                            )
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                putExtra(Intent.EXTRA_STREAM, uri)
                                setDataAndType(uri, "application/gzip")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, logSavedMessage))
                            viewModel.dismissDialog()
                        }
                    },
                    startAction = {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 12.dp),
                            tint = colorScheme.primary
                        )
                    }
                )
            }

            TextButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                text = stringResource(id = android.R.string.cancel),
                onClick = { viewModel.dismissDialog() }
            )
        }
    }
}

@Composable
fun ResetSUPathDialogMiuix(viewModel: SettingsViewModel) {
    val context = LocalContext.current
    var suPath by remember { mutableStateOf(Natives.suPath()) }
    val isPathValid = suPath.startsWith("/") && suPath.trim().length > 1

    WindowDialog(
        show = true,
        title = stringResource(R.string.setting_reset_su_path),
        onDismissRequest = { viewModel.dismissDialog() }
    ) {
        TextField(
            value = suPath,
            onValueChange = { suPath = it },
            label = stringResource(R.string.setting_reset_su_new_path),
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                stringResource(android.R.string.cancel),
                onClick = { viewModel.dismissDialog() },
                modifier = Modifier.weight(1f),
            )

            Spacer(Modifier.width(20.dp))

            TextButton(
                stringResource(android.R.string.ok),
                onClick = {
                    viewModel.resetSuPath(suPath) { success ->
                        Toast.makeText(
                            context,
                            if (success) R.string.success else R.string.failure,
                            Toast.LENGTH_SHORT
                        ).show()
                        viewModel.dismissDialog()
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
                enabled = isPathValid
            )
        }
    }
}

@Composable
fun PageScaleDialogMiuix(viewModel: ThemeViewModel) {
    val currentPageScale = pageScale
    var text by remember { mutableStateOf((currentPageScale * 100).toInt().toString()) }

    WindowDialog(
        show = true,
        title = stringResource(R.string.settings_page_scale),
        summary = "80% - 110%",
        onDismissRequest = { viewModel.dismissPageScaleDialog() }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            TextField(
                modifier = Modifier.padding(bottom = 16.dp),
                value = text,
                maxLines = 1,
                trailingIcon = {
                    Text(
                        text = "%",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = colorScheme.onSurfaceVariantActions,
                    )
                },
                onValueChange = { newValue ->
                    if (newValue.isEmpty()) {
                        text = ""
                    } else if (newValue.all { it.isDigit() }) {
                        text = newValue
                    }
                },
            )
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(
                    text = stringResource(android.R.string.cancel),
                    onClick = { viewModel.dismissPageScaleDialog() },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(20.dp))
                TextButton(
                    text = stringResource(android.R.string.ok),
                    onClick = {
                        val parsed = text.toIntOrNull()
                        val clamped = parsed?.coerceIn(80, 110) ?: (currentPageScale * 100).toInt()
                        viewModel.setPageScale(clamped / 100f)
                        viewModel.dismissPageScaleDialog()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    }
}
