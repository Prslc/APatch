package me.bmax.apatch.ui.page.online

import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.ScrollToTopOnChange
import me.bmax.apatch.ui.component.dialog.ConfirmResult
import me.bmax.apatch.ui.component.dialog.ModuleDownloadProgressHost
import me.bmax.apatch.ui.component.dialog.rememberConfirmDialog
import me.bmax.apatch.ui.component.searchbar.AppSearchBar
import me.bmax.apatch.ui.navigation.LocalNavigator
import me.bmax.apatch.ui.navigation.MODULE_TYPE
import me.bmax.apatch.ui.theme.blurEffect
import me.bmax.apatch.ui.theme.getAppBarColor
import me.bmax.apatch.ui.theme.rememberBlurBackdrop
import me.bmax.apatch.ui.theme.withBackdrop
import me.bmax.apatch.util.DownloadCancelHandle
import me.bmax.apatch.util.ModuleRepo
import me.bmax.apatch.util.reboot
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

private val StarColor = Color(0xFFFFB300)

@Composable
fun OnlineScreenMiuix(
    kpmOnly: Boolean,
    onBack: () -> Unit,
    viewModel: OnlineViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollBehavior = MiuixScrollBehavior()
    val listState = rememberLazyListState()
    val confirmDialog = rememberConfirmDialog()
    val backdrop = rememberBlurBackdrop()

    val titleText = stringResource(
        if (kpmOnly) R.string.online_kpm_title else R.string.online_apm_title
    )
    val installText = stringResource(R.string.online_install)
    val updateText = stringResource(R.string.online_update)
    val cancelText = stringResource(android.R.string.cancel)
    val rebootText = stringResource(R.string.reboot)
    val downloadFailedText = stringResource(R.string.online_download_failed)
    val downloadCancelledText = stringResource(R.string.online_download_cancelled)
    val installFailedText = stringResource(R.string.online_install_failed)
    val updateSuccessText = stringResource(R.string.kpm_update_succ)
    val installSuccessText = stringResource(R.string.kpm_install_toast_succ)
    val updateConfirmText = stringResource(R.string.online_update_confirm)
    val installConfirmText = stringResource(R.string.online_install_confirm)

    val state by APApplication.apStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)
    val kpatchReady = state != APApplication.State.UNKNOWN_STATE

    LaunchedEffect(Unit) { viewModel.onEnter(kpmOnly) }

    ScrollToTopOnChange(
        listState,
        uiState.search,
        isBusy = { uiState.isRefreshing },
        observedList = { uiState.modules }
    )

    val downloadTask = remember { mutableStateOf<Pair<String, Int>?>(null) }
    var downloadHandle by remember { mutableStateOf<DownloadCancelHandle?>(null) }
    var detailModule by remember { mutableStateOf<OnlineModule?>(null) }

    suspend fun install(module: OnlineModule, doc: ModuleRepo.RepoModuleDoc) {
        if (doc.downloadUrl.isBlank()) return
        val canUpdate = module.installedModule?.let {
            ModuleRepo.isNewerVersion(doc, it.version, it.versionCode)
        } == true
        val name = doc.name.ifBlank { module.id }
        val confirmed = confirmDialog.awaitConfirm(
            title = if (canUpdate) updateText else installText,
            content = if (canUpdate) {
                updateConfirmText.format(name, module.installedVersion, doc.version)
            } else {
                installConfirmText.format(
                    listOf(name, doc.version)
                        .filter { it.isNotBlank() }
                        .joinToString(" ")
                )
            },
            confirm = if (canUpdate) updateText else installText,
            dismiss = cancelText
        )
        if (confirmed != ConfirmResult.Confirmed) return

        val fileName = "${module.id}-${doc.version}.${if (module.kpm) "kpm" else "zip"}"
        val handle = DownloadCancelHandle()
        downloadHandle = handle
        downloadTask.value = module.id to 0
        val file = try {
            ModuleRepo.downloadModuleFile(
                id = module.id,
                kpm = module.kpm,
                fileName = fileName,
                authorUrl = doc.downloadUrl,
                cancel = handle
            ) { percent -> downloadTask.value = module.id to percent }
        } finally {
            downloadTask.value = null
            downloadHandle = null
        }
        if (handle.isCancelled) {
            Toast.makeText(context, downloadCancelledText, Toast.LENGTH_SHORT).show()
            return
        }
        if (file == null) {
            Toast.makeText(context, downloadFailedText, Toast.LENGTH_SHORT).show()
            return
        }

        if (module.kpm) {
            val rc = viewModel.installKpm(Uri.fromFile(file))
            file.delete()
            if (rc == 0) {
                viewModel.refresh()
                val rebootConfirmed = confirmDialog.awaitConfirm(
                    title = rebootText,
                    content = if (canUpdate) {
                        updateSuccessText.format(doc.version)
                    } else {
                        installSuccessText
                    },
                    confirm = rebootText,
                    dismiss = cancelText
                )
                if (rebootConfirmed == ConfirmResult.Confirmed) {
                    withContext(Dispatchers.IO) { reboot() }
                }
            } else {
                Toast.makeText(context, installFailedText.format(rc), Toast.LENGTH_SHORT).show()
            }
        } else {
            navigator.navigateToInstall(Uri.fromFile(file), MODULE_TYPE.APM)
            viewModel.refresh()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                modifier = Modifier.blurEffect(backdrop),
                color = backdrop.getAppBarColor(),
                title = titleText,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = null,
                            tint = colorScheme.onSurface
                        )
                    }
                }
            ) {
                AppSearchBar(
                    query = uiState.search,
                    onQueryChange = viewModel::onSearchChange,
                    placeholder = stringResource(R.string.search_modules)
                )
            }
        }
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
            onRefresh = viewModel::refresh,
            contentPadding = innerPadding
        ) {
            OnlineModuleList(
                state = uiState.listState,
                modules = uiState.modules,
                hasMore = uiState.hasMore,
                isLoadingMore = uiState.isLoadingMore,
                errorMessage = uiState.errorMessage,
                contentPadding = innerPadding,
                backdrop = backdrop,
                listState = listState,
                scrollBehavior = scrollBehavior,
                onOpenDetail = { module -> detailModule = module },
                onLoadMore = viewModel::loadMore,
                onRetry = viewModel::refresh
            )
        }
    }

    detailModule?.let { module ->
        OnlineModuleDetailDialog(
            module = module,
            kpatchReady = kpatchReady,
            cachedDoc = viewModel.cachedDoc(module),
            loadDoc = { viewModel.moduleDocResult(module) },
            onDismiss = { detailModule = null },
            onInstall = { doc ->
                detailModule = null
                scope.launch { install(module, doc) }
            }
        )
    }

    ModuleDownloadProgressHost(downloadTask) { downloadHandle?.cancel() }
}

@Composable
private fun OnlineModuleList(
    state: OnlineListState,
    modules: List<OnlineModule>,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    errorMessage: String,
    contentPadding: PaddingValues,
    backdrop: LayerBackdrop?,
    listState: LazyListState,
    scrollBehavior: ScrollBehavior,
    onOpenDetail: (OnlineModule) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .withBackdrop(backdrop)
            .overScrollVertical()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 16.dp,
            bottom = 32.dp,
            start = 16.dp,
            end = 16.dp
        )
    ) {
        when (state) {
            OnlineListState.LOADING -> item {
                Box(modifier = Modifier.fillParentMaxSize())
            }

            OnlineListState.ERROR -> item {
                Box(
                    modifier = Modifier.fillParentMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.online_load_failed),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = errorMessage,
                            fontSize = 12.sp,
                            color = colorScheme.onSurfaceVariantSummary,
                            textAlign = TextAlign.Center
                        )
                        TextButton(
                            text = stringResource(R.string.online_retry),
                            onClick = onRetry
                        )
                    }
                }
            }

            OnlineListState.EMPTY -> item {
                Box(
                    modifier = Modifier.fillParentMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.online_empty),
                        textAlign = TextAlign.Center
                    )
                }
            }

            OnlineListState.CONTENT -> items(
                modules,
                key = { "${it.kpm}:${it.id}" }
            ) { module ->
                OnlineModuleItem(
                    module = module,
                    onOpen = { onOpenDetail(module) }
                )
            }
        }

        if (state == OnlineListState.CONTENT && hasMore) {
            item {
                LaunchedEffect(modules.size, hasMore) { onLoadMore() }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoadingMore) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
private fun OnlineModuleItem(
    module: OnlineModule,
    onOpen: () -> Unit
) {
    Card(modifier = Modifier.clickable { onOpen() }) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = module.id,
                            fontSize = 17.sp,
                            fontWeight = FontWeight(550),
                            color = colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, false)
                        )
                    }

                    if (module.author.isNotBlank()) {
                        Text(
                            text = module.author,
                            fontSize = 12.sp,
                            color = colorScheme.onSurfaceVariantSummary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (module.installed) {
                        Text(
                            text = if (module.installedVersion.isBlank()) {
                                stringResource(R.string.online_installed)
                            } else {
                                stringResource(
                                    R.string.online_installed_version,
                                    module.installedVersion
                                )
                            },
                            fontSize = 12.sp,
                            color = colorScheme.primary
                        )
                    }
                }

                if (module.stars > 0) {
                    val stars = ModuleRepo.formatStars(module.stars)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = stringResource(R.string.online_stars, stars),
                            modifier = Modifier.size(13.dp),
                            tint = StarColor
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = stars,
                            fontSize = 12.sp,
                            color = colorScheme.onSurfaceVariantSummary
                        )
                    }
                }
            }

            if (module.summary.isNotBlank()) {
                Text(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    text = module.summary,
                    fontSize = 14.sp,
                    color = colorScheme.onSurfaceVariantSummary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
