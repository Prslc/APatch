package me.bmax.apatch.util

import android.net.Uri
import android.util.Log
import com.topjohnwu.superuser.nio.FileSystemManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.bmax.apatch.Natives
import me.bmax.apatch.apApp
import me.bmax.apatch.ui.viewmodel.KPModel
import java.io.IOException

private const val TAG = "KernelPatchModuleCli"

fun getKernelModuleCount(): Int = Natives.kernelPatchModuleNum().toInt().coerceAtLeast(0)

fun listKernelModules(): List<KPModel.KPMInfo> {
    val count = Natives.kernelPatchModuleNum()
    if (count <= 0) return emptyList()

    val names = Natives.kernelPatchModuleList()
    if (names.isBlank()) return emptyList()

    return names.split('\n')
        .filter { it.isNotBlank() }
        .map { id ->
            val infoLine = Natives.kernelPatchModuleInfo(id)
            val lines = infoLine.split('\n')

            fun getValue(key: String) =
                lines.find { it.startsWith("$key=") }?.substringAfter('=') ?: ""

            KPModel.KPMInfo(
                KPModel.ExtraType.KPM,
                getValue("name"),
                "",
                getValue("args"),
                getValue("version"),
                getValue("license"),
                getValue("author"),
                getValue("description")
            )
        }
}

suspend fun loadKernelModule(uri: Uri, args: String): Int = withContext(Dispatchers.IO) {
    val kpmDir = FileSystemManager.getLocal().getFile(apApp.filesDir.parent, "kpm")
    kpmDir.deleteRecursively()
    kpmDir.mkdirs()

    val rand = (1..4).map { ('a'..'z').random() }.joinToString("")
    val kpmFile = kpmDir.getChildFile("${rand}.kpm")

    try {
        uri.inputStream().buffered().use { input ->
            kpmFile.newOutputStream().use { output ->
                input.copyTo(output)
            }
        }
        Natives.loadKernelPatchModule(kpmFile.path, args).toInt()
    } catch (e: IOException) {
        Log.e(TAG, "Copy kpm error", e)
        -1
    }
}

suspend fun controlKernelModule(name: String, param: String): Natives.KPMCtlRes = withContext(Dispatchers.IO) {
    Natives.kernelPatchModuleControl(name, param)
}

suspend fun unloadKernelModule(name: String): Boolean = withContext(Dispatchers.IO) {
    Natives.unloadKernelPatchModule(name) == 0L
}