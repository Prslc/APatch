package me.bmax.apatch.ui

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold as Material3Scaffold
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlin.math.abs
import me.bmax.apatch.ui.component.snackbar.AppSnackbarHostState
import me.bmax.apatch.ui.component.bottombar.BottomBar
import me.bmax.apatch.ui.component.bottombar.BottomBarDestination
import me.bmax.apatch.ui.component.bottombar.ModuleCounts
import me.bmax.apatch.ui.component.bottombar.rememberAvailablePages
import me.bmax.apatch.ui.component.snackbar.SwipeableSnackbarHostMaterial
import me.bmax.apatch.ui.component.snackbar.rememberAppSnackbarHostState
import me.bmax.apatch.ui.navigation.LocalNavigator
import me.bmax.apatch.ui.page.apm.APModuleScreen
import me.bmax.apatch.ui.page.home.HomeScreen
import me.bmax.apatch.ui.page.kpm.KPModuleScreen
import me.bmax.apatch.ui.page.settings.SettingScreen
import me.bmax.apatch.ui.page.superuser.SuperUserScreen
import me.bmax.apatch.ui.theme.LocalEnableBlur
import me.bmax.apatch.ui.theme.blurEnabled
import me.bmax.apatch.ui.theme.rememberBlurBackdrop
import me.bmax.apatch.ui.theme.withBackdrop
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SnackbarHost

val LocalSelectedPage = compositionLocalOf { 0 }
val LocalSnackbarHost = compositionLocalOf { AppSnackbarHostState() }
val LocalModuleCounts = compositionLocalOf { ModuleCounts() }

@Composable
fun MainScreen() {
    val navigator = LocalNavigator.current
    val activity = LocalActivity.current as MainActivity
    val coroutineScope = rememberCoroutineScope()
    val uiMode = LocalUiMode.current

    val availablePages = rememberAvailablePages()

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { availablePages.size })

    val mainPagerState = remember {
        MainPagerState(
            pagerState = pagerState,
            coroutineScope = coroutineScope,
        )
    }

    val backdrop = rememberBlurBackdrop(blurEnabled)
    val snackBarHostState = rememberAppSnackbarHostState()
    val moduleCounts = remember { ModuleCounts() }
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

    val previousPages = remember { arrayOf(availablePages) }
    LaunchedEffect(availablePages) {
        val prev = previousPages[0]
        if (prev != availablePages) {
            val currentPage = mainPagerState.pagerState.currentPage
            if (currentPage < prev.size) {
                val destination = prev[currentPage]
                val newIndex = availablePages.indexOf(destination)
                if (newIndex != currentPage) {
                    mainPagerState.pagerState.scrollToPage(if (newIndex != -1) newIndex else 0)
                }
            }
            previousPages[0] = availablePages
        }
    }

    BackHandler {
        if (mainPagerState.selectedPage != 0) {
            mainPagerState.animateToPage(0)
        } else {
            activity.finish()
        }
    }

    CompositionLocalProvider(
        LocalSnackbarHost provides snackBarHostState,
        LocalModuleCounts provides moduleCounts,
        LocalSelectedPage provides mainPagerState.selectedPage,
        LocalEnableBlur provides blurEnabled
    ) {
        val pagerContent = @Composable { innerPadding: PaddingValues ->
            HorizontalPager(
                modifier = Modifier
                    .fillMaxSize()
                    .withBackdrop(backdrop),
                state = mainPagerState.pagerState,
                beyondViewportPageCount = if (contentReady) 1 else 0,
                userScrollEnabled = availablePages.contains(BottomBarDestination.AModule),
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

        when (uiMode) {
            UiMode.Miuix -> Scaffold(
                bottomBar = { BottomBar(backdrop) },
                snackbarHost = { SnackbarHost(state = snackBarHostState.miuixState) },
                content = pagerContent,
            )

            UiMode.Material -> Material3Scaffold(
                bottomBar = { BottomBar(backdrop) },
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                snackbarHost = {
                    SwipeableSnackbarHostMaterial(hostState = snackBarHostState.materialState)
                },
                content = pagerContent,
            )
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
