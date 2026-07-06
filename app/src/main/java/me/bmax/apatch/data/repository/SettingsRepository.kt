package me.bmax.apatch.data.repository

interface SettingsRepository {
    fun getBoolean(key: String, default: Boolean): Boolean
    fun getInt(key: String, default: Int): Int
    fun getFloat(key: String, default: Float): Float
    fun setBoolean(key: String, value: Boolean)
    fun setInt(key: String, value: Int)
    fun setFloat(key: String, value: Float)
    fun getPageScale(): Float
    fun setPageScale(scale: Float)
    suspend fun isGlobalNamespaceEnabled(): Boolean
    suspend fun setGlobalNamespaceEnabled(enabled: Boolean)
    suspend fun calculateCacheSize(): Long
    suspend fun resetSuPath(newPath: String): Boolean
}
