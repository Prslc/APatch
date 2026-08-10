package me.bmax.apatch.ui.webui

import android.webkit.JsPromptResult
import android.webkit.JsResult

sealed class WebUIEvent {
    data class ShowAlert(val message: String, val result: JsResult) : WebUIEvent()
    data class ShowConfirm(val message: String, val result: JsResult) : WebUIEvent()
    data class ShowPrompt(val message: String, val defaultValue: String, val result: JsPromptResult) : WebUIEvent()
}
