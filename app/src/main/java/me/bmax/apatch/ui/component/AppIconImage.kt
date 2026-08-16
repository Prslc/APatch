package me.bmax.apatch.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import me.bmax.apatch.ui.LocalUiMode
import me.bmax.apatch.ui.UiMode
import me.bmax.apatch.util.ui.AppIconCache
import top.yukonga.miuix.kmp.theme.MiuixTheme

private data class IconKey(val uid: Int, val packageName: String)

@Composable
fun AppIconImage(
    modifier: Modifier = Modifier,
    uid: Int,
    packageName: String,
    sourceDir: String,
    icon: Int,
    label: String,
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val targetSizePx = with(density) { 48.dp.roundToPx() }

    val iconKey = IconKey(uid, packageName)
    val cachedBitmap = remember(iconKey) {
        AppIconCache.getFromCache(packageName, uid, sourceDir)
    }

    Box(modifier = modifier) {
        var appBitmap by remember(iconKey) { mutableStateOf(cachedBitmap) }

        if (cachedBitmap == null) {
            LaunchedEffect(iconKey) {
                appBitmap = AppIconCache.loadIcon(context, packageName, uid, sourceDir, icon, targetSizePx)
            }
        }

        if (appBitmap == null) {
            PlaceHolderBox(Modifier.fillMaxSize())
        } else {
            Image(
                bitmap = appBitmap!!.asImageBitmap(),
                contentDescription = label,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun PlaceHolderBox(modifier: Modifier = Modifier) {
    val color = when (LocalUiMode.current) {
        UiMode.Miuix -> MiuixTheme.colorScheme.secondaryContainer
        UiMode.Material -> MaterialTheme.colorScheme.surfaceVariant
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color)
    )
}
