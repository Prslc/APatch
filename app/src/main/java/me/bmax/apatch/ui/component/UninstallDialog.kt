package me.bmax.apatch.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Adb
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.RemoveModerator
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.navigation.LocalNavigator
import me.bmax.apatch.ui.page.patch.PatchMode
import me.bmax.apatch.ui.page.patch.PatchesViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
fun UninstallDialog(showDialog: MutableState<Boolean>) {
    val navigator = LocalNavigator.current
    val runAction = { type: UninstallType ->
        showDialog.value = false

        when (type) {
            UninstallType.TEMPORARY -> {
                APApplication.uninstallApatch()
            }

            UninstallType.RESTORE_STOCK_IMAGE -> {
                navigator.navigateToPatches(PatchMode.UNPATCH)
            }

            UninstallType.PERMANENT -> {
                APApplication.uninstallApatch()
                navigator.navigateToPatches(PatchMode.UNPATCH)
            }

            else -> {}
        }
    }

    WindowDialog(
        show = showDialog.value,
        insideMargin = DpSize(0.dp, 0.dp),
        onDismissRequest = { showDialog.value = false },
        content = {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 12.dp),
                text = stringResource(R.string.home_dialog_uninstall_title),
                fontSize = MiuixTheme.textStyles.title4.fontSize,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = MiuixTheme.colorScheme.onSurface
            )

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Card {
                    Column {
                        UninstallType.entries.filter { it != UninstallType.NONE }.forEach { type ->
                            ArrowPreference(
                                title = stringResource(type.titleRes),
                                summary = stringResource(type.summaryRes),
                                onClick = {
                                    runAction(type)
                                },
                                startAction = {
                                    Icon(
                                        imageVector = type.icon,
                                        contentDescription = null,
                                        modifier = Modifier.padding(end = 12.dp),
                                        tint = MiuixTheme.colorScheme.primary
                                    )
                                }
                            )
                        }
                    }
                }

                TextButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 24.dp),
                    text = stringResource(id = android.R.string.cancel),
                    onClick = { showDialog.value = false }
                )
            }
        }
    )
}

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