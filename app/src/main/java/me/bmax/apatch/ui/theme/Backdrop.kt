// This file includes code derived from https://github.com/wxxsfxyzm/InstallerX-Revived
// Copyright (C) 2026 InstallerX Revived contributors
// Modified: Adapted for Miuix surface logic and implemented safe backdrop capturing wrappers.

package me.bmax.apatch.ui.theme

import androidx.compose.runtime.Composable
import me.bmax.apatch.APApplication
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.isRenderEffectSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Remember a LayerBackdrop with a solid background to prevent alpha-blending artifacts.
 * * @param enableBlur Whether the blur effect is globally enabled.
 * @return A LayerBackdrop instance if supported and enabled, null otherwise.
 */
@Composable
fun rememberBlurBackdrop(enableBlur: Boolean): LayerBackdrop? {
    if (!enableBlur || !isRenderEffectSupported()) return null
    val surfaceColor = MiuixTheme.colorScheme.surface
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
}

/**
 * Remember a LayerBackdrop controlled by the user's blur preference.
 * @return A LayerBackdrop instance if enabled and supported, null otherwise.
 */
@Composable
fun rememberBlurBackdrop(): LayerBackdrop? {
    val prefs = APApplication.sharedPreferences
    return rememberBlurBackdrop(prefs.getBoolean("blur_enabled", true))
}

/**
 * Determine the app bar background color based on the Backdrop availability.
 * @return Transparent if Backdrop is active, otherwise the default surface color.
 */
@Composable
fun LayerBackdrop?.getAppBarColor(): Color =
    this?.let { Color.Transparent } ?: MiuixTheme.colorScheme.surface

/**
 * Captures the content of this composable into the given [LayerBackdrop].
 *
 * If [backdrop] is null (e.g., when blur is disabled or not supported),
 * this modifier does nothing and returns the original [Modifier].
 *
 * @param backdrop The [LayerBackdrop] used to record the content.
 */
fun Modifier.withBackdrop(backdrop: LayerBackdrop?): Modifier =
    backdrop?.let { this.layerBackdrop(it) } ?: this

/**
 * Apply a standard glassmorphism blur effect using Miuix Backdrop.
 * @param backdrop The LayerBackdrop providing the visual source.
 * @param enabled Whether the effect is locally enabled for this component.
 * @param blurRadius The radius of the Gaussian blur.
 * @param shape The clipping shape for the blurred area.
 */
@Composable
fun Modifier.blurEffect(
    backdrop: LayerBackdrop?,
    enabled: Boolean = true,
    blurRadius: Float = 25f,
    shape: Shape = RectangleShape
): Modifier {
    // Return early if disabled or backdrop is unavailable
    if (!enabled || backdrop == null) return this

    // Grab the current theme surface color for blending
    val blendColor = MiuixTheme.colorScheme.surface.copy(alpha = 0.8f)

    return this.then(
        Modifier.textureBlur(
            backdrop = backdrop,
            shape = shape,
            blurRadius = blurRadius,
            colors = BlurColors(
                blendColors = listOf(
                    BlendColorEntry(color = blendColor)
                )
            )
        )
    )
}