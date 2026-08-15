package me.bmax.apatch.util

import android.os.Parcelable
import android.util.Log
import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import com.topjohnwu.superuser.io.SuFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import me.bmax.apatch.APApplication
import me.bmax.apatch.Natives

object PkgConfig {
    private const val TAG = "PkgConfig"

    private const val CSV_HEADER = "pkg,exclude,allow,uid,to_uid,sctx"

    @Immutable
    @Parcelize
    @Keep
    data class Config(
        var pkg: String = "", var exclude: Int = 0, var allow: Int = 0, var profile: Natives.Profile
    ) : Parcelable {
        companion object {
            fun fromLine(line: String): Config? {
                val sp = line.split(',', limit = 6)
                if (sp.size < 6) return null

                val pkg = sp[0].trim()
                val exclude = sp[1].trim().toIntOrNull()
                val allow = sp[2].trim().toIntOrNull()
                val uid = sp[3].trim().toIntOrNull()
                val toUid = sp[4].trim().toIntOrNull()
                val scontext = sp[5].trim()

                if (pkg.isEmpty() || exclude == null || allow == null ||
                    uid == null || toUid == null || scontext.isEmpty()
                ) {
                    return null
                }

                return Config(pkg, exclude, allow, Natives.Profile(uid, toUid, scontext))
            }
        }

        fun isDefault(): Boolean {
            return allow == 0 && exclude == 0
        }

        fun toLine(): String {
            return "${pkg},${exclude},${allow},${profile.uid},${profile.toUid},${profile.scontext}"
        }
    }

    fun readConfigs(): HashMap<Int, Config> {
        val configs = HashMap<Int, Config>()
        val file = SuFile(APApplication.PACKAGE_CONFIG_FILE)
        file.shell = getRootShell()

        if (!file.exists()) return configs

        try {
            file.newInputStream().bufferedReader().use { reader ->
                reader.lineSequence()
                    .drop(1)
                    .filter { it.isNotBlank() }
                    .forEach { line ->
                        val config = Config.fromLine(line)
                        if (config == null) {
                            Log.w(TAG, "Skip malformed package_config line")
                            return@forEach
                        }
                        if (!config.isDefault()) {
                            configs[config.profile.uid] = config
                        }
                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read package_config", e)
        }

        return configs
    }

    private fun writeConfigs(configs: HashMap<Int, Config>) {
        val file = SuFile(APApplication.PACKAGE_CONFIG_FILE)
        file.shell = getRootShell()

        file.parent?.let { parentPath ->
            val parent = SuFile(parentPath)
            parent.shell = file.shell
            if (!parent.exists()) {
                parent.mkdirs()
            }
        }

        file.newOutputStream(false).writer().buffered().use { writer ->
            writer.write(CSV_HEADER)
            writer.newLine()
            configs.values.forEach {
                if (!it.isDefault()) {
                    writer.write(it.toLine())
                    writer.newLine()
                }
            }
        }
    }

    suspend fun changeConfig(config: Config) = withContext(Dispatchers.IO) {
        synchronized(PkgConfig.javaClass) {
            val configs = readConfigs()
            val uid = config.profile.uid
            // Root App should not be excluded
            if (config.allow == 1) {
                config.exclude = 0
            }
            if (config.allow == 0 && configs[uid] != null && config.exclude != 0) {
                configs.remove(uid)
            } else {
                Log.d(TAG, "change config: $config")
                configs[uid] = config
            }

            try {
                writeConfigs(configs)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write package_config", e)
            }
        }
    }
}
