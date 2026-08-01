package me.bmax.apatch.ui

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import me.bmax.apatch.ui.LocalUiMode
import me.bmax.apatch.ui.UiMode
import me.bmax.apatch.ui.component.AppSnackbarHostState
import me.bmax.apatch.ui.component.BottomBar
import me.bmax.apatch.ui.navigation.LocalNavigator
import me.bmax.apatch.ui.navigation.NavGraph
import me.bmax.apatch.ui.navigation.Navigator
import me.bmax.apatch.ui.theme.APatchTheme
import me.bmax.apatch.ui.theme.APatchMaterialTheme
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SnackbarHost
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.Density


class MainActivity : ComponentActivity() {

    private var isLoading = true
    private var navigatorInstance: Navigator? = null
    private var pendingIntent: Intent? = null

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingIntent = intent
    }

    override fun onResume() {
        super.onResume()
        pendingIntent?.let {
            pendingIntent = null
            navigatorInstance?.onNewIntent(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { isLoading }
        super.onCreate(savedInstanceState)

        setContent {
            val navController = rememberNavController()
            val navigator = remember { Navigator(navController) }
            navigatorInstance = navigator

            val context = LocalActivity.current ?: this
            val prefs = context.getSharedPreferences("config", MODE_PRIVATE)
            var colorMode by remember { mutableIntStateOf(prefs.getInt("color_mode", 0)) }
            var keyColorInt by remember { mutableIntStateOf(prefs.getInt("key_color", 0)) }
            var uiMode by remember { mutableStateOf(prefs.getString("ui_mode", "miuix") ?: "miuix") }
            val keyColor =
                remember(keyColorInt) { if (keyColorInt == 0) null else Color(keyColorInt) }

            val darkMode = when (colorMode) {
                2, 5 -> true
                0, 3 -> isSystemInDarkTheme()
                else -> false
            }

            DisposableEffect(prefs, darkMode) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        Color.Transparent.value.toInt(),
                        Color.Transparent.value.toInt()
                    ) { darkMode },
                    navigationBarStyle = SystemBarStyle.auto(
                        Color.Transparent.value.toInt(),
                        Color.Transparent.value.toInt()
                    ) { darkMode },
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                }

                val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    when (key) {
                        "color_mode" -> colorMode = prefs.getInt("color_mode", 0)
                        "key_color" -> keyColorInt = prefs.getInt("key_color", 0)
                        "ui_mode" -> uiMode = prefs.getString("ui_mode", "miuix") ?: "miuix"
                    }
                }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
            }

            CompositionLocalProvider(LocalUiMode provides UiMode.fromValue(uiMode)) {
                when (UiMode.fromValue(uiMode)) {
                    UiMode.Miuix -> APatchTheme(colorMode = colorMode, keyColor = keyColor) {
                        CompositionLocalProvider(LocalNavigator provides navigator) {
                            NavGraph()
                            LaunchedEffect(Unit) { navigator.onNewIntent(intent) }
                        }
                    }
                    UiMode.Material -> APatchMaterialTheme(colorMode = colorMode, keyColor = keyColor) {
                        CompositionLocalProvider(LocalNavigator provides navigator) {
                            NavGraph()
                            LaunchedEffect(Unit) { navigator.onNewIntent(intent) }
                        }
                    }
                }
            }
        }
        isLoading = false
    }
}

