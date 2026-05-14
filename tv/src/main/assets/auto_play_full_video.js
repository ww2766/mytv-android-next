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
            "  width: 100vw !important;" +
            "  height: 100vh !important;" +
            "  margin: 0 !important;" +
            "  padding: 0 !important;" +
            "  z-index: 2147483646 !important;" +
            "  background-color: black !important;" +
            "  display: block !important;" +
            "}" +
            ".no-scroll-webview { overflow: hidden !important; }";
        document.head.appendChild(style);

        document.addEventListener('keydown', handleKeyDown, true);

        window.addEventListener('message', function(e) {
            var iframe = e.source && e.source.frameElement;
            if (iframe) {
                if (e.data && e.data.action === 'iframeEnterFullscreen') {
                    if (iframe.classList) {
                        iframe.classList.add('fullscreen-webview');
                    } else {
                        iframe.className += ' fullscreen-webview';
                    }
                    if (document.body.classList) {
                        document.body.classList.add('no-scroll-webview');
                    } else {
                        document.body.className += ' no-scroll-webview';
                    }
                    if (window.parent && window.parent !== window) {
                        window.parent.postMessage({ action: 'iframeEnterFullscreen' }, '*');
                    }
                } else if (e.data && e.data.action === 'iframeExitFullscreen') {
                    if (iframe.classList) {
                        iframe.classList.remove('fullscreen-webview');
                    }
                    if (document.body.classList) {
                        document.body.classList.remove('no-scroll-webview');
                    }
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
            if (window.top && window.top.foundVideo) return;
        } catch (e) {}
        var video = document.querySelector('video');
        if (video) {
            enterInlineFullScreen(video);
        }
        setTimeout(searchVideo, 500);
    }

    function setVideoResolution(video) {
        try {
            if (window.AndroidBridge && window.AndroidBridge.changeVideoResolution) {
                if (video && video.videoWidth && video.videoHeight) {
                    window.AndroidBridge.changeVideoResolution(video.videoWidth, video.videoHeight);
                } else {
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

    function enterInlineFullScreen(video) {
        try {
            if (window.top) {
                window.top.foundVideo = video;
            }
        } catch (e) {}
        if (video.paused) {
            // 换源期间不强行 play，等 canplay 事件后自然恢复
            if (!isVideoLoading) {
                safePlay(video, function() { enterInlineFullScreen(video); });
            }
            return;
        }
        if (isInlineFullScreen) {
            return;
        }

        var currentNode = video;
        while (currentNode && currentNode !== document && currentNode !== window) {
            try {
                if (currentNode === video) {
                    if (currentNode.classList) {
                        currentNode.classList.add('fullscreen-webview');
                    } else if (currentNode.className && typeof currentNode.className === 'string' && currentNode.className.indexOf('fullscreen-webview') === -1) {
                        currentNode.className += ' fullscreen-webview';
                    }
                    
                    var css = "z-index: 2147483647 !important; " +
                              "position: fixed !important; " +
                              "top: 0 !important; " +
                              "left: 0 !important; " +
                              "width: 100vw !important; " +
                              "height: 100vh !important; " +
                              "margin: 0 !important; " +
                              "padding: 0 !important; " +
                              "background-color: black !important; " +
                              "display: block !important; " +
                              "box-sizing: border-box !important; " +
                              "object-fit: contain !important;";
                    setStyle(currentNode, css);
                } else {
                    // 对于父元素，绝对不能加 position: fixed 或改变其宽高，
                    // 否则会导致网站自带的播放器（尤其是 CCTV 这类 WASM 播放器）检测到外层容器尺寸坍塌，
                    // 从而触发其内部的“不可见即暂停”逻辑，导致跟我们的 loopFunction 疯狂互抢播放权（引发 AbortError）。
                    // 我们只需要保证父元素不裁剪（overflow: visible），并提升其层级（z-index）即可。
                    // 额外清理 transform 确保 getBoundingClientRect 测量正确。
                    setStyle(currentNode, "overflow: visible !important; z-index: 2147483640 !important; transform: none !important; transition: none !important;");
                }
            } catch (error) {
                console.error("Error setting styles on node", currentNode, error);
            }

            if (currentNode.parentNode === null && currentNode.host) {
                currentNode = currentNode.host;
            } else {
                currentNode = currentNode.parentNode;
            }
        }

        isInlineFullScreen = true;
        
        if (window.parent && window.parent !== window) {
            window.parent.postMessage({ action: 'iframeEnterFullscreen' }, '*');
        }
        
        setVideoResolution(video);
        loopFunction(video);
        console.warn('enterInlineFullScreen() end');
    }

    function loopFunction(video) {
        // 订阅换源事件：loadstart / emptied 说明播放器正在切换流地址
        if (!video.__loopBound) {
            video.__loopBound = true;

            video.addEventListener('loadstart', function() {
                console.log('loopFunction: video loadstart → 进入换源等待');
                isVideoLoading = true;
                isPlayPending   = false; // 释放锁，旧的 promise 已作废
            });

            video.addEventListener('emptied', function() {
                isVideoLoading = true;
                isPlayPending   = false;
            });

            // canplay / playing 说明新源就绪或已开始播放
            video.addEventListener('canplay', function() {
                console.log('loopFunction: canplay → 换源完成，检查是否需要 play');
                isVideoLoading = false;
                // 如果此时仍是暂停状态，才主动干预（部分网站加载完不自动 play）
                if (video.paused) {
                    safePlay(video, function() {
                        // 播放成功后确保全屏流程也跟上
                        if (!isInlineFullScreen) {
                            enterInlineFullScreen(video);
                        }
                    });
                }
            });

            video.addEventListener('playing', function() {
                isVideoLoading = false;
                isPlayPending   = false;
            });

            video.addEventListener('pause', function() {
                if (!isVideoLoading && isInlineFullScreen) {
                    console.log('loopFunction: detected unintended pause, force resume');
                    setTimeout(function() { safePlay(video, null); }, 100);
                }
            });

            // 模拟用户活跃，防止播放器超时进入休眠
            (function activeGuard() {
                if (isInlineFullScreen && !video.paused) {
                    try {
                        // 极致兼容性：使用老旧内核支持的 createEvent 模式
                        var evt = document.createEvent('MouseEvents');
                        evt.initMouseEvent('mousemove', true, true, window, 0, 0, 0, 0, 0, false, false, false, false, 0, null);
                        document.dispatchEvent(evt);
                        console.log('loopFunction: simulated mousemove (legacy mode)');
                    } catch(e) {
                        console.error('activeGuard error', e);
                    }
                }
                setTimeout(activeGuard, 10000);
            })();

            // 音量保活仍需定时检查
            (function volumeGuard() {
                if (video.muted)      video.muted = false;
                if (video.volume < 1) video.volume = 1;

                // 只有在没换源、没锁、且真的卡住了（paused 但 readyState 正常）才主动 play
                if (!isVideoLoading && !isPlayPending && video.paused && video.readyState >= 3) {
                    console.log('loopFunction: video stuck paused, try safePlay');
                    safePlay(video, function() {
                        if (!isInlineFullScreen) {
                            enterInlineFullScreen(video);
                        }
                    });
                }

                setTimeout(volumeGuard, 5000);
            })();
        }
    }

    function handleAddedNode(node) {
        if (node.nodeType !== 1) return; // Node.ELEMENT_NODE

        if (node.tagName === 'VIDEO') {
            if (node.getAttribute('data-video-handled') !== 'true') {
                node.addEventListener('play', function() { enterInlineFullScreen(node); });
                node.setAttribute('data-video-handled', 'true');
                if (!node.paused) {
                    enterInlineFullScreen(node);
                }
            }
        } else if (node.tagName === 'IFRAME') {
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

        var videos = context.querySelectorAll('video');
        for (var i = 0; i < videos.length; i++) {
            var video = videos[i];
            if (video.getAttribute('data-video-handled') !== 'true') {
                video.addEventListener('play', (function(v) {
                    return function() { enterInlineFullScreen(v); };
                })(video));
                video.setAttribute('data-video-handled', 'true');
                if (!video.paused) {
                    enterInlineFullScreen(video);
                }
            }
        }

        var iframes = context.querySelectorAll('iframe');
        if (iframes.length <= 5) {
            for (var i = 0; i < iframes.length; i++) {
                var iframe = iframes[i];
                var handleIframeLoad = (function(ifr) {
                    return function() {
                        try {
                            var iframeDoc = ifr.contentDocument || (ifr.contentWindow && ifr.contentWindow.document);
                            if (iframeDoc) {
                                detectAndListenVideo.call(iframeDoc, depth + 1);
                            }
                        } catch (err) {}
                    };
                })(iframe);

                try {
                    if (iframe.contentDocument && iframe.contentDocument.readyState === 'complete') {
                        handleIframeLoad();
                    } else {
                        iframe.addEventListener('load', handleIframeLoad);
                    }
                } catch(e) {}
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
        console.log('Page loaded, initializing plugin...');
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
        }

        detectAndListenVideo.call(document, 0);
        
        if (window.AndroidBridge && window.AndroidBridge.clickKeyCodeF) {
            try {
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