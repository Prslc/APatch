package me.bmax.apatch.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.bmax.apatch.ui.LocalUiMode
import me.bmax.apatch.ui.UiMode
import top.yukonga.miuix.kmp.basic.Surface as MiuixSurface
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ChipLabel(text: String) {
    when (LocalUiMode.current) {
        UiMode.Material -> ChipLabelMaterial(text)
        UiMode.Miuix -> ChipLabelMiuix(text)
    }
}

@Composable
private fun ChipLabelMaterial(text: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.tertiary
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 12.sp,
                lineHeight = 14.sp
            ),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
            color = MaterialTheme.colorScheme.onTertiary,
            maxLines = 1
        )
    }
}

@Composable
private fun ChipLabelMiuix(text: String) {
    MiuixSurface(
        shape = RoundedCornerShape(4.dp),
        color = MiuixTheme.colorScheme.tertiaryContainer
    ) {
        MiuixText(
            text = text,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
            color = MiuixTheme.colorScheme.onTertiaryContainer,
            maxLines = 1
        )
    }
}
