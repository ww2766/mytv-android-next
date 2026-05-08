package top.yogiczy.mytv.tv.ui.screens.classicchannel.components

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.DenseListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import top.yogiczy.mytv.core.data.entities.channel.Channel
import top.yogiczy.mytv.core.data.entities.channel.ChannelGroup
import top.yogiczy.mytv.core.data.entities.channel.ChannelList
import top.yogiczy.mytv.core.data.entities.epg.EpgList
import top.yogiczy.mytv.core.data.entities.epg.EpgList.Companion.recentProgramme
import top.yogiczy.mytv.core.data.entities.epg.EpgProgramme.Companion.progress
import top.yogiczy.mytv.core.data.entities.epg.EpgProgrammeRecent
import top.yogiczy.mytv.tv.ui.material.rememberDebounceState
import top.yogiczy.mytv.tv.ui.screens.channel.components.ChannelItemLogo
import top.yogiczy.mytv.tv.ui.screens.settings.LocalSettings
import top.yogiczy.mytv.tv.ui.theme.MyTVTheme
import top.yogiczy.mytv.tv.ui.utils.handleKeyEvents
import top.yogiczy.mytv.tv.ui.utils.ifElse
import top.yogiczy.mytv.tv.ui.utils.saveFocusRestorer
import top.yogiczy.mytv.tv.ui.utils.saveRequestFocus
import kotlin.math.max

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ClassicChannelItemList(
    modifier: Modifier = Modifier,
    channelGroupProvider: () -> ChannelGroup = { ChannelGroup() },
    channelListProvider: () -> ChannelList = { ChannelList() },
    initialChannelProvider: () -> Channel = { Channel() },
    initialChannelIdxProvider: () -> Int = { -1 },
    showChannelLogoProvider: () -> Boolean = { false },
    onChannelSelected: (Channel) -> Unit = {},
    onChannelFavoriteToggle: (Channel) -> Unit = {},
    onChannelFocused: (Channel) -> Unit = { },
    epgListProvider: () -> EpgList = { EpgList() },
    showEpgProgrammeProgressProvider: () -> Boolean = { false },
    inFavoriteModeProvider: () -> Boolean = { false },
    onUserAction: () -> Unit = {},
) {
    val currentOnChannelSelected = rememberUpdatedState(onChannelSelected)
    val currentOnChannelFavoriteToggle = rememberUpdatedState(onChannelFavoriteToggle)
    val currentOnChannelFocused = rememberUpdatedState(onChannelFocused)

    val focusManager = LocalFocusManager.current
    val channelGroup = channelGroupProvider()
    val channelList = channelListProvider()
    val initialChannel = initialChannelProvider()
    
    // 焦点请求器映射表绑定到分类名
    // 1. 确保在当前分类刷新数据时（名字不变），焦点请求器得以复用，不丢焦点
    // 2. 确保在切换分类时（名字变化），旧分类的映射表被及时销毁，防止内存膨胀
    val itemFocusRequesterMap = remember(channelGroup.name) { mutableMapOf<String, FocusRequester>() }
    fun getFocusRequester(channel: Channel) = 
        itemFocusRequesterMap.getOrPut("${channel.name}_${channel.urlList.firstOrNull()}") { FocusRequester() }

    val initialChannelIdx = initialChannelIdxProvider()
    var hasFocused by remember(channelList) { mutableStateOf(initialChannelIdx == -1) }
    var focusedChannel by remember(channelList) {
        mutableStateOf(
            if (hasFocused) channelList.firstOrNull() ?: Channel() else initialChannel
        )
    }

    val focusedChannelIdxState = remember(channelList, focusedChannel) {
        derivedStateOf { channelList.indexOf(focusedChannel) }
    }

    val onChannelFocusedDebounce = rememberDebounceState(wait = 100L) {
        onChannelFocused(focusedChannel)
    }

    val listState = remember(channelGroup) {
        LazyListState(
            if (hasFocused) 0
            else max(0, initialChannelIdx - 2)
        )
    }
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.distinctUntilChanged()
            .collect { _ -> onUserAction() }
    }

    val coroutineScope = rememberCoroutineScope()
    val firstFocusRequester = remember { FocusRequester() }
    val lastFocusRequester = remember { FocusRequester() }
    fun scrollToFirst() {
        coroutineScope.launch {
            listState.scrollToItem(0)
            firstFocusRequester.saveRequestFocus()
        }
    }

    fun scrollToLast() {
        coroutineScope.launch {
            listState.scrollToItem(channelList.lastIndex)
            lastFocusRequester.saveRequestFocus()
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxHeight()
            .width(if (showChannelLogoProvider()) 280.dp else 220.dp)
            .background(MaterialTheme.colorScheme.surface.copy(0.8f))
            .ifElse(
                LocalSettings.current.uiFocusOptimize,
                Modifier.saveFocusRestorer {
                    getFocusRequester(focusedChannel)
                },
            ),
    ) {
        itemsIndexed(channelList, key = { _, channel -> "${channel.name}_${channel.urlList.firstOrNull()}" }) { index, channel ->
            val isSelected by remember { derivedStateOf { channel == focusedChannel } }
            val initialFocused = !hasFocused && channel == initialChannel

            val currentScrollToLast by rememberUpdatedState { scrollToLast() }
            val currentScrollToFirst by rememberUpdatedState { scrollToFirst() }
            val onUp = remember { { currentScrollToLast() } }
            val onDown = remember { { currentScrollToFirst() } }

            val focusRequester = getFocusRequester(channel)

            ClassicChannelItem(
                modifier = Modifier
                    .ifElse(
                        index == 0,
                        Modifier
                            .focusRequester(firstFocusRequester)
                            .handleKeyEvents(onUp = onUp)
                    )
                    .ifElse(
                        index == channelList.lastIndex,
                        Modifier
                            .focusRequester(lastFocusRequester)
                            .handleKeyEvents(onDown = onDown)
                    ),
                channel = channel,
                onChannelSelected = remember(channel) { { currentOnChannelSelected.value(channel) } },
                onChannelFavoriteToggle = remember(channel) {
                    {
                        if (inFavoriteModeProvider()) {
                            if (channelList.size == 1) {
                                focusManager.moveFocus(FocusDirection.Left)
                            } else if (channelList.first() == channel) {
                                focusManager.moveFocus(FocusDirection.Down)
                            } else if (channelList.last() == channel) {
                                focusManager.moveFocus(FocusDirection.Up)
                            } else {
                                focusManager.moveFocus(FocusDirection.Down)
                            }
                        }
                        currentOnChannelFavoriteToggle.value(channel)
                    }
                },
                onChannelFocused = remember(channel) {
                    {
                        focusedChannel = channel
                        onChannelFocusedDebounce.send()
                    }
                },
                epgList = epgListProvider(),
                showEpgProgrammeProgressProvider = showEpgProgrammeProgressProvider,
                focusRequesterProvider = remember(focusRequester) { { focusRequester } },
                initialFocusedProvider = remember(initialFocused) { { initialFocused } },
                onInitialFocused = remember { { hasFocused = true } },
                isSelectedProvider = { isSelected },
                showChannelLogoProvider = showChannelLogoProvider,
            )
        }
    }
}

@Composable
private fun ClassicChannelItem(
    modifier: Modifier = Modifier,
    channel: Channel,
    showChannelLogoProvider: () -> Boolean = { false },
    onChannelSelected: () -> Unit = {},
    onChannelFavoriteToggle: () -> Unit = {},
    onChannelFocused: () -> Unit = {},
    epgList: EpgList = EpgList(),
    showEpgProgrammeProgressProvider: () -> Boolean = { false },
    focusRequesterProvider: () -> FocusRequester = { FocusRequester() },
    initialFocusedProvider: () -> Boolean = { false },
    onInitialFocused: () -> Unit = {},
    isSelectedProvider: () -> Boolean = { false },
) {
    val nowEpgProgramme = remember(channel, epgList) { epgList.recentProgramme(channel)?.now }
    val showEpgProgrammeProgress = showEpgProgrammeProgressProvider()
    val focusRequester = focusRequesterProvider()

    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (initialFocusedProvider()) {
            onInitialFocused()
            focusRequester.saveRequestFocus()
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showChannelLogoProvider()) {
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .fillMaxHeight()
            ) {
                ChannelItemLogo(
                    modifier = Modifier.align(Alignment.Center),
                    logo = channel.logo,
                )
            }
        }

        Box(modifier = modifier.clip(ListItemDefaults.shape().shape)) {
            val isSelected = isSelectedProvider()
            val backgroundColor = if (isFocused) {
                MaterialTheme.colorScheme.onSurface
            } else if (isSelected) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                androidx.compose.ui.graphics.Color.Transparent
            }

            val contentColor = if (isFocused) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.onSurface
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundColor)
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        isFocused = it.isFocused || it.hasFocus
                        if (isFocused) onChannelFocused()
                    }
                    .focusable()
                    .handleKeyEvents(
                        onSelect = onChannelSelected,
                        onLongSelect = onChannelFavoriteToggle,
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        channel.name,
                        color = contentColor,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(animationMode = androidx.compose.foundation.MarqueeAnimationMode.WhileFocused),
                    )
                    Text(
                        text = nowEpgProgramme?.title ?: "无节目",
                        color = contentColor.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee(animationMode = androidx.compose.foundation.MarqueeAnimationMode.WhileFocused),
                    )
                }
            }

            if (showEpgProgrammeProgress && nowEpgProgramme != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(nowEpgProgramme.progress())
                        .height(3.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)),
                )
            }
        }
    }
}

@Preview
@Composable
private fun ClassicChannelItemListPreview() {
    MyTVTheme {
        Row {
            ClassicChannelItemList(
                channelListProvider = { ChannelList.EXAMPLE },
                initialChannelProvider = { ChannelList.EXAMPLE.first() },
                initialChannelIdxProvider = { 0 },
                epgListProvider = { EpgList.example(ChannelList.EXAMPLE) },
                showEpgProgrammeProgressProvider = { true },
            )
        }
    }
}

@Preview
@Composable
private fun ClassicChannelItemListWithChannelLogoPreview() {
    MyTVTheme {
        Row {
            ClassicChannelItemList(
                channelListProvider = { ChannelList.EXAMPLE },
                initialChannelProvider = { ChannelList.EXAMPLE.first() },
                initialChannelIdxProvider = { 0 },
                epgListProvider = { EpgList.example(ChannelList.EXAMPLE) },
                showEpgProgrammeProgressProvider = { true },
                showChannelLogoProvider = { true },
            )
        }
    }
}
