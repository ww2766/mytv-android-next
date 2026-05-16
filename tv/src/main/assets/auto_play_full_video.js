;(function() {
    if (window.__myPluginInitialized) return;
    window.__myPluginInitialized = true;
    console.log('Plugin enter.');
    var zIdx = 1000000000;
    var userInteracted = false;
    var isEventListenerBound = false;
    var isInlineFullScreen = false;
    var isPlayPending = false; // 播放锁：防止多路 play() 并发导致 AbortError
    var isVideoLoading = false; // 换源锁：播放器正在 load 新源时禁止我们主动 play()

    // 辅助函数：安全地设置样式
    function setStyle(element, cssText) {
        if (element && element.style) {
            element.style.cssText += ';' + cssText;
        }
    }

    // 序列化的安全播放函数，同一时刻只允许一个 play() 调用
    function safePlay(video, callback) {
        if (isPlayPending) {
            return;
        }
        // 换源期间不主动 play，等 canplay 事件驱动
        if (isVideoLoading) {
            console.log('safePlay: video loading, defer to canplay event');
            return;
        }
        if (!video.paused) {
            if (callback) callback();
            return;
        }
        if (video.readyState < 3) {
            console.log('safePlay: readyState too low (' + video.readyState + '), wait...');
            // 不再无限递归，等 canplay 事件来驱动
            return;
        }
        isPlayPending = true;
        var playPromise;
        try {
            // 模拟点击以满足某些网站的交互校验
            try { video.click(); } catch(clickErr) {}
            playPromise = video.play();
        } catch(e) {
            console.error('safePlay: play() threw', e);
            isPlayPending = false;
            setTimeout(function() { safePlay(video, callback); }, 1000);
            return;
        }
        if (playPromise !== undefined && typeof playPromise.then === 'function') {
            playPromise.then(function() {
                isPlayPending = false;
                console.log('safePlay: play succeeded');
                if (callback) callback();
            })['catch'](function(err) {
                isPlayPending = false;
                var errName = err && err.name;
                var errMsg  = err && err.message || '';
                // "interrupted by a new load request" 说明播放器正在换源，
                // 此时重试毫无意义，应等待 canplay 事件后由 loopFunction 接管。
                if (errName === 'AbortError' && errMsg.indexOf('load') !== -1) {
                    console.warn('safePlay: source reloading, will wait for canplay event');
                    isVideoLoading = true; // 标记换源中，loopFunction 不再主动 play
                    // 注册一次性 canplay 监听器，新源就绪后重置标志
                    video.addEventListener('canplay', function onCanPlay() {
                        video.removeEventListener('canplay', onCanPlay);
                        console.log('safePlay: canplay fired, isVideoLoading reset');
                        isVideoLoading = false;
                    });
                    return;
                }
                // "interrupted by a call to pause()" 说明网站调了 pause，退避 1.5s 再试
                var delay = (errName === 'AbortError') ? 1500 : 500;
                console.warn('safePlay: play failed (' + errName + '), retry in ' + delay + 'ms');
                setTimeout(function() { safePlay(video, callback); }, delay);
            });
        } else {
            // 旧版浏览器不返回 Promise
            isPlayPending = false;
            video.addEventListener('play', function onPlay() {
                video.removeEventListener('play', onPlay);
                if (callback) callback();
            });
            setTimeout(function() {
                if (video.paused) {
                    safePlay(video, callback);
                }
            }, 500);
        }
    }

    function initPlugin() {
        if (isEventListenerBound) return;

        var style = document.createElement('style');
        style.type = 'text/css';
        style.innerHTML = 
            ".fullscreen-webview {" +
            "  position: fixed !important;" +
            "  top: 0 !important;" +
            "  left: 0 !important;" +
            "  right: 0 !important;" +
            "  bottom: 0 !important;" +
            "  width: 100vw !important;" +
            "  height: 100vh !important;" +
            "  min-width: 0 !important;" +
            "  min-height: 0 !important;" +
            "  max-width: none !important;" +
            "  max-height: none !important;" +
            "  margin: 0 !important;" +
            "  padding: 0 !important;" +
            "  z-index: 2147483640 !important;" +
            "  background-color: black !important;" +
            "  display: block !important;" +
            "  box-sizing: border-box !important;" +
            "  transform: none !important;" +
            "  filter: none !important;" +
            "  contain: none !important;" +
            "  backdrop-filter: none !important;" +
            "  perspective: none !important;" +
            "  clip-path: none !important;" +
            "  zoom: 1 !important;" +
            "  will-change: auto !important;" +
            "}" +
            ".no-scroll-webview { overflow: hidden !important; }";
        document.head.appendChild(style);

        document.addEventListener('keydown', handleKeyDown, true);

        window.addEventListener('message', function(e) {
            if (!e.data || !e.data.action) return;

            var findIframeByWindow = function(win) {
                var iframes = document.querySelectorAll('iframe');
                for (var i = 0; i < iframes.length; i++) {
                    try {
                        if (iframes[i].contentWindow === win) return iframes[i];
                    } catch (err) {}
                }
                return null;
            };

            var iframe = findIframeByWindow(e.source);
            
            if (e.data.action === 'iframeEnterFullscreen') {
                console.log('Message: 收到 Iframe 全屏请求');
                if (iframe) {
                    console.log('Message: 锁定请求来源 Iframe: ' + (iframe.id || iframe.className));
                    if (iframe.classList) {
                        iframe.classList.add('fullscreen-webview');
                    } else {
                        iframe.className += ' fullscreen-webview';
                    }
                    // 继续向上传递
                    if (window.parent && window.parent !== window) {
                        window.parent.postMessage({ action: 'iframeEnterFullscreen' }, '*');
                    }
                } else {
                    console.warn('Message: 无法在当前层级锁定请求来源 Iframe (可能由于深度嵌套或跨域)');
                    // 即使找不到具体哪一个，也尝试把页面上所有活跃的 iframe 处理一下
                    var allIframes = document.querySelectorAll('iframe');
                    for (var j = 0; j < allIframes.length; j++) {
                        setStyle(allIframes[j], "position: fixed !important; top: 0 !important; left: 0 !important; right: 0 !important; bottom: 0 !important; width: 100vw !important; height: 100vh !important; z-index: 2147483640 !important; background: black !important; min-width: 0 !important; min-height: 0 !important; max-width: none !important; max-height: none !important; box-sizing: border-box !important;");
                    }
                }
                if (document.body && document.body.classList) {
                    document.body.classList.add('no-scroll-webview');
                }
            } else if (e.data.action === 'iframeExitFullscreen') {
                if (iframe && iframe.classList) {
                    iframe.classList.remove('fullscreen-webview');
                }
                if (document.body && document.body.classList) {
                    document.body.classList.remove('no-scroll-webview');
                }
            }
        });

        isEventListenerBound = true;
        console.log('Plugin initialized.');
    }

    function handleKeyDown(event) {
        console.warn('handleKeyDown() start');
        userInteracted = true;
        searchVideo();
    }

    function searchVideo() {
        try {
            if (window.top && window.top.foundVideo) {
                if (window.__searchInterval) clearInterval(window.__searchInterval);
                return;
            }
        } catch (e) {}
        var video = document.querySelector('video');
        if (video) {
            console.log('searchVideo(): 发现 video 元素');
            enterInlineFullScreen(video);
            if (window.__searchInterval) clearInterval(window.__searchInterval);
            return;
        }
        if (!window.__searchInterval) {
            window.__searchInterval = setInterval(searchVideo, 500);
        }
    }

    function setVideoResolution(video) {
        try {
            if (window.AndroidBridge && window.AndroidBridge.changeVideoResolution) {
                if (video && video.videoWidth && video.videoHeight) {
                    console.info('setVideoResolution(): ' + video.videoWidth + 'x' + video.videoHeight);
                    window.AndroidBridge.changeVideoResolution(video.videoWidth, video.videoHeight);
                } else {
                    console.info('setVideoResolution(): 默认 1280x720');
                    window.AndroidBridge.changeVideoResolution(1280, 720);
                }
            }
        } catch (ex) {
            console.error('AndroidBridge error', ex);
        }
    }

    function isPlaying(video) {
        return !!(video.currentTime > 0 && !video.paused && !video.ended && video.readyState > 2);
    }

    function autoPlay(video) {
        if (video.paused) {
            console.info('autoPlay(): 尝试播放 video');
            safePlay(video);
        } else {
            console.log('autoPlay(): video 已经在播放中');
        }
    }

    function enterInlineFullScreen(video) {
        console.warn('enterInlineFullScreen(): 开始处理全屏');
        try {
            if (window.top) {
                window.top.foundVideo = video;
            }
        } catch (e) {}

        if (isInlineFullScreen) {
            console.log('enterInlineFullScreen(): 已经是全屏状态，跳过');
            return;
        }

        console.info('enterInlineFullScreen(): 正在应用 CSS 强制全屏样式, 视频源: ' + video.src);
        
        // 强行修改 viewport 以适配屏幕宽度，解决 width=1400 等硬编码视口导致的裁切问题
        try {
            var meta = document.querySelector('meta[name="viewport"]');
            var content = "width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no";
            if (meta) {
                meta.setAttribute('content', content);
            } else {
                meta = document.createElement('meta');
                meta.name = "viewport";
                meta.content = content;
                document.head.appendChild(meta);
            }
            console.log('enterInlineFullScreen(): Viewport 已重置为 device-width');
            window.dispatchEvent(new Event('resize'));
        } catch (e) {
            console.warn('enterInlineFullScreen(): 重置 Viewport 失败', e);
        }

        var currentNode = video;
        var depth = 0;
        
        while (currentNode && currentNode !== window) {
            if (currentNode === document) {
                break; // 到达主页面根节点，遍历完成
            }

            try {
                if (currentNode.nodeType === 1) { // ELEMENT_NODE
                    var tag = currentNode.tagName || 'UNKNOWN';
                    var id = currentNode.id ? '#' + currentNode.id : '';
                    var cls = currentNode.className && typeof currentNode.className === 'string' ? '.' + currentNode.className.split(' ').join('.') : '';
                    
                    // 检测可能破坏 fixed 定位的属性
                    var win = (currentNode.ownerDocument && currentNode.ownerDocument.defaultView) || window;
                    var style = win.getComputedStyle ? win.getComputedStyle(currentNode) : null;
                    if (style) {
                        var transform = style.getPropertyValue('transform');
                        var filter = style.getPropertyValue('filter');
                        var contain = style.getPropertyValue('contain');
                        if ((transform && transform !== 'none') || (filter && filter !== 'none') || (contain && contain !== 'none')) {
                            console.warn('enterInlineFullScreen(): 警告! 节点 ' + tag + id + cls + ' 含有破坏 fixed 布局的属性: transform=' + transform + ', filter=' + filter + ', contain=' + contain);
                        }
                    }

                    if (currentNode.classList) {
                        currentNode.classList.add('fullscreen-webview');
                    } else if (currentNode.className && typeof currentNode.className === 'string' && currentNode.className.indexOf('fullscreen-webview') === -1) {
                        currentNode.className += ' fullscreen-webview';
                    }
                    
                    var currentZ = (currentNode.tagName === 'VIDEO') ? 1 : (++zIdx);
                    var css = "z-index: " + currentZ + " !important; " +
                              "position: fixed !important; " +
                              "top: 0 !important; " +
                              "left: 0 !important; " +
                              "right: 0 !important; " +
                              "bottom: 0 !important; " +
                              "width: 100vw !important; " +
                              "height: 100vh !important; " +
                              "min-width: 0 !important; " +
                              "min-height: 0 !important; " +
                              "max-width: none !important; " +
                              "max-height: none !important; " +
                              "margin: 0 !important; " +
                              "padding: 0 !important; " +
                              "background-color: black !important; " +
                              "display: block !important; " +
                              "box-sizing: border-box !important; " +
                              "transform: none !important; " +
                              "filter: none !important; " +
                              "contain: none !important; " +
                              "backdrop-filter: none !important; " +
                              "perspective: none !important; " +
                              "clip-path: none !important; " +
                              "zoom: 1 !important; " +
                              "will-change: auto !important;";
                              
                    if (currentNode.tagName === 'VIDEO') {
                        css += " object-fit: contain !important; object-position: center !important;";
                    }
                    
                    setStyle(currentNode, css);
                    console.log('enterInlineFullScreen(): 已处理层级 ' + depth + ': ' + tag + id + cls);
                }
            } catch (error) {
                console.error("Error setting styles on node", currentNode, error);
            }

            depth++;
            var nextNode = currentNode.parentNode;
            
            // 跳出 Iframe 的边界判断
            if (!nextNode) {
                if (currentNode.host) {
                    nextNode = currentNode.host; // Shadow DOM 支持
                } else if (currentNode.defaultView && currentNode.defaultView.frameElement) {
                    nextNode = currentNode.defaultView.frameElement; // 同源 iframe 边界跳跃！
                    console.warn('enterInlineFullScreen(): 成功跨越 Iframe 边界，继续在父页面中向上处理');
                }
            }
            currentNode = nextNode;
        }

        isInlineFullScreen = true;
        
        if (window.parent && window.parent !== window) {
            console.log('enterInlineFullScreen(): 通知跨域父框架进入全屏（兜底方案）');
            window.parent.postMessage({ action: 'iframeEnterFullscreen' }, '*');
        }
        
        setVideoResolution(video);
        loopFunction(video);
        console.warn('enterInlineFullScreen(): 全屏处理完成, 总层数: ' + depth);
    }

    function loopFunction(video) {
        if (!video) return;
        var loopCount = video.__loopCount || 0;
        video.__loopCount = loopCount + 1;
        
        var playing = isPlaying(video);
        if (playing && !video.muted && video.volume === 1) {
            video.__loopCount = 0; // 正常播放时重置计数器
            setTimeout(function() { loopFunction(video); }, 5000);
            return;
        }
        
        console.log('loopFunction(): 检测到播放状态异常: ' +
                    'playing=' + playing + ' (currentTime=' + video.currentTime + ', readyState=' + video.readyState + '), ' +
                    'paused=' + video.paused + ', ' +
                    'muted=' + video.muted + ', ' +
                    'vol=' + video.volume);
        
        // 启动宽限期：给播放器缓冲的时间，防止打断加载
        if (video.readyState < 3 && video.__loopCount < 10) {
            console.log('loopFunction(): video 正在加载中 (readyState=' + video.readyState + ')，等待缓冲');
            setTimeout(function() { loopFunction(video); }, 2000);
            return;
        }

        if (video.muted) {
            video.muted = false;
            console.info('loopFunction(): 取消静音');
        }
        if (video.volume < 1) {
            video.volume = 1;
            console.info('loopFunction(): 恢复最大音量');
        }
        
        if (video.paused) {
            console.info('loopFunction(): 尝试恢复播放');
            safePlay(video);
        }
        
        setTimeout(function() { loopFunction(video); }, 2000);
    }

    function handleAddedNode(node) {
        if (node.nodeType !== 1) return; // Node.ELEMENT_NODE

        if (node.tagName === 'VIDEO') {
            console.log('MutationObserver: 发现新 video 元素');
            if (node.getAttribute('data-video-handled') !== 'true') {
                node.addEventListener('play', function() { 
                    console.log('Event: video 开始播放');
                    enterInlineFullScreen(node); 
                });
                node.setAttribute('data-video-handled', 'true');
                if (!node.paused) {
                    enterInlineFullScreen(node);
                }
            }
        } else if (node.tagName === 'IFRAME') {
            console.log('MutationObserver: 发现新 iframe 元素');
            try {
                var doc = node.contentDocument || (node.contentWindow && node.contentWindow.document);
                if (doc) detectAndListenVideo.call(doc, 0);
            } catch (e) {}
        }

        if (node.shadowRoot) {
            handleShadowRoot(node.shadowRoot);
        }

        var children = node.childNodes;
        for (var i = 0; i < children.length; i++) {
            handleAddedNode(children[i]);
        }
    }

    function handleShadowRoot(shadowRoot) {
        console.log('handleShadowRoot(): 正在处理 Shadow DOM');
        if (typeof MutationObserver !== 'undefined') {
            var observer = new MutationObserver(function(mutations) {
                for (var i = 0; i < mutations.length; i++) {
                    var addedNodes = mutations[i].addedNodes;
                    for (var j = 0; j < addedNodes.length; j++) {
                        handleAddedNode(addedNodes[j]);
                    }
                }
            });
            observer.observe(shadowRoot, { childList: true, subtree: true });
        }

        var videosAndIframes = shadowRoot.querySelectorAll('video, iframe');
        for (var i = 0; i < videosAndIframes.length; i++) {
            handleAddedNode(videosAndIframes[i]);
        }
    }

    function detectAndListenVideo(depth) {
        depth = depth || 0;
        if (depth >= 5) return;

        var context = this === window ? document : this;
        console.log('detectAndListenVideo(): 正在扫描层级 ' + depth);

        var videos = context.querySelectorAll('video');
        for (var i = 0; i < videos.length; i++) {
            var video = videos[i];
            if (video.getAttribute('data-video-handled') !== 'true') {
                console.log('detectAndListenVideo(): 发现未处理 video');
                video.addEventListener('play', (function(v) {
                    return function() { 
                        console.log('Event: video 开始播放 (from scanner)');
                        enterInlineFullScreen(v); 
                    };
                })(video));
                video.setAttribute('data-video-handled', 'true');
                if (!video.paused) {
                    enterInlineFullScreen(video);
                }
            }
        }

        var iframes = context.querySelectorAll('iframe');
        if (iframes.length <= 10) {
            for (var i = 0; i < iframes.length; i++) {
                var iframe = iframes[i];
                var handleIframeLoad = (function(ifr) {
                    return function() {
                        try {
                            var iframeDoc = ifr.contentDocument || (ifr.contentWindow && ifr.contentWindow.document);
                            if (iframeDoc) {
                                console.log('Iframe loaded: 递归扫描内容');
                                detectAndListenVideo.call(iframeDoc, depth + 1);
                            }
                        } catch (err) {
                            console.warn('Iframe load error: 跨域访问受限');
                        }
                    };
                })(iframe);

                try {
                    if (iframe.contentDocument && iframe.contentDocument.readyState === 'complete') {
                        handleIframeLoad();
                    } else {
                        iframe.addEventListener('load', handleIframeLoad);
                    }
                } catch(e) {
                    console.warn('Iframe error: 跨域访问受限');
                }
            }
        }

        var allElements = context.querySelectorAll('*');
        for (var i = 0; i < allElements.length; i++) {
            if (allElements[i].shadowRoot) {
                handleShadowRoot(allElements[i].shadowRoot);
            }
        }
    }

    function onPageLoad() {
        console.info('onPageLoad(): 页面加载完成，初始化插件...');
        initPlugin();

        if (typeof MutationObserver !== 'undefined') {
            var observer = new MutationObserver(function(mutations) {
                for (var i = 0; i < mutations.length; i++) {
                    var addedNodes = mutations[i].addedNodes;
                    for (var j = 0; j < addedNodes.length; j++) {
                        handleAddedNode(addedNodes[j]);
                    }
                }
            });
            observer.observe(document.body || document.documentElement, { childList: true, subtree: true });
            console.log('onPageLoad(): MutationObserver 已启动');
        }

        detectAndListenVideo.call(document, 0);
        
        if (window.AndroidBridge && window.AndroidBridge.clickKeyCodeF) {
            try {
                console.info('onPageLoad(): 触发 AndroidBridge.clickKeyCodeF() 以获取用户交互授权');
                window.AndroidBridge.clickKeyCodeF();
            } catch(e) {}
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', onPageLoad);
    } else {
        onPageLoad();
    }

    searchVideo();

})();