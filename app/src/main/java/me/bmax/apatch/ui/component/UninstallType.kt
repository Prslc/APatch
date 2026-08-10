package me.bmax.apatch.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Adb
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.RemoveModerator
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.ui.graphics.vector.ImageVector
import me.bmax.apatch.R

enum class UninstallType(
    val icon: ImageVector,
    val titleRes: Int,
    val summaryRes: Int
) {
    TEMPORARY(
        Icons.Rounded.RemoveModerator,
        R.string.home_dialog_uninstall_ap_only,
        R.string.mode_uninstall_method_ap_only_summary
    ),
    RESTORE_STOCK_IMAGE(
        Icons.Rounded.RestartAlt,
        R.string.home_dialog_restore_image,
        R.string.mode_uninstall_method_restore_summary
    ),
    PERMANENT(
        Icons.Rounded.DeleteForever,
        R.string.home_dialog_uninstall_all,
        R.string.mode_uninstall_method_all_summary
    ),
    NONE(Icons.Rounded.Adb, 0, 0)
}
