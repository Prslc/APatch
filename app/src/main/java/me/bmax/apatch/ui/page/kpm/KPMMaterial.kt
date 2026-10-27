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
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.outlined.Cloud
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import me.bmax.apatch.ui.component.dialog.rememberConfirmDialog
import me.bmax.apatch.ui.component.dialog.rememberLoadingDialog
import me.bmax.apatch.util.DownloadCancelHandle
import me.bmax.apatch.util.ModuleRepo
import me.bmax.apatch.util.reboot
import me.bmax.apatch.ui.component.searchbar.AppSearchBar
import me.bmax.apatch.ui.navigation.LocalNavigator
import me.bmax.apatch.ui.page.patch.PatchMode
import me.bmax.apatch.ui.theme.LocalEnableBlur
import me.bmax.apatch.ui.theme.getMaterial3AppBarColor
import me.bmax.apatch.ui.theme.material3BlurEffect
import me.bmax.apatch.ui.theme.rememberMaterial3BlurBackdrop
import me.bmax.apatch.ui.theme.withBackdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun KPModuleScreenMaterial(
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

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
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
    val backdrop =
        if (isCurrentPage) rememberMaterial3BlurBackdrop(LocalEnableBlur.current) else null

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
            Text(
                text = stringResource(id = R.string.kpm_kp_not_installed),
                style = MaterialTheme.typography.bodyMedium
            )
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
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            Column(modifier = Modifier.material3BlurEffect(backdrop)) {
                LargeFlexibleTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.kpm),
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    },
                    actions = {
                        IconButton(onClick = { navigator.navigateToOnline(kpmOnly = true) }) {
                            Icon(
                                imageVector = Icons.Outlined.Cloud,
                                contentDescription = stringResource(R.string.online_module)
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
                AppSearchBar(
                    query = uiState.search,
                    onQueryChange = { viewModel.updateSearch(it) },
                    placeholder = stringResource(R.string.search_modules),
                )
            }
        },
        floatingActionButton = {
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
                KPMFloatingActionButtonMaterial(
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .withBackdrop(backdrop),
                state = kpModuleListState,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(
                    top = paddingValues.calculateTopPadding() + 12.dp,
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
                            KPModuleItemMaterial(
                                module = module,
                                update = uiState.updates[module.moduleId],
                                onUninstall = {
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
                                onControl = { viewModel.openControlDialog(module) },
                                onUpdate = { scope.launch { onModuleUpdate(module) } },
                                onToggle = { enabled ->
                                    scope.launch { viewModel.setModuleDisabled(module, enabled) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    if (uiState.showControlDialog && uiState.controlTarget != null) {
        val target = uiState.controlTarget!!
        KPMControlDialogMaterial(
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun KPMFloatingActionButtonMaterial(
    onLoadModule: (Uri) -> Unit,
    onInstallModule: (Uri) -> Unit,
    onNavigateToPatches: () -> Unit,
    bottomPadding: Dp
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val selectKpmLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode != RESULT_OK) return@rememberLauncherForActivityResult
        it.data?.data?.let { uri -> onLoadModule(uri) }
    }

    val selectInstallLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode != RESULT_OK) return@rememberLauncherForActivityResult
        it.data?.data?.let { uri -> onInstallModule(uri) }
    }

    val moduleLoad = stringResource(id = R.string.kpm_load)
    val moduleInstall = stringResource(id = R.string.kpm_install)
    val moduleEmbed = stringResource(id = R.string.kpm_embed)

    Column {
        FloatingActionButton(
            onClick = { expanded = !expanded },
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = bottomPadding + 16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }

        DropdownMenuPopup(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuGroup(
                shapes = MenuDefaults.groupShape(index = 0, count = 1)
            ) {
                listOf(moduleEmbed, moduleInstall, moduleLoad).forEach { label ->
                    DropdownMenuItem(
                        onClick = {
                            when (label) {
                                moduleEmbed -> onNavigateToPatches()
                                moduleInstall -> {
                                    val intent =
                                        Intent(Intent.ACTION_GET_CONTENT).apply { type = "*/*" }
                                    selectInstallLauncher.launch(intent)
                                }

                                moduleLoad -> {
                                    val intent =
                                        Intent(Intent.ACTION_GET_CONTENT).apply { type = "*/*" }
                                    selectKpmLauncher.launch(intent)
                                }
                            }
                            expanded = false
                        },
                        text = { Text(label) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KPMControlDialogMaterial(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var controlParam by remember { mutableStateOf("") }
    var enable by remember { mutableStateOf(false) }

    BasicAlertDialog(
        onDismissRequest = onDismiss
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
                    .padding(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.kpm_control_dialog_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.kpm_control_dialog_content),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = controlParam,
                    onValueChange = {
                        controlParam = it
                        enable = controlParam.isNotBlank()
                    },
                    label = { Text(stringResource(id = R.string.kpm_control_paramters)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text(stringResource(android.R.string.cancel))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onDismiss()
                            onConfirm(controlParam)
                        },
                        enabled = enable
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            }
        }
    }
}

@Composable
private fun KPModuleItemMaterial(
    module: KPModel.KPMInfo,
    update: KPModel.KpmUpdateInfo?,
    onUninstall: (KPModel.KPMInfo) -> Unit,
    onControl: (KPModel.KPMInfo) -> Unit,
    onUpdate: (KPModel.KPMInfo) -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    val moduleVersion = stringResource(id = R.string.kpm_version)
    val moduleAuthor = stringResource(id = R.string.kpm_author)
    val moduleArgs = stringResource(id = R.string.kpm_args)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceBright
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = module.name,
                            fontSize = MaterialTheme.typography.titleMedium.fontSize,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
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
                }
                if (module.installed && module.loadSource != "embedded") {
                    Switch(
                        checked = !module.disabled,
                        onCheckedChange = onToggle
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "$moduleVersion: ${module.version}",
                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$moduleAuthor: ${module.author}",
                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$moduleArgs: ${module.args}",
                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (update != null) {
                Text(
                    text = stringResource(R.string.kpm_update_available, update.version),
                    fontSize = MaterialTheme.typography.labelSmall.fontSize,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = module.description,
                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                overflow = TextOverflow.Ellipsis,
                maxLines = 4,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            HorizontalDivider(thickness = Dp.Hairline)

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (update != null) {
                    ModuleActionButtonMaterial(
                        icon = Icons.Filled.Update,
                        text = stringResource(R.string.kpm_update),
                        onClick = { onUpdate(module) }
                    )
                    Spacer(Modifier.width(8.dp))
                }
                ModuleActionButtonMaterial(
                    icon = Icons.Filled.Settings,
                    text = stringResource(R.string.kpm_control),
                    onClick = { onControl(module) }
                )
                if (module.loadSource != "embedded") {
                    Spacer(Modifier.width(8.dp))
                    ModuleActionButtonMaterial(
                        icon = Icons.Filled.Delete,
                        text = stringResource(R.string.kpm_unload),
                        onClick = { onUninstall(module) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ModuleActionButtonMaterial(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        modifier = Modifier.defaultMinSize(minWidth = 52.dp, minHeight = 32.dp),
        onClick = onClick,
        contentPadding = ButtonDefaults.TextButtonContentPadding,
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            imageVector = icon,
            contentDescription = null
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight(500)
        )
    }
}
