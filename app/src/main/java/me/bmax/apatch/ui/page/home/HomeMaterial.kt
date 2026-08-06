package me.bmax.apatch.ui.page.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.apApp
import me.bmax.apatch.ui.component.WarningCard
import me.bmax.apatch.ui.component.dialog.rememberConfirmDialog
import me.bmax.apatch.ui.component.material.SegmentedColumn
import me.bmax.apatch.ui.navigation.LocalNavigator
import me.bmax.apatch.ui.theme.LocalEnableBlur
import me.bmax.apatch.ui.theme.getMaterial3AppBarColor
import me.bmax.apatch.ui.theme.material3BlurEffect
import me.bmax.apatch.ui.theme.rememberMaterial3BlurBackdrop
import me.bmax.apatch.ui.theme.withBackdrop
import me.bmax.apatch.util.Version.getManagerVersion
import me.bmax.apatch.util.reboot

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreenMaterial(
    modifier: Modifier,
    bottomPadding: Dp,
    isCurrentPage: Boolean = true,
    viewModel: HomeViewModel = viewModel(),
) {
    val navigator = LocalNavigator.current

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val backdrop =
        if (isCurrentPage) rememberMaterial3BlurBackdrop(LocalEnableBlur.current) else null

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val kpState = uiState.kpState

    if (isCurrentPage) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            if (APApplication.sharedPreferences.getBoolean("check_update", true)) {
                viewModel.checkUpdate()
            }
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
                            text = stringResource(R.string.app_name),
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    },
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
                            RebootDropdown()
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = backdrop.getMaterial3AppBarColor(),
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        scrolledContainerColor = backdrop.getMaterial3AppBarColor()
                    )
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .withBackdrop(backdrop)
                .verticalScroll(rememberScrollState())
                .padding(
                    top = paddingValues.calculateTopPadding() + 12.dp,
                    bottom = bottomPadding + 16.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BackupWarningCardMaterial()
            KStatusCardMaterial(
                kpState = uiState.kpState,
                apState = uiState.apState,
            )
            if (uiState.kpState != APApplication.State.UNKNOWN_STATE &&
                uiState.apState != APApplication.State.ANDROIDPATCH_INSTALLED
            ) {
                AStatusCardMaterial(uiState.apState)
            }
            val checkUpdate =
                APApplication.sharedPreferences.getBoolean("check_update", true)
            if (checkUpdate) {
                UpdateCardMaterial(uiState)
            }
            InfoCardMaterial(uiState)
            LearnMoreCardMaterial()
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RebootDropdown() {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = stringResource(id = R.string.reboot)
            )
        }
        DropdownMenuPopup(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuGroup(
                shapes = MenuDefaults.groupShape(index = 0, count = 1)
            ) {
                listOf(
                    R.string.reboot to "",
                    R.string.reboot_recovery to "recovery",
                    R.string.reboot_bootloader to "bootloader",
                    R.string.reboot_download to "download",
                    R.string.reboot_edl to "edl",
                ).forEach { (res, reason) ->
                    DropdownMenuItem(
                        onClick = {
                            reboot(reason)
                            expanded = false
                        },
                        text = { Text(text = stringResource(res)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BackupWarningCardMaterial() {
    val show = rememberSaveable { mutableStateOf(apApp.getBackupWarningState()) }

    if (show.value) {
        WarningCard(
            message = stringResource(id = R.string.patch_warnning),
            onClose = {
                apApp.updateBackupWarningState(false)
                show.value = false
            }
        )
    }
}

@Composable
private fun UpdateCardMaterial(state: HomeUiState) {
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
                containerColor = MaterialTheme.colorScheme.outlineVariant,
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.SystemUpdate,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
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
private fun InfoCardMaterial(state: HomeUiState) {
    SegmentedColumn(contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)) {
        if (state.kpState != APApplication.State.UNKNOWN_STATE) {
            item { shape ->
                Surface(shape = shape, color = MaterialTheme.colorScheme.surfaceBright) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tag,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.home_su_path),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = state.suPath,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        item { shape ->
            Surface(shape = shape, color = MaterialTheme.colorScheme.surfaceBright) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhoneAndroid,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.home_device_info),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = state.deviceInfo,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        item { shape ->
            Surface(shape = shape, color = MaterialTheme.colorScheme.surfaceBright) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Memory,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.home_kernel),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text= state.kernelVersion,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        item { shape ->
            Surface(shape = shape, color = MaterialTheme.colorScheme.surfaceBright) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Android,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            stringResource(R.string.home_system_version),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            state.androidVersion,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        item { shape ->
            Surface(shape = shape, color = MaterialTheme.colorScheme.surfaceBright) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_fingerprint_24dp),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.home_fingerprint),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = state.fingerprint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        item { shape ->
            val selinuxText = when (state.selinux) {
                "Enforcing" -> stringResource(R.string.home_selinux_status_enforcing)
                "Permissive" -> stringResource(R.string.home_selinux_status_permissive)
                "Disabled" -> stringResource(R.string.home_selinux_status_disabled)
                else -> stringResource(R.string.home_selinux_status_unknown)
            }
            Surface(shape = shape, color = MaterialTheme.colorScheme.surfaceBright) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Security,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            stringResource(R.string.home_selinux_status),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            selinuxText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LearnMoreCardMaterial() {
    val uriHandler = LocalUriHandler.current
    val url = "https://apatch.dev"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { uriHandler.openUri(url) },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceBright
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_learn_apatch),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.home_click_to_learn_apatch),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Filled.Link,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
