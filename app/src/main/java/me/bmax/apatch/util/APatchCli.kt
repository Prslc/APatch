package me.bmax.apatch.util

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.OpenableColumns
import android.system.Os
import android.util.Base64
import android.util.Log
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.internal.MainShell
import com.topjohnwu.superuser.io.SuFile
import me.bmax.apatch.APApplication
import me.bmax.apatch.BuildConfig
import me.bmax.apatch.R
import me.bmax.apatch.apApp
import java.io.File
import java.security.MessageDigest

private const val TAG = "APatchCli"

/** Reboot destinations offered by the reboot menus. */
enum class RebootMode(val labelRes: Int, val reason: String) {
    NORMAL(R.string.reboot, ""),
    SOFT(R.string.reboot_soft, "soft_reboot"),
    RECOVERY(R.string.reboot_recovery, "recovery"),
    BOOTLOADER(R.string.reboot_bootloader, "bootloader"),
    DOWNLOAD(R.string.reboot_download, "download"),
    EDL(R.string.reboot_edl, "edl"),
}

val dataDir: String
    get() = Environment.getDataDirectory().absolutePath

class RootShellInitializer : Shell.Initializer() {
    override fun onInit(context: Context, shell: Shell): Boolean {
        shell.newJob().add("""export PATH=${'$'}PATH:/system_ext/bin:/vendor/bin""").exec()
        return true
    }
}

/**
 * @param globalMnt Whether to use global mount namespace.
 * @param asMain Whether to register as the global main shell.
 */
private fun createShell(globalMnt: Boolean, asMain: Boolean): Shell {
    Shell.enableVerboseLogging = BuildConfig.DEBUG
    val builder = Shell.Builder.create()
        .setInitializers(RootShellInitializer::class.java)

    val shell = try {
        // KernelPatch supercall is the primary root channel; the truncate
        // binary is hooked by the kernel to raise to the magisk context.
        if (globalMnt) {
            builder.setCommands(
                APApplication.SUPERCMD, APApplication.superKey,
                "-Z", APApplication.MAGISK_SCONTEXT, "--mount-master"
            )
        } else {
            builder.setCommands(
                APApplication.SUPERCMD, APApplication.superKey,
                "-Z", APApplication.MAGISK_SCONTEXT
            )
        }
        builder.build()
    } catch (e: Throwable) {
        Log.e(TAG, "truncate su failed: ", e)
        try {
            if (globalMnt) {
                builder.setCommands("su", "-M")
            } else {
                builder.setCommands("su")
            }
            builder.build()
        } catch (e2: Throwable) {
            Log.e(TAG, "su failed: ", e2)
            builder.setCommands("sh")
            builder.build()
        }
    }

    if (asMain) MainShell.setBuilder(builder)
    return shell
}

/** Create a temporary root shell. */
fun createRootShell(globalMnt: Boolean = false): Shell = createShell(globalMnt, asMain = false)

/** Create and register the global main shell. */
private fun createMainRootShell(): Shell = createShell(globalMnt = false, asMain = true)

/** Quick root shell for logging or diagnostics. */
fun tryGetRootShell(): Shell = createShell(globalMnt = false, asMain = false)

object APatchCli {
    var SHELL: Shell = createMainRootShell()
    val GLOBAL_MNT_SHELL: Shell = createRootShell(true)
    fun refresh() {
        val tmp = SHELL

        val clazz = MainShell::class.java // reset MainShell
        clazz.getDeclaredField("isInitMain").apply {
            isAccessible = true
            setBoolean(null, false)
            isAccessible = false
        }

        clazz.getDeclaredField("mainShell").apply {
            isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val arr = get(null) as Array<Any?>
            arr[0] = null
            isAccessible = false
        }

        clazz.getDeclaredField("mainBuilder").apply {
            isAccessible = true
            set(null, null)
            isAccessible = false
        }

        SHELL = createMainRootShell()
        tmp.close()
    }
}

fun getRootShell(globalMnt: Boolean = false): Shell {

    return if (globalMnt) APatchCli.GLOBAL_MNT_SHELL else {
        APatchCli.SHELL
    }
}

fun rootAvailable(): Boolean {
    val shell = getRootShell()
    return shell.isRoot
}

fun shellForResult(shell: Shell, vararg cmds: String): Shell.Result {
    val out = ArrayList<String>()
    val err = ArrayList<String>()
    return shell.newJob().add(*cmds).to(out, err).exec()
}

fun rootShellForResult(vararg cmds: String): Shell.Result {
    val out = ArrayList<String>()
    val err = ArrayList<String>()
    return getRootShell().newJob().add(*cmds).to(out, err).exec()
}

fun listModuleJson(): String {
    val shell = getRootShell()
    val out = shell.newJob()
        .add("${APApplication.APD_PATH} module list")
        .to(ArrayList(), null)
        .exec().out
    return out.joinToString("\n").ifBlank { "[]" }
}

fun reboot(mode: RebootMode = RebootMode.NORMAL) {
    if (mode == RebootMode.SOFT) {
        softReboot()
        return
    }
    val shell = getRootShell()
    val job = shell.newJob()
    if (mode == RebootMode.RECOVERY) {
        job.add("/system/bin/input keyevent 26")
    }
    job.add("/system/bin/svc power reboot ${mode.reason} || /system/bin/reboot ${mode.reason}")
    job.exec()
}

/** Soft reboot: restart the Android framework while keeping runtime-loaded modules. */
fun softReboot() {
    getRootShell().newJob().add("${APApplication.APD_PATH} soft-reboot").exec()
}

/**
 * Reboot that respects jailbreak mode: in jailbreak mode a full reboot would
 * unload the runtime-loaded module, so fall back to a soft reboot instead.
 */
fun rebootJailbreakAware() {
    if (isJailbreakMode()) {
        softReboot()
    } else {
        reboot()
    }
}

/**
 * Detect the Kernel Module Interface (KMI) of the running kernel, e.g.
 * `android14-5.15`, from `uname -r` (same parsing as KernelSU).
 */
fun getKmi(): String? {
    val release = runCatching { Os.uname().release }.getOrNull() ?: return null
    val m = Regex("(.* )?(\\d+\\.\\d+)(\\S+)?(android\\d+)(.*)").find(release) ?: return null
    return "${m.groupValues[4]}-${m.groupValues[2]}"
}

/** Asset name of the KernelPatch ko matching this device's kernel (KMI). */
fun jailbreakAssetName(): String? {
    val kmi = getKmi() ?: return null
    return "${kmi}_kernelpatch.ko"
}

/** Extract the bundled kernelpatch.ko for this device's kernel to the app files dir. */
fun extractJailbreakKo(): File? {
    val name = jailbreakAssetName() ?: return null
    val file = File(apApp.filesDir, "kernelpatch.ko")
    return runCatching {
        apApp.assets.open(name).use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        file
    }.getOrNull()
}

/**
 * Install jailbreak mode: extract the bundled kernelpatch.ko for this kernel to
 * the app files dir (no root needed), then trigger the magica chain via the
 * isolated app-zygote service. The apd then escalates to full root through adb
 * and runs `late-load` (loads the module, applies Magisk policy, marks jailbreak).
 */
fun installJailbreak(): Boolean {
    val ko = extractJailbreakKo() ?: return false
    if (!ko.exists() || ko.length() == 0L) {
        Log.e(TAG, "extracted jailbreak ko is missing or empty")
        return false
    }
    return try {
        val intent = Intent(apApp, me.bmax.apatch.magica.MagicaService::class.java)
        apApp.startService(intent)
        Log.i(TAG, "MagicaService started for jailbreak")
        true
    } catch (e: Throwable) {
        Log.e(TAG, "start MagicaService failed: $e")
        false
    }
}

/**
 * Whether jailbreak mode is active (the ko has been loaded and a marker written).
 * Permissive SELinux is a prerequisite for jailbreak.
 */
fun isJailbreakMode(): Boolean {
    return runCatching {
        val file = SuFile(APApplication.JAILBREAK_FILE)
        file.shell = getRootShell()
        file.exists()
    }.getOrDefault(false)
}

fun hasMagisk(): Boolean {
    val shell = getRootShell()
    val result = shell.newJob().add("nsenter --mount=/proc/1/ns/mnt which magisk").exec()
    Log.i(TAG, "has magisk: ${result.isSuccess}")
    return result.isSuccess
}

fun getFileNameFromUri(context: Context, uri: Uri): String? {
    var fileName: String? = null
    val contentResolver: ContentResolver = context.contentResolver
    val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            fileName = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
        }
    }
    return fileName
}

@Suppress("DEPRECATION")
private fun signatureFromAPI(context: Context): ByteArray? {
    return try {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.packageManager.getPackageInfo(
                context.packageName, PackageManager.GET_SIGNING_CERTIFICATES
            )
        } else {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES
            )
        }

        val signatures: Array<out Signature>? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                packageInfo.signatures
            }

        signatures?.firstOrNull()?.toByteArray()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun validateSignature(signatureBytes: ByteArray?, validSignature: String): Boolean {
    signatureBytes ?: return false
    val digest = MessageDigest.getInstance("SHA-256")
    val signatureHash = Base64.encodeToString(digest.digest(signatureBytes), Base64.NO_WRAP)
    return signatureHash == validSignature
}

fun verifyAppSignature(validSignature: String): Boolean {
    val context = apApp.applicationContext
    val apiSignature = signatureFromAPI(context)
    return validateSignature(apiSignature, validSignature)
}
