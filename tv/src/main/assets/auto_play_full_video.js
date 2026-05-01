
console.log('Plugin enter.');
;(async () => {
    console.log('Plugin enter.');
    let zIdx=1000000000;
    // 标记是否有用户交互
    let userInteracted = false;
    //let window.top.foundVideo=null;
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
      style.innerText = ` .fullscreen-webview {
                            position: fixed !important;
                            top: 0 !important;
                            left: 0 !important;
                            width: 100vw !important;
                            height: 100vh !important;
                            margin: 0 !important;
                            padding: 0 !important;
                            z-index: 2147483646 !important;
                            background-color: black !important;
                            display:block !important;
                          }
                          .hide-webview {
                            display:none;
                          }
                          .no-scroll-webview { overflow: hidden !important; }
                          * {
                            transform: none !important;
                            transform-origin: unset !important;
                            transition: none !important;
                            animation: none !important;
                          }
                          `;
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
      console.warn('handleKeyDown() start');
      userInteracted = true;
      searchVideo();
      return;
    }
   // 处理标准视频
   function searchVideo() {
      if(window.top.foundVideo)return;
      console.warn('searchVideo start');
      const video = document.querySelector('video');
      if (video) {
        enterInlineFullScreen(video);
      } else {
        console.warn('Video element not found!');
      }
      //setVideoResolution(video);
      console.warn('searchVideo end');
      setTimeout(function() {
        searchVideo(); // 传递新的参数给函数
      }, 500); // 延迟1秒（1000毫秒）后再次调用
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
    function isPlaying(video){
      return !video.paused && !video.ended && video.readyState > 2;
    }

    // 视频播放
    function autoPlay(video){
      //console.warn('autoPlay() start');
      if (video.paused){
          //console.log('autoPlay() 未播放');

          if(video.readyState == 4){
            //已缓冲好，可以播放
            console.log("then 3");

            if (video.play() instanceof Promise) {
              video.play().then(() => {
                console.log('autoPlay() 播放成功');
                enterInlineFullScreen(video);
              }).catch((err) => {
                console.error('autoPlay() Failed to play video:', err);
                setTimeout(function() {
                  autoPlay(video); // 传递新的参数给函数
                }, 50); // 延迟1秒（1000毫秒）后再次调用
                return;
              });
            } else {
              // 对于不支持 Promise 的浏览器，直接调用 play() 并监听事件
              video.play();
              video.addEventListener('play', () => {
                console.log('autoPlay() 播放成功');
                enterInlineFullScreen(video);
              });
              video.addEventListener('error', () => {
                console.error('autoPlay() Failed to play video:', err);
                setTimeout(function() {
                  autoPlay(video); // 传递新的参数给函数
                }, 50); // 延迟1秒（1000毫秒）后再次调用
                return;
              });
          }


          }else{
            //没缓冲好，继续等待
            //console.log('autoPlay() loop');
              setTimeout(function() {
                autoPlay(video); // 传递新的参数给函数
              }, 500); // 延迟1秒（1000毫秒）后再次调用
              return;
          }
      }else{
        console.log('已播放');
        enterInlineFullScreen(video);
      }
      //console.warn('autoPlay() end');
    }
    // 网页内全屏
    function enterInlineFullScreen(video) {
      window.top.foundVideo=video;
      console.warn('enterInlineFullScreen() start');
      console.log(video);
      if (video.paused){
        autoPlay(video);
        return;
      }
      if(isInlineFullScreen===true){
        return;
      }
      let currentNode = video;

      while (currentNode) {

        try {
          // 清除内联样式和类
          //currentNode.style.cssText = '';
          //currentNode.className = '';
          if (currentNode.classList) {
            currentNode.classList.add('fullscreen-webview');
          }
          currentNode.style = `z-index:${++zIdx} !important;
                            position: fixed !important;
                            top: 0 !important;
                            left: 0 !important;
                            width: 100% !important;
                            height: 100% !important;
                            margin: 0 !important;
                            padding: 0 !important;
                            background-color: black !important;
                            display:block !important;
                            object-fit: contain !important;`;
        } catch (error) {
          console.error(currentNode)
          console.error(error)
        }
        if(currentNode.parentNode === null && currentNode.host)
        {
          console.log(currentNode.host);
          currentNode = currentNode.host;
        }else {
          currentNode = currentNode.parentNode;
        }

      }
      isInlineFullScreen = true; // 已经全屏
      if(window.parent){
        window.parent.postMessage(
          { action: 'iframeEnterFullscreen' },
        );
      }
      setVideoResolution(video);
      loopFunction(video);
      console.log(video);
      console.warn('enterInlineFullScreen() end');
    }
    function loopFunction(video) {
      console.log("loopFunction() start");
      if(isPlaying(video)&&video.muted===false&&video.volume===1)
      {
        return;
      }
      setTimeout(function() {
        loopFunction(video); // 传递新的参数给函数
      }, 5000); // 延迟1秒（1000毫秒）后再次调用
      if(video.muted){
          video.muted=false;
          console.log(' loopFunction(video) video.muted=false;.');
      }
      if(video.volume < 1){
          console.log(video.volume);
          video.volume = 1;
          console.log(' loopFunction(video) video.volume =1;.');
      }
      setTimeout(() => {
          if (video.paused && video.readyState == 4){
            console.log("then 4");
            video.play().then(() => console.log('播放成功')).catch((err) => {
                console.error(' loopFunction() Failed to play video:', err);
              });
          }else{

          }
      }, 100);
      console.log("loopFunction() end");
    }
    function detectAndListenVideo(depth = 0) {
      console.warn('detectAndListenVideo() start');
      if (depth >= 5) {
        console.warn('Maximum recursion depth reached.');
        return;
      }

      // 检测当前文档中的视频
      const videos = this.querySelectorAll('video');
      videos.forEach((video) => {
        if(!video.paused){
          enterInlineFullScreen(video);
        }
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

      // 检测现有 Shadow Root（关键点）
      this.querySelectorAll('*').forEach(element => {
        if (element.shadowRoot) {
          handleShadowRoot(element.shadowRoot);
        }
      });
      console.warn('detectAndListenVideo() end');
    }



    function handleAddedNode(node) {
      //console.warn('handleAddedNode() start');
      if (node.nodeType !== Node.ELEMENT_NODE) return;

      // 检查当前节点是否是 VIDEO 或 IFRAME
      if (node.tagName === 'VIDEO') {
        if (!node.dataset.videoHandled) {
          node.addEventListener('play', () => enterInlineFullScreen(node));
          node.dataset.videoHandled = 'true';
        }
      } else if (node.tagName === 'IFRAME') {
        try {
          const doc = node.contentDocument;
          if (doc) detectAndListenVideo.call(doc);
        } catch (e) {
          console.error('无法访问 IFRAME 内容:', e);
        }
      }
     // 递归处理 Shadow Root（关键点）
      if (node.shadowRoot) {
        handleShadowRoot(node.shadowRoot);
      }
      // 递归处理子节点
      node.childNodes.forEach(child => handleAddedNode(child));
      //console.warn('handleAddedNode() end');
    }
    // 处理 Shadow Root
    function handleShadowRoot(shadowRoot) {
      console.warn('handleShadowRoot() start');
      // 监听 Shadow Root 内部的变化
      const observer = new MutationObserver(mutations => {
        mutations.forEach(mutation => {
          mutation.addedNodes.forEach(node => handleAddedNode(node));
        });
      });
      observer.observe(shadowRoot, { childList: true, subtree: true });

      // 初始检测 Shadow Root 内已有的 VIDEO 和 IFRAME
      shadowRoot.querySelectorAll('video, iframe').forEach(element => {
        handleAddedNode(element);
      });
      console.warn('handleShadowRoot() end');
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
    searchVideo();
  })()            