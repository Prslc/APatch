package me.bmax.apatch.ui.page.patch.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileNotFoundException

val checkSuperKeyValidation: (superKey: String) -> Boolean = { superKey ->
    superKey.length in 8..63 && superKey.any { it.isDigit() } && superKey.any { it.isLetter() }
}

fun isSuExecutable(): Boolean {
    val suFile = File("/system/bin/su")
    return suFile.exists() && suFile.canExecute()
}

fun File.isSymbolicLink(): Boolean {
    return try {
        val canonicalFile = if (parent == null) this else File(parentFile!!.canonicalFile, name)
        !canonicalFile.canonicalFile.equals(canonicalFile.absoluteFile)
    } catch (e: Exception) { false }
}

@RequiresApi(Build.VERSION_CODES.Q)
fun createDownloadUri(context: Context, outFilename: String): Uri? {
    val contentValues = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, outFilename)
        put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
        put(MediaStore.Downloads.IS_PENDING, 1)
    }
    val resolver = context.contentResolver
    return resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
}

@RequiresApi(Build.VERSION_CODES.Q)
fun insertDownload(context: Context, outUri: Uri?, inputUri: Uri): Boolean {
    if (outUri == null) return false
    return try {
        val resolver = context.contentResolver
        resolver.openInputStream(inputUri)?.use { inputStream ->
            resolver.openOutputStream(outUri)?.use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.IS_PENDING, 0)
        }
        resolver.update(outUri, contentValues, null, null)
        true
    } catch (_: FileNotFoundException) {
        false
    }
}

fun File.getUri(context: Context): Uri {
    val authority = "${context.packageName}.fileprovider"
    return FileProvider.getUriForFile(context, authority, this)
}