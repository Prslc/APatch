package me.bmax.apatch.ui.page.superuser

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.graphics.drawable.Drawable
import android.os.IBinder
import android.os.Parcelable
import android.util.Log
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
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
import java.text.Collator
import java.util.Locale
import kotlin.concurrent.thread
import kotlin.coroutines.resume

class SuperUserViewModel : ViewModel() {
    companion object {
        private const val TAG = "SuperUserViewModel"
        private val appsLock = Any()
        var apps by mutableStateOf<List<AppInfo>>(emptyList())

        suspend fun fetchAppListIfEmpty(viewModel: SuperUserViewModel) {
            if (apps.isEmpty()) {
                viewModel.fetchAppList()
            }
        }
        fun getAppIconDrawable(context: Context, packageName: String): Drawable? {
            val appList = synchronized(appsLock) { apps }
            val appDetail = appList.find { it.packageName == packageName }
            return appDetail?.packageInfo?.applicationInfo?.loadIcon(context.packageManager)
        }
    }

    @Parcelize
    data class AppInfo(
        val label: String, val packageInfo: PackageInfo, val config: PkgConfig.Config
    ) : Parcelable {
        val packageName: String
            get() = packageInfo.packageName
        val uid: Int
            get() = packageInfo.applicationInfo!!.uid
    }

    var search by mutableStateOf("")
    var showSystemApps by mutableStateOf(false)
    var isRefreshing by mutableStateOf(false)
        private set

    private val sortedList by derivedStateOf {
        val comparator = compareBy<AppInfo> {
            when {
                it.config.allow != 0 -> 0
                it.config.exclude == 1 -> 1
                else -> 2
            }
        }.then(compareBy(Collator.getInstance(Locale.getDefault()), AppInfo::label))
        apps.sortedWith(comparator)
    }

    val appList by derivedStateOf {
        sortedList.filter {
            it.label.lowercase().contains(search.lowercase()) || it.packageName.lowercase()
                .contains(search.lowercase()) || HanziToPinyin.getInstance()
                .toPinyinString(it.label).contains(search.lowercase())
        }.filter {
            it.uid == 2000 // Always show shell
                    || showSystemApps || it.packageInfo.applicationInfo!!.flags.and(ApplicationInfo.FLAG_SYSTEM) == 0
        }
    }

    private suspend inline fun connectRootService(
        crossinline onDisconnect: () -> Unit = {}
    ): Pair<IBinder, ServiceConnection> = suspendCancellableCoroutine { cont ->
        val connection = object : ServiceConnection {
            override fun onServiceDisconnected(name: ComponentName?) {
                onDisconnect()
            }

            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                cont.resume(binder as IBinder to this)
            }
        }

        val intent = Intent(apApp, RootServices::class.java)
        val task = RootServices.bindOrTask(
            intent,
            Shell.EXECUTOR,
            connection,
        )
        val shell = APatchCli.SHELL
        task?.let { shell.execTask(it) }

        cont.invokeOnCancellation {
            try {
                apApp.unbindService(connection)
            } catch (e: Exception) {
                Log.w("SuperUserViewModel", "unbindService failed", e)
            }
        }
    }

    private fun stopRootService() {
        val intent = Intent(apApp, RootServices::class.java)
        RootServices.stop(intent)
    }

    suspend fun fetchAppList() {
        isRefreshing = true

        try {
            val result = connectRootService {
                Log.w(TAG, "RootService disconnected")
            }

            withContext(Dispatchers.IO) {
                val binder = result.first
                val allPackages = IAPRootService.Stub.asInterface(binder).getPackages(0)

                withContext(Dispatchers.Main) {
                    stopRootService()
                }
                val uids = Natives.suUids().toList()
                Log.d(TAG, "all allows: $uids")

                var configs: HashMap<Int, PkgConfig.Config> = HashMap()
                thread {
                    Natives.su()
                    configs = PkgConfig.readConfigs()
                }.join()

                Log.d(TAG, "all configs: $configs")

                val newApps = allPackages.list.map {
                    val appInfo = it.applicationInfo
                    val uid = appInfo!!.uid
                    val actProfile = if (uids.contains(uid)) Natives.suProfile(uid) else null
                    val config = configs.getOrDefault(
                        uid,
                        PkgConfig.Config(
                            appInfo.packageName,
                            Natives.isUidExcluded(uid),
                            0,
                            Natives.Profile(uid = uid)
                        )
                    )
                    config.allow = 0

                    // from kernel
                    if (actProfile != null) {
                        config.allow = 1
                        config.profile = actProfile
                    }
                    AppInfo(
                        label = appInfo.loadLabel(apApp.packageManager).toString(),
                        packageInfo = it,
                        config = config
                    )
                }

                withContext(Dispatchers.Main) {
                    synchronized(appsLock) {
                        apps = newApps
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch app list", e)
        } finally {
            isRefreshing = false
        }
    }
}