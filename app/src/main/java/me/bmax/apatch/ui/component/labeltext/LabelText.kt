package me.bmax.apatch.ui.component.labeltext

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import me.bmax.apatch.ui.LocalUiMode
import me.bmax.apatch.ui.UiMode

@Composable
fun LabelText(
    label: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = contentColorFor(containerColor)
) {
    when(LocalUiMode.current) {
        UiMode.Miuix -> LabelTextMiuix(
            label = label,
            modifier = modifier,
            containerColor = containerColor,
            contentColor = contentColor
        )
        UiMode.Material -> LabelTextMaterial(
            label = label,
            modifier = modifier,
            containerColor = containerColor,
            contentColor = contentColor
        )
    }
}
