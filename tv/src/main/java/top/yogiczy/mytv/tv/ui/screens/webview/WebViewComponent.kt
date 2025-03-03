package top.yogiczy.mytv.tv.ui.screens.webview

import android.annotation.SuppressLint
import android.content.pm.PackageInfo
import android.graphics.Bitmap
import android.os.Build
import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import top.yogiczy.mytv.core.data.network.WebViewUtils.updateWebViewProxy
import top.yogiczy.mytv.core.data.utils.ChannelUtil
import top.yogiczy.mytv.core.data.utils.Logger
import java.lang.Thread.sleep


@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewComponent(
    modifier: Modifier = Modifier,
    urlProvider: () -> String = { "${ChannelUtil.HYBRID_WEB_VIEW_URL_PREFIX}https://tv.cctv.com/live/index.shtml" },
    onVideoResolutionChanged: (width: Int, height: Int) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val log= Logger.create("WebViewComponent")
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var customView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    val params = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
    )
    // 针对所有 WebView（推荐在 Application 类中初始化）
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        WebView.setWebContentsDebuggingEnabled(true);
    }

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
                    settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                    settings.loadsImagesAutomatically = false
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

        // 标记是否已绑定事件监听器
        let isEventListenerBound = false;

        //标记是否已页面全屏
        let isInlineFullScreen =false;

        // 初始化插件
        function initPlugin() {
          if (isEventListenerBound) {
            return; // 如果已绑定，则不再重复绑定
          }
          var style = document.createElement('style');
          style.type = 'text/css';
          style.innerText = `.fullscreen-webview {
                                position: fixed !important;
                                top: 0 !important;
                                left: 0 !important;
                                width: 100vw !important;
                                height: 100vh !important;
                                z-index: 2147483646 !important;
                                background-color: black !important;
                              }
                              .no-scroll-webview { overflow: hidden !important; }`;
          document.head.appendChild(style);
          // 监听键盘事件
          document.addEventListener('keydown', handleKeyDown, true); // 使用捕获阶段
          window.addEventListener('message', (e) => {
            const iframe = e.source.frameElement;  
            if (iframe && (e.data.action === 'iframeEnterFullscreen')) {
              iframe.classList.add('fullscreen-webview');
              document.body.classList.add('no-scroll-webview');
              if(window.parent){
                window.parent.postMessage(
                  { action: 'iframeEnterFullscreen' }, 
                );
              }
            } else if (e.data.action === 'iframeExitFullscreen') {
              iframe.classList.remove('fullscreen-webview');
              document.body.classList.remove('no-scroll-webview');
            }
          });
          // 标记为已绑定
          isEventListenerBound = true;
          
          console.log('Plugin initialized.');
        }

        // 处理键盘事件
        function handleKeyDown(event) { 
          // 检查事件目标是否为文本框
          const isInputField = event.target.tagName === 'INPUT' || event.target.tagName === 'TEXTAREA';

          // 按下 F 键且不在文本框内时触发播放并网页内全屏
          if ((event.key === 'f' || event.key === 'F') && !isInputField) {
            console.warn('handleKeyDown:'+event.key);
            event.preventDefault(); // 阻止默认行为
            userInteracted = true; 
            handleStandardVideo(); 
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
          console.warn('playAndFullScreen');
          const video = document.querySelector('video');
          if (video) {
            playAndFullScreen(video);
          } else {
            console.warn('Video element not found!'); 
          }
          setVideoResolution(video);
        }

        // 播放并网页内全屏视频
        function playAndFullScreen(video) {
          console.warn('playAndFullScreen()');
          if (userInteracted) {
            // 如果视频未播放，先播放
            if (video.paused) {
              video.play().catch((err) => {
                console.error('Failed to play video:', err);
              });

              // 监听播放事件，播放后网页内全屏
              video.addEventListener('play', () => {
                enterFullscreen(video);
              }, { once: true }); // 只监听一次
            } else {
              // 如果视频已经在播放，直接网页内全屏
              enterFullscreen(video);
            }
          } else {
            console.warn('Play and fullscreen blocked: user interaction required.');
            clickKeyCodeF();  
          }
        }
      
          
        // 屏幕全屏
        function enterFullscreen(video) {
          if(userInteracted===false){
            clickKeyCodeF(); 
            setTimeout(() => {  enterFullscreen(video); }, 50); 
            return;
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
          }else{
            console.log('无法全屏，浏览器不允许');
          }
          video.muted=false;
          video.volume =1
          video.play();
          userInteracted = false; // 重置用户交互标志
          console.log('Entered inline fullscreen mode.');
        }

        // 网页内全屏
        function enterInlineFullScreen(video) {
          if(isInlineFullScreen===true){ 
            return;
          }  

          clickKeyCodeF();   
          let currentNode = video;
          while (currentNode) { 
            try {
              // 清除内联样式和类
              currentNode.style.cssText = '';
              currentNode.className = '';
              currentNode.style.position = 'fixed';
              currentNode.style.left = '0';
              currentNode.style.top = '0';
              currentNode.style.margin = 0;
              currentNode.style.padding = 0;
              currentNode.style.zIndex = 2147483646; // 确保视频在最上层 
              currentNode.style.width = '100%';
              currentNode.style.height = '100%'; 
              currentNode.style.objectFit = 'contain'; // 确保视频内容适应容器
              currentNode.classList.add('fullscreen-webview');
            } catch (error) {
              
            }
            
            currentNode = currentNode.parentNode;
          }
          isInnerFull=true 

          //video.muted=false;
          //video.volume =1
          //video.play();
          isInlineFullScreen = true; // 已经全屏 
          if(window.parent){
            window.parent.postMessage(
              { action: 'iframeEnterFullscreen' }, 
            );
          }
          
        } 

        function detectAndListenVideo(depth = 0) {
          if (depth >= 5) {
            console.warn('Maximum recursion depth reached.');
            return;
          }

          // 检测当前文档中的视频
          const videos = this.querySelectorAll('video');
          videos.forEach((video) => { 
            enterInlineFullScreen(video);  
            if (!video.dataset.videoHandled) {
              video.addEventListener('play', () => enterInlineFullScreen(video));
              video.dataset.videoHandled = 'true';
            }
          });

          // 检测 iframe
          const iframes = this.querySelectorAll('iframe');
          if (iframes.length > 5) {
            console.warn('Too many iframes, skipping.');
            return;
          }

          iframes.forEach((iframe) => {
            const handleIframeLoad = () => {
              try {
                const iframeDoc = iframe.contentDocument || iframe.contentWindow?.document;
                if (iframeDoc) {
                  detectAndListenVideo.call(iframeDoc, depth + 1);
                }
              } catch (err) {
                console.warn('Cross-origin iframe blocked:', err);
              }
            };

            if (iframe.contentDocument) {
              handleIframeLoad();
            } else {
              iframe.addEventListener('load', handleIframeLoad);
            }
          });
        }
        


        function handleAddedNode(node) {
          if (node.nodeType !== Node.ELEMENT_NODE) return;
        
          // 检查当前节点是否是 VIDEO 或 IFRAME
          if (node.tagName === 'VIDEO') { 
              enterInlineFullScreen(node); 
          } else if (node.tagName === 'IFRAME') {
            try {
              const doc = node.contentDocument;
              if (doc) detectAndListenVideo.call(doc);
            } catch (e) {
              console.error('无法访问 IFRAME 内容:', e);
            }
          }
        
          // 递归处理子节点
          node.childNodes.forEach(child => handleAddedNode(child));
        }
        // 在页面加载完成后初始化插件
        function onPageLoad() {
          
          console.log('Page loaded, initializing plugin...');
          initPlugin();
        
          // 创建 Mutation Observer 实例
          const observer = new MutationObserver(function(mutations) {
            mutations.forEach(function(mutation) {
              // 检查是否有节点被添加
              if (mutation.type === 'childList' && mutation.addedNodes.length > 0) {
                mutation.addedNodes.forEach(function(node) {
                  //console.log('MutationObserver:'+node.tagName+node.className);
                  handleAddedNode(node); 
                });
              }
            });
          });
        
          // 配置 Mutation Observer
          const config = { childList: true, subtree: true };
        
          // 开始监听目标元素
          observer.observe(document.body, config);
        
          // 初次检测页面中的视频元素
          detectAndListenVideo.call(document);
        }
        console.warn('监听页面加载事件');
        // 监听页面加载事件
        if (document.readyState === 'loading') { 
          // 如果页面仍在加载，等待 DOMContentLoaded 事件
          document.addEventListener('DOMContentLoaded', onPageLoad);
        } else if(document.body!=null) {
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

                        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                            if (view != null && callback != null) {
                                customView = view
                                customViewCallback = callback
                            } else {
                                customView = null
                                customViewCallback = null
                            }
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


                        override fun onJsAlert(
                            view: WebView?,
                            url: String?,
                            message: String?,
                            result: JsResult?
                        ): Boolean {
                            result?.cancel()
                            return true
                        }
                    }
                    setBackgroundColor(android.graphics.Color.BLACK)
                    addJavascriptInterface(
                        WebViewInterface(
                            onVideoResolutionChanged = onVideoResolutionChanged,
                            this
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
        )
        //WebViewCover()
        DisposableEffect(Unit) {
            onDispose {
                webViewRef?.destroy()
            }
        }
    }
}

class WebViewInterface(
private val onVideoResolutionChanged: (width: Int, height: Int) -> Unit = { _, _ -> },
private val webView: WebView,
) {
    @JavascriptInterface
    fun changeVideoResolution(width: Int, height: Int) {
        onVideoResolutionChanged(width, height)
    }

    @JavascriptInterface
    fun clickKeyCodeF() {
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