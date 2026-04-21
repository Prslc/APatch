package me.bmax.apatch.ui.page.settings

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.LocaleList
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Commit
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.app.LocaleManagerCompat
import androidx.core.content.FileProvider
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.BuildConfig
import me.bmax.apatch.Natives
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.ArrowItem
import me.bmax.apatch.ui.component.DropdownItem
import me.bmax.apatch.ui.component.SwitchItem
import me.bmax.apatch.ui.component.rememberLoadingDialog
import me.bmax.apatch.ui.navigation.LocalNavigator
import me.bmax.apatch.ui.theme.blurEffect
import me.bmax.apatch.ui.theme.getAppBarColor
import me.bmax.apatch.ui.theme.rememberBlurBackdrop
import me.bmax.apatch.util.calculateCacheSize
import me.bmax.apatch.util.clearAppCache
import me.bmax.apatch.util.formatSize
import me.bmax.apatch.util.getBugreportFile
import me.bmax.apatch.util.isGlobalNamespaceEnabled
import me.bmax.apatch.util.outputStream
import me.bmax.apatch.util.rootShellForResult
import me.bmax.apatch.util.setGlobalNamespaceEnabled
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.window.WindowDialog
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun SettingScreen(bottomPadding: Dp) {
    val navigator = LocalNavigator.current
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop(true)

    val state by APApplication.apStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)
    val kPatchReady = state != APApplication.State.UNKNOWN_STATE
    val aPatchReady =
        (state == APApplication.State.ANDROIDPATCH_INSTALLING || state == APApplication.State.ANDROIDPATCH_INSTALLED || state == APApplication.State.ANDROIDPATCH_NEED_UPDATE)
    var isGlobalNamespaceEnabled by rememberSaveable {
        mutableStateOf(false)
    }
    if (kPatchReady && aPatchReady) {
        isGlobalNamespaceEnabled = isGlobalNamespaceEnabled()
    }
    val showResetSuPathDialog = remember { mutableStateOf(false) }
    val showLogDialog = remember { mutableStateOf(false) }
    val showClearDialog = rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cacheSize = remember { mutableLongStateOf(0L) }
    val noClear = stringResource(R.string.no_cache_to_clear)

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.blurEffect(backdrop),
                color = backdrop.getAppBarColor(),
                title = stringResource(R.string.settings),
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->

        ResetSUPathDialog(showResetSuPathDialog, context)
        ClearDialog(showClearDialog, cacheSize.longValue, context, scope)
        LogDialog(showLogDialog, context, scope)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier)
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = paddingValues.calculateTopPadding() + 16.dp,
                end = 16.dp,
                bottom = paddingValues.calculateBottomPadding() + 16.dp
            )
        ) {
            item {
                val prefs = APApplication.sharedPreferences
                Card {
                    // Global mount
                    if (kPatchReady && aPatchReady) {
                        SwitchItem(
                            title = stringResource(R.string.settings_global_namespace_mode),
                            summary = stringResource(R.string.settings_global_namespace_mode_summary),
                            icon = Icons.Filled.Engineering,
                            checked = isGlobalNamespaceEnabled,
                            contentDescription = stringResource(R.string.settings_global_namespace_mode_summary),
                            onCheckedChange = {
                                setGlobalNamespaceEnabled(
                                    if (isGlobalNamespaceEnabled) "0" else "1"
                                )
                                isGlobalNamespaceEnabled = it
                            }
                        )
                    }

                    // WebView Debug
                    if (aPatchReady) {
                        var enableWebDebugging by rememberSaveable {
                            mutableStateOf(prefs.getBoolean("enable_web_debugging", false))
                        }

                        SwitchItem(
                            title = stringResource(R.string.enable_web_debugging),
                            summary = stringResource(R.string.enable_web_debugging_summary),
                            icon = Icons.Filled.DeveloperMode,
                            checked = enableWebDebugging,
                            contentDescription = stringResource(R.string.enable_web_debugging_summary),
                            onCheckedChange = { isChecked ->
                                enableWebDebugging = isChecked
                                APApplication.sharedPreferences.edit {
                                    putBoolean("enable_web_debugging", isChecked)
                                }
                            }
                        )
                    }

                    // Check Update
                    var checkUpdate by rememberSaveable {
                        mutableStateOf(
                            prefs.getBoolean("check_update", true)
                        )
                    }

                    SwitchItem(
                        title = stringResource(R.string.settings_check_update),
                        summary = stringResource(R.string.settings_check_update_summary),
                        icon = Icons.Filled.Update,
                        checked = checkUpdate,
                        contentDescription = stringResource(R.string.settings_check_update_summary),
                        onCheckedChange = { isChecked ->
                            checkUpdate = isChecked
                            prefs.edit { putBoolean("check_update", isChecked) }
                        }
                    )

                    // Theme System
                    val themeItems = listOf(
                        stringResource(R.string.settings_theme_mode_system),
                        stringResource(R.string.settings_theme_mode_light),
                        stringResource(R.string.settings_theme_mode_dark),
                        stringResource(R.string.settings_theme_mode_monet_system),
                        stringResource(R.string.settings_theme_mode_monet_light),
                        stringResource(R.string.settings_theme_mode_monet_dark),
                    )
                    var themeMode by rememberSaveable {
                        mutableIntStateOf(prefs.getInt("color_mode", 0))
                    }
                    DropdownItem(
                        title = stringResource(R.string.settings_theme),
                        summary = stringResource(R.string.settings_theme_summary),
                        items = themeItems,
                        selectedIndex = themeMode,
                        icon = Icons.Rounded.Palette,
                        onSelectedIndexChange = { index ->
                            prefs.edit { putInt("color_mode", index) }
                            themeMode = index
                        }
                    )

                    AnimatedVisibility(
                        visible = themeMode in 3..5
                    ) {
                        val colorItems = listOf(
                            stringResource(R.string.settings_key_color_default),
                            stringResource(R.string.color_red),
                            stringResource(R.string.color_green),
                            stringResource(R.string.color_blue),
                            stringResource(R.string.color_purple),
                            stringResource(R.string.color_orange),
                            stringResource(R.string.color_teal),
                            stringResource(R.string.color_pink),
                            stringResource(R.string.color_brown),
                        )
                        val colorValues = listOf(
                            0,
                            Color(0xFFEA4335).toArgb(),
                            Color(0xFF34A853).toArgb(),
                            Color(0xFF1A73E8).toArgb(),
                            Color(0xFF9333EA).toArgb(),
                            Color(0xFFFB8C00).toArgb(),
                            Color(0xFF009688).toArgb(),
                            Color(0xFFE91E63).toArgb(),
                            Color(0xFF795548).toArgb(),
                        )
                        var keyColorIndex by rememberSaveable {
                            mutableIntStateOf(
                                colorValues.indexOf(prefs.getInt("key_color", 0))
                                    .takeIf { it >= 0 }
                                    ?: 0
                            )
                        }
                        DropdownItem(
                            title = stringResource(R.string.settings_key_color),
                            summary = stringResource(R.string.settings_key_color_summary),
                            items = colorItems,
                            selectedIndex = keyColorIndex,
                            icon = Icons.Rounded.Colorize,
                            onSelectedIndexChange = { index ->
                                prefs.edit { putInt("key_color", colorValues[index]) }
                                keyColorIndex = index
                            }
                        )
                    }

                    // reset su path
                    if (kPatchReady) {
                        ArrowItem(
                            title = stringResource(R.string.setting_reset_su_path),
                            summary = stringResource(R.string.setting_reset_su_path_summary),
                            icon = Icons.Filled.Commit,
                            contentDescription = stringResource(R.string.setting_reset_su_path),
                            onClick = {
                                showResetSuPathDialog.value = true
                            }
                        )
                    }

                    // language
                    val languages = stringArrayResource(R.array.languages)
                    val languagesValues = stringArrayResource(R.array.languages_values)

                    val currentLocales = LocaleManagerCompat.getApplicationLocales(context)
                    val currentLanguageTag = if (currentLocales.isEmpty) {
                        null
                    } else {
                        currentLocales.get(0)?.toLanguageTag()
                    }

                    val initialIndex = if (currentLanguageTag == null) {
                        0
                    } else {
                        val index = languagesValues.indexOf(currentLanguageTag)
                        if (index >= 0) index else 0
                    }

                    var selectedIndex by remember { mutableIntStateOf(initialIndex) }

                    DropdownItem(
                        title = stringResource(R.string.settings_app_language),
                        summary = stringResource(R.string.settings_app_language_summary),
                        items = languages.toList(),
                        selectedIndex = selectedIndex,
                        icon = Icons.Filled.Translate,
                        onSelectedIndexChange = { newIndex ->
                            selectedIndex = newIndex
                            val tag = if (newIndex == 0) "" else languagesValues[newIndex]
                            updateLanguage(context, tag)
                        }
                    )

                    // clean cache
                    ArrowItem(
                        title = stringResource(R.string.settings_clean_cache),
                        summary = stringResource(R.string.settings_clean_cache_summary),
                        icon = Icons.Filled.CleaningServices,
                        contentDescription = stringResource(R.string.settings_clean_cache),
                        onClick = {
                            scope.launch {
                                val size = calculateCacheSize(context)
                                cacheSize.longValue = size
                                if (size > 0L) {
                                    showClearDialog.value = true
                                } else {
                                    Toast.makeText(
                                        context,
                                        noClear,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    )

                    // log
                    ArrowItem(
                        title = stringResource(R.string.send_log),
                        summary = stringResource(R.string.send_log_summary),
                        icon = Icons.Filled.BugReport,
                        contentDescription = stringResource(R.string.send_log),
                        onClick = {
                            showLogDialog.value = true
                        }
                    )

                    // about
                    ArrowItem(
                        title = stringResource(R.string.home_more_menu_about),
                        summary = stringResource(R.string.about_summary),
                        icon = Icons.Filled.Info,
                        contentDescription = stringResource(R.string.home_more_menu_about),
                        onClick = {
                            navigator.navigateToAbout()
                        }
                    )
                }
                Spacer(Modifier.height(bottomPadding))
            }
        }
    }
}

fun updateLanguage(context: Context, localeTag: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val localeManager = context.getSystemService(LocaleManager::class.java)
        val locales = if (localeTag.isEmpty()) {
            LocaleList.getEmptyLocaleList()
        } else {
            LocaleList.forLanguageTags(localeTag)
        }
        // Native API 33+ handles persistence and lifecycle automatically.
        localeManager.applicationLocales = locales
    } else {
        // For API 26-32, we manually persist the preference.
        APApplication.sharedPreferences.edit {
            putString("app_lang", localeTag)
        }

        // Recreate the activity to apply language changes on legacy API levels.
        (context as? Activity)?.recreate()
    }
}

@Composable
fun LogDialog(
    showLogDialog: MutableState<Boolean>,
    context: Context,
    scope: CoroutineScope
) {
    val loadingDialog = rememberLoadingDialog()
    val logSavedMessage = stringResource(R.string.log_saved)

    val exportBugreportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/gzip")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                loadingDialog.show()
                uri.outputStream().use { output ->
                    getBugreportFile(context).inputStream().use { it.copyTo(output) }
                }
                loadingDialog.hide()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, logSavedMessage, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    WindowDialog(
        show = showLogDialog.value,
        title = stringResource(R.string.send_log),
        onDismissRequest = { showLogDialog.value = false }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable {
                        scope.launch {
                            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH_mm")
                            val current = LocalDateTime.now().format(formatter)
                            exportBugreportLauncher.launch("APatch_bugreport_${current}.tar.gz")
                            showLogDialog.value = false
                        }
                    }
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(30.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.save_log))
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable {
                        scope.launch {
                            val bugreport = loadingDialog.withLoading {
                                withContext(Dispatchers.IO) { getBugreportFile(context) }
                            }

                            val uri: Uri = FileProvider.getUriForFile(
                                context,
                                "${BuildConfig.APPLICATION_ID}.fileprovider",
                                bugreport
                            )

                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                putExtra(Intent.EXTRA_STREAM, uri)
                                setDataAndType(uri, "application/gzip")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }

                            context.startActivity(
                                Intent.createChooser(shareIntent, logSavedMessage)
                            )
                            showLogDialog.value = false
                        }
                    }
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(30.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.send_log))
            }
        }
    }
}

@Composable
fun ResetSUPathDialog(
    showResetSuDialog: MutableState<Boolean>,
    context: Context
) {
    var suPath by remember { mutableStateOf(Natives.suPath()) }

    val suPathChecked: (path: String) -> Boolean = {
        it.startsWith("/") && it.trim().length > 1
    }

    WindowDialog(
        show = showResetSuDialog.value,
        title = stringResource(R.string.setting_reset_su_path),
        onDismissRequest = { showResetSuDialog.value = false }
    ) {
        TextField(
            value = suPath,
            onValueChange = { suPath = it },
            label = stringResource(R.string.setting_reset_su_new_path),
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {

            TextButton(
                stringResource(android.R.string.cancel),
                onClick = { showResetSuDialog.value = false },
                modifier = Modifier.weight(1f),
            )

            Spacer(Modifier.width(20.dp))

            TextButton(
                stringResource(android.R.string.ok),
                onClick = {
                    showResetSuDialog.value = false
                    val success = Natives.resetSuPath(suPath)
                    Toast.makeText(
                        context,
                        if (success) R.string.success else R.string.failure,
                        Toast.LENGTH_SHORT
                    ).show()
                    rootShellForResult("echo $suPath > ${APApplication.SU_PATH_FILE}")
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
                enabled = suPathChecked(suPath)
            )
        }
    }
}

@Composable
fun ClearDialog(
    showCleanDialog: MutableState<Boolean>,
    initialCacheSize: Long,
    context: Context,
    scope: CoroutineScope
) {
    val cacheSize = remember { mutableStateOf(formatSize(initialCacheSize)) }
    val loading = rememberLoadingDialog()

    LaunchedEffect(showCleanDialog.value) {
        if (showCleanDialog.value) {
            val size = calculateCacheSize(context)
            cacheSize.value = formatSize(size)
        }
    }

    WindowDialog(
        show = showCleanDialog.value,
        title = stringResource(R.string.clear_cache_title),
        summary = stringResource(R.string.clear_cache_message, cacheSize.value),
        onDismissRequest = { showCleanDialog.value = false }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                stringResource(android.R.string.cancel),
                onClick = { showCleanDialog.value = false },
                modifier = Modifier.weight(1f),
            )

            Spacer(Modifier.width(20.dp))

            TextButton(
                stringResource(android.R.string.ok),
                onClick = {
                    showCleanDialog.value = false
                    scope.launch {
                        loading.withLoading {
                            val freed = clearAppCache(context)
                            Log.i("Cache", "Freed ${formatSize(freed)}")
                            cacheSize.value = formatSize(calculateCacheSize(context))
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }
}
