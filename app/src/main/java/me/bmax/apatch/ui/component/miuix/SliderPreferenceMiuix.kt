package me.bmax.apatch.ui.component.miuix

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

@Composable
fun SliderPreferenceMiuix(
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
    var sliderValue by remember(value) { mutableFloatStateOf(value) }

    ArrowPreference(
        modifier = modifier,
        title = title,
        summary = summary,
        startAction = icon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = title,
                    modifier = Modifier.padding(end = 6.dp),
                    tint = colorScheme.onBackground
                )
            }
        },
        endActions = {
            Text(
                text = valueTextFormatter(value),
                color = colorScheme.onSurfaceVariantActions
            )
        },
        onClick = onClick,
        holdDownState = holdDownState,
        bottomAction = {
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { onValueChangeFinished(sliderValue) },
                valueRange = valueRange,
                showKeyPoints = keyPoints.isNotEmpty(),
                keyPoints = keyPoints,
                magnetThreshold = magnetThreshold
            )
        }
    )
}
