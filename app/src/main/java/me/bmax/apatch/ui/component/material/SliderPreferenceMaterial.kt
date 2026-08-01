package me.bmax.apatch.ui.component.material

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun SliderPreferenceMaterial(
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
) {
    var sliderValue by remember(value) { mutableFloatStateOf(value) }
    val shape = LocalSegmentedItemShape.current

    val displayKeyPoints = keyPoints.filter {
        it != valueRange.start && it != valueRange.endInclusive
    }

    val animatedValue by animateFloatAsState(
        targetValue = sliderValue,
        label = "PageScale Slider"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        color = MaterialTheme.colorScheme.surfaceBright,
        shape = shape
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (summary != null) {
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = valueTextFormatter(sliderValue),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            KeyPointSlider(
                modifier = Modifier.fillMaxWidth(),
                value = animatedValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { onValueChangeFinished(sliderValue) },
                valueRange = valueRange,
                keyPoints = displayKeyPoints.takeIf { it.isNotEmpty() },
                showKeyPoints = displayKeyPoints.isNotEmpty(),
                magnetThreshold = magnetThreshold
            )
        }
    }
}
