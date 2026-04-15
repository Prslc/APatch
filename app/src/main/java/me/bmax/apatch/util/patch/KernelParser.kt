package me.bmax.apatch.util.patch

import me.bmax.apatch.ui.viewmodel.KPModel
import org.ini4j.Ini
import java.io.StringReader

object KernelParser {
    fun parseKpimg(output: List<String>, defaultKey: String = ""): KPModel.KPImgInfo? {
        val ini = Ini(StringReader(output.joinToString("\n")))
        val section = ini["kpimg"] ?: return null
        return KPModel.KPImgInfo(
            section["version"].toString(),
            section["compile_time"].toString(),
            section["config"].toString(),
            defaultKey, // default key
            section["root_superkey"].toString()
        )
    }

    fun parseBootInfo(output: List<String>): Pair<KPModel.KImgInfo, List<KPModel.IExtraInfo>> {
        val ini = Ini(StringReader(output.joinToString("\n")))
        val kernel = ini["kernel"] ?: return KPModel.KImgInfo("", false) to emptyList()

        val info = KPModel.KImgInfo(kernel["banner"].toString(), kernel["patched"].toBoolean())
        val extras = mutableListOf<KPModel.IExtraInfo>()

        if (info.patched) {
            val kpmNum = kernel["extra_num"]?.toInt() ?: ini["extras"]?.get("num")?.toInt() ?: 0
            for (i in 0 until kpmNum) {
                ini["extra $i"]?.let { extra ->
                    val type = runCatching {
                        KPModel.ExtraType.valueOf(extra["type"]?.uppercase() ?: "")
                    }.getOrNull() ?: continue
                    if (type == KPModel.ExtraType.KPM) {
                        extras.add(KPModel.KPMInfo(
                            type, extra["name"].toString(),
                            extra["event"].takeIf { it!!.isNotEmpty() } ?: KPModel.TriggerEvent.PRE_KERNEL_INIT.event,
                            extra["args"].toString(), extra["version"].toString(),
                            extra["license"].toString(), extra["author"].toString(),
                            extra["description"].toString()
                        ))
                    }
                }
            }
        }
        return info to extras
    }
}