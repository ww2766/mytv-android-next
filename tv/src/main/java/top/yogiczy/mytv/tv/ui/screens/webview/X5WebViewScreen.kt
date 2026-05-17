package top.yogiczy.mytv.tv.ui.screens.webview

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.graphics.Bitmap
import android.net.Uri
import android.view.KeyEvent
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.viewinterop.AndroidView
import com.tencent.smtt.export.external.extension.interfaces.IX5WebSettingsExtension
import com.tencent.smtt.export.external.interfaces.ConsoleMessage
import com.tencent.smtt.export.external.interfaces.IX5WebChromeClient
import com.tencent.smtt.export.external.interfaces.JsResult
import com.tencent.smtt.export.external.interfaces.WebResourceRequest
import com.tencent.smtt.export.external.interfaces.WebResourceResponse
import com.tencent.smtt.sdk.WebChromeClient
import com.tencent.smtt.sdk.WebSettings
import com.tencent.smtt.sdk.WebView
import com.tencent.smtt.sdk.WebViewClient
import top.yogiczy.mytv.core.data.network.WebViewUtils.updateWebViewProxy
import top.yogiczy.mytv.core.data.utils.ChannelUtil
import top.yogiczy.mytv.core.data.utils.Logger
import java.io.ByteArrayInputStream


@SuppressLint("SetJavaScriptEnabled")
@Composable
fun X5WebViewScreen(
    modifier: Modifier = Modifier,
    urlProvider: () -> String = { "${ChannelUtil.HYBRID_WEB_VIEW_URL_PREFIX}https://tv.cctv.com/live/index.shtml" },
    onVideoResolutionChanged: (width: Int, height: Int) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val log= Logger.create("X5WebViewComponent")
    val jsString = remember {
        try {
            AssetUtil.readStringFromAssets(context, "auto_play_full_video.js")
        } catch (e: Exception) {
            "console.error('Failed to load auto_play_full_video.js');"
        }
    }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var customView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<IX5WebChromeClient.CustomViewCallback?>(null) }
    val params = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusable(false)
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewRef = this
                    settings.javaScriptEnabled = true
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    settings.loadsImagesAutomatically = false
                    settings.blockNetworkImage = true
                    settings.userAgentString =
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36 Edg/126.0.0.0"
                    settings.javaScriptCanOpenWindowsAutomatically = true
                    settings.setSupportZoom(false)
                    settings.displayZoomControls = false
                    settings.builtInZoomControls = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        com.tencent.smtt.sdk.CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    }
                    settings.mediaPlaybackRequiresUserGesture = false
                    settingsExtension?.setPicModel(IX5WebSettingsExtension.PicModel_NoPic)

                    layoutParams = params
                    setOnKeyListener { _, keyCode, event ->
                        if (keyCode == KeyEvent.KEYCODE_F) false // 放行 F 键，供 JS 层触发全屏
                        else true // 拦截其他按键，避免焦点冲突
                    }
                    setOnClickListener { true }
                    setOnDragListener { v, event -> true }
                    setOnTouchListener { v, event -> true }
                    setOnGenericMotionListener { _, motionEvent ->
                        true // 屏蔽鼠标/滚轮事件
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            view?.evaluateJavascript("window.__myPluginInitialized = false;", null)
                        }

                        override fun onReceivedSslError(
                            view: WebView?,
                            handler: com.tencent.smtt.export.external.interfaces.SslErrorHandler?,
                            error: com.tencent.smtt.export.external.interfaces.SslError?
                        ) {
                            handler?.proceed()
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            view?.evaluateJavascript(jsString.trimIndent(), null)
                            super.onPageFinished(view, url)
                        }

                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            request?.url?.let {
                                if (shouldBlockResource(it)) {
                                    return emptyResponse()
                                }
                            }
                            return super.shouldInterceptRequest(view, request)
                        }

                        override fun shouldInterceptRequest(
                            view: WebView?,
                            url: String?
                        ): WebResourceResponse? {
                            url?.let {
                                if (shouldBlockResource(android.net.Uri.parse(it))) {
                                    return emptyResponse()
                                }
                            }
                            return super.shouldInterceptRequest(view, url)
                        }
                        private fun shouldBlockResource(uri: android.net.Uri): Boolean {
                            val path = uri.lastPathSegment ?: return false
                            return path.matches(Regex(".*\\.(jpg|png|webp|gif|bmp|svg)(\\?.*)?$"))
                        }

                        private fun emptyResponse(): WebResourceResponse {
                            // 1x1像素透明GIF的字节数据（43字节）
                            val smallestTransparentGif = byteArrayOf(
                                0x47.toByte(), 0x49.toByte(), 0x46.toByte(), 0x38.toByte(), 0x39.toByte(), 0x61.toByte(), // GIF89a头
                                0x01.toByte(), 0x00.toByte(), 0x01.toByte(), 0x00.toByte(), // 逻辑屏幕宽高1x1
                                0xF0.toByte(),  // Packed Field: 1111 0000 (全局颜色表|颜色表大小2)
                                0x00.toByte(),  // 背景色索引0
                                0x00.toByte(),  // 像素宽高比
                                // 全局颜色表（2个颜色）
                                0x00.toByte(), 0x00.toByte(), 0x00.toByte(), // 颜色0（透明色）
                                0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), // 颜色1（实际不会显示）
                                // 图形控制扩展
                                0x21.toByte(), 0xF9.toByte(), 0x04.toByte(), // 扩展标签
                                0x01.toByte(),  // 标志位（启用透明色）
                                0x00.toByte(), 0x00.toByte(), // 延迟时间
                                0x00.toByte(),  // 透明色索引0
                                0x00.toByte(),  // 块终结符
                                // 图像描述符
                                0x2C.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), // 位置
                                0x01.toByte(), 0x00.toByte(), 0x01.toByte(), 0x00.toByte(), // 图像宽高
                                0x00.toByte(),  // 无局部颜色表
                                // 图像数据
                                0x02.toByte(),  // LZW最小码长
                                0x02.toByte(),  // 数据块长度
                                0x14.toByte(), 0x01.toByte(), // LZW压缩数据（编码透明像素0x00）
                                0x00.toByte(),  // 数据块终结
                                0x3B.toByte()   // 文件结束
                            )

                            return WebResourceResponse(
                                "image/gif",  // 修改MIME类型为图片格式
                                null,         // 图片不需要字符编码
                                ByteArrayInputStream(smallestTransparentGif)
                            )
                        }

                    }

                    webChromeClient = object : WebChromeClient() {

                        override fun onShowCustomView(view: View?, callback: IX5WebChromeClient.CustomViewCallback?) {
                            if (view != null && callback != null) {
                                customView = view
                                customViewCallback = callback
                            } else {
                                customView = null
                                customViewCallback = null
                            }
                        }

                        override fun onHideCustomView() {
                            customViewCallback?.onCustomViewHidden()
                            customView = null
                            customViewCallback = null
                        }

                        override fun onJsAlert(
                            view: WebView?,
                            url: String?,
                            message: String?,
                            result: JsResult?
                        ): Boolean {
                            result?.cancel()
                            return true
                        }
                        override fun onConsoleMessage(msg: ConsoleMessage): Boolean {
                            when (msg.messageLevel()) {
                                ConsoleMessage.MessageLevel.DEBUG, null -> log.d(msg.message())
                                ConsoleMessage.MessageLevel.LOG, ConsoleMessage.MessageLevel.TIP -> log.i(msg.message())
                                ConsoleMessage.MessageLevel.WARNING -> log.w(msg.message())
                                ConsoleMessage.MessageLevel.ERROR -> log.e(msg.message())
                            }
                            return true
                        }
                    }
                    setBackgroundColor(android.graphics.Color.BLACK)
                    addJavascriptInterface(
                        X5MyWebViewInterface(
                            onVideoResolutionChanged = onVideoResolutionChanged,
                            this,
                        ), "AndroidBridge"
                    )
                    customView = null
                    customViewCallback = null
                    updateWebViewProxy(
                        this.context,
                        ChannelUtil.clearHybridPrefixFromUrl(urlProvider())
                    );
                    loadUrl(ChannelUtil.clearAllPrefixFromUrl(urlProvider()))
                }

            },
            update = { webView ->
                if (webView.url != ChannelUtil.clearAllPrefixFromUrl(urlProvider())) {
                    customViewCallback?.let {
                        customViewCallback?.onCustomViewHidden()
                    }
                    customView = null
                    customViewCallback = null
                    updateWebViewProxy(
                        webView.context,
                        ChannelUtil.clearHybridPrefixFromUrl(urlProvider())
                    );
                    webView.loadUrl(ChannelUtil.clearAllPrefixFromUrl(urlProvider()))
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .focusable(false)
        )
        AndroidView(
            factory = { ctx ->
                FrameLayout(ctx).apply {
                    customView?.let {
                        addView(it, layoutParams)
                    }
                }
            },
            update = { frameLayout ->
                if ((customView == null)
                    || ((frameLayout.childCount > 0) && (frameLayout.getChildAt(0) != customView))
                ) {
                    frameLayout.removeAllViews()
                }
                if (customView != null) {
                    if (frameLayout.childCount == 0) {
                        frameLayout.addView(customView, params)
                    }
                    if ((frameLayout.childCount > 0) && (frameLayout.getChildAt(0) == customView)) {
                        println("已存在，不再添加，忽略")
                    }
                }

            },
            modifier = Modifier
                .fillMaxSize()
                .alpha(
                    if (customView == null) {
                        0f
                    } else {
                        1f
                    }
                )
                .focusable(false)
        )
        //WebViewCover()
        DisposableEffect(Unit) {
            onDispose {
                webViewRef?.destroy()
            }
        }
    }
}

class X5MyWebViewInterface(
    private val onVideoResolutionChanged: (width: Int, height: Int) -> Unit = { _, _ -> },
    private val webView: WebView,
) {
    @JavascriptInterface
    fun changeVideoResolution(width: Int, height: Int) {
        Logger.create("X5WebViewComponent").i("changeVideoResolution: ${width}x${height}")
        onVideoResolutionChanged(width, height)
    }

    @JavascriptInterface
    fun clickKeyCodeF() {
        Logger.create("X5WebViewComponent").i("clickKeyCodeF()")
        webView.post {
            webView.requestFocus()
            val downTime = SystemClock.uptimeMillis()
            webView.dispatchKeyEvent(
                KeyEvent(
                    downTime,
                    downTime,
                    KeyEvent.ACTION_DOWN,
                    KeyEvent.KEYCODE_F,
                    0
                )
            )
            webView.dispatchKeyEvent(
                KeyEvent(
                    downTime,
                    SystemClock.uptimeMillis(),
                    KeyEvent.ACTION_UP,
                    KeyEvent.KEYCODE_F,
                    0
                )
            )
            onVideoResolutionChanged(-100, -100)
        }
    }
}