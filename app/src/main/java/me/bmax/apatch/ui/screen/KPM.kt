package me.bmax.apatch.ui.screen

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.generated.destinations.InstallScreenDestination
import com.ramcosta.composedestinations.generated.destinations.PatchesDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.ConfirmResult
import me.bmax.apatch.ui.component.DropdownItem
import me.bmax.apatch.ui.component.IconTextButton
import me.bmax.apatch.ui.component.LoadingIndicator
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.ui.component.rememberLoadingDialog
import me.bmax.apatch.ui.theme.blurEffect
import me.bmax.apatch.ui.theme.getAppBarColor
import me.bmax.apatch.ui.theme.rememberBlurBackdrop
import me.bmax.apatch.ui.viewmodel.KPModel
import me.bmax.apatch.ui.viewmodel.KPModuleViewModel
import me.bmax.apatch.ui.viewmodel.PatchesViewModel
import me.bmax.apatch.util.controlKernelModule
import me.bmax.apatch.util.loadKernelModule
import me.bmax.apatch.util.unloadKernelModule
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.window.WindowDialog
import top.yukonga.miuix.kmp.window.WindowListPopup

private const val TAG = "KernelPatchModule"
private lateinit var targetKPMToControl: KPModel.KPMInfo

@Composable
fun KPModuleScreen(
    bottomPadding: Dp,
    navigator: DestinationsNavigator
) {
    val backdrop = rememberBlurBackdrop(true)

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

    val scrollBehavior = MiuixScrollBehavior()
    val viewModel = viewModel<KPModuleViewModel>()
    val controlDialogState = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (viewModel.moduleList.isEmpty() || viewModel.isNeedRefresh) {
            viewModel.fetchModuleList()
        }
    }

    val kpModuleListState = rememberLazyListState()

    Box {
        Scaffold(
            topBar = {
                TopAppBar(
                    modifier = Modifier.blurEffect(backdrop),
                    color = backdrop.getAppBarColor(),
                    title = stringResource(R.string.kpm),
                    scrollBehavior = scrollBehavior,
                )
            },
            floatingActionButton = {
                val scope = rememberCoroutineScope()
                val context = LocalContext.current

                val moduleLoad = stringResource(id = R.string.kpm_load)
                val moduleInstall = stringResource(id = R.string.kpm_install)
                val moduleEmbed = stringResource(id = R.string.kpm_embed)
                val successToastText = stringResource(id = R.string.kpm_load_toast_succ)
                val failToastText = stringResource(id = R.string.kpm_load_toast_failed)
                val loadingDialog = rememberLoadingDialog()

                val selectZipLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) {
                    if (it.resultCode != RESULT_OK) return@rememberLauncherForActivityResult
                    val data = it.data ?: return@rememberLauncherForActivityResult
                    val uri = data.data ?: return@rememberLauncherForActivityResult
                    Log.i(TAG, "select zip result: $uri")
                    navigator.navigate(InstallScreenDestination(uri, MODULE_TYPE.KPM))
                }

                val selectKpmLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) {
                    if (it.resultCode != RESULT_OK) return@rememberLauncherForActivityResult
                    val data = it.data ?: return@rememberLauncherForActivityResult
                    val uri = data.data ?: return@rememberLauncherForActivityResult

                    scope.launch {
                        val rc = loadingDialog.withLoading {
                            loadKernelModule(uri, "")
                        }
                        val toastText = if (rc == 0) successToastText else "$failToastText: $rc"
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
                        }
                        viewModel.markNeedRefresh()
                        viewModel.fetchModuleList()
                    }
                }

                val expanded = remember { mutableStateOf(false) }
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
                                            moduleEmbed -> navigator.navigate(
                                                PatchesDestination(PatchesViewModel.PatchMode.PATCH_AND_INSTALL)
                                            )

                                            moduleInstall -> {
//                                            val intent = Intent(Intent.ACTION_GET_CONTENT)
//                                            intent.type = "application/zip"
//                                            selectZipLauncher.launch(intent)
                                                Toast.makeText(
                                                    context,
                                                    "Under development",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }

                                            moduleLoad -> {
                                                val intent = Intent(Intent.ACTION_GET_CONTENT)
                                                intent.type = "*/*"
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
        ) { innerPadding ->
            KPModuleList(
                viewModel = viewModel,
                backdrop = backdrop,
                state = kpModuleListState,
                scrollBehavior = scrollBehavior,
                contentPadding = innerPadding,
                bottomPadding = bottomPadding,
                onShowControlDialog = { controlDialogState.value = true }
            )
        }
        if (controlDialogState.value) {
            KPMControlDialog(controlDialog = controlDialogState)
        }
    }
}

@Composable
fun KPMControlDialog(
    controlDialog: MutableState<Boolean>
) {
    var controlParam by remember { mutableStateOf("") }
    var enable by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val loadingDialog = rememberLoadingDialog()
    val context = LocalContext.current
    val outMsgStringRes = stringResource(id = R.string.kpm_control_outMsg)
    val okStringRes = stringResource(id = R.string.kpm_control_ok)
    val failedStringRes = stringResource(id = R.string.kpm_control_failed)

    suspend fun onModuleControl(module: KPModel.KPMInfo) {
        val controlResult = loadingDialog.withLoading {
            controlKernelModule(module.name, controlParam)
        }

        if (controlResult.rc >= 0) {
            Toast.makeText(
                context,
                "$okStringRes\n${outMsgStringRes}: ${controlResult.outMsg}",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                context,
                "$failedStringRes\n${outMsgStringRes}: ${controlResult.outMsg}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    WindowDialog(
        show = controlDialog.value,
        title = stringResource(R.string.kpm_control_dialog_title),
        summary = stringResource(R.string.kpm_control_dialog_content),
        onDismissRequest = { controlDialog.value = false }
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
                onClick = {
                    controlDialog.value = false
                },
                modifier = Modifier.weight(1f),
            )

            Spacer(Modifier.width(20.dp))

            TextButton(
                text = stringResource(android.R.string.ok),
                onClick = {
                    controlDialog.value = false
                    scope.launch { onModuleControl(targetKPMToControl) }
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
    viewModel: KPModuleViewModel,
    backdrop: LayerBackdrop?,
    state: LazyListState,
    scrollBehavior: ScrollBehavior,
    contentPadding: PaddingValues,
    bottomPadding: Dp,
    scaffoldPadding: PaddingValues = PaddingValues(),
    onShowControlDialog: () -> Unit = {}
) {
    val moduleStr = stringResource(id = R.string.kpm)
    val moduleUninstallConfirm = stringResource(id = R.string.kpm_unload_confirm)
    val uninstall = stringResource(id = R.string.kpm_unload)
    val cancel = stringResource(id = android.R.string.cancel)

    val confirmDialog = rememberConfirmDialog()
    val loadingDialog = rememberLoadingDialog()

    val pullToRefreshState = rememberPullToRefreshState()

    suspend fun onModuleUninstall(module: KPModel.KPMInfo) {
        val confirmResult = confirmDialog.awaitConfirm(
            moduleStr,
            content = moduleUninstallConfirm.format(module.name),
            confirm = uninstall,
            dismiss = cancel
        )
        if (confirmResult != ConfirmResult.Confirmed) {
            return
        }

        val success = loadingDialog.withLoading {
            unloadKernelModule(module.name)
        }

        if (success) {
            viewModel.fetchModuleList()
        }
    }
    Box(modifier = Modifier.padding(scaffoldPadding)) {
        PullToRefresh(
            isRefreshing = viewModel.isRefreshing,
            pullToRefreshState = pullToRefreshState,
            refreshTexts = listOf(
                stringResource(R.string.refresh_pulling),
                stringResource(R.string.refresh_release),
                stringResource(R.string.refresh_refresh),
                stringResource(R.string.refresh_complete)
            ),
            onRefresh = { viewModel.fetchModuleList() }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier)
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                state = state,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(
                    top = contentPadding.calculateTopPadding() + 12.dp,
                    bottom = bottomPadding + 16.dp + 60.dp,   /*  Scaffold Fab Spacing + Fab container height */
                    start = 16.dp,
                    end = 16.dp
                )
            ) {
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
                                        stringResource(R.string.kpm_apm_empty),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        items(viewModel.moduleList) { module ->
                            val scope = rememberCoroutineScope()
                            KPModuleItem(
                                module,
                                onUninstall = {
                                    scope.launch { onModuleUninstall(module) }
                                },
                                onControl = {
                                    targetKPMToControl = module
                                    onShowControlDialog()
                                },
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
    onUninstall: (KPModel.KPMInfo) -> Unit,
    onControl: (KPModel.KPMInfo) -> Unit,
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
                        Text(
                            text = module.name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight(550),
                            color = colorScheme.onSurface,
                            textDecoration = decoration
                        )

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
                    IconTextButton(
                        iconRes = MiuixIcons.Settings,
                        textRes = R.string.kpm_control,
                        onClick = { onControl(module) },
                    )

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
