package me.bmax.apatch.ui

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import me.bmax.apatch.ui.page.crash.CrashScreenMaterial
import me.bmax.apatch.ui.page.crash.CrashScreenMiuix
import me.bmax.apatch.ui.page.crash.buildCrashLog
import me.bmax.apatch.ui.theme.APatchMaterialTheme
import me.bmax.apatch.ui.theme.APatchTheme

class CrashHandleActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        super.onCreate(savedInstanceState)

        val message = buildCrashLog(intent)

        setContent {
            val prefs = getSharedPreferences("config", MODE_PRIVATE)
            val colorMode = prefs.getInt("color_mode", 0)
            val keyColorInt = prefs.getInt("key_color", 0)
            val keyColor = if (keyColorInt == 0) null else Color(keyColorInt)
            val uiMode = UiMode.fromValue(prefs.getString("ui_mode", "miuix") ?: "miuix")

            when (uiMode) {
                UiMode.Miuix -> APatchTheme(colorMode = colorMode, keyColor = keyColor) {
                    CompositionLocalProvider(LocalUiMode provides uiMode) {
                        CrashScreenMiuix(message)
                    }
                }
                UiMode.Material -> APatchMaterialTheme(colorMode = colorMode, keyColor = keyColor) {
                    CompositionLocalProvider(LocalUiMode provides uiMode) {
                        CrashScreenMaterial(message)
                    }
                }
            }
        }
    }
}
