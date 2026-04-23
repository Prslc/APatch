package me.bmax.apatch.ui.page.terminal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.bmax.apatch.ui.navigation.MODULE_TYPE
import me.bmax.apatch.ui.navigation.TERMINAL_TASK_TYPE
import me.bmax.apatch.util.installModule
import me.bmax.apatch.util.runAPModuleAction

class TerminalViewModel : ViewModel() {
    var uiState by mutableStateOf(TerminalUiState())
        private set

    private val fullLog = StringBuilder()

    fun executeTask(
        taskType: TERMINAL_TASK_TYPE,
        targetId: String,
        moduleType: MODULE_TYPE
    ) {
        if (uiState.isRunning) return

        viewModelScope.launch(Dispatchers.IO) {
            uiState = uiState.copy(isRunning = true)

            val success = when (taskType) {
                TERMINAL_TASK_TYPE.INSTALL -> {
                    val uri = targetId.toUri()
                    installModule(
                        uri = uri,
                        type = moduleType,
                        onStdout = { appendLog(it) },
                        onStderr = { appendLog(it) }
                    )
                }
                TERMINAL_TASK_TYPE.ACTION -> {
                    runAPModuleAction(
                        moduleId = targetId,
                        onStdout = { appendLog(it) },
                        onStderr = { appendLog(it) }
                    )
                }
            }

            uiState = uiState.copy(
                isRunning = false,
                isFinished = true,
                isSuccess = success
            )
        }
    }

    private fun appendLog(line: String) {
        synchronized(fullLog) {
            if (line.startsWith("[H[J")) fullLog.clear()  // // clear command
            fullLog.append(line).append("\n")
            uiState = uiState.copy(logs = fullLog.toString())
        }
    }

    fun getFullLog() = synchronized(fullLog) { fullLog.toString() }
}