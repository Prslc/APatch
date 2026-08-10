package me.bmax.apatch.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

private const val TAG = "CacheUtil"

private val PATCH_CACHE_FILES = setOf(
    "kernel.ori", "new-boot.img", "boot.img", "temp.gz", "kernel"
)

suspend fun calculateCacheSize(context: Context): Long = withContext(Dispatchers.IO) {
    val cacheDir = context.cacheDir
    val patchDir = File(context.filesDir.parentFile, "patch")

    val cacheSize = cacheDir.listFiles()?.sumOf { file ->
        try {
            if (file.isDirectory) file.walkBottomUp().sumOf { it.length() } else file.length()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get file size", e)
            0L
        }
    } ?: 0L

    val patchSize = patchDir.listFiles()?.sumOf { file ->
        if (file.name in PATCH_CACHE_FILES) file.length() else 0L
    } ?: 0L

    cacheSize + patchSize
}

fun formatSize(size: Long): String {
    val kb = 1024.0
    val mb = kb * 1024
    return when {
        size >= mb -> String.format(Locale.US, "%.1f MB", size / mb)
        size >= kb -> String.format(Locale.US, "%.1f KB", size / kb)
        else -> "$size B"
    }
}

// patch cache
suspend fun clearPatchCacheSafe(context: Context) = withContext(Dispatchers.IO) {
    val patchDir = File(context.filesDir.parentFile, "patch")
    if (!patchDir.exists()) return@withContext

    patchDir.listFiles()?.forEach { file ->
        if (file.name in PATCH_CACHE_FILES) {
            try {
                val deleted = file.delete()
                Log.i(TAG, "Deleted ${file.absolutePath} -> $deleted")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete ${file.absolutePath}", e)
            }
        }
    }
}

// log cache
suspend fun clearCache(context: Context) = withContext(Dispatchers.IO) {
    val cacheDir = context.cacheDir
    if (!cacheDir.exists()) return@withContext

    cacheDir.listFiles()?.forEach { file ->
        try {
            val deleted = file.deleteRecursively()
            Log.i(TAG, "Deleted cache file: ${file.absolutePath} -> $deleted")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete cache file: ${file.absolutePath}", e)
        }
    }

    Log.i(TAG, "App cache cleared")
}

suspend fun clearAppCache(context: Context): Long = withContext(Dispatchers.IO) {
    val sizeBefore = calculateCacheSize(context)
    clearPatchCacheSafe(context)
    clearCache(context)
    val sizeAfter = calculateCacheSize(context)
    val freed = sizeBefore - sizeAfter
    Log.i(TAG, "App cache cleared, freed ${formatSize(freed)}")
    freed
}