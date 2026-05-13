package top.yogiczy.mytv.tv.ui.screens.epg.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import top.yogiczy.mytv.core.data.entities.channel.Channel
import top.yogiczy.mytv.core.data.entities.epg.Epg
import top.yogiczy.mytv.core.data.entities.epg.EpgProgramme
import top.yogiczy.mytv.core.data.entities.epg.EpgProgramme.Companion.isLive
import top.yogiczy.mytv.core.data.entities.epg.EpgProgrammeList
import top.yogiczy.mytv.core.data.entities.epg.EpgProgrammeReserveList
import top.yogiczy.mytv.tv.ui.theme.MyTVTheme
import kotlin.math.max

@Composable
fun EpgProgrammeItemList(
    modifier: Modifier = Modifier,
    epgProgrammeListProvider: () -> EpgProgrammeList = { EpgProgrammeList() },
    epgProgrammeReserveListProvider: () -> EpgProgrammeReserveList = { EpgProgrammeReserveList() },
    supportPlaybackProvider: () -> Boolean = { false },
    currentPlaybackProvider: () -> EpgProgramme? = { null },
    onPlayback: (EpgProgramme) -> Unit = {},
    onReserve: (EpgProgramme) -> Unit = {},
    focusOnLive: Boolean = true,
    onUserAction: () -> Unit = {},
) {
    val epgProgrammeList = epgProgrammeListProvider()
    val itemFocusRequesterMap = remember(epgProgrammeList) { mutableMapOf<Int, FocusRequester>() }

    val currentPlayback = currentPlaybackProvider()
    val initialIndex = remember(epgProgrammeList, currentPlayback) {
        val playbackIndex = epgProgrammeList.indexOf(currentPlayback)
        if (playbackIndex != -1) playbackIndex
        else max(0, epgProgrammeList.indexOfFirst { it.isLive() })
    }

    val listState = remember(epgProgrammeList) { LazyListState(max(0, initialIndex - 2)) }
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { _ -> onUserAction() }
    }

    LazyColumn(
        modifier = modifier,
        // FIXME 闪退
        // .focusRestorer {
        //     itemFocusRequesterMap[initialIndex] ?: FocusRequester.Default
        // },
        state = listState,
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        itemsIndexed(
            epgProgrammeList,
            key = { _, programme -> programme.hashCode() },
        ) { index, programme ->
            val focusRequester = remember(index) { itemFocusRequesterMap.getOrPut(index) { FocusRequester() } }
            
            val isPlayback by remember(programme) { derivedStateOf { currentPlaybackProvider() == programme } }
            val hasReserved by remember(programme) { derivedStateOf { epgProgrammeReserveListProvider().any { it.programme == programme.title } } }

            EpgProgrammeItem(
                modifier = Modifier.focusRequester(focusRequester),
                epgProgrammeProvider = { programme },
                supportPlaybackProvider = supportPlaybackProvider,
                isPlaybackProvider = { isPlayback },
                hasReservedProvider = { hasReserved },
                onPlayback = { onPlayback(programme) },
                onReserve = { onReserve(programme) },
                isInitialFocusProvider = { focusOnLive && index == initialIndex },
            )
        }
    }
}

@Preview
@Composable
private fun EpgProgrammeItemListPreview() {
    MyTVTheme {
        EpgProgrammeItemList(
            epgProgrammeListProvider = { EpgProgrammeList(Epg.example(Channel.EXAMPLE).programmeList) }
        )
    }
}