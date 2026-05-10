package top.yogiczy.mytv.core.data.repositories.epg

import android.net.Uri
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import top.yogiczy.mytv.core.data.entities.epg.Epg
import top.yogiczy.mytv.core.data.entities.epg.EpgList
import top.yogiczy.mytv.core.data.entities.epg.EpgProgramme
import top.yogiczy.mytv.core.data.entities.epg.EpgProgrammeList
import top.yogiczy.mytv.core.data.entities.epgsource.EpgSource
import top.yogiczy.mytv.core.data.network.await
import top.yogiczy.mytv.core.data.network.getProxyOkHttpClient
import top.yogiczy.mytv.core.data.repositories.FileCacheRepository
import top.yogiczy.mytv.core.data.repositories.epg.fetcher.EpgFetcher
import top.yogiczy.mytv.core.data.utils.ChannelUtil
import top.yogiczy.mytv.core.data.utils.Globals
import top.yogiczy.mytv.core.data.utils.Logger
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 节目单获取
 */
class EpgRepository(
    source: EpgSource,
)/* : FileCacheRepository("epg-${source.url.hashCode().toUInt().toString(16)}.json") */{
    private val log = Logger.create(javaClass.simpleName)
    //private val epgXmlRepository = EpgXmlRepository(source.url)
    private val xmlUrl  =source.url
    /**
     * 解析节目单xml
     */
    private suspend fun parseFromXml(
        inputStream: java.io.InputStream,
    ) = withContext(Dispatchers.Default) {
        val dateFormat = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.getDefault())
        fun parseTime(time: String): Long {
            if (time.length < 14) return 0
            return dateFormat.parse(time)?.time ?: 0
        }

        val parser: XmlPullParser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, "UTF-8")

        val channelNameMap = mutableMapOf<String, String>()
        val programmeMap = mutableMapOf<String, MutableList<EpgProgramme>>()

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "channel") {
                        val channelId = parser.getAttributeValue(null, "id") ?: continue
                        parser.nextTag()
                        val channelName = parser.nextText()

                        channelNameMap[channelId] = channelName
                        programmeMap[channelId] = mutableListOf()
                    } else if (parser.name == "programme") {
                        val channelId = parser.getAttributeValue(null, "channel") ?: continue
                        if (channelNameMap.containsKey(channelId)) {
                            val startTime = parser.getAttributeValue(null, "start") ?: ""
                            val stopTime = parser.getAttributeValue(null, "stop") ?: ""
                            parser.nextTag()
                            val title = parser.nextText()

                            programmeMap[channelId]?.add(
                                EpgProgramme(
                                    startAt = parseTime(startTime),
                                    endAt = parseTime(stopTime),
                                    title = title,
                                )
                            )
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        val resultList = channelNameMap.map { (id, name) ->
            Epg(name, EpgProgrammeList(programmeMap[id] ?: emptyList()))
        }

        log.i("解析节目单完成，共${resultList.size}个频道，${programmeMap.values.sumOf { it.size }}个节目")
        return@withContext EpgList(resultList)
    }

    suspend fun getEpgList(
        filteredChannels: List<String> = emptyList(),
        refreshTimeThreshold: Int,
    ) = withContext(Dispatchers.Default) {

        try {
            // Step 1: 仅在达到刷新时间阈值后，尝试下载并缓存 EPG
            if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) >= refreshTimeThreshold) {
                try {
                    refreshEpgCache()
                } catch (ex: Exception) {
                    log.e("下载节目单失败", ex)
                }
            } else {
                log.d("未到${refreshTimeThreshold}:00，跳过下载，使用本地缓存")
            }

            // Step 2: 读取所有已缓存的 EPG 文件（无论是否刷新成功）
            val gList = mutableListOf<Epg>()
            val epgFiles = Globals.cacheDir.listFiles { pathname ->
                pathname.isFile && pathname.name.startsWith("epg-")
            }?.sortedByDescending { it.lastModified() }

            epgFiles?.forEach { file ->
                // 清理超过 7 天的旧缓存
                if (System.currentTimeMillis() - file.lastModified() > 7 * 24 * 3600 * 1000) {
                    try { file.delete() } catch (_: Exception) {}
                    return@forEach
                }

                val cacheRepo = FileCacheRepository(file.name)
                val jsonData = cacheRepo.getCacheData()
                try {
                    jsonData?.let { Json.decodeFromString<List<Epg>>(it) }?.let { list ->
                        // 内存过滤：仅保留频道列表中存在的频道
                        if (filteredChannels.isEmpty()) {
                            gList.addAll(list)
                        } else {
                            val lowerFilteredChannels = filteredChannels.map { it.lowercase() }
                            gList.addAll(list.filter { lowerFilteredChannels.contains(it.channel.lowercase()) })
                        }
                    }
                } catch (_: Exception) {}
            }
            val groupedItems = gList.groupBy { e -> e.channel }
                .map { (channel, itemsInCategory) ->
                    val combinedValues = itemsInCategory.flatMap { ie -> ie.programmeList }
                        .distinctBy { p -> p.startAt }
                        .sortedBy { p -> p.startAt }
                    Epg(channel, EpgProgrammeList(combinedValues))
                }
            EpgList(groupedItems)
        } catch (ex: Exception) {
            log.e("获取节目单失败", ex)
            throw Exception(ex)
        }
    }

    /**
     * 下载远程 EPG XML，解析后以 JSON 格式写入本地缓存
     */
    private suspend fun refreshEpgCache() {
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(System.currentTimeMillis())
        val cacheFileName = "epg-${xmlUrl.hashCode().toUInt().toString(16)}-$today.json"
        val cacheRepo = FileCacheRepository(cacheFileName)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        cacheRepo.getOrRefresh({ lastModified, _ ->
            // 如果缓存日期不是今天，则认为已过期需要刷新
            dateFormat.format(System.currentTimeMillis()) != dateFormat.format(lastModified)
        }) {
            log.i("开始下载节目单: $xmlUrl")
            fetchEpgXml(xmlUrl).use { inputStream ->
                val epgList = parseFromXml(inputStream)
                log.i("节目单解析完成，共${epgList.size}个频道")
                Json.encodeToString(epgList.value)
            }
        }
    }

    /**
     * 从远程 URL 获取 EPG XML 原始流（支持 .gz 压缩格式）
     */
    private suspend fun fetchEpgXml(url: String): java.io.InputStream = withContext(Dispatchers.IO) {
        log.d("获取远程节目单xml流: $url")
        val client = getProxyOkHttpClient(url)
        val request = Request.Builder().url(ChannelUtil.clearAllPrefixFromUrl(url)).build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            response.close()
            throw Exception("获取远程节目单xml失败: ${response.code}")
        }
        val fetcher = EpgFetcher.instances.first { it.isSupport(url) }
        fetcher.fetch(response)
    }

    fun clearCache() {
        val epgFiles = Globals.cacheDir.listFiles { pathname ->
            pathname.isFile && pathname.name.startsWith("epg-")
        }

        epgFiles?.forEach { file ->
            println(file.absolutePath)
            try {
                file.delete()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}

