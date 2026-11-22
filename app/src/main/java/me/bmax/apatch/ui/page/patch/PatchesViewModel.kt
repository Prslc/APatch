package me.bmax.apatch.ui.page.patch

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.bmax.apatch.ui.page.kpm.KPModel
import me.bmax.apatch.ui.page.patch.utils.PatchEngine
import me.bmax.apatch.ui.page.patch.utils.checkSuperKeyValidation
import me.bmax.apatch.util.LogBuffer
import me.bmax.apatch.util.copyAndCloseOut
import me.bmax.apatch.util.createRootShell
import me.bmax.apatch.util.inputStream
import me.bmax.apatch.util.isJailbreakMode

private const val TAG = "PatchViewModel"

class PatchesViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PatchUiState())
    val uiState: StateFlow<PatchUiState> = _uiState.asStateFlow()

    private val actionMutex = Mutex()

    private var entryMode = PatchMode.PATCH_ONLY
    private var prepared = false

    private val _log = MutableStateFlow("")
    val log: StateFlow<String> = _log.asStateFlow()

    private val logBuffer = LogBuffer(scope = viewModelScope, intervalMs = 0) { _log.value = it }

    private val patchEngine by lazy {
        val errorBuilder = StringBuilder()
        PatchEngine(
            shell = createRootShell(),
            onLog = { logMsg -> logBuffer.append(logMsg) },
            onError = { errMsg ->
                errorBuilder.append(errMsg)
                _uiState.update { it.copy(error = errorBuilder.toString()) }
            },
            onStateUpdate = { updateBlock -> _uiState.update(updateBlock) }
        )
    }

    fun prepare(mode: PatchMode) {
        entryMode = mode
        viewModelScope.launch(Dispatchers.IO) {
            actionMutex.withLock {
                _uiState.update { it.copy(error = "") }
                ensurePrepared()
            }
        }
    }

    private suspend fun ensurePrepared(): Boolean {
        if (isJailbreakMode()) {
            _uiState.update { it.copy(isRunning = false, jailbreakBlocked = true) }
            return false
        }
        if (prepared) {
            _uiState.update { it.copy(jailbreakBlocked = false) }
            return true
        }
        _uiState.update { it.copy(isRunning = true, error = "", jailbreakBlocked = false) }
        return try {
            patchEngine.prepareEnv()
            if (entryMode != PatchMode.UNPATCH) {
                patchEngine.parseKpimg()
            }
            if (entryMode in listOf(PatchMode.PATCH_AND_INSTALL, PatchMode.UNPATCH, PatchMode.INSTALL_TO_NEXT_SLOT)) {
                patchEngine.extractAndParseBootimg(entryMode, _uiState.value.superkey)
            }
            prepared = true
            true
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Initialization failed: ${e.message}") }
            false
        } finally {
            _uiState.update { it.copy(isRunning = false) }
        }
    }

    fun copyAndParseBootimg(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            actionMutex.withLock {
                if (!ensurePrepared()) return@withLock
                _uiState.update {
                    it.copy(
                        isRunning = true,
                        error = "",
                        kimgInfo = KPModel.KImgInfo("", false),
                        existedExtras = emptyList()
                    )
                }
                try {
                    uri.inputStream().buffered().use { src ->
                        src.copyAndCloseOut(patchEngine.srcBoot.newOutputStream())
                    }
                    patchEngine.parseBootimg(patchEngine.srcBoot.path, _uiState.value.superkey)
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = "Copy error: ${e.message}") }
                } finally {
                    _uiState.update { it.copy(isRunning = false) }
                }
            }
        }
    }

    fun embedKPM(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            actionMutex.withLock {
                if (!ensurePrepared()) return@withLock
                _uiState.update { it.copy(isRunning = true, error = "") }
                try {
                    patchEngine.embedKPM(uri)
                } finally {
                    _uiState.update { it.copy(isRunning = false) }
                }
            }
        }
    }

    fun doUnpatch() {
        viewModelScope.launch(Dispatchers.IO) {
            actionMutex.withLock {
                if (!ensurePrepared()) return@withLock
                logBuffer.clear()
                _uiState.update { it.copy(isPatching = true) }
                try {
                    patchEngine.doUnpatch(_uiState.value.bootDev)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logBuffer.append("Unpatch error: ${e.message}\n")
                    _uiState.update { it.copy(error = "Unpatch error: ${e.message}") }
                } finally {
                    logBuffer.flush()
                    _uiState.update { it.copy(isPatching = false, isPatchDone = true) }
                }
            }
        }
    }

    fun doPatch(mode: PatchMode, useKey: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            actionMutex.withLock {
                if (!ensurePrepared()) return@withLock
                logBuffer.clear()
                _uiState.update { it.copy(isPatching = true) }
                try {
                    patchEngine.doPatch(_uiState.value, mode, useKey)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logBuffer.append("Patch error: ${e.message}\n")
                    _uiState.update { it.copy(error = "Patch error: ${e.message}") }
                } finally {
                    logBuffer.flush()
                    _uiState.update { it.copy(isPatching = false, isPatchDone = true) }
                }
            }
        }
    }

    fun setSuperKey(key: String) {
        val isValid = checkSuperKeyValidation(key)
        _uiState.update {
            it.copy(
                superkey = if (isValid) key else ""
            )
        }
    }

    fun removeExistedExtra(extra: KPModel.IExtraInfo) {
        _uiState.update { it.copy(existedExtras = it.existedExtras - extra) }
    }

    fun removeNewExtra(index: Int) {
        _uiState.update {
            it.copy(
                newExtras = it.newExtras.toMutableList().apply { removeAt(index) },
                newExtrasFileName = it.newExtrasFileName.toMutableList().apply { removeAt(index) }
            )
        }
    }
}
