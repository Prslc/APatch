package me.bmax.apatch.ui.page.terminal

import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.KeyEventBlocker
import me.bmax.apatch.ui.navigation.MODULE_TYPE
import me.bmax.apatch.ui.navigation.TERMINAL_TASK_TYPE
import me.bmax.apatch.ui.theme.blurEffect
import me.bmax.apatch.ui.theme.getAppBarColor
import me.bmax.apatch.ui.theme.rememberBlurBackdrop
import me.bmax.apatch.ui.theme.withBackdrop
import me.bmax.apatch.util.rebootJailbreakAware
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TerminalScreenMiuix(
    taskType: TERMINAL_TASK_TYPE,
    targetId: String,
    moduleType: MODULE_TYPE,
    onBack: () -> Unit,
    viewModel: TerminalViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val backdrop = rememberBlurBackdrop()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.executeTask(taskType, targetId, moduleType)
    }

    LaunchedEffect(state.logs) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    LaunchedEffect(state.isFinished, state.isSuccess) {
        if (state.isFinished && state.isSuccess) {
            if (taskType == TERMINAL_TASK_TYPE.ACTION) {
                onBack()
            }
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                modifier = Modifier.blurEffect(backdrop),
                color = backdrop.getAppBarColor(),
                title = if (taskType == TERMINAL_TASK_TYPE.INSTALL) {
                    stringResource(R.string.apm_install)
                } else {
                    stringResource(R.string.apm_action)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch(Dispatchers.IO) {
                            runCatching {
                                val format = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                                val file = File(
                                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                                    "APatch_${taskType.name}_${format.format(Date())}.log"
                                )
                                file.writeText(viewModel.getFullLog())
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "Log saved to Downloads",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }) {
                        Icon(Icons.Filled.Save, contentDescription = "Save Log")
                    }
                }
            )
        },
        floatingActionButton = {
            if (taskType == TERMINAL_TASK_TYPE.INSTALL && state.isFinished && state.isSuccess) {
                FloatingActionButton(
                    modifier = Modifier.padding(bottom = 30.dp),
                    onClick = { scope.launch(Dispatchers.IO) { rebootJailbreakAware() } }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        tint = MiuixTheme.colorScheme.onPrimary,
                        contentDescription = "Reboot"
                    )
                }
            }
        }
    ) { innerPadding ->
        KeyEventBlocker { it.key == Key.VolumeDown || it.key == Key.VolumeUp }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .withBackdrop(backdrop)
                .padding(innerPadding)
                .verticalScroll(scrollState),
        ) {
            Text(
                modifier = Modifier.padding(12.dp),
                text = state.logs,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}