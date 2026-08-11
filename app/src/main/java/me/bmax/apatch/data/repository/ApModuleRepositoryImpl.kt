package me.bmax.apatch.data.repository

import android.net.Uri
import android.util.Log
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.ShellUtils
import com.topjohnwu.superuser.io.SuFile
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.apApp
import me.bmax.apatch.ui.navigation.MODULE_TYPE
import me.bmax.apatch.ui.page.apm.ModuleInfo
import me.bmax.apatch.util.HanziToPinyin
import me.bmax.apatch.util.createRootShell
import me.bmax.apatch.util.listModuleJson
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale

object ApModuleRepositoryImpl : ApModuleRepository {
    private const val TAG = "ApModuleRepo"

    override suspend fun listModules(): Result<List<ModuleInfo>> {
        return runCatching {
            val result = listModuleJson()
            val array = JSONArray(result)
            val h2p = HanziToPinyin.getInstance()

            (0 until array.length()).map { i ->
                parseModuleInfo(array.getJSONObject(i), h2p)
            }
        }
    }

    override suspend fun getModuleCount(): Int {
        return listModuleJson().let { result ->
            runCatching {
                JSONArray(result).length()
            }.getOrDefault(0)
        }
    }

    override suspend fun checkModuleUpdate(module: ModuleInfo): Triple<String, String, String> {
        if (module.updateJson.isEmpty() || module.remove || module.update || !module.enabled) {
            return Triple("", "", "")
        }

        val response = runCatching {
            val request = okhttp3.Request.Builder().url(module.updateJson).build()
            apApp.okhttpClient.newCall(request).execute()
        }.getOrNull()

        val body = response?.takeIf { it.isSuccessful }?.body?.string() ?: return Triple("", "", "")
        val updateJson = runCatching { JSONObject(body) }.getOrNull() ?: return Triple("", "", "")

        val version = updateJson.optString("version", "").sanitizeVersion()
        val versionCode = updateJson.optInt("versionCode", 0)
        val zipUrl = updateJson.optString("zipUrl", "")
        val changelog = updateJson.optString("changelog", "")

        if (versionCode <= module.versionCode || zipUrl.isEmpty()) {
            return Triple("", "", "")
        }
        return Triple(zipUrl, version, changelog)
    }

    override suspend fun checkMetaModuleWarning(modules: List<ModuleInfo>): String? {
        val needsMount = modules.any { module ->
            val moduleDir = "/data/adb/modules/${module.id}"
            SuFile.open("$moduleDir/system").isDirectory &&
                    !SuFile.open("$moduleDir/skip_mount").isFile
        }
        if (!needsMount) return null

        val metaDir = "/data/adb/metamodule"
        return when {
            !SuFile.open("$metaDir/module.prop").isFile -> apApp.getString(R.string.no_meta_module_installed)
            SuFile.open("$metaDir/remove").isFile -> apApp.getString(R.string.meta_module_removed)
            SuFile.open("$metaDir/disable").isFile -> apApp.getString(R.string.meta_module_disabled)
            else -> null
        }
    }

    override suspend fun toggleModule(id: String, enable: Boolean): Boolean {
        val cmd = if (enable) "module enable ${ShellUtils.escapedString(id)}"
        else "module disable ${ShellUtils.escapedString(id)}"
        val result = execApd(cmd)
        Log.i(TAG, "$cmd result: $result")
        return result
    }

    override suspend fun uninstallModule(id: String): Boolean {
        val result = execApd("module uninstall ${ShellUtils.escapedString(id)}")
        Log.i(TAG, "uninstall module $id result: $result")
        return result
    }

    override suspend fun undoUninstallModule(id: String): Boolean {
        val result = execApd("module undo-uninstall ${ShellUtils.escapedString(id)}")
        Log.i(TAG, "undo-uninstall module $id result: $result")
        return result
    }

    override suspend fun installModule(
        uri: Uri,
        type: MODULE_TYPE,
        onStdout: (String) -> Unit,
        onStderr: (String) -> Unit
    ): Boolean {
        val resolver = apApp.contentResolver
        val file = File(apApp.cacheDir, "module_${type}_${System.currentTimeMillis()}.zip")

        resolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }

        val stdoutCb = object : CallbackList<String?>() {
            override fun onAddElement(s: String?) { onStdout(s ?: "") }
        }
        val stderrCb = object : CallbackList<String?>() {
            override fun onAddElement(s: String?) { onStderr(s ?: "") }
        }

        val result = createRootShell().use { shell ->
            shell.newJob()
                .add("${APApplication.APD_PATH} module install \"${file.absolutePath}\"")
                .to(stdoutCb, stderrCb)
                .exec()
        }

        if (file.exists()) file.delete()
        Log.i(TAG, "install $type module result: ${result.isSuccess}")
        return result.isSuccess
    }

    override suspend fun runAction(
        moduleId: String,
        onStdout: (String) -> Unit,
        onStderr: (String) -> Unit
    ): Boolean {
        val stdoutCb = object : CallbackList<String?>() {
            override fun onAddElement(s: String?) { onStdout(s ?: "") }
        }
        val stderrCb = object : CallbackList<String?>() {
            override fun onAddElement(s: String?) { onStderr(s ?: "") }
        }

        val result = createRootShell().use { shell ->
            shell.newJob()
                .add("${APApplication.APD_PATH} module action ${ShellUtils.escapedString(moduleId)}")
                .to(stdoutCb, stderrCb)
                .exec()
        }
        Log.i(TAG, "runAction $moduleId result: ${result.isSuccess}")
        return result.isSuccess
    }

    private fun execApd(args: String): Boolean {
        return createRootShell().use { shell ->
            ShellUtils.fastCmdResult(shell, "${APApplication.APD_PATH} $args")
        }
    }

    private fun parseModuleInfo(obj: JSONObject, h2p: HanziToPinyin): ModuleInfo {
        val name = obj.optString("name")
        return ModuleInfo(
            id = obj.getString("id"),
            name = name,
            pinyinName = h2p.toPinyinString(name).lowercase(Locale.getDefault()),
            author = obj.optString("author", "Unknown"),
            version = obj.optString("version", "Unknown"),
            versionCode = obj.optInt("versionCode", 0),
            description = obj.optString("description"),
            enabled = obj.getBoolean("enabled"),
            update = obj.getBoolean("update"),
            remove = obj.getBoolean("remove"),
            updateJson = obj.optString("updateJson"),
            hasWebUi = obj.getBooleanCompat("web"),
            hasActionScript = obj.getBooleanCompat("action"),
            metamodule = obj.getBooleanCompat("metamodule"),
            actionIconPath = obj.optString("actionIcon").takeIf { it.isNotBlank() },
            webUiIconPath = obj.optString("webuiIcon").takeIf { it.isNotBlank() }
        )
    }

    private fun String.sanitizeVersion(): String {
        return this.replace(Regex("[^a-zA-Z0-9.\\-_]"), "_")
    }

    private fun JSONObject.getBooleanCompat(key: String, default: Boolean = false): Boolean {
        if (!has(key)) return default
        return when (val value = opt(key)) {
            is Boolean -> value
            is String -> value.equals("true", ignoreCase = true) || value == "1"
            is Number -> value.toInt() != 0
            else -> default
        }
    }
}
