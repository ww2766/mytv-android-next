package top.yogiczy.mytv.tv.ui.screens.webview

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Build
import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import com.tencent.smtt.export.external.extension.proxy.ProxyWebChromeClientExtension
import com.tencent.smtt.export.external.extension.proxy.ProxyWebViewClientExtension
import com.tencent.smtt.export.external.interfaces.ConsoleMessage
import com.tencent.smtt.export.external.interfaces.IX5WebChromeClient
import com.tencent.smtt.export.external.interfaces.JsResult
import com.tencent.smtt.sdk.WebChromeClient
import com.tencent.smtt.sdk.WebSettings
import com.tencent.smtt.sdk.WebView
import com.tencent.smtt.sdk.WebViewClient
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import top.yogiczy.mytv.core.data.network.WebViewUtils.updateWebViewProxy
import top.yogiczy.mytv.core.data.utils.ChannelUtil
import top.yogiczy.mytv.core.data.utils.Logger
import java.lang.Thread.sleep



@SuppressLint("SetJavaScriptEnabled")
@Composable
fun X5WebViewComponent(
    modifier: Modifier = Modifier,
    urlProvider: () -> String = { "${ChannelUtil.HYBRID_WEB_VIEW_URL_PREFIX}https://tv.cctv.com/live/index.shtml" },
    onVideoResolutionChanged: (width: Int, height: Int) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val log= Logger.create("X5WebViewComponent")
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
            .background(Color.Black),
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
                    //settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    settings.mediaPlaybackRequiresUserGesture = false

                    layoutParams = params
                    setOnKeyListener { _, keyCode, event ->
                        true // 全局按键拦截
                    }
                    setOnClickListener { true }
                    setOnDragListener { v, event -> true }
                    setOnTouchListener { v, event -> true }
                    setOnGenericMotionListener { _, motionEvent ->
                        true // 屏蔽鼠标/滚轮事件
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            view?.evaluateJavascript(
                                """
                                                    
                                                                        console.log('Plugin enter.');
                                                                        ;(async () => {
                                                                            console.log('Plugin enter.');
                                                                            // 标记是否有用户交互
                                                                            let userInteracted = false;
                                                                            // 标记是否网页内全屏
                                                                            let isInnerFull = false;
                                                                
                                                                            // 标记是否已绑定事件监听器
                                                                            let isEventListenerBound = false;
                                                                
                                                                            // 初始化插件
                                                                            function initPlugin() {
                                                                              if (isEventListenerBound) {
                                                                                return; // 如果已绑定，则不再重复绑定
                                                                              }
                                                                
                                                                              // 监听键盘事件
                                                                              document.addEventListener('keydown', handleKeyDown, true); // 使用捕获阶段
                                                                
                                                                              // 标记为已绑定
                                                                              isEventListenerBound = true;
                                                                
                                                                              console.log('Plugin initialized.');
                                                                            }
                                                                
                                                                            // 处理键盘事件
                                                                            function handleKeyDown(event) { 
                                                                              userInteracted = true;
                                                                              // 检查事件目标是否为文本框
                                                                              const isInputField = event.target.tagName === 'INPUT' || event.target.tagName === 'TEXTAREA';
                                                                
                                                                              // 按下 F 键且不在文本框内时触发播放并网页内全屏
                                                                              if ((event.key === 'f' || event.key === 'F') && !isInputField) {
                                                                                console.warn('handleKeyDown:'+event.key);
                                                                                
                                                                                userInteracted = true;
                                                                                if (isYouTubePage()) {
                                                                                  handleYouTubeVideo();
                                                                                } else {
                                                                                  handleStandardVideo();
                                                                                }
                                                                              }
                                                                              event.preventDefault(); // 阻止默认行为
                                                                            }
                                                                
                                                                            // 检测是否为 YouTube 页面
                                                                            function isYouTubePage() {
                                                                              //return window.location.hostname.includes('youtube.com');
                                                                              return false;
                                                                            }
                                                                
                                                                            // 处理 YouTube 视频
                                                                            function handleYouTubeVideo() {
                                                                              const iframe = document.querySelector('iframe');
                                                                              if (iframe && iframe.src.includes('youtube.com/embed')) {
                                                                                const player = new YT.Player(iframe, {
                                                                                  events: {
                                                                                    onReady: (event) => {
                                                                                      const video = event.target;
                                                                                      if (userInteracted) {
                                                                                        video.playVideo(); // 播放视频
                                                                                        enterInlineFullscreen(video.getIframe()); // 网页内全屏
                                                                                      } else {
                                                                                        console.warn('Play and fullscreen blocked: user interaction required.');
                                                                                        clickKeyCodeF(); 
                                                                                      }
                                                                                    },
                                                                                  },
                                                                                });
                                                                              } else {
                                                                                console.warn('YouTube iframe not found!');
                                                                              }
                                                                            }
                                                                            function clickKeyCodeF()
                                                                            {
                                                                               if (userInteracted) {
                                                                                    return;
                                                                                }
                                                                                try{window.AndroidBridge.clickKeyCodeF(); }catch(ex){}
                                                                                
                                                                            }
                                                                            function setVideoResolution(video)
                                                                            { 
                                                                                try{
                                                                                    if (video) { 
                                                                                        window.AndroidBridge.changeVideoResolution(video.videoWidth, video.videoHeight);
                                                                                    }else{
                                                                                        window.AndroidBridge.changeVideoResolution(1280, 720);
                                                                                    }
                                                                                }catch(ex){}
                                                                                
                                                                            }
                                                                            // 处理标准视频
                                                                            function handleStandardVideo() {
                                                                              console.warn('playAndInlineFullscreen');
                                                                              const video = document.querySelector('video');
                                                                              if (video) {
                                                                                playAndInlineFullscreen(video);
                                                                              } else {
                                                                                console.warn('Video element not found!');
                                                                                setVideoResolution(video);
                                                                              }
                                                                            }
                                                                
                                                                            // 播放并网页内全屏视频
                                                                            function playAndInlineFullscreen(video) {
                                                                              console.warn('playAndInlineFullscreen()');
                                                                              if (userInteracted) {
                                                                                // 如果视频未播放，先播放
                                                                                if (video.paused) {
                                                                                  video.play().catch((err) => {
                                                                                    console.error('Failed to play video:', err);
                                                                                  });
                                                                
                                                                                  // 监听播放事件，播放后网页内全屏
                                                                                  video.addEventListener('play', () => {
                                                                                    enterInlineFullscreen(video);
                                                                                  }, { once: true }); // 只监听一次
                                                                                } else {
                                                                                  // 如果视频已经在播放，直接网页内全屏
                                                                                  enterInlineFullscreen(video);
                                                                                }
                                                                              } else {
                                                                                console.warn('Play and fullscreen blocked: user interaction required.');
                                                                                clickKeyCodeF();  
                                                                              }
                                                                            }
                                                                
                                                                            // 网页内全屏
                                                                            function enterInlineFullscreen(video) {
                                                                              setVideoResolution(video);
                                                                              let currentNode = video;
                                                                              while (currentNode) { 
                                                                                try {
                                                                                  currentNode.style.position = 'fixed';
                                                                                  currentNode.style.left = '0';
                                                                                  currentNode.style.top = '0';
                                                                                  currentNode.style.margin = 0;
                                                                                  currentNode.style.padding = 0;
                                                                                  currentNode.style.zIndex = 2147483646; // 确保视频在最上层 
                                                                                  currentNode.style.width = '100%';
                                                                                  currentNode.style.height = '100%'; 
                                                                                  currentNode.style.objectFit = 'cover'; // 确保视频内容适应容器
                                                                                } catch (error) {
                                                                                  
                                                                                }
                                                                               
                                                                                currentNode = currentNode.parentNode;
                                                                              }
                                                                              isInnerFull=true 
                                                          
                                                                              if(userInteracted===false){
                                                                                //clickKeyCodeF(); 
                                                                                return;
                                                                              } 
                                                                             
                                                                              video.muted=false;
                                                                              video.volume =1
                                                                              if (video.paused || video.canplay) {
                                                                                  video.play();
                                                                                }
                                                                              if (video.requestFullscreen) {
                                                                                video.requestFullscreen().catch((err) => {
                                                                                  console.error('Failed to enter fullscreen:', err);
                                                                                });
                                                                              } else if (video.webkitRequestFullscreen) { // Safari 支持
                                                                                video.webkitRequestFullscreen();
                                                                              } else if (video.mozRequestFullScreen) { // Firefox 支持
                                                                                video.mozRequestFullScreen();
                                                                              } else if (video.msRequestFullscreen) { // IE/Edge 支持
                                                                                video.msRequestFullscreen();
                                                                              }

                                                                              userInteracted = false; // 重置用户交互标志
                                                                              console.log('Entered inline fullscreen mode.');
                                                                            }
                                                                
                                                                            // 网页内全屏
                                                                            function enterInlineFullscreen1(video) {
                                                                              
                                                                              setVideoResolution(video);
                                                                              if(userInteracted===false){
                                                                                //clickKeyCodeF(); 
                                                                                return;
                                                                              } 
                                                                              /*document.body.innerHTML = '';*/
                                                                              const div = document.createElement('div');
                                                                              div.appendChild(video); 
                                                                              let firstChild = document.body.firstChild
                                                                              document.body.insertBefore(div, firstChild)
                                                                              
                                                                              div.style.position = 'fixed';
                                                                              div.style.width = '100%';
                                                                              div.style.height = '100%';
                                                                              div.style.margin = '0';
                                                                              div.style.padding = '0';
                                                                              div.style.zIndex = '2147483646'; // 确保视频在最上层 
                                                                              video.style.width = '100%';
                                                                              video.style.height = '100%'; 
                                                                              video.style.objectFit = 'cover'; // 确保视频内容适应容器
                                        
                                        
                                                                              video.muted=false;
                                                                              video.volume =1
                                                                              video.play();
                                                                              userInteracted = false; // 重置用户交互标志
                                                                              console.log('Entered inline fullscreen mode.');
                                                                            }
                                                                            // 检测并监听视频元素
                                                                            function detectAndListenVideo() {
                                                                              if (isYouTubePage()) {
                                                                                // YouTube 页面不需要循环检测
                                                                                return;
                                                                              }
                                                                
                                                                              // 查找当前文档中的视频元素
                                                                              const videos = document.querySelectorAll('video');
                                                                
                                                                              videos.forEach((video) => {
                                                                                // 如果视频已经在播放，直接网页内全屏
                                                                                if (!video.paused && userInteracted) {
                                                                                  enterInlineFullscreen(video);
                                                                                }
                                                                
                                                                                // 监听播放事件
                                                                                video.addEventListener('play', () => {
                                                                                  enterInlineFullscreen(video);
                                                                                });
                                                                              });
                                                                
                                                                              // 查找 iframe 并递归检测（限制深度）
                                                                              const iframes = document.querySelectorAll('iframe');
                                                                              if (iframes.length > 5) { // 限制 iframe 检测数量
                                                                                console.warn('Too many iframes, skipping recursive detection.');
                                                                                return;
                                                                              }
                                                                
                                                                              iframes.forEach((iframe) => {
                                                                                try {
                                                                                  // 访问 iframe 内部文档
                                                                                  const iframeDocument = iframe.contentDocument || iframe.contentWindow?.document;
                                                                
                                                                                  if (iframeDocument) {
                                                                                    // 递归检测 iframe 内的视频（限制递归深度）
                                                                                    if (arguments.length < 3) { // 限制递归深度为 3
                                                                                      detectAndListenVideo.call(iframeDocument, iframeDocument);
                                                                                    } else {
                                                                                      console.warn('Too deep iframe recursion, skipping.');
                                                                                    }
                                                                                  }
                                                                                } catch (err) {
                                                                                  // 跨域 iframe 无法访问
                                                                                  console.warn('Cannot access iframe due to cross-origin restrictions:', err);
                                                                                }
                                                                              });
                                                                            }
                                                                
                                                                
                                                                            // 在页面加载完成后初始化插件
                                                                            function onPageLoad() {
                                                                              console.log('Page loaded, initializing plugin...');
                                                                              initPlugin();
                                                                              clickKeyCodeF();
                                                                              // 创建 Mutation Observer 实例
                                                                              const observer = new MutationObserver(function(mutations) {
                                                                                mutations.forEach(function(mutation) {
                                                                                  // 检查是否有节点被添加
                                                                                  if (mutation.type === 'childList' && mutation.addedNodes.length > 0) {
                                                                                    mutation.addedNodes.forEach(function(node) {
                                                                                      // 如果添加的节点是视频元素或 iframe，则进行处理
                                                                                      if (node.tagName === 'VIDEO' || (node.tagName === 'IFRAME' && node.contentDocument)) {
                                                                                        detectAndListenVideo.call(node.contentDocument || node);
                                                                                      }
                                                                                    });
                                                                                  }
                                                                                });
                                                                              });
                                                                            
                                                                              // 配置 Mutation Observer
                                                                              const config = { childList: true, subtree: true };
                                                                            
                                                                              // 开始监听目标元素
                                                                              observer.observe(document.body, config);
                                                                            
                                                                              // 初次检测页面中的视频元素
                                                                              detectAndListenVideo(document);
                                                                            }
                                                                            
                                                                            console.warn('监听页面加载事件');
                                                                            // 监听页面加载事件
                                                                            if (document.readyState === 'loading') {
                                                                              // 如果页面仍在加载，等待 DOMContentLoaded 事件
                                                                              document.addEventListener('DOMContentLoaded', onPageLoad);
                                                                            } else {
                                                                              // 如果页面已加载，直接初始化插件
                                                                              onPageLoad();
                                                                            }
                                                                            console.warn('完成监听页面加载事件');
                                                                          })() 
                                                """.trimIndent()
                            ) {
                                //onPageFinished()
                            }
                            super.onPageFinished(view, url)
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
                                ConsoleMessage.MessageLevel.DEBUG, null -> log.i(msg.message())
                                ConsoleMessage.MessageLevel.LOG, ConsoleMessage.MessageLevel.TIP -> log.i( msg.message())
                                ConsoleMessage.MessageLevel.WARNING -> log.i(msg.message())
                                ConsoleMessage.MessageLevel.ERROR -> log.i( msg.message())
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
                .fillMaxSize().alpha(
                    if (customView == null) {
                        0f
                    } else {
                        1f
                    }
                ).focusable(false)
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
        onVideoResolutionChanged(width, height)
    }

    @JavascriptInterface
    public fun clickKeyCodeF() {
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
        sleep(50)
        webView.dispatchKeyEvent(
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