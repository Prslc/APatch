package me.bmax.apatch.ui.component

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.bmax.apatch.R
import me.bmax.apatch.util.InstallPreview
import me.bmax.apatch.util.ModuleParser
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
fun ModuleInstallDialog(
    uri: Uri,
    onDismiss: () -> Unit,
    onConfirm: (Uri) -> Unit
) {
    val context = LocalContext.current
    val loadingDialog = rememberLoadingDialog()

    var previewData by remember { mutableStateOf<InstallPreview?>(null) }
    var moduleIcon by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(uri) {
        val preview = loadingDialog.withLoading {
            withContext(Dispatchers.IO) {
                ModuleParser.getModuleInstallPreview(context, uri)
            }
        }

        moduleIcon = withContext(Dispatchers.Default) {
            preview.icon?.let { bytes ->
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        }
        previewData = preview
    }

    previewData?.let { data ->
        WindowDialog(
            show = true,
            title = stringResource(R.string.apm),
            onDismissRequest = onDismiss
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Module icon
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (moduleIcon != null) {
                        Image(
                            bitmap = moduleIcon!!.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = data.name,
                    style = MiuixTheme.textStyles.title4,
                    color = colorScheme.onSurface,
                    fontWeight = FontWeight(550),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = data.id,
                    style = MiuixTheme.textStyles.body1,
                    color = colorScheme.onSurfaceVariantSummary,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))

                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    InfoItem(
                        label = stringResource(R.string.module_info_author),
                        value = data.author
                    )
                    InfoItem(
                        label = stringResource(R.string.module_info_version),
                        value = data.version
                    )
                    InfoItem(
                        label = stringResource(R.string.module_info_version_code),
                        value = data.versionCode.toString()
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = data.description,
                    style = MiuixTheme.textStyles.body2,
                    color = colorScheme.onSurface,
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                )

                data.errorMessage?.let { error ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = error, color = colorScheme.error,
                        style = MiuixTheme.textStyles.body1,
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(28.dp))
            }

            // Bottom button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    text = stringResource(android.R.string.cancel),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )

                Spacer(Modifier.width(20.dp))

                TextButton(
                    text = stringResource(R.string.apm_install),
                    onClick = { onConfirm(uri) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary()
                )
            }
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.body2,
            color = colorScheme.onSurfaceVariantSummary,
        )
        Text(
            text = value ?: "",
            style = MiuixTheme.textStyles.body1,
            color = colorScheme.onSurface,
            textAlign = TextAlign.End
        )
    }
}