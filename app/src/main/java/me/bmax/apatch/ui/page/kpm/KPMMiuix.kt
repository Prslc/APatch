package me.bmax.apatch.ui.page.kpm

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.ChipLabel
import me.bmax.apatch.ui.component.ScrollToTopOnChange
import me.bmax.apatch.ui.component.dialog.ConfirmResult
import me.bmax.apatch.ui.component.dialog.ModuleDownloadProgressDialog
import me.bmax.apatch.ui.component.miuix.DropdownItem
import me.bmax.apatch.ui.component.miuix.IconTextButton
import me.bmax.apatch.ui.component.dialog.rememberConfirmDialog
import me.bmax.apatch.ui.component.dialog.rememberLoadingDialog
import me.bmax.apatch.ui.component.searchbar.AppSearchBar
import me.bmax.apatch.ui.navigation.LocalNavigator
import me.bmax.apatch.ui.page.patch.PatchMode
import me.bmax.apatch.util.DownloadCancelHandle
import me.bmax.apatch.util.ModuleRepo
import me.bmax.apatch.util.reboot
import me.bmax.apatch.ui.theme.blurEffect
import me.bmax.apatch.ui.theme.getAppBarColor
import me.bmax.apatch.ui.theme.rememberBlurBackdrop
import me.bmax.apatch.ui.theme.withBackdrop
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Update
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.window.WindowDialog
import top.yukonga.miuix.kmp.window.WindowListPopup

private const val TAG = "KernelPatchModule"

@Composable
fun KPModuleScreenMiuix(
    modifier: Modifier,
    bottomPadding: Dp,
    isCurrentPage: Boolean = true,
    viewModel: KPModuleViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val confirmDialog = rememberConfirmDialog()
    val loadingDialog = rememberLoadingDialog()

    val scrollBehavior = MiuixScrollBehavior()
    val kpModuleListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    var fabVisible by remember { mutableStateOf(true) }

    val latestFilteredModules = rememberUpdatedState(uiState.filteredModules)
    ScrollToTopOnChange(
        kpModuleListState,
        uiState.search,
        isBusy = { uiState.isRefreshing },
        observedList = { latestFilteredModules.value }
    )

    val navigator = LocalNavigator.current
    val backdrop = if (isCurrentPage) rememberBlurBackdrop() else null

    // Load lazily on first visit; the pager pre-composes all pages, so this
    // avoids scanning KPM modules at startup before the page is ever shown.
    LaunchedEffect(isCurrentPage) {
        if (isCurrentPage) viewModel.loadIfNeeded()
    }

    val latestIsCurrentPage by rememberUpdatedState(isCurrentPage)
    LifecycleResumeEffect(Unit) {
        if (latestIsCurrentPage) viewModel.loadIfNeeded()
        onPauseOrDispose { }
    }

    val moduleStr = stringResource(id = R.string.kpm)
    val moduleUninstallConfirm = stringResource(id = R.string.kpm_unload_confirm)
    val unloadText = stringResource(R.string.kpm_unload)
    val cancelText = stringResource(android.R.string.cancel)
    val successToastText = stringResource(id = R.string.kpm_load_toast_succ)
    val failToastText = stringResource(id = R.string.kpm_load_toast_failed)
    val installSuccessText = stringResource(R.string.kpm_install_toast_succ)
    val updateTitleText = stringResource(R.string.kpm_update_confirm_title)
    val updateConfirmText = stringResource(R.string.kpm_update_confirm)
    val updateText = stringResource(R.string.kpm_update)
    val updateSuccessText = stringResource(R.string.kpm_update_succ)
    val downloadFailedText = stringResource(R.string.online_download_failed)
    val downloadCancelledText = stringResource(R.string.online_download_cancelled)
    val installFailedText = stringResource(R.string.online_install_failed)
    val rebootText = stringResource(R.string.reboot)
    val controlMsgText = stringResource(R.string.kpm_control_outMsg)
    val controlOkText = stringResource(R.string.kpm_control_ok)
    val controlFailText = stringResource(R.string.kpm_control_failed)

    var updateTask by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var updateHandle by remember { mutableStateOf<DownloadCancelHandle?>(null) }

    suspend fun onModuleInstall(uri: Uri) {
        val rc = viewModel.installModule(uri)
        val text = if (rc == 0) installSuccessText else "$failToastText: $rc"
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }

    suspend fun onModuleControl(module: KPModel.KPMInfo, param: String) {
        val result = loadingDialog.withLoading {
            viewModel.controlModule(module.name, param)
        }
        val text = if (result.rc >= 0) {
            "$controlOkText\n$controlMsgText: ${result.outMsg}"
        } else {
            "$controlFailText\n$controlMsgText: ${result.outMsg}"
        }
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }

    suspend fun onModuleUpdate(module: KPModel.KPMInfo) {
        val update = uiState.updates[module.moduleId] ?: return
        val confirmed = confirmDialog.awaitConfirm(
            title = updateTitleText,
            content = updateConfirmText.format(module.name, update.version),
            confirm = updateText,
            dismiss = cancelText
        )
        if (confirmed != ConfirmResult.Confirmed) return

        updateTask = module.name to 0
        val handle = DownloadCancelHandle()
        updateHandle = handle
        val file = try {
            ModuleRepo.downloadModuleFile(
                id = module.moduleId,
                kpm = true,
                fileName = "${module.moduleId}-${update.version}.kpm",
                authorUrl = update.authorUrl,
                cancel = handle
            ) { percent -> updateTask = module.name to percent }
        } finally {
            updateTask = null
            updateHandle = null
        }
        if (handle.isCancelled) {
            Toast.makeText(context, downloadCancelledText, Toast.LENGTH_SHORT).show()
            return
        }
        if (file == null) {
            Toast.makeText(context, downloadFailedText, Toast.LENGTH_SHORT).show()
            return
        }

        val rc = viewModel.installModule(Uri.fromFile(file))
        file.delete()
        if (rc != 0) {
            Toast.makeText(context, installFailedText.format(rc), Toast.LENGTH_SHORT).show()
            return
        }

        val rebootConfirmed = confirmDialog.awaitConfirm(
            title = rebootText,
            content = updateSuccessText.format(update.version),
            confirm = rebootText,
            dismiss = cancelText
        )
        if (rebootConfirmed == ConfirmResult.Confirmed) {
            withContext(Dispatchers.IO) { reboot() }
        }
    }

    val state by APApplication.apStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)
    if (state == APApplication.State.UNKNOWN_STATE) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row {
                Text(
                    text = stringResource(id = R.string.kpm_kp_not_installed),
                    style = MiuixTheme.textStyles.body2
                )
            }
        }
        return
    }

    LaunchedEffect(kpModuleListState) {
        var lastIndex = 0
        var lastOffset = 0
        snapshotFlow {
            kpModuleListState.firstVisibleItemIndex to
                    kpModuleListState.firstVisibleItemScrollOffset
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
                title = stringResource(R.string.kpm),
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = { navigator.navigateToOnline(kpmOnly = true) }) {
                        Icon(
                            imageVector = MiuixIcons.Download,
                            tint = colorScheme.onSurface,
                            contentDescription = stringResource(R.string.online_module)
                        )
                    }
                },
            ) {
                AppSearchBar(
                    query = uiState.search,
                    onQueryChange = { viewModel.updateSearch(it) },
                    placeholder = stringResource(R.string.search_modules),
                )
            }
        },
        floatingActionButton = {
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
                KPMFloatingActionButton(
                    onLoadModule = { uri ->
                        scope.launch {
                            val rc = viewModel.loadModule(uri)
                            val toastText = if (rc == 0) successToastText else "$failToastText: $rc"
                            Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onInstallModule = { uri ->
                        scope.launch { onModuleInstall(uri) }
                    },
                    onNavigateToPatches = {
                        navigator.navigateToPatches(PatchMode.PATCH_AND_INSTALL)
                    },
                    bottomPadding = bottomPadding
                )
            }
        }
    ) { innerPadding ->
        KPModuleList(
            uiState = uiState,
            onRefresh = { viewModel.fetchModuleList() },
            onUninstall = { module ->
                scope.launch {
                    val result = confirmDialog.awaitConfirm(
                        title = moduleStr,
                        content = moduleUninstallConfirm.format(module.name),
                        confirm = unloadText,
                        dismiss = cancelText
                    )
                    if (result == ConfirmResult.Confirmed) {
                        viewModel.uninstallModule(module)
                    }
                }
            },
            onControl = { module ->
                viewModel.openControlDialog(module)
            },
            onUpdate = { module -> scope.launch { onModuleUpdate(module) } },
            onToggle = { module, enabled ->
                scope.launch { viewModel.setModuleDisabled(module, enabled) }
            },
            backdrop = backdrop,
            state = kpModuleListState,
            scrollBehavior = scrollBehavior,
            contentPadding = innerPadding,
            bottomPadding = bottomPadding
        )
    }
    if (uiState.showControlDialog && uiState.controlTarget != null) {
        val target = uiState.controlTarget!!
        KPMControlDialog(
            onDismiss = { viewModel.closeControlDialog() },
            onConfirm = { param ->
                scope.launch { onModuleControl(target, param) }
            }
        )
    }
    updateTask?.let { (name, progress) ->
        ModuleDownloadProgressDialog(
            title = name,
            progress = progress,
            onCancel = { updateHandle?.cancel() }
        )
    }
}

@Composable
fun KPMFloatingActionButton(
    onLoadModule: (Uri) -> Unit,
    onInstallModule: (Uri) -> Unit,
    onNavigateToPatches: () -> Unit,
    bottomPadding: Dp
) {
    val expanded = remember { mutableStateOf(false) }
    val context = LocalContext.current

    val selectZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode != RESULT_OK) return@rememberLauncherForActivityResult
        it.data?.data?.let { uri -> onInstallModule(uri) }
        // Log.i(TAG, "select zip result: $it.uri")
    }

    val selectKpmLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode != RESULT_OK) return@rememberLauncherForActivityResult
        it.data?.data?.let { uri -> onLoadModule(uri) }
    }

    val moduleLoad = stringResource(id = R.string.kpm_load)
    val moduleInstall = stringResource(id = R.string.kpm_install)
    val moduleEmbed = stringResource(id = R.string.kpm_embed)
    val options = listOf(moduleEmbed, moduleInstall, moduleLoad)

    Column {
        FloatingActionButton(
            onClick = { expanded.value = !expanded.value },
            containerColor = colorScheme.primary,
            modifier = Modifier.padding(bottom = bottomPadding + 16.dp)
        ) {
            Icon(
                imageVector = MiuixIcons.Add,
                contentDescription = null,
                tint = colorScheme.onPrimary
            )
        }

        WindowListPopup(
            show = expanded.value,
            alignment = PopupPositionProvider.Align.TopEnd,
            onDismissRequest = { expanded.value = false }
        ) {
            ListPopupColumn {
                options.forEachIndexed { index, label ->
                    DropdownItem(
                        text = label,
                        optionSize = options.size,
                        index = index,
                        onSelectedIndexChange = {
                            when (label) {
                                moduleEmbed -> onNavigateToPatches()
                                moduleInstall -> {
                                    val intent =
                                        Intent(Intent.ACTION_GET_CONTENT).apply { type = "*/*" }
                                    selectZipLauncher.launch(intent)
                                }

                                moduleLoad -> {
                                    val intent =
                                        Intent(Intent.ACTION_GET_CONTENT).apply { type = "*/*" }
                                    selectKpmLauncher.launch(intent)
                                }
                            }
                            expanded.value = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun KPMControlDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var controlParam by remember { mutableStateOf("") }
    var enable by remember { mutableStateOf(false) }

    WindowDialog(
        show = true,
        title = stringResource(R.string.kpm_control_dialog_title),
        summary = stringResource(R.string.kpm_control_dialog_content),
        onDismissRequest = onDismiss
    ) {
        TextField(
            value = controlParam,
            label = stringResource(id = R.string.kpm_control_paramters),
            onValueChange = {
                controlParam = it
                enable = controlParam.isNotBlank()
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                text = stringResource(android.R.string.cancel),
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            )

            Spacer(Modifier.width(20.dp))

            TextButton(
                text = stringResource(android.R.string.ok),
                onClick = {
                    onDismiss()
                    onConfirm(controlParam)
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
                enabled = enable
            )
        }
    }
}

@Composable
private fun KPModuleList(
    uiState: KPModuleUiState,
    onRefresh: () -> Unit,
    onUninstall: (KPModel.KPMInfo) -> Unit,
    onControl: (KPModel.KPMInfo) -> Unit,
    onUpdate: (KPModel.KPMInfo) -> Unit,
    onToggle: (KPModel.KPMInfo, Boolean) -> Unit,
    backdrop: LayerBackdrop?,
    state: LazyListState,
    scrollBehavior: ScrollBehavior,
    contentPadding: PaddingValues,
    bottomPadding: Dp,
    scaffoldPadding: PaddingValues = PaddingValues(),
) {
    val updates = uiState.updates
    Box(modifier = Modifier.padding(scaffoldPadding)) {
        PullToRefresh(
            isRefreshing = uiState.isRefreshing,
            contentPadding = contentPadding,
            refreshTexts = listOf(
                stringResource(R.string.refresh_pulling),
                stringResource(R.string.refresh_release),
                stringResource(R.string.refresh_refresh),
                stringResource(R.string.refresh_complete)
            ),
            onRefresh = onRefresh
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .withBackdrop(backdrop)
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                state = state,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(
                    top = contentPadding.calculateTopPadding() + 12.dp,
                    bottom = bottomPadding + 16.dp,
                    start = 16.dp,
                    end = 16.dp
                )
            ) {
                when {
                    uiState.filteredModules.isEmpty() && uiState.error != null -> {
                        item {
                            Box(
                                modifier = Modifier.fillParentMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    stringResource(R.string.list_load_failed),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    uiState.filteredModules.isEmpty() && !uiState.hasLoaded -> {
                        item {
                            Box(
                                modifier = Modifier.fillParentMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!uiState.isRefreshing) {
                                    Text(
                                        stringResource(R.string.loading),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    uiState.filteredModules.isEmpty() -> {
                        item {
                            Box(
                                modifier = Modifier.fillParentMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!uiState.isRefreshing) {
                                    Text(
                                        stringResource(R.string.kpm_apm_empty),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        items(
                            uiState.filteredModules,
                            key = { it.moduleId.ifBlank { it.name } }
                        ) { module ->
                            KPModuleItem(
                                module = module,
                                update = updates[module.moduleId],
                                onUninstall = { onUninstall(module) },
                                onControl = { onControl(module) },
                                onUpdate = { onUpdate(module) },
                                onToggle = { enabled -> onToggle(module, enabled) }
                            )

                            // fix last item shadow incomplete in LazyColumn
                            Spacer(Modifier.height(1.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KPModuleItem(
    module: KPModel.KPMInfo,
    update: KPModel.KpmUpdateInfo?,
    onUninstall: (KPModel.KPMInfo) -> Unit,
    onControl: (KPModel.KPMInfo) -> Unit,
    onUpdate: (KPModel.KPMInfo) -> Unit,
    onToggle: (Boolean) -> Unit,
    alpha: Float = 1f,
) {
    val moduleVersion = stringResource(id = R.string.kpm_version)
    val moduleAuthor = stringResource(id = R.string.kpm_author)
    val moduleArgs = stringResource(id = R.string.kpm_args)
    val decoration = TextDecoration.None

    Card {
        Box(
            modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = module.name,
                                fontSize = 17.sp,
                                fontWeight = FontWeight(550),
                                color = colorScheme.onSurface,
                                textDecoration = decoration,
                                modifier = Modifier.weight(1f, false)
                            )
                            when {
                                module.loadSource == "embedded" -> {
                                    Spacer(Modifier.width(6.dp))
                                    ChipLabel(stringResource(R.string.kpm_embed))
                                }

                                module.installed && module.disabled -> {
                                    Spacer(Modifier.width(6.dp))
                                    ChipLabel(stringResource(R.string.kpm_disabled))
                                }

                                module.installed -> {
                                    Spacer(Modifier.width(6.dp))
                                    ChipLabel(stringResource(R.string.kpm_installed))
                                }
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

                        Text(
                            text = "$moduleArgs: ${module.args}",
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 1.dp),
                            fontWeight = FontWeight(550),
                            color = colorScheme.onSurfaceVariantSummary,
                            textDecoration = decoration
                        )

                        if (update != null) {
                            Text(
                                text = stringResource(R.string.kpm_update_available, update.version),
                                fontSize = 12.sp,
                                color = colorScheme.primary
                            )
                        }
                    }

                    if (module.installed && module.loadSource != "embedded") {
                        Switch(
                            checked = !module.disabled,
                            onCheckedChange = onToggle
                        )
                    }
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
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    if (update != null) {
                        IconTextButton(
                            iconRes = MiuixIcons.Update,
                            textRes = R.string.kpm_update,
                            onClick = { onUpdate(module) },
                        )

                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    IconTextButton(
                        iconRes = MiuixIcons.Settings,
                        textRes = R.string.kpm_control,
                        onClick = { onControl(module) },
                    )

                    if (module.loadSource != "embedded") {
                        Spacer(modifier = Modifier.width(12.dp))

                        IconTextButton(
                            iconRes = MiuixIcons.Delete,
                            textRes = R.string.kpm_unload,
                            onClick = { onUninstall(module) },
                        )
                    }
                }
            }

        }
    }
}
