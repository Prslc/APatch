package me.bmax.apatch.ui.component

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.bmax.apatch.ui.LocalUiMode
import me.bmax.apatch.ui.UiMode
import me.bmax.apatch.ui.component.material.ModuleInstallDialogMaterial
import me.bmax.apatch.ui.component.miuix.ModuleInstallDialogMiuix
import me.bmax.apatch.util.InstallPreview
import me.bmax.apatch.util.ModuleParser

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
        when (LocalUiMode.current) {
            UiMode.Miuix -> ModuleInstallDialogMiuix(
                data = data,
                moduleIcon = moduleIcon,
                onDismiss = onDismiss,
                onConfirm = { onConfirm(uri) }
            )
            UiMode.Material -> ModuleInstallDialogMaterial(
                data = data,
                moduleIcon = moduleIcon,
                onDismiss = onDismiss,
                onConfirm = { onConfirm(uri) }
            )
        }
    }
}
