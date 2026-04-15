package me.bmax.apatch.util.patch

import android.system.Os
import com.topjohnwu.superuser.nio.ExtendedFile
import com.topjohnwu.superuser.nio.FileSystemManager
import me.bmax.apatch.apApp
import me.bmax.apatch.util.writeTo
import java.io.File

object PatchEnv {
    val patchDir: ExtendedFile = FileSystemManager.getLocal().getFile(apApp.filesDir.parent, "patch")
    var srcBoot: ExtendedFile = patchDir.getChildFile("boot.img")

    fun prepare(): String? {
        try {
            patchDir.deleteRecursively()
            patchDir.mkdirs()

            val execs = listOf("libkptools.so", "libbusybox.so", "libkpatch.so", "libbootctl.so")
            val info = apApp.applicationInfo
            val libs = File(info.nativeLibraryDir).listFiles { _, name ->
                execs.contains(name)
            } ?: emptyArray()

            for (lib in libs) {
                val name = lib.name.substring(3, lib.name.length - 3)
                Os.symlink(lib.path, "$patchDir/$name")
            }

            listOf("boot_patch.sh", "boot_unpatch.sh", "boot_install.sh", "boot_extract.sh", "util_functions.sh", "kpimg")
                .forEach { script ->
                    apApp.assets.open(script).writeTo(File(patchDir, script))
                }

            // set permissions
            patchDir.listFiles()?.forEach { it.setExecutable(true, false) }
            return null
        } catch (e: Exception) {
            return e.message
        }
    }
}