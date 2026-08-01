package me.bmax.apatch.ui.component.loadingindicator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.bmax.apatch.R
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun LoadingIndicatorMiuix(
    modifier: Modifier = Modifier,
    text: String? = stringResource(R.string.loading)
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        InfiniteProgressIndicator()

        if (!text.isNullOrEmpty()) {
            Spacer(modifier = Modifier.size(12.dp))

            Text(
                text = text,
                color = MiuixTheme.colorScheme.onSurface,
                style = MiuixTheme.textStyles.body1
            )
        }
    }
}
