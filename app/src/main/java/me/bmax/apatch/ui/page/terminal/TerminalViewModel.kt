package me.bmax.apatch.ui.page.terminal

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.bmax.apatch.data.repository.ApModuleRepository
import me.bmax.apatch.data.repository.ApModuleRepositoryImpl
import me.bmax.apatch.ui.navigation.MODULE_TYPE
import me.bmax.apatch.ui.navigation.TERMINAL_TASK_TYPE

class TerminalViewModel(
    private val moduleRepo: ApModuleRepository = ApModuleRepositoryImpl
) : ViewModel() {
    private val _uiState = MutableStateFlow(TerminalUiState())
    val uiState = _uiState.asStateFlow()

    private val fullLog = StringBuilder()

    private val clearScreenSequence = "\u001B[H\u001B[J"

    private companion object {
        // Cap the retained install log to bound memory usage on long installs.
        const val MAX_LOG_LENGTH = 100_000
    }

    fun executeTask(
        taskType: TERMINAL_TASK_TYPE,
        targetId: String,
        moduleType: MODULE_TYPE
    ) {
        if (_uiState.value.isRunning) return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isRunning = true) }

            val success = when (taskType) {
                TERMINAL_TASK_TYPE.INSTALL -> {
                    val uri = targetId.toUri()
                    moduleRepo.installModule(
                        uri = uri,
                        type = moduleType,
                        onStdout = { appendLog(it) },
                        onStderr = { appendLog(it) }
                    )
                }

                TERMINAL_TASK_TYPE.ACTION -> {
                    moduleRepo.runAction(
                        moduleId = targetId,
                        onStdout = { appendLog(it) },
                        onStderr = { appendLog(it) }
                    )
                }
            }

            _uiState.update { it.copy(
                isRunning = false,
                isFinished = true,
                isSuccess = success
            ) }
        }
    }

    private fun appendLog(line: String) {
        synchronized(fullLog) {
            if (line.startsWith(clearScreenSequence)) {     //  clear command
                fullLog.clear()
                val rest = line.removePrefix(clearScreenSequence)
                if (rest.isNotEmpty()) {
                    fullLog.append(rest).append("\n")
                }
            } else {
                fullLog.append(line).append("\n")
            }
            // Cap the retained log to bound memory usage on long installs.
            if (fullLog.length > MAX_LOG_LENGTH) {
                fullLog.delete(0, fullLog.length - MAX_LOG_LENGTH)
            }
            _uiState.update { it.copy(logs = fullLog.toString()) }
        }
    }

    fun getFullLog() = synchronized(fullLog) { fullLog.toString() }
}