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
}
