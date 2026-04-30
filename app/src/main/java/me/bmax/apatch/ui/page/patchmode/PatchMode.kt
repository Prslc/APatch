package me.bmax.apatch.ui.page.patchmode

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.WarningCard
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.ui.navigation.LocalNavigator
import me.bmax.apatch.ui.page.patch.PatchMode
import me.bmax.apatch.ui.theme.blurEffect
import me.bmax.apatch.ui.theme.getAppBarColor
import me.bmax.apatch.ui.theme.rememberBlurBackdrop
import me.bmax.apatch.ui.theme.withBackdrop
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.CheckboxPreference

@Composable
fun PatchMode() {
    val navigator = LocalNavigator.current
    val viewModel: PatchModeViewModel = viewModel()
    val state = viewModel.uiState
    val backdrop = rememberBlurBackdrop(true)

    val selectImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                viewModel.onFileSelected(uri)
                navigator.navigateToPatches(PatchMode.PATCH_ONLY, uri)
            }
        }
    }

    val alertTitle = stringResource(android.R.string.dialog_alert_title)
    val inactiveSlotWarning =
        stringResource(R.string.mode_select_page_install_inactive_slot_warning)
    val confirmDialog = rememberConfirmDialog(
        onConfirm = {
            navigator.navigateToPatches(PatchMode.INSTALL_TO_NEXT_SLOT)
        },
        onDismiss = null
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopBar(
                title = stringResource(R.string.mode_select_page_title),
                backdrop = backdrop,
                onBack = dropUnlessResumed { navigator.popBackStack() }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .withBackdrop(backdrop)
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // waring card
            if (!state.rootAvailable) {
                WarningCard(message = stringResource(R.string.home_install_unknown_summary))
                Spacer(modifier = Modifier.height(16.dp))
            }

            // mode
            val radioOptions = remember(state.rootAvailable, state.isAbDevice) {
                buildList {
                    add(InstallMethod.SelectFile())
                    if (state.rootAvailable) {
                        add(InstallMethod.DirectInstall)
                        if (state.isAbDevice) add(InstallMethod.DirectInstallToInactiveSlot)
                    }
                }
            }

            Card {
                radioOptions.forEach { option ->
                    CheckboxPreference(
                        title = stringResource(id = option.label),
                        summary = when (option) {
                            is InstallMethod.SelectFile -> stringResource(R.string.mode_install_method_select_file_summary)
                            is InstallMethod.DirectInstall -> stringResource(R.string.mode_install_method_direct_install_summary)
                            is InstallMethod.DirectInstallToInactiveSlot -> stringResource(R.string.mode_install_method_inactive_slot_summary)
                        },
                        checked = state.selectedOption?.javaClass == option.javaClass,
                        onCheckedChange = { viewModel.onOptionSelected(option) }
                    )
                }
            }

            TextButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                text = if (state.selectedOption is InstallMethod.SelectFile)
                    stringResource(R.string.action_select_file)
                else
                    stringResource(R.string.action_start_install),
                enabled = state.selectedOption != null,
                colors = ButtonDefaults.textButtonColorsPrimary(),
                onClick = {
                    when (state.selectedOption) {
                        is InstallMethod.SelectFile -> {
                            selectImageLauncher.launch(
                                Intent(Intent.ACTION_GET_CONTENT).apply {
                                    type = "application/octet-stream"
                                }
                            )
                        }

                        is InstallMethod.DirectInstall -> {
                            navigator.navigateToPatches(PatchMode.PATCH_AND_INSTALL)
                        }

                        is InstallMethod.DirectInstallToInactiveSlot -> {
                            confirmDialog.showConfirm(alertTitle, inactiveSlotWarning, true)
                        }

                        null -> {}
                    }
                }
            )
        }
    }
}

sealed class InstallMethod {
    data class SelectFile(@param:StringRes override val label: Int = R.string.mode_select_page_select_file) :
        InstallMethod()

    data object DirectInstall : InstallMethod() {
        override val label = R.string.mode_select_page_patch_and_install
    }

    data object DirectInstallToInactiveSlot : InstallMethod() {
        override val label = R.string.mode_select_page_install_inactive_slot
    }

    abstract val label: Int
}

@Composable
private fun TopBar(
    title: String,
    backdrop: LayerBackdrop?,
    onBack: () -> Unit
) {
    SmallTopAppBar(
        modifier = Modifier.blurEffect(backdrop),
        color = backdrop.getAppBarColor(),
        title = title,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(MiuixIcons.Back, contentDescription = null)
            }
        },
    )
}