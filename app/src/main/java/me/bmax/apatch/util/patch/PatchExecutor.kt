package me.bmax.apatch.util.patch

import me.bmax.apatch.APApplication
import me.bmax.apatch.ui.viewmodel.KPModel
import me.bmax.apatch.ui.viewmodel.PatchesViewModel.PatchMode
import java.io.BufferedReader
import java.io.InputStreamReader

object PatchExecutor {
    fun buildPatchCommand(
        mode: PatchMode,
        superkey: String,
        bootPath: String,
        newExtras: List<KPModel.KPMInfo>,
        newExtrasFileName: List<String>,
        existedExtras: List<KPModel.IExtraInfo>,
        isSuAvailable: Boolean,
        magiskSContext: String
    ): List<String> {
        val cmd = mutableListOf<String>()

        if (mode == PatchMode.PATCH_AND_INSTALL || mode == PatchMode.INSTALL_TO_NEXT_SLOT) {
            if (!isSuAvailable) {
                cmd.addAll(listOf("truncate", superkey, "-Z", magiskSContext, "-c"))
                cmd.addAll(listOf("./busybox", "sh", "boot_patch.sh", superkey, bootPath, "true"))
            } else {
                cmd.addAll(listOf("./busybox", "sh", "boot_patch.sh", superkey, bootPath, "true"))
            }
        } else {
            // PATCH_ONLY
            cmd.addAll(listOf("./busybox", "sh", "boot_patch.sh", superkey, bootPath))
        }

        // Added KPM
        newExtras.forEachIndexed { i, extra ->
            cmd.addAll(listOf("-M", newExtrasFileName[i]))
            if (extra.args.isNotEmpty()) cmd.addAll(listOf("-A", extra.args))
            if (extra.event.isNotEmpty()) cmd.addAll(listOf("-V", extra.event))
            cmd.addAll(listOf("-T", extra.type.desc))
        }

        // Added Extras
        existedExtras.forEach { extra ->
            cmd.addAll(listOf("-E", extra.name))
            if (extra.args.isNotEmpty()) cmd.addAll(listOf("-A", extra.args))
            if (extra.event.isNotEmpty()) cmd.addAll(listOf("-V", extra.event))
            cmd.addAll(listOf("-T", extra.type.desc))
        }

        return cmd
    }

    fun buildUnpatchCommand(bootDev: String): List<String> {
        return listOf(
            "sh", "-c",
            "cp /data/adb/ap/ori.img new-boot.img && " +
            "./busybox sh ./boot_unpatch.sh $bootDev && " +
            "rm -f ${APApplication.APD_PATH} && " +
            "rm -rf ${APApplication.APATCH_FOLDER}"
        )
    }

    fun runRawProcess(
        command: List<String>,
        workDir: java.io.File,
        onLog: (String) -> Unit
    ): Boolean {
        return try {
            val builder = ProcessBuilder(command)
                .directory(workDir)
                .redirectErrorStream(true)
            builder.environment()["ASH_STANDALONE"] = "1"
            val process = builder.start()

            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    onLog(line!!)
                }
            }
            process.waitFor() == 0
        } catch (e: Exception) {
            onLog("Error: ${e.message}")
            false
        }
    }
}