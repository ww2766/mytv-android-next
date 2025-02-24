package top.yogiczy.mytv.tv.ui.screens.webview

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebSettings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import top.yogiczy.mytv.core.data.network.WebViewUtils.updateWebViewProxy
import top.yogiczy.mytv.core.data.utils.ChannelUtil
import top.yogiczy.mytv.tv.ui.material.Visible
import top.yogiczy.mytv.tv.ui.screens.webview.components.WebViewPlaceholder


@SuppressLint("SetJavaScriptEnabled")
@Composable
fun X5WebViewScreen(
    modifier: Modifier = Modifier,
    urlProvider: () -> String = { "${ChannelUtil.HYBRID_WEB_VIEW_URL_PREFIX}https://tv.cctv.com/live/index.shtml" },
    onVideoResolutionChanged: (width: Int, height: Int) -> Unit = { _, _ -> },
    releaseFocus: ()->Unit={},
) {
    //val url = urlProvider().replace(ChannelUtil.HYBRID_WEB_VIEW_URL_PREFIX, "")
    val url = ChannelUtil.clearHybridPrefixFromUrl(urlProvider())
    val placeholderVisible by remember { mutableStateOf(false) }


    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight()
                .background(Color.Black),
            factory = {
                X5WebView(
                    context = it,
                    onVideoResolutionChanged =onVideoResolutionChanged,
                    releaseFocus=releaseFocus,

                    ).apply {

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
                    settings.blockNetworkImage = true
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
                    isClickable = true
                    isFocusable = true
                    isFocusableInTouchMode = true

                }
            },
            update = { updateWebViewProxy(it.context,url);it.loadUrl(ChannelUtil.clearAllPrefixFromUrl(url));},

        )

        Visible({ placeholderVisible }) { WebViewPlaceholder() }
    }

}



