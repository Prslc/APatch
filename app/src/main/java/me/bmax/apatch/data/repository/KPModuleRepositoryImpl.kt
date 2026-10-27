package me.bmax.apatch.data.repository

import android.net.Uri
import android.util.Log
import com.topjohnwu.superuser.nio.FileSystemManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.Natives
import me.bmax.apatch.apApp
import me.bmax.apatch.ui.page.kpm.KPModel
import me.bmax.apatch.util.rootAvailable
import me.bmax.apatch.util.rootShellForResult
import me.bmax.apatch.util.safeKpmModuleId
import org.ini4j.Ini
import java.io.File
import java.io.IOException
import java.io.StringReader

object KPModuleRepositoryImpl : KPModuleRepository {
    private const val TAG = "KPModuleRepo"
    private val installMutex = Mutex()

    override suspend fun listModules(): List<KPModel.KPMInfo> {
        // A non-root shell makes the on-disk scan below return an empty list; surface
        // that as an error so the UI can retry instead of showing "no modules".
        check(rootAvailable()) { "Root is not available" }

        val result = linkedMapOf<String, KPModel.KPMInfo>()

        val names = if (Natives.kernelPatchModuleNum() > 0) {
            Natives.kernelPatchModuleList()
        } else {
            ""
        }
        names.split('\n').map(String::trim).filter(String::isNotBlank).forEach { kernelName ->
            val lines = Natives.kernelPatchModuleInfo(kernelName).split('\n')
            fun value(key: String) =
                lines.firstOrNull { it.startsWith("$key=") }?.removePrefix("$key=") ?: ""

            val name = value("name").ifBlank { kernelName }
            val info = KPModel.KPMInfo(
                type = KPModel.ExtraType.KPM,
                name = name,
                event = value("load_event"),
                args = value("args"),
                version = value("version"),
                license = value("license"),
                author = value("author"),
                description = value("description"),
                moduleId = safeKpmModuleId(kernelName),
                loadSource = value("load_source").ifBlank {
                    if (value("embedded") == "1") "embedded" else ""
                },
                installed = false,
                disabled = false
            )
            result[info.moduleId] = info
        }

        val dirs = rootShellForResult(
            "find ${APApplication.KPMS_DIR} -mindepth 1 -maxdepth 1 -type d -print"
        ).out.map(String::trim).filter(String::isNotBlank)

        dirs.forEach { path ->
            val dirName = path.substringAfterLast('/')
            val id = safeKpmModuleId(dirName)
            val file = "$path/$dirName.kpm"
            val disabled = rootShellForResult("[ -e '$path/disable' ]").isSuccess
            val parsed = parseKpmInfo(
                rootShellForResult("${APApplication.APATCH_BIN_FOLDER}kptools -l -M '$file'")
                    .out.joinToString("\n"),
                fallbackId = dirName
            ) ?: KPModel.KPMInfo(
                type = KPModel.ExtraType.KPM,
                name = dirName,
                event = "",
                args = "",
                version = "",
                license = "",
                author = "",
                description = "",
                moduleId = dirName,
                installed = true,
                disabled = disabled
            )
            val current = result[id] ?: parsed
            result[id] = current.copy(
                moduleId = dirName,
                installed = true,
                disabled = disabled,
                version = current.version.ifBlank { parsed.version },
                license = current.license.ifBlank { parsed.license },
                author = current.author.ifBlank { parsed.author },
                description = current.description.ifBlank { parsed.description },
                storedVersion = parsed.version
            )
        }

        return result.values.toList()
    }

    private fun parseKpmInfo(raw: String, fallbackId: String): KPModel.KPMInfo? = runCatching {
        val section = Ini(StringReader(raw))["kpm"] ?: return null
        val name = section["name"]?.trim().orEmpty().ifEmpty { fallbackId }
        KPModel.KPMInfo(
            type = KPModel.ExtraType.KPM,
            name = name,
            event = section["load_event"].orEmpty(),
            args = section["args"].orEmpty(),
            version = section["version"].orEmpty(),
            license = section["license"].orEmpty(),
            author = section["author"].orEmpty(),
            description = section["description"].orEmpty(),
            moduleId = safeKpmModuleId(name),
            loadSource = section["load_source"].orEmpty().ifBlank {
                if (section["embedded"]?.trim() == "1") "embedded" else ""
            }
        )
    }.getOrNull()

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

    override suspend fun installModule(uri: Uri): Int = installMutex.withLock {
        withContext(Dispatchers.IO) {
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
                val name = section["name"]?.trim().orEmpty()
                if (name.isEmpty()) return@withContext -4

                val id = safeKpmModuleId(name)
                val dir = "${APApplication.KPMS_DIR}$id"
                val destination = "$dir/$id.kpm"
                val result = rootShellForResult(
                    "mkdir -p '$dir' && cp -f '${temp.path}' '$destination.tmp' && mv -f '$destination.tmp' '$destination'"
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
