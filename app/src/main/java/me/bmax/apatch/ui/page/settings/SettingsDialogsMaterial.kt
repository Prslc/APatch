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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.BuildConfig
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.dialog.rememberLoadingDialog
import me.bmax.apatch.ui.component.material.BaseWidget
import me.bmax.apatch.ui.component.material.SegmentedColumn
import me.bmax.apatch.ui.page.theme.ThemeViewModel
import me.bmax.apatch.ui.page.theme.pageScale
import me.bmax.apatch.util.getBugreportFile
import me.bmax.apatch.util.outputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun PageScaleDialogMaterial(viewModel: ThemeViewModel) {
    val currentPageScale = pageScale
    var text by remember { mutableStateOf((currentPageScale * 100).toInt().toString()) }

    AlertDialog(
        onDismissRequest = { viewModel.dismissPageScaleDialog() },
        title = { Text(stringResource(R.string.settings_page_scale)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_page_scale_summary))
                Spacer(Modifier.height(12.dp))
                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = text,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty()) text = ""
                        else if (newValue.all { it.isDigit() }) text = newValue
                    },
                    singleLine = true,
                    trailingIcon = {
                        Text("%", style = MaterialTheme.typography.bodyMedium)
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val scale = (text.toIntOrNull()?.coerceIn(80, 110) ?: (currentPageScale * 100).toInt()) / 100f
                viewModel.setPageScale(scale)
                viewModel.dismissPageScaleDialog()
            }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.dismissPageScaleDialog() }) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetSUPathDialogMaterial(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel
) {
    val context = LocalContext.current
    val suPath = uiState.suPath
    val isPathValid = suPath.startsWith("/") && suPath.trim().length > 1

    BasicAlertDialog(
        onDismissRequest = { viewModel.dismissDialog() }
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = AlertDialogDefaults.shape,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.setting_reset_su_path),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.setting_reset_su_path_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = suPath,
                    onValueChange = { viewModel.setSuPath(it) },
                    label = { Text(stringResource(R.string.setting_reset_su_new_path)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { viewModel.dismissDialog() }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
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
                        enabled = isPathValid
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogDialogMaterial(viewModel: SettingsViewModel) {
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

    BasicAlertDialog(
        onDismissRequest = { viewModel.dismissDialog() }
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = AlertDialogDefaults.shape,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 16.dp),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.send_log),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(Modifier.height(24.dp))
                SegmentedColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                ) {
                    item {
                        BaseWidget(
                            icon = Icons.Filled.Save,
                            title = stringResource(R.string.save_log),
                            description = stringResource(R.string.send_log_summary),
                            onClick = {
                                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH_mm")
                                val current = LocalDateTime.now().format(formatter)
                                exportBugreportLauncher.launch("APatch_bugreport_${current}.tar.gz")
                            }
                        )
                    }
                    item {
                        BaseWidget(
                            icon = Icons.Filled.Share,
                            title = stringResource(R.string.send_log),
                            description = stringResource(R.string.send_log_summary),
                            onClick = {
                                scope.launch {
                                    val bugreport = loadingDialog.withLoading {
                                        withContext(Dispatchers.IO) { getBugreportFile(context) }
                                    }
                                    val uri = androidx.core.content.FileProvider.getUriForFile(
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
                            }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                TextButton(
                    onClick = { viewModel.dismissDialog() },
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        }
    }
}
