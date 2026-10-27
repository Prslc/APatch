package me.bmax.apatch.ui.page.online

import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.ScrollToTopOnChange
import me.bmax.apatch.ui.component.dialog.ConfirmResult
import me.bmax.apatch.ui.component.dialog.ModuleDownloadProgressDialog
import me.bmax.apatch.ui.component.dialog.rememberConfirmDialog
import me.bmax.apatch.ui.component.searchbar.AppSearchBar
import me.bmax.apatch.ui.navigation.LocalNavigator
import me.bmax.apatch.ui.navigation.MODULE_TYPE
import me.bmax.apatch.ui.theme.LocalEnableBlur
import me.bmax.apatch.ui.theme.getMaterial3AppBarColor
import me.bmax.apatch.ui.theme.material3BlurEffect
import me.bmax.apatch.ui.theme.rememberMaterial3BlurBackdrop
import me.bmax.apatch.ui.theme.withBackdrop
import me.bmax.apatch.util.DownloadCancelHandle
import me.bmax.apatch.util.ModuleRepo
import me.bmax.apatch.util.reboot
import top.yukonga.miuix.kmp.blur.LayerBackdrop

private val StarColor = Color(0xFFFFB300)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnlineScreenMaterial(
    kpmOnly: Boolean,
    onBack: () -> Unit,
    viewModel: OnlineViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val confirmDialog = rememberConfirmDialog()
    val backdrop = rememberMaterial3BlurBackdrop(LocalEnableBlur.current)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val pullRefreshState = rememberPullToRefreshState()

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

    var downloadTask by remember { mutableStateOf<Pair<String, Int>?>(null) }
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
        downloadTask = module.id to 0
        val file = try {
            ModuleRepo.downloadModuleFile(
                id = module.id,
                kpm = module.kpm,
                fileName = fileName,
                authorUrl = doc.downloadUrl,
                cancel = handle
            ) { percent -> downloadTask = module.id to percent }
        } finally {
            downloadTask = null
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
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            Column(modifier = Modifier.material3BlurEffect(backdrop)) {
                LargeFlexibleTopAppBar(
                    title = {
                        Text(
                            text = titleText,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null
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
                    onQueryChange = viewModel::onSearchChange,
                    placeholder = stringResource(R.string.search_modules)
                )
            }
        }
    ) { paddingValues ->
        PullToRefreshBox(
            state = pullRefreshState,
            isRefreshing = uiState.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize(),
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    modifier = Modifier
                        .padding(top = paddingValues.calculateTopPadding())
                        .align(Alignment.TopCenter),
                    state = pullRefreshState,
                    isRefreshing = uiState.isRefreshing
                )
            }
        ) {
            OnlineModuleList(
                state = uiState.listState,
                modules = uiState.modules,
                hasMore = uiState.hasMore,
                isLoadingMore = uiState.isLoadingMore,
                errorMessage = uiState.errorMessage,
                topPadding = paddingValues.calculateTopPadding(),
                bottomPadding = 16.dp,
                backdrop = backdrop,
                listState = listState,
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

    downloadTask?.let { (title, progress) ->
        ModuleDownloadProgressDialog(
            title = title,
            progress = progress,
            onCancel = { downloadHandle?.cancel() }
        )
    }
}

@Composable
private fun OnlineModuleList(
    state: OnlineListState,
    modules: List<OnlineModule>,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    errorMessage: String,
    topPadding: Dp,
    bottomPadding: Dp,
    backdrop: LayerBackdrop?,
    listState: LazyListState,
    onOpenDetail: (OnlineModule) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .withBackdrop(backdrop),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(
            top = topPadding + 16.dp,
            bottom = bottomPadding + 16.dp,
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
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                        TextButton(onClick = onRetry) {
                            Text(stringResource(R.string.online_retry))
                        }
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
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
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
    Surface(
        onClick = onOpen,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceBright
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.fillMaxWidth(0.8f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = module.id,
                            fontSize = MaterialTheme.typography.titleMedium.fontSize,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, false)
                        )
                    }

                    if (module.author.isNotBlank()) {
                        Text(
                            text = module.author,
                            fontSize = MaterialTheme.typography.bodySmall.fontSize,
                            lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            fontSize = MaterialTheme.typography.labelSmall.fontSize,
                            color = MaterialTheme.colorScheme.primary
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
                            fontSize = MaterialTheme.typography.labelSmall.fontSize,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (module.summary.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = module.summary,
                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
