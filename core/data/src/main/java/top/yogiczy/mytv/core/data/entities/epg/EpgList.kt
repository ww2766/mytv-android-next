package top.yogiczy.mytv.core.data.entities.epg

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import top.yogiczy.mytv.core.data.entities.channel.Channel
import top.yogiczy.mytv.core.data.entities.channel.ChannelList
import top.yogiczy.mytv.core.data.entities.epg.Epg.Companion.recentProgramme

/**
 * 频道节目单列表
 */
@Serializable
@Immutable
data class EpgList(
    val value: List<Epg> = emptyList(),
) : List<Epg> by value {
    private val fuzzyMatchMap by lazy {
        val map = mutableMapOf<String, Epg>()
        value.forEach { epg ->
            val name = epg.channel.lowercase()
            map[name] = epg
            // 存入几种常见的变体，增加命中率
            map[name.replace("-", "")] = epg
            map[name.replace(" ", "")] = epg
            if (name.endsWith("hd")) {
                val baseName = name.substring(0, name.length - 2).trimEnd()
                if (baseName.isNotEmpty()) map[baseName] = epg
            }
        }
        map
    }

    private fun String.toFuzzyKey() = this.lowercase()
        .replace("-", "")
        .replace(" ", "")
        .let {
            if (it.endsWith("hd") && it.length > 2) it.substring(0, it.length - 2) else it
        }

    companion object {
        fun EpgList.recentProgramme(channel: Channel): EpgProgrammeRecent? {
            if (isEmpty()) return null

            return match(channel)?.recentProgramme()
        }

        fun EpgList.match(channel: Channel): Epg? {
            if (isEmpty()) return null

            val name = channel.epgName.lowercase()
            return fuzzyMatchMap[name] ?: fuzzyMatchMap[name.toFuzzyKey()]
        }

        fun clearCache() {
            // 已移除 matchCache，此方法保留为空以兼容调用
        }

        fun example(channelList: ChannelList): EpgList {
            return EpgList(channelList.map(Epg.Companion::example))
        }
    }
}