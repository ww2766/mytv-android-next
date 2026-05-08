package top.yogiczy.mytv.tv.ui.screens.classicchannel.components

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.focusable
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.DenseListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import top.yogiczy.mytv.core.data.entities.channel.ChannelGroup
import top.yogiczy.mytv.core.data.entities.channel.ChannelGroupList
import top.yogiczy.mytv.tv.ui.material.rememberDebounceState
import top.yogiczy.mytv.tv.ui.screens.settings.LocalSettings
import top.yogiczy.mytv.tv.ui.theme.MyTVTheme
import top.yogiczy.mytv.tv.ui.utils.focusOnLaunchedSaveable
import top.yogiczy.mytv.tv.ui.utils.handleKeyEvents
import top.yogiczy.mytv.tv.ui.utils.ifElse
import top.yogiczy.mytv.tv.ui.utils.saveFocusRestorer
import top.yogiczy.mytv.tv.ui.utils.saveRequestFocus
import kotlin.math.max

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ClassicChannelGroupItemList(
    modifier: Modifier = Modifier,
    channelGroupListProvider: () -> ChannelGroupList = { ChannelGroupList() },
    initialChannelGroupProvider: () -> ChannelGroup = { ChannelGroup() },
    onChannelGroupFocused: (ChannelGroup) -> Unit = {},
    onUserAction: () -> Unit = {},
) {
    val channelGroupList = channelGroupListProvider()
    val initialChannelGroup = initialChannelGroupProvider()
    val itemFocusRequesterMap = remember(channelGroupList) { mutableMapOf<Int, FocusRequester>() }

    var focusedChannelGroup by remember { mutableStateOf(initialChannelGroup) }

    val listState = rememberLazyListState(max(0, channelGroupList.indexOf(initialChannelGroup) - 2))
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { _ -> onUserAction() }
    }

    val onChannelGroupFocusedDebounce = rememberDebounceState(wait = 100L) {
        onChannelGroupFocused(focusedChannelGroup)
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
            listState.scrollToItem(channelGroupList.lastIndex)
            lastFocusRequester.saveRequestFocus()
        }
    }

    LazyColumn(
        modifier = modifier
            .width(140.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface.copy(0.9f))
            .ifElse(
                LocalSettings.current.uiFocusOptimize,
                Modifier.saveFocusRestorer {
                    itemFocusRequesterMap[channelGroupList.indexOf(focusedChannelGroup)] ?: FocusRequester.Default
                },
            ),
        state = listState,
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        itemsIndexed(channelGroupList) { index, channelGroup ->
            val isSelected by remember { derivedStateOf { channelGroup == focusedChannelGroup } }

            val focusRequester = remember(index) { itemFocusRequesterMap.getOrPut(index) { FocusRequester() } }

            val currentScrollToLast by rememberUpdatedState { scrollToLast() }
            val currentScrollToFirst by rememberUpdatedState { scrollToFirst() }

            ClassicChannelGroupItem(
                modifier = Modifier
                    .ifElse(channelGroup == initialChannelGroup, Modifier.focusOnLaunchedSaveable())
                    .focusRequester(focusRequester)
                    .ifElse(
                        index == 0,
                        Modifier
                            .focusRequester(firstFocusRequester)
                            .handleKeyEvents(onUp = { currentScrollToLast() })
                    )
                    .ifElse(
                        index == channelGroupList.lastIndex,
                        Modifier
                            .focusRequester(lastFocusRequester)
                            .handleKeyEvents(onDown = { currentScrollToFirst() })
                    ),
                channelGroupProvider = { channelGroup },
                isSelectedProvider = { isSelected },
                onFocused = {
                    focusedChannelGroup = channelGroup
                    onChannelGroupFocusedDebounce.send()
                },
            )
        }
    }
}

@Composable
private fun ClassicChannelGroupItem(
    modifier: Modifier = Modifier,
    channelGroupProvider: () -> ChannelGroup = { ChannelGroup() },
    isSelectedProvider: () -> Boolean = { false },
    onFocused: () -> Unit = {},
) {
    val channelGroup = channelGroupProvider()

    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

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

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(ListItemDefaults.shape().shape)
            .background(backgroundColor)
            .focusRequester(focusRequester)
            .onFocusChanged {
                isFocused = it.isFocused || it.hasFocus
                if (isFocused) onFocused()
            }
            .focusable()
            .handleKeyEvents(
                isFocused = { isFocused },
                focusRequester = focusRequester,
                onSelect = {},
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = channelGroup.name,
            textAlign = TextAlign.Center,
            color = contentColor,
            maxLines = 1,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .fillMaxWidth()
                .basicMarquee(animationMode = androidx.compose.foundation.MarqueeAnimationMode.WhileFocused),
        )
    }
}

@Preview
@Composable
private fun ClassicChannelGroupItemListPreview() {
    MyTVTheme {
        ClassicChannelGroupItemList(
            channelGroupListProvider = { ChannelGroupList.EXAMPLE },
            initialChannelGroupProvider = { ChannelGroupList.EXAMPLE.first() },
        )
    }
}