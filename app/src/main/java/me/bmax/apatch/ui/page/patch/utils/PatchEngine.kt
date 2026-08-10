package me.bmax.apatch.ui.page.patch.utils

import android.net.Uri
import android.os.Build
import android.os.Environment
import android.system.Os
import android.util.Log
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.io.SuFile
import com.topjohnwu.superuser.nio.ExtendedFile
import com.topjohnwu.superuser.nio.FileSystemManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.BuildConfig
import me.bmax.apatch.apApp
import me.bmax.apatch.ui.page.kpm.KPModel
import me.bmax.apatch.ui.page.patch.PatchMode
import me.bmax.apatch.ui.page.patch.PatchUiState
import me.bmax.apatch.util.Version
import me.bmax.apatch.util.copyAndClose
import me.bmax.apatch.util.copyAndCloseOut
import me.bmax.apatch.util.dataDir
import me.bmax.apatch.util.inputStream
import me.bmax.apatch.util.shellForResult
import me.bmax.apatch.util.writeTo
import org.ini4j.Ini
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.io.StringReader

private const val TAG = "PatchEngine"

class PatchEngine(
    private val shell: Shell,
    private val onLog: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onStateUpdate: (PatchUiState.() -> PatchUiState) -> Unit
) {
    val patchDir: ExtendedFile = FileSystemManager.getLocal().getFile(apApp.filesDir.parent, "patch")
    var srcBoot: ExtendedFile = patchDir.getChildFile("boot.img")
    var isPrepared = false

    suspend fun prepareEnv() = withContext(Dispatchers.IO) {
        patchDir.deleteRecursively()
        patchDir.mkdirs()

        val execs = listOf("libkptools.so", "libbusybox.so", "libkpatch.so", "libbootctl.so")
        val info = apApp.applicationInfo
        val libs = File(info.nativeLibraryDir).listFiles { _, name -> execs.contains(name) } ?: emptyArray()

        for (lib in libs) {
            val name = lib.name.substring(3, lib.name.length - 3)
            val dest = File(patchDir, name)

            try {
                if (dest.exists() || dest.isSymbolicLink()) { dest.delete() }
                Os.symlink(lib.path, dest.absolutePath)
            } catch (e: Exception) {
                Log.w(TAG, "Symlink failed for $name, falling back to copy: ${e.message}")
                try {
                    lib.inputStream().use { input ->
                        dest.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    dest.setExecutable(true)
                } catch (copyEx: Exception) {
                    onError("Failed to setup binary $name: ${copyEx.message}")
                }
            }
        }

        for (script in listOf("boot_patch.sh", "boot_unpatch.sh", "boot_extract.sh", "util_functions.sh", "kpimg")) {
            val dest = File(patchDir, script)
            apApp.assets.open(script).writeTo(dest)
        }
        isPrepared = true
    }

    suspend fun parseKpimg() = withContext(Dispatchers.IO) {
        val result = shellForResult(shell, "cd $patchDir", "./kptools -l -k kpimg")
        if (result.isSuccess) {
            val ini = Ini(StringReader(result.out.joinToString("\n")))
            val kpimg = ini["kpimg"]
            if (kpimg != null) {
                onStateUpdate {
                    copy(
                        kpimgInfo = KPModel.KPImgInfo(
                            kpimg["version"].toString(),
                            kpimg["compile_time"].toString(),
                            kpimg["config"].toString(),
                            "", // manager no longer keeps a separate superkey
                            kpimg["root_superkey"].toString()   // empty
                        )
                    )
                }
            } else {
                onError("parse kpimg error\n")
            }
        } else {
            onError(result.err.joinToString("\n"))
        }
    }

    suspend fun parseBootimg(
        bootimg: String,
        currentSuperKey: String
    ) = withContext(Dispatchers.IO) {
        val result = shellForResult(
            shell,
            "cd $patchDir",
            "./kptools unpacknolog $bootimg",
            "./kptools -l -i kernel",
        )

        if (result.isSuccess) {
            val ini = Ini(StringReader(result.out.joinToString("\n")))
            Log.d(TAG, "kernel image info: $ini")

            val kernel = ini["kernel"]
            if (kernel == null) {
                onError("empty kernel section")
                onStateUpdate { copy(kimgInfo = KPModel.KImgInfo("", false)) }
                return@withContext
            }

            val isPatched = kernel["patched"].toBoolean()
            var validSuperKey = currentSuperKey
            val newExistedExtras = mutableListOf<KPModel.IExtraInfo>()
            var extractedSuperKey = ""

            if (isPatched) {
                extractedSuperKey = ini["kpimg"]?.getOrDefault("superkey", "") ?: ""
                if (checkSuperKeyValidation(extractedSuperKey)) {
                    validSuperKey = extractedSuperKey
                }

                val kpmNum = kernel["extra_num"]?.toInt() ?: ini["extras"]?.get("num")?.toInt()
                if (kpmNum != null && kpmNum > 0) {
                    for (i in 0..<kpmNum) {
                        val extra = ini["extra $i"]
                        if (extra == null) {
                            onError("empty extra section")
                            break
                        }
                        val type = KPModel.ExtraType.valueOf(extra["type"]!!.uppercase())
                        var event = extra["event"].toString()
                        if (event.isEmpty()) event = KPModel.TriggerEvent.PRE_KERNEL_INIT.event

                        if (type == KPModel.ExtraType.KPM) {
                            newExistedExtras.add(KPModel.KPMInfo(
                                type, extra["name"].toString(), event, extra["args"].toString(),
                                extra["version"].toString(), extra["license"].toString(),
                                extra["author"].toString(), extra["description"].toString()
                            ))
                        }
                    }
                }
            }

            onStateUpdate {
                val updatedKpimg = kpimgInfo.apply { superKey = extractedSuperKey }
                copy(
                    kimgInfo = KPModel.KImgInfo(kernel["banner"].toString(), isPatched),
                    kpimgInfo = updatedKpimg,
                    superkey = validSuperKey,
                    existedExtras = newExistedExtras
                )
            }
        } else {
            onError(result.err.joinToString("\n"))
            onStateUpdate { copy(kimgInfo = KPModel.KImgInfo("", false)) }
        }
    }

    suspend fun extractAndParseBootimg(mode: PatchMode, currentSuperKey: String) = withContext(Dispatchers.IO) {
        var cmdBuilder = "boot_extract.sh"
        if (mode == PatchMode.INSTALL_TO_NEXT_SLOT) cmdBuilder += " true"

        val result = shellForResult(shell, "export ASH_STANDALONE=1", "cd $patchDir", "./busybox sh $cmdBuilder")

        if (result.isSuccess) {
            val outSlot = if (!result.out.toString().contains("SLOT=")) "" else result.out.filter { it.startsWith("SLOT=") }[0].removePrefix("SLOT=")
            val outDev = result.out.filter { it.startsWith("BOOTIMAGE=") }[0].removePrefix("BOOTIMAGE=")

            Log.i(TAG, "current slot: $outSlot")
            Log.i(TAG, "current bootimg: $outDev")
            srcBoot = FileSystemManager.getLocal().getFile(outDev)

            onStateUpdate { copy(bootSlot = outSlot, bootDev = outDev) }
            parseBootimg(outDev, currentSuperKey)
        } else {
            onError(result.err.joinToString("\n"))
        }
    }

    suspend fun embedKPM(uri: Uri) = withContext(Dispatchers.IO) {
        val rand = (1..4).map { ('a'..'z').random() }.joinToString("")
        val kpmFileName = "${rand}.kpm"
        val kpmFile: ExtendedFile = patchDir.getChildFile(kpmFileName)

        Log.i(TAG, "copy kpm to: ${kpmFile.path}")
        try {
            uri.inputStream().buffered().use { src ->
                src.copyAndCloseOut(kpmFile.newOutputStream())
            }
        } catch (e: IOException) {
            Log.e(TAG, "Copy kpm error: $e")
        }

        val result = shellForResult(shell, "cd $patchDir", "./kptools -l -M ${kpmFile.path}")

        if (result.isSuccess) {
            val ini = Ini(StringReader(result.out.joinToString("\n")))
            val kpm = ini["kpm"]
            if (kpm != null) {
                val kpmInfo = KPModel.KPMInfo(
                    KPModel.ExtraType.KPM, kpm["name"].toString(),
                    KPModel.TriggerEvent.PRE_KERNEL_INIT.event, "",
                    kpm["version"].toString(), kpm["license"].toString(),
                    kpm["author"].toString(), kpm["description"].toString()
                )
                onStateUpdate {
                    copy(
                        newExtras = newExtras + kpmInfo,
                        newExtrasFileName = newExtrasFileName + kpmFileName
                    )
                }
            }
        } else {
            onError("Invalid KPM\n")
        }
    }

    // Move the ori.img backup to /data/adb/ap, where doUnpatch reads it.
    private fun migrateOriImage() {
        runCatching {
            val dstDir = SuFile("$dataDir/adb/ap/")
            if (!dstDir.exists()) dstDir.mkdirs()

            val sources = mutableListOf(patchDir.getChildFile("ori.img"))
            // Legacy locations: older versions left backups in per-user dirs.
            SuFile("$dataDir/user").listFiles()?.forEach { userDir ->
                sources.add(SuFile(userDir, "me.bmax.apatch/patch/ori.img"))
            }

            for (src in sources) {
                if (!src.exists()) continue
                val dst = SuFile(dstDir, "ori.img")
                src.newInputStream().use { input ->
                    dst.newOutputStream(false).use { output -> input.copyTo(output) }
                }
                src.delete()
                Log.i(TAG, "migrated ori.img from ${src.path} to ${dst.path}")
            }
        }.onFailure {
            Log.e(TAG, "migrate ori.img failed", it)
        }
    }

    suspend fun doUnpatch(bootDev: String) = withContext(Dispatchers.IO) {
        Log.i(TAG, "starting unpatching...")
        // Fallback: migrate any ori.img leftover from older versions.
        migrateOriImage()
        val logs = object : CallbackList<String>() {
            override fun onAddElement(e: String?) {
                if (e != null) onLog(e)
            }
        }

        val result = shell.newJob().add(
            "export ASH_STANDALONE=1", "cd $patchDir",
            "cp /data/adb/ap/ori.img new-boot.img",
            "./busybox sh ./boot_unpatch.sh $bootDev",
            "rm -f ${APApplication.APD_PATH}",
            "rm -rf ${APApplication.APATCH_FOLDER}"
        ).to(logs, logs).exec()

        if (result.isSuccess) {
            logs.add(" Unpatch successful")
            onStateUpdate { copy(needReboot = true) }
            APApplication.markNeedReboot()
        } else {
            logs.add(" Unpatched failed")
            onError(result.err.joinToString("\n"))
        }
        logs.add("****************************")
    }

    suspend fun doPatch(state: PatchUiState, mode: PatchMode, useKey: Boolean) = withContext(Dispatchers.IO) {
        if (state.kimgInfo.banner.isEmpty()) {
            onError("Aborting: No valid kernel detected to patch.")
            return@withContext
        }

        Log.d(TAG, "starting patching...")
        val apVer = Version.getManagerVersion().second
        val rand = (1..4).map { ('a'..'z').random() }.joinToString("")
        val outFilename = "apatch_patched_${apVer}_${BuildConfig.buildKPV}_${rand}.img"

        val logs = object : CallbackList<String>() {
            override fun onAddElement(e: String?) {
                if (e != null) onLog(e)
            }
        }
        logs.add("****************************")

        var patchCommand = mutableListOf("./busybox sh boot_patch.sh \"$0\" \"$@\"")
        var isKpOld = false
        val superkeyToUse = if (useKey && state.superkey.isNotEmpty()) state.superkey else "su"

        if (mode == PatchMode.PATCH_AND_INSTALL || mode == PatchMode.INSTALL_TO_NEXT_SLOT) {
            val KPCheck = shell.newJob().add(
                "${APApplication.SUPERCMD} ${APApplication.superKey} -Z ${APApplication.MAGISK_SCONTEXT} -c whoami"
            ).exec()
            if (KPCheck.isSuccess && !isSuExecutable()) {
                patchCommand.addAll(0, listOf(
                    APApplication.SUPERCMD, APApplication.superKey, "-Z", APApplication.MAGISK_SCONTEXT, "-c"
                ))
                patchCommand.addAll(listOf(superkeyToUse, srcBoot.path, "true"))
            } else {
                patchCommand = mutableListOf("./busybox", "sh", "boot_patch.sh")
                patchCommand.addAll(listOf(superkeyToUse, srcBoot.path, "true"))
                isKpOld = true
            }
        } else {
            patchCommand.addAll(0, listOf("sh", "-c"))
            patchCommand.addAll(listOf(superkeyToUse, srcBoot.path))
        }

        for (i in state.newExtrasFileName.indices) {
            patchCommand.addAll(listOf("-M", state.newExtrasFileName[i]))
            val extra = state.newExtras[i]
            if (extra.args.isNotEmpty()) patchCommand.addAll(listOf("-A", extra.args))
            if (extra.event.isNotEmpty()) patchCommand.addAll(listOf("-V", extra.event))
            patchCommand.addAll(listOf("-T", extra.type.desc))
        }

        for (extra in state.existedExtras) {
            patchCommand.addAll(listOf("-E", extra.name))
            if (extra.args.isNotEmpty()) patchCommand.addAll(listOf("-A", extra.args))
            if (extra.event.isNotEmpty()) patchCommand.addAll(listOf("-V", extra.event))
            patchCommand.addAll(listOf("-T", extra.type.desc))
        }

        val builder = ProcessBuilder(patchCommand)
        Log.i(TAG, "patchCommand: $patchCommand")
        var succ: Boolean

        if (isKpOld) {
            val resultString = "\"" + patchCommand.joinToString(separator = "\" \"") + "\""
            val result = shell.newJob().add(
                "export ASH_STANDALONE=1", "cd $patchDir", resultString
            ).to(logs, logs).exec()
            succ = result.isSuccess
        } else {
            builder.environment()["ASH_STANDALONE"] = "1"
            builder.directory(patchDir)
            builder.redirectErrorStream(true)
            val process = builder.start()

            Thread {
                BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        onLog(line!!)
                    }
                }
            }.start()
            succ = process.waitFor() == 0
        }

        if (!succ) {
            onError(" Patch failed.")
            logs.add("****************************")
            return@withContext
        }

        // Persist the ori.img backup before the patch dir can be wiped later.
        migrateOriImage()

        if (mode == PatchMode.PATCH_AND_INSTALL) {
            logs.add("- Reboot to finish the installation...")
            onStateUpdate { copy(needReboot = true) }
            APApplication.markNeedReboot()
        } else if (mode == PatchMode.INSTALL_TO_NEXT_SLOT) {
            logs.add("- Connecting boot hal...")
            val bootctlStatus = shell.newJob().add("cd $patchDir", "chmod 0777 $patchDir/bootctl", "./bootctl hal-info").to(logs, logs).exec()
            if (!bootctlStatus.isSuccess) {
                logs.add("[X] Failed to connect to boot hal, you may need switch slot manually")
            } else {
                val currSlot = shellForResult(shell, "cd $patchDir", "./bootctl get-current-slot").out.toString()
                val targetSlot = if (currSlot.contains("0")) 1 else 0
                logs.add("- Switching to next slot: $targetSlot...")

                val setNextActiveSlot = shell.newJob().add("cd $patchDir", "./bootctl set-active-boot-slot $targetSlot").exec()
                if (setNextActiveSlot.isSuccess) {
                    logs.add("- Switch done")
                    logs.add("- Writing boot marker script...")
                    val markBootableScript = shell.newJob().add(
                        "mkdir -p /data/adb/post-fs-data.d && rm -rf /data/adb/post-fs-data.d/post_ota.sh && touch /data/adb/post-fs-data.d/post_ota.sh",
                        "echo \"chmod 0777 $patchDir/bootctl\" > /data/adb/post-fs-data.d/post_ota.sh",
                        "echo \"chown root:root 0777 $patchDir/bootctl\" > /data/adb/post-fs-data.d/post_ota.sh",
                        "echo \"$patchDir/bootctl mark-boot-successful\" > /data/adb/post-fs-data.d/post_ota.sh",
                        "echo >> /data/adb/post-fs-data.d/post_ota.sh",
                        "echo \"rm -rf $patchDir\" >> /data/adb/post-fs-data.d/post_ota.sh",
                        "echo >> /data/adb/post-fs-data.d/post_ota.sh",
                        "echo \"rm -f /data/adb/post-fs-data.d/post_ota.sh\" >> /data/adb/post-fs-data.d/post_ota.sh",
                        "chmod 0777 /data/adb/post-fs-data.d/post_ota.sh",
                        "chown root:root /data/adb/post-fs-data.d/post_ota.sh",
                    ).to(logs, logs).exec()

                    if (markBootableScript.isSuccess) logs.add("- Boot marker script write done")
                    else logs.add("[X] Boot marker scripts write failed")
                }
            }
            logs.add("- Reboot to finish the installation...")
            onStateUpdate { copy(needReboot = true) }
            APApplication.markNeedReboot()
        } else if (mode == PatchMode.PATCH_ONLY) {
            val newBootFile = patchDir.getChildFile("new-boot.img")
            val outDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!outDir.exists()) outDir.mkdirs()
            val outPath = File(outDir, outFilename)
            val inputUri = newBootFile.getUri(apApp)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val outUri = createDownloadUri(apApp, outFilename)
                succ = insertDownload(apApp, outUri, inputUri)
            } else {
                newBootFile.inputStream().copyAndClose(outPath.outputStream())
            }

            if (succ) {
                logs.add(" Output file is written to ")
                logs.add(" ${outPath.path}")
            } else {
                logs.add(" Write patched boot.img failed")
            }
        }
        logs.add("****************************")
    }
}