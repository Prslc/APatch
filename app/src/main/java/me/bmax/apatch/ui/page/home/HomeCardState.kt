package me.bmax.apatch.ui.page.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Cached
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.util.Version

/**
 * KernelPatch status card state
 */
@Immutable
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
    NONE
}

/**
 * AndroidPatch status card state
 */
@Immutable
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
            buttonText = R.string.home_working,
            buttonAction = KPatchAction.NONE,
            isButtonEnabled = false
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
            buttonAction = APatchAction.NONE,
            showButton = false
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
