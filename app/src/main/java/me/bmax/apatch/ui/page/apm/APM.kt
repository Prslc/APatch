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
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.apApp
import me.bmax.apatch.ui.LocalSnackbarHost
import me.bmax.apatch.ui.WebUIActivity
import me.bmax.apatch.ui.component.ConfirmResult
import me.bmax.apatch.ui.component.IconTextButton
import me.bmax.apatch.ui.component.LoadingIndicator
import me.bmax.apatch.ui.component.ModuleStateIndicator
import me.bmax.apatch.ui.component.WarningCard
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.ui.component.rememberLoadingDialog
import me.bmax.apatch.ui.navigation.LocalNavigator
import me.bmax.apatch.ui.navigation.MODULE_TYPE
import me.bmax.apatch.ui.navigation.Navigator
import me.bmax.apatch.ui.theme.blurEffect
import me.bmax.apatch.ui.theme.getAppBarColor
import me.bmax.apatch.ui.theme.rememberBlurBackdrop
import me.bmax.apatch.ui.theme.withBackdrop
import me.bmax.apatch.util.DownloadListener
import me.bmax.apatch.util.Shortcut
import me.bmax.apatch.util.download
import me.bmax.apatch.util.hasMagisk
import me.bmax.apatch.util.reboot
import me.bmax.apatch.util.toggleModule
import me.bmax.apatch.util.undoUninstallModule
import me.bmax.apatch.util.uninstallModule
import okhttp3.Request
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.SnackbarDuration
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.SnackbarResult
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.HorizontalSplit
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.icon.extended.Undo
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
fun APModuleScreen(
    modifier: Modifier,
    bottomPadding: Dp,
    isCurrentPage: Boolean = true,
    viewModel: APModuleViewModel = viewModel()
) {
    val uiState = viewModel.uiState

    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val snackBarHostState = LocalSnackbarHost.current
    val scrollBehavior = MiuixScrollBehavior()

    var expanded by remember { mutableStateOf(false) }
    val backdrop = if (isCurrentPage) rememberBlurBackdrop() else null

    val state by APApplication.apStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)

    if (state != APApplication.State.ANDROIDPATCH_INSTALLED && state != APApplication.State.ANDROIDPATCH_NEED_UPDATE) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(id = R.string.apm_not_installed),
                style = MiuixTheme.textStyles.body2
            )
        }
        return
    }

    val webUILauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            viewModel.fetchModuleList()
        }

    val hasMagisk = hasMagisk()
    val moduleListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    var fabVisible by remember { mutableStateOf(true) }

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
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                modifier = Modifier.blurEffect(backdrop),
                color = backdrop.getAppBarColor(),
                title = stringResource(R.string.apm),
                scrollBehavior = scrollBehavior,
            ) {
                SearchBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    inputField = {
                        InputField(
                            query = uiState.search,
                            onQueryChange = { viewModel.onSearchChange(it) },
                            onSearch = { expanded = false },
                            expanded = expanded,
                            onExpandedChange = {
                                expanded = it
                                if (!it) viewModel.onSearchChange("")
                            }
                        )
                    },
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    content = {}
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
                    visible = fabVisible, enter = scaleIn(
                        initialScale = 0.6f, animationSpec = spring(
                            dampingRatio = 0.75f, stiffness = 420f
                        )
                    ) + fadeIn(
                        animationSpec = tween(120)
                    ), exit = scaleOut(
                        targetScale = 0.9f, animationSpec = tween(
                            durationMillis = 180, easing = FastOutSlowInEasing
                        )
                    ) + fadeOut(
                        animationSpec = tween(
                            durationMillis = 280, easing = LinearOutSlowInEasing
                        )
                    )
                ) {
                    FloatingActionButton(
                        containerColor = colorScheme.primary,
                        modifier = Modifier.padding(bottom = bottomPadding + 16.dp),
                        onClick = {
                            val intent =
                                Intent(Intent.ACTION_GET_CONTENT).apply { type = "application/zip" }
                            selectZipLauncher.launch(intent)
                        }
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Add,
                            contentDescription = null,
                            tint = colorScheme.onPrimary
                        )
                    }
                }
            }
        },
    ) { innerPadding ->

        PullToRefresh(
            modifier = Modifier.fillMaxSize(),
            isRefreshing = uiState.isRefreshing,
            refreshTexts = listOf(
                stringResource(R.string.refresh_pulling),
                stringResource(R.string.refresh_release),
                stringResource(R.string.refresh_refresh),
                stringResource(R.string.refresh_complete)
            ),
            onRefresh = { viewModel.fetchModuleList() },
            contentPadding = innerPadding
        ) {
            when {
                hasMagisk -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.apm_magisk_conflict),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                else -> {
                    ModuleList(
                        navigator = navigator,
                        viewModel = viewModel,
                        state = moduleListState,
                        backdrop = backdrop,
                        contentPadding = innerPadding,
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
                        snackBarHost = snackBarHostState,
                        scrollBehavior = scrollBehavior
                    )
                }
            }
        }
    }
}

@Composable
private fun MetaModuleWarningCard(
    text: String,
    onClosed: () -> Unit
) {
    WarningCard(
        message = text,
        onClose = onClosed
    )
}

@Composable
private fun ModuleList(
    navigator: Navigator,
    viewModel: APModuleViewModel,
    state: LazyListState,
    backdrop: LayerBackdrop?,
    contentPadding: PaddingValues,
    bottomPadding: Dp,
    onInstallModule: (Uri) -> Unit,
    onClickModule: (id: String, name: String, hasWebUi: Boolean) -> Unit,
    context: Context,
    snackBarHost: SnackbarHostState,
    scrollBehavior: ScrollBehavior
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

    val uiState = viewModel.uiState
    val filteredModules = viewModel.filteredModules

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

    if (showShortcutDialog.value) {
        WindowDialog(
            show = showShortcutDialog.value,
            title = stringResource(R.string.apm_shortcut_title),
            onDismissRequest = {
                showShortcutDialog.value = false
            }
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (currentModuleHasAction && currentModuleHasWebUi) {
                    TabRowWithContour(
                        tabs = listOf("Action", "WebUI"),
                        selectedTabIndex = if (selectedShortcutType == ShortcutType.WebUI) 1 else 0,
                        onTabSelected = { index ->
                            val newType =
                                if (index == 0) ShortcutType.Action else ShortcutType.WebUI
                            if (selectedShortcutType != newType) {
                                openShortcutDialogForType(newType)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .size(72.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black),
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

                Row {
                    TextButton(
                        modifier = Modifier.weight(1f),
                        text = stringResource(id = R.string.apm_shortcut_icon_pick),
                        onClick = { pickShortcutIconLauncher.launch("image/*") },
                    )
                    AnimatedVisibility(
                        visible = shortcutIconUri != defaultShortcutIconUri,
                        enter = expandHorizontally() + slideInHorizontally(initialOffsetX = { it }),
                        exit = shrinkHorizontally() + slideOutHorizontally(targetOffsetX = { it }),
                        modifier = Modifier.align(Alignment.CenterVertically),
                    ) {
                        IconButton(
                            onClick = { shortcutIconUri = defaultShortcutIconUri },
                            modifier = Modifier.padding(start = 12.dp)
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Undo,
                                contentDescription = null,
                                tint = colorScheme.onSurface,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                }

                TextField(
                    value = shortcutName,
                    onValueChange = { shortcutName = it },
                    label = stringResource(id = R.string.apm_shortcut_name_label)
                )

                if (hasExistingShortcut) {
                    TextButton(
                        text = stringResource(id = R.string.apm_shortcut_delete),
                        onClick = {
                            val moduleId = shortcutModuleId
                            val type = selectedShortcutType
                            if (!moduleId.isNullOrBlank() && type != null) {
                                deleteModuleShortcut(context, moduleId, type)
                            }
                            showShortcutDialog.value = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        text = stringResource(id = android.R.string.cancel),
                        onClick = { showShortcutDialog.value = false },
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        text = if (hasExistingShortcut) {
                            stringResource(id = R.string.apm_update)
                        } else {
                            stringResource(id = android.R.string.ok)
                        },
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
                        },
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
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
                uninstallModule(module.id)
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
            message = message, actionLabel = actionLabel, duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) {
            reboot()
        }
    }

    suspend fun onModuleUndoUninstall(module: ModuleInfo) {
        val success = loadingDialog.withLoading {
            withContext(Dispatchers.IO) {
                undoUninstallModule(module.id)
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
            message = message, actionLabel = actionLabel, duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) {
            reboot()
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
            .withBackdrop(backdrop)
            .overScrollVertical()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        state = state,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
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
                    MetaModuleWarningCard(
                        text = uiState.metaModuleWarning ?: "",
                        onClosed = { isWarningManuallyClosed = true }
                    )
                }
            }
        }

        when {
            filteredModules.isEmpty() -> {
                item {
                    Box(
                        modifier = Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.isRefreshing) {
                            LoadingIndicator()
                        } else {
                            Text(
                                stringResource(R.string.apm_empty),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            else -> {
                items(filteredModules, key = { it.id }) { module ->
                    val scope = rememberCoroutineScope()
                    val updatedModule = viewModel.uiState.updateResults[module.id] ?: Triple("", "", "")

                    ModuleItem(
                        navigator,
                        module,
                        updatedModule.first,
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
                                        toggleModule(module.id, !module.enabled)
                                    }
                                }

                                if (success) {
                                    viewModel.fetchModuleList()

                                    val result = snackBarHost.showSnackbar(
                                        message = rebootToApply,
                                        actionLabel = reboot,
                                        duration = SnackbarDuration.Short
                                    )

                                    if (result == SnackbarResult.ActionPerformed) {
                                        reboot()
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
                            onClickModule(it.id, it.name, it.hasWebUi)
                        },
                        onModuleAddShortcut = { moduleInfo ->
                            onModuleAddShortcut(moduleInfo)
                        }
                    )
                    // fix last item shadow incomplete in LazyColumn
                    Spacer(Modifier.height(1.dp))
                }
            }
        }
    }
    DownloadListener(context, onInstallModule)
}

@Composable
private fun ModuleItem(
    navigator: Navigator,
    module: ModuleInfo,
    updateUrl: String,
    onUninstall: (ModuleInfo) -> Unit,
    onUndoUninstall: (ModuleInfo) -> Unit,
    onCheckChanged: (Boolean) -> Unit,
    onUpdate: (ModuleInfo) -> Unit,
    onClick: (ModuleInfo) -> Unit,
    onModuleAddShortcut: (ModuleInfo) -> Unit,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
) {
    val decoration = if (!module.remove) TextDecoration.None else TextDecoration.LineThrough
    val moduleVersion = stringResource(id = R.string.apm_version)
    val moduleAuthor = stringResource(id = R.string.apm_author)
    val viewModel = viewModel<APModuleViewModel>()
    Card(
        modifier = modifier,
        pressFeedbackType = PressFeedbackType.Sink,
        showIndication = true
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { onClick(module) },
                    onLongClick = { onModuleAddShortcut(module) }
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .alpha(alpha = alpha)
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        SubcomposeLayout { constraints ->
                            val spacingPx = 6.dp.roundToPx()
                            var nameTextLayout: TextLayoutResult? = null
                            val metaPlaceable = if (module.metamodule) {
                                subcompose("meta") {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = colorScheme.tertiaryContainer
                                    ) {
                                        Text(
                                            text = "META",
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(
                                                horizontal = 4.dp,
                                                vertical = 1.dp
                                            ),
                                            color = colorScheme.onTertiaryContainer,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }.first().measure(
                                    Constraints(
                                        0,
                                        constraints.maxWidth,
                                        0,
                                        constraints.maxHeight
                                    )
                                )
                            } else null

                            val reserved = (metaPlaceable?.width
                                ?: 0) + if (metaPlaceable != null) spacingPx else 0
                            val nameMax = (constraints.maxWidth - reserved).coerceAtLeast(0)
                            val namePlaceable = subcompose("name") {
                                Text(
                                    text = module.name,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight(550),
                                    maxLines = 2,
                                    textDecoration = decoration,
                                    overflow = TextOverflow.Ellipsis,
                                    onTextLayout = { nameTextLayout = it }
                                )
                            }.first().measure(
                                Constraints(
                                    constraints.minWidth,
                                    nameMax,
                                    constraints.minHeight,
                                    constraints.maxHeight
                                )
                            )

                            val width = (namePlaceable.width + reserved).coerceIn(
                                constraints.minWidth,
                                constraints.maxWidth
                            )
                            val height = maxOf(namePlaceable.height, metaPlaceable?.height ?: 0)

                            layout(width, height) {
                                namePlaceable.placeRelative(0, 0)
                                val endX = nameTextLayout?.let { layoutRes ->
                                    val last = (layoutRes.lineCount - 1).coerceAtLeast(0)
                                    layoutRes.getLineRight(last).toInt()
                                } ?: namePlaceable.width
                                metaPlaceable?.placeRelative(
                                    endX + spacingPx,
                                    (height - (metaPlaceable.height)) / 2
                                )
                            }
                        }

                        Text(
                            text = "$moduleVersion: ${module.version}",
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp),
                            fontWeight = FontWeight(550),
                            color = colorScheme.onSurfaceVariantSummary,
                            textDecoration = decoration
                        )

                        Text(
                            text = "$moduleAuthor: ${module.author}",
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 1.dp),
                            fontWeight = FontWeight(550),
                            color = colorScheme.onSurfaceVariantSummary,
                            textDecoration = decoration
                        )
                    }

                    Switch(
                        enabled = !module.update,
                        checked = module.enabled,
                        onCheckedChange = onCheckChanged
                    )
                }

                Text(
                    text = module.description,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    fontSize = 14.sp,
                    color = colorScheme.onSurfaceVariantSummary,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 4,
                    textDecoration = decoration
                )

                HorizontalDivider(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = colorScheme.outline.copy(alpha = 0.5f)
                )

                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    val hideText = module.hasWebUi && module.hasActionScript

                    if (module.hasActionScript) {
                        IconTextButton(
                            iconRes = MiuixIcons.Play,
                            textRes = R.string.apm_action,
                            showText = !hideText,
                            onClick = {
                                navigator.navigateToExecuteAction(module.id)
                                viewModel.markNeedRefresh()
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    if (module.hasWebUi) {
                        IconTextButton(
                            iconRes = MiuixIcons.HorizontalSplit,
                            textRes = R.string.apm_webui_open,
                            showText = !hideText,
                            onClick = { onClick(module) }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    if (updateUrl.isNotEmpty()) {
                        IconTextButton(
                            iconRes = MiuixIcons.Download,
                            textRes = R.string.apm_update,
                            onClick = { onUpdate(module) }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    if (!module.remove) {
                        IconTextButton(
                            textRes = R.string.apm_uinstall,
                            iconRes = MiuixIcons.Delete,
                            onClick = { onUninstall(module) }
                        )
                    } else {
                        IconTextButton(
                            textRes = R.string.apm_undo,
                            iconRes = MiuixIcons.Undo,
                            onClick = { onUndoUninstall(module) }
                        )
                    }
                }
            }
            if (module.update) {
                ModuleStateIndicator(MiuixIcons.Download)
            }
        }
    }
}
