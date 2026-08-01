package me.bmax.apatch.ui.component.miuix

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun WarningCard(
    message: String,
    containerColor: Color = MiuixTheme.colorScheme.error,
    contentColor: Color = MiuixTheme.colorScheme.onError,
    onClick: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null
) {
    val cardColors = CardDefaults.defaultColors(
        color = containerColor,
        contentColor = contentColor
    )

    Card(colors = cardColors) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(onClick?.let { Modifier.clickable { it() } } ?: Modifier)
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().align(Alignment.CenterStart).padding(end = 40.dp)
            ) {
                if (icon != null) {
                    icon()
                } else {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = message,
                    style = MiuixTheme.textStyles.body2,
                    color = cardColors.contentColor,
                    modifier = Modifier.wrapContentHeight(Alignment.CenterVertically)
                )
            }


            if (onClose != null) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(android.R.string.cancel),
                    tint = contentColor,
                    modifier = Modifier.clickable {
                        onClose()
                    }.size(18.dp).align(Alignment.TopEnd)
                )
            }
        }
    }
}

@Preview
@Composable
private fun WarningCardPreview() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        WarningCard(message = "Warning message")
        WarningCard(message = "Warning message", onClose = {})
        WarningCard(
            message = "Warning message",
            containerColor = MiuixTheme.colorScheme.error,
            contentColor = MiuixTheme.colorScheme.onError
        )
        WarningCard(
            message = "Warning message",
            containerColor = MiuixTheme.colorScheme.outline,
            contentColor = MiuixTheme.colorScheme.onSurface
        )
    }
}