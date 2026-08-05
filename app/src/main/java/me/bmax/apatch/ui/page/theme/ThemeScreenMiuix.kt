package me.bmax.apatch.ui.page.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Style
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.materialkolor.PaletteStyle
import me.bmax.apatch.R
import me.bmax.apatch.ui.UiMode
import me.bmax.apatch.ui.component.settingsitem.DropdownItem
import me.bmax.apatch.ui.component.settingsitem.SwitchItem
import me.bmax.apatch.ui.component.sliderpreference.SliderPreference
import me.bmax.apatch.ui.theme.blurEffect
import me.bmax.apatch.ui.theme.getAppBarColor
import me.bmax.apatch.ui.theme.rememberBlurBackdrop
import me.bmax.apatch.ui.theme.supportsSpec2025
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back

@Composable
fun ThemeScreenMiuix(
    state: ThemeUiState,
    actions: ThemeScreenActions,
    viewModel: ThemeViewModel,
) {
    val backdrop = rememberBlurBackdrop(state.blurEnabled)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            SmallTopAppBar(
                modifier = Modifier.blurEffect(backdrop),
                color = backdrop.getAppBarColor(),
                title = stringResource(R.string.settings_theme),
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(MiuixIcons.Back, contentDescription = null)
                    }
                },
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 16.dp,
            )
        ) {
            // UI Mode
            item {
                SmallTitle(stringResource(R.string.settings_ui_mode))
                Card(modifier = Modifier.padding(horizontal = 16.dp)) {
                    DropdownItem(
                        title = stringResource(R.string.settings_ui_mode),
                        summary = stringResource(R.string.settings_ui_mode_summary),
                        items = UiMode.entries.map { it.name },
                        selectedIndex = if (state.uiMode == UiMode.Material.value) 1 else 0,
                        icon = Icons.Rounded.Dashboard,
                        onSelectedIndexChange = { index ->
                            actions.onSetUiMode(if (index == 0) UiMode.Miuix.value else UiMode.Material.value)
                        }
                    )
                }
            }

            // Theme + Key Color + Palette Style
            item {
                SmallTitle(stringResource(R.string.settings_theme))
                Card(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SwitchItem(
                        title = stringResource(R.string.settings_blur_enabled),
                        summary = stringResource(R.string.settings_blur_enabled_summary),
                        icon = Icons.Filled.BlurOn,
                        checked = state.blurEnabled,
                        onCheckedChange = actions.onSetBlur,
                    )
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
                        selectedIndex = state.themeMode,
                        icon = Icons.Rounded.Palette,
                        onSelectedIndexChange = { actions.onSetThemeMode(it) }
                    )
                    AnimatedVisibility(visible = state.themeMode in 3..5) {
                        Column {
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
                                selectedIndex = state.keyColorIndex,
                                icon = Icons.Rounded.Colorize,
                                onSelectedIndexChange = { actions.onSetKeyColor(it) }
                            )
                            DropdownItem(
                                title = stringResource(R.string.settings_palette_style),
                                summary = stringResource(R.string.settings_palette_style_summary),
                                items = PaletteStyle.entries.map { it.name },
                                selectedIndex = state.paletteStyleIndex,
                                icon = Icons.Rounded.Style,
                                onSelectedIndexChange = { actions.onSetPaletteStyle(it) }
                            )
                            val supportsSpec2025 =
                                PaletteStyle.entries.getOrElse(state.paletteStyleIndex) { PaletteStyle.TonalSpot }
                                    .supportsSpec2025
                            DropdownItem(
                                title = stringResource(R.string.settings_color_spec),
                                summary = if (supportsSpec2025) {
                                    stringResource(R.string.settings_color_spec_summary)
                                } else {
                                    stringResource(R.string.settings_color_spec_only_2021)
                                },
                                items = listOf(
                                    stringResource(R.string.settings_color_spec_2021),
                                    stringResource(R.string.settings_color_spec_2025),
                                ),
                                selectedIndex = if (supportsSpec2025) state.colorSpecIndex else 0,
                                icon = Icons.Rounded.Style,
                                enabled = supportsSpec2025,
                                onSelectedIndexChange = { actions.onSetColorSpec(it) }
                            )
                        }
                    }
                }
            }

            // Page Scale
            item {
                SmallTitle(stringResource(R.string.settings_page_scale))
                Card(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SliderPreference(
                        title = stringResource(R.string.settings_page_scale),
                        summary = stringResource(R.string.settings_page_scale_summary),
                        icon = Icons.Filled.ZoomIn,
                        value = pageScale,
                        onValueChangeFinished = actions.onSetPageScale,
                        valueRange = 0.8f..1.1f,
                        keyPoints = listOf(0.8f, 0.9f, 1.0f, 1.1f),
                        onClick = { viewModel.showPageScaleDialog() },
                    )
                }
            }
        }
    }
}
