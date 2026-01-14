package me.bmax.apatch.ui.screen

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.generated.destinations.ExecuteAPMActionScreenDestination
import com.ramcosta.composedestinations.generated.destinations.InstallScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.topjohnwu.superuser.io.SuFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.apApp
import me.bmax.apatch.ui.WebUIActivity
import me.bmax.apatch.ui.component.ConfirmResult
import me.bmax.apatch.ui.component.IconTextButton
import me.bmax.apatch.ui.component.LoadingIndicator
import me.bmax.apatch.ui.component.ModuleStateIndicator
import me.bmax.apatch.ui.component.WarningCard
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.ui.component.rememberLoadingDialog
import me.bmax.apatch.ui.viewmodel.APModuleViewModel
import me.bmax.apatch.util.DownloadListener
import me.bmax.apatch.util.download
import me.bmax.apatch.util.hasMagisk
import me.bmax.apatch.util.toggleModule
import me.bmax.apatch.util.undoUninstallModule
import me.bmax.apatch.util.uninstallModule
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.HorizontalSplit
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.icon.extended.Undo
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

@Composable
fun APModuleScreen(
    bottomPadding: Dp,
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    val scrollBehavior = MiuixScrollBehavior()
    var expanded by remember { mutableStateOf(false) }

    val state by APApplication.apStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)
    if (state != APApplication.State.ANDROIDPATCH_INSTALLED && state != APApplication.State.ANDROIDPATCH_NEED_UPDATE) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row {
                Text(
                    text = stringResource(id = R.string.apm_not_installed),
                    style = MiuixTheme.textStyles.body2
                )
            }
        }
        return
    }

    val viewModel = viewModel<APModuleViewModel>()

    LaunchedEffect(Unit) {
        if (viewModel.moduleList.isEmpty() || viewModel.isNeedRefresh) {
            viewModel.fetchModuleList()
        }
    }
    val webUILauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { viewModel.fetchModuleList() }
    //TODO: FIXME -> val isSafeMode = Natives.getSafeMode()
    val isSafeMode = false
    val hasMagisk = hasMagisk()
    val hideInstallButton = isSafeMode || hasMagisk

    val moduleListState = rememberLazyListState()

    Scaffold(
        modifier = Modifier.padding(bottom = bottomPadding),
        topBar = {
            TopAppBar(
                title = stringResource(R.string.apm),
                scrollBehavior = scrollBehavior
            )
        }, floatingActionButton = if (hideInstallButton) {
            { /* Empty */ }
        } else {
            {
                val selectZipLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) {
                    if (it.resultCode != RESULT_OK) {
                        return@rememberLauncherForActivityResult
                    }
                    val data = it.data ?: return@rememberLauncherForActivityResult
                    val uri = data.data ?: return@rememberLauncherForActivityResult

                    Log.i("ModuleScreen", "select zip result: $uri")

                    navigator.navigate(InstallScreenDestination(uri, MODULE_TYPE.APM))

                    viewModel.markNeedRefresh()
                }

                FloatingActionButton(
                    containerColor = colorScheme.primary,
                    modifier = Modifier.padding(bottom = 30.dp),
                    onClick = {
                        // select the zip file to install
                        val intent = Intent(Intent.ACTION_GET_CONTENT)
                        intent.type = "application/zip"
                        selectZipLauncher.launch(intent)
                    }) {
                    Icon(
                        imageVector = MiuixIcons.Add,
                        contentDescription = null,
                        tint = colorScheme.onPrimary
                    )
                }
            }
        }, popupHost = {}
        ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 5.dp, bottom = 16.dp)
                    .zIndex(10f)
            ) {
                SearchBar(
                    inputField = {
                        InputField(
                            query = viewModel.search,
                            onQueryChange = { viewModel.search = it },
                            onSearch = { expanded = false },
                            expanded = expanded,
                            onExpandedChange = {
                                expanded = it
                                if (!it) viewModel.search = ""
                            }
                        )
                    },
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    content = {}
                )
            }
            when {
                hasMagisk -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.apm_magisk_conflict),
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                else -> {
                    ModuleList(
                        navigator,
                        viewModel = viewModel,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        state = moduleListState,
                        onInstallModule = {
                            navigator.navigate(InstallScreenDestination(it, MODULE_TYPE.APM))
                        },
                        onClickModule = { id, name, hasWebUi ->
                            if (hasWebUi) {
                                webUILauncher.launch(
                                    Intent(
                                        context, WebUIActivity::class.java
                                    ).setData("apatch://webui/$id".toUri()).putExtra("id", id)
                                        .putExtra("name", name)
                                )
                            }
                        },
                        context = context,
                        scrollBehavior = scrollBehavior
                    )
                }
            }
        }
    }
}

@Composable
private fun MetaModuleWarningCard(
    viewModel: APModuleViewModel
) {
    val hasSystemModule = viewModel.moduleList.any { module ->
        SuFile.open("/data/adb/modules/${module.id}/system").exists()
    }


    if (!hasSystemModule) return

    val metaProp = SuFile.open("/data/adb/metamodule/module.prop").exists()
    val metaRemoved = SuFile.open("/data/adb/metamodule/remove").exists()
    val metaDisabled = SuFile.open("/data/adb/metamodule/disable").exists()

    val warningText = when {
        !metaProp ->
            stringResource(R.string.no_meta_module_installed)

        metaProp && metaRemoved ->
            stringResource(R.string.meta_module_removed)

        metaProp && metaDisabled ->
            stringResource(R.string.meta_module_disabled)

        else -> null
    }

    if (warningText == null) return
    var show by remember { mutableStateOf(true) }

        WarningCard(
            message = warningText,
            onClose = {
                show = false
            }
        )

        Spacer(Modifier.height(8.dp))
}

@Composable
private fun ModuleList(
    navigator: DestinationsNavigator,
    viewModel: APModuleViewModel,
    modifier: Modifier = Modifier,
    state: LazyListState,
    onInstallModule: (Uri) -> Unit,
    onClickModule: (id: String, name: String, hasWebUi: Boolean) -> Unit,
    context: Context,
    scrollBehavior: ScrollBehavior
) {
    val failedEnable = stringResource(R.string.apm_failed_to_enable)
    val failedDisable = stringResource(R.string.apm_failed_to_disable)
    val failedUninstall = stringResource(R.string.apm_uninstall_failed)
    val failedUndoUninstall = stringResource(R.string.apm_module_undo_uninstall_failed)
    val successUninstall = stringResource(R.string.apm_uninstall_success)
    val successUndoUninstall = stringResource(R.string.apm_uninstall_success)
    val rebootToApply = stringResource(id = R.string.apm_reboot_to_apply)
    val moduleStr = stringResource(id = R.string.apm)
    val uninstall = stringResource(id = R.string.apm_remove)
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

    suspend fun onModuleUpdate(
        module: APModuleViewModel.ModuleInfo,
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
                                    okhttp3.Request.Builder().url(changelogUrl).build()
                                )
                                .execute()
                                .use { it.body?.string().orEmpty() }
                        } else {
                            changelogUrl
                        }
                    }.getOrDefault("")
                }
        }


        val confirmResult = confirmDialog.awaitConfirm(
            changelogText,
            content = changelog.ifEmpty { changelogFailed },
            markdown = true,
            confirm = updateText,
        )

        if (confirmResult != ConfirmResult.Confirmed){
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

    suspend fun onModuleUninstall(module: APModuleViewModel.ModuleInfo) {
        val formatter = if (module.metamodule) metaModuleUninstallConfirm else moduleUninstallConfirm
        val confirmResult = confirmDialog.awaitConfirm(
            moduleStr,
            content = formatter.format(module.name),
            confirm = uninstall,
            dismiss = cancel
        )
        if (confirmResult != ConfirmResult.Confirmed) {
            return
        }

        val success = withContext(Dispatchers.IO) {
            uninstallModule(module.id)
        }

        if (success) {
            viewModel.fetchModuleList()
        }

        val message = if (success) {
            successUninstall.format(module.name)
        } else {
            failedUninstall.format(module.name)
        }

        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    suspend fun onUndoModuleUninstall(module: APModuleViewModel.ModuleInfo) {
        val success = withContext(Dispatchers.IO) {
                undoUninstallModule(module.id)
        }

        if (success) {
            viewModel.fetchModuleList()
        }

        val message = if (success) {
            successUndoUninstall.format(module.name)
        } else {
            failedUndoUninstall.format(module.name)
        }

        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }


    PullToRefresh(
        modifier = modifier,
        isRefreshing = viewModel.isRefreshing,
        onRefresh = { viewModel.fetchModuleList() },
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            state = state,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = remember {
                PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp /*  Scaffold Fab Spacing + Fab container height */
                )
            },
        ) {
            item {
                MetaModuleWarningCard(viewModel)
            }
            when {
                viewModel.moduleList.isEmpty() -> {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (viewModel.isRefreshing) {
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
                    items(viewModel.moduleList) { module ->
                        val scope = rememberCoroutineScope()
                        val updatedModule by produceState(initialValue = Triple("", "", "")) {
                            scope.launch(Dispatchers.IO) {
                                value = viewModel.checkUpdate(module)
                            }
                        }

                        ModuleItem(
                            navigator,
                            module,
                            updatedModule.first,
                            onUninstall = {
                                scope.launch { onModuleUninstall(module) }
                            },
                            onUndoUninstall = {
                                scope.launch { onUndoModuleUninstall(module) }
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

                                        Toast.makeText(context, rebootToApply, Toast.LENGTH_LONG).show()

                                    } else {
                                        val message = if (module.enabled) failedDisable else failedEnable
                                        Toast.makeText(context, message.format(module.name), Toast.LENGTH_LONG).show()
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
                            })
                        // fix last item shadow incomplete in LazyColumn
                        Spacer(Modifier.height(1.dp))
                    }
                }
            }
        }

        DownloadListener(context, onInstallModule)
    }
}

@Composable
private fun ModuleItem(
    navigator: DestinationsNavigator,
    module: APModuleViewModel.ModuleInfo,
    updateUrl: String,
    onUninstall: (APModuleViewModel.ModuleInfo) -> Unit,
    onUndoUninstall: (APModuleViewModel.ModuleInfo) -> Unit,
    onCheckChanged: (Boolean) -> Unit,
    onUpdate: (APModuleViewModel.ModuleInfo) -> Unit,
    onClick: (APModuleViewModel.ModuleInfo) -> Unit,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
) {
    val decoration = if (!module.remove) TextDecoration.None else TextDecoration.LineThrough
    val moduleVersion = stringResource(id = R.string.apm_version)
    val moduleAuthor = stringResource(id = R.string.apm_author)
    val viewModel = viewModel<APModuleViewModel>()
    Card(modifier = modifier)
    {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick(module) },
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
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                            color = colorScheme.onTertiaryContainer,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }.first().measure(Constraints(0, constraints.maxWidth, 0, constraints.maxHeight))
                            } else null

                            val reserved = (metaPlaceable?.width ?: 0) + if (metaPlaceable != null) spacingPx else 0
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
                            }.first().measure(Constraints(constraints.minWidth, nameMax, constraints.minHeight, constraints.maxHeight))

                            val width = (namePlaceable.width + reserved).coerceIn(constraints.minWidth, constraints.maxWidth)
                            val height = maxOf(namePlaceable.height, metaPlaceable?.height ?: 0)

                            layout(width, height) {
                                namePlaceable.placeRelative(0, 0)
                                val endX = nameTextLayout?.let { layoutRes ->
                                    val last = (layoutRes.lineCount - 1).coerceAtLeast(0)
                                    layoutRes.getLineRight(last).toInt()
                                } ?: namePlaceable.width
                                metaPlaceable?.placeRelative(endX + spacingPx, (height - (metaPlaceable.height)) / 2)
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
                    modifier = Modifier.padding(vertical = 8.dp)
                        .padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = colorScheme.outline.copy(alpha = 0.5f)
                )

                Row(
                    modifier = Modifier.padding(horizontal = 16.dp)
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
                                navigator.navigate(ExecuteAPMActionScreenDestination(module.id))
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
                            iconRes =  MiuixIcons.Download,
                            textRes = R.string.apm_update,
                            onClick = { onUpdate(module) }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    if (!module.remove) {
                        IconTextButton(
                            textRes = R.string.apm_remove,
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
