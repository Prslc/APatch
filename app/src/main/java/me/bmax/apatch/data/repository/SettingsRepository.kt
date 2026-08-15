package me.bmax.apatch.data.repository

interface SettingsRepository {
    fun getBoolean(key: String, default: Boolean): Boolean
    fun getInt(key: String, default: Int): Int
    fun getFloat(key: String, default: Float): Float
    fun setBoolean(key: String, value: Boolean)
    fun setInt(key: String, value: Int)
    fun setFloat(key: String, value: Float)
    fun getString(key: String, default: String): String
    fun setString(key: String, value: String)
    fun getPageScale(): Float
    fun setPageScale(scale: Float)
    suspend fun isGlobalNamespaceEnabled(): Boolean
    suspend fun setGlobalNamespaceEnabled(enabled: Boolean): Boolean
    suspend fun calculateCacheSize(): Long
    suspend fun resetSuPath(newPath: String): Boolean
}
