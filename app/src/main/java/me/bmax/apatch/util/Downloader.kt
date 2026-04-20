package me.bmax.apatch.util

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import io.ktor.client.call.body
import io.ktor.client.request.get
import me.bmax.apatch.apApp

private const val TAG = "Downloader"

@SuppressLint("Range")
fun download(
    context: Context,
    url: String,
    fileName: String,
    description: String,
    onDownloaded: (Uri) -> Unit = {},
    onDownloading: () -> Unit = {}
) {
    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val query = DownloadManager.Query()

    query.setFilterByStatus(DownloadManager.STATUS_RUNNING or DownloadManager.STATUS_PAUSED or DownloadManager.STATUS_PENDING)
    downloadManager.query(query)?.use { cursor ->
        while (cursor.moveToNext()) {
            val uri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_URI))
            val localUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
            val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
            val columnTitle = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_TITLE))
            if (url == uri || fileName == columnTitle) {
                if (status == DownloadManager.STATUS_RUNNING || status == DownloadManager.STATUS_PENDING) {
                    onDownloading()
                    return
                } else if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    onDownloaded(localUri.toUri())
                    return
                }
            }
        }
    }

    val request = DownloadManager.Request(url.toUri()).setDestinationInExternalPublicDir(
        Environment.DIRECTORY_DOWNLOADS, fileName
    ).setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setMimeType("application/zip").setTitle(fileName).setDescription(description)

    downloadManager.enqueue(request)
}

suspend fun checkNewVersion(): LatestVersionInfo {
    val url = "https://api.github.com/repos/bmax121/APatch/releases/latest"

    return try {
        val release = apApp.httpClient.get(url).body<GithubReleaseDto>()

        val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }

        LatestVersionInfo(
            versionCode = release.name.toIntOrNull() ?: 0,
            downloadUrl = apkAsset?.downloadUrl ?: "",
            changelog = release.body
        )
    } catch (e: Exception) {
        Log.e(TAG, "checkNewVersion failed: ${e.message}", e)
        LatestVersionInfo()
    }
}

@Composable
fun DownloadListener(context: Context, onDownloaded: (Uri) -> Unit) {
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            @SuppressLint("Range")
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                    val id = intent.getLongExtra(
                        DownloadManager.EXTRA_DOWNLOAD_ID, -1
                    )
                    val downloadManager = context?.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                    val query = DownloadManager.Query().setFilterById(id)

                    downloadManager?.query(query)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                val uriString = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
                                uriString?.toUri()?.let { onDownloaded(it) }
                            }
                        }
                    }
                }
            }
        }
        val intentFilter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        ContextCompat.registerReceiver(
            context,
            receiver,
            intentFilter,
            ContextCompat.RECEIVER_EXPORTED
        )
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }
}
