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
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
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
import me.bmax.apatch.ui.theme.rememberBlurBackdrop
import me.bmax.apatch.ui.theme.withBackdrop
import top.yukonga.miuix.kmp.basic.Scaffold
import kotlin.math.abs

val LocalSelectedPage = compositionLocalOf { 0 }

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

    val mainPagerState = remember {
        MainPagerState(
            pagerState = pagerState,
            coroutineScope = coroutineScope,
        )
    }

    val backdrop = rememberBlurBackdrop()
    val contentReady = rememberContentReady()
    val settledPage by remember { derivedStateOf { mainPagerState.pagerState.settledPage } }

    LaunchedEffect(mainPagerState.pagerState) {
        navigator.bindPager { page ->
            mainPagerState.animateToPage(page)
        }
    }

    LaunchedEffect(mainPagerState) {
        snapshotFlow { mainPagerState.pagerState.currentPage }
            .collect { mainPagerState.syncPage() }
    }

    BackHandler {
        if (mainPagerState.selectedPage != 0) {
            mainPagerState.animateToPage(0)
        } else {
            activity.moveTaskToBack(true)
        }
    }

    CompositionLocalProvider(
        LocalSelectedPage provides mainPagerState.selectedPage
    ) {
        Scaffold(
            bottomBar = { BottomBar(backdrop) },
        ) { innerPadding ->
            HorizontalPager(
                modifier = Modifier
                    .fillMaxSize()
                    .withBackdrop(backdrop),
                state = mainPagerState.pagerState,
                beyondViewportPageCount = if (contentReady) availablePages.size - 1 else 0,
                userScrollEnabled = aPatchReady,
            ) { pageIndex ->
                val pageModifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Auto
                    }

                val bottomPadding = innerPadding.calculateBottomPadding()
                val isCurrentPage = pageIndex == settledPage

                if (isCurrentPage || contentReady) {
                    when (availablePages[pageIndex]) {
                        BottomBarDestination.Home -> HomeScreen(
                            modifier = pageModifier,
                            bottomPadding =  bottomPadding,
                            isCurrentPage = isCurrentPage
                        )

                        BottomBarDestination.KModule -> KPModuleScreen(
                            modifier = pageModifier,
                            bottomPadding = bottomPadding,
                            isCurrentPage = isCurrentPage
                        )

                        BottomBarDestination.SuperUser -> SuperUserScreen(
                            modifier = pageModifier,
                            bottomPadding = bottomPadding,
                            isCurrentPage = isCurrentPage
                        )

                        BottomBarDestination.AModule -> APModuleScreen(
                            modifier = pageModifier,
                            bottomPadding = bottomPadding,
                            isCurrentPage = isCurrentPage
                        )

                        BottomBarDestination.Settings -> SettingScreen(
                            modifier = pageModifier,
                            bottomPadding = bottomPadding,
                            isCurrentPage = isCurrentPage
                        )
                    }
                }
            }
        }
    }
}

// https://github.com/compose-miuix-ui/miuix/blob/main/example/shared/src/commonMain/kotlin/AppContent.kt
@Stable
class MainPagerState(
    val pagerState: PagerState,
    private val coroutineScope: CoroutineScope,
) {
    var selectedPage by mutableIntStateOf(pagerState.currentPage)
        private set

    var isNavigating by mutableStateOf(false)
        private set

    private var navJob: Job? = null

    fun animateToPage(targetIndex: Int) {
        if (targetIndex == selectedPage) return

        navJob?.cancel()

        selectedPage = targetIndex
        isNavigating = true

        navJob = coroutineScope.launch {
            val myJob = coroutineContext.job
            try {
                pagerState.scroll(MutatePriority.UserInput) {
                    val distance = abs(targetIndex - pagerState.currentPage).coerceAtLeast(2)
                    val duration = 100 * distance + 100
                    val layoutInfo = pagerState.layoutInfo
                    val pageSize = layoutInfo.pageSize + layoutInfo.pageSpacing
                    val currentDistanceInPages =
                        targetIndex - pagerState.currentPage - pagerState.currentPageOffsetFraction
                    val scrollPixels = currentDistanceInPages * pageSize

                    var previousValue = 0f
                    animate(
                        initialValue = 0f,
                        targetValue = scrollPixels,
                        animationSpec = tween(easing = EaseInOut, durationMillis = duration),
                    ) { currentValue, _ ->
                        previousValue += scrollBy(currentValue - previousValue)
                    }
                }

                if (pagerState.currentPage != targetIndex) {
                    pagerState.scrollToPage(targetIndex)
                }
            } finally {
                if (navJob == myJob) {
                    isNavigating = false
                    if (pagerState.currentPage != targetIndex) {
                        selectedPage = pagerState.currentPage
                    }
                }
            }
        }
    }

    fun syncPage() {
        if (!isNavigating && selectedPage != pagerState.currentPage) {
            selectedPage = pagerState.currentPage
        }
    }
}

@Composable
private fun rememberContentReady(): Boolean {
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        ready = true
    }
    return ready
}