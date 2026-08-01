package me.bmax.apatch.ui.page.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.bmax.apatch.R
import me.bmax.apatch.ui.theme.LocalPageScale

@Composable
fun PageScaleDialogMaterial(viewModel: SettingsViewModel) {
    val pageScale = LocalPageScale.current
    var text by remember { mutableStateOf((pageScale * 100).toInt().toString()) }

    AlertDialog(
        onDismissRequest = { viewModel.dismissDialog() },
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
                val scale = (text.toIntOrNull()?.coerceIn(80, 110) ?: (pageScale * 100).toInt()) / 100f
                viewModel.setPageScale(scale)
                viewModel.dismissDialog()
            }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.dismissDialog() }) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}
