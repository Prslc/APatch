package me.bmax.apatch.ui.page.crash

import android.content.Context
import android.content.Intent
import android.os.Build
import me.bmax.apatch.BuildConfig
import me.bmax.apatch.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

fun Context.buildCrashLog(intent: Intent): String {
    val appName = getString(R.string.app_name)
    val versionName = BuildConfig.VERSION_NAME
    val versionCode = BuildConfig.VERSION_CODE

    val deviceBrand = Build.BRAND
    val deviceModel = Build.MODEL
    val sdkLevel = Build.VERSION.SDK_INT
    val currentDateTime = Calendar.getInstance().time
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val formattedDateTime = formatter.format(currentDateTime)

    val exceptionMessage = intent.getStringExtra("exception_message").orEmpty()
    val threadName = intent.getStringExtra("thread").orEmpty()

    return buildString {
        append(appName).append(" version: ").append(versionName).append(" ($versionCode)\n\n")
        append("Brand: ").append(deviceBrand).append("\n")
        append("Model: ").append(deviceModel).append("\n")
        append("SDK Level: ").append(sdkLevel).append("\n")
        append("Time: ").append(formattedDateTime).append("\n\n")
        append("Thread: ").append(threadName).append("\n")
        append("Crash Info: \n").append(exceptionMessage)
    }
}
