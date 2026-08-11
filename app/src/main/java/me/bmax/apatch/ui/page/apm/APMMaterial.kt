package me.bmax.apatch.ui.page.apm

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Patterns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HorizontalSplit
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults

import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.apApp
import me.bmax.apatch.ui.LocalSnackbarHost
import me.bmax.apatch.ui.WebUIActivity
import me.bmax.apatch.ui.component.snackbar.AppSnackbarHostState
import me.bmax.apatch.ui.component.dialog.ConfirmResult
import me.bmax.apatch.ui.component.labeltext.LabelText
import me.bmax.apatch.ui.component.dialog.rememberConfirmDialog
import me.bmax.apatch.ui.component.dialog.rememberLoadingDialog
import me.bmax.apatch.ui.component.searchbar.AppSearchBar
import me.bmax.apatch.ui.navigation.LocalNavigator
import me.bmax.apatch.ui.navigation.MODULE_TYPE
import me.bmax.apatch.ui.navigation.Navigator
import me.bmax.apatch.ui.theme.LocalEnableBlur
import me.bmax.apatch.ui.theme.getMaterial3AppBarColor
import me.bmax.apatch.ui.theme.material3BlurEffect
import me.bmax.apatch.ui.theme.rememberMaterial3BlurBackdrop
import me.bmax.apatch.ui.theme.withBackdrop
import me.bmax.apatch.util.DownloadListener
import me.bmax.apatch.util.Shortcut
import me.bmax.apatch.util.download
import me.bmax.apatch.util.isJailbreakMode
import me.bmax.apatch.util.reboot
import me.bmax.apatch.util.rebootJailbreakAware
import okhttp3.Request
import top.yukonga.miuix.kmp.basic.SnackbarDuration
import top.yukonga.miuix.kmp.blur.LayerBackdrop

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun APModuleScreenMaterial(
    modifier: Modifier,
    bottomPadding: Dp,
    isCurrentPage: Boolean = true,
    viewModel: APModuleViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val snackBarHostState = LocalSnackbarHost.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val backdrop =
        if (isCurrentPage) rememberMaterial3BlurBackdrop(LocalEnableBlur.current) else null

    val state by APApplication.apStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)

    if (state != APApplication.State.ANDROIDPATCH_INSTALLED && state != APApplication.State.ANDROIDPATCH_NEED_UPDATE) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(id = R.string.apm_not_installed),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    val webUILauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            viewModel.fetchModuleList()
        }

    val hasMagisk = uiState.hasMagisk
    val moduleListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    var fabVisible by remember { mutableStateOf(true) }

    // Clearing the search returns to the full list, so jump back to the top.
    LaunchedEffect(uiState.search) {
        if (uiState.search.isEmpty()) moduleListState.scrollToItem(0)
    }

    if (isCurrentPage) {
        LaunchedEffect(uiState.isNeedRefresh) {
            if (uiState.modules.isEmpty() || uiState.isNeedRefresh) {
                viewModel.fetchModuleList()
            }
        }
    }

    LaunchedEffect(moduleListState) {
        var lastIndex = 0
        var lastOffset = 0
        snapshotFlow {
            moduleListState.firstVisibleItemIndex to
                moduleListState.firstVisibleItemScrollOffset
        }.collect { (index, offset) ->
            val scrollingDown = index > lastIndex || (index == lastIndex && offset > lastOffset)
            fabVisible = !scrollingDown
            lastIndex = index
            lastOffset = offset
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            Column(modifier = Modifier.material3BlurEffect(backdrop)) {
                LargeFlexibleTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.apm),
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
                    onQueryChange = { viewModel.onSearchChange(it) },
                    placeholder = stringResource(R.string.search_modules),
                )
            }
        },
        floatingActionButton = {
            if (!hasMagisk) {
                val selectZipLauncher =
                    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                        if (it.resultCode == RESULT_OK) {
                            it.data?.data?.let { uri ->
                                navigator.navigateToInstall(uri, MODULE_TYPE.APM)
                                viewModel.markNeedRefresh()
                            }
                        }
                    }

                AnimatedVisibility(
                    visible = fabVisible,
                    enter = scaleIn(
                        initialScale = 0.6f,
                        animationSpec = spring(dampingRatio = 0.75f, stiffness = 420f)
                    ) + fadeIn(animationSpec = tween(120)),
                    exit = scaleOut(
                        targetScale = 0.9f,
                        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                    ) + fadeOut(
                        animationSpec = tween(durationMillis = 280, easing = LinearOutSlowInEasing)
                    )
                ) {
                    FloatingActionButton(
                        containerColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = bottomPadding + 16.dp),
                        onClick = {
                            val intent =
                                Intent(Intent.ACTION_GET_CONTENT).apply { type = "application/zip" }
                            selectZipLauncher.launch(intent)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        val pullRefreshState = rememberPullToRefreshState()
        PullToRefreshBox(
            state = pullRefreshState,
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.fetchModuleList() },
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
            when {
                hasMagisk -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = paddingValues.calculateTopPadding()),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.apm_magisk_conflict),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                else -> {
                    ModuleListMaterial(
                        navigator = navigator,
                        viewModel = viewModel,
                        state = moduleListState,
                        backdrop = backdrop,
                        topPadding = paddingValues.calculateTopPadding(),
                        bottomPadding = bottomPadding,
                        onInstallModule = {
                            navigator.navigateToInstall(it, MODULE_TYPE.APM)
                        },
                        onClickModule = { id, name, hasWebUi ->
                            if (hasWebUi) {
                                webUILauncher.launch(
                                    Intent(context, WebUIActivity::class.java)
                                        .setData("apatch://webui/$id".toUri())
                                        .putExtra("id", id).putExtra("name", name)
                                )
                            }
                        },
                        context = context,
                        snackBarHost = snackBarHostState
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SortDropdown(
    viewModel: APModuleViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var sortIndex by remember {
        mutableIntStateOf(
            when {
                uiState.sortEnabledFirst -> 1
                uiState.sortActionFirst -> 2
                uiState.sortWebFirst -> 3
                else -> 0
            }
        )
    }
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
                shapes = MenuDefaults.groupShape(index = 0, count = 1)
            ) {
                listOf(
                    R.string.apm_sort_default,
                    R.string.apm_sort_enabled_first,
                    R.string.apm_sort_action_first,
                    R.string.apm_sort_web_first
                ).forEachIndexed { index, res ->
                    DropdownMenuItem(
                        selected = sortIndex == index,
                        onClick = {
                            sortIndex = index
                            when (sortIndex) {
                                0 -> viewModel.resetSort()
                                1 -> viewModel.selectSortEnabledFirst()
                                2 -> viewModel.selectSortActionFirst()
                                3 -> viewModel.selectSortWebFirst()
                            }
                            expanded = false
                        },
                        text = { Text(stringResource(res)) },
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
                            count = 4
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ModuleListMaterial(
    navigator: Navigator,
    viewModel: APModuleViewModel,
    state: LazyListState,
    backdrop: LayerBackdrop?,
    topPadding: Dp,
    bottomPadding: Dp,
    onInstallModule: (Uri) -> Unit,
    onClickModule: (id: String, name: String, hasWebUi: Boolean) -> Unit,
    context: Context,
    snackBarHost: AppSnackbarHostState
) {
    val failedEnable = stringResource(R.string.apm_failed_to_enable)
    val failedDisable = stringResource(R.string.apm_failed_to_disable)
    val failedUninstall = stringResource(R.string.apm_uninstall_failed)
    val failedUndoUninstall = stringResource(R.string.apm_module_undo_uninstall_failed)
    val successUninstall = stringResource(R.string.apm_uninstall_success)
    val successUndoUninstall = stringResource(R.string.apm_module_undo_uninstall_success)
    val reboot = stringResource(id = R.string.reboot)
    val rebootToApply = stringResource(id = R.string.apm_reboot_to_apply)
    val moduleStr = stringResource(id = R.string.apm)
    val uninstall = stringResource(id = R.string.apm_uinstall)
    val cancel = stringResource(id = android.R.string.cancel)
    val moduleUninstallConfirm = stringResource(id = R.string.apm_uninstall_confirm)
    val metaModuleUninstallConfirm = stringResource(R.string.metamodule_uninstall_confirm)
    val updateText = stringResource(R.string.apm_update)
    val changelogText = stringResource(R.string.apm_changelog)
    val downloadingText = stringResource(R.string.apm_downloading)
    val startDownloadingText = stringResource(R.string.apm_start_downloading)
    val changelogFailed = stringResource(R.string.apm_changelog_failed)

    val loadingDialog = rememberLoadingDialog()
    val confirmDialog = rememberConfirmDialog()

    var shortcutModuleId by rememberSaveable { mutableStateOf<String?>(null) }
    var shortcutName by rememberSaveable { mutableStateOf("") }
    var shortcutIconUri by rememberSaveable { mutableStateOf<String?>(null) }
    var defaultShortcutIconUri by rememberSaveable { mutableStateOf<String?>(null) }
    var defaultActionShortcutIconUri by rememberSaveable { mutableStateOf<String?>(null) }
    var defaultWebUiShortcutIconUri by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedShortcutType by rememberSaveable { mutableStateOf<ShortcutType?>(null) }
    val showShortcutDialog = remember { mutableStateOf(false) }
    var currentModuleHasAction by remember { mutableStateOf(false) }
    var currentModuleHasWebUi by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filteredModules by viewModel.filteredModules.collectAsStateWithLifecycle()

    fun openShortcutDialogForType(type: ShortcutType) {
        selectedShortcutType = type
        val defaultIcon = when (type) {
            ShortcutType.Action -> defaultActionShortcutIconUri ?: defaultWebUiShortcutIconUri
            ShortcutType.WebUI -> defaultWebUiShortcutIconUri ?: defaultActionShortcutIconUri
        }
        defaultShortcutIconUri = defaultIcon
        shortcutIconUri = defaultIcon
        showShortcutDialog.value = true
    }

    fun hasModuleShortcut(context: Context, moduleId: String, type: ShortcutType): Boolean {
        return when (type) {
            ShortcutType.Action -> Shortcut.hasModuleActionShortcut(context, moduleId)
            ShortcutType.WebUI -> Shortcut.hasModuleWebUiShortcut(context, moduleId)
        }
    }

    fun deleteModuleShortcut(context: Context, moduleId: String, type: ShortcutType) {
        when (type) {
            ShortcutType.Action -> Shortcut.deleteModuleActionShortcut(context, moduleId)
            ShortcutType.WebUI -> Shortcut.deleteModuleWebUiShortcut(context, moduleId)
        }
    }

    fun createModuleShortcut(
        context: Context,
        moduleId: String,
        name: String,
        iconUri: String?,
        type: ShortcutType
    ) {
        when (type) {
            ShortcutType.Action -> {
                Shortcut.createModuleActionShortcut(
                    context = context,
                    moduleId = moduleId,
                    name = name,
                    iconUri = iconUri
                )
            }

            ShortcutType.WebUI -> {
                Shortcut.createModuleWebUiShortcut(
                    context = context,
                    moduleId = moduleId,
                    name = name,
                    iconUri = iconUri
                )
            }
        }
    }

    val pickShortcutIconLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        shortcutIconUri = uri?.toString()
    }

    val shortcutPreviewIcon = remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(shortcutIconUri) {
        val uriStr = shortcutIconUri
        if (uriStr.isNullOrBlank()) {
            shortcutPreviewIcon.value = null
            return@LaunchedEffect
        }
        val bitmap = withContext(Dispatchers.IO) {
            Shortcut.loadShortcutBitmap(context, uriStr)
        }
        shortcutPreviewIcon.value = bitmap?.asImageBitmap()
    }

    var hasExistingShortcut by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(shortcutModuleId, selectedShortcutType, showShortcutDialog.value) {
        val moduleId = shortcutModuleId
        val type = selectedShortcutType
        if (!showShortcutDialog.value || moduleId.isNullOrBlank() || type == null) {
            hasExistingShortcut = false
            return@LaunchedEffect
        }
        val exists = withContext(Dispatchers.IO) {
            hasModuleShortcut(context, moduleId, type)
        }
        hasExistingShortcut = exists
    }

    if (showShortcutDialog.value) {
        BasicAlertDialog(
            onDismissRequest = { showShortcutDialog.value = false }
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
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.apm_shortcut_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    val preview = shortcutPreviewIcon.value
                    if (preview != null) {
                        Image(
                            bitmap = preview,
                            modifier = Modifier.fillMaxSize(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                if (currentModuleHasAction && currentModuleHasWebUi) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        listOf("Action", "WebUI").forEachIndexed { index, label ->
                            SegmentedButton(
                                selected = selectedShortcutType ==
                                    (if (index == 0) ShortcutType.Action else ShortcutType.WebUI),
                                onClick = {
                                    val newType =
                                        if (index == 0) ShortcutType.Action else ShortcutType.WebUI
                                    if (selectedShortcutType != newType) {
                                        openShortcutDialogForType(newType)
                                    }
                                },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = 2
                                ),
                                label = { Text(label) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = shortcutName,
                    onValueChange = { shortcutName = it },
                    label = { Text(stringResource(id = R.string.apm_shortcut_name_label)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { pickShortcutIconLauncher.launch("image/*") }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Photo,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.apm_shortcut_icon_pick),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    if (shortcutIconUri != defaultShortcutIconUri) {
                        IconButton(
                            onClick = { shortcutIconUri = defaultShortcutIconUri }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (hasExistingShortcut) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                val moduleId = shortcutModuleId
                                val type = selectedShortcutType
                                if (!moduleId.isNullOrBlank() && type != null) {
                                    deleteModuleShortcut(context, moduleId, type)
                                }
                                showShortcutDialog.value = false
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.apm_shortcut_delete),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                ) {
                    TextButton(
                        onClick = { showShortcutDialog.value = false }
                    ) {
                        Text(cancel)
                    }
                    Button(
                        onClick = {
                            val moduleId = shortcutModuleId
                            val type = selectedShortcutType
                            if (!moduleId.isNullOrBlank() && shortcutName.isNotBlank() && type != null) {
                                createModuleShortcut(
                                    context = context,
                                    moduleId = moduleId,
                                    name = shortcutName,
                                    iconUri = shortcutIconUri,
                                    type = type
                                )
                            }
                            showShortcutDialog.value = false
                        }
                    ) {
                        Text(
                            text = if (hasExistingShortcut) {
                                stringResource(id = R.string.apm_update)
                            } else {
                                stringResource(id = android.R.string.ok)
                            }
                        )
                    }
                }
            }
            }
        }
    }

    suspend fun onModuleUpdate(
        module: ModuleInfo,
        changelogUrl: String,
        downloadUrl: String,
        fileName: String
    ) {
        val changelog = loadingDialog.withLoading {
            withContext(Dispatchers.IO) {
                runCatching {
                    if (Patterns.WEB_URL.matcher(changelogUrl).matches()) {
                        apApp.okhttpClient
                            .newCall(
                                Request.Builder().url(changelogUrl).build()
                            )
                            .execute()
                            .use { it.body.string() }
                    } else {
                        changelogUrl
                    }
                }.getOrDefault("")
            }
        }

        val confirmResult = confirmDialog.awaitConfirm(
            title = changelogText,
            content = changelog.ifEmpty { changelogFailed },
            markdown = true,
            confirm = updateText,
        )

        if (confirmResult != ConfirmResult.Confirmed) {
            return
        }

        withContext(Dispatchers.Main) {
            Toast.makeText(
                context, startDownloadingText.format(module.name), Toast.LENGTH_SHORT
            ).show()
        }

        val downloading = downloadingText.format(module.name)
        withContext(Dispatchers.IO) {
            download(
                context,
                downloadUrl,
                fileName,
                downloading,
                onDownloaded = onInstallModule,
                onDownloading = {
                    launch(Dispatchers.Main) {
                        Toast.makeText(context, downloading, Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    suspend fun onModuleUninstall(module: ModuleInfo) {
        val formatter =
            if (module.metamodule) metaModuleUninstallConfirm else moduleUninstallConfirm
        val confirmResult = confirmDialog.awaitConfirm(
            moduleStr,
            content = formatter.format(module.name),
            confirm = uninstall,
            dismiss = cancel
        )
        if (confirmResult != ConfirmResult.Confirmed) {
            return
        }

        val success = loadingDialog.withLoading {
            withContext(Dispatchers.IO) {
                Shortcut.deleteModuleActionShortcut(context, module.id)
                Shortcut.deleteModuleWebUiShortcut(context, module.id)
                viewModel.uninstallModule(module.id)
            }
        }

        if (success) {
            viewModel.fetchModuleList()
        }

        val message = if (success) {
            successUninstall.format(module.name)
        } else {
            failedUninstall.format(module.name)
        }

        val actionLabel = if (success) {
            reboot
        } else {
            null
        }

        val result = snackBarHost.showSnackbar(
            message = message, actionLabel = actionLabel, duration = SnackbarDuration.Long
        )
        if (result) {
            rebootJailbreakAware()
        }
    }

    suspend fun onModuleUndoUninstall(module: ModuleInfo) {
        val success = loadingDialog.withLoading {
            withContext(Dispatchers.IO) {
                viewModel.undoUninstallModule(module.id)
            }
        }

        if (success) {
            viewModel.fetchModuleList()
        }

        val message = if (success) {
            successUndoUninstall.format(module.name)
        } else {
            failedUndoUninstall.format(module.name)
        }

        val actionLabel = if (success) {
            reboot
        } else {
            null
        }

        val result = snackBarHost.showSnackbar(
            message = message, actionLabel = actionLabel, duration = SnackbarDuration.Long
        )
        if (result) {
            rebootJailbreakAware()
        }
    }

    fun onModuleAddShortcut(module: ModuleInfo) {
        shortcutModuleId = module.id
        shortcutName = module.name
        shortcutIconUri = null
        defaultShortcutIconUri = null

        currentModuleHasAction = module.hasActionScript
        currentModuleHasWebUi = module.hasWebUi

        defaultActionShortcutIconUri = module.actionIconPath
            ?.takeIf { it.isNotBlank() }
            ?.let { "su:$it" }
        defaultWebUiShortcutIconUri = module.webUiIconPath
            ?.takeIf { it.isNotBlank() }
            ?.let { "su:$it" }

        if (module.hasActionScript && module.hasWebUi) {
            openShortcutDialogForType(ShortcutType.Action)
        } else if (module.hasActionScript) {
            openShortcutDialogForType(ShortcutType.Action)
        } else if (module.hasWebUi) {
            openShortcutDialogForType(ShortcutType.WebUI)
        }
    }

    var isWarningManuallyClosed by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .withBackdrop(backdrop),
        state = state,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(
            top = topPadding + 8.dp,
            bottom = bottomPadding + 16.dp,
            start = 16.dp,
            end = 16.dp
        )
    ) {
        item(key = "meta_module_warning") {
            AnimatedVisibility(
                visible = uiState.metaModuleWarning != null && !isWarningManuallyClosed,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    MetaModuleWarningCardMaterial(
                        text = uiState.metaModuleWarning ?: "",
                        onClosed = { isWarningManuallyClosed = true }
                    )
                }
            }
        }

        if (uiState.search.isNotEmpty()) {
            item(key = "search_count") {
                Text(
                    text = stringResource(R.string.apm_search_count, filteredModules.size),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        when {
            filteredModules.isEmpty() -> {
                item {
                    Box(
                        modifier = Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!uiState.isRefreshing) {
                            Text(
                                stringResource(R.string.apm_empty),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            else -> {
                items(
                    items = filteredModules,
                    key = { it.id }
                ) { module ->
                    val scope = rememberCoroutineScope()
                    val updatedModule =
                        viewModel.uiState.value.updateResults[module.id] ?: Triple("", "", "")

                    ModuleItemMaterial(
                        navigator = navigator,
                        module = module,
                        updateUrl = updatedModule.first,
                        onUninstall = {
                            scope.launch { onModuleUninstall(module) }
                        },
                        onUndoUninstall = {
                            scope.launch { onModuleUndoUninstall(module) }
                        },
                        onCheckChanged = {
                            scope.launch {
                                val success = loadingDialog.withLoading {
                                    withContext(Dispatchers.IO) {
                                        viewModel.toggleModule(module.id, !module.enabled)
                                    }
                                }

                                if (success) {
                                    viewModel.fetchModuleList()

                                    // In jailbreak mode a full reboot would unload the
                                    // runtime-loaded module, so apply without the prompt.
                                    val isJailbreak = withContext(Dispatchers.IO) {
                                        isJailbreakMode()
                                    }
                                    if (!isJailbreak) {
                                        val result = snackBarHost.showSnackbar(
                                            message = rebootToApply,
                                            actionLabel = reboot,
                                            duration = SnackbarDuration.Long
                                        )

                                        if (result) {
                                            reboot()
                                        }
                                    }
                                } else {
                                    val message =
                                        if (module.enabled) failedDisable else failedEnable
                                    snackBarHost.showSnackbar(message.format(module.name))
                                }
                            }
                        },
                        onUpdate = {
                            scope.launch {
                                onModuleUpdate(
                                    module,
                                    updatedModule.third,
                                    updatedModule.first,
                                    "${module.name}-${updatedModule.second}.zip"
                                )
                            }
                        },
                        onClick = {
                            onClickModule(module.id, module.name, module.hasWebUi)
                        },
                        onModuleAddShortcut = { moduleInfo ->
                            onModuleAddShortcut(moduleInfo)
                        }
                    )
                }
            }
        }
    }
    DownloadListener(context, onInstallModule)
}

@Composable
private fun MetaModuleWarningCardMaterial(
    text: String,
    onClosed: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Error,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onClosed) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ModuleItemMaterial(
    navigator: Navigator,
    module: ModuleInfo,
    updateUrl: String,
    onUninstall: (ModuleInfo) -> Unit,
    onUndoUninstall: (ModuleInfo) -> Unit,
    onCheckChanged: (Boolean) -> Unit,
    onUpdate: (ModuleInfo) -> Unit,
    onClick: (ModuleInfo) -> Unit,
    onModuleAddShortcut: (ModuleInfo) -> Unit,
) {
    val decoration = if (!module.remove) TextDecoration.None else TextDecoration.LineThrough
    val moduleVersion = stringResource(id = R.string.apm_version)
    val moduleAuthor = stringResource(id = R.string.apm_author)
    val viewModel = viewModel<APModuleViewModel>()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceBright
    ) {
        Box {
            Column(
                modifier = Modifier
                    .run {
                        if (module.hasActionScript || module.hasWebUi) {
                            combinedClickable(
                                onLongClick = { onModuleAddShortcut(module) },
                                onClick = {
                                    if (module.hasWebUi) {
                                        onClick(module)
                                    }
                                }
                        )
                    } else {
                        this
                    }
                }
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = module.name,
                            fontSize = MaterialTheme.typography.titleMedium.fontSize,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                            textDecoration = decoration,
                            modifier = Modifier.weight(1f, false)
                        )
                        if (module.metamodule) {
                            LabelText(
                                label = "META",
                                containerColor = MaterialTheme.colorScheme.tertiary,
                            )
                        }
                    }

                    Text(
                        text = "$moduleVersion: ${module.version}",
                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                        textDecoration = decoration,
                    )

                    Text(
                        text = "$moduleAuthor: ${module.author}",
                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                        textDecoration = decoration,
                    )
                }

                Switch(
                    enabled = !module.update,
                    checked = module.enabled,
                    onCheckedChange = onCheckChanged,
                    thumbContent = {
                        if (module.enabled) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.surfaceBright,
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                            )
                        }
                    }
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = module.description,
                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                overflow = TextOverflow.Ellipsis,
                maxLines = 4,
                textDecoration = decoration,
            )

            Spacer(Modifier.height(16.dp))

            HorizontalDivider(thickness = Dp.Hairline)

            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val hideText = module.hasWebUi && module.hasActionScript

                if (module.hasActionScript) {
                    ModuleActionButton(
                        icon = Icons.Filled.PlayArrow,
                        text = stringResource(R.string.apm_action),
                        showText = !hideText,
                        enabled = !module.remove && module.enabled,
                        onClick = {
                            navigator.navigateToExecuteAction(module.id)
                            viewModel.markNeedRefresh()
                        }
                    )
                }

                if (module.hasWebUi) {
                    ModuleActionButton(
                        icon = Icons.Filled.HorizontalSplit,
                        text = stringResource(R.string.apm_webui_open),
                        showText = !hideText,
                        enabled = !module.remove && module.enabled,
                        onClick = { onClick(module) }
                    )
                }

                Spacer(modifier = Modifier.weight(1f, true))

                if (updateUrl.isNotEmpty()) {
                    ModuleActionButton(
                        icon = Icons.Filled.Download,
                        text = stringResource(R.string.apm_update),
                        showText = true,
                        filled = true,
                        enabled = !module.remove,
                        onClick = { onUpdate(module) }
                    )
                }

                if (!module.remove) {
                    ModuleActionButton(
                        icon = Icons.Filled.Delete,
                        text = stringResource(R.string.apm_uinstall),
                        showText = true,
                        onClick = { onUninstall(module) }
                    )
                } else {
                    ModuleActionButton(
                        icon = Icons.AutoMirrored.Filled.Undo,
                        text = stringResource(R.string.apm_undo),
                        showText = true,
                        onClick = { onUndoUninstall(module) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
        }

            if (module.update) {
                Icon(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(150.dp)
                        .alpha(0.1f),
                    imageVector = Icons.Filled.Download,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun ModuleActionButton(
    icon: ImageVector,
    text: String,
    showText: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    filled: Boolean = false,
) {
    val content: @Composable () -> Unit = {
        Icon(
            modifier = Modifier.size(20.dp),
            imageVector = icon,
            contentDescription = null
        )
        if (showText) {
            Spacer(Modifier.width(4.dp))
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = FontWeight(500)
            )
        }
    }
    if (filled) {
        Button(
            modifier = modifier.defaultMinSize(minWidth = 52.dp, minHeight = 32.dp),
            enabled = enabled,
            onClick = onClick,
            shape = ButtonDefaults.textShape,
            contentPadding = ButtonDefaults.TextButtonContentPadding,
        ) {
            content()
        }
    } else {
        FilledTonalButton(
            modifier = modifier.defaultMinSize(minWidth = 52.dp, minHeight = 32.dp),
            enabled = enabled,
            onClick = onClick,
            contentPadding = ButtonDefaults.TextButtonContentPadding,
        ) {
            content()
        }
    }
}
