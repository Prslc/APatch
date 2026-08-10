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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.dialog.ConfirmResult
import me.bmax.apatch.ui.component.dialog.rememberConfirmDialog
import me.bmax.apatch.ui.component.dialog.rememberLoadingDialog
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
    val filteredModules by viewModel.filteredModules.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val confirmDialog = rememberConfirmDialog()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val kpModuleListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    var fabVisible by remember { mutableStateOf(true) }

    // Clearing the search returns to the full list, so jump back to the top.
    LaunchedEffect(uiState.search) {
        if (uiState.search.isEmpty()) kpModuleListState.scrollToItem(0)
    }

    val navigator = LocalNavigator.current
    val backdrop =
        if (isCurrentPage) rememberMaterial3BlurBackdrop(LocalEnableBlur.current) else null

    // Load lazily on first visit; the pager pre-composes all pages, so this
    // avoids scanning KPM modules at startup before the page is ever shown.
    if (isCurrentPage) {
        LaunchedEffect(Unit) {
            if (uiState.modules.isEmpty()) viewModel.fetchModuleList()
        }
    }

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
                    onInstallModule = { /*TODO*/ },
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
                    filteredModules.isEmpty() -> {
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
                        items(filteredModules, key = { it.name }) { module ->
                            KPModuleItemMaterial(
                                module = module,
                                onUninstall = {
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
                                onControl = { viewModel.openControlDialog(module) }
                            )
                        }
                    }
                }
            }
        }
    }
    if (uiState.showControlDialog && uiState.controlTarget != null) {
        KPMControlDialogMaterial(
            module = uiState.controlTarget!!,
            onDismiss = { viewModel.closeControlDialog() }
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
                                    Toast.makeText(context, "Under development", Toast.LENGTH_SHORT)
                                        .show()
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
    module: KPModel.KPMInfo,
    onDismiss: () -> Unit,
    viewModel: KPModuleViewModel = viewModel()
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
            viewModel.controlModule(module.name, controlParam)
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
                            scope.launch {
                                onModuleControl(module)
                            }
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
    onUninstall: (KPModel.KPMInfo) -> Unit,
    onControl: (KPModel.KPMInfo) -> Unit,
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
            Text(
                text = module.name,
                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                fontWeight = FontWeight.SemiBold,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight
            )
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
                ModuleActionButtonMaterial(
                    icon = Icons.Filled.Settings,
                    text = stringResource(R.string.kpm_control),
                    onClick = { onControl(module) }
                )
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
