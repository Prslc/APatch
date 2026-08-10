package me.bmax.apatch.ui.page.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Commit
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.UninstallType
import me.bmax.apatch.ui.component.dialog.rememberConfirmDialog
import me.bmax.apatch.ui.component.material.BaseWidget
import me.bmax.apatch.ui.component.material.SegmentedColumn
import me.bmax.apatch.ui.component.settingsitem.ArrowItem
import me.bmax.apatch.ui.component.settingsitem.SwitchItem
import me.bmax.apatch.ui.navigation.LocalNavigator
import me.bmax.apatch.ui.page.patch.PatchMode
import me.bmax.apatch.ui.theme.LocalEnableBlur
import me.bmax.apatch.ui.theme.getMaterial3AppBarColor
import me.bmax.apatch.ui.theme.material3BlurEffect
import me.bmax.apatch.ui.theme.rememberMaterial3BlurBackdrop
import me.bmax.apatch.ui.theme.withBackdrop
import me.bmax.apatch.util.formatSize
import me.bmax.apatch.util.softReboot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreenMaterial(
    modifier: Modifier,
    bottomPadding: Dp,
    isCurrentPage: Boolean = true,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val navigator = LocalNavigator.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val backdrop = if (isCurrentPage) rememberMaterial3BlurBackdrop(LocalEnableBlur.current) else null

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showUninstallDialog by remember { mutableStateOf(false) }

    if (showLanguageDialog) {
        LanguageDialogMaterial(
            current = uiState.currentLanguageIndex,
            onDismiss = { showLanguageDialog = false },
            viewModel = viewModel,
        )
    }

    if (showUninstallDialog) {
        UninstallDialogMaterial(
            onDismiss = { showUninstallDialog = false },
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                modifier = Modifier.material3BlurEffect(backdrop),
                title = {
                    Text(
                        text = stringResource(R.string.settings),
                        modifier = Modifier.padding(start = 12.dp)
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backdrop.getMaterial3AppBarColor(),
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    scrolledContainerColor = backdrop.getMaterial3AppBarColor()
                )
            )
        }
    ) { paddingValues ->
        SettingsDialogOverlay(uiState, viewModel)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .withBackdrop(backdrop),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding() + 16.dp
            )
        ) {
            item {
                SegmentedColumn {
                    // Jailbreak mode
                    if (uiState.isJailbreak) {
                        item {
                            val softRebootDialog = rememberConfirmDialog(
                                onConfirm = { softReboot() }
                            )
                            val jailbreakLoaded = stringResource(R.string.settings_jailbreak_loaded)
                            val jailbreakSoftRebootMessage =
                                stringResource(R.string.settings_jailbreak_soft_reboot_message)
                            val jailbreakSoftReboot =
                                stringResource(R.string.settings_jailbreak_soft_reboot)
                            ArrowItem(
                                title = stringResource(R.string.settings_jailbreak_mode),
                                summary = stringResource(R.string.settings_jailbreak_mode_summary),
                                icon = Icons.Rounded.LockOpen,
                                onClick = {
                                    softRebootDialog.showConfirm(
                                        title = jailbreakLoaded,
                                        content = jailbreakSoftRebootMessage,
                                        confirm = jailbreakSoftReboot
                                    )
                                }
                            )
                        }
                    }

                    // Global mount
                    if (uiState.isKpatchReady && uiState.isApatchReady) {
                        item {
                            SwitchItem(
                                title = stringResource(R.string.settings_global_namespace_mode),
                                summary = stringResource(R.string.settings_global_namespace_mode_summary),
                                icon = Icons.Filled.Engineering,
                                checked = uiState.isGlobalNamespaceEnabled,
                                onCheckedChange = { viewModel.toggleGlobalNamespace(it) }
                            )
                        }
                    }

                    // Legacy sucompat (path_probe) support
                    if (uiState.isKpatchReady && uiState.isApatchReady) {
                        item {
                            SwitchItem(
                                icon = Icons.Filled.Terminal,
                                title = stringResource(R.string.settings_sucompat),
                                summary = stringResource(R.string.settings_sucompat_summary),
                                checked = uiState.sucompatEnabled,
                                onCheckedChange = { viewModel.toggleSucompat(it) }
                            )
                        }
                    }

                    // WebView Debug
                    if (uiState.isApatchReady) {
                        item {
                            SwitchItem(
                                title = stringResource(R.string.enable_web_debugging),
                                summary = stringResource(R.string.enable_web_debugging_summary),
                                icon = Icons.Filled.DeveloperMode,
                                checked = uiState.enableWebDebugging,
                                onCheckedChange = { viewModel.setWebDebugging(it) }
                            )
                        }
                    }
                    // Check Update
                    item {
                        SwitchItem(
                            title = stringResource(R.string.settings_check_update),
                            summary = stringResource(R.string.settings_check_update_summary),
                            icon = Icons.Filled.Update,
                            checked = uiState.checkUpdate,
                            contentDescription = stringResource(R.string.settings_check_update_summary),
                            onCheckedChange = { isChecked ->
                                viewModel.setCheckUpdate(isChecked)
                            }
                        )
                    }

                    // language
                    item {
                        val languageName = stringArrayResource(R.array.languages)[uiState.currentLanguageIndex]
                        ArrowItem(
                            title = stringResource(R.string.settings_app_language),
                            summary = languageName,
                            icon = Icons.Filled.Translate,
                            onClick = { showLanguageDialog = true }
                        )
                    }
                    // Theme
                    item {
                        ArrowItem(
                            title = stringResource(R.string.settings_theme),
                            summary = stringResource(R.string.settings_theme_summary),
                            icon = Icons.Rounded.Palette,
                            onClick = { navigator.navigateToTheme() }
                        )
                    }

                    // reset su path
                    if (uiState.isKpatchReady) {
                        item {
                            ArrowItem(
                                title = stringResource(R.string.setting_reset_su_path),
                                summary = stringResource(R.string.setting_reset_su_path_summary),
                                icon = Icons.Filled.Commit,
                                contentDescription = stringResource(R.string.setting_reset_su_path),
                                onClick = { viewModel.showDialog(SettingDialogType.RESET_SU_PATH) }
                            )
                        }
                    }

                    // save log
                    item {
                        ArrowItem(
                            title = stringResource(R.string.send_log),
                            summary = stringResource(R.string.send_log_summary),
                            icon = Icons.Filled.BugReport,
                            onClick = { viewModel.showDialog(SettingDialogType.SEND_LOG) }
                        )
                    }

                    // clean cache
                    item {
                        ArrowItem(
                            title = stringResource(R.string.settings_clean_cache),
                            summary = formatSize(uiState.cacheSize),
                            icon = Icons.Filled.CleaningServices,
                            onClick = {
                                if (uiState.cacheSize > 0L) {
                                    viewModel.showDialog(SettingDialogType.CLEAR_CACHE)
                                } else {
                                    Toast.makeText(context, R.string.no_cache_to_clear, Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }

                    // uninstall
                    item {
                        ArrowItem(
                            title = stringResource(R.string.home_dialog_uninstall_title),
                            summary = stringResource(R.string.mode_uninstall_method_all_summary),
                            icon = Icons.Rounded.DeleteForever,
                            onClick = { showUninstallDialog = true }
                        )
                    }

                    // about
                    item {
                        ArrowItem(
                            title = stringResource(R.string.home_more_menu_about),
                            summary = stringResource(R.string.about_summary),
                            icon = Icons.Filled.Info,
                            onClick = { navigator.navigateToAbout() }
                        )
                    }
                }
                Spacer(Modifier.height(bottomPadding))
            }
        }
    }
}

@Composable
private fun LanguageDialogMaterial(
    current: Int,
    onDismiss: () -> Unit,
    viewModel: SettingsViewModel,
) {
    val context = LocalContext.current
    val languages = stringArrayResource(R.array.languages)
    val languagesValues = stringArrayResource(R.array.languages_values)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_app_language)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                languages.forEachIndexed { index, language ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                val tag = if (index == 0) "" else languagesValues[index]
                                viewModel.updateLanguage(context, tag, index)
                                onDismiss()
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = current == index,
                            onClick = {
                                val tag = if (index == 0) "" else languagesValues[index]
                                viewModel.updateLanguage(context, tag, index)
                                onDismiss()
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(language)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UninstallDialogMaterial(onDismiss: () -> Unit) {
    val navigator = LocalNavigator.current
    val runAction = { type: UninstallType ->
        onDismiss()
        when (type) {
            UninstallType.TEMPORARY -> APApplication.uninstallApatch()
            UninstallType.RESTORE_STOCK_IMAGE -> navigator.navigateToPatches(PatchMode.UNPATCH)
            UninstallType.PERMANENT -> {
                APApplication.uninstallApatch()
                navigator.navigateToPatches(PatchMode.UNPATCH)
            }
            else -> {}
        }
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = AlertDialogDefaults.shape,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.home_dialog_uninstall_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(Modifier.height(24.dp))

                SegmentedColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                ) {
                    UninstallType.entries.filter { it != UninstallType.NONE }.forEach { type ->
                        item {
                            BaseWidget(
                                icon = type.icon,
                                title = stringResource(type.titleRes),
                                description = stringResource(type.summaryRes),
                                titleStyle = if (type == UninstallType.PERMANENT) {
                                    MaterialTheme.typography.titleMedium.copy(
                                        color = MaterialTheme.colorScheme.error
                                    )
                                } else {
                                    MaterialTheme.typography.titleMedium
                                },
                                onClick = { runAction(type) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Text(stringResource(id = android.R.string.cancel))
                }
            }
        }
    }
}