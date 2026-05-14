package top.yogiczy.mytv.tv.ui.screens.webview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.SystemClock
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.webkit.JavascriptInterface
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.interaction.FocusInteraction
import com.tencent.smtt.export.external.extension.proxy.ProxyWebChromeClientExtension
import com.tencent.smtt.export.external.extension.proxy.ProxyWebViewClientExtension
import com.tencent.smtt.export.external.interfaces.ConsoleMessage
import com.tencent.smtt.export.external.interfaces.IX5WebChromeClient
import com.tencent.smtt.export.external.interfaces.JsResult
import com.tencent.smtt.sdk.WebChromeClient
import com.tencent.smtt.sdk.WebSettings
import com.tencent.smtt.sdk.WebView
import com.tencent.smtt.sdk.WebViewClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yogiczy.mytv.core.data.entities.git.GitRelease
import top.yogiczy.mytv.core.data.utils.Logger
import java.lang.Thread.sleep
import kotlin.system.measureTimeMillis


typealias LP = FrameLayout.LayoutParams

@Suppress("unused", "DEPRECATION")
@SuppressLint("SetJavaScriptEnabled")
class X5WebView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    val onVideoResolutionChanged: (width: Int, height: Int) -> Unit = { _, _ -> },
    val releaseFocus: ()->Unit={},
) : WebView(context, attrs, defStyleAttr) {
    val log= Logger.create("X5WebView")
    private val jsCode: String by lazy {
        try {
            context.assets.open("auto_play_full_video.js").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "console.error('Failed to load auto_play_full_video.js');"
        }
    }

    private val client = object : WebViewClient() {

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            log.i( "onPageStarted, $url")
        }

        override fun onPageFinished(view: WebView, url: String) {
            if (url == URL_BLANK)
            {
                super.onPageFinished(view, url)
                isOpenBlankIng=false
                return
            }
            view.evaluateJavascript(jsCode) {
                //onPageFinished(view, url)
            }
            super.onPageFinished(view, url)
            log.i( "onPageFinished, $url")
        }
    }

    private val clientExtension = object : ProxyWebViewClientExtension() {

    }
    private var fullscreenView: View? = null
    //var isFullscreen by remember { mutableStateOf(false) }
    private val chromeClient = object : WebChromeClient() {

        private var callback: IX5WebChromeClient.CustomViewCallback? = null

        override fun onJsAlert(view: WebView, url: String, message: String?, result: JsResult): Boolean {
            result.cancel()
            return true
        }

        override fun onConsoleMessage(msg: ConsoleMessage): Boolean {
            when (msg.messageLevel()) {
                ConsoleMessage.MessageLevel.DEBUG, null -> log.i(msg.message())
                ConsoleMessage.MessageLevel.LOG, ConsoleMessage.MessageLevel.TIP -> log.i( msg.message())
                ConsoleMessage.MessageLevel.WARNING -> log.i(msg.message())
                ConsoleMessage.MessageLevel.ERROR -> log.i( msg.message())
            }
            return true
        }
        private var customViewCallback: IX5WebChromeClient.CustomViewCallback? = null

        override fun onShowCustomView(view: View?, p1: IX5WebChromeClient.CustomViewCallback?) {
            super.onShowCustomView(view, callback)
            log.i("onShowCustomView")
            if (view is FrameLayout) {
                fullscreenView = view
                customViewCallback = callback
                //isFullscreen = true
                //enterFullscreen()

            }
        }

        override fun onHideCustomView() {
            super.onHideCustomView()
            log.i("onHideCustomView")
            if (fullscreenView != null) {
                //isFullscreen = false
                //exitFullscreen()
                fullscreenView = null
                customViewCallback?.onCustomViewHidden()
            }
        }
    }

    private val chromeClientExtension = object : ProxyWebChromeClientExtension() {     }

    init {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            useWideViewPort = true
            loadWithOverviewMode = true
            setAppCacheEnabled(true)
            cacheMode = WebSettings.LOAD_DEFAULT
            databaseEnabled=true
            blockNetworkImage=true
            loadsImagesAutomatically=false
            javaScriptCanOpenWindowsAutomatically=true
        }
        apply {
            webViewClient = client
            webViewClientExtension = clientExtension
            webChromeClient = chromeClient
            webChromeClientExtension = chromeClientExtension
            //setLayerType(View.LAYER_TYPE_HARDWARE, null)
            setBackgroundColor(Color.BLACK)
            addJavascriptInterface(this, "AndroidBridge")
        }
    }
    val URL_BLANK = "about:blank"
    private val CHECK_PAGE_LOADING_INTERVAL = 50L
    private val BLANK_PAGE_WAIT = 800L
    private var isOpenBlankIng=false
    override fun loadUrl(url: String?) {
        if (url==URL_BLANK)
        {
            return
        }
        log.i( "Resetting page...")
        CoroutineScope(Dispatchers.Main).launch {
            val cost = measureTimeMillis {
                isOpenBlankIng=true
                super.loadUrl(URL_BLANK)
                while (isOpenBlankIng) {
                    delay(CHECK_PAGE_LOADING_INTERVAL)
                }
            }
            log.i("Done Resetting, cost ${cost}ms.")
            super.loadUrl(url)
        }

    }

    @JavascriptInterface
    fun clickKeyCodeF() {
        log.i("clickKeyCodeF()")
        this.post {
            this.requestFocus()
            val downTime = SystemClock.uptimeMillis()
            dispatchKeyEvent(
                KeyEvent(
                    downTime,
                    downTime,
                    KeyEvent.ACTION_DOWN,
                    KeyEvent.KEYCODE_F,
                    0
                )
            )
            dispatchKeyEvent(
                KeyEvent(
                    downTime,
                    SystemClock.uptimeMillis(),
                    KeyEvent.ACTION_UP,
                    KeyEvent.KEYCODE_F,
                    0
                )
            )
        }
    }

    @JavascriptInterface
    fun changeVideoResolution(width: Int, height: Int) {
        log.i( "changeVideoResolution()")
        onVideoResolutionChanged(width, height)
    }
}