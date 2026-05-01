package top.yogiczy.mytv.core.data.entities.channel

import androidx.compose.runtime.Immutable

/**
 * 频道分组列表
 */
@Immutable
data class ChannelGroupList(
    val value: List<ChannelGroup> = emptyList(),
) : List<ChannelGroup> by value {
    val channelList: ChannelList by lazy {
        ChannelList(value.flatMap { it.channelList })
    }

    private val channelIdxMap: Map<Channel, Int> by lazy {
        channelList.withIndex().associate { it.value to it.index }
    }

    fun channelIdx(channel: Channel): Int = channelIdxMap[channel] ?: -1

    fun channelGroupIdx(channel: Channel): Int =
        value.indexOfFirst { group -> group.channelList.any { it == channel } }

    companion object {
        val EXAMPLE = ChannelGroupList(List(20) { groupIdx ->
            ChannelGroup(
                name = "频道分组${groupIdx + 1}",
                channelList = ChannelList(
                    List(20) { idx ->
                        Channel.EXAMPLE.copy(
                            name = "频道${groupIdx + 1}-${idx + 1}",
                            epgName = "频道${groupIdx + 1}-${idx + 1}",
                        )
                    },
                )
            )
        })
    }
}