package me.bmax.apatch.ui.component.bottombar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import me.bmax.apatch.ui.LocalSelectedPage
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

    val availablePages = rememberAvailablePages()

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
