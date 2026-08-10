package me.bmax.apatch.ui.page.patchmode

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.material.BaseWidget
import me.bmax.apatch.ui.component.material.SegmentedColumn
import me.bmax.apatch.ui.component.dialog.rememberConfirmDialog
import me.bmax.apatch.ui.navigation.LocalNavigator
import me.bmax.apatch.ui.page.patch.PatchMode
import me.bmax.apatch.ui.theme.LocalEnableBlur
import me.bmax.apatch.ui.theme.getMaterial3AppBarColor
import me.bmax.apatch.ui.theme.material3BlurEffect
import me.bmax.apatch.ui.theme.rememberMaterial3BlurBackdrop
import me.bmax.apatch.ui.theme.withBackdrop

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PatchModeMaterial(viewModel: PatchModeViewModel = viewModel()) {
    val navigator = LocalNavigator.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val backdrop = rememberMaterial3BlurBackdrop(LocalEnableBlur.current)

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
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            Column(modifier = Modifier.material3BlurEffect(backdrop)) {
                LargeFlexibleTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.mode_select_page_title),
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = dropUnlessResumed { navigator.popBackStack() }) {
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
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .withBackdrop(backdrop)
                .padding(paddingValues)
        ) {
            if (state.jailbreakBlocked) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Error,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.jailbreak_no_patch),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            } else if (!state.rootAvailable) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Error,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.home_install_unknown_summary),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            if (!state.jailbreakBlocked) {
                val radioOptions = remember(state.rootAvailable, state.isAbDevice) {
                    buildList {
                        add(InstallMethod.SelectFile())
                        if (state.rootAvailable) {
                            add(InstallMethod.DirectInstall)
                            if (state.isAbDevice) add(InstallMethod.DirectInstallToInactiveSlot)
                        }
                    }
                }

                SegmentedColumn(modifier = Modifier.fillMaxWidth()) {
                    radioOptions.forEach { option ->
                        val isSelected = state.selectedOption?.javaClass == option.javaClass
                        item {
                            BaseWidget(
                                title = stringResource(id = option.label),
                                description = when (option) {
                                    is InstallMethod.SelectFile -> stringResource(R.string.mode_install_method_select_file_summary)
                                    is InstallMethod.DirectInstall -> stringResource(R.string.mode_install_method_direct_install_summary)
                                    is InstallMethod.DirectInstallToInactiveSlot -> stringResource(R.string.mode_install_method_inactive_slot_summary)
                                },
                                selected = isSelected,
                                icon = if (isSelected) {
                                    Icons.Filled.RadioButtonChecked
                                } else {
                                    Icons.Outlined.RadioButtonUnchecked
                                },
                                onClick = { viewModel.onOptionSelected(option) }
                            )
                        }
                    }
                }

                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 8.dp, end = 16.dp),
                    enabled = state.selectedOption != null,
                    shape = RoundedCornerShape(16.dp),
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
                ) {
                    Text(
                        text = if (state.selectedOption is InstallMethod.SelectFile)
                            stringResource(R.string.action_select_file)
                        else
                            stringResource(R.string.action_start_install),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
