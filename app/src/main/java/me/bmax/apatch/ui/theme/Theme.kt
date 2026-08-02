package me.bmax.apatch.ui.theme

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
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

@Composable
fun APatchMaterialTheme(
    colorMode: Int = 0,
    keyColor: Color? = null,
    paletteStyle: String = "TonalSpot",
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val darkTheme = isInDarkTheme(colorMode)

    val resolvedStyle = remember(paletteStyle) {
        runCatching { PaletteStyle.valueOf(paletteStyle) }.getOrDefault(PaletteStyle.TonalSpot)
    }

    val isMonet = colorMode in 3..5
    val colorScheme = remember(keyColor, darkTheme, colorMode, resolvedStyle) {
        when {
            isMonet && keyColor != null ->
                dynamicColorScheme(
                    seedColor = keyColor,
                    isDark = darkTheme,
                    style = resolvedStyle,
                    contrastLevel = 0.0,
                    specVersion = ColorSpec.SpecVersion.SPEC_2025,
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
                    specVersion = ColorSpec.SpecVersion.SPEC_2025,
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

@Composable
@ReadOnlyComposable
fun isInDarkTheme(themeMode: Int): Boolean {
    return when (themeMode) {
        1, 4 -> false  // Force light mode
        2, 5 -> true   // Force dark mode
        else -> isSystemInDarkTheme()  // Follow system (0 or default)
    }
}
