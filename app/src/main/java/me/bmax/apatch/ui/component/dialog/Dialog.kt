package me.bmax.apatch.ui.component.dialog

import android.os.Parcelable
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.parcelize.Parcelize
import me.bmax.apatch.R
import me.bmax.apatch.ui.LocalUiMode
import me.bmax.apatch.ui.UiMode
import me.bmax.apatch.util.RebootMode
import me.bmax.apatch.util.isJailbreakMode
import me.bmax.apatch.util.reboot
import me.bmax.apatch.util.softReboot
import kotlin.coroutines.resume

private const val TAG = "DialogComponent"

interface ConfirmDialogVisuals : Parcelable {
    val title: String
    val content: String
    val isMarkdown: Boolean
    val confirm: String?
    val dismiss: String?
}

@Parcelize
private data class ConfirmDialogVisualsImpl(
    override val title: String,
    override val content: String,
    override val isMarkdown: Boolean,
    override val confirm: String?,
    override val dismiss: String?,
) : ConfirmDialogVisuals {
    companion object {
        val Empty: ConfirmDialogVisuals = ConfirmDialogVisualsImpl("", "", false, null, null)
    }
}

interface DialogHandle {
    val isShown: Boolean
    val dialogType: String
    fun show()
    fun hide()
}

interface LoadingDialogHandle : DialogHandle {
    suspend fun <R> withLoading(block: suspend () -> R): R
    fun showLoading()
}

sealed interface ConfirmResult {
    data object Confirmed : ConfirmResult
    data object Canceled : ConfirmResult
}

interface ConfirmDialogHandle : DialogHandle {
    val visuals: ConfirmDialogVisuals

    fun showConfirm(
        title: String,
        content: String,
        markdown: Boolean = false,
        confirm: String? = null,
        dismiss: String? = null,
        onConfirm: () -> Unit = {}
    )

    suspend fun awaitConfirm(
        title: String,
        content: String,
        markdown: Boolean = false,
        confirm: String? = null,
        dismiss: String? = null
    ): ConfirmResult
}

private abstract class DialogHandleBase(
    protected val visible: MutableState<Boolean>,
    protected val coroutineScope: CoroutineScope
) : DialogHandle {
    override val isShown: Boolean
        get() = visible.value

    override fun show() {
        coroutineScope.launch {
            visible.value = true
        }
    }

    final override fun hide() {
        coroutineScope.launch {
            visible.value = false
        }
    }

    override fun toString(): String {
        return dialogType
    }
}

private class LoadingDialogHandleImpl(
    visible: MutableState<Boolean>,
    coroutineScope: CoroutineScope
) : LoadingDialogHandle, DialogHandleBase(visible, coroutineScope) {
    override suspend fun <R> withLoading(block: suspend () -> R): R {
        return coroutineScope.async {
            try {
                visible.value = true
                block()
            } finally {
                visible.value = false
            }
        }.await()
    }

    override fun showLoading() {
        show()
    }

    override val dialogType: String get() = "LoadingDialog"
}

typealias NullableCallback = (() -> Unit)?

interface ConfirmCallback {

    val onConfirm: NullableCallback

    val onDismiss: NullableCallback

    val isEmpty: Boolean get() = onConfirm == null && onDismiss == null

    companion object {
        operator fun invoke(
            onConfirmProvider: () -> NullableCallback,
            onDismissProvider: () -> NullableCallback
        ): ConfirmCallback {
            return object : ConfirmCallback {
                override val onConfirm: NullableCallback
                    get() = onConfirmProvider()
                override val onDismiss: NullableCallback
                    get() = onDismissProvider()
            }
        }
    }
}

private class ConfirmDialogHandleImpl(
    visible: MutableState<Boolean>,
    coroutineScope: CoroutineScope,
    callback: ConfirmCallback,
    visuals: ConfirmDialogVisuals = ConfirmDialogVisualsImpl.Empty,
    private val resultFlow: ReceiveChannel<ConfirmResult>
) : ConfirmDialogHandle, DialogHandleBase(visible, coroutineScope) {
    // State-backed so updateVisuals() triggers recomposition of readers
    // (plain property would leave M3 dialogs showing stale/Empty visuals)
    override var visuals: ConfirmDialogVisuals by mutableStateOf(visuals)
    private class ResultCollector(
        private val callback: ConfirmCallback
    ) : FlowCollector<ConfirmResult> {
        fun handleResult(result: ConfirmResult) {
            Log.d(TAG, "handleResult: ${result.javaClass.simpleName}")
            when (result) {
                ConfirmResult.Confirmed -> onConfirm()
                ConfirmResult.Canceled -> onDismiss()
            }
        }

        fun onConfirm() {
            callback.onConfirm?.invoke()
        }

        fun onDismiss() {
            callback.onDismiss?.invoke()
        }

        override suspend fun emit(value: ConfirmResult) {
            handleResult(value)
        }
    }

    private val resultCollector = ResultCollector(callback)

    private var awaitContinuation: CancellableContinuation<ConfirmResult>? = null

    private val isCallbackEmpty = callback.isEmpty

    init {
        coroutineScope.launch {
            resultFlow
                .consumeAsFlow()
                .onEach { result ->
                    awaitContinuation?.let {
                        awaitContinuation = null
                        if (it.isActive) {
                            it.resume(result)
                        }
                    }
                }
                .onEach { hide() }
                .collect(resultCollector)
        }
    }

    private suspend fun awaitResult(): ConfirmResult {
        return suspendCancellableCoroutine {
            awaitContinuation = it.apply {
                if (isCallbackEmpty) {
                    invokeOnCancellation {
                        visible.value = false
                    }
                }
            }
        }
    }

    fun updateVisuals(visuals: ConfirmDialogVisuals) {
        this.visuals = visuals
    }

    override fun show() {
        if (visuals !== ConfirmDialogVisualsImpl.Empty) {
            super.show()
        } else {
            throw UnsupportedOperationException("can't show confirm dialog with the Empty visuals")
        }
    }

    override fun showConfirm(
        title: String,
        content: String,
        markdown: Boolean,
        confirm: String?,
        dismiss: String?,
        onConfirm: () -> Unit
    ) {
        coroutineScope.launch {
            updateVisuals(ConfirmDialogVisualsImpl(title, content, markdown, confirm, dismiss))
            show()
        }
    }

    override suspend fun awaitConfirm(
        title: String,
        content: String,
        markdown: Boolean,
        confirm: String?,
        dismiss: String?
    ): ConfirmResult {
        coroutineScope.launch {
            updateVisuals(ConfirmDialogVisualsImpl(title, content, markdown, confirm, dismiss))
            show()
        }
        return awaitResult()
    }

    override val dialogType: String get() = "ConfirmDialog"

    override fun toString(): String {
        return "${super.toString()}(visuals: $visuals)"
    }

    companion object {
        fun Saver(
            visible: MutableState<Boolean>,
            coroutineScope: CoroutineScope,
            callback: ConfirmCallback,
            resultChannel: ReceiveChannel<ConfirmResult>
        ) = Saver<ConfirmDialogHandle, ConfirmDialogVisuals>(
            save = {
                it.visuals
            },
            restore = {
                Log.d(TAG, "ConfirmDialog restore, visuals: $it")
                ConfirmDialogHandleImpl(visible, coroutineScope, callback, it, resultChannel)
            }
        )
    }
}

private class CustomDialogHandleImpl(
    visible: MutableState<Boolean>,
    coroutineScope: CoroutineScope
) : DialogHandleBase(visible, coroutineScope) {
    override val dialogType: String get() = "CustomDialog"
}

@Composable
fun rememberLoadingDialog(): LoadingDialogHandle {
    val visible = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val uiMode = LocalUiMode.current
    when (uiMode) {
        UiMode.Miuix -> if (visible.value) LoadingDialogMiuix()
        UiMode.Material -> LoadingDialogMaterial(visible)
    }

    return remember {
        LoadingDialogHandleImpl(visible, coroutineScope)
    }
}

@Composable
private fun rememberConfirmDialog(
    visuals: ConfirmDialogVisuals,
    callback: ConfirmCallback
): ConfirmDialogHandle {
    val visible = rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val resultChannel = remember { Channel<ConfirmResult>() }

    val handle = rememberSaveable(
        saver = ConfirmDialogHandleImpl.Saver(visible, coroutineScope, callback, resultChannel),
        init = {
            ConfirmDialogHandleImpl(visible, coroutineScope, callback, visuals, resultChannel)
        }
    )

    val uiMode = LocalUiMode.current
    when (uiMode) {
        UiMode.Miuix -> if (visible.value) ConfirmDialogMiuix(
            handle.visuals,
            confirm = { coroutineScope.launch { resultChannel.send(ConfirmResult.Confirmed) } },
            dismiss = { coroutineScope.launch { resultChannel.send(ConfirmResult.Canceled) } },
            showDialog = visible
        )
        UiMode.Material -> if (visible.value) ConfirmDialogMaterial(
            handle.visuals,
            confirm = { coroutineScope.launch { resultChannel.send(ConfirmResult.Confirmed) } },
            dismiss = { coroutineScope.launch { resultChannel.send(ConfirmResult.Canceled) } },
            showDialog = visible
        )
    }

    return handle
}

@Composable
fun rememberConfirmCallback(
    onConfirm: NullableCallback,
    onDismiss: NullableCallback
): ConfirmCallback {
    val currentOnConfirm by rememberUpdatedState(newValue = onConfirm)
    val currentOnDismiss by rememberUpdatedState(newValue = onDismiss)
    return remember {
        ConfirmCallback({ currentOnConfirm }, { currentOnDismiss })
    }
}

@Composable
fun rememberConfirmDialog(
    onConfirm: NullableCallback = null,
    onDismiss: NullableCallback = null
): ConfirmDialogHandle {
    return rememberConfirmDialog(rememberConfirmCallback(onConfirm, onDismiss))
}

@Composable
fun rememberConfirmDialog(callback: ConfirmCallback): ConfirmDialogHandle {
    return rememberConfirmDialog(ConfirmDialogVisualsImpl.Empty, callback)
}

/**
 * A confirm-before-soft-reboot dialog used in jailbreak mode: rebooting
 * the framework keeps runtime-loaded modules alive, so jailbreak entry
 * points ask for confirmation before triggering `apd soft-reboot`.
 *
 * Returns a handle whose [show] displays the dialog; confirming triggers
 * the soft reboot.
 */
@Composable
fun rememberJailbreakSoftRebootDialog(): DialogHandle {
    val softRebootDialog = rememberConfirmDialog(
        onConfirm = { softReboot() }
    )
    val title = stringResource(R.string.settings_jailbreak_loaded)
    val content = stringResource(R.string.settings_jailbreak_soft_reboot_message)
    val confirm = stringResource(R.string.settings_jailbreak_soft_reboot)

    return remember {
        object : DialogHandle {
            override val isShown: Boolean
                get() = softRebootDialog.isShown

            override val dialogType: String
                get() = "JailbreakSoftRebootDialog"

            override fun show() {
                softRebootDialog.showConfirm(
                    title = title,
                    content = content,
                    confirm = confirm
                )
            }

            override fun hide() {
                softRebootDialog.hide()
            }
        }
    }
}

/**
 * Returns a reboot action for a reboot list. In jailbreak mode a plain reboot
 * ([RebootMode.NORMAL]) drops root, so it asks for confirmation first; the soft
 * reboot and recovery/bootloader/download/edl entries run immediately since the
 * user picked an explicit target.
 *
 * Mirrors KernelSU's `rememberRebootAction`.
 */
@Composable
fun rememberRebootAction(): (RebootMode) -> Unit {
    val title = stringResource(R.string.reboot)
    val content = stringResource(R.string.settings_jailbreak_reboot_warning)
    val confirmDialog = rememberConfirmDialog(onConfirm = { reboot() })

    return remember(title, content, confirmDialog) {
        { mode ->
            if (isJailbreakMode() && mode == RebootMode.NORMAL) {
                confirmDialog.showConfirm(title = title, content = content)
            } else {
                reboot(mode)
            }
        }
    }
}

/**
 * A reusable settings-style dialog that dispatches to the right shell
 * based on [me.bmax.apatch.ui.LocalUiMode].
 *
 * Call [EditableDialogHandle.show] to display, or use the returned handle
 * directly. The dialog includes a title, a custom body composable, and
 * confirm / dismiss buttons with callbacks.
 */
interface EditableDialogHandle {
    fun show()
    fun hide()
}

private class EditableDialogHandleImpl(
    private val visible: MutableState<Boolean>
) : EditableDialogHandle {
    override fun show() { visible.value = true }
    override fun hide() { visible.value = false }
}

@Composable
fun rememberEditableDialog(
    title: String,
    confirmText: String? = null,
    dismissText: String? = null,
    onConfirm: () -> Unit = {},
    onDismiss: () -> Unit = {},
    content: @Composable () -> Unit,
): EditableDialogHandle {
    val visible = rememberSaveable { mutableStateOf(false) }

    val uiMode = LocalUiMode.current
    if (visible.value) {
        when (uiMode) {
            UiMode.Miuix -> me.bmax.apatch.ui.component.dialog.EditableDialogMiuix(
                title = title,
                content = content,
                confirmText = confirmText,
                dismissText = dismissText,
                onConfirm = {
                    onConfirm()
                    visible.value = false
                },
                onDismiss = {
                    onDismiss()
                    visible.value = false
                }
            )

            UiMode.Material -> me.bmax.apatch.ui.component.dialog.EditableDialogMaterial(
                title = title,
                content = content,
                confirmText = confirmText,
                dismissText = dismissText,
                onConfirm = {
                    onConfirm()
                    visible.value = false
                },
                onDismiss = {
                    onDismiss()
                    visible.value = false
                }
            )
        }
    }

    return remember { EditableDialogHandleImpl(visible) }
}
