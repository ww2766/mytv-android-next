package top.yogiczy.mytv.tv.ui.screens.webview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentCompositionLocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import top.yogiczy.mytv.core.data.network.WebViewUtils.updateWebViewProxy
import top.yogiczy.mytv.core.data.utils.ChannelUtil
import top.yogiczy.mytv.tv.ui.material.Visible
import top.yogiczy.mytv.tv.ui.screens.webview.components.WebViewPlaceholder
import java.lang.Thread.sleep

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(
    modifier: Modifier = Modifier,
    urlProvider: () -> String = { "${ChannelUtil.HYBRID_WEB_VIEW_URL_PREFIX}https://tv.cctv.com/live/index.shtml" },
    onVideoResolutionChanged: (width: Int, height: Int) -> Unit = { _, _ -> },
) {
    //val url = urlProvider().replace(ChannelUtil.HYBRID_WEB_VIEW_URL_PREFIX, "",true)
    val url = ChannelUtil.clearHybridPrefixFromUrl(urlProvider())
    var urlR by remember { mutableStateOf(url) }
    var placeholderVisible by remember { mutableStateOf(true) }
    var fullScreenView: View? = null
    var fullScreenViewR by remember { mutableStateOf(fullScreenView) }
    var isVideoFullScreen by remember { mutableStateOf(false) }
    var customViewCallback: WebChromeClient.CustomViewCallback? = null
    var customViewCallbackR  by remember { mutableStateOf(customViewCallback) }
    val context = LocalContext.current
    val jsCode = remember {
        try {
            context.assets.open("auto_play_full_video.js").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "console.error('Failed to load auto_play_full_video.js');"
        }
    }

        Box(modifier = modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxHeight()
                    .background(Color.Black),
                factory = {
                    MyWebView(it).apply {
                        webViewClient = MyClient(
                            jsCode = jsCode,
                            onPageStarted = { placeholderVisible = true },
                            onPageFinished = { placeholderVisible = false },
                        )
                        webChromeClient = object : WebChromeClient() {


                            override fun onShowCustomView(
                                view: View?,
                                callback: CustomViewCallback?
                            ) {
                                if (fullScreenViewR != null) {
                                    onHideCustomView()
                                    return
                                }
                                super.onShowCustomView(view, callback)
                                if (view is FrameLayout) {
                                    fullScreenViewR = view
                                    customViewCallbackR = callback
                                    isVideoFullScreen = true
                                }

                            }

                            override fun onHideCustomView() {
                                super.onHideCustomView()
                                if (fullScreenView != null) {
                                    fullScreenViewR = null
                                    customViewCallbackR?.onCustomViewHidden()
                                    isVideoFullScreen = false
                                }
                            }

                            override fun onConsoleMessage(msg: ConsoleMessage): Boolean {
                                when (msg.messageLevel()) {
                                    ConsoleMessage.MessageLevel.DEBUG, null -> Log.i(
                                        "",
                                        msg.message()
                                    )

                                    ConsoleMessage.MessageLevel.LOG, ConsoleMessage.MessageLevel.TIP -> Log.i(
                                        "",
                                        msg.message()
                                    )

                                    ConsoleMessage.MessageLevel.WARNING -> Log.i("", msg.message())
                                    ConsoleMessage.MessageLevel.ERROR -> Log.i("", msg.message())
                                }
                                return true
                            }
                        }
                        setBackgroundColor(Color.Black.toArgb())
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )

                        settings.javaScriptEnabled = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                        settings.loadsImagesAutomatically = true
                        settings.blockNetworkImage = false
                        settings.userAgentString =
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36 Edg/126.0.0.0"
                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                        settings.javaScriptCanOpenWindowsAutomatically = true
                        settings.setSupportZoom(false)
                        settings.displayZoomControls = false
                        settings.builtInZoomControls = false
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        settings.mediaPlaybackRequiresUserGesture = false

                        isHorizontalScrollBarEnabled = false
                        isVerticalScrollBarEnabled = false
                        isClickable = false
                        isFocusable = false
                        isFocusableInTouchMode = false

                        addJavascriptInterface(
                            MyWebViewInterface(
                                onVideoResolutionChanged = onVideoResolutionChanged,
                                this,
                            ), "AndroidBridge"
                        )
                    }

                },
                update = {
                    //isVideoFullScreen = false
                    //customViewCallbackR?.onCustomViewHidden()
                    //fullScreenViewR = null
                    updateWebViewProxy(
                        it.context,
                        url
                    );
                    it.loadUrl(ChannelUtil.clearAllPrefixFromUrl(url));
                },
            )
        }
        if (isVideoFullScreen) {

            Box(modifier = modifier.fillMaxSize()) {
                AndroidView(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxHeight()
                        .background(Color.Black),
                    factory = {
                        fullScreenViewR?.apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )

                            isClickable = false
                            isFocusable = false
                            isFocusableInTouchMode = false
                        }!!
                    })
            }
        }
        Visible({ placeholderVisible }) { WebViewPlaceholder() }
}


class MyClient(
    private val jsCode: String,
    private val onPageStarted: () -> Unit,
    private val onPageFinished: () -> Unit,
) : WebViewClient() {

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        onPageStarted()
        super.onPageStarted(view, url, favicon)
    }

    override fun onPageFinished(view: WebView, url: String) {
        view.evaluateJavascript(jsCode) {
            onPageFinished()
        }
    }
}

class MyWebView(context: Context) : WebView(context) {
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return false
    }
}

class MyWebViewInterface(
    private val onVideoResolutionChanged: (width: Int, height: Int) -> Unit = { _, _ -> },
    private val webView: WebView,
) {
    @JavascriptInterface
    fun changeVideoResolution(width: Int, height: Int) {
        onVideoResolutionChanged(width, height)
    }
    @JavascriptInterface
    public fun clickKeyCodeF() {
        webView.requestFocus()
        val downTime = SystemClock.uptimeMillis()
        webView.dispatchKeyEvent(KeyEvent(downTime, downTime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F, 0))
        sleep(50)
        webView.dispatchKeyEvent(KeyEvent(downTime, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, KeyEvent.KEYCODE_F, 0))

    }
}