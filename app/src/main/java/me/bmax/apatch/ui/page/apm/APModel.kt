package me.bmax.apatch.ui.page.apm

import androidx.compose.runtime.Immutable

@Immutable
data class ModuleInfo(
    val id: String,
    val name: String,
    val pinyinName: String,
    val author: String,
    val version: String,
    val versionCode: Int,
    val description: String,
    val enabled: Boolean,
    val update: Boolean,
    val remove: Boolean,
    val updateJson: String,
    val hasWebUi: Boolean,
    val hasActionScript: Boolean,
    val metamodule: Boolean,
    val actionIconPath: String?,
    val webUiIconPath: String?,
)