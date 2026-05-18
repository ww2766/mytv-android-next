package top.yogiczy.mytv.tv.ui.screens.main.components


import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tencent.smtt.sdk.QbSdk
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yogiczy.mytv.core.data.entities.channel.ChannelGroupList
import top.yogiczy.mytv.core.data.entities.channel.ChannelList
import top.yogiczy.mytv.core.data.entities.epg.Epg
import top.yogiczy.mytv.core.data.entities.epg.EpgList
import top.yogiczy.mytv.core.data.entities.epg.EpgList.Companion.match
import top.yogiczy.mytv.core.data.entities.epg.EpgList.Companion.recentProgramme
import top.yogiczy.mytv.core.data.entities.epg.EpgProgrammeReserveList
import top.yogiczy.mytv.core.data.repositories.epg.EpgRepository
import top.yogiczy.mytv.core.data.repositories.iptv.IptvRepository
import top.yogiczy.mytv.core.data.utils.ChannelUtil
import top.yogiczy.mytv.core.data.utils.Logger
import top.yogiczy.mytv.tv.ui.material.PopupContent
import top.yogiczy.mytv.tv.ui.material.Snackbar
import top.yogiczy.mytv.tv.ui.material.Visible
import top.yogiczy.mytv.tv.ui.material.popupable
import top.yogiczy.mytv.tv.ui.screens.channel.ChannelNumberSelectScreen
import top.yogiczy.mytv.tv.ui.screens.channel.ChannelScreen
import top.yogiczy.mytv.tv.ui.screens.channel.ChannelTempScreen
import top.yogiczy.mytv.tv.ui.screens.channel.rememberChannelNumberSelectState
import top.yogiczy.mytv.tv.ui.screens.channelurl.ChannelUrlScreen
import top.yogiczy.mytv.tv.ui.screens.classicchannel.ClassicChannelScreen
import top.yogiczy.mytv.tv.ui.screens.datetime.DatetimeScreen
import top.yogiczy.mytv.tv.ui.screens.epg.EpgProgrammeProgressScreen
import top.yogiczy.mytv.tv.ui.screens.epg.EpgScreen
import top.yogiczy.mytv.tv.ui.screens.epgreverse.EpgReverseScreen
import top.yogiczy.mytv.tv.ui.screens.monitor.MonitorScreen
import top.yogiczy.mytv.tv.ui.screens.quickop.QuickOpScreen
import top.yogiczy.mytv.tv.ui.screens.settings.SettingsScreen
import top.yogiczy.mytv.tv.ui.screens.settings.SettingsViewModel
import top.yogiczy.mytv.tv.ui.screens.update.UpdateScreen
import top.yogiczy.mytv.tv.ui.screens.videoplayer.VideoPlayerScreen
import top.yogiczy.mytv.tv.ui.screens.videoplayer.rememberVideoPlayerState
import top.yogiczy.mytv.tv.ui.screens.videoplayercontroller.VideoPlayerControllerScreen
import top.yogiczy.mytv.tv.ui.screens.videoplayerdiaplaymode.VideoPlayerDisplayModeScreen
import top.yogiczy.mytv.tv.ui.screens.webview.WebViewComponent
import top.yogiczy.mytv.tv.ui.screens.webview.X5WebViewScreen
import top.yogiczy.mytv.tv.ui.utils.Configs
import top.yogiczy.mytv.tv.ui.utils.captureBackKey
import top.yogiczy.mytv.tv.ui.utils.handleDragGestures
import top.yogiczy.mytv.tv.ui.utils.handleKeyEvents


@Composable
fun MainContent(
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit = {},
    channelGroupListProvider: () -> ChannelGroupList = { ChannelGroupList() },
    filteredChannelGroupListProvider: () -> ChannelGroupList = { ChannelGroupList() },
    epgListProvider: () -> EpgList = { EpgList() },
    settingsViewModel: SettingsViewModel = viewModel(),
) {
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val videoPlayerState =
        rememberVideoPlayerState(defaultDisplayModeProvider = { settingsViewModel.videoPlayerDisplayMode })
    val mainContentState = rememberMainContentState(
        videoPlayerState = videoPlayerState,
        channelGroupListProvider = filteredChannelGroupListProvider,
        epgListProvider = epgListProvider,
    )
    // 预计算收藏频道列表并缓存，避免每次重组时重新 filter
    val favoriteChannelNameList = settingsViewModel.iptvChannelFavoriteList
    val favoriteChannelList = remember(favoriteChannelNameList, filteredChannelGroupListProvider()) {
        ChannelList(filteredChannelGroupListProvider().channelList
            .filter { it.name in favoriteChannelNameList })
    }
    val channelNumberSelectState = rememberChannelNumberSelectState {
        val idx = it.toInt() - 1
        filteredChannelGroupListProvider().channelList.getOrNull(idx)?.let { channel ->
            mainContentState.changeCurrentChannel(channel)
        }
    }

    val handleBackPressed = {
        val hasVisiblePopup = mainContentState.isTempChannelScreenVisible ||
                mainContentState.isChannelScreenVisible ||
                mainContentState.isSettingsScreenVisible ||
                mainContentState.isQuickOpScreenVisible ||
                mainContentState.isEpgScreenVisible ||
                mainContentState.isChannelUrlScreenVisible ||
                mainContentState.isVideoPlayerControllerScreenVisible ||
                mainContentState.isVideoPlayerDisplayModeScreenVisible

        if (hasVisiblePopup) {
            mainContentState.isTempChannelScreenVisible = false
            mainContentState.isChannelScreenVisible = false
            mainContentState.isSettingsScreenVisible = false
            mainContentState.isQuickOpScreenVisible = false
            mainContentState.isEpgScreenVisible = false
            mainContentState.isChannelUrlScreenVisible = false
            mainContentState.isVideoPlayerControllerScreenVisible = false
            mainContentState.isVideoPlayerDisplayModeScreenVisible = false
        } else {
            onBackPressed()
        }
    }

    Box(
        modifier = modifier
            .popupable()
            .captureBackKey { handleBackPressed() }
            .focusable(false)
    ) {
        VideoPlayerScreen(
            state = videoPlayerState,
            showMetadataProvider = { settingsViewModel.debugShowVideoPlayerMetadata },
        )

        Visible({ Configs.sysWebViewMode && ChannelUtil.isHybridWebViewUrl(mainContentState.currentChannel.urlList[mainContentState.currentChannelUrlIdx]) }) {

            QbSdk.forceSysWebView()
            WebViewComponent(
                urlProvider = { 
                    ChannelUtil.appendChannelParam(
                        mainContentState.currentChannel.urlList[mainContentState.currentChannelUrlIdx],
                        mainContentState.currentChannel.name
                    )
                },
                onVideoResolutionChanged = { width, height ->
                    if(width==-100){
                        coroutineScope.launch(Dispatchers.IO) {
                            withContext(Dispatchers.Main) { // 切换回主线程
                                //focusRequester.requestFocus()
                            }
                        }
                    }
                    else {
                        videoPlayerState.metadata = videoPlayerState.metadata.copy(
                            videoWidth = width,
                            videoHeight = height,
                        )
                        mainContentState.isTempChannelScreenVisible = false
                    }
                },
            )
        }
        Visible({ !Configs.sysWebViewMode && ChannelUtil.isHybridWebViewUrl(mainContentState.currentChannel.urlList[mainContentState.currentChannelUrlIdx]) }) {

            QbSdk.unForceSysWebView()

            //QbSdk.
            //mainContentState.isTempChannelScreenVisible = false
            X5WebViewScreen(
                urlProvider = { 
                    ChannelUtil.appendChannelParam(
                        mainContentState.currentChannel.urlList[mainContentState.currentChannelUrlIdx],
                        mainContentState.currentChannel.name
                    )
                },
                onVideoResolutionChanged = { width, height ->
                    if(width==-100){
                        coroutineScope.launch(Dispatchers.IO) {
                            withContext(Dispatchers.Main) {
                                focusRequester.requestFocus()
                            }
                        }
                    }
                    else {
                        videoPlayerState.metadata = videoPlayerState.metadata.copy(
                            videoWidth = width,
                            videoHeight = height,
                        )
                        mainContentState.isTempChannelScreenVisible = false
                    }
                },
            )
        }
    }


    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(0f)
            .popupable()
            .captureBackKey { handleBackPressed() }
            .handleKeyEvents(
                onUp = {
                    if (settingsViewModel.iptvChannelChangeFlip) mainContentState.changeCurrentChannelToNext()
                    else mainContentState.changeCurrentChannelToPrev()
                },
                onDown = {
                    if (settingsViewModel.iptvChannelChangeFlip) mainContentState.changeCurrentChannelToPrev()
                    else mainContentState.changeCurrentChannelToNext()
                },
                onLeft = {
                    if (mainContentState.currentChannel.urlList.size > 1) {
                        mainContentState.changeCurrentChannel(
                            mainContentState.currentChannel,
                            mainContentState.currentChannelUrlIdx - 1,
                        )
                    }
                },
                onRight = {
                    if (mainContentState.currentChannel.urlList.size > 1) {
                        mainContentState.changeCurrentChannel(
                            mainContentState.currentChannel,
                            mainContentState.currentChannelUrlIdx + 1,
                        )
                    }
                },
                onSelect = { mainContentState.isChannelScreenVisible = true },
                onLongSelect = { mainContentState.isQuickOpScreenVisible = true },
                onSettings = { mainContentState.isQuickOpScreenVisible = true },
                onLongLeft = { mainContentState.isEpgScreenVisible = true },
                onLongRight = { mainContentState.isChannelUrlScreenVisible = true },
                onLongDown = { mainContentState.isVideoPlayerControllerScreenVisible = true },
                onNumber = { channelNumberSelectState.input(it) },
            )
            .handleDragGestures(
                onSwipeDown = {
                    if (settingsViewModel.iptvChannelChangeFlip) mainContentState.changeCurrentChannelToNext()
                    else mainContentState.changeCurrentChannelToPrev()
                },
                onSwipeUp = {
                    if (settingsViewModel.iptvChannelChangeFlip) mainContentState.changeCurrentChannelToPrev()
                    else mainContentState.changeCurrentChannelToNext()
                },
                onSwipeRight = {
                    if (mainContentState.currentChannel.urlList.size > 1) {
                        mainContentState.changeCurrentChannel(
                            mainContentState.currentChannel,
                            mainContentState.currentChannelUrlIdx - 1,
                        )
                    }
                },
                onSwipeLeft = {
                    if (mainContentState.currentChannel.urlList.size > 1) {
                        mainContentState.changeCurrentChannel(
                            mainContentState.currentChannel,
                            mainContentState.currentChannelUrlIdx + 1,
                        )
                    }
                },
            )
            .focusRequester(focusRequester)
            .focusable(true)
            .onFocusEvent { focusState ->
            if (!focusState.isFocused) {
                // 失去焦点时可按需重新请求
                }
            }
    ) {
    }

    Visible({ settingsViewModel.uiShowEpgProgrammePermanentProgress }) {
        EpgProgrammeProgressScreen(
            currentEpgProgrammeProvider = {
                mainContentState.currentPlaybackEpgProgramme
                    ?: epgListProvider().recentProgramme(mainContentState.currentChannel)?.now
            },
            videoPlayerCurrentPositionProvider = { videoPlayerState.currentPosition },
        )
    }

    Visible({
        !mainContentState.isTempChannelScreenVisible
                && !mainContentState.isChannelScreenVisible
                && !mainContentState.isSettingsScreenVisible
                && !mainContentState.isQuickOpScreenVisible
                && !mainContentState.isEpgScreenVisible
                && !mainContentState.isChannelUrlScreenVisible
                && channelNumberSelectState.channelNumber.isEmpty()
    }) {
        DatetimeScreen(showModeProvider = { settingsViewModel.uiTimeShowMode })
    }

    ChannelNumberSelectScreen(channelNumberProvider = { channelNumberSelectState.channelNumber })

    Visible({
        mainContentState.isTempChannelScreenVisible
                && !mainContentState.isChannelScreenVisible
                && !mainContentState.isSettingsScreenVisible
                && !mainContentState.isQuickOpScreenVisible
                && !mainContentState.isEpgScreenVisible
                && !mainContentState.isChannelUrlScreenVisible
                && channelNumberSelectState.channelNumber.isEmpty()
    }) {
        ChannelTempScreen(
            channelProvider = { mainContentState.currentChannel },
            channelUrlIdxProvider = { mainContentState.currentChannelUrlIdx },
            channelNumberProvider = { filteredChannelGroupListProvider().channelIdx(mainContentState.currentChannel) + 1 },
            showChannelLogoProvider = { settingsViewModel.uiShowChannelLogo },
            recentEpgProgrammeProvider = {
                epgListProvider().recentProgramme(mainContentState.currentChannel)
            },
            currentPlaybackEpgProgrammeProvider = { mainContentState.currentPlaybackEpgProgramme },
            videoPlayerMetadataProvider = { videoPlayerState.metadata },
        )
    }

    PopupContent(
        visibleProvider = { mainContentState.isEpgScreenVisible },
        onDismissRequest = { mainContentState.isEpgScreenVisible = false },
    ) {
        EpgScreen(
            epgProvider = {
                epgListProvider().match(mainContentState.currentChannel)
                    ?: Epg.empty(mainContentState.currentChannel)
            },
            epgProgrammeReserveListProvider = {
                EpgProgrammeReserveList(settingsViewModel.epgChannelReserveList.filter {
                    it.channel == mainContentState.currentChannel.name
                })
            },
            supportPlaybackProvider = { mainContentState.supportPlayback() },
            currentPlaybackEpgProgrammeProvider = { mainContentState.currentPlaybackEpgProgramme },
            onEpgProgrammePlayback = {
                mainContentState.isEpgScreenVisible = false
                mainContentState.changeCurrentChannel(
                    mainContentState.currentChannel,
                    mainContentState.currentChannelUrlIdx,
                    it,
                )
            },
            onEpgProgrammeReserve = { programme ->
                mainContentState.reverseEpgProgrammeOrNot(
                    mainContentState.currentChannel,
                    programme
                )
            },
            onClose = { mainContentState.isEpgScreenVisible = false },
        )
    }

    PopupContent(
        visibleProvider = { mainContentState.isChannelUrlScreenVisible },
        onDismissRequest = { mainContentState.isChannelUrlScreenVisible = false },
    ) {
        ChannelUrlScreen(
            channelProvider = { mainContentState.currentChannel },
            currentUrlProvider = { mainContentState.currentChannel.urlList[mainContentState.currentChannelUrlIdx] },
            onUrlSelected = {
                mainContentState.isChannelUrlScreenVisible = false
                mainContentState.changeCurrentChannel(
                    mainContentState.currentChannel,
                    mainContentState.currentChannel.urlList.indexOf(it),
                )
            },
            onClose = { mainContentState.isChannelUrlScreenVisible = false },
        )
    }

    PopupContent(
        visibleProvider = { mainContentState.isVideoPlayerControllerScreenVisible },
        onDismissRequest = { mainContentState.isVideoPlayerControllerScreenVisible = false },
    ) {
        val threshold = 1000L * 60 * 60 * 24 * 365
        val hour0 = -28800000L

        VideoPlayerControllerScreen(
            isVideoPlayerPlayingProvider = { videoPlayerState.isPlaying },
            isVideoPlayerBufferingProvider = { videoPlayerState.isBuffering },
            videoPlayerCurrentPositionProvider = {
                if (videoPlayerState.currentPosition >= threshold) videoPlayerState.currentPosition
                else hour0 + videoPlayerState.currentPosition
            },
            videoPlayerDurationProvider = {
                if (videoPlayerState.currentPosition >= threshold) {
                    val playback = mainContentState.currentPlaybackEpgProgramme

                    if (playback != null) {
                        playback.startAt to playback.endAt
                    } else {
                        val programme =
                            epgListProvider().recentProgramme(mainContentState.currentChannel)?.now
                        (programme?.startAt ?: hour0) to (programme?.endAt ?: hour0)
                    }
                } else {
                    hour0 to (hour0 + videoPlayerState.duration)
                }
            },
            onVideoPlayerPlay = { videoPlayerState.play() },
            onVideoPlayerPause = { videoPlayerState.pause() },
            onVideoPlayerSeekTo = { videoPlayerState.seekTo(it) },
            onClose = { mainContentState.isVideoPlayerControllerScreenVisible = false },
        )
    }

    PopupContent(
        visibleProvider = { mainContentState.isVideoPlayerDisplayModeScreenVisible },
        onDismissRequest = { mainContentState.isVideoPlayerDisplayModeScreenVisible = false },
    ) {
        VideoPlayerDisplayModeScreen(
            currentDisplayModeProvider = { videoPlayerState.displayMode },
            onDisplayModeChanged = { videoPlayerState.displayMode = it },
            onApplyToGlobal = {
                mainContentState.isVideoPlayerDisplayModeScreenVisible = false
                settingsViewModel.videoPlayerDisplayMode = videoPlayerState.displayMode
                Snackbar.show("已应用到全局")
            },
            onClose = { mainContentState.isVideoPlayerDisplayModeScreenVisible = false },
        )
    }

    PopupContent(
        visibleProvider = { mainContentState.isQuickOpScreenVisible },
        onDismissRequest = { mainContentState.isQuickOpScreenVisible = false },
    ) {
        QuickOpScreen(
            currentChannelProvider = { mainContentState.currentChannel },
            currentChannelUrlIdxProvider = { mainContentState.currentChannelUrlIdx },
            currentChannelNumberProvider = {
                (filteredChannelGroupListProvider().channelList.indexOf(mainContentState.currentChannel) + 1).toString()
            },
            showChannelLogoProvider = { settingsViewModel.uiShowChannelLogo },
            epgListProvider = epgListProvider,
            currentPlaybackEpgProgrammeProvider = { mainContentState.currentPlaybackEpgProgramme },
            videoPlayerMetadataProvider = { videoPlayerState.metadata },
            onShowEpg = {
                mainContentState.isQuickOpScreenVisible = false
                mainContentState.isEpgScreenVisible = true
            },
            onShowChannelUrl = {
                mainContentState.isQuickOpScreenVisible = false
                mainContentState.isChannelUrlScreenVisible = true
            },
            onShowVideoPlayerController = {
                mainContentState.isQuickOpScreenVisible = false
                mainContentState.isVideoPlayerControllerScreenVisible = true
            },
            onShowVideoPlayerDisplayMode = {
                mainContentState.isQuickOpScreenVisible = false
                mainContentState.isVideoPlayerDisplayModeScreenVisible = true
            },
            onShowMoreSettings = {
                mainContentState.isQuickOpScreenVisible = false
                mainContentState.isSettingsScreenVisible = true
            },
            onClearCache = {
                settingsViewModel.iptvPlayableHostList = emptySet()
                coroutineScope.launch {
                    IptvRepository(settingsViewModel.iptvSourceCurrent).clearCache()
                    EpgRepository(settingsViewModel.epgSourceCurrent).clearCache()
                    Snackbar.show("缓存已清除，请重启应用")
                }
            },
            onClose = { mainContentState.isQuickOpScreenVisible = false },
        )
    }

    PopupContent(
        visibleProvider = { mainContentState.isChannelScreenVisible && !settingsViewModel.uiUseClassicPanelScreen },
        onDismissRequest = { mainContentState.isChannelScreenVisible = false },
    ) {
        ChannelScreen(
            channelGroupListProvider = filteredChannelGroupListProvider,
            currentChannelProvider = { mainContentState.currentChannel },
            currentChannelUrlIdxProvider = { mainContentState.currentChannelUrlIdx },
            showChannelLogoProvider = { settingsViewModel.uiShowChannelLogo },
            onChannelSelected = {
                mainContentState.isChannelScreenVisible = false
                mainContentState.changeCurrentChannel(it)
            },
            onChannelFavoriteToggle = { mainContentState.favoriteChannelOrNot(it) },
            epgListProvider = epgListProvider,
            showEpgProgrammeProgressProvider = { settingsViewModel.uiShowEpgProgrammeProgress },
            currentPlaybackEpgProgrammeProvider = { mainContentState.currentPlaybackEpgProgramme },
            videoPlayerMetadataProvider = { videoPlayerState.metadata },
            channelFavoriteEnabledProvider = { settingsViewModel.iptvChannelFavoriteEnable },
            channelFavoriteListProvider = { settingsViewModel.iptvChannelFavoriteList.toImmutableList() },
            channelFavoriteListVisibleProvider = { settingsViewModel.iptvChannelFavoriteListVisible },
            onChannelFavoriteListVisibleChange = {
                settingsViewModel.iptvChannelFavoriteListVisible = it
            },
            onClose = { mainContentState.isChannelScreenVisible = false },
        )
    }

    // 经典选台界面：「预热组合」策略
    // 应用就绪后在空闲帧就预先准备好组合树，第一次按键就能立即显示
    var classicScreenEverShown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        launch {
            if (settingsViewModel.uiUseClassicPanelScreen) {
                // 延迟 500ms，让主界面先完成首帧渲染，再在后台预热选台界面
                delay(500)
                classicScreenEverShown = true
            }
        }
        launch {
            snapshotFlow { mainContentState.isChannelScreenVisible }
                .collect { isVisible ->
                    if (isVisible && settingsViewModel.uiUseClassicPanelScreen) {
                        classicScreenEverShown = true
                    }
                }
        }
    }

    if (classicScreenEverShown && settingsViewModel.uiUseClassicPanelScreen) {
        PopupContent(
            visibleProvider = { mainContentState.isChannelScreenVisible },
            onDismissRequest = { mainContentState.isChannelScreenVisible = false },
        ) {
            ClassicChannelScreen(
                channelGroupListProvider = filteredChannelGroupListProvider,
                currentChannelProvider = { mainContentState.currentChannel },
                currentChannelIdxProvider = { mainContentState.currentChannelIdx },
                currentChannelUrlIdxProvider = { mainContentState.currentChannelUrlIdx },
                favoriteChannelListProvider = { favoriteChannelList },
                showChannelLogoProvider = { settingsViewModel.uiShowChannelLogo },
                onChannelSelected = {
                    mainContentState.isChannelScreenVisible = false
                    mainContentState.changeCurrentChannel(it)
                },
                onChannelFavoriteToggle = { mainContentState.favoriteChannelOrNot(it) },
                epgListProvider = epgListProvider,
                epgProgrammeReserveListProvider = {
                    EpgProgrammeReserveList(settingsViewModel.epgChannelReserveList)
                },
                showEpgProgrammeProgressProvider = { settingsViewModel.uiShowEpgProgrammeProgress },
                supportPlaybackProvider = { mainContentState.supportPlayback(it, null) },
                currentPlaybackEpgProgrammeProvider = { mainContentState.currentPlaybackEpgProgramme },
                onEpgProgrammePlayback = { channel, programme ->
                    mainContentState.isChannelScreenVisible = false
                    mainContentState.changeCurrentChannel(channel, null, programme)
                },
                onEpgProgrammeReserve = { channel, programme ->
                    mainContentState.reverseEpgProgrammeOrNot(channel, programme)
                },
                videoPlayerMetadataProvider = { videoPlayerState.metadata },
                channelFavoriteEnabledProvider = { settingsViewModel.iptvChannelFavoriteEnable },
                channelFavoriteListVisibleProvider = { settingsViewModel.iptvChannelFavoriteListVisible },
                onChannelFavoriteListVisibleChange = {
                    settingsViewModel.iptvChannelFavoriteListVisible = it
                },
                onClose = { mainContentState.isChannelScreenVisible = false },
            )
        }
    }

    PopupContent(
        visibleProvider = { mainContentState.isSettingsScreenVisible },
        onDismissRequest = { mainContentState.isSettingsScreenVisible = false },
    ) {
        SettingsScreen(
            channelGroupListProvider = channelGroupListProvider,
            onClose = { mainContentState.isSettingsScreenVisible = false },
        )
    }

    EpgReverseScreen(
        epgProgrammeReserveListProvider = { settingsViewModel.epgChannelReserveList },
        onConfirmReserve = { reserve ->
            filteredChannelGroupListProvider().channelList.firstOrNull { it.name == reserve.channel }
                ?.let {
                    mainContentState.changeCurrentChannel(it)
                }
        },
        onDeleteReserve = { reserve ->
            settingsViewModel.epgChannelReserveList =
                EpgProgrammeReserveList(settingsViewModel.epgChannelReserveList - reserve)
        },
    )

    UpdateScreen()
    Visible({ settingsViewModel.debugShowFps }) { MonitorScreen() }

}