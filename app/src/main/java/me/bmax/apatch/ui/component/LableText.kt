package me.bmax.apatch.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import me.bmax.apatch.ui.LocalUiMode
import me.bmax.apatch.ui.UiMode
import me.bmax.apatch.ui.component.material.LabelTextMaterial
import me.bmax.apatch.ui.component.miuix.LabelTextMiuix

@Composable
fun LabelText(
    label: String,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = contentColorFor(containerColor)
) {
    when(LocalUiMode.current) {
        UiMode.Miuix -> LabelTextMiuix(
            label = label,
            containerColor = containerColor,
            contentColor = contentColor
        )
        UiMode.Material -> LabelTextMaterial(
            label = label,
            containerColor = containerColor,
            contentColor = contentColor
        )
    }
}