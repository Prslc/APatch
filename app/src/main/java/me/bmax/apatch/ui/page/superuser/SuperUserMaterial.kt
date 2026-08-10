package me.bmax.apatch.ui.page.superuser

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.bmax.apatch.R
import me.bmax.apatch.data.AppInfo
import me.bmax.apatch.data.AppRepository
import me.bmax.apatch.ui.component.AppIconImage
import me.bmax.apatch.ui.component.labeltext.LabelText
import me.bmax.apatch.ui.component.material.SegmentedColumn
import me.bmax.apatch.ui.component.searchbar.AppSearchBar
import me.bmax.apatch.ui.theme.LocalEnableBlur
import me.bmax.apatch.ui.theme.getMaterial3AppBarColor
import me.bmax.apatch.ui.theme.material3BlurEffect
import me.bmax.apatch.ui.theme.rememberMaterial3BlurBackdrop
import me.bmax.apatch.ui.theme.withBackdrop

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SuperUserScreenMaterial(
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
    isCurrentPage: Boolean = true,
    viewModel: SuperUserViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val appList by viewModel.filteredApps.collectAsStateWithLifecycle()

    val backdrop =
        if (isCurrentPage) rememberMaterial3BlurBackdrop(LocalEnableBlur.current) else null

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val pullRefreshState = rememberPullToRefreshState()
    val listState = rememberLazyListState()

    // Clearing the search returns to the full list, so jump back to the top.
    LaunchedEffect(uiState.search) {
        if (uiState.search.isEmpty()) listState.scrollToItem(0)
    }

    if (isCurrentPage) {
        LaunchedEffect(Unit) {
            if (AppRepository.apps.value.isEmpty()) viewModel.fetchAppList()
        }
    }

    val bgColor = MaterialTheme.colorScheme.surfaceContainer

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = bgColor,
        topBar = {
            Column(
                modifier = Modifier.material3BlurEffect(backdrop)
            ) {
                LargeFlexibleTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.su_title),
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    },
                    actions = {
                        SortDropdown(viewModel)
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = backdrop.getMaterial3AppBarColor(),
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        scrolledContainerColor = backdrop.getMaterial3AppBarColor()
                    )
                )
                AppSearchBar(
                    query = uiState.search,
                    onQueryChange = { viewModel.updateSearch(it) },
                    placeholder = stringResource(R.string.search_apps),
                )
            }
        }
    ) { paddingValues ->
        PullToRefreshBox(
            state = pullRefreshState,
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.fetchAppList() },
            modifier = Modifier.fillMaxSize(),
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    modifier = Modifier
                        .padding(top = paddingValues.calculateTopPadding())
                        .align(Alignment.TopCenter),
                    state = pullRefreshState,
                    isRefreshing = uiState.isRefreshing,
                )
            }
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .withBackdrop(backdrop),
                contentPadding = PaddingValues(
                    top = paddingValues.calculateTopPadding() + 8.dp,
                    bottom = bottomPadding + 16.dp
                )
            ) {

                if (appList.isNotEmpty()) {
                    item {
                        SegmentedColumn {
                            appList.forEach { app ->
                                item(key = app.packageName + app.uid) { shape ->
                                    AppItemMaterial(
                                        app = app,
                                        shape = shape,
                                        onToggleSu = { granted ->
                                            viewModel.toggleSu(app, granted)
                                        },
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
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SortDropdown(
    viewModel: SuperUserViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.AutoMirrored.Filled.Sort,
                stringResource(R.string.apm_sort)
            )
        }
        DropdownMenuPopup(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuGroup(
                shapes = MenuDefaults.groupShape(index = 0, count = 2)
            ) {
                DropdownMenuItem(
                    checked = uiState.showSystemApps,
                    onCheckedChange = {
                        viewModel.toggleSystemApps()
                        expanded = false
                    },
                    text = { Text(stringResource(R.string.su_show_system_apps)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.CheckBoxOutlineBlank,
                            contentDescription = null
                        )
                    },
                    checkedLeadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.CheckBox,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    colors = MenuDefaults.itemColors(),
                    shapes = MenuDefaults.itemShape(index = 0, count = 1)
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(MenuDefaults.HorizontalDividerPadding)
            )
            DropdownMenuGroup(
                shapes = MenuDefaults.groupShape(index = 1, count = 2)
            ) {
                SortBy.entries.forEachIndexed { index, sortBy ->
                    val label = when (sortBy) {
                        SortBy.NAME -> stringResource(R.string.su_sort_name)
                        SortBy.PACKAGE_NAME -> stringResource(R.string.su_sort_package)
                        SortBy.INSTALL_TIME -> stringResource(R.string.su_sort_install_time)
                    }
                    DropdownMenuItem(
                        selected = uiState.sortBy == sortBy,
                        onClick = {
                            viewModel.updateSort(sortBy)
                            expanded = false
                        },
                        text = { Text(label) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.RadioButtonUnchecked,
                                contentDescription = null
                            )
                        },
                        selectedLeadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.RadioButtonChecked,
                                contentDescription = null
                            )
                        },
                        shapes = MenuDefaults.itemShape(
                            index = index,
                            count = SortBy.entries.size
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun AppItemMaterial(
    app: AppInfo,
    shape: Shape,
    onToggleSu: (Boolean) -> Unit,
    onToggleExclude: (Boolean) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val isAllowed = app.config.allow != 0
    val isExcluded = app.config.exclude == 1

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceBright
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { if (!isAllowed) isExpanded = !isExpanded else onToggleSu(false) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIconImage(
                    Modifier
                        .size(44.dp)
                        .padding(4.dp),
                    app.packageInfo,
                    app.label
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        app.label,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        app.packageName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row {
                        if (isExcluded) {
                            LabelText(
                                label = stringResource(R.string.su_pkg_excluded_label),
                                modifier = Modifier.padding(top = 4.dp),
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        if (isAllowed) {
                            LabelText(
                                label = "UID: ${app.config.profile.uid}",
                                modifier = Modifier.padding(top = 4.dp),
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            LabelText(
                                label = app.config.profile.scontext.ifEmpty { stringResource(R.string.su_selinux_via_hook) },
                                modifier = Modifier.padding(top = 4.dp),
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                Switch(
                    checked = isAllowed,
                    onCheckedChange = { onToggleSu(it) },
                    thumbContent = {
                        Icon(
                            imageVector = if (isAllowed) Icons.Filled.Check else Icons.Filled.Close,
                            contentDescription = null,
                            tint = if (isAllowed) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceBright
                            },
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                        )
                    }
                )
            }

            AnimatedVisibility(visible = isExpanded && !isAllowed) {
                Surface(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onToggleExclude(!isExcluded) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.su_pkg_excluded_setting_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                stringResource(R.string.su_pkg_excluded_setting_summary),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isExcluded,
                            onCheckedChange = { onToggleExclude(it) },
                            thumbContent = {
                                Icon(
                                    imageVector = if (isExcluded) Icons.Filled.Check else Icons.Filled.Close,
                                    contentDescription = null,
                                    tint = if (isExcluded) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceBright
                                    },
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}