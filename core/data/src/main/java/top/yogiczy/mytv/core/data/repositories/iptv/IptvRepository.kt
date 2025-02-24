package top.yogiczy.mytv.core.data.repositories.iptv

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import top.yogiczy.mytv.core.data.entities.channel.Channel
import top.yogiczy.mytv.core.data.entities.channel.ChannelGroup
import top.yogiczy.mytv.core.data.entities.channel.ChannelGroupList
import top.yogiczy.mytv.core.data.entities.channel.ChannelList
import top.yogiczy.mytv.core.data.entities.iptvsource.IptvSource
import top.yogiczy.mytv.core.data.network.await
import top.yogiczy.mytv.core.data.network.getProxyOkHttpClient
import top.yogiczy.mytv.core.data.network.proxyOkHttpFetchSource
import top.yogiczy.mytv.core.data.repositories.FileCacheRepository
import top.yogiczy.mytv.core.data.repositories.iptv.parser.IptvParser
import top.yogiczy.mytv.core.data.utils.ChannelUtil
import top.yogiczy.mytv.core.data.utils.Globals
import top.yogiczy.mytv.core.data.utils.Logger

/**
 * 直播源数据获取
 */
class IptvRepository(
    private val source: IptvSource,
) /*: FileCacheRepository(
    if (source.isLocal) source.url
    else "iptv-${source.url.hashCode().toUInt().toString(16)}.txt",
    source.isLocal,
)*/ {
    private val log = Logger.create(javaClass.simpleName)



    /**
     * 获取直播源分组列表
     */
    suspend fun getChannelGroupList(cacheTime: Long): ChannelGroupList {
        try {
            val gList = mutableListOf<IptvParser.IptvResponseItem>()
            val urlList=if (source.isLocal) listOf(source.url)
            else source.url.replace(';','#').replace(',','#').replace('$','#').replace('\n','#').split('#')
            var hasFailure=false
            urlList.forEach { item ->
                val url=item.trim()
                if(url.isEmpty())return@forEach
                val fileCacheRepository=FileCacheRepository("iptv-${url.hashCode().toUInt().toString(16)}.txt",false)
                var sourceData: String? = null
                try {
                    sourceData = fileCacheRepository.getOrRefresh(if (source.isLocal) Long.MAX_VALUE else cacheTime) {
                        proxyOkHttpFetchSource(url)
                    }
                } catch (ex: Exception) {
                    log.e("获取直播源失败", ex)
                    hasFailure=true
                    //throw Exception(ex)
                }

                //if (!hasFailure) {
                sourceData = fileCacheRepository.getCacheData()
                if(!sourceData.isNullOrEmpty()) {
                    val parser = IptvParser.instances.first { p -> p.isSupport(url, sourceData) }
                    val gTmpList = parser.parse(sourceData)
                    gList.addAll(gTmpList)
                }
                //}

            }
            if(hasFailure&&gList.isEmpty())
            {
                throw Exception("在线获取直播源失败，没有可播放源。")
            }
            val groupList=ChannelGroupList(gList.groupBy { it.groupName }.map { groupEntry ->
                ChannelGroup(
                    name = groupEntry.key,
                    channelList = ChannelList(groupEntry.value.groupBy { it.name }.map { nameEntry ->
                        Channel(
                            name = nameEntry.key,
                            epgName = nameEntry.value.first().channelName,
                            urlList = nameEntry.value.map { it.url },
                        )
                    })
                )
            })
            log.i("解析直播源完成：${gList.size} , ${groupList.size}个分组，${groupList.flatMap { it.channelList }.size}个频道")


            return groupList
        } catch (ex: Exception) {
            log.e("获取直播源失败", ex)
            throw Exception(ex)
        }
    }

    fun clearCache() {

        val iptvFiles = Globals.cacheDir.listFiles { pathname ->
            pathname.isFile && pathname.name.startsWith("iptv-")
        }

        iptvFiles?.forEach { file ->
            println(file.absolutePath)
            try {
                file.delete()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
    /*
    suspend fun getChannelGroupList(cacheTime: Long): ChannelGroupList {
        try {
            val sourceData = getOrRefresh(if (source.isLocal) Long.MAX_VALUE else cacheTime) {
                fetchSource(source.url)
            }

            val parser = IptvParser.instances.first { it.isSupport(source.url, sourceData) }
            val startTime = System.currentTimeMillis()
            val groupList = parser.parse(sourceData)
            log.i(
                listOf(
                    "解析直播源（${source.name}）完成：${groupList.size}个分组",
                    "${groupList.sumOf { it.channelList.size }}个频道",
                    "${groupList.sumOf { it.channelList.sumOf { channel -> channel.urlList.size } }}条线路",
                    "耗时：${System.currentTimeMillis() - startTime}ms",
                ).joinToString()
            )

            return groupList
        } catch (ex: Exception) {
            log.e("获取直播源失败", ex)
            throw Exception(ex)
        }
    }

    override suspend fun clearCache() {
        if (source.isLocal) return
        super.clearCache()
    }*/
}