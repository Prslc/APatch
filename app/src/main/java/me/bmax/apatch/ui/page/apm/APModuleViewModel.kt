package me.bmax.apatch.ui.page.apm

import android.util.Log
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topjohnwu.superuser.io.SuFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.R
import me.bmax.apatch.apApp
import me.bmax.apatch.util.HanziToPinyin
import me.bmax.apatch.util.listModules
import org.json.JSONArray
import org.json.JSONObject
import java.text.Collator
import java.util.Locale

class APModuleViewModel : ViewModel() {
    companion object {
        private const val TAG = "ModuleViewModel"
    }

    var uiState by mutableStateOf(APMUiState())
        private set

    val filteredModules by derivedStateOf {
        val collator = Collator.getInstance(Locale.getDefault())
        val comparator = compareByDescending<ModuleInfo> { it.metamodule && it.enabled }
            .thenBy(collator) { it.id }

        val query = uiState.search
        uiState.modules.filter {
            it.id.contains(query, true) ||
                    it.name.contains(query, true) ||
                    it.pinyinName.contains(query, true)
        }.sortedWith(comparator)
    }

    fun onSearchChange(newSearch: String) {
        uiState = uiState.copy(search = newSearch)
    }

    fun markNeedRefresh() {
        uiState = uiState.copy(isNeedRefresh = true)
    }

    private suspend fun checkMetaModuleWarning(modules: List<ModuleInfo>) = withContext(Dispatchers.IO) {
        val needsMountModule = modules.any { module ->
            val moduleDir = "/data/adb/modules/${module.id}"
            // Module requires mounting if it has a system dir and no skip_mount file
            SuFile.open("$moduleDir/system").isDirectory && !SuFile.open("$moduleDir/skip_mount").isFile
        }

        val warning = if (needsMountModule) {
            val metaDir = "/data/adb/metamodule"
            when {
                !SuFile.open("$metaDir/module.prop").isFile -> apApp.getString(R.string.no_meta_module_installed)
                SuFile.open("$metaDir/remove").isFile -> apApp.getString(R.string.meta_module_removed)
                SuFile.open("$metaDir/disable").isFile -> apApp.getString(R.string.meta_module_disabled)
                else -> null
            }
        } else null

        withContext(Dispatchers.Main) {
            uiState = uiState.copy(metaModuleWarning = warning)
        }
    }

    fun fetchModuleList() {
        viewModelScope.launch {
            uiState = uiState.copy(isRefreshing = true)
            delay(50)

            val newList = withContext(Dispatchers.IO) {
                runCatching {
                    val result = listModules()
                    val array = JSONArray(result)
                    val h2p = HanziToPinyin.getInstance()

                    (0 until array.length()).map { i ->
                        parseModuleInfo(array.getJSONObject(i), h2p)
                    }
                }.getOrElse {
                    Log.e(TAG, "fetchModuleList failed", it)
                    emptyList()
                }
            }

            uiState = uiState.copy(modules = newList, isNeedRefresh = false, isRefreshing = false)
            checkMetaModuleWarning(newList)
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

    private fun sanitizeVersionString(version: String): String {
        return version.replace(Regex("[^a-zA-Z0-9.\\-_]"), "_")
    }

    fun checkUpdate(m: ModuleInfo): Triple<String, String, String> {
        val empty = Triple("", "", "")
        if (m.updateJson.isEmpty() || m.remove || m.update || !m.enabled) {
            return empty
        }
        // download updateJson
        val result = kotlin.runCatching {
            val url = m.updateJson
            Log.i(TAG, "checkUpdate url: $url")
            val response = apApp.okhttpClient
                .newCall(
                    okhttp3.Request.Builder()
                        .url(url)
                        .build()
                ).execute()
            Log.d(TAG, "checkUpdate code: ${response.code}")
            if (response.isSuccessful) {
                response.body.string()
            } else {
                ""
            }
        }.getOrDefault("")
        Log.i(TAG, "checkUpdate result: $result")

        if (result.isEmpty()) {
            return empty
        }

        val updateJson = kotlin.runCatching {
            JSONObject(result)
        }.getOrNull() ?: return empty

        val version = sanitizeVersionString(updateJson.optString("version", ""))
        val versionCode = updateJson.optInt("versionCode", 0)
        val zipUrl = updateJson.optString("zipUrl", "")
        val changelog = updateJson.optString("changelog", "")
        if (versionCode <= m.versionCode || zipUrl.isEmpty()) {
            return empty
        }

        return Triple(zipUrl, version, changelog)
    }
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