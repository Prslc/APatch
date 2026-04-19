package me.bmax.apatch.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import me.bmax.apatch.APApplication
import me.bmax.apatch.Natives
import me.bmax.apatch.R
import me.bmax.apatch.apApp
import me.bmax.apatch.ui.component.AStatusCard
import me.bmax.apatch.ui.component.BottomBarDestination
import me.bmax.apatch.ui.component.DropdownItem
import me.bmax.apatch.ui.component.KStatusCard
import me.bmax.apatch.ui.component.WarningCard
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.ui.navigation.LocalNavigator
import me.bmax.apatch.ui.navigation.Navigator
import me.bmax.apatch.ui.theme.blurEffect
import me.bmax.apatch.ui.theme.getAppBarColor
import me.bmax.apatch.ui.theme.rememberBlurBackdrop
import me.bmax.apatch.ui.viewmodel.HomeViewModel
import me.bmax.apatch.util.Version.getManagerVersion
import me.bmax.apatch.util.reboot
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.window.WindowListPopup

@Composable
fun HomeScreen(
    bottomPadding: Dp,
    viewModel: HomeViewModel = viewModel()
) {
    val navigator = LocalNavigator.current

    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop(true)

    val kpState by viewModel.kpState.collectAsState(APApplication.State.UNKNOWN_STATE)
    val apState by viewModel.apState.collectAsState(APApplication.State.UNKNOWN_STATE)
    val apmCount by viewModel.apmCount.collectAsState()
    val kpmCount by viewModel.kpmCount.collectAsState()

    val kPatchReady = apState != APApplication.State.UNKNOWN_STATE
    val aPatchReady = apState == APApplication.State.ANDROIDPATCH_INSTALLED

    val availablePages = remember(kPatchReady, aPatchReady) {
        BottomBarDestination.entries.filter { d ->
            !(d.kPatchRequired && !kPatchReady) && !(d.aPatchRequired && !aPatchReady)
        }
    }

    LaunchedEffect(Unit) {
        if (APApplication.sharedPreferences.getBoolean("check_update", true)) {
            viewModel.checkUpdate()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopBar(
                backdrop = backdrop,
                navigator = navigator,
                kpState = kpState,
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier)
                .padding(horizontal = 16.dp)
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 12.dp,
                bottom = innerPadding.calculateBottomPadding()
            )
        ) {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    BackupWarningCard()
                    KStatusCard(
                        kpState = kpState,
                        apState = apState,
                        apmCount = apmCount,
                        kpmCount = kpmCount,
                        onApmClick = {
                            val index = availablePages.indexOf(BottomBarDestination.AModule)
                            if (index != -1) navigator.switchToTab(index)
                        },
                        onKpmClick = {
                            val index = availablePages.indexOf(BottomBarDestination.KModule)
                            if (index != -1) navigator.switchToTab(index)
                        }
                    )
                    if (kpState != APApplication.State.UNKNOWN_STATE && apState != APApplication.State.ANDROIDPATCH_INSTALLED) {
                        AStatusCard(apState)
                    }
                    val checkUpdate =
                        APApplication.sharedPreferences.getBoolean("check_update", true)
                    if (checkUpdate) {
                        UpdateCard(viewModel)
                    }
                    InfoCard(kpState, viewModel)
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
    scrollBehavior: ScrollBehavior
) {
    val howDropdownReboot = remember { mutableStateOf(false) }

    val rebootItems = listOf(
        stringResource(R.string.reboot),
        stringResource(R.string.reboot_recovery),
        stringResource(R.string.reboot_bootloader),
        stringResource(R.string.reboot_download),
        stringResource(R.string.reboot_edl),
    )

    TopAppBar(
        modifier = Modifier.blurEffect(backdrop),
        color = backdrop.getAppBarColor(),
        title = stringResource(R.string.app_name),
        actions = {
            IconButton(onClick = dropUnlessResumed {
                navigator.navigateToModeSelect()
            }) {
                Icon(
                    imageVector = Icons.Filled.InstallMobile,
                    contentDescription = stringResource(id = R.string.mode_select_page_title)
                )
            }

            if (kpState != APApplication.State.UNKNOWN_STATE) {
                IconButton(
                    onClick = {
                        howDropdownReboot.value = true
                    }) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = stringResource(id = R.string.reboot)
                    )

                    WindowListPopup(
                        show = howDropdownReboot.value,
                        alignment = PopupPositionProvider.Align.BottomStart,
                        onDismissRequest = { howDropdownReboot.value = false }
                    ) {
                        ListPopupColumn {
                            rebootItems.forEachIndexed { index, string ->
                                DropdownItem(
                                    text = string,
                                    optionSize = rebootItems.size,
                                    onSelectedIndexChange = {
                                        when (index) {
                                            0 -> reboot()
                                            1 -> reboot("recovery")
                                            2 -> reboot("bootloader")
                                            3 -> reboot("download")
                                            4 -> reboot("edl")
                                        }
                                        howDropdownReboot.value = false
                                    },
                                    index = index
                                )
                            }
                        }
                    }
                }
            }
        }, scrollBehavior = scrollBehavior
    )
}


@Composable
fun BackupWarningCard() {
    val show = rememberSaveable { mutableStateOf(apApp.getBackupWarningState()) }
    if (show.value) {
        Card(
            colors = CardDefaults.defaultColors(run {
                colorScheme.error
            })
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = "warning")
                }
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.CenterHorizontally),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = stringResource(id = R.string.patch_warnning),
                        )

                        Spacer(Modifier.width(12.dp))

                        Icon(
                            Icons.Outlined.Clear,
                            contentDescription = "",
                            modifier = Modifier.clickable {
                                apApp.updateBackupWarningState(false)
                                show.value = false
                            },
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun InfoCard(
    kpState: APApplication.State,
    viewModel: HomeViewModel
) {

    val systemInfo by viewModel.systemInfo.collectAsState()

    val selinuxText = when (systemInfo.selinux) {
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
            if (kpState != APApplication.State.UNKNOWN_STATE) {
                InfoText(
                    title = stringResource(R.string.home_su_path),
                    content = Natives.suPath()
                )
            }
            InfoText(
                title = stringResource(R.string.home_device_info),
                content = systemInfo.deviceInfo,
            )
            InfoText(
                title = stringResource(R.string.home_kernel),
                content = systemInfo.kernelVersion
            )
            InfoText(
                title = stringResource(R.string.home_system_version),
                content = systemInfo.androidVersion
            )
            InfoText(
                title = stringResource(R.string.home_fingerprint),
                content = systemInfo.fingerprint
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
fun UpdateCard(viewModel: HomeViewModel) {
    val newVersion by viewModel.newVersionInfo.collectAsState()
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

            WarningCard(
                message = stringResource(id = R.string.home_new_apatch_found).format(info.versionCode),
                color = colorScheme.outline
            ) {
                if (info.changelog.isEmpty()) {
                    uriHandler.openUri(info.downloadUrl)
                } else {
                    updateDialog.showConfirm(
                        title = stringResource(id = R.string.apm_changelog),
                        content = info.changelog,
                        markdown = true,
                        confirm = stringResource(id = R.string.apm_update)
                    )
                }
            }
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