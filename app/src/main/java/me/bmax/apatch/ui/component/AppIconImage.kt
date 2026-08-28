package me.bmax.apatch.ui.component

import android.content.pm.PackageInfo
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import me.bmax.apatch.ui.LocalUiMode
import me.bmax.apatch.ui.UiMode
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AppIconImage(
    packageInfo: PackageInfo,
    label: String,
    modifier: Modifier = Modifier,
) {
    val placeholderColor = when (LocalUiMode.current) {
        UiMode.Miuix -> MiuixTheme.colorScheme.secondaryContainer
        UiMode.Material -> MaterialTheme.colorScheme.surfaceVariant
    }

    AsyncImage(
        model = packageInfo,
        contentDescription = label,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp)),
        placeholder = ColorPainter(placeholderColor),
        error = ColorPainter(placeholderColor),
        contentScale = ContentScale.Fit,
    )
}
