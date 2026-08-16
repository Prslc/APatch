package me.bmax.apatch.ui.page.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.util.RebootMode
import me.bmax.apatch.apApp
import me.bmax.apatch.ui.component.bottombar.BottomBarDestination
import me.bmax.apatch.ui.component.bottombar.rememberAvailablePages
import me.bmax.apatch.ui.component.WarningCard
import me.bmax.apatch.ui.component.dialog.rememberConfirmDialog
import me.bmax.apatch.ui.component.dialog.rememberRebootAction
import me.bmax.apatch.ui.navigation.LocalNavigator
import me.bmax.apatch.ui.navigation.Navigator
import me.bmax.apatch.ui.theme.blurEffect
import me.bmax.apatch.ui.theme.getAppBarColor
import me.bmax.apatch.ui.theme.rememberBlurBackdrop
import me.bmax.apatch.ui.theme.withBackdrop
import me.bmax.apatch.util.Version.getManagerVersion
import me.bmax.apatch.util.installJailbreak
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

@Composable
fun HomeScreenMiuix(
    modifier: Modifier,
    bottomPadding: Dp,
    isCurrentPage: Boolean = true,
    viewModel: HomeViewModel = viewModel(),
) {
    val navigator = LocalNavigator.current

    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = if (isCurrentPage) rememberBlurBackdrop() else null

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val availablePages = rememberAvailablePages()

    LaunchedEffect(isCurrentPage) {
        if (isCurrentPage && APApplication.sharedPreferences.getBoolean("check_update", true)) {
            viewModel.checkUpdate()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopBar(
                backdrop = backdrop,
                navigator = navigator,
                kpState = uiState.kpState,
                isJailbreak = uiState.isJailbreak,
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .withBackdrop(backdrop)
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 12.dp,
                bottom = innerPadding.calculateBottomPadding() + 16.dp,
                start = 16.dp,
                end = 16.dp
            )
        ) {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    BackupWarningCard()
                    val isPermissive = uiState.selinux.equals("Permissive", ignoreCase = true)
                    val scope = rememberCoroutineScope()
                    val context = LocalContext.current
                    val jailbreakTriggeredMsg = stringResource(R.string.jailbreak_triggered)
                    val jailbreakFailedMsg = stringResource(R.string.settings_jailbreak_failed)
                    KStatusCard(
                        kpState = uiState.kpState,
                        apState = uiState.apState,
                        apmCount = uiState.apmCount,
                        kpmCount = uiState.kpmCount,
                        isPermissive = isPermissive,
                        isJailbreak = uiState.isJailbreak,
                        onApmClick = {
                            val index = availablePages.indexOf(BottomBarDestination.AModule)
                            if (index != -1) navigator.switchToTab(index)
                        },
                        onKpmClick = {
                            val index = availablePages.indexOf(BottomBarDestination.KModule)
                            if (index != -1) navigator.switchToTab(index)
                        },
                        onJailbreakClick = {
                            scope.launch {
                                val success = installJailbreak()
                                val msg = if (success) {
                                    jailbreakTriggeredMsg
                                } else {
                                    jailbreakFailedMsg
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    if (!uiState.isJailbreak && uiState.kpState != APApplication.State.UNKNOWN_STATE && uiState.apState != APApplication.State.ANDROIDPATCH_INSTALLED) {
                        AStatusCard(uiState.apState)
                    }
                    val checkUpdate =
                        APApplication.sharedPreferences.getBoolean("check_update", true)
                    if (checkUpdate) {
                        UpdateCard(uiState)
                    }
                    InfoCard(uiState)
                    LearnMoreCard()
                }
                Spacer(Modifier.height(bottomPadding))
            }
        }
    }
}

@Composable
private fun TopBar(
    backdrop: LayerBackdrop?,
    navigator: Navigator,
    kpState: APApplication.State,
    isJailbreak: Boolean,
    scrollBehavior: ScrollBehavior
) {
    val onReboot = rememberRebootAction()
    TopAppBar(
        modifier = Modifier.blurEffect(backdrop),
        color = backdrop.getAppBarColor(),
        title = stringResource(R.string.app_name),
        actions = {
            if (!isJailbreak) {
                IconButton(onClick = dropUnlessResumed {
                    navigator.navigateToModeSelect()
                }) {
                    Icon(
                        imageVector = Icons.Filled.InstallMobile,
                        contentDescription = stringResource(id = R.string.mode_select_page_title)
                    )
                }
            }

            if (kpState != APApplication.State.UNKNOWN_STATE) {
                val rebootEntry = DropdownEntry(
                    items = RebootMode.entries.map { mode ->
                        DropdownItem(text = stringResource(mode.labelRes), onClick = {
                            onReboot(mode)
                        })
                    }
                )
                OverlayIconDropdownMenu(entry = rebootEntry) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = stringResource(id = R.string.reboot)
                    )
                }
            }
        }, scrollBehavior = scrollBehavior
    )
}


@Composable
fun BackupWarningCard() {
    val show = rememberSaveable { mutableStateOf(apApp.getBackupWarningState()) }

    if (show.value) {
        WarningCard(
            message = stringResource(id = R.string.patch_warnning),
            icon = {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
            onClose = {
                apApp.updateBackupWarningState(false)
                show.value = false
            }
        )
    }
}

@Composable
private fun InfoCard(state: HomeUiState) {
    val selinuxText = when (state.selinux) {
        "Enforcing" -> stringResource(R.string.home_selinux_status_enforcing)
        "Permissive" -> stringResource(R.string.home_selinux_status_permissive)
        "Disabled" -> stringResource(R.string.home_selinux_status_disabled)
        else -> stringResource(R.string.home_selinux_status_unknown)
    }

    @Composable
    fun InfoText(
        title: String,
        content: String,
        bottomPadding: Dp = 24.dp
    ) {
        Text(
            text = title,
            fontSize = MiuixTheme.textStyles.headline1.fontSize,
            fontWeight = FontWeight.Medium,
            color = colorScheme.onSurface
        )
        Text(
            text = content,
            fontSize = MiuixTheme.textStyles.body2.fontSize,
            color = colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(top = 2.dp, bottom = bottomPadding)
        )
    }
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (state.kpState != APApplication.State.UNKNOWN_STATE) {
                InfoText(
                    title = stringResource(R.string.home_su_path),
                    content = state.suPath
                )
            }
            InfoText(
                title = stringResource(R.string.home_device_info),
                content = state.deviceInfo,
            )
            InfoText(
                title = stringResource(R.string.home_kernel),
                content = state.kernelVersion
            )
            InfoText(
                title = stringResource(R.string.home_system_version),
                content = state.androidVersion
            )
            InfoText(
                title = stringResource(R.string.home_fingerprint),
                content = state.fingerprint
            )
            InfoText(
                title = stringResource(R.string.home_selinux_status),
                content = selinuxText,
                bottomPadding = 0.dp
            )
        }
    }
}

@Composable
fun UpdateCard(state: HomeUiState) {
    val newVersion = state.newVersionInfo
    val uriHandler = LocalUriHandler.current
    val currentCode = remember { getManagerVersion().second }

    newVersion?.let { info ->
        AnimatedVisibility(
            visible = info.versionCode > currentCode,
            enter = fadeIn() + expandVertically(),
            exit = shrinkVertically() + fadeOut()
        ) {
            val updateDialog =
                rememberConfirmDialog(onConfirm = { uriHandler.openUri(info.downloadUrl) })
            val changelogTitle = stringResource(id = R.string.apm_changelog)
            val updateText = stringResource(id = R.string.apm_update)

            WarningCard(
                message = stringResource(id = R.string.home_new_apatch_found).format(info.versionCode),
                onClick = {
                    if (info.changelog.isEmpty()) {
                        uriHandler.openUri(info.downloadUrl)
                    } else {
                        updateDialog.showConfirm(
                            title = changelogTitle,
                            content = info.changelog,
                            markdown = true,
                            confirm = updateText
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun LearnMoreCard() {
    val uriHandler = LocalUriHandler.current
    val url = "https://apatch.dev"

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        BasicComponent(
            title = stringResource(R.string.home_learn_apatch),
            summary = stringResource(R.string.home_click_to_learn_apatch),
            endActions = {
                Icon(
                    imageVector = MiuixIcons.Link,
                    tint = colorScheme.onSurface,
                    contentDescription = null
                )
            },
            onClick = {
                uriHandler.openUri(url)
            },
        )
    }
}
