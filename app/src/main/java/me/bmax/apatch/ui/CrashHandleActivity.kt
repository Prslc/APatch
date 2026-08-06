package me.bmax.apatch.ui

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import me.bmax.apatch.ui.page.crash.CrashScreen
import me.bmax.apatch.ui.page.crash.buildCrashLog
import me.bmax.apatch.ui.theme.APatchAppTheme
import me.bmax.apatch.ui.theme.readAppThemeSettings

class CrashHandleActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        super.onCreate(savedInstanceState)

        val message = buildCrashLog(intent)

        setContent {
            val settings = remember {
                readAppThemeSettings(getSharedPreferences("config", MODE_PRIVATE))
            }
            APatchAppTheme(settings) {
                CrashScreen(message)
            }
        }
    }
}
