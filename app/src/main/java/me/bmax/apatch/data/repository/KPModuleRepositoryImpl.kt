package me.bmax.apatch.data.repository

import android.net.Uri
import android.util.Log
import com.topjohnwu.superuser.nio.FileSystemManager
import me.bmax.apatch.Natives
import me.bmax.apatch.apApp
import me.bmax.apatch.ui.page.kpm.KPModel
import java.io.IOException

object KPModuleRepositoryImpl : KPModuleRepository {
    private const val TAG = "KPModuleRepo"

    override suspend fun listModules(): List<KPModel.KPMInfo> {
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
                    getValue("description"),
                    getValue("embedded") == "1"
                )
            }
    }

    override fun getModuleCount(): Int {
        return Natives.kernelPatchModuleNum().toInt().coerceAtLeast(0)
    }

    override suspend fun loadModule(uri: Uri, args: String): Int {
        val kpmDir = FileSystemManager.getLocal().getFile(apApp.filesDir.parent, "kpm")
        kpmDir.deleteRecursively()
        kpmDir.mkdirs()

        val rand = (1..4).map { ('a'..'z').random() }.joinToString("")
        val kpmFile = kpmDir.getChildFile("${rand}.kpm")

        return try {
            val inputStream = apApp.contentResolver.openInputStream(uri)
                ?: throw IOException("Failed to open URI")
            inputStream.buffered().use { input ->
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

    override fun unloadModule(name: String): Boolean {
        return Natives.unloadKernelPatchModule(name) == 0L
    }

    override fun controlModule(name: String, param: String): Natives.KPMCtlRes {
        return Natives.kernelPatchModuleControl(name, param)
    }
}
