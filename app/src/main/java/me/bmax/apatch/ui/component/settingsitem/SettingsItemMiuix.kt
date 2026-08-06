package me.bmax.apatch.ui.component.settingsitem

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

@Composable
fun ArrowItemMiuix(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    contentDescription: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    ArrowPreference(
        title = title,
        modifier = modifier,
        summary = summary,
        enabled = enabled,
        onClick = onClick,
        startAction = icon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = contentDescription,
                    modifier = Modifier.padding(end = 6.dp),
                    tint = colorScheme.onBackground
                )
            }
        }
    )
}

@Composable
fun SwitchItemMiuix(
    title: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    contentDescription: String? = null,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    SwitchPreference(
        title = title,
        modifier = modifier,
        summary = summary,
        checked = checked,
        enabled = enabled,
        onCheckedChange = onCheckedChange,
        startAction = icon?.let {
            {
                Icon(
                    imageVector = it,
                    modifier = Modifier.padding(end = 6.dp),
                    contentDescription = contentDescription,
                    tint = colorScheme.onBackground
                )
            }
        }
    )
}

@Composable
fun DropdownItemMiuix(
    title: String,
    items: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    contentDescription: String? = null,
    enabled: Boolean = true,
    onSelectedIndexChange: (Int) -> Unit,
) {
    WindowDropdownPreference(
        title = title,
        modifier = modifier,
        summary = summary,
        items = items,
        selectedIndex = selectedIndex,
        enabled = enabled,
        onSelectedIndexChange = onSelectedIndexChange,
        startAction = icon?.let {
            {
                Icon(
                    imageVector = it,
                    modifier = Modifier.padding(end = 6.dp),
                    contentDescription = contentDescription,
                    tint = colorScheme.onBackground
                )
            }
        }
    )
}