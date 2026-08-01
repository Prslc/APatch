package me.bmax.apatch.ui.component.miuix

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import me.bmax.apatch.APApplication
import me.bmax.apatch.ui.LocalSelectedPage
import me.bmax.apatch.ui.component.BottomBarDestination
import me.bmax.apatch.ui.navigation.LocalNavigator
import me.bmax.apatch.ui.theme.blurEffect
import me.bmax.apatch.ui.theme.getAppBarColor
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.blur.LayerBackdrop

@Composable
fun BottomBarMiuix(backdrop: LayerBackdrop?) {
    val navigator = LocalNavigator.current
    val selectedPage = LocalSelectedPage.current

    val apState by APApplication.apStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)
    val kPatchReady = apState != APApplication.State.UNKNOWN_STATE
    val aPatchReady = apState == APApplication.State.ANDROIDPATCH_INSTALLED

    val availablePages = remember(kPatchReady, aPatchReady) {
        BottomBarDestination.entries.filter { d ->
            !(d.kPatchRequired && !kPatchReady) && !(d.aPatchRequired && !aPatchReady)
        }
    }

    NavigationBar(
        modifier = Modifier.blurEffect(backdrop),
        color = backdrop.getAppBarColor()
    ) {
        availablePages.forEachIndexed { index, destination ->
            val isSelected = selectedPage == index

            NavigationBarItem(
                selected = isSelected,
                onClick = { navigator.switchToTab(index) },
                icon = if (isSelected) destination.iconSelected else destination.iconNotSelected,
                label = stringResource(destination.label)
            )
        }
    }
}
