package me.bmax.apatch.data.repository

import android.net.Uri
import android.util.Log
import com.topjohnwu.superuser.nio.FileSystemManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.Natives
import me.bmax.apatch.apApp
import me.bmax.apatch.ui.page.kpm.KPModel
import me.bmax.apatch.util.rootShellForResult
import me.bmax.apatch.util.safeKpmModuleId
import org.ini4j.Ini
import java.io.File
import java.io.IOException
import java.io.StringReader

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
                    getValue("description")
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

    override suspend fun installModule(uri: Uri): Int = withContext(Dispatchers.IO) {
        val tempDir = File(apApp.cacheDir, "kpm-install").apply {
            deleteRecursively()
            mkdirs()
        }
        val rand = (1..4).map { ('a'..'z').random() }.joinToString("")
        val temp = File(tempDir, "$rand.kpm")
        try {
            apApp.contentResolver.openInputStream(uri)?.use { input ->
                temp.outputStream().use { output -> input.copyTo(output) }
            } ?: return@withContext -1

            val infoResult = rootShellForResult(
                "${APApplication.APATCH_BIN_FOLDER}kptools -l -M '${temp.path}'"
            )
            if (!infoResult.isSuccess) return@withContext -2
            val section = Ini(StringReader(infoResult.out.joinToString("\n")))["kpm"]
                ?: return@withContext -3
            val name = section["name"]?.toString()?.trim().orEmpty()
            if (name.isEmpty()) return@withContext -4

            val id = safeKpmModuleId(name)
            val dir = "${APApplication.KPMS_DIR}$id"
            val destination = "$dir/$id.kpm"
            val result = rootShellForResult(
                "mkdir -p '$dir' && cp -f '${temp.path}' '$destination'"
            )
            if (!result.isSuccess) return@withContext -5

            // Installed KPMs are loaded by the boot-time loader. Do not load them
            // in the current session; installation takes effect after reboot.
            Log.i(TAG, "install KPM $name to $destination; reboot required")
            0
        } catch (e: Exception) {
            Log.e(TAG, "install KPM failed", e)
            -1
        } finally {
            tempDir.deleteRecursively()
        }
    }

    override suspend fun removeInstalledModule(id: String): Boolean =
        withContext(Dispatchers.IO) {
            val safe = safeKpmModuleId(id)
            val dir = "${APApplication.KPMS_DIR}$safe"
            rootShellForResult("rm -rf '$dir' && test ! -e '$dir'").isSuccess
        }

    override suspend fun setModuleDisabled(id: String, disabled: Boolean): Boolean =
        withContext(Dispatchers.IO) {
            val safe = safeKpmModuleId(id)
            val marker = "${APApplication.KPMS_DIR}$safe/disable"
            val command = if (disabled) "touch '$marker'" else "rm -f '$marker'"
            rootShellForResult(command).isSuccess
        }
}
