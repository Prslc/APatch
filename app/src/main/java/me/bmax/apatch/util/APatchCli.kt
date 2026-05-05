package me.bmax.apatch.util

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Log
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ShellUtils
import com.topjohnwu.superuser.internal.MainShell
import com.topjohnwu.superuser.io.SuFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.BuildConfig
import me.bmax.apatch.apApp
import me.bmax.apatch.ui.navigation.MODULE_TYPE
import org.json.JSONArray
import java.io.File
import java.security.MessageDigest

private const val TAG = "APatchCli"

val dataDir: String
    get() = Environment.getDataDirectory().absolutePath

private fun getKPatchPath(): String {
    return apApp.applicationInfo.nativeLibraryDir + File.separator + "libkpatch.so"
}

class RootShellInitializer : Shell.Initializer() {
    override fun onInit(context: Context, shell: Shell): Boolean {
        shell.newJob().add("""export PATH=${'$'}PATH:/system_ext/bin:/vendor/bin""").exec()
        return true
    }
}

/**
 * Core shell creation logic (nested version).
 * @param globalMnt Whether to use global mount namespace.
 * @param asMain Whether to register as the global main shell.
 */
private fun createShell(globalMnt: Boolean, asMain: Boolean): Shell {
    Shell.enableVerboseLogging = BuildConfig.DEBUG
    val builder = Shell.Builder.create()
        .setInitializers(RootShellInitializer::class.java)

    val shell = try {
        if (globalMnt) {
            builder.setCommands("su", "-M")
        } else {
            builder.setCommands("su")
        }
        builder.build()
    } catch (e: Throwable) {
        Log.e(TAG, "su failed: ", e)
        try {
            val cmds = if (globalMnt) {
                arrayOf(
                    getKPatchPath(), APApplication.superKey, "su", "-Z",
                    APApplication.MAGISK_SCONTEXT, "--mount-master"
                )
            } else {
                arrayOf(
                    getKPatchPath(), APApplication.superKey, "su", "-Z",
                    APApplication.MAGISK_SCONTEXT
                )
            }
            builder.setCommands(*cmds)
            builder.build()
        } catch (e: Throwable) {
            Log.e(TAG, "retry kpatch su failed: ", e)
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

inline fun <T> withNewRootShell(
    globalMnt: Boolean = false,
    block: Shell.() -> T
): T {
    return createRootShell(globalMnt).use(block)
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

fun execApd(args: String, newShell: Boolean = false): Boolean {
    return if (newShell) {
        withNewRootShell {
            ShellUtils.fastCmdResult(this, "${APApplication.APD_PATH} $args")
        }
    } else {
        ShellUtils.fastCmdResult(getRootShell(), "${APApplication.APD_PATH} $args")
    }
}

fun listModules(): String {
    val shell = getRootShell()
    val out = shell.newJob()
        .add("${APApplication.APD_PATH} module list")
        .to(ArrayList(), null)
        .exec().out

    try {
        val dstDir = SuFile("$dataDir/adb/ap/")
        if (!dstDir.exists()) dstDir.mkdirs()

        SuFile("$dataDir/user").listFiles()?.forEach { userDir ->
            val oriFile = SuFile(userDir, "me.bmax.apatch/patch/ori.img")
            if (oriFile.exists()) {
                val dstFile = SuFile(dstDir, oriFile.name)
                oriFile.newInputStream().use { input ->
                    dstFile.newOutputStream(false).use { output ->
                        input.copyTo(output)
                    }
                }
                oriFile.delete()
            }
        }
    } catch (e: Throwable) {
        Log.e("ModuleUtil", "SuFile operation failed", e)
    }

    return out.joinToString("\n").ifBlank { "[]" }
}

fun getModuleCount(): Int {
    val result = listModules()
    runCatching {
        val array = JSONArray(result)
        return array.length()
    }.getOrElse { return 0 }
}

fun toggleModule(id: String, enable: Boolean): Boolean {
    val cmd = if (enable) {
        "module enable $id"
    } else {
        "module disable $id"
    }
    val result = execApd(cmd,true)
    Log.i(TAG, "$cmd result: $result")
    return result
}

fun uninstallModule(id: String): Boolean {
    val cmd = "module uninstall $id"
    val result = execApd(cmd,true)
    Log.i(TAG, "uninstall module $id result: $result")
    return result
}

fun undoUninstallModule(id: String): Boolean {
    val cmd = "module undo-uninstall $id"
    val result = execApd(cmd,true)
    Log.i(TAG, "undo-uninstall module $id result: $result")
    return result
}

suspend fun installModule(
    uri: Uri,
    type: MODULE_TYPE,
    onStdout: (String) -> Unit,
    onStderr: (String) -> Unit
): Boolean = withContext(Dispatchers.IO) {
    val resolver = apApp.contentResolver
    val file = File(apApp.cacheDir, "module_${type}_${System.currentTimeMillis()}.zip")

    resolver.openInputStream(uri)?.use { input ->
        file.outputStream().use { output ->
            input.copyTo(output)
        }
    }

    val stdoutCallback = object : CallbackList<String?>() {
        override fun onAddElement(s: String?) { onStdout(s ?: "") }
    }
    val stderrCallback = object : CallbackList<String?>() {
        override fun onAddElement(s: String?) { onStderr(s ?: "") }
    }

    val result = withNewRootShell {
        val cmd = "${APApplication.APD_PATH} module install \"${file.absolutePath}\""
        newJob().add(cmd).to(stdoutCallback, stderrCallback).exec()
    }

    Log.i(TAG, "install $type module result: ${result.isSuccess}")

    if (file.exists()) file.delete()

    result.isSuccess
}

suspend fun runAPModuleAction(
    moduleId: String,
    onStdout: (String) -> Unit,
    onStderr: (String) -> Unit
): Boolean = withContext(Dispatchers.IO) {
    val stdoutCallback = object : CallbackList<String?>() {
        override fun onAddElement(s: String?) { onStdout(s ?: "") }
    }

    val stderrCallback = object : CallbackList<String?>() {
        override fun onAddElement(s: String?) { onStderr(s ?: "") }
    }

    val result = withNewRootShell {
        newJob().add("${APApplication.APD_PATH} module action \"$moduleId\"")
            .to(stdoutCallback, stderrCallback)
            .exec()
    }

    Log.i(TAG, "APModule runAction $moduleId result: ${result.isSuccess}")

    result.isSuccess
}

fun reboot(reason: String = "") {
    val shell = getRootShell()
    val job = shell.newJob()
    if (reason == "recovery") {
        job.add("/system/bin/input keyevent 26")
    }
    job.add("/system/bin/svc power reboot $reason || /system/bin/reboot $reason")
    job.exec()
}

fun hasMagisk(): Boolean {
    val shell = getRootShell()
    val result = shell.newJob().add("nsenter --mount=/proc/1/ns/mnt which magisk").exec()
    Log.i(TAG, "has magisk: ${result.isSuccess}")
    return result.isSuccess
}

fun isGlobalNamespaceEnabled(): Boolean {
    val shell = getRootShell()
    val result = ShellUtils.fastCmd(shell, "cat ${APApplication.GLOBAL_NAMESPACE_FILE}")
    Log.i(TAG, "is global namespace enabled: $result")
    return result == "1"
}

fun setGlobalNamespaceEnabled(value: String) {
    getRootShell().newJob().add("echo $value > ${APApplication.GLOBAL_NAMESPACE_FILE}")
        .submit { result ->
            Log.i(TAG, "setGlobalNamespaceEnabled result: ${result.isSuccess} [${result.out}]")
        }
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
