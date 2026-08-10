package me.bmax.apatch.data.repository

import android.net.Uri
import me.bmax.apatch.ui.navigation.MODULE_TYPE
import me.bmax.apatch.ui.page.apm.ModuleInfo

interface ApModuleRepository {
    suspend fun listModules(): Result<List<ModuleInfo>>

    suspend fun getModuleCount(): Int

    suspend fun checkModuleUpdate(module: ModuleInfo): Triple<String, String, String>

    suspend fun checkMetaModuleWarning(modules: List<ModuleInfo>): String?

    suspend fun toggleModule(id: String, enable: Boolean): Boolean

    suspend fun uninstallModule(id: String): Boolean

    suspend fun undoUninstallModule(id: String): Boolean

    suspend fun installModule(
        uri: Uri,
        type: MODULE_TYPE,
        onStdout: (String) -> Unit = {},
        onStderr: (String) -> Unit = {}
    ): Boolean

    suspend fun runAction(
        moduleId: String,
        onStdout: (String) -> Unit = {},
        onStderr: (String) -> Unit = {}
    ): Boolean
}
