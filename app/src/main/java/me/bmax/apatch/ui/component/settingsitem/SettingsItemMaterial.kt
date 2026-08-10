package me.bmax.apatch.ui.component.settingsitem

import me.bmax.apatch.ui.component.material.DropDownMenuWidget
import me.bmax.apatch.ui.component.material.NavigationItemWidget
import me.bmax.apatch.ui.component.material.SwitchWidget

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun ArrowItemMaterial(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    contentDescription: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    NavigationItemWidget(
        modifier = modifier,
        icon = icon,
        title = title,
        description = summary ?: "",
        enabled = enabled,
        onClick = onClick
    )
}

@Composable
fun SwitchItemMaterial(
    title: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    contentDescription: String? = null,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    SwitchWidget(
        modifier = modifier,
        icon = icon,
        title = title,
        description = summary,
        enabled = enabled,
        checked = checked,
        onCheckedChange = onCheckedChange
    )
}

@Composable
fun DropdownItemMaterial(
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
    DropDownMenuWidget(
        modifier = modifier,
        icon = icon,
        title = title,
        description = summary,
        enabled = enabled,
        choice = selectedIndex,
        data = items,
        onChoiceChange = onSelectedIndexChange
    )
}
