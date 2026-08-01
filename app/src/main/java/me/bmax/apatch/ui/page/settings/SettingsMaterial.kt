package me.bmax.apatch.ui.page.settings

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Commit
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.bmax.apatch.R
import me.bmax.apatch.ui.UiMode
import me.bmax.apatch.ui.component.ArrowItem
import me.bmax.apatch.ui.component.DropdownItem
import me.bmax.apatch.ui.component.SwitchItem
import me.bmax.apatch.ui.component.material.SegmentedColumn
import me.bmax.apatch.ui.navigation.LocalNavigator
import me.bmax.apatch.ui.component.SliderPreference
import me.bmax.apatch.ui.theme.LocalEnableBlur
import me.bmax.apatch.ui.theme.LocalPageScale
import me.bmax.apatch.ui.theme.getMaterial3AppBarColor
import me.bmax.apatch.ui.theme.material3BlurEffect
import me.bmax.apatch.ui.theme.rememberMaterial3BlurBackdrop
import me.bmax.apatch.ui.theme.withBackdrop
import me.bmax.apatch.util.formatSize

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

                    // Blur Effects (API 33+ only)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        item {
                            SwitchItem(
                                title = stringResource(R.string.settings_blur_enabled),
                                summary = stringResource(R.string.settings_blur_enabled_summary),
                                icon = Icons.Filled.BlurOn,
                                checked = uiState.blurEnabled,
                                onCheckedChange = { viewModel.setBlurEnabled(it) }
                            )
                        }
                    }
                    // Page Scale
                    item {
                        val pageScale = LocalPageScale.current
                        SliderPreference(
                            title = stringResource(R.string.settings_page_scale),
                            summary = stringResource(R.string.settings_page_scale_summary),
                            icon = Icons.Filled.ZoomIn,
                            value = pageScale,
                            onValueChangeFinished = { viewModel.setPageScale(it) },
                            valueRange = 0.8f..1.1f,
                            keyPoints = listOf(0.8f, 0.9f, 1.0f, 1.1f),
                            onClick = { viewModel.showDialog(SettingDialogType.PAGE_SCALE) },
                        )
                    }
                    // UI Style
                    item {
                        DropdownItem(
                            title = stringResource(R.string.settings_ui_mode),
                            summary = stringResource(R.string.settings_ui_mode_summary),
                            items = UiMode.entries.map { it.name },
                            selectedIndex = if (uiState.uiMode == UiMode.Material.value) 1 else 0,
                            icon = Icons.Filled.Dashboard,
                            onSelectedIndexChange = { index ->
                                viewModel.setUiMode(if (index == 0) UiMode.Miuix.value else UiMode.Material.value)
                            }
                        )
                    }
                    // Theme System
                    item {
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
                    }
                    // key color
                    item(
                        animatedVisibility = uiState.themeMode in 3..5
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
                    item {
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