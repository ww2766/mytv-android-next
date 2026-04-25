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
    LocalContext.current.assets.open("auto_play_full_video.js").reader().readText()

        Box(modifier = modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxHeight()
                    .background(Color.Black),
                factory = {
                    MyWebView(it).apply {
                        webViewClient = MyClient(
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
    private val onPageStarted: () -> Unit,
    private val onPageFinished: () -> Unit,
) : WebViewClient() {

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        onPageStarted()
        super.onPageStarted(view, url, favicon)
    }

    override fun onPageFinished(view: WebView, url: String) {
        view.evaluateJavascript(
            """
            
                                console.log('Plugin enter.');
                                ;(async () => {
                                    console.log('Plugin enter.');
                                    // 标记是否有用户交互
                                    let userInteracted = false;
                        
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
                                        event.preventDefault(); // 阻止默认行为
                                        userInteracted = true;
                                        if (isYouTubePage()) {
                                          handleYouTubeVideo();
                                        } else {
                                          handleStandardVideo();
                                        }
                                      }
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
                                      if(userInteracted===false){
                                        //clickKeyCodeF(); 
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
                                      }
                                      if(video.muted){
                                          video.muted=false;
                                      } 
                                      if (video.paused){
                                          video.play();
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