package me.bmax.apatch.ui.page.home

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.ui.LocalUiMode
import me.bmax.apatch.ui.LocalModuleCounts
import me.bmax.apatch.ui.UiMode
import me.bmax.apatch.util.migrateStockBootBackup

@Composable
fun HomeScreen(
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
    isCurrentPage: Boolean = true,
    viewModel: HomeViewModel = viewModel(),
) {
    val counts = LocalModuleCounts.current

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { migrateStockBootBackup() }
    }

    LaunchedEffect(viewModel) {
        viewModel.uiState
            .map { Triple(it.apmCount, it.kpmCount, it.suCount) }
            .distinctUntilChanged()
            .collect { (apm, kpm, su) ->
                counts.apmCount = apm
                counts.kpmCount = kpm
                counts.suCount = su
            }
    }

    when (LocalUiMode.current) {
        UiMode.Miuix -> HomeScreenMiuix(bottomPadding, modifier, isCurrentPage, viewModel)
        UiMode.Material -> HomeScreenMaterial(bottomPadding, modifier, isCurrentPage, viewModel)
    }
}

@Composable
internal fun rememberCheckUpdate(): Boolean {
    val prefs = APApplication.sharedPreferences
    val enabled by produceState(initialValue = prefs.getBoolean("check_update", true)) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
            if (key == "check_update") value = p.getBoolean(key, true)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return enabled
}
