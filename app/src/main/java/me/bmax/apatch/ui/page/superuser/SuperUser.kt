package me.bmax.apatch.ui.page.superuser

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.bmax.apatch.R
import me.bmax.apatch.data.AppInfo
import me.bmax.apatch.data.AppRepository
import me.bmax.apatch.ui.component.AppIconImage
import me.bmax.apatch.ui.component.DropdownItem
import me.bmax.apatch.ui.component.LoadingIndicator
import me.bmax.apatch.ui.theme.blurEffect
import me.bmax.apatch.ui.theme.getAppBarColor
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.window.WindowListPopup

@Composable
fun SuperUserScreen(bottomPadding: Dp) {
    val viewModel = viewModel<SuperUserViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val appList by viewModel.filteredApps.collectAsStateWithLifecycle()

    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberLayerBackdrop()

    LaunchedEffect(Unit) {
        if (AppRepository.apps.value.isEmpty()) {
            viewModel.fetchAppList()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            SuperTopBar(
                viewModel = viewModel,
                backdrop = backdrop,
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            PullToRefresh(
                modifier = Modifier.fillMaxSize(),
                isRefreshing = uiState.isRefreshing,
                refreshTexts = listOf(
                    stringResource(R.string.refresh_pulling),
                    stringResource(R.string.refresh_release),
                    stringResource(R.string.refresh_refresh),
                    stringResource(R.string.refresh_complete)
                ),
                onRefresh = { viewModel.fetchAppList() },
                contentPadding = innerPadding
            ) {
                if (uiState.isRefreshing && appList.isEmpty()) {
                    LoadingIndicator()
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(backdrop.let { Modifier.layerBackdrop(it) })
                            .overScrollVertical()
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                        contentPadding = PaddingValues(
                            top = innerPadding.calculateTopPadding() + 8.dp,
                            bottom = bottomPadding + 16.dp,
                            start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                            end = innerPadding.calculateEndPadding(LocalLayoutDirection.current)
                        )
                    ) {
                        items(
                            items = appList,
                            key = { it.packageName + it.uid }
                        ) { app ->
                            AppItem(
                                app = app,
                                onToggleSu = { granted -> viewModel.toggleSu(app, granted) },
                                onToggleExclude = { excluded ->
                                    viewModel.toggleExclude(app, excluded)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SuperTopBar(
    viewModel: SuperUserViewModel,
    backdrop: LayerBackdrop,
    scrollBehavior: ScrollBehavior
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val appListItemsCount = 2

    var expanded by remember { mutableStateOf(false) }

    TopAppBar(
        modifier = Modifier.blurEffect(backdrop),
        color = backdrop.getAppBarColor(),
        title = stringResource(R.string.su_title),
        actions = {
            val showDropdown = remember { mutableStateOf(false) }

            IconButton(onClick = { showDropdown.value = true }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(id = R.string.settings)
                )

                WindowListPopup(
                    show = showDropdown.value,
                    onDismissRequest = { showDropdown.value = false }
                ) {
                    ListPopupColumn {
                        DropdownItem(
                            text = stringResource(R.string.su_refresh),
                            optionSize = appListItemsCount,
                            index = 0,
                            onSelectedIndexChange = {
                                viewModel.fetchAppList()
                                showDropdown.value = false
                            }
                        )

                        DropdownItem(
                            text = if (uiState.showSystemApps) {
                                stringResource(R.string.su_hide_system_apps)
                            } else {
                                stringResource(R.string.su_show_system_apps)
                            },
                            optionSize = appListItemsCount,
                            index = 1,
                            onSelectedIndexChange = {
                                viewModel.toggleSystemApps()
                                showDropdown.value = false
                            }
                        )
                    }
                }
            }
        },
        scrollBehavior = scrollBehavior
    ) {
        SearchBar(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp),
            inputField = {
                InputField(
                    query = uiState.search,
                    onQueryChange = { viewModel.updateSearch(it) },
                    onSearch = { expanded = false },
                    expanded = expanded,
                    onExpandedChange = {
                        expanded = it
                        if (!it) viewModel.updateSearch("")
                    }
                )
            },
            expanded = expanded,
            onExpandedChange = { expanded = it },
            content = { }
        )
    }
}

@Composable
private fun AppItem(
    app: AppInfo,
    onToggleSu: (Boolean) -> Unit,
    onToggleExclude: (Boolean) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    val isAllowed = app.config.allow != 0
    val isExcluded = app.config.exclude == 1

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (!isAllowed) {
                        isExpanded = !isExpanded
                    } else {
                        onToggleSu(false)
                    }
                }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIconImage(
                    packageInfo = app.packageInfo,
                    label = app.label,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(4.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.label,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = app.packageName,
                        style = MiuixTheme.textStyles.body2,
                        color = colorScheme.onSurfaceVariantActions,
                        maxLines = 1
                    )

                    Row {
                        if (isExcluded) {
                            LabelText(label = stringResource(R.string.su_pkg_excluded_label))
                        }
                        if (isAllowed) {
                            LabelText(label = "UID: ${app.config.profile.uid}")
                            val scontext = app.config.profile.scontext.ifEmpty {
                                stringResource(R.string.su_selinux_via_hook)
                            }
                            LabelText(label = scontext)
                        }
                    }
                }

                Switch(
                    checked = app.config.allow != 0,
                    onCheckedChange = { onToggleSu(it) }
                )
            }

            AnimatedVisibility(visible = isExpanded && !isAllowed) {
                Column {
                    Box(modifier = Modifier.size(8.dp))
                    SwitchPreference(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(R.string.su_pkg_excluded_setting_title),
                        summary = stringResource(R.string.su_pkg_excluded_setting_summary),
                        checked = isExcluded,
                        onCheckedChange = { onToggleExclude(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun LabelText(label: String) {
    Box(
        modifier = Modifier
            .padding(top = 4.dp, end = 4.dp)
            .background(
                color = colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(4.dp)
            )
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            text = label,
            color = colorScheme.onTertiaryContainer,
            fontSize = 9.sp,
            fontWeight = FontWeight(750),
            maxLines = 1,
            softWrap = false
        )
    }
}