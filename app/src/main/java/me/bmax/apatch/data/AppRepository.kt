package me.bmax.apatch.data

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Parcelable
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.core.content.pm.PackageInfoCompat
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import me.bmax.apatch.IAPRootService
import me.bmax.apatch.Natives
import me.bmax.apatch.apApp
import me.bmax.apatch.services.RootServices
import me.bmax.apatch.util.APatchCli
import me.bmax.apatch.util.HanziToPinyin
import me.bmax.apatch.util.PkgConfig
import java.util.Locale
import kotlin.coroutines.resume

private const val TAG = "AppRepository"

@Immutable
@Parcelize
data class AppInfo(
    val label: String,
    val lowercaseLabel: String,
    val pinyinLabel: String,
    val packageName: String,
    val isSystem: Boolean,
    val uid: Int,
    val firstInstallTime: Long,
    val versionName: String,
    val versionCode: Long,
    val packageInfo: PackageInfo,
    val config: PkgConfig.Config
) : Parcelable

object AppRepository {
    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps = _apps.asStateFlow()

    val ApplicationInfo.isActuallyInstalled: Boolean
        get() = sourceDir != null && java.io.File(sourceDir).exists()

    suspend fun fetchAppList() {
        val result = withContext(Dispatchers.Main) {
            connectRootService()
        } ?: return

        val (binder, connection) = result

        try {
            withContext(Dispatchers.IO) {
                val service = IAPRootService.Stub.asInterface(binder)
                val allPackages = service.getPackages(0)
                val pm = apApp.packageManager
                val h2p = HanziToPinyin.getInstance()

                val uids = Natives.suUids().toSet()
                val configs = PkgConfig.readConfigs()

                val newApps = allPackages.list.map { pkg ->
                    async {
                        val appInfo = pkg.applicationInfo ?: return@async null
                        if (!appInfo.isActuallyInstalled) return@async null

                        val uid = appInfo.uid

                        val actProfile = if (uids.contains(uid)) Natives.suProfile(uid) else null

                        val config = (configs[uid] ?: PkgConfig.Config(
                            pkg.packageName,
                            Natives.isUidExcluded(uid),
                            0,
                            Natives.Profile(uid = uid)
                        )).apply {
                            allow = if (actProfile != null) 1 else 0
                            if (actProfile != null) profile = actProfile
                        }

                        val label = appInfo.loadLabel(pm).toString()
                        val pinyin = h2p.toPinyinString(label).lowercase(Locale.getDefault())
                        val lowerLabel = label.lowercase(Locale.getDefault())
                        AppInfo(
                            label = label,
                            lowercaseLabel = lowerLabel,
                            pinyinLabel = pinyin,
                            packageName = pkg.packageName,
                            isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                            uid = uid,
                            firstInstallTime = pkg.firstInstallTime,
                            versionName = pkg.versionName ?: "",
                            versionCode = PackageInfoCompat.getLongVersionCode(pkg),
                            packageInfo = pkg,
                            config = config
                        )
                    }
                }.awaitAll().filterNotNull()
                _apps.value = newApps
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fetch failed", e)
        } finally {
            withContext(Dispatchers.Main) {
                try {
                    apApp.unbindService(connection)
                } catch (_: Exception) {
                }
            }
        }
    }

    private suspend fun connectRootService(): Pair<IBinder, ServiceConnection>? =
        suspendCancellableCoroutine { cont ->
            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                    if (binder != null && cont.isActive) cont.resume(binder to this)
                }

                override fun onServiceDisconnected(name: ComponentName?) {}
            }

            val intent = Intent(apApp, RootServices::class.java)
            val task = RootServices.bindOrTask(intent, Shell.EXECUTOR, connection)

            if (task != null) {
                Shell.EXECUTOR.execute {
                    try {
                        APatchCli.SHELL.execTask(task)
                    } catch (e: Exception) {
                        Log.e(TAG, "execTask failed", e)
                        if (cont.isActive) cont.resume(null)
                    }
                }
            } else {
                Handler(Looper.getMainLooper()).post {
                    if (cont.isActive) cont.resume(null)
                }
            }

            cont.invokeOnCancellation {
                try {
                    apApp.unbindService(connection)
                } catch (_: Exception) {
                }
            }
        }

    fun updateLocalConfig(uid: Int, newConfig: PkgConfig.Config) {
        _apps.update { currentList ->
            currentList.map { app ->
                if (app.uid == uid) {
                    app.copy(config = newConfig)
                } else {
                    app
                }
            }
        }
    }

    suspend fun stopRootService() {
        withContext(Dispatchers.Main) {
            try {
                val intent = Intent(apApp, RootServices::class.java)
                RootServices.stop(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Stop RootService failed", e)
            }
        }
    }
}
