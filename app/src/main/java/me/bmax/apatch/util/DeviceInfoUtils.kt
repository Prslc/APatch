package me.bmax.apatch.util

import android.os.Build
import android.util.Log

fun getSELinuxStatus(): String {
    val shell = getRootShell()
    val list = ArrayList<String>()
    val result = shell.newJob().add("getenforce").to(list, list).exec()
    val output = result.out.joinToString("\n").trim()

    return if (result.isSuccess) output
    else if (output.endsWith("Permission denied")) "Enforcing"
    else "Unknown"
}

private fun getSystemProperty(key: String): Boolean {
    try {
        val c = Class.forName("android.os.SystemProperties")
        val get = c.getMethod(
            "getBoolean",
            String::class.java,
            Boolean::class.javaPrimitiveType
        )
        return get.invoke(c, key, false) as Boolean
    } catch (e: Exception) {
        Log.e("APatch", "[DeviceUtils] Failed to get system property: ", e)
    }
    return false
}

// Check to see if device supports A/B (seamless) system updates
fun isABDevice(): Boolean {
    return getSystemProperty("ro.build.ab_update")
}

fun getSystemVersion(): String {
    return "${Build.VERSION.RELEASE} ${if (Build.VERSION.PREVIEW_SDK_INT != 0) "Preview" else ""} (API ${Build.VERSION.SDK_INT})"
}

fun getDeviceInfo(): String {
    var manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
    if (!Build.BRAND.equals(Build.MANUFACTURER, ignoreCase = true)) {
        manufacturer += " " + Build.BRAND.replaceFirstChar { it.uppercase() }
    }
    return "$manufacturer ${Build.MODEL} "
}
