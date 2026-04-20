package me.bmax.apatch.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

@Composable
fun ArrowItem(
    title: String,
    summary: String,
    icon: ImageVector,
    contentDescription: String? = null,
    onClick: () -> Unit
) {
    ArrowPreference(
        title = title,
        summary = summary,
        onClick = onClick,
        startAction = {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.padding(end = 6.dp),
                tint = colorScheme.onBackground
            )
        }
    )
}

@Composable
fun SwitchItem(
    title: String,
    summary: String,
    icon: ImageVector,
    checked: Boolean,
    enabled: Boolean = true,
    contentDescription: String? = null,
    onCheckedChange: (Boolean) -> Unit,
) {
    SwitchPreference(
        title = title,
        summary = summary,
        checked = checked,
        enabled = enabled,
        onCheckedChange = onCheckedChange,
        startAction = {
            Icon(
                imageVector = icon,
                modifier = Modifier.padding(end = 6.dp),
                contentDescription = contentDescription,
                tint = colorScheme.onBackground
            )
        }
    )
}

@Composable
fun DropdownItem(
    title: String,
    items: List<String>,
    selectedIndex: Int,
    icon: ImageVector,
    summary: String? = null,
    contentDescription: String? = null,
    onSelectedIndexChange: (Int) -> Unit,
) {
    WindowDropdownPreference(
        title = title,
        summary = summary,
        items = items,
        selectedIndex = selectedIndex,
        onSelectedIndexChange = onSelectedIndexChange,
        startAction = {
            Row(
                verticalAlignment = CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    modifier = Modifier.padding(end = 6.dp),
                    contentDescription = contentDescription,
                    tint = colorScheme.onBackground
                )
            }
        }
    )
}