// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
// This file includes code derived from https://github.com/wxxsfxyzm/InstallerX-Revived
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
// Modified: Adapted for APatch Material3 widgets
package me.bmax.apatch.ui.component.material

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * A setting pkg that navigates to a secondary page, built upon BaseWidget.
 * It includes an icon, title, description, and a trailing arrow.
 *
 * @param icon The leading icon for the pkg.
 * @param title The main title text of the pkg.
 * @param description The supporting description text.
 * @param onClick The callback to be invoked when this pkg is clicked.
 */
@Composable
fun NavigationItemWidget(
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconPlaceholder: Boolean = true,
    title: String,
    description: String = "",
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    BaseWidget(
        modifier = modifier,
        icon = icon,
        iconPlaceholder = iconPlaceholder,
        title = title,
        description = description,
        enabled = enabled,
        onClick = onClick
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null
        )
    }
}
