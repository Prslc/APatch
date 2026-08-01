package me.bmax.apatch.ui.component.miuix

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

@Composable
fun IconTextButton(
    iconRes: ImageVector,
    textRes: Int,
    showText: Boolean? = null,
    onClick: () -> Unit
) {
    val finalShowText = showText ?: true

    IconButton(
        onClick = onClick,
        minHeight = 35.dp,
        minWidth = 35.dp,
        backgroundColor = colorScheme.secondaryContainer
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = if (finalShowText) 10.dp else 4.dp)
        ) {
            Icon(
                imageVector = iconRes,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            if (finalShowText) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(id = textRes),
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                )
            }
        }
    }
}



@Composable
fun ModuleStateIndicator(
    icon: ImageVector,
    color: Color = colorScheme.outline
) {
    Image(
        modifier = Modifier.requiredSize(150.dp),
        imageVector = icon,
        contentDescription = null,
        alpha = 0.1f,
        colorFilter = ColorFilter.tint(color)
    )
}