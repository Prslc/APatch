package me.bmax.apatch.data.repository

import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.Natives
import me.bmax.apatch.apApp
import me.bmax.apatch.util.getRootShell
import me.bmax.apatch.util.rootShellForResult
import java.io.File

object SettingsRepositoryImpl : SettingsRepository {
    private const val TAG = "SettingsRepo"

    private val prefs: SharedPreferences = APApplication.sharedPreferences

    override fun getBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)
    override fun getInt(key: String, default: Int): Int = prefs.getInt(key, default)
    override fun getFloat(key: String, default: Float): Float = prefs.getFloat(key, default)
    override fun setBoolean(key: String, value: Boolean) { prefs.edit().putBoolean(key, value).apply() }
    override fun setInt(key: String, value: Int) { prefs.edit().putInt(key, value).apply() }
    override fun setFloat(key: String, value: Float) { prefs.edit().putFloat(key, value).apply() }

    override fun getPageScale(): Float = prefs.getFloat("page_scale", 1.0f)
    override fun setPageScale(scale: Float) {
        prefs.edit().putFloat("page_scale", scale.coerceIn(0.8f, 1.1f)).apply()
    }

    override suspend fun isGlobalNamespaceEnabled(): Boolean = withContext(Dispatchers.IO) {
        val shell = getRootShell()
        val result = com.topjohnwu.superuser.ShellUtils.fastCmd(
            shell, "cat ${APApplication.GLOBAL_NAMESPACE_FILE}"
        )
        Log.i(TAG, "is global namespace enabled: $result")
        result == "1"
    }

    override suspend fun setGlobalNamespaceEnabled(enabled: Boolean) {
        val value = if (enabled) "1" else "0"
        getRootShell().newJob()
            .add("echo $value > ${APApplication.GLOBAL_NAMESPACE_FILE}")
            .submit { result ->
                Log.i(TAG, "setGlobalNamespaceEnabled result: ${result.isSuccess}")
            }
    }

    override suspend fun calculateCacheSize(): Long = withContext(Dispatchers.IO) {
        val context = apApp
        val cacheDir = context.cacheDir
        val patchDir = File(context.filesDir.parentFile, "patch")
        val patchFiles = setOf("kernel.ori", "new-boot.img", "boot.img", "temp.gz", "kernel")

        val cacheSize = cacheDir.listFiles()?.sumOf { file ->
            try {
                if (file.isDirectory) file.walkBottomUp().sumOf { it.length() } else file.length()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get file size", e)
                0L
            }
        } ?: 0L

        val patchSize = patchDir.listFiles()?.sumOf { file ->
            if (file.name in patchFiles) file.length() else 0L
        } ?: 0L

        cacheSize + patchSize
    }

    override suspend fun resetSuPath(newPath: String): Boolean = withContext(Dispatchers.IO) {
        val success = Natives.resetSuPath(newPath)
        if (success) {
            rootShellForResult("echo $newPath > ${APApplication.SU_PATH_FILE}")
        }
        success
    }
}
