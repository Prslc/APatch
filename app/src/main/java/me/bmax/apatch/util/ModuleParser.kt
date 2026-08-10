package me.bmax.apatch.util

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import me.bmax.apatch.R
import java.io.ByteArrayOutputStream
import java.util.Properties
import java.util.zip.ZipInputStream

@Suppress("ArrayInDataClass")
data class ParsedModuleInfo(
    val id: String,
    val name: String,
    val version: String,
    val versionCode: Int,
    val author: String,
    val description: String,
    val icon: ByteArray? = null
)

@Suppress("ArrayInDataClass")
data class InstallPreview(
    val id: String = "",
    val name: String = "",
    val version: String = "",
    val versionCode: Int = 0,
    val author: String = "",
    val description: String = "",
    val icon: ByteArray? = null,
    val fileName: String = "",
    val errorMessage: String? = null
)

object ModuleParser {

    class ModuleParseException(@param:StringRes val messageRes: Int, vararg val formatArgs: Any) :
        Exception() {
        fun getMessage(context: Context): String {
            return context.getString(messageRes, *formatArgs)
        }
    }

    private const val MAX_PROP_SIZE = 1 * 1024 * 1024 // 1 MiB

    fun parse(context: Context, uri: Uri): Result<ParsedModuleInfo> {
        return try {
            val properties = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val zipInputStream = ZipInputStream(inputStream)
                var props: Properties? = null
                while (true) {
                    val entry = zipInputStream.nextEntry ?: break
                    if (entry.name == "module.prop") {
                        val bytes = readEntry(zipInputStream)
                        props = Properties().apply { load(bytes.inputStream().reader(Charsets.UTF_8)) }
                        break
                    }
                    zipInputStream.closeEntry()
                }
                props
            } ?: return Result.failure(ModuleParseException(R.string.module_error_no_prop))

            val actionIconPath = properties.getProperty("actionIcon")?.trim()
            val webuiIconPath = properties.getProperty("webuiIcon")?.trim()
            val targetPaths = listOfNotNull(actionIconPath, webuiIconPath, "icon.png")

            var iconBytes: ByteArray? = null
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val zipInputStream = ZipInputStream(inputStream)
                val foundIcons = mutableMapOf<String, ByteArray>()
                while (true) {
                    val entry = zipInputStream.nextEntry ?: break
                    if (targetPaths.contains(entry.name)) {
                        foundIcons[entry.name] = readEntry(zipInputStream)
                    }
                    zipInputStream.closeEntry()
                    if (foundIcons.size == targetPaths.size) break
                }
                for (path in targetPaths) {
                    if (foundIcons.containsKey(path)) {
                        iconBytes = foundIcons[path]
                        break
                    }
                }
            }

            finalizeParsedInfo(properties, iconBytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun finalizeParsedInfo(properties: Properties, iconBytes: ByteArray?): Result<ParsedModuleInfo> {
        val id = properties.getProperty("id")?.trim()
        if (id.isNullOrEmpty()) return Result.failure(ModuleParseException(R.string.module_error_missing_id))
        if (!id.matches("^[a-zA-Z][a-zA-Z0-9._-]+$".toRegex())) {
            return Result.failure(ModuleParseException(R.string.module_error_invalid_id, id))
        }

        return Result.success(
            ParsedModuleInfo(
                id = id,
                name = properties.getProperty("name").trim(),
                version = properties.getProperty("version").trim(),
                versionCode = properties.getProperty("versionCode").trim().toInt(),
                author = properties.getProperty("author").trim(),
                description = properties.getProperty("description").trim(),
                icon = iconBytes
            )
        )
    }

    private fun readEntry(zipInputStream: ZipInputStream): ByteArray {
        val baos = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        var len: Int
        var totalRead = 0L
        while (zipInputStream.read(buffer).also { len = it } > -1) {
            totalRead += len
            if (totalRead > MAX_PROP_SIZE) {
                throw ModuleParseException(R.string.module_error_prop_too_large)
            }
            baos.write(buffer, 0, len)
        }
        return baos.toByteArray()
    }

    @SuppressLint("StringFormatInvalid")
    fun getModuleInstallPreview(context: Context, uri: Uri): InstallPreview {
        val fileName = getFileNameFromUri(context, uri) ?: uri.lastPathSegment ?: "Unknown"

        return parse(context, uri).fold(
            onSuccess = { info ->
                InstallPreview(
                    id = info.id,
                    name = info.name,
                    version = info.version,
                    versionCode = info.versionCode,
                    author = info.author,
                    description = info.description,
                    icon = info.icon,
                    fileName = fileName
                )
            },
            onFailure = { exception ->
                val reason = if (exception is ModuleParseException) {
                    exception.getMessage(context)
                } else {
                    exception.message ?: "unknown error"
                }
                InstallPreview(fileName = fileName, errorMessage = reason)
            }
        )
    }
}