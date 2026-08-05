package me.bmax.apatch.ui.page.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.materialkolor.PaletteStyle
import me.bmax.apatch.R
import me.bmax.apatch.ui.UiMode
import me.bmax.apatch.ui.component.material.BaseWidget
import me.bmax.apatch.ui.component.material.SegmentedColumn
import me.bmax.apatch.ui.component.settingsitem.DropdownItem
import me.bmax.apatch.ui.component.settingsitem.SwitchItem
import me.bmax.apatch.ui.component.sliderpreference.SliderPreference
import me.bmax.apatch.ui.theme.getMaterial3AppBarColor
import me.bmax.apatch.ui.theme.material3BlurEffect
import me.bmax.apatch.ui.theme.rememberMaterial3BlurBackdrop
import me.bmax.apatch.ui.theme.supportsSpec2025

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeScreenMaterial(
    state: ThemeUiState,
    actions: ThemeScreenActions,
    viewModel: ThemeViewModel,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val backdrop = rememberMaterial3BlurBackdrop(state.blurEnabled)

    var showThemeDialog by remember { mutableStateOf(false) }
    var showPaletteDialog by remember { mutableStateOf(false) }

    if (showThemeDialog) {
        ThemeModeDialogMaterial(
            state.themeMode,
            { showThemeDialog = false },
            actions.onSetThemeMode
        )
    }
    if (showPaletteDialog) {
        PaletteStyleDialogMaterial(
            state.paletteStyleIndex,
            { showPaletteDialog = false },
            actions.onSetPaletteStyle
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            Column(modifier = Modifier.material3BlurEffect(backdrop)) {
                LargeFlexibleTopAppBar(
                    title = {
                        Text(
                            stringResource(R.string.settings_theme),
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = actions.onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                null
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = backdrop.getMaterial3AppBarColor(),
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        scrolledContainerColor = backdrop.getMaterial3AppBarColor()
                    )
                )
            }
        }
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
        ) {
            SegmentedColumn(title = stringResource(R.string.settings_ui_mode)) {
                item {
                    val isMiuix = state.uiMode == UiMode.Miuix.value
                    BaseWidget(
                        iconPlaceholder = false,
                        title = "MIUIX",
                        description = stringResource(R.string.settings_ui_mode_summary),
                        selected = isMiuix,
                        onClick = { if (!isMiuix) actions.onSetUiMode(UiMode.Miuix.value) },
                        trailingContent = {
                            if (isMiuix) Icon(
                                imageVector = Icons.Rounded.RadioButtonChecked,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            else Icon(
                                imageVector = Icons.Outlined.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
                item {
                    val isMaterial = state.uiMode == UiMode.Material.value
                    BaseWidget(
                        iconPlaceholder = false,
                        title = "Material Design 3",
                        description = stringResource(R.string.settings_ui_mode_summary),
                        selected = isMaterial,
                        onClick = { if (!isMaterial) actions.onSetUiMode(UiMode.Material.value) },
                        trailingContent = {
                            if (isMaterial) Icon(
                                imageVector = Icons.Rounded.RadioButtonChecked,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            else Icon(
                                imageVector = Icons.Outlined.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }
            SegmentedColumn(title = stringResource(R.string.settings_theme)) {
                item {
                    SwitchItem(
                        icon = Icons.Filled.BlurOn,
                        title = stringResource(R.string.settings_blur_enabled),
                        summary = stringResource(R.string.settings_blur_enabled_summary),
                        checked = state.blurEnabled,
                        onCheckedChange = actions.onSetBlur,
                    )
                }
                item {
                    val currentMode = when (state.themeMode) {
                        1 -> stringResource(R.string.settings_theme_mode_light)
                        2 -> stringResource(R.string.settings_theme_mode_dark)
                        3 -> stringResource(R.string.settings_theme_mode_monet_system)
                        4 -> stringResource(R.string.settings_theme_mode_monet_light)
                        5 -> stringResource(R.string.settings_theme_mode_monet_dark)
                        else -> stringResource(R.string.settings_theme_mode_system)
                    }
                    BaseWidget(
                        icon = Icons.Filled.DarkMode,
                        title = stringResource(R.string.settings_theme),
                        description = currentMode,
                        onClick = { showThemeDialog = true }
                    )
                }
                if (state.themeMode in 3..5) {
                    item {
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
                    }
                    item {
                        BaseWidget(
                            icon = Icons.Rounded.Style,
                            title = stringResource(R.string.settings_palette_style),
                            description = PaletteStyle.entries.getOrElse(state.paletteStyleIndex) { PaletteStyle.TonalSpot }.name,
                            onClick = { showPaletteDialog = true }
                        )
                    }
                    item {
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

            SegmentedColumn(title = stringResource(R.string.settings_page_scale)) {
                item {
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

@Composable
private fun ThemeModeDialogMaterial(current: Int, onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_theme)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                listOf(
                    0 to R.string.settings_theme_mode_system,
                    1 to R.string.settings_theme_mode_light,
                    2 to R.string.settings_theme_mode_dark,
                    3 to R.string.settings_theme_mode_monet_system,
                    4 to R.string.settings_theme_mode_monet_light,
                    5 to R.string.settings_theme_mode_monet_dark,
                ).forEach { (index, label) ->
                    Row(Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(index); onDismiss() }
                        .padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = current == index,
                            onClick = { onSelect(index); onDismiss() })
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(label))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } }
    )
}

@Composable
private fun PaletteStyleDialogMaterial(
    current: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_palette_style)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                PaletteStyle.entries.forEachIndexed { index, style ->
                    Row(Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(index); onDismiss() }
                        .padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = current == index,
                            onClick = { onSelect(index); onDismiss() })
                        Spacer(Modifier.width(8.dp))
                        Text(style.name)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } }
    )
}

