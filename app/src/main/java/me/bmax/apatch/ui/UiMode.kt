package me.bmax.apatch.ui

import androidx.compose.runtime.staticCompositionLocalOf

enum class UiMode(val value: String) {
    Miuix("miuix"),
    Material("material");

    companion object {
        fun fromValue(value: String): UiMode = when (value) {
            Material.value -> Material
            else -> Miuix
        }
    }
}

val LocalUiMode = staticCompositionLocalOf { UiMode.Miuix }
