package me.bmax.apatch.data.repository

import android.net.Uri
import me.bmax.apatch.Natives
import me.bmax.apatch.ui.page.kpm.KPModel

interface KPModuleRepository {
    suspend fun listModules(): List<KPModel.KPMInfo>

    fun getModuleCount(): Int

    suspend fun loadModule(uri: Uri, args: String = ""): Int

    fun unloadModule(name: String): Boolean

    fun controlModule(name: String, param: String): Natives.KPMCtlRes

    /**
     * Persists a KPM from [uri] into the file-backed store. It is loaded by the
     * boot-time loader, so the caller has to prompt for a reboot afterwards.
     * Returns 0 on success and a negative code on failure.
     */
    suspend fun installModule(uri: Uri): Int

    /** Removes an installed KPM's persisted directory. */
    suspend fun removeInstalledModule(id: String): Boolean

    /** Enables or disables an installed KPM for the next boot. */
    suspend fun setModuleDisabled(id: String, disabled: Boolean): Boolean
}
