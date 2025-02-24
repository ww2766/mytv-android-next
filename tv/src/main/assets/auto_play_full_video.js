// 标记是否有用户交互
let userInteracted = false;

// 监听键盘事件
document.addEventListener('keydown', (event) => {
  // 检查事件目标是否为文本框
  const isInputField = event.target.tagName === 'INPUT' || event.target.tagName === 'TEXTAREA';

  // 按下 F 键且不在文本框内时触发播放并全屏
  if ((event.key === 'f' || event.key === 'F') && !isInputField) {
    userInteracted = true;
    if (isYouTubePage()) {
      handleYouTubeVideo();
    } else {
      handleStandardVideo();
    }
  }
});

// 检测是否为 YouTube 页面
function isYouTubePage() {
  return window.location.hostname.includes('youtube.com');
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
            video.addEventListener('onStateChange', (state) => {
              if (state.data === YT.PlayerState.PLAYING) {
                video.getIframe().requestFullscreen(); // 全屏
              }
            });
          } else {
            console.warn('Play and fullscreen blocked: user interaction required.');
          }
        },
      },
    });
  } else {
    console.warn('YouTube iframe not found!');
  }
}

// 处理标准视频
function handleStandardVideo() {
  const video = document.querySelector('video');
  if (video) {
    playAndFullscreen(video);
  } else {
    console.warn('Video element not found!');
  }
}

// 播放并全屏视频
function playAndFullscreen(video) {
  if (userInteracted) {
    // 如果视频未播放，先播放
    if (video.paused) {
      video.play().catch((err) => {
        console.error('Failed to play video:', err);
      });

      // 监听播放事件，播放后全屏
      video.addEventListener('play', () => {
        fullscreenVideo(video);
      }, { once: true }); // 只监听一次
    } else {
      // 如果视频已经在播放，直接全屏
      fullscreenVideo(video);
    }
  } else {
    console.warn('Play and fullscreen blocked: user interaction required.');
  }
}

// 全屏视频元素
function fullscreenVideo(video) {
  if(userInteracted = false)
  {
    Android.clickKeyCodeF();
    fullscreenVideo(video)
    return
  }
  if (video.requestFullscreen) {
    video.requestFullscreen().catch((err) => {
      console.error('Failed to enter fullscreen:', err);
    });
    userInteracted = false;
  } else if (video.webkitRequestFullscreen) { // Safari 支持
    video.webkitRequestFullscreen();
    userInteracted = false;
  } else if (video.mozRequestFullScreen) { // Firefox 支持
    video.mozRequestFullScreen();
    userInteracted = false;
  } else if (video.msRequestFullscreen) { // IE/Edge 支持
    video.msRequestFullscreen();
    userInteracted = false;
  }
}

// 尝试自动播放视频
function tryAutoPlay(video) {
  if (userInteracted) {
    video.play().catch((err) => {
      console.error('Failed to autoplay:', err);
    });
  } else {
    console.warn('Autoplay blocked: user interaction required.');
  }
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
    // 尝试自动播放
    tryAutoPlay(video);

    // 如果视频已经在播放，直接全屏
    if (!video.paused && userInteracted) {
      fullscreenVideo(video);
    }

    // 监听播放事件
    video.addEventListener('play', () => {
      fullscreenVideo(video);
    });
  });

  // 查找 iframe 并递归检测
  const iframes = document.querySelectorAll('iframe');

  iframes.forEach((iframe) => {
    try {
      // 访问 iframe 内部文档
      const iframeDocument = iframe.contentDocument || iframe.contentWindow?.document;

      if (iframeDocument) {
        // 递归检测 iframe 内的视频
        detectAndListenVideo.call(iframeDocument);
      }
    } catch (err) {
      // 跨域 iframe 无法访问
      console.warn('Cannot access iframe due to cross-origin restrictions:', err);
    }
  });
}

// 循环检测视频元素
function startDetection() {
  setInterval(detectAndListenVideo, 1000); // 每秒检测一次
}

// 启动检测
startDetection();