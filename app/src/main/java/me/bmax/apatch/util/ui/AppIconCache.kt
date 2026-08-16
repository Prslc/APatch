package me.bmax.apatch.util.ui

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.os.Process
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import me.zhanghai.android.appiconloader.AppIconLoader

/**
 * Remap the uid to the current user to avoid SecurityException
 * when loading icons for cross-user apps.
 * See: https://github.com/zhanghai/AppIconLoader/issues/3
 */
fun ApplicationInfo.withCurrentUserUid(): ApplicationInfo {
    val myUserId = Process.myUid() / 100000
    val appId = uid % 100000
    val targetUid = myUserId * 100000 + appId
    if (uid == targetUid) return this
    return ApplicationInfo(this).apply { uid = targetUid }
}

object AppIconCache {
    private val maxMemory = Runtime.getRuntime().maxMemory() / 1024
    private val cacheSize = (maxMemory / 8).toInt()
    private val loadSemaphore = Semaphore(2)

    private val lruCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.allocationByteCount / 1024
        }
    }

    fun getFromCache(packageName: String, uid: Int, sourceDir: String): Bitmap? {
        val key = buildKey(packageName, uid, sourceDir)
        synchronized(lruCache) { return lruCache.get(key) }
    }

    private fun buildKey(packageName: String, uid: Int, sourceDir: String): String {
        return "$packageName:$uid:$sourceDir"
    }

    suspend fun loadIcon(
        context: Context,
        packageName: String,
        uid: Int,
        sourceDir: String,
        icon: Int,
        size: Int
    ): Bitmap {
        val key = buildKey(packageName, uid, sourceDir)

        synchronized(lruCache) {
            val cached = lruCache.get(key)
            if (cached != null) return cached
        }

        return loadSemaphore.withPermit {
            synchronized(lruCache) {
                val cached = lruCache.get(key)
                if (cached != null) return@withPermit cached
            }

            withContext(Dispatchers.IO) {
                val loader = AppIconLoader(size, false, context)
                val applicationInfo = ApplicationInfo().apply {
                    this.packageName = packageName
                    this.uid = uid
                    this.sourceDir = sourceDir
                    this.publicSourceDir = sourceDir
                    this.icon = icon
                }
                val bitmap = loader.loadIcon(applicationInfo.withCurrentUserUid())

                val gpuBitmap = try {
                    bitmap.copy(Bitmap.Config.HARDWARE, false)?.also {
                        bitmap.recycle()
                    } ?: bitmap.also { it.prepareToDraw() }
                } catch (_: Exception) {
                    bitmap.also { it.prepareToDraw() }
                }

                synchronized(lruCache) {
                    lruCache.put(key, gpuBitmap)
                }

                gpuBitmap
            }
        }
    }
}
