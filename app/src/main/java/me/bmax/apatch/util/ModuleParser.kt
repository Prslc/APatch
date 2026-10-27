package me.bmax.apatch.util

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import me.bmax.apatch.R
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipFile
import java.io.ByteArrayOutputStream
import java.util.Properties

@Immutable
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

@Immutable
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
            withInstallArchive(context, uri) { zip ->
                val propEntry = zip.getEntry("module.prop")
                    ?: return@withInstallArchive Result.failure(
                        ModuleParseException(R.string.module_error_no_prop)
                    )

                val properties = Properties().apply {
                    load(readEntry(zip, propEntry).inputStream().reader(Charsets.UTF_8))
                }

                finalizeParsedInfo(properties, readIcon(zip, properties))
            }
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

    private fun readIcon(zip: ZipFile, properties: Properties): ByteArray? {
        val actionIconPath = properties.getProperty("actionIcon")?.trim()
        val webuiIconPath = properties.getProperty("webuiIcon")?.trim()
        for (path in listOfNotNull(actionIconPath, webuiIconPath, "icon.png")) {
            val entry = zip.getEntry(path) ?: continue
            return readEntry(zip, entry)
        }
        return null
    }

    private fun readEntry(zip: ZipFile, entry: ZipArchiveEntry): ByteArray {
        val baos = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        var totalRead = 0L
        zip.getInputStream(entry).use { input ->
            while (true) {
                val len = input.read(buffer)
                if (len < 0) break
                totalRead += len
                if (totalRead > MAX_PROP_SIZE) {
                    throw ModuleParseException(R.string.module_error_prop_too_large)
                }
                baos.write(buffer, 0, len)
            }
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
