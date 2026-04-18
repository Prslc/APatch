package me.bmax.apatch.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.bmax.apatch.BuildConfig
import me.bmax.apatch.R
import me.bmax.apatch.ui.navigation.LocalNavigator
import me.bmax.apatch.ui.navigation.Navigator
import me.bmax.apatch.ui.theme.getAppBarColor
import me.bmax.apatch.ui.theme.blurEffect
import me.bmax.apatch.ui.theme.rememberBlurBackdrop
import me.bmax.apatch.util.Version
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

@Composable
fun AboutScreen() {
    val navigator = LocalNavigator.current
    val scrollBehavior = MiuixScrollBehavior()
    val uriHandler = LocalUriHandler.current

    val topBarBackdrop = rememberBlurBackdrop(true)

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.blurEffect(topBarBackdrop),
                title = stringResource(R.string.about),
                color = topBarBackdrop.getAppBarColor(),
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = {navigator.popBackStack()}) {
                        Icon(imageVector = MiuixIcons.Back, contentDescription = null)
                    }
                },
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .then(topBarBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier)
                .fillMaxSize()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = innerPadding,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Surface(
                    modifier = Modifier.size(95.dp),
                    color = colorResource(id = R.color.ic_launcher_background),
                    shape = RoundedCornerShape(30.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "icon",
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            item {
                Text(
                    text = stringResource(id = R.string.app_name),
                    style = MiuixTheme.textStyles.title2,
                    fontWeight = FontWeight(550)
                )
                Text(
                    text = stringResource(
                        id = R.string.about_app_version,
                        if (BuildConfig.VERSION_NAME.contains(BuildConfig.VERSION_CODE.toString())) "${BuildConfig.VERSION_CODE}" else "${BuildConfig.VERSION_CODE} (${BuildConfig.VERSION_NAME})"
                    ),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                    modifier = Modifier.padding(top = 5.dp)
                )
                Text(
                    text = stringResource(
                        id = R.string.about_powered_by,
                        "KernelPatch (${Version.buildKPVString()})"
                    ),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                    modifier = Modifier.padding(top = 5.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            item {
                Card(
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    LinkItem(
                        title = stringResource(R.string.about_github),
                        summary = stringResource(R.string.about_github_summary),
                        icon = painterResource(R.drawable.github)
                    ) {
                        uriHandler.openUri("https://github.com/bmax121/APatch")
                    }

                    LinkItem(
                        title = stringResource(R.string.about_telegram_channel),
                        summary = stringResource(R.string.about_telegram_channel_summary),
                        icon = painterResource(R.drawable.channel)
                    ) {
                        uriHandler.openUri("https://t.me/APatchChannel")
                    }

                    LinkItem(
                        title = stringResource(R.string.about_weblate),
                        summary = stringResource(R.string.about_weblate_summary),
                        icon = painterResource(R.drawable.weblate)
                    ) {
                        uriHandler.openUri("https://hosted.weblate.org/engage/APatch")
                    }

                    LinkItem(
                        title = stringResource(R.string.about_telegram_group),
                        summary = stringResource(R.string.about_telegram_group_summary),
                        icon = painterResource(R.drawable.telegram)
                    ) {
                        uriHandler.openUri("https://t.me/apatch_discuss")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                Card(
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.about_app_desc),
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun LinkItem(
    title: String,
    summary: String,
    icon: Painter,
    onClick: () -> Unit
) {
    ArrowPreference(
        title = title,
        summary = summary,
        onClick = onClick,
        startAction = {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }
    )
}