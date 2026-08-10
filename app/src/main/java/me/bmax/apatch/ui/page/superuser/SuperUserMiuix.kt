package me.bmax.apatch.ui.page.superuser

import androidx.compose.animation.AnimatedVisibility
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.bmax.apatch.R
import me.bmax.apatch.data.AppInfo
import me.bmax.apatch.data.AppRepository
import me.bmax.apatch.ui.component.AppIconImage
import me.bmax.apatch.ui.component.labeltext.LabelText
import me.bmax.apatch.ui.component.searchbar.AppSearchBar
import me.bmax.apatch.ui.theme.blurEffect
import me.bmax.apatch.ui.theme.getAppBarColor
import me.bmax.apatch.ui.theme.rememberBlurBackdrop
import me.bmax.apatch.ui.theme.withBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Sort
import top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

@Composable
fun SuperUserScreenMiuix(
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
    isCurrentPage: Boolean = true,
    viewModel: SuperUserViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val appList by viewModel.filteredApps.collectAsStateWithLifecycle()

    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()

    if (isCurrentPage) {
        LaunchedEffect(Unit) {
            if (AppRepository.apps.value.isEmpty()) {
                viewModel.fetchAppList()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
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
                if (appList.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .withBackdrop(backdrop)
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
    backdrop: LayerBackdrop?,
    scrollBehavior: ScrollBehavior
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TopAppBar(
        modifier = Modifier.blurEffect(backdrop),
        color = backdrop.getAppBarColor(),
        title = stringResource(R.string.su_title),
        actions = {
            val entries = listOf(
                DropdownEntry(
                    items = listOf(
                        DropdownItem(
                            text = stringResource(R.string.su_show_system_apps),
                            selected = uiState.showSystemApps,
                            onClick = { viewModel.toggleSystemApps() }
                        )
                    )
                ),
                DropdownEntry(
                    items = SortBy.entries.map { sortBy ->
                        val label = when (sortBy) {
                            SortBy.NAME -> stringResource(R.string.su_sort_name)
                            SortBy.PACKAGE_NAME -> stringResource(R.string.su_sort_package)
                            SortBy.INSTALL_TIME -> stringResource(R.string.su_sort_install_time)
                        }
                        DropdownItem(
                            text = label,
                            selected = uiState.sortBy == sortBy,
                            onClick = { viewModel.updateSort(sortBy) }
                        )
                    }
                )
            )

            OverlayIconDropdownMenu(entries = entries, collapseOnSelection = true) {
                Icon(
                    imageVector = MiuixIcons.Sort,
                    contentDescription = stringResource(R.string.apm_sort)
                )
            }
        },
        scrollBehavior = scrollBehavior
    ) {
        AppSearchBar(
            query = uiState.search,
            onQueryChange = { viewModel.updateSearch(it) },
            placeholder = stringResource(R.string.search_apps),
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
                            LabelText(
                                label = stringResource(R.string.su_pkg_excluded_label),
                                modifier = Modifier.padding(top = 4.dp),
                                containerColor = colorScheme.tertiaryContainer,
                                contentColor = colorScheme.onTertiaryContainer
                            )
                        }
                        if (isAllowed) {
                            LabelText(
                                label = "UID: ${app.config.profile.uid}",
                                modifier = Modifier.padding(top = 4.dp),
                                containerColor = colorScheme.tertiaryContainer,
                                contentColor = colorScheme.onTertiaryContainer
                            )
                            val scontext = app.config.profile.scontext.ifEmpty {
                                stringResource(R.string.su_selinux_via_hook)
                            }
                            LabelText(
                                label = scontext,
                                modifier = Modifier.padding(top = 4.dp),
                                containerColor = colorScheme.secondaryContainer,
                                contentColor = colorScheme.onSecondaryContainer
                            )
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