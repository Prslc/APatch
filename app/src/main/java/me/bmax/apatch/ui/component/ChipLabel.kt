package me.bmax.apatch.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ChipLabel(text: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MiuixTheme.colorScheme.tertiaryContainer
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
            color = MiuixTheme.colorScheme.onTertiaryContainer,
            maxLines = 1,
        )
    }
}
