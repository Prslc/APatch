package me.bmax.apatch.data.repository

import me.bmax.apatch.Natives
import me.bmax.apatch.util.PkgConfig

interface SuRepository {
    fun grantSu(uid: Int, toUid: Int, scontext: String): Long
    fun revokeSu(uid: Int): Long
    fun setUidExclude(uid: Int, exclude: Int): Int
    suspend fun changeConfig(config: PkgConfig.Config)
}

object SuRepositoryImpl : SuRepository {
    override fun grantSu(uid: Int, toUid: Int, scontext: String): Long = Natives.grantSu(uid, toUid, scontext)
    override fun revokeSu(uid: Int): Long = Natives.revokeSu(uid)
    override fun setUidExclude(uid: Int, exclude: Int): Int = Natives.setUidExclude(uid, exclude)

    override suspend fun changeConfig(config: PkgConfig.Config) {
        PkgConfig.changeConfig(config)
    }
}
