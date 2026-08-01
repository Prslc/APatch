package me.bmax.apatch.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import me.bmax.apatch.ui.LocalUiMode
import me.bmax.apatch.ui.UiMode
import me.bmax.apatch.ui.component.material.SliderPreferenceMaterial
import me.bmax.apatch.ui.component.miuix.SliderPreferenceMiuix

@Composable
fun SliderPreference(
    title: String,
    value: Float,
    onValueChangeFinished: (Float) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    keyPoints: List<Float> = emptyList(),
    magnetThreshold: Float = 0f,
    valueTextFormatter: (Float) -> String = { "${(it * 100).toInt()}%" },
    onClick: (() -> Unit)? = null,
    holdDownState: Boolean = false,
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> SliderPreferenceMiuix(
            title = title,
            value = value,
            onValueChangeFinished = onValueChangeFinished,
            modifier = modifier,
            summary = summary,
            icon = icon,
            valueRange = valueRange,
            keyPoints = keyPoints,
            magnetThreshold = magnetThreshold,
            valueTextFormatter = valueTextFormatter,
            onClick = onClick,
            holdDownState = holdDownState,
        )
        UiMode.Material -> SliderPreferenceMaterial(
            title = title,
            value = value,
            onValueChangeFinished = onValueChangeFinished,
            modifier = modifier,
            summary = summary,
            icon = icon,
            valueRange = valueRange,
            keyPoints = keyPoints,
            magnetThreshold = magnetThreshold,
            valueTextFormatter = valueTextFormatter,
            onClick = onClick,
        )
    }
}
