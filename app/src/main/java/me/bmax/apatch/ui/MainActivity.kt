package me.bmax.apatch.ui

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import me.bmax.apatch.APApplication
import me.bmax.apatch.ui.component.BottomBar
import me.bmax.apatch.ui.component.BottomBarDestination
import me.bmax.apatch.ui.navigation.LocalNavigator
import me.bmax.apatch.ui.navigation.NavGraph
import me.bmax.apatch.ui.navigation.Navigator
import me.bmax.apatch.ui.page.apm.APModuleScreen
import me.bmax.apatch.ui.page.home.HomeScreen
import me.bmax.apatch.ui.page.kpm.KPModuleScreen
import me.bmax.apatch.ui.page.settings.SettingScreen
import me.bmax.apatch.ui.page.superuser.SuperUserScreen
import me.bmax.apatch.ui.theme.APatchTheme
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

val LocalSelectedPage = compositionLocalOf { 0 }

class MainActivity : ComponentActivity() {

    private var isLoading = true
    private var navigatorInstance: Navigator? = null

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        navigatorInstance?.onNewIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { isLoading }
        super.onCreate(savedInstanceState)

        setContent {
            val navController = rememberNavController()
            val navigator = remember { Navigator(navController) }
            navigatorInstance = navigator

            LaunchedEffect(Unit) {
                navigator.onNewIntent(intent)
            }

            val context = LocalActivity.current ?: this
            val prefs = context.getSharedPreferences("config", MODE_PRIVATE)
            var colorMode by remember { mutableIntStateOf(prefs.getInt("color_mode", 0)) }
            var keyColorInt by remember { mutableIntStateOf(prefs.getInt("key_color", 0)) }
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
                    }
                }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
            }

            APatchTheme(colorMode = colorMode, keyColor = keyColor) {
                CompositionLocalProvider(LocalNavigator provides navigator) {
                    NavGraph()

                    LaunchedEffect(Unit) {
                        navigator.onNewIntent(intent)
                    }
                }
            }
        }
        isLoading = false
    }
}

@Composable
fun MainScreen() {
    val navigator = LocalNavigator.current
    val activity = LocalActivity.current as MainActivity
    val coroutineScope = rememberCoroutineScope()

    val state by APApplication.apStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)
    val kPatchReady = state != APApplication.State.UNKNOWN_STATE
    val aPatchReady = state == APApplication.State.ANDROIDPATCH_INSTALLED

    val availablePages = remember(kPatchReady, aPatchReady) {
        BottomBarDestination.entries.filter { d ->
            !(d.kPatchRequired && !kPatchReady) && !(d.aPatchRequired && !aPatchReady)
        }
    }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { availablePages.size })
    val backdrop = rememberLayerBackdrop()

    LaunchedEffect(pagerState) {
        navigator.bindPager { page ->
            coroutineScope.launch { pagerState.animateScrollToPage(page) }
        }
    }

    BackHandler {
        if (pagerState.currentPage != 0) {
            navigator.switchToTab(0)
        } else {
            activity.moveTaskToBack(true)
        }
    }

    CompositionLocalProvider(
        LocalSelectedPage provides pagerState.currentPage
    ) {
        Scaffold(
            bottomBar = { BottomBar(backdrop) },
        ) { innerPadding ->
            HorizontalPager(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop),
                state = pagerState,
                beyondViewportPageCount = 1,
                userScrollEnabled = aPatchReady,
            ) { pageIndex ->
                val bottomPadding = innerPadding.calculateBottomPadding()

                when (availablePages[pageIndex]) {
                    BottomBarDestination.Home -> HomeScreen(bottomPadding)
                    BottomBarDestination.KModule -> KPModuleScreen(bottomPadding)
                    BottomBarDestination.SuperUser -> SuperUserScreen(bottomPadding)
                    BottomBarDestination.AModule -> APModuleScreen(bottomPadding)
                    BottomBarDestination.Settings -> SettingScreen(bottomPadding)
                }
            }
        }
    }
}