package me.bmax.apatch.ui.page.patch

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import me.bmax.apatch.R
import me.bmax.apatch.ui.page.kpm.KPModel

@Serializable
enum class PatchMode(val sId: Int) {
    PATCH_ONLY(R.string.patch_mode_bootimg_patch),
    PATCH_AND_INSTALL(R.string.patch_mode_patch_and_install),
    INSTALL_TO_NEXT_SLOT(R.string.patch_mode_install_to_next_slot),
    UNPATCH(R.string.patch_mode_uninstall_patch)
}

@Immutable
data class PatchUiState(
    val bootSlot: String = "",
    val bootDev: String = "",
    val kimgInfo: KPModel.KImgInfo = KPModel.KImgInfo("", false),
    val kpimgInfo: KPModel.KPImgInfo = KPModel.KPImgInfo("", "", "", "", ""),
    val superkey: String = "",

    val existedExtras: List<KPModel.IExtraInfo> = emptyList(),
    val newExtras: List<KPModel.IExtraInfo> = emptyList(),
    val newExtrasFileName: List<String> = emptyList(),

    val isRunning: Boolean = false,
    val isPatching: Boolean = false,
    val isPatchDone: Boolean = false,
    val needReboot: Boolean = false,

    val error: String = "",
    val patchLog: String = ""
)