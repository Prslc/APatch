package me.bmax.apatch.ui.component.settingsitem

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import me.bmax.apatch.ui.LocalUiMode
import me.bmax.apatch.ui.UiMode

@Composable
fun ArrowItem(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    contentDescription: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> ArrowItemMiuix(
            title = title,
            modifier = modifier,
            summary = summary,
            icon = icon,
            contentDescription = contentDescription,
            enabled = enabled,
            onClick = onClick
        )
        UiMode.Material -> ArrowItemMaterial(
            title = title,
            modifier = modifier,
            summary = summary,
            icon = icon,
            contentDescription = contentDescription,
            enabled = enabled,
            onClick = onClick
        )
    }
}

@Composable
fun SwitchItem(
    title: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    contentDescription: String? = null,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> SwitchItemMiuix(
            title = title,
            checked = checked,
            modifier = modifier,
            summary = summary,
            icon = icon,
            contentDescription = contentDescription,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
        UiMode.Material -> SwitchItemMaterial(
            title = title,
            checked = checked,
            modifier = modifier,
            summary = summary,
            icon = icon,
            contentDescription = contentDescription,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun DropdownItem(
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
    when (LocalUiMode.current) {
        UiMode.Miuix -> DropdownItemMiuix(
            title = title,
            items = items,
            selectedIndex = selectedIndex,
            modifier = modifier,
            summary = summary,
            icon = icon,
            contentDescription = contentDescription,
            enabled = enabled,
            onSelectedIndexChange = onSelectedIndexChange
        )
        UiMode.Material -> DropdownItemMaterial(
            title = title,
            items = items,
            selectedIndex = selectedIndex,
            modifier = modifier,
            summary = summary,
            icon = icon,
            contentDescription = contentDescription,
            enabled = enabled,
            onSelectedIndexChange = onSelectedIndexChange
        )
    }
}