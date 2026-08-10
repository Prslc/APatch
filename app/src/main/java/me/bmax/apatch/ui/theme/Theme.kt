package me.bmax.apatch.ui.theme

import android.content.SharedPreferences
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec
import me.bmax.apatch.ui.LocalUiMode
import me.bmax.apatch.ui.UiMode
import me.bmax.apatch.ui.webui.MonetColorsProvider.UpdateCss
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@Composable
fun APatchTheme(
    colorMode: Int = 0,
    keyColor: Color? = null,
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val controller = when (colorMode) {
        1 -> ThemeController(ColorSchemeMode.Light)
        2 -> ThemeController(ColorSchemeMode.Dark)
        3 -> ThemeController(ColorSchemeMode.MonetSystem, keyColor = keyColor, isDark = isDark)
        4 -> ThemeController(ColorSchemeMode.MonetLight, keyColor = keyColor)
        5 -> ThemeController(ColorSchemeMode.MonetDark, keyColor = keyColor)
        else -> ThemeController(ColorSchemeMode.System)
    }
    return MiuixTheme(
        controller = controller,
        content = {
            UpdateCss()
            content()
        }
    )
}

val PaletteStyle.supportsSpec2025: Boolean
    get() = this == PaletteStyle.TonalSpot ||
            this == PaletteStyle.Neutral ||
            this == PaletteStyle.Vibrant ||
            this == PaletteStyle.Expressive

fun ColorSpec.SpecVersion.effectiveFor(style: PaletteStyle): ColorSpec.SpecVersion =
    if (this == ColorSpec.SpecVersion.SPEC_2025 && !style.supportsSpec2025) {
        ColorSpec.SpecVersion.SPEC_2021
    } else {
        this
    }

@Composable
fun APatchMaterialTheme(
    colorMode: Int = 0,
    keyColor: Color? = null,
    paletteStyle: String = "TonalSpot",
    colorSpec: String = "SPEC_2025",
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val darkTheme = isInDarkTheme(colorMode)

    val resolvedStyle = remember(paletteStyle) {
        runCatching { PaletteStyle.valueOf(paletteStyle) }.getOrDefault(PaletteStyle.TonalSpot)
    }
    val resolvedSpec = remember(colorSpec, resolvedStyle) {
        runCatching { ColorSpec.SpecVersion.valueOf(colorSpec) }
            .getOrDefault(ColorSpec.SpecVersion.SPEC_2025)
            .effectiveFor(resolvedStyle)
    }

    // Material 3 is Monet-based; the fixed System/Light/Dark modes (0/1/2)
    // are only meaningful for Miuix, so always use the Monet color path
    val isMonet = true
    val colorScheme = remember(keyColor, darkTheme, colorMode, resolvedStyle, resolvedSpec) {
        when {
            isMonet && keyColor != null ->
                dynamicColorScheme(
                    seedColor = keyColor,
                    isDark = darkTheme,
                    style = resolvedStyle,
                    contrastLevel = 0.0,
                    specVersion = resolvedSpec,
                )

            isMonet && darkTheme && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                @Suppress("DEPRECATION")
                dynamicDarkColorScheme(context)

            isMonet && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                @Suppress("DEPRECATION")
                dynamicLightColorScheme(context)

            else ->
                dynamicColorScheme(
                    seedColor = Color.Unspecified,
                    isDark = darkTheme,
                    style = resolvedStyle,
                    contrastLevel = 0.0,
                    specVersion = resolvedSpec,
                )
        }
    }

    if (!view.isInEditMode) {
        LaunchedEffect(darkTheme) {
            val window = (context as? ComponentActivity)?.window ?: return@LaunchedEffect
            WindowCompat
                .getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        content = {
            UpdateCss()
            content()
        }
    )
}

data class AppThemeSettings(
    val uiMode: UiMode,
    val colorMode: Int,
    val keyColor: Color?,
    val paletteStyle: String,
    val colorSpec: String,
)

fun readAppThemeSettings(prefs: SharedPreferences): AppThemeSettings = AppThemeSettings(
    uiMode = UiMode.fromValue(prefs.getString("ui_mode", "miuix") ?: "miuix"),
    colorMode = prefs.getInt("color_mode", 0),
    keyColor = prefs.getInt("key_color", 0).takeIf { it != 0 }?.let { Color(it) },
    paletteStyle = prefs.getString("palette_style", "TonalSpot") ?: "TonalSpot",
    colorSpec = prefs.getString("color_spec", "SPEC_2025") ?: "SPEC_2025",
)

@Composable
fun APatchAppTheme(
    settings: AppThemeSettings,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalUiMode provides settings.uiMode) {
        when (settings.uiMode) {
            UiMode.Miuix -> APatchTheme(colorMode = settings.colorMode, keyColor = settings.keyColor) {
                content()
            }
            UiMode.Material -> APatchMaterialTheme(
                colorMode = settings.colorMode,
                keyColor = settings.keyColor,
                paletteStyle = settings.paletteStyle,
                colorSpec = settings.colorSpec,
            ) {
                content()
            }
        }
    }
}

@Composable
@ReadOnlyComposable
fun isInDarkTheme(themeMode: Int): Boolean {
    return when (themeMode) {
        1, 4 -> false  // Force light mode
        2, 5 -> true   // Force dark mode
        else -> isSystemInDarkTheme()  // Follow system (0 or default)
    }
}
