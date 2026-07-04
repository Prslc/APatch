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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.ConfirmResult
import me.bmax.apatch.ui.component.DropdownItem
import me.bmax.apatch.ui.component.IconTextButton
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.ui.component.rememberLoadingDialog
import me.bmax.apatch.ui.navigation.LocalNavigator
import me.bmax.apatch.ui.page.patch.PatchMode
import me.bmax.apatch.ui.theme.blurEffect
import me.bmax.apatch.ui.theme.getAppBarColor
import me.bmax.apatch.ui.theme.rememberBlurBackdrop
import me.bmax.apatch.ui.theme.withBackdrop
import me.bmax.apatch.util.controlKernelModule
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
import top.yukonga.miuix.kmp.blur.LayerBackdrop
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

@Composable
fun KPModuleScreen(
    modifier: Modifier,
    bottomPadding: Dp,
    isCurrentPage: Boolean = true,
    viewModel: KPModuleViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val confirmDialog = rememberConfirmDialog()

    val scrollBehavior = MiuixScrollBehavior()
    val kpModuleListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    var fabVisible by remember { mutableStateOf(true) }

    val navigator = LocalNavigator.current
    val backdrop = if (isCurrentPage) rememberBlurBackdrop() else null

    val moduleStr = stringResource(id = R.string.kpm)
    val moduleUninstallConfirm = stringResource(id = R.string.kpm_unload_confirm)
    val unloadText = stringResource(R.string.kpm_unload)
    val cancelText = stringResource(android.R.string.cancel)
    val successToastText = stringResource(id = R.string.kpm_load_toast_succ)
    val failToastText = stringResource(id = R.string.kpm_load_toast_failed)

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
            )
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
                    onInstallModule = { /*TODO*/ },
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
                        viewModel.uninstallModule(module.name)
                    }
                }
            },
            onControl = { module ->
                viewModel.openControlDialog(module)
            },
            backdrop = backdrop,
            state = kpModuleListState,
            scrollBehavior = scrollBehavior,
            contentPadding = innerPadding,
            bottomPadding = bottomPadding
        )
    }
    if (uiState.showControlDialog && uiState.controlTarget != null) {
        KPMControlDialog(
            module = uiState.controlTarget!!,
            onDismiss = { viewModel.closeControlDialog() }
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
                                    Toast.makeText(context, "Under development", Toast.LENGTH_SHORT)
                                        .show()
                                    // val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "application/zip" }
                                    // selectZipLauncher.launch(intent)
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
    module: KPModel.KPMInfo,
    onDismiss: () -> Unit
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
                    scope.launch {
                        onModuleControl(module)
                    }
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
    backdrop: LayerBackdrop?,
    state: LazyListState,
    scrollBehavior: ScrollBehavior,
    contentPadding: PaddingValues,
    bottomPadding: Dp,
    scaffoldPadding: PaddingValues = PaddingValues(),
) {
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
                    uiState.modules.isEmpty() -> {
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
                        items(uiState.modules, key = { it.name }) { module ->
                            KPModuleItem(
                                module,
                                onUninstall = { onUninstall(module) },
                                onControl = { onControl(module) }
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
