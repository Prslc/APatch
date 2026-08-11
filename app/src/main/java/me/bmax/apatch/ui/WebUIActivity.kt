package me.bmax.apatch.ui

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.webkit.WebViewAssetLoader
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import me.bmax.apatch.APApplication
import me.bmax.apatch.data.AppRepository
import me.bmax.apatch.ui.component.loadingindicator.LoadingIndicator
import me.bmax.apatch.ui.theme.APatchAppTheme
import me.bmax.apatch.ui.theme.readAppThemeSettings
import me.bmax.apatch.ui.webui.AppIconUtil
import me.bmax.apatch.ui.webui.HandleWebUIEventMaterial
import me.bmax.apatch.ui.webui.HandleWebUIEventMiuix
import me.bmax.apatch.ui.webui.Insets
import me.bmax.apatch.ui.webui.SuFilePathHandler
import me.bmax.apatch.ui.webui.WebUIEvent
import me.bmax.apatch.ui.webui.WebViewInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.File

@SuppressLint("SetJavaScriptEnabled")
class WebUIActivity : ComponentActivity() {
    private lateinit var webViewInterface: WebViewInterface
    private var webView: WebView? = null
    private lateinit var container: FrameLayout
    private lateinit var insets: Insets
    private var insetsContinuation: CancellableContinuation<Unit>? = null
    private var isInsetsEnabled = false
    private var webCanGoBack = false
    private lateinit var fileChooserLauncher: ActivityResultLauncher<Intent>
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var uiEvent by mutableStateOf<WebUIEvent?>(null)
    private var isLoading by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webCanGoBack) {
                    webView?.goBack()
                    return
                }
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        })

        setContent {
            val settings = remember {
                readAppThemeSettings(getSharedPreferences("config", MODE_PRIVATE))
            }

            APatchAppTheme(settings) {
                val background = when (LocalUiMode.current) {
                    UiMode.Miuix -> MiuixTheme.colorScheme.background
                    UiMode.Material -> MaterialTheme.colorScheme.surfaceContainer
                }
                Box(
                    modifier = Modifier.fillMaxSize().background(background)
                ) {
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingIndicator()
                        }
                    }
                }
                when (LocalUiMode.current) {
                    UiMode.Miuix -> HandleWebUIEventMiuix(
                        event = uiEvent,
                        onAlertResult = ::dismissAlert,
                        onConfirmResult = ::dismissConfirm,
                        onPromptResult = ::dismissPrompt,
                    )
                    UiMode.Material -> HandleWebUIEventMaterial(
                        event = uiEvent,
                        onAlertResult = ::dismissAlert,
                        onConfirmResult = ::dismissConfirm,
                        onPromptResult = ::dismissPrompt,
                    )
                }

                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_RESUME -> webView?.onResume()
                            Lifecycle.Event.ON_PAUSE -> webView?.onPause()
                            else -> {}
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                val configuration = LocalConfiguration.current
                LaunchedEffect(configuration.fontScale) {
                    webView?.settings?.textZoom = (configuration.fontScale * 100).toInt()
                }
            }
        }

        lifecycleScope.launch {
            if (AppRepository.apps.value.isEmpty()) {
                AppRepository.fetchAppList()
            }
            setupWebView()
        }

        fileChooserLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val uris: Array<Uri>? = when (result.resultCode) {
                RESULT_OK -> result.data?.let { data ->
                    when {
                        data.clipData != null -> {
                            Array(data.clipData!!.itemCount) { i ->
                                data.clipData!!.getItemAt(i).uri // Multiple files
                            }
                        }
                        data.data != null -> { arrayOf(data.data!!) } // Single file
                        else -> null
                    }
                }
                else -> null
            }
            filePathCallback?.onReceiveValue(uris)
            filePathCallback = null
        }
    }

    private suspend fun setupWebView() {
        val moduleId = intent.getStringExtra("id")!!
        val name = intent.getStringExtra("name")!!
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            @Suppress("DEPRECATION")
            setTaskDescription(ActivityManager.TaskDescription("APatch - $name"))
        } else {
            val taskDescription = ActivityManager.TaskDescription.Builder().setLabel("APatch - $name").build()
            setTaskDescription(taskDescription)
        }

        val prefs = APApplication.sharedPreferences
        WebView.setWebContentsDebuggingEnabled(prefs.getBoolean("enable_web_debugging", false))

        val webRoot = File("/data/adb/modules/${moduleId}/webroot")
        insets = Insets(0, 0, 0, 0)

        container = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        this.webView = WebView(this).apply {
            setBackgroundColor(0)
        }

        val density = resources.displayMetrics.density
        ViewCompat.setOnApplyWindowInsetsListener(container) { view, windowInsets ->
            val inset = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            insets = Insets(
                top = (inset.top / density).toInt(),
                bottom = (inset.bottom / density).toInt(),
                left = (inset.left / density).toInt(),
                right = (inset.right / density).toInt()
            )
            if (isInsetsEnabled) {
                view.setPadding(0, 0, 0, 0)
            } else {
                view.setPadding(inset.left, inset.top, inset.right, inset.bottom)
            }
            insetsContinuation?.resumeWith(Result.success(Unit))
            insetsContinuation = null
            WindowInsetsCompat.CONSUMED
        }
        container.addView(this.webView)

        suspendCancellableCoroutine { cont ->
            insetsContinuation = cont
            cont.invokeOnCancellation {
                insetsContinuation = null
            }
            addContentView(
                container,
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            )

            if (insets != Insets(0, 0, 0, 0)) {
                cont.resumeWith(Result.success(Unit))
                insetsContinuation = null
            }
        }

        val webViewAssetLoader = WebViewAssetLoader.Builder()
            .setDomain("mui.kernelsu.org")
            .addPathHandler(
                "/",
                SuFilePathHandler(this, webRoot, { insets }, { enable -> enableInsets(enable) })
            )
            .build()

        val webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                val url = request.url

                // Handle ksu://icon/[packageName] to serve app icon via WebView
                if (url.scheme.equals("ksu", ignoreCase = true) && url.host.equals("icon", ignoreCase = true)) {
                    val packageName = url.path?.substring(1)
                    if (!packageName.isNullOrEmpty()) {
                        val icon = AppIconUtil.loadAppIconSync(this@WebUIActivity, packageName, 512)
                        if (icon != null) {
                            val stream = ByteArrayOutputStream()
                            icon.compress(Bitmap.CompressFormat.PNG, 100, stream)
                            return WebResourceResponse(
                                "image/png", null, 200, "OK",
                                mapOf("Access-Control-Allow-Origin" to "*"),
                                ByteArrayInputStream(stream.toByteArray())
                            )
                        }
                    }
                }

                return webViewAssetLoader.shouldInterceptRequest(url)
            }

            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                webCanGoBack = view?.canGoBack() == true
                super.doUpdateVisitedHistory(view, url, isReload)
            }
        }

        webView?.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.textZoom = (resources.configuration.fontScale * 100).toInt()
            webViewInterface = WebViewInterface(this@WebUIActivity, this)
            addJavascriptInterface(webViewInterface, "ksu")
            setWebViewClient(webViewClient)
            // WebView is set up and visible; hide the loading indicator. It must
            // not stay stacked behind the transparent WebView or it would show
            // through any page with a transparent background.
            isLoading = false
            webChromeClient = object : WebChromeClient() {
                override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                    if (result == null) return false
                    uiEvent = WebUIEvent.ShowAlert(message.orEmpty(), result)
                    return true
                }

                override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                    if (result == null) return false
                    uiEvent = WebUIEvent.ShowConfirm(message.orEmpty(), result)
                    return true
                }

                override fun onJsPrompt(
                    view: WebView?,
                    url: String?,
                    message: String?,
                    defaultValue: String?,
                    result: JsPromptResult?
                ): Boolean {
                    if (result == null) return false
                    uiEvent = WebUIEvent.ShowPrompt(message.orEmpty(), defaultValue.orEmpty(), result)
                    return true
                }

                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    this@WebUIActivity.filePathCallback?.onReceiveValue(null)
                    this@WebUIActivity.filePathCallback = filePathCallback
                    val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply { type = "*/*" }
                    if (fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE) {
                        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    }
                    try {
                        fileChooserLauncher.launch(intent)
                    } catch (_: ActivityNotFoundException) {
                        filePathCallback?.onReceiveValue(null)
                        this@WebUIActivity.filePathCallback = null
                        return false
                    }
                    return true
                }
            }
            loadUrl("https://mui.kernelsu.org/index.html")
        }
    }

    private fun dismissAlert() {
        (uiEvent as? WebUIEvent.ShowAlert)?.result?.confirm()
        uiEvent = null
    }

    private fun dismissConfirm(confirmed: Boolean) {
        (uiEvent as? WebUIEvent.ShowConfirm)?.result?.let {
            if (confirmed) it.confirm() else it.cancel()
        }
        uiEvent = null
    }

    private fun dismissPrompt(value: String?) {
        (uiEvent as? WebUIEvent.ShowPrompt)?.result?.let {
            if (value != null) it.confirm(value) else it.cancel()
        }
        uiEvent = null
    }

    fun enableInsets(enable: Boolean = true) {
        runOnUiThread {
            if (isInsetsEnabled != enable) {
                isInsetsEnabled = enable
                ViewCompat.requestApplyInsets(container)
            }
        }
    }
}