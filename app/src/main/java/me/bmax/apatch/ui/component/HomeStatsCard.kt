package me.bmax.apatch.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Cached
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.navigation.LocalNavigator
import me.bmax.apatch.ui.theme.isInDarkTheme
import me.bmax.apatch.ui.viewmodel.PatchesViewModel
import me.bmax.apatch.util.Version
import me.bmax.apatch.util.Version.getManagerVersion
import me.bmax.apatch.util.reboot
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.isDynamicColor
import top.yukonga.miuix.kmp.utils.PressFeedbackType

private val managerVersion = getManagerVersion()

/**
 * KernelPatch status card state
 */
data class KPatchCardState(
    val icon: ImageVector,
    val iconDesc: String,
    val title: Int,
    val subtitle: String? = null,
    val versionInfo: String? = null,
    val buttonText: Int,
    val buttonAction: KPatchAction,
    val isButtonEnabled: Boolean = true
)

enum class KPatchAction {
    UNKNOWN_STATE,
    UPDATE,
    REBOOT,
    UNINSTALL,
    NONE
}

/**
 * AndroidPatch status card state
 */
data class APatchCardState(
    val icon: ImageVector,
    val iconDesc: String,
    val title: Int,
    val subtitle: String? = null,
    val buttonText: Int? = null,
    val buttonIcon: ImageVector? = null,
    val buttonAction: APatchAction,
    val showButton: Boolean = true,
    val isButtonEnabled: Boolean = true
)

enum class APatchAction {
    INSTALL,
    UPDATE,
    UNINSTALL,
    NONE
}

/**
 * Map KernelPatch state to UI card state
 */
fun APApplication.State.toKPatchCardState(
    apState: APApplication.State,
    managerVersion: Pair<String, Long>
): KPatchCardState {
    return when (this) {
        APApplication.State.KERNELPATCH_INSTALLED -> KPatchCardState(
            icon = Icons.Filled.CheckCircle,
            iconDesc = "Working",
            title = R.string.home_working,
            versionInfo = "${Version.installedKPVString()} (${managerVersion.second}) - " +
                    if (apState != APApplication.State.ANDROIDPATCH_NOT_INSTALLED) "Full" else "KernelPatch",
            buttonText = R.string.home_ap_cando_uninstall,
            buttonAction = KPatchAction.UNINSTALL
        )

        APApplication.State.KERNELPATCH_NEED_UPDATE -> KPatchCardState(
            icon = Icons.Outlined.SystemUpdate,
            iconDesc = "Need Update",
            title = R.string.home_need_update,
            subtitle = "KernelPatch: ${Version.installedKPVString()} → ${Version.buildKPVString()}",
            buttonText = R.string.home_ap_cando_update,
            buttonAction = KPatchAction.UPDATE
        )

        APApplication.State.KERNELPATCH_NEED_REBOOT -> KPatchCardState(
            icon = Icons.Outlined.SystemUpdate,
            iconDesc = "Need Reboot",
            title = R.string.home_need_update,
            subtitle = "KernelPatch: ${Version.installedKPVString()} → ${Version.buildKPVString()}",
            buttonText = R.string.home_ap_cando_reboot,
            buttonAction = KPatchAction.REBOOT
        )

        APApplication.State.KERNELPATCH_UNINSTALLING -> KPatchCardState(
            icon = Icons.Outlined.Cached,
            iconDesc = "Busy",
            title = R.string.home_working,
            buttonText = R.string.home_working,
            buttonAction = KPatchAction.NONE,
            isButtonEnabled = false
        )

        else -> KPatchCardState(
            icon = Icons.AutoMirrored.Outlined.HelpOutline,
            iconDesc = "Unknown",
            title = R.string.home_install_unknown,
            subtitle = null,
            buttonText = R.string.home_ap_cando_install,
            buttonAction = KPatchAction.UNKNOWN_STATE
        )
    }
}

/**
 * Map AndroidPatch state to UI card state
 */
fun APApplication.State.toAPatchCardState(managerVersion: Pair<String, Long>): APatchCardState {
    return when (this) {
        APApplication.State.ANDROIDPATCH_NOT_INSTALLED -> APatchCardState(
            icon = Icons.Outlined.Block,
            iconDesc = "Not Installed",
            title = R.string.home_not_installed,
            buttonText = R.string.home_ap_cando_install,
            buttonAction = APatchAction.INSTALL
        )

        APApplication.State.ANDROIDPATCH_INSTALLED -> APatchCardState(
            icon = Icons.Outlined.CheckCircle,
            iconDesc = "Working",
            title = R.string.home_working,
            buttonText = R.string.home_ap_cando_uninstall,
            buttonAction = APatchAction.UNINSTALL
        )

        APApplication.State.ANDROIDPATCH_NEED_UPDATE -> APatchCardState(
            icon = Icons.Outlined.SystemUpdate,
            iconDesc = "Need Update",
            title = R.string.home_need_update,
            subtitle = "APatch: ${Version.installedApdVString} → ${managerVersion.second}",
            buttonText = R.string.home_ap_cando_update,
            buttonAction = APatchAction.UPDATE
        )

        APApplication.State.ANDROIDPATCH_INSTALLING -> APatchCardState(
            icon = Icons.Outlined.InstallMobile,
            iconDesc = "Installing",
            title = R.string.home_installing,
            buttonText = R.string.home_installing,
            buttonIcon = Icons.Outlined.Cached,
            buttonAction = APatchAction.NONE,
            isButtonEnabled = false,
            showButton = true
        )

        APApplication.State.ANDROIDPATCH_UNINSTALLING -> APatchCardState(
            icon = Icons.Outlined.Cached,
            iconDesc = "Uninstalling",
            title = R.string.home_installing,
            buttonText = R.string.home_installing,
            buttonIcon = Icons.Outlined.Cached,
            buttonAction = APatchAction.NONE,
            isButtonEnabled = false,
            showButton = true
        )

        else -> APatchCardState(
            icon = Icons.AutoMirrored.Outlined.HelpOutline,
            iconDesc = "Unknown",
            title = R.string.home_install_unknown,
            buttonText = R.string.home_install_unknown,
            buttonAction = APatchAction.NONE,
            showButton = false
        )
    }
}

@Composable
fun KStatusCard(
    kpState: APApplication.State,
    apState: APApplication.State,
    apmCount: Int,
    kpmCount: Int,
    onApmClick: () -> Unit,
    onKpmClick: () -> Unit
) {
    val navigator = LocalNavigator.current
    val cardState = remember(kpState, apState) {
        kpState.toKPatchCardState(apState, managerVersion)
    }

    val showUninstallDialog = remember { mutableStateOf(false) }

    UninstallDialog(showUninstallDialog)

    val onMainCardClick = {
        when (cardState.buttonAction) {
            KPatchAction.UNKNOWN_STATE -> navigator.navigateToModeSelect()
            KPatchAction.UPDATE -> {
                if (Version.installedKPVUInt() < 0x900u) {
                    navigator.navigateToPatches(PatchesViewModel.PatchMode.PATCH_ONLY)
                } else {
                    navigator.navigateToModeSelect()
                }
            }

            KPatchAction.REBOOT -> reboot()
            KPatchAction.UNINSTALL -> {
                if (apState == APApplication.State.ANDROIDPATCH_INSTALLED ||
                    apState == APApplication.State.ANDROIDPATCH_NEED_UPDATE
                ) {
                    showUninstallDialog.value = true
                } else {
                    navigator.navigateToPatches(PatchesViewModel.PatchMode.UNPATCH)
                }
            }

            else -> {
                if (kpState != APApplication.State.KERNELPATCH_INSTALLED) {
                    navigator.navigateToModeSelect()
                }
            }
        }
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                colors = CardDefaults.defaultColors(
                    color = when {
                        cardState.buttonAction == KPatchAction.UPDATE -> colorScheme.errorContainer
                        cardState.buttonAction == KPatchAction.UNKNOWN_STATE -> colorScheme.surfaceVariant
                        isDynamicColor -> colorScheme.secondaryContainer
                        isInDarkTheme(0) -> Color(0xFF1A3825)
                        else -> Color(0xFFDFFAE4)
                    }
                ),
                onClick = onMainCardClick,
                pressFeedbackType = PressFeedbackType.Tilt
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(38.dp, 45.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Icon(
                            modifier = Modifier.size(170.dp),
                            imageVector = when {
                                cardState.buttonAction == KPatchAction.UPDATE -> Icons.Rounded.ErrorOutline
                                cardState.buttonAction == KPatchAction.UNKNOWN_STATE -> Icons.AutoMirrored.Outlined.HelpOutline
                                else -> Icons.Rounded.CheckCircleOutline
                            },
                            tint = when {
                                cardState.buttonAction == KPatchAction.UPDATE -> colorScheme.error.copy(alpha = 0.6f)
                                cardState.buttonAction == KPatchAction.UNKNOWN_STATE -> colorScheme.onSurfaceVariantSummary.copy(alpha = 0.4f)
                                isDynamicColor -> colorScheme.primary.copy(alpha = 0.8f)
                                else -> Color(0xFF36D167)
                            },
                            contentDescription = null
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(all = 16.dp)
                    ) {

                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(cardState.title),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurface
                        )

                        Spacer(Modifier.height(2.dp))
                        cardState.versionInfo?.let {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = it,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    insideMargin = PaddingValues(16.dp),
                    onClick = onApmClick,
                    showIndication = true,
                    pressFeedbackType = PressFeedbackType.Tilt
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(R.string.apm),
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            color = colorScheme.onSurfaceVariantSummary,
                        )
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = apmCount.toString(),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurface,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    insideMargin = PaddingValues(16.dp),
                    onClick = onKpmClick,
                    showIndication = true,
                    pressFeedbackType = PressFeedbackType.Tilt
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(R.string.kpm),
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            color = colorScheme.onSurfaceVariantSummary,
                        )
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = kpmCount.toString(),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AStatusCard(
    apState: APApplication.State
) {
    val cardState = remember(apState) {
        apState.toAPatchCardState(managerVersion)
    }

    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row {
                Text(
                    text = stringResource(R.string.android_patch),
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(cardState.icon, contentDescription = cardState.iconDesc)
                Column(
                    Modifier
                        .weight(2f)
                        .padding(start = 16.dp)
                ) {
                    Text(
                        text = stringResource(cardState.title),
                        style = MiuixTheme.textStyles.body2
                    )
                    cardState.subtitle?.let {
                        Text(text = it, style = MiuixTheme.textStyles.body2)
                    }
                }

                if (cardState.showButton) {
                    Column(
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Button(
                            enabled = cardState.isButtonEnabled,
                            colors = ButtonDefaults.buttonColors(),
                            onClick = {
                                when (cardState.buttonAction) {
                                    APatchAction.INSTALL, APatchAction.UPDATE -> {
                                        APApplication.installApatch()
                                    }

                                    APatchAction.UNINSTALL -> {
                                        APApplication.uninstallApatch()
                                    }

                                    APatchAction.NONE -> {}
                                }
                            },
                            content = {
                                val bIcon = cardState.buttonIcon
                                val bText = cardState.buttonText
                                if (bIcon != null) {
                                    Icon(bIcon, contentDescription = null)
                                } else if (bText != null) {
                                    Text(text = stringResource(id = bText))
                                }
                            })
                    }
                }
            }
        }
    }
}