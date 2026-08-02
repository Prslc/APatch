package me.bmax.apatch.ui.component.bottombar

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import me.bmax.apatch.APApplication
import me.bmax.apatch.ui.LocalSelectedPage
import me.bmax.apatch.ui.component.bottombar.BottomBarDestination
import me.bmax.apatch.ui.navigation.LocalNavigator
import me.bmax.apatch.ui.theme.getMaterial3AppBarColor
import me.bmax.apatch.ui.theme.material3BlurEffect
import top.yukonga.miuix.kmp.blur.LayerBackdrop

@Composable
fun BottomBarMaterial(backdrop: LayerBackdrop?) {
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
        modifier = Modifier.material3BlurEffect(backdrop),
        containerColor = backdrop.getMaterial3AppBarColor(),
    ) {
        availablePages.forEachIndexed { index, destination ->
            val selected = selectedPage == index

            NavigationBarItem(
                selected = selected,
                onClick = { navigator.switchToTab(index) },
                icon = {
                    BadgedBox(
                        badge = { /* TODO: badge counts */ }
                    ) {
                        Icon(
                            imageVector = if (selected) destination.iconSelected else destination.iconNotSelected,
                            contentDescription = stringResource(destination.label)
                        )
                    }
                },
                label = {
                    Text(
                        text = stringResource(destination.label),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
}
