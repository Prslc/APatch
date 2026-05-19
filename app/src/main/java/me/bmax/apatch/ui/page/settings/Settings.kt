package me.bmax.apatch.ui.page.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Commit
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.ArrowItem
import me.bmax.apatch.ui.component.DropdownItem
import me.bmax.apatch.ui.component.SwitchItem
import me.bmax.apatch.ui.navigation.LocalNavigator
import me.bmax.apatch.ui.theme.blurEffect
import me.bmax.apatch.ui.theme.getAppBarColor
import me.bmax.apatch.ui.theme.rememberBlurBackdrop
import me.bmax.apatch.ui.theme.withBackdrop
import me.bmax.apatch.util.formatSize
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.utils.overScrollVertical

@Composable
fun SettingScreen(
    modifier: Modifier,
    bottomPadding: Dp,
    isCurrentPage: Boolean = true,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState = viewModel.uiState
    val context = LocalContext.current

    val navigator = LocalNavigator.current
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = if (isCurrentPage) rememberBlurBackdrop() else null

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                modifier = Modifier.blurEffect(backdrop),
                color = backdrop.getAppBarColor(),
                title = stringResource(R.string.settings),
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        SettingsDialogOverlay(uiState, viewModel)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .withBackdrop(backdrop)
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

                    // Blur Effects
                    SwitchItem(
                        title = stringResource(R.string.settings_blur_enabled),
                        summary = stringResource(R.string.settings_blur_enabled_summary),
                        icon = Icons.Filled.BlurOn,
                        checked = uiState.blurEnabled,
                        onCheckedChange = { viewModel.setBlurEnabled(it) }
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
                            onClick = { viewModel.showDialog(SettingDialogType.RESET_SU_PATH) }
                        )
                    }

                    // save log
                    ArrowItem(
                        title = stringResource(R.string.send_log),
                        summary = stringResource(R.string.send_log_summary),
                        icon = Icons.Filled.BugReport,
                        onClick = { viewModel.showDialog(SettingDialogType.SEND_LOG) }
                    )

                    // clean cache
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
