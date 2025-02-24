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
                                    function enterInlineFullscreen1(video) {
                                      if(userInteracted===false){
                                        clickKeyCodeF(); 
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
                                      video.muted=false;
                                      video.volume =1
                                      video.play();
                                      userInteracted = false; // 重置用户交互标志
                                      console.log('Entered inline fullscreen mode.');
                                    }
                        
                                    // 网页内全屏
                                    function enterInlineFullscreen(video) {
                                      
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
        log.i( "clickKeyCodeF()")
        this.requestFocus()
        val downTime = SystemClock.uptimeMillis()
        dispatchKeyEvent(KeyEvent(downTime, downTime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F, 0))
        sleep(50)
        dispatchKeyEvent(KeyEvent(downTime, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, KeyEvent.KEYCODE_F, 0))
        //this.clearFocus()
        //releaseFocus()
    }

    @JavascriptInterface
    fun changeVideoResolution(width: Int, height: Int) {
        log.i( "changeVideoResolution()")
        onVideoResolutionChanged(width, height)
    }
}