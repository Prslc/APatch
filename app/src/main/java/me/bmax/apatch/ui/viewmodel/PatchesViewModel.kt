package me.bmax.apatch.ui.viewmodel

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.nio.ExtendedFile
import com.topjohnwu.superuser.nio.FileSystemManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.apApp
import me.bmax.apatch.util.copyAndCloseOut
import me.bmax.apatch.util.createRootShell
import me.bmax.apatch.util.inputStream
import me.bmax.apatch.util.patch.KernelParser
import me.bmax.apatch.util.patch.PatchEnv
import me.bmax.apatch.util.patch.PatchEnv.patchDir
import me.bmax.apatch.util.patch.PatchEnv.srcBoot
import me.bmax.apatch.util.patch.PatchExecutor
import me.bmax.apatch.util.shellForResult
import org.ini4j.Ini
import java.io.File
import java.io.IOException
import java.io.StringReader

private const val TAG = "PatchViewModel"

class PatchesViewModel : ViewModel() {
    private var shell: Shell = createRootShell()

    enum class PatchMode(val sId: Int) {
        PATCH_ONLY(R.string.patch_mode_bootimg_patch),
        PATCH_AND_INSTALL(R.string.patch_mode_patch_and_install),
        INSTALL_TO_NEXT_SLOT(R.string.patch_mode_install_to_next_slot),
        UNPATCH(R.string.patch_mode_uninstall_patch)
    }

    var bootSlot by mutableStateOf("")
    var bootDev by mutableStateOf("")
    var kimgInfo by mutableStateOf(KPModel.KImgInfo("", false))
    var kpimgInfo by mutableStateOf(KPModel.KPImgInfo("", "", "", "", ""))
    var superkey by mutableStateOf(APApplication.superKey)
    var existedExtras = mutableStateListOf<KPModel.IExtraInfo>()
    var newExtras = mutableStateListOf<KPModel.IExtraInfo>()
    var newExtrasFileName = mutableListOf<String>()

    var running by mutableStateOf(false)
    var patching by mutableStateOf(false)
    var patchdone by mutableStateOf(false)
    var needReboot by mutableStateOf(false)

    var error by mutableStateOf("")
    var patchLog by mutableStateOf("")

    private var prepared: Boolean = false

    fun prepare(mode: PatchMode) {
        viewModelScope.launch(Dispatchers.IO) {
            if (prepared) return@launch
            running = true

            PatchEnv.prepare()?.let {
                error = "Initialization failed: $it"
                running = false
                return@launch
            }

            try {
                if (mode != PatchMode.UNPATCH) {
                    val result = shellForResult(shell, "cd $patchDir", "./kptools -l -k kpimg")
                    kpimgInfo = KernelParser.parseKpimg(result.out) ?: kpimgInfo
                }

                if (mode != PatchMode.PATCH_ONLY) {
                    extractAndParseBootimg(mode)
                }
            } finally {
                prepared = true
                running = false
            }
        }
    }

    private fun parseBootimg(bootimg: String) {
        val result = shellForResult(
            shell,
            "cd $patchDir",
            "./kptools unpacknolog $bootimg",
            "./kptools -l -i kernel",
        )
        if (result.isSuccess) {
            val (info, extras) = KernelParser.parseBootInfo(result.out)
            kimgInfo = info

            if (kimgInfo.patched) {
                val foundKey =
                    result.out.find { it.startsWith("superkey=") }?.substringAfter('=') ?: ""
                if (checkSuperKeyValidation(foundKey)) {
                    this.superkey = foundKey
                    kpimgInfo.superKey = foundKey
                }
            }
            existedExtras.clear()
            existedExtras.addAll(extras)
        } else {
            error += result.err.joinToString("\n")
        }
    }

    val checkSuperKeyValidation: (superKey: String) -> Boolean = { superKey ->
        superKey.length in 8..63 && superKey.any { it.isDigit() } && superKey.any { it.isLetter() }
    }

    fun copyAndParseBootimg(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            while (running) yield()
            running = true
            error = ""
            kimgInfo = KPModel.KImgInfo("", false)
            try {
                uri.inputStream().buffered().use { src ->
                    srcBoot.also { src.copyAndCloseOut(it.newOutputStream()) }
                }
                parseBootimg(srcBoot.path)
            } catch (e: Exception) {
                error = "Copy error: ${e.message}"
            } finally {
                running = false
            }
        }
    }

    private fun extractAndParseBootimg(mode: PatchMode) {
        val cmd =
            if (mode == PatchMode.INSTALL_TO_NEXT_SLOT) "./boot_extract.sh true" else "./boot_extract.sh"
        val result =
            shellForResult(shell, "export ASH_STANDALONE=1", "cd $patchDir", "./busybox sh $cmd")

        if (result.isSuccess) {
            bootSlot = result.out.find { it.startsWith("SLOT=") }?.substringAfter("=") ?: ""
            bootDev = result.out.find { it.startsWith("BOOTIMAGE=") }?.substringAfter("=") ?: ""
            srcBoot = FileSystemManager.getLocal().getFile(bootDev)
            parseBootimg(bootDev)
        } else {
            error = result.err.joinToString("\n")
        }
        running = false
    }

    fun embedKPM(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            if (running) return@launch
            running = true
            error = ""

            val rand = (1..4).map { ('a'..'z').random() }.joinToString("")
            val kpmFileName = "${rand}.kpm"
            val kpmFile: ExtendedFile = patchDir.getChildFile(kpmFileName)

            try {
                uri.inputStream().buffered().use { src ->
                    kpmFile.also { src.copyAndCloseOut(it.newOutputStream()) }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Copy kpm error: $e")
            }

            val result = shellForResult(shell, "cd $patchDir", "./kptools -l -M ${kpmFile.path}")

            if (result.isSuccess) {
                val ini = Ini(StringReader(result.out.joinToString("\n")))
                ini["kpm"]?.let { kpm ->
                    val kpmInfo = KPModel.KPMInfo(
                        KPModel.ExtraType.KPM,
                        kpm["name"].toString(),
                        KPModel.TriggerEvent.PRE_KERNEL_INIT.event,
                        "",
                        kpm["version"].toString(),
                        kpm["license"].toString(),
                        kpm["author"].toString(),
                        kpm["description"].toString(),
                    )
                    newExtras.add(kpmInfo)
                    newExtrasFileName.add(kpmFileName)
                }
            } else {
                error = "Invalid KPM"
            }
            running = false
        }
    }

    fun doAction(mode: PatchMode) {
        if (mode != PatchMode.UNPATCH && superkey.trim().isEmpty()) {
            error = "SuperKey is empty or only whitespace"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            patching = true
            patchLog = ""
            error = ""

            try {
                val cmd = if (mode == PatchMode.UNPATCH) {
                    PatchExecutor.buildUnpatchCommand(bootDev)
                } else {
                    PatchExecutor.buildPatchCommand(
                        mode, superkey.trim(), srcBoot.path,
                        newExtras.filterIsInstance<KPModel.KPMInfo>(),
                        newExtrasFileName, existedExtras,
                        isSuExecutable(), APApplication.MAGISK_SCONTEXT
                    )
                }

                val success = PatchExecutor.runRawProcess(cmd, patchDir) { line ->
                    Log.d(TAG, "[Shell]: $line")
                    patchLog += "$line\n"
                }

                if (success) {
                    handlePostAction(mode)
                    patchdone = true
                } else {
                    error = "Action failed. Check logcat for details."
                }
            } catch (e: Exception) {
                Log.e(TAG, "Action exception", e)
                error = e.localizedMessage ?: "Unknown exception occurred"
            } finally {
                patching = false
            }
        }
    }

    private fun handlePostAction(mode: PatchMode) {
        when (mode) {
            PatchMode.PATCH_AND_INSTALL, PatchMode.UNPATCH -> {
                needReboot = true
                APApplication.markNeedReboot()
            }

            PatchMode.INSTALL_TO_NEXT_SLOT -> {
                val result =
                    shellForResult(shell, "cd $patchDir", "./busybox sh ./boot_install.sh true")
                if (result.isSuccess) {
                    needReboot = true
                    APApplication.markNeedReboot()
                } else {
                    error = "Slot switch failed: ${result.err.joinToString("\n")}"
                }
            }

            PatchMode.PATCH_ONLY -> exportPatchedImage()
        }
    }

    private fun exportPatchedImage() {
        val newBootFile = File(patchDir.path, "new-boot.img")
        if (!newBootFile.exists()) {
            error = "new-boot.img not found"
            return
        }

        val outFilename = "apatch_patched_${System.currentTimeMillis() / 1000}.img"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val destUri = createDownloadUri(apApp, outFilename)
            val srcUri = newBootFile.getUri(apApp)
            if (insertDownload(apApp, destUri, srcUri)) {
                patchLog += "Successfully exported to Downloads/$outFilename\n"
            } else {
                error = "Failed to write image to MediaStore"
            }
        } else {
            patchLog += "Image saved at: ${newBootFile.absolutePath}\n"
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun createDownloadUri(context: Context, outFilename: String): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, outFilename)
            put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        return context.contentResolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            contentValues
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun insertDownload(context: Context, outUri: Uri?, inputUri: Uri): Boolean {
        if (outUri == null) return false
        return try {
            val resolver = context.contentResolver
            resolver.openInputStream(inputUri)?.use { input ->
                resolver.openOutputStream(outUri)?.use { output ->
                    input.copyTo(output)
                }
            }
            val contentValues = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
            resolver.update(outUri, contentValues, null, null)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to insert download", e)
            false
        }
    }

    private fun isSuExecutable(): Boolean {
        val suFile = File("/system/bin/su")
        return suFile.exists() && suFile.canExecute()
    }

    private fun File.getUri(context: Context): Uri {
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, this)
    }
}