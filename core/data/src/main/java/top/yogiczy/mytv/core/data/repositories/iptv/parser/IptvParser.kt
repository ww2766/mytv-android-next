package top.yogiczy.mytv.core.data.repositories.iptv.parser

import top.yogiczy.mytv.core.data.entities.channel.ChannelGroupList

/**
 * 直播源数据解析接口
 */
interface IptvParser {
    /**
     * 是否支持该直播源格式
     */
    fun isSupport(url: String, data: String): Boolean

    /**
     * 解析直播源数据
     */
    suspend fun parse(data: String): MutableList<IptvResponseItem>// ChannelGroupList

    companion object {
        val instances = listOf(
            M3uIptvParser(),
            TxtIptvParser(),
            DefaultIptvParser(),
        )
    }
    data class IptvResponseItem(
        val name: String,
        val channelName: String,
        val groupName: String,
        val url: String,
        val logo: String?,
    )
}