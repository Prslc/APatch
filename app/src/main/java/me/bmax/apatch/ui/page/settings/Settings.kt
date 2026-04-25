package me.bmax.apatch.ui.page.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Commit
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.BuildConfig
import me.bmax.apatch.Natives
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.ArrowItem
import me.bmax.apatch.ui.component.DropdownItem
import me.bmax.apatch.ui.component.SwitchItem
import me.bmax.apatch.ui.component.rememberLoadingDialog
import me.bmax.apatch.ui.navigation.LocalNavigator
import me.bmax.apatch.ui.theme.blurEffect
import me.bmax.apatch.ui.theme.getAppBarColor
import me.bmax.apatch.ui.theme.rememberBlurBackdrop
import me.bmax.apatch.util.clearAppCache
import me.bmax.apatch.util.formatSize
import me.bmax.apatch.util.getBugreportFile
import me.bmax.apatch.util.outputStream
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.window.WindowDialog
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun SettingScreen(bottomPadding: Dp) {
    val viewModel: SettingsViewModel = viewModel()
    val uiState = viewModel.uiState
    val context = LocalContext.current

    val navigator = LocalNavigator.current
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop(true)

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.blurEffect(backdrop),
                color = backdrop.getAppBarColor(),
                title = stringResource(R.string.settings),
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->

        ResetSUPathDialog()
        ClearDialog()
        LogDialog()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    remember(backdrop) {
                        backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier
                    }
                )
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = paddingValues.calculateTopPadding() + 16.dp,
                end = 16.dp,
                bottom = paddingValues.calculateBottomPadding() + 16.dp
            )
        ) {
            item {
                Card {
                    // Global mount
                    if (uiState.isKpatchReady && uiState.isApatchReady) {
                        SwitchItem(
                            title = stringResource(R.string.settings_global_namespace_mode),
                            summary = stringResource(R.string.settings_global_namespace_mode_summary),
                            icon = Icons.Filled.Engineering,
                            checked = uiState.isGlobalNamespaceEnabled,
                            onCheckedChange = { viewModel.toggleGlobalNamespace(it) }
                        )
                    }

                    // WebView Debug
                    if (uiState.isApatchReady) {
                        SwitchItem(
                            title = stringResource(R.string.enable_web_debugging),
                            summary = stringResource(R.string.enable_web_debugging_summary),
                            icon = Icons.Filled.DeveloperMode,
                            checked = uiState.enableWebDebugging,
                            onCheckedChange = { viewModel.setWebDebugging(it) }
                        )
                    }

                    // Check Update
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

                    // Theme System
                    DropdownItem(
                        title = stringResource(R.string.settings_theme),
                        summary = stringResource(R.string.settings_theme_summary),
                        items = listOf(
                            stringResource(R.string.settings_theme_mode_system),
                            stringResource(R.string.settings_theme_mode_light),
                            stringResource(R.string.settings_theme_mode_dark),
                            stringResource(R.string.settings_theme_mode_monet_system),
                            stringResource(R.string.settings_theme_mode_monet_light),
                            stringResource(R.string.settings_theme_mode_monet_dark),
                        ),
                        selectedIndex = uiState.themeMode,
                        icon = Icons.Rounded.Palette,
                        onSelectedIndexChange = { viewModel.setThemeMode(it) }
                    )
                    // key color
                    AnimatedVisibility(
                        visible = uiState.themeMode in 3..5
                    ) {
                        val keyColorIndex = viewModel.colorValues.indexOf(uiState.keyColor).coerceAtLeast(0)
                        DropdownItem(
                            title = stringResource(R.string.settings_key_color),
                            summary = stringResource(R.string.settings_key_color_summary),
                            items = listOf(
                                stringResource(R.string.settings_key_color_default),
                                stringResource(R.string.color_red),
                                stringResource(R.string.color_green),
                                stringResource(R.string.color_blue),
                                stringResource(R.string.color_purple),
                                stringResource(R.string.color_orange),
                                stringResource(R.string.color_teal),
                                stringResource(R.string.color_pink),
                                stringResource(R.string.color_brown),
                            ),
                            selectedIndex = keyColorIndex,
                            icon = Icons.Rounded.Colorize,
                            onSelectedIndexChange = { viewModel.setKeyColor(it) }
                        )
                    }

                    // language
                    val languagesValues = stringArrayResource(R.array.languages_values)
                    DropdownItem(
                        title = stringResource(R.string.settings_app_language),
                        summary = stringResource(R.string.settings_app_language_summary),
                        items = stringArrayResource(R.array.languages).toList(),
                        selectedIndex = uiState.currentLanguageIndex,
                        icon = Icons.Filled.Translate,
                        onSelectedIndexChange = { index ->
                            val tag = if (index == 0) "" else languagesValues[index]
                            viewModel.updateLanguage(context, tag, index)
                        }
                    )

                    // reset su path
                    if (uiState.isKpatchReady) {
                        ArrowItem(
                            title = stringResource(R.string.setting_reset_su_path),
                            summary = stringResource(R.string.setting_reset_su_path_summary),
                            icon = Icons.Filled.Commit,
                            contentDescription = stringResource(R.string.setting_reset_su_path),
                            onClick = {
                                viewModel.setShowResetSuDialog(true)
                            }
                        )
                    }

                    // save log
                    ArrowItem(
                        title = stringResource(R.string.send_log),
                        summary = stringResource(R.string.send_log_summary),
                        icon = Icons.Filled.BugReport,
                        onClick = { viewModel.setShowLogDialog(true) }
                    )

                    // clean cache
                    ArrowItem(
                        title = stringResource(R.string.settings_clean_cache),
                        summary = formatSize(uiState.cacheSize),
                        icon = Icons.Filled.CleaningServices,
                        onClick = {
                            if (uiState.cacheSize > 0L) {
                                viewModel.setShowClearDialog(true)
                            } else {
                                Toast.makeText(context, R.string.no_cache_to_clear, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    // about
                    ArrowItem(
                        title = stringResource(R.string.home_more_menu_about),
                        summary = stringResource(R.string.about_summary),
                        icon = Icons.Filled.Info,
                        onClick = { navigator.navigateToAbout() }
                    )
                }
                Spacer(Modifier.height(bottomPadding))
            }
        }
    }
}

@Composable
fun LogDialog() {
    val viewModel: SettingsViewModel = viewModel()
    val uiState = viewModel.uiState

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val loadingDialog = rememberLoadingDialog()
    val logSavedMessage = stringResource(R.string.log_saved)

    val exportBugreportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/gzip")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                loadingDialog.show()
                runCatching {
                    uri.outputStream().use { output ->
                        getBugreportFile(context).inputStream().use { it.copyTo(output) }
                    }
                }.onSuccess {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, logSavedMessage, Toast.LENGTH_LONG).show()
                    }
                }
                loadingDialog.hide()
            }
        }
    }

    WindowDialog(
        show = uiState.showLogDialog,
        title = stringResource(R.string.send_log),
        onDismissRequest = { viewModel.setShowLogDialog(false) }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // save log
            LogActionItem(
                icon = Icons.Default.Save,
                label = stringResource(R.string.save_log),
                onClick = {
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH_mm")
                    val current = LocalDateTime.now().format(formatter)
                    exportBugreportLauncher.launch("APatch_bugreport_${current}.tar.gz")
                    viewModel.setShowLogDialog(false)
                }
            )

            // share log
            LogActionItem(
                icon = Icons.Default.Share,
                label = stringResource(R.string.send_log),
                onClick = {
                    scope.launch {
                        val bugreport = loadingDialog.withLoading {
                            withContext(Dispatchers.IO) { getBugreportFile(context) }
                        }
                        val uri: Uri = FileProvider.getUriForFile(
                            context,
                            "${BuildConfig.APPLICATION_ID}.fileprovider",
                            bugreport
                        )
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            putExtra(Intent.EXTRA_STREAM, uri)
                            setDataAndType(uri, "application/gzip")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, logSavedMessage))
                        viewModel.setShowLogDialog(false)
                    }
                }
            )
        }
    }
}

@Composable
private fun LogActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(30.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(label)
    }
}

@Composable
fun ResetSUPathDialog() {
    val viewModel: SettingsViewModel = viewModel()
    val uiState = viewModel.uiState
    val context = LocalContext.current

    var suPath by remember(uiState.showResetSuPathDialog) {
        mutableStateOf(Natives.suPath())
    }

    val isPathValid = suPath.startsWith("/") && suPath.trim().length > 1

    WindowDialog(
        show = uiState.showResetSuPathDialog,
        title = stringResource(R.string.setting_reset_su_path),
        onDismissRequest = { viewModel.setShowResetSuDialog(false) }
    ) {
        TextField(
            value = suPath,
            onValueChange = { suPath = it },
            label = stringResource(R.string.setting_reset_su_new_path),
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                stringResource(android.R.string.cancel),
                onClick = { viewModel.setShowResetSuDialog(false) },
                modifier = Modifier.weight(1f),
            )

            Spacer(Modifier.width(20.dp))

            TextButton(
                stringResource(android.R.string.ok),
                onClick = {
                    viewModel.resetSuPath(suPath) { success ->
                        Toast.makeText(
                            context,
                            if (success) R.string.success else R.string.failure,
                            Toast.LENGTH_SHORT
                        ).show()
                        viewModel.setShowResetSuDialog(false)
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
                enabled = isPathValid
            )
        }
    }
}

@Composable
fun ClearDialog() {
    val viewModel: SettingsViewModel = viewModel()
    val uiState = viewModel.uiState

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val loading = rememberLoadingDialog()

    WindowDialog(
        show = uiState.showClearDialog,
        title = stringResource(R.string.clear_cache_title),
        summary = stringResource(R.string.clear_cache_message, formatSize(uiState.cacheSize)),
        onDismissRequest = { viewModel.setShowClearDialog(false) }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                stringResource(android.R.string.cancel),
                onClick = { viewModel.setShowClearDialog(false) },
                modifier = Modifier.weight(1f),
            )

            Spacer(Modifier.width(20.dp))

            TextButton(
                stringResource(android.R.string.ok),
                onClick = {
                    viewModel.setShowClearDialog(false)
                    scope.launch {
                        loading.withLoading {
                            clearAppCache(context)
                            viewModel.refreshCacheSize()
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }
}
