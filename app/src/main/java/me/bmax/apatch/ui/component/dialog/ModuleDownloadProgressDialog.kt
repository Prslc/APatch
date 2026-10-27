package me.bmax.apatch.ui.component.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import me.bmax.apatch.R
import me.bmax.apatch.ui.LocalUiMode
import me.bmax.apatch.ui.UiMode
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
fun ModuleDownloadProgressDialog(
    title: String,
    progress: Int,
    onCancel: () -> Unit
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> ModuleDownloadProgressDialogMiuix(title, progress, onCancel)
        UiMode.Material -> ModuleDownloadProgressDialogMaterial(title, progress, onCancel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModuleDownloadProgressDialogMaterial(
    title: String,
    progress: Int,
    onCancel: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.width(310.dp),
            shape = RoundedCornerShape(30.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(R.string.online_downloading, title, progress),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onCancel) {
                        Text(stringResource(android.R.string.cancel))
                    }
                }
            }
        }
    }
}

@Composable
private fun ModuleDownloadProgressDialogMiuix(
    title: String,
    progress: Int,
    onCancel: () -> Unit
) {
    WindowDialog(
        show = true,
        title = stringResource(R.string.online_downloading, title, progress),
        onDismissRequest = onCancel
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            MiuixTextButton(
                text = stringResource(android.R.string.cancel),
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
